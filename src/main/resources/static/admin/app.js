const api = (path, opts = {}) =>
  fetch(path, {
    credentials: "include",
    headers: { "content-type": "application/json", ...(opts.headers || {}) },
    ...opts,
  });

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => [...document.querySelectorAll(sel)];

const TITLES = {
  login: ["AAAX", "Sign in"],
  dashboard: ["Operate", "Dashboard"],
  users: ["Operate", "Users"],
  clients: ["Operate", "OAuth clients"],
  mfa: ["Security", "MFA"],
  events: ["Security", "Identity events"],
  audit: ["Security", "Audit"],
  settings: ["System", "Settings"],
};

let me = null;

function show(view) {
  $$(".panel").forEach((p) => p.classList.add("hidden"));
  const el = $(`#view-${view}`);
  if (el) el.classList.remove("hidden");
  $$(".nav-btn").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
  const t = TITLES[view] || ["AAAX", "Console"];
  const eye = $("#pageEyebrow");
  const title = $("#pageTitle");
  if (eye) eye.textContent = t[0];
  if (title) title.textContent = t[1];
  document.body.dataset.view = view;
}

async function refreshMe() {
  const r = await api("/v1/accounts/me");
  if (!r.ok) {
    me = null;
    $("#nav").classList.add("hidden");
    $("#logoutBtn").classList.add("hidden");
    $("#whoami").textContent = "";
    $("#app").classList.add("is-auth");
    show("login");
    await checkBootstrap();
    await loadSocialProviders();
    return false;
  }
  me = await r.json();
  $("#app").classList.remove("is-auth");
  $("#nav").classList.remove("hidden");
  $("#logoutBtn").classList.remove("hidden");
  $("#whoami").innerHTML = `<strong>${esc(me.username)}</strong><br/><span class="muted small">${esc((me.roles || []).join(" · "))}</span>`;
  show("dashboard");
  await loadDashboard();
  return true;
}

async function checkBootstrap() {
  const r = await api("/v1/auth/bootstrap/status");
  if (!r.ok) return;
  const s = await r.json();
  $("#bootstrapBox").classList.toggle("hidden", !s.needsBootstrap);
}

async function loadSocialProviders() {
  const box = $("#socialButtons");
  const div = $("#socialDivider");
  if (!box) return;
  try {
    const r = await api("/v1/auth/social/providers");
    if (!r.ok) return;
    const s = await r.json();
    const list = s.providers || [];
    if (!list.length) {
      box.classList.add("hidden");
      div?.classList.add("hidden");
      return;
    }
    box.classList.remove("hidden");
    div?.classList.remove("hidden");
    box.innerHTML = list
      .map((p) => {
        const ico =
          p.id === "google"
            ? `<span class="g" aria-hidden="true"></span>`
            : `<span class="gh" aria-hidden="true">⌘</span>`;
        return `<a class="btn-social" href="${esc(p.authorizationUrl)}">${ico} Continue with ${esc(p.label)}</a>`;
      })
      .join("");
  } catch {
    /* ignore */
  }
}

$("#loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  $("#loginError").textContent = "";
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({
      username: fd.get("username"),
      password: fd.get("password"),
    }),
  });
  const body = await r.json().catch(() => ({}));
  if (!r.ok) {
    $("#loginError").textContent = body.message || "Login failed";
    return;
  }
  if (body.mfaRequired) {
    $("#loginForm").classList.add("hidden");
    $("#mfaForm").classList.remove("hidden");
    return;
  }
  if (!(body.account?.roles || []).includes("ADMIN")) {
    $("#loginError").textContent = "Signed in, but ADMIN role required for console";
    await api("/v1/auth/logout", { method: "POST" });
    return;
  }
  await refreshMe();
});

