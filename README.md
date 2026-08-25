# AAAX

**Accounts · Authentication · Authorization · eXperiences**

qs/uaa + app-core in **one** Maven project, two packages:

```text
src/main/java/com/aaax/
├── core/      ← app-core (BizException, R/Result, AuditEntity, …)
└── server/    ← uaa (entity, endpoint, usecase, config, …)
                 App.java lives here
```

| | |
|--|--|
| **Site** | https://aaax-www.vercel.app/ |
| **Main class** | `com.aaax.server.App` |
| **Scan** | `com.aaax.core` + `com.aaax.server` |
| **Base** | Spring Boot **3.1.0** · Java **17+** |

```bash
export JAVA_HOME=…/openjdk@21
mvn -DskipTests package
java -jar target/aaax-0.9.0-SNAPSHOT.jar
```

Port **8081**. Secrets via env only.

Apache-2.0
