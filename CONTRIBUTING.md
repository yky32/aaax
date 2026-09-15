# Contributing to AAAX

Thanks for interest. AAAX is a small Spring Boot OIDC product — **structure and neatness matter** (open-source bar).

## Dev setup

- JDK **21+**, Maven **3.9+**
- `mvn test` must pass
- No private Maven deps (`com.quinsic`, `app-core` banned by enforcer)
- Runtime smoke (optional): `./scripts/quickstart-smoke.sh` with seed on — see README

## Read first

1. **[docs/booklet.md](./docs/booklet.md)** — single product/eng SoT  
2. **[§3 Layout](./docs/booklet.md#3-layout)** — package tree + layering  
3. **[§8 Security posture](./docs/booklet.md#8-security-posture)** — CSRF split, JWK, public routes  

## Layout (do not invent a parallel tree)

```text
endpoint/<domain>/*Endpoint   → HTTP only
usecase/<domain>/*UseCase     → business
repository/ · spi/            → persistence / ports
entity/po                     → @Entity only (AuditEntity*)
entity/model                  → non-JPA domain
entity/dto/request|response   → *RequestDto · Get*|…*ResponseDto
core/                         → AuditEntity · BaseResponseDto · BizException · Ids
```

## Hard rules

1. HTTP types: `*Endpoint` under `endpoint/<domain>/` — not `*Controller`, not flat `web/`
2. Writes: `usecase` only — **no new business `@Service`** (`service/` = UDS / Totp / Audit / seeds)
3. PO: bare `@Entity` / `@Column` (**no `name=`**) · extend `AuditEntity` / `AuditEntityWithIsActive`
4. DTO: **one type per file** · suffix `RequestDto` / `ResponseDto` · **no bag classes**
5. Non-JPA types → `entity/model`, never `entity/po`
6. Behavior change → update **`docs/booklet.md`** (+ `CHANGELOG.md`)
7. Do not commit secrets
8. Do not claim features absent from booklet §2 (no Event Bus catalog, no `/v1`, no MCP PRM on this jar)

## PR / push

Solo maintainer may push `main` directly. External contributors: open a PR against `main` with:

- What / why  
- Test plan (`mvn test` + smoke scripts if OAuth/HTTP changed)  
- Docs touch when surface changes  
- **Do not merge** if CI `build` is red

## Good first issues

File issues on the repo for starter tasks. **Not in this tree:** hosted `/admin`, `/sign-in` product UI, events catalog HTTP, Identity Event Bus product surface.

**Hosted browser pages:** Thymeleaf + `static/css/aaax-hosted.css`. `HostedLoginEndpoint` → GET `/login`; `OAuthLoopbackEndpoint` → GET `/authorized`. POST `/login` + CSRF on `hostedLoginFilterChain`. After sign-in, Spring Security resumes the saved `/oauth2/authorize` request.

## Security

Report vulnerabilities via [GitHub Security Advisories](https://github.com/yky32/aaax/security) — do not open public issues for secrets/vulns.

## Code of conduct

See [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md).
