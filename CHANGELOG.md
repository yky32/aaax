# Changelog

## [0.9.0-SNAPSHOT] — rebuild

### Structure
- **Reset product tree** to qs/**uaa** + qs/**app-core** (1:1 source port)
- Multi-module Maven:
  - `aaax-core` — `com.aaax.core` (vendored app-core, no private GH packages)
  - `aaax-server` — `com.aaax.*` (vendored uaa)
- Spring Boot **3.1.0** · Java 17+ (build with JDK 21)
- Product name **AAAX**

### Build
```bash
mvn -pl aaax-server -am package -Dmaven.test.skip=true
```

Integration Testcontainers suite excluded until deps restored (same spirit as uaa surefire excludes).
