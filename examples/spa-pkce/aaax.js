/**
 * Minimal AAAX browser helper — OIDC authorization code + PKCE.
 * No build step. Works with public client `aaax-spa`.
 *
 * @example
 *   const aaax = Aaax.create({ issuer: 'http://localhost:8081', clientId: 'aaax-spa' });
 *   await aaax.login();                 // redirect
 *   // on callback.html:
 *   await aaax.handleRedirectCallback();
 *   const hello = await aaax.fetchJson('/v1/api/hello');
 */
(function (global) {
  const STORAGE = {
    verifier: "aaax_pkce_verifier",
    state: "aaax_oauth_state",
    tokens: "aaax_tokens",
  };

  function b64url(buf) {
    const bytes = buf instanceof ArrayBuffer ? new Uint8Array(buf) : buf;
    let s = "";
    bytes.forEach((b) => (s += String.fromCharCode(b)));
    return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  function randomString(n) {
    const a = new Uint8Array(n);
    crypto.getRandomValues(a);
    return b64url(a);
  }

  async function sha256(str) {
    const data = new TextEncoder().encode(str);
    return crypto.subtle.digest("SHA-256", data);
  }

  function Aaax(opts) {
    if (!opts || !opts.issuer || !opts.clientId) {
      throw new Error("Aaax.create({ issuer, clientId }) required");
    }
    this.issuer = opts.issuer.replace(/\/$/, "");
    this.clientId = opts.clientId;
    this.redirectUri = opts.redirectUri || global.location.origin + "/callback.html";
    this.scopes = opts.scopes || "openid profile api.read";
    this.storage = opts.storage || global.sessionStorage;
  }

  Aaax.create = function (opts) {
    return new Aaax(opts);
  };

  Aaax.prototype._tokens = function () {
    try {
      return JSON.parse(this.storage.getItem(STORAGE.tokens) || "null");
    } catch (_) {
      return null;
    }
  };

  Aaax.prototype._saveTokens = function (t) {
    this.storage.setItem(STORAGE.tokens, JSON.stringify(t));
  };

  Aaax.prototype.getAccessToken = function () {
    const t = this._tokens();
    return t && t.access_token ? t.access_token : null;
  };

  Aaax.prototype.getIdToken = function () {
    const t = this._tokens();
    return t && t.id_token ? t.id_token : null;
  };

  Aaax.prototype.isAuthenticated = function () {
    return !!this.getAccessToken();
  };

  Aaax.prototype.clear = function () {
    this.storage.removeItem(STORAGE.tokens);
    this.storage.removeItem(STORAGE.verifier);
    this.storage.removeItem(STORAGE.state);
  };

  Aaax.prototype.login = async function () {
    const verifier = randomString(32);
    const challenge = b64url(await sha256(verifier));
    const state = randomString(16);
    this.storage.setItem(STORAGE.verifier, verifier);
    this.storage.setItem(STORAGE.state, state);
    const u = new URL(this.issuer + "/oauth2/authorize");
    u.searchParams.set("response_type", "code");
    u.searchParams.set("client_id", this.clientId);
    u.searchParams.set("redirect_uri", this.redirectUri);
    u.searchParams.set("scope", this.scopes);
    u.searchParams.set("state", state);
    u.searchParams.set("code_challenge", challenge);
    u.searchParams.set("code_challenge_method", "S256");
    global.location.assign(u.toString());
  };

  Aaax.prototype.handleRedirectCallback = async function () {
    const params = new URLSearchParams(global.location.search);
    const err = params.get("error");
    if (err) {
      throw new Error(err + ": " + (params.get("error_description") || ""));
    }
    const code = params.get("code");
    const state = params.get("state");
    if (!code) {
      throw new Error("missing code");
    }
    if (state !== this.storage.getItem(STORAGE.state)) {
      throw new Error("state mismatch");
    }
    const verifier = this.storage.getItem(STORAGE.verifier);
    if (!verifier) {
      throw new Error("missing PKCE verifier");
    }
    const body = new URLSearchParams();
    body.set("grant_type", "authorization_code");
    body.set("code", code);
    body.set("redirect_uri", this.redirectUri);
    body.set("client_id", this.clientId);
    body.set("code_verifier", verifier);
    const resp = await fetch(this.issuer + "/oauth2/token", {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body,
    });
    const json = await resp.json().catch(() => ({}));
    if (!resp.ok) {
      throw new Error(json.error_description || json.error || "token exchange failed");
    }
    this._saveTokens(json);
    this.storage.removeItem(STORAGE.verifier);
    this.storage.removeItem(STORAGE.state);
    return json;
  };

  Aaax.prototype.refresh = async function () {
    const t = this._tokens();
    if (!t || !t.refresh_token) {
      throw new Error("no refresh_token");
    }
    const body = new URLSearchParams();
    body.set("grant_type", "refresh_token");
    body.set("refresh_token", t.refresh_token);
    body.set("client_id", this.clientId);
    const resp = await fetch(this.issuer + "/oauth2/token", {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body,
    });
    const json = await resp.json().catch(() => ({}));
    if (!resp.ok) {
      throw new Error(json.error_description || json.error || "refresh failed");
    }
    this._saveTokens({ ...t, ...json });
    return json;
  };

  Aaax.prototype.fetchJson = async function (path, init) {
    const token = this.getAccessToken();
    if (!token) {
      throw new Error("not authenticated");
    }
    const url = path.startsWith("http") ? path : this.issuer + path;
    const headers = Object.assign({}, (init && init.headers) || {}, {
      Authorization: "Bearer " + token,
    });
    const resp = await fetch(url, Object.assign({}, init || {}, { headers }));
    if (resp.status === 401 && this._tokens()?.refresh_token) {
      await this.refresh();
      headers.Authorization = "Bearer " + this.getAccessToken();
      const retry = await fetch(url, Object.assign({}, init || {}, { headers }));
      return retry.json();
    }
    return resp.json();
  };

  Aaax.prototype.logoutLocal = function () {
    this.clear();
  };

  global.Aaax = Aaax;
})(typeof window !== "undefined" ? window : globalThis);
