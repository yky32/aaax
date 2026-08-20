const api = (path, opts = {}) =>
  fetch(path, {
    credentials: "include",
    headers: { "content-type": "application/json", ...(opts.headers || {}) },
    ...opts,
  });

const $ = (s) => document.querySelector(s);
const msg = (t) => {
  $("#msg").textContent = t || "";
  $("#ok").textContent = "";
};
const ok = (t) => {
  $("#ok").textContent = t || "";
  $("#msg").textContent = "";
};

const modes = {
  password: "#passwordForm",
  magic: "#magicForm",
  otp: "#otpForm",
  qr: "#qrPanel",
};

let qrPollTimer = null;
let qrSessionId = null;

function stopQrPoll() {
  if (qrPollTimer) {
    clearInterval(qrPollTimer);
    qrPollTimer = null;
  }
}

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
    tab.classList.add("active");
    Object.entries(modes).forEach(([k, sel]) => {
      const el = $(sel);
      if (el) el.classList.toggle("hidden", k !== tab.dataset.mode);
    });
    $("#mfaForm").classList.add("hidden");
    msg("");
    if (tab.dataset.mode === "qr") startQr();
    else stopQrPoll();
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

async function startQr() {
  stopQrPoll();
  msg("");
  const r = await api("/v1/auth/qr/sessions", { method: "POST" });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) return msg(j.message || "Could not start QR session");
  qrSessionId = j.sessionId;
  $("#qrStatus").textContent = j.status || "PENDING";
  $("#qrCode").textContent = j.userCode || "—";
  const approveUrl = j.approveUrl || j.approvePath;
  $("#qrApproveLink").href = j.approvePath || approveUrl;
  $("#qrImg").src =
    "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + encodeURIComponent(approveUrl);
  ok("Waiting for phone approval…");
  qrPollTimer = setInterval(async () => {
    if (!qrSessionId) return;
    const pr = await api("/v1/auth/qr/sessions/" + qrSessionId);
    const st = await pr.json().catch(() => ({}));
    if (!pr.ok) {
      $("#qrStatus").textContent = "ERROR";
      stopQrPoll();
      return msg(st.message || "Session lost");
    }
    $("#qrStatus").textContent = st.status;
    if (st.status === "APPROVED") {
      stopQrPoll();
      const cr = await api("/v1/auth/qr/sessions/" + qrSessionId + "/consume", { method: "POST" });
      const cj = await cr.json().catch(() => ({}));
      if (!cr.ok) return msg(cj.message || "Consume failed");
      location.href = "/user/";
    }
    if (st.status === "EXPIRED" || st.status === "CONSUMED") {
      stopQrPoll();
      msg("QR expired — tap New QR");
    }
  }, 1500);
}

$("#qrRefresh")?.addEventListener("click", () => startQr());

$("#passwordForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  msg("");
  const fd = new FormData(e.target);
  const r = await api("/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({
      username: fd.get("username"),
      password: fd.get("password"),
      rememberDevice: fd.get("rememberDevice") === "on",
    }),
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) return msg(j.message || "Sign in failed");
  if (j.mfaRequired) {
    $("#passwordForm").classList.add("hidden");
    $("#mfaForm").classList.remove("hidden");
    window.__aaaxRememberDevice = fd.get("rememberDevice") === "on";
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
    body: JSON.stringify({
      code: fd.get("code"),
      rememberDevice: window.__aaaxRememberDevice === true,
    }),
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
