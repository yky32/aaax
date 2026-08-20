# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Greenfield open identity product. Self-host OIDC-grade AAA with UX/DX that doesn’t fight you.

**Repo:** [`yky32/aaax`](https://github.com/yky32/aaax)

---

## The four letters

| | | |
|--|--|--|
| **A** | **Accounts** | People, orgs, profiles, identity records |
| **A** | **Authentication** | Prove who you are (password, OTP, OAuth, sessions, tokens) |
| **A** | **Authorization** | What you may do (roles, permissions, clients, scopes) |
| **X** | **eXperiences** | **UX** for humans + **DX** for builders |

Most auth stacks stop at the first three.  
**X is the product bet:** self-host *and* feel good to integrate.

---

## One-liner

> **AAAX** — open AAA with experiences: own your identity stack without giving up UX/DX.

---

## Competitive frame

| | Strength | AAAX angle |
|--|----------|------------|
| **Clerk** | Hosted UX, polish | Self-host + control + no seat tax |
| **better-auth** | TS/Next DX | JVM/Spring-first + OIDC-grade server |
| **Logto** | OIDC + self-host | Ops-hardened defaults + clearer DX from real production scars |

We do **not** win by cloning Clerk’s dashboard day one.  
We win by **trust + run-your-own + fewer footguns**.

---

## Principles

1. **Self-host first**  
2. **OIDC-grade core** — clients, tokens, JWKS, refresh  
3. **X is mandatory** — docs, quickstarts, sane defaults  
4. **Greenfield honesty** — no private monorepo dump; build in public  
5. **Secrets never in git**  
6. Product GitHub org later when name + scope are stable  

---

## v1 scope

**In**
- Accounts (register / basic profile)
- Authentication (password + OTP path, OAuth2/OIDC server)
- Authorization (RBAC baseline + protected API sample)
- DX: Compose, curl cookbook, English README
- UX: intentional login/OTP (not a full design system)

**Out (later)**
- Full hosted admin dashboard
- Every social provider day one
- Passkeys / enterprise SSO packs

---

## Where we are (now)

Greenfield `aaax-server` skeleton:
- Spring Authorization Server boots
- In-memory demo user + demo client
- Ephemeral RSA JWK (dev)
- Public product meta + health + OIDC discovery

Next execution order → [ROADMAP.md](./ROADMAP.md)

---

*AAAX — four letters, one job: identity you can run and ship with.*
