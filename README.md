# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Open-source identity server — **qs/uaa + app-core in one project**.

```text
src/main/java/com/aaax/
├── core/          ← qs app-core (BizException, R/Result, AuditEntity, …)
├── entity/        ← uaa POs / DTOs
├── endpoint/
├── usecase/
├── repository/
├── config/
└── App.java
```

| | |
|--|--|
| **Site** | https://aaax-www.vercel.app/ |
| **Layout** | **Single** Maven module (no aaax-core / aaax-server split) |
| **Packages** | `com.aaax.*` · `com.aaax.core.*` |
| **Base** | Spring Boot **3.1.0** · Java **17+** |

```bash
export JAVA_HOME=…/openjdk@21
mvn -DskipTests package
java -jar target/aaax-0.9.0-SNAPSHOT.jar
```

Default port **8081**. Config via env (no secrets in yml defaults).

Apache-2.0