$("#mfaForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  $("#mfaError").textContent = "";
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/mfa/totp", {
    method: "POST",
    body: JSON.stringify({ code: fd.get("code") }),
  });
  const body = await r.json().catch(() => ({}));
  if (!r.ok) {
    $("#mfaError").textContent = body.message || "Invalid code";
    return;
  }
  $("#mfaForm").classList.add("hidden");
  $("#loginForm").classList.remove("hidden");
  await refreshMe();
});

$("#bootstrapForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  $("#bootstrapError").textContent = "";
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/bootstrap/admin", {
    method: "POST",
    body: JSON.stringify({
      username: fd.get("username"),
      email: fd.get("email") || null,
      password: fd.get("password"),
      bootstrapToken: fd.get("bootstrapToken") || null,
    }),
  });
  const body = await r.json().catch(() => ({}));
  if (!r.ok) {
    $("#bootstrapError").textContent = body.message || "Bootstrap failed";
    return;
  }
  $("#bootstrapError").textContent = "";
  $("#bootstrapBox").classList.add("hidden");
  $("#loginError").textContent = "Admin created — sign in above.";
});

$("#logoutBtn").addEventListener("click", async () => {
  await api("/v1/auth/logout", { method: "POST" });
  location.reload();
});

$$(".nav-btn").forEach((btn) => {
  btn.addEventListener("click", async () => {
    const v = btn.dataset.view;
    show(v);
    if (v === "dashboard") await loadDashboard();
    if (v === "users") await loadUsers();
    if (v === "clients") await loadClients();
    if (v === "events") await loadEvents();
    if (v === "audit") await loadAudit();
    if (v === "settings") await loadSettings();
    if (v === "mfa") {
      $("#mfaStatus").textContent = me?.mfaEnabled
        ? "MFA is enabled on this account."
        : "MFA is off for this account.";
    }
  });
});

async function loadDashboard() {
  const r = await api("/v1/admin/settings");
  if (!r.ok) return;
  const s = await r.json();
  const c = s.counts || {};
  $("#stats").innerHTML = [
    ["Users", c.users],
    ["Admins", c.admins],
    ["Clients", c.clients],
    ["Events", c.eventsBuffered ?? "—"],
    ["OTP", s.otpChannel],
  ]
    .map(
      ([l, n]) =>
        `<div class="stat"><div class="n">${esc(String(n ?? "—"))}</div><div class="l">${esc(l)}</div></div>`
    )
    .join("");

  const bus = s.identityEventBus || {};
  $("#wedgeCard").innerHTML = `
    <p class="eyebrow">Win wedge</p>
    <h2 class="wedge-title">${esc(bus.wedge || "AAAX authenticates. Your mesh notifies.")}</h2>
    <p class="wedge-body">Identity Event Bus fans out login, MFA, OTP, and client changes to your notification-service — you keep SMS.</p>
    <div class="kv-list">
      <div class="kv-row"><div class="k">Kafka</div><div class="v">${bus.kafkaLive ? "live" : "off"} · ${esc(bus.kafkaTopic || "—")}</div></div>
      <div class="kv-row"><div class="k">Webhook</div><div class="v">${bus.webhookConfigured ? "configured" : "not set"}</div></div>
      <div class="kv-row"><div class="k">Buffer</div><div class="v">${esc(String(bus.bufferSize ?? 0))} events</div></div>
    </div>`;

  const feats = s.features || {};
  $("#featureFlags").innerHTML =
    `<p class="eyebrow">Features</p><h2 class="wedge-title">Flags</h2><div class="flags-wrap">` +
    Object.entries(feats)
      .map(
        ([k, v]) =>
          `<span class="pill ${v ? "" : "off"}">${esc(k)}: ${v ? "on" : "off"}</span>`
      )
      .join("") +
    `</div>`;
}

