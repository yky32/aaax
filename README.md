# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Open-source identity server — **1:1 port of qs/uaa** + **in-project `aaax-core`** (from qs `app-core`).

| | |
|--|--|
| **Site** | https://aaax-www.vercel.app/ |
| **Layout** | Multi-module Maven |
| **Modules** | `aaax-core` (`com.aaax.core`) · `aaax-server` (`com.aaax.*`) |
| **Base** | Spring Boot **3.1.0** · Java **17+** (build with JDK 21 recommended) |
| **Source of truth (upstream)** | qs `uaa` + `app-core` trees |

```text
aaax/
├── pom.xml                 # parent
├── aaax-core/              # qs app-core → com.aaax.core (no private Maven)
└── aaax-server/            # qs uaa → com.aaax.*
```

### Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home)
mvn -pl aaax-server -am package -Dmaven.test.skip=true
java -jar aaax-server/target/aaax-server-0.9.0-SNAPSHOT.jar
```

Default port: **8081** (same as uaa).

### Product web

Separate repo: [yky32/aaax-www](https://github.com/yky32/aaax-www) · https://aaax-www.vercel.app/

### License

Apache-2.0
