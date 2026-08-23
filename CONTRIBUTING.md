# Contributing to AAAX

Thanks for interest. AAAX is a small Spring Boot OIDC product — **structure and neatness matter** (open-source bar).

## Dev setup

- JDK **21+**, Maven **3.9+**
- `mvn test` must pass
- No private Maven deps (`com.quinsic`, `app-core` banned by enforcer)

## Read first

1. **[docs/booklet.md](./docs/booklet.md)** — single product/eng SoT  
2. **[§7 Architecture](./docs/booklet.md#7-architecture)** — **layer-first** layout + PO/DTO rules  
3. **[§8 Code map](./docs/booklet.md#8-code-map-clone-tour)** — where to start reading  

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
6. Identity events: `IdentityEvent.Types` + catalog v1.0 (additive OK; renames need catalog bump)
7. Behavior change → update **`docs/booklet.md`** (+ `CHANGELOG.md`)
8. Do not commit secrets

## PR / push

Solo maintainer may push `main` directly. External contributors: open a PR against `main` with:

- What / why  
- Test plan (`mvn test` + curl if API)  
- Docs touch when surface changes  

## Good first issues

Label ideas (open issues on the repo):

1. **docs:** more curl recipes under `examples/curl/`  
2. **admin:** Events UI filter by `type` / `catalogVersion`  
3. **dx:** SSO login page — `/oauth2/authorize` unauthenticated → `/sign-in/` then resume  
4. **example:** minimal webhook consumer README with HMAC verify snippet  

## Security

Report vulnerabilities via [GitHub Security Advisories](https://github.com/yky32/aaax/security) — do not open public issues for secrets/vulns.