async function loadEvents() {
  const r = await api("/v1/admin/events?limit=80");
  if (!r.ok) return;
  const rows = await r.json();
  $("#eventsBody").innerHTML =
    rows
      .map((e) => {
        const data = e.data ? JSON.stringify(e.data) : "";
        const safe = data.length > 100 ? data.slice(0, 100) + "…" : data;
        const time = (e.time || "").replace("T", " ").replace(/\.\d+Z?$/, "");
        return `<tr>
      <td class="small mono">${esc(time)}</td>
      <td><code>${esc(e.type || "")}</code></td>
      <td>${esc(e.subject || "")}</td>
      <td class="small muted">${esc(safe)}</td>
    </tr>`;
      })
      .join("") ||
    `<tr><td colspan="4" class="muted">No events yet — sign in or register to emit.</td></tr>`;
}

async function loadUsers() {
  const r = await api("/v1/admin/users");
  if (!r.ok) return;
  const users = await r.json();
  $("#usersBody").innerHTML = users
    .map(
      (u) => `<tr>
      <td><div class="user-cell"><strong>${esc(u.username)}</strong></div></td>
      <td class="muted small">${esc(u.email || "—")}</td>
      <td><span class="pill ${u.roles?.includes("ADMIN") ? "" : "off"}">${esc((u.roles || []).join(", ") || "—")}</span></td>
      <td>${u.mfaEnabled ? '<span class="pill">on</span>' : '<span class="pill off">off</span>'}</td>
      <td>${u.enabled ? '<span class="pill">active</span>' : '<span class="pill off">disabled</span>'}</td>
      <td><div class="row-actions">
        <button type="button" class="btn small" data-toggle="${esc(u.id)}" data-en="${!u.enabled}">${u.enabled ? "Disable" : "Enable"}</button>
        <button type="button" class="btn small ghost" data-roles="${esc(u.id)}" data-current="${esc((u.roles || []).join(","))}">Roles</button>
      </div></td>
    </tr>`
    )
    .join("");

  $$("[data-toggle]").forEach((b) =>
    b.addEventListener("click", async () => {
      await api(`/v1/admin/users/${b.dataset.toggle}/status`, {
        method: "PATCH",
        body: JSON.stringify({ enabled: b.dataset.en === "true" }),
      });
      await loadUsers();
    })
  );
  $$("[data-roles]").forEach((b) =>
    b.addEventListener("click", async () => {
      const roles = prompt("Roles CSV (e.g. USER,ADMIN)", b.dataset.current || "USER");
      if (!roles) return;
      await api(`/v1/admin/users/${b.dataset.roles}/roles`, {
        method: "PATCH",
        body: JSON.stringify({ roles }),
      });
      await loadUsers();
    })
  );
}

async function loadClients() {
  const r = await api("/v1/admin/clients");
  if (!r.ok) return;
  const clients = await r.json();
  $("#clientsList").innerHTML =
    clients
      .map(
        (c) => `<div class="client-row">
      <strong>${esc(c.clientId)}</strong>
      <div class="small muted">${esc(c.clientName || "")}</div>
      <div class="small muted" style="margin:.35rem 0">${esc((c.scopes || []).join(" · "))}</div>
      <button type="button" class="btn small danger" data-del="${esc(c.clientId)}">Delete</button>
    </div>`
      )
      .join("") || `<p class="muted">No clients yet.</p>`;

  $$("[data-del]").forEach((b) =>
    b.addEventListener("click", async () => {
      if (!confirm("Delete " + b.dataset.del + "?")) return;
      await api(`/v1/admin/clients/${encodeURIComponent(b.dataset.del)}`, { method: "DELETE" });
      await loadClients();
    })
  );
}

$("#clientForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const body = {
    clientId: fd.get("clientId"),
    clientName: fd.get("clientName") || fd.get("clientId"),
    redirectUris: String(fd.get("redirectUris") || "")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean),
    scopes: String(fd.get("scopes") || "openid,api.read")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean),
    grantTypes: ["authorization_code", "refresh_token", "client_credentials"],
  };
  const r = await api("/v1/admin/clients", { method: "POST", body: JSON.stringify(body) });
  const j = await r.json();
  const out = $("#clientSecretOut");
  if (!r.ok) {
    out.hidden = false;
    out.textContent = j.message || "Failed";
    return;
  }
  out.hidden = false;
  out.textContent =
    "Save this secret now (shown once)\n\n" +
    (j.clientSecret || "") +
    "\n\nclientId=" +
    (j.client?.clientId || "");
  e.target.reset();
  await loadClients();
});

