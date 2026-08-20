const api = (path, opts = {}) =>
  fetch(path, {
    credentials: "include",
    headers: { "content-type": "application/json", ...(opts.headers || {}) },
    ...opts,
  });

const $ = (s) => document.querySelector(s);
const msg = (t) => { $("#msg").textContent = t || ""; $("#ok").textContent = ""; };
const ok = (t) => { $("#ok").textContent = t || ""; $("#msg").textContent = ""; };

const modes = {
  password: "#passwordForm",
  magic: "#magicForm",
  otp: "#otpForm",
};

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
    tab.classList.add("active");
    Object.entries(modes).forEach(([k, sel]) => {
      $(sel).classList.toggle("hidden", k !== tab.dataset.mode);
    });
    $("#mfaForm").classList.add("hidden");
    msg("");
  });
});

async function loadSocial() {
  const r = await api("/v1/auth/social/providers");
  if (!r.ok) return;
  const j = await r.json();
  const list = j.providers || [];
  if (!list.length) return;
  $("#social").classList.remove("hidden");
  $("#socialDiv").classList.remove("hidden");
  $("#social").innerHTML = list
    .map((p) => `<a class="btn-social" href="${p.authorizationUrl}">Continue with ${p.label}</a>`)
    .join("");
}

$("#passwordForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  msg("");
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ username: fd.get("username"), password: fd.get("password") }),
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) return msg(j.message || "Sign in failed");
  if (j.mfaRequired) {
    $("#passwordForm").classList.add("hidden");
    $("#mfaForm").classList.remove("hidden");
    ok("Enter authenticator code");
    return;
  }
  location.href = "/user/";
});

$("#mfaForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/mfa/totp", {
    method: "POST",
    body: JSON.stringify({ code: fd.get("code") }),
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) return msg(j.message || "Invalid code");
  location.href = "/user/";
});

$("#magicForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  msg("");
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/magic/request", {
    method: "POST",
    body: JSON.stringify({ identifier: fd.get("identifier") }),
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) return msg(j.message || "Could not send");
  ok(j.devLink ? `Dev link: ${j.devLink}` : "Check your email / notify channel");
});

$("#otpRequest").addEventListener("click", async () => {
  const username = $("#otpForm [name=username]").value;
  const r = await api("/v1/otp/request", {
    method: "POST",
    body: JSON.stringify({ username }),
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) return msg(j.message || "OTP failed");
  ok("Code sent (console/mail/kafka/sms per server config)");
});

$("#otpForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/otp/login", {
    method: "POST",
    body: JSON.stringify({ username: fd.get("username"), code: fd.get("code") }),
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) return msg(j.message || "Invalid code");
  location.href = "/user/";
});

// magic hash consume
(async () => {
  const hash = location.hash || "";
  if (hash.startsWith("#magic=")) {
    const token = hash.slice("#magic=".length);
    const r = await api("/v1/auth/magic/consume", {
      method: "POST",
      body: JSON.stringify({ token }),
    });
    if (r.ok) location.href = "/user/";
    else msg("Magic link invalid or expired");
  }
  await loadSocial();
  const me = await api("/v1/accounts/me");
  if (me.ok) location.href = "/user/";
})();
