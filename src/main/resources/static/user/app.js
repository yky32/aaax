const api = (path, opts = {}) =>
  fetch(path, {
    credentials: "include",
    headers: { "content-type": "application/json", ...(opts.headers || {}) },
    ...opts,
  });

const $ = (s) => document.querySelector(s);
const msg = (t) => ($("#msg").textContent = t || "");

function b64url(buf) {
  const bytes = buf instanceof ArrayBuffer ? new Uint8Array(buf) : buf;
  let s = "";
  bytes.forEach((b) => (s += String.fromCharCode(b)));
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function fromB64url(s) {
  const pad = s.length % 4 === 0 ? "" : "=".repeat(4 - (s.length % 4));
  const b64 = s.replace(/-/g, "+").replace(/_/g, "/") + pad;
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out.buffer;
}

async function boot() {
  const r = await api("/v1/accounts/me");
  if (!r.ok) {
    location.href = "/sign-in/";
    return;
  }
  const me = await r.json();
  $("#profile").innerHTML = [
    ["Username", me.username],
    ["Email", me.email || "—"],
    ["Roles", (me.roles || []).join(", ")],
    ["MFA", me.mfaEnabled ? "on" : "off"],
    ["Google", me.googleLinked ? "linked" : "—"],
    ["GitHub", me.githubLinked ? "linked" : "—"],
  ]
    .map(([k, v]) => `<div><div class="k">${k}</div><div class="v">${v}</div></div>`)
    .join("");
  await loadSessions();
  await loadDevices();

  let passkeysOn = false;
  try {
    const meta = await fetch("/").then((x) => x.json());
    passkeysOn = meta.features && (meta.features.passkeys === "experimental" || meta.features.passkeys === "webauthn4j");
  } catch (_) {}
  const pkSection = $("#passkeySection");
  if (!passkeysOn) {
    if (pkSection) pkSection.hidden = true;
  } else {
    if (pkSection) pkSection.hidden = false;
    await loadPasskeys();
  }
}

async function loadDevices() {
  const r = await api("/v1/devices");
  if (!r.ok) {
    $("#devices").innerHTML = `<div class="small">Could not load devices</div>`;
    return;
  }
  const rows = await r.json();
  $("#devices").innerHTML =
    rows
      .map(
        (d) => `<div class="row">
      <div><strong>${d.label || "Device"}</strong><div class="small">${(d.userAgent || "").slice(0, 40)} · exp ${d.expiresAt || ""}</div></div>
      <button type="button" class="btn ghost" style="width:auto;padding:.35rem .6rem" data-dev="${d.id}">Revoke</button>
    </div>`
      )
      .join("") || `<div class="small">No trusted devices</div>`;
  document.querySelectorAll("[data-dev]").forEach((b) =>
    b.addEventListener("click", async () => {
      await api(`/v1/devices/${b.dataset.dev}`, { method: "DELETE" });
      await loadDevices();
    })
  );
}

$("#trustDevice")?.addEventListener("click", async () => {
  msg("");
  const r = await api("/v1/devices", { method: "POST", body: JSON.stringify({ label: "This browser" }) });
  if (!r.ok) return msg("Could not trust device");
  await loadDevices();
});

$("#revokeDevices")?.addEventListener("click", async () => {
  await api("/v1/devices/revoke-all", { method: "POST" });
  await loadDevices();
});

async function loadSessions() {
  const r = await api("/v1/sessions");
  if (!r.ok) {
    $("#sessions").innerHTML = `<div class="small">Could not load sessions</div>`;
    return;
  }
  const rows = await r.json();
  $("#sessions").innerHTML =
    rows
      .map(
        (s) => `<div class="row">
      <div><strong>${(s.userAgent || "session").slice(0, 48)}</strong><div class="small">${s.ip || ""} · ${s.createDt || ""}</div></div>
      <button type="button" class="btn ghost" style="width:auto;padding:.35rem .6rem" data-id="${s.id}">Revoke</button>
    </div>`
      )
      .join("") || `<div class="small">No tracked sessions yet</div>`;
  document.querySelectorAll("[data-id]").forEach((b) =>
    b.addEventListener("click", async () => {
      await api(`/v1/sessions/${b.dataset.id}`, { method: "DELETE" });
      await loadSessions();
    })
  );
}

async function loadPasskeys() {
  const r = await api("/v1/passkeys");
  if (r.status === 404) {
    $("#passkeys").innerHTML = `<div class="small">Passkeys disabled on this server</div>`;
    return;
  }
  if (!r.ok) {
    $("#passkeys").innerHTML = `<div class="small">Sign in required</div>`;
    return;
  }
  const rows = await r.json();
  $("#passkeys").innerHTML =
    rows
      .map(
        (p) => `<div class="row">
      <div><strong>${p.label || "Passkey"}</strong><div class="small"><code>${(p.credentialId || "").slice(0, 24)}…</code></div></div>
      <button type="button" class="btn ghost" style="width:auto;padding:.35rem .6rem" data-pk="${p.id}">Remove</button>
    </div>`
      )
      .join("") || `<div class="small">No passkeys yet</div>`;
  document.querySelectorAll("[data-pk]").forEach((b) =>
    b.addEventListener("click", async () => {
      await api(`/v1/passkeys/${b.dataset.pk}`, { method: "DELETE" });
      await loadPasskeys();
    })
  );
}

$("#revokeAll").addEventListener("click", async () => {
  await api("/v1/sessions/revoke-all", { method: "POST" });
  await loadSessions();
});

$("#logout").addEventListener("click", async () => {
  await api("/v1/auth/logout", { method: "POST" });
  location.href = "/sign-in/";
});

const addBtn = $("#addPasskey");
if (addBtn) {
  addBtn.addEventListener("click", async () => {
    msg("");
    try {
      const optR = await api("/v1/passkeys/register/options");
      const opts = await optR.json();
      if (!optR.ok) throw new Error(opts.message || "options failed (passkeys may be disabled)");
      if (!window.PublicKeyCredential) {
        throw new Error("WebAuthn not available in this browser");
      }
      const publicKey = {
        challenge: fromB64url(opts.challenge),
        rp: opts.rp,
        user: {
          id: fromB64url(opts.user.id),
          name: opts.user.name,
          displayName: opts.user.displayName,
        },
        pubKeyCredParams: opts.pubKeyCredParams,
        timeout: opts.timeout,
        attestation: opts.attestation,
        authenticatorSelection: opts.authenticatorSelection,
      };
      const cred = await navigator.credentials.create({ publicKey });
      const att = cred.response;
      const reg = await api("/v1/passkeys/register", {
        method: "POST",
        body: JSON.stringify({
          clientDataJSON: b64url(att.clientDataJSON),
          attestationObject: b64url(att.attestationObject),
          label: "Passkey",
        }),
      });
      if (!reg.ok) {
        const j = await reg.json().catch(() => ({}));
        throw new Error(j.message || "register failed");
      }
      await loadPasskeys();
    } catch (e) {
      msg(e.message || String(e));
    }
  });
}

boot();