$("#setupMfaBtn").addEventListener("click", async () => {
  const r = await api("/v1/accounts/me/mfa/totp/setup", { method: "POST", body: "{}" });
  const j = await r.json();
  if (!r.ok) {
    $("#mfaStatus").textContent = j.message || "setup failed";
    return;
  }
  $("#mfaSetup").classList.remove("hidden");
  $("#mfaSecret").textContent = j.secret;
  $("#mfaUrl").textContent = j.otpauthUrl;
});

$("#confirmMfaBtn").addEventListener("click", async () => {
  const code = $("#mfaConfirmCode").value.trim();
  const r = await api("/v1/accounts/me/mfa/totp/confirm", {
    method: "POST",
    body: JSON.stringify({ code }),
  });
  const j = await r.json();
  if (!r.ok) {
    $("#mfaStatus").textContent = j.message || "confirm failed";
    return;
  }
  me = j;
  $("#mfaStatus").textContent = "MFA enabled.";
  $("#mfaSetup").classList.add("hidden");
});

async function loadAudit() {
  const r = await api("/v1/admin/audit?limit=80");
  if (!r.ok) return;
  const rows = await r.json();
  $("#auditBody").innerHTML =
    rows
      .map((e) => {
        const when = String(e.createdAt || "").replace("T", " ").replace(/\.\d+Z?$/, "");
        return `<tr>
      <td class="small mono">${esc(when)}</td>
      <td><code>${esc(e.action)}</code></td>
      <td>${esc(e.actor || "")}</td>
      <td class="small muted">${esc(e.detail || "")}</td>
    </tr>`;
      })
      .join("") || `<tr><td colspan="4" class="muted">No audit rows.</td></tr>`;
}

async function loadSettings() {
  const r = await api("/v1/admin/settings");
  if (!r.ok) return;
  const s = await r.json();
  const bus = s.identityEventBus || {};
  const rows = [
    ["Issuer", s.issuer],
    ["Win wedge", bus.wedge || "—"],
    ["Event bus", `buffer ${bus.bufferSize ?? 0} · kafka ${bus.kafkaLive ? "live" : "off"} · ${bus.kafkaTopic || "—"}`],
    ["Events webhook", bus.webhookConfigured ? "configured" : "not set"],
    ["OTP channel", s.otpChannel],
    ["Orgs model", s.orgsModel || "single"],
    ["Demo seeds", `client=${s.demoSeedClient} · account=${s.demoSeedAccount}`],
    ["Google OIDC", s.googleLoginEnabled ? "configured" : "set GOOGLE_CLIENT_ID + profile social|google"],
    ["GitHub OAuth", s.githubLoginEnabled ? "configured" : "set GITHUB_CLIENT_ID + profile social"],
    ["SAML SP", s.samlEnabled ? `on · ${s.samlLoginPath || ""}` : "AAAX_SAML_ENABLED + IdP metadata"],
    ["Version", s.version],
  ];
  $("#settingsCard").innerHTML = rows
    .map(
      ([k, v]) =>
        `<div class="kv-row"><div class="k">${esc(k)}</div><div class="v">${esc(String(v ?? "—"))}</div></div>`
    )
    .join("");

  $("#decisions").innerHTML =
    (s.decisionBlockers || [])
      .map(
        (d) =>
          `<li><code>${esc(d.id)}</code><br/>${esc(d.question)}</li>`
      )
      .join("") || `<li class="muted">None listed.</li>`;
}

function esc(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

// mono helper class used in templates
const style = document.createElement("style");
style.textContent = `.mono{font-family:var(--mono)}`;
document.head.appendChild(style);

refreshMe();
