const api = (path, opts = {}) =>
  fetch(path, {
    credentials: "include",
    headers: { "content-type": "application/json", ...(opts.headers || {}) },
    ...opts,
  });

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => [...document.querySelectorAll(sel)];

let me = null;

function show(view) {
  $$(".panel").forEach((p) => p.classList.add("hidden"));
  const el = $(`#view-${view}`);
  if (el) el.classList.remove("hidden");
  $$(".nav-btn").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
}

async function refreshMe() {
  const r = await api("/v1/accounts/me");
  if (!r.ok) {
    me = null;
    $("#nav").classList.add("hidden");
    $("#logoutBtn").classList.add("hidden");
    $("#whoami").textContent = "";
    show("login");
    await checkBootstrap();
    return false;
  }
  me = await r.json();
  $("#nav").classList.remove("hidden");
  $("#logoutBtn").classList.remove("hidden");
  $("#whoami").textContent = `${me.username} · ${(me.roles || []).join(",")}`;
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
    $("#loginError").textContent = "Signed in, but ADMIN role required for portal";
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
  $("#bootstrapError").textContent = "Admin created — sign in above.";
  $("#bootstrapBox").classList.add("hidden");
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
      $("#mfaStatus").textContent = me?.mfaEnabled ? "MFA is enabled on this account." : "MFA is off.";
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
    ["Events buffered", c.eventsBuffered ?? "—"],
    ["OTP channel", s.otpChannel],
  ]
    .map(([l, n]) => `<div class="stat"><div class="n">${n ?? "—"}</div><div class="l">${l}</div></div>`)
    .join("");
  const bus = s.identityEventBus || {};
  const feats = s.features || {};
  $("#featureFlags").innerHTML =
    "<h2>Identity Event Bus</h2>" +
    `<p class="muted">${esc(bus.wedge || "AAAX authenticates. Your mesh notifies.")}</p>` +
    `<p class="small">kafkaLive=${bus.kafkaLive} · topic=${esc(bus.kafkaTopic || "")} · webhook=${bus.webhookConfigured}</p>` +
    "<h2 style='margin-top:1rem'>Feature flags</h2>" +
    Object.entries(feats)
      .map(([k, v]) => `<span class="pill ${v ? "" : "off"}" style="margin:.2rem">${k}: ${v ? "on" : "off"}</span>`)
      .join(" ");
}

async function loadEvents() {
  const r = await api("/v1/admin/events?limit=80");
  if (!r.ok) return;
  const rows = await r.json();
  $("#eventsBody").innerHTML =
    rows
      .map((e) => {
        const data = e.data ? JSON.stringify(e.data) : "";
        const safe = data.length > 120 ? data.slice(0, 120) + "…" : data;
        return `<tr>
      <td class="small">${esc(e.time || "")}</td>
      <td><code>${esc(e.type || "")}</code></td>
      <td>${esc(e.subject || "")}</td>
      <td class="small muted">${esc(safe)}</td>
    </tr>`;
      })
      .join("") ||
    "<tr><td colspan='4' class='muted'>No events yet — login or register to emit.</td></tr>";
}

async function loadUsers() {
  const r = await api("/v1/admin/users");
  if (!r.ok) return;
  const users = await r.json();
  $("#usersBody").innerHTML = users
    .map(
      (u) => `<tr>
      <td>${esc(u.username)}</td>
      <td>${esc(u.email || "")}</td>
      <td>${esc((u.roles || []).join(", "))}</td>
      <td>${u.mfaEnabled ? '<span class="pill">on</span>' : '<span class="pill off">off</span>'}</td>
      <td>${u.enabled ? "yes" : "no"}</td>
      <td>
        <button class="btn small" data-toggle="${u.id}" data-en="${!u.enabled}">${u.enabled ? "Disable" : "Enable"}</button>
        <button class="btn small" data-roles="${u.id}" data-current="${esc((u.roles || []).join(","))}">Roles</button>
      </td>
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
        (c) => `<div style="padding:.5rem 0;border-bottom:1px solid var(--border)">
      <strong>${esc(c.clientId)}</strong> · ${esc(c.clientName || "")}
      <div class="small muted">${(c.scopes || []).join(" ")}</div>
      <button class="btn small danger" data-del="${esc(c.clientId)}">Delete</button>
    </div>`
      )
      .join("") || "<p class='muted'>No clients</p>";
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
  if (!r.ok) {
    $("#clientSecretOut").textContent = j.message || "Failed";
    return;
  }
  $("#clientSecretOut").textContent =
    "Save this secret now (shown once):\n" + (j.clientSecret || "") + "\nclientId=" + j.client?.clientId;
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
  $("#auditBody").innerHTML = rows
    .map(
      (e) => `<tr>
      <td class="small">${esc(e.createdAt || "")}</td>
      <td>${esc(e.action)}</td>
      <td>${esc(e.actor || "")}</td>
      <td class="small muted">${esc(e.detail || "")}</td>
    </tr>`
    )
    .join("");
}

async function loadSettings() {
  const r = await api("/v1/admin/settings");
  if (!r.ok) return;
  const s = await r.json();
  $("#settingsCard").innerHTML = `
    <div><strong>Issuer</strong><div class="muted">${esc(s.issuer)}</div></div>
    <div style="margin-top:.75rem"><strong>Win wedge</strong><div class="muted">${esc((s.identityEventBus && s.identityEventBus.wedge) || "Event bus")}</div></div>
    <div style="margin-top:.75rem"><strong>Event bus</strong><div class="muted">buffer=${(s.identityEventBus && s.identityEventBus.bufferSize) ?? 0} · kafka=${s.identityEventBus && s.identityEventBus.kafkaLive ? "live" : "off"} · topic=${esc((s.identityEventBus && s.identityEventBus.kafkaTopic) || "")}</div></div>
    <div style="margin-top:.75rem"><strong>OTP channel</strong><div class="muted">${esc(s.otpChannel)}</div></div>
    <div style="margin-top:.75rem"><strong>Orgs model</strong><div class="muted">${esc(s.orgsModel || "single")}</div></div>
    <div style="margin-top:.75rem"><strong>Demo seeds</strong><div class="muted">client=${s.demoSeedClient} account=${s.demoSeedAccount}</div></div>
    <div style="margin-top:.75rem"><strong>Google OIDC</strong><div class="muted">${s.googleLoginEnabled ? "configured" : "set GOOGLE_CLIENT_ID/SECRET + profile google"}</div></div>
    <div style="margin-top:.75rem"><strong>SAML SP</strong><div class="muted">${s.samlEnabled ? "on · " + (s.samlLoginPath || "") : "AAAX_SAML_ENABLED + IDP metadata URI"}</div></div>
    <div style="margin-top:.75rem"><strong>Version</strong><div class="muted">${esc(s.version)}</div></div>
  `;
  $("#decisions").innerHTML = (s.decisionBlockers || [])
    .map((d) => `<li><code>${esc(d.id)}</code> — ${esc(d.question)}</li>`)
    .join("");
}

function esc(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

refreshMe();
