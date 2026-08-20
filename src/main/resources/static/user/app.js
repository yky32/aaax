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
  await loadPasskeys();
}

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
      <div><strong>${(s.userAgent || "session").slice(0, 48)}</strong><div class="small">${s.ip || ""} · ${s.createdAt || ""}</div></div>
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

$("#addPasskey").addEventListener("click", async () => {
  msg("");
  try {
    const optR = await api("/v1/passkeys/register/options");
    const opts = await optR.json();
    if (!optR.ok) throw new Error(opts.message || "options failed");

    if (!window.PublicKeyCredential) {
      // Fallback store for environments without WebAuthn (dev/tests)
      const fakeId = b64url(crypto.getRandomValues(new Uint8Array(16)));
      const fakeKey = b64url(crypto.getRandomValues(new Uint8Array(32)));
      const reg = await api("/v1/passkeys/register", {
        method: "POST",
        body: JSON.stringify({
          credentialId: fakeId,
          publicKeyCoseBase64: fakeKey,
          label: "Dev passkey",
        }),
      });
      if (!reg.ok) throw new Error("register failed");
      await loadPasskeys();
      return;
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
    // Store credential id + attestation public key bytes (COSE-ish payload from attestationObject tail — phase1)
    const reg = await api("/v1/passkeys/register", {
      method: "POST",
      body: JSON.stringify({
        credentialId: b64url(cred.rawId),
        publicKeyCoseBase64: b64url(att.getPublicKey ? att.getPublicKey() : att.attestationObject),
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

boot();
