# 3. Maven & project structure

## 3.1 `pom.xml` vs `package.json`

| package.json | pom.xml |
|---|---|
| `name`, `version` | `groupId` + `artifactId` + `version` (the "coordinates", e.g. `com.homefin:customer-service:0.1.0-SNAPSHOT`) |
| `dependencies` | `<dependencies>` with `<scope>compile</scope>` (default) |
| `devDependencies` | `<scope>test</scope>` (tests only), `<scope>provided</scope>` / `<optional>true</optional>` (compile-time only, e.g. Lombok) |
| — | `<scope>runtime</scope>` — needed to run, not to compile (JDBC driver) |
| lockfile | none by default; versions are pinned explicitly (often via a **BOM**) |
| `scripts` | **lifecycle phases** + **plugins** |
| `workspaces` | **multi-module** build (`<modules>`) |
| `node_modules/` | `~/.m2/repository` (shared cache across projects) |
| npm registry | Maven Central (+ your company's Nexus/Artifactory) |

`-SNAPSHOT` = a mutable development version; releases drop the suffix.

## 3.2 Parent POMs and BOMs (why you rarely write versions)

This repo's [root `pom.xml`](../pom.xml) inherits `spring-boot-starter-parent`, which brings:
- a **BOM** (Bill of Materials) of hundreds of tested-together versions (Hibernate, Jackson, Kafka client, Testcontainers...). That's why service poms say `spring-boot-starter-web` **without** a version.
- sensible plugin config (Java version, `-parameters`, resource filtering, `spring-boot-maven-plugin`).

We import a second BOM, `spring-cloud-dependencies`, in `<dependencyManagement>`. Always check the **Spring Cloud ↔ Spring Boot compatibility table** before changing either version: <https://spring.io/projects/spring-cloud#overview>.

`<dependencyManagement>` = "if someone uses X, use this version" (doesn't add the dependency). `<dependencies>` = "add X".

**Starters** (`spring-boot-starter-*`) are curated dependency bundles + auto-configuration (e.g. `spring-boot-starter-data-jpa` = Hibernate + HikariCP + Spring Data + transaction support).

## 3.3 Lifecycle & commands (npm → mvn)

Phases run in order; running one runs all previous ones: `validate → compile → test → package → verify → install → deploy`.

| npm | Maven |
|---|---|
| `npm install` | `mvn dependency:resolve` (happens automatically) |
| `npm run build` | `mvn package` (jar in `target/`) |
| `npm test` | `mvn test` (surefire runs `*Test`) |
| — | `mvn verify` (+ failsafe runs `*IT` integration tests) |
| `npm link` / publish to local | `mvn install` (puts jars in `~/.m2`, so other modules/projects can use them) |
| `npm start` | `mvn spring-boot:run` |
| `npm ls` | `mvn dependency:tree` (find where a transitive dependency comes from) |
| `npm outdated` | `mvn versions:display-dependency-updates` |
| `rm -rf dist` | `mvn clean` |

Flags you'll use constantly:

```bash
mvn -pl customer-service -am verify     # -pl: only this module, -am: also-make its dependencies
mvn -DskipTests package                 # skip running tests
mvn -Dtest=CustomerServiceTest test     # one test class  (-Dtest='CustomerServiceTest#kyc*')
mvn -o ...                              # offline
mvn -U ...                              # force-update snapshots
mvn -T 1C ...                           # parallel build, 1 thread per core
```

**Maven Wrapper** (`./mvnw`): a script committed to the repo that downloads the right Maven version — like pinning `packageManager` in package.json. If your project has it, always use `./mvnw` instead of `mvn`. Generate one with `mvn -N wrapper:wrapper`.

## 3.4 Standard directory layout

```
customer-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/homefin/customer/...      # production code
    │   └── resources/
    │       ├── application.yml                # config
    │       └── db/migration/V1__*.sql         # Flyway migrations
    └── test/
        ├── java/com/homefin/customer/...      # tests mirror the main package structure
        └── resources/application-test.yml
target/                                        # build output (git-ignored), like dist/
```

## 3.5 Multi-module builds

The root `pom.xml` lists `<modules>`; each module has `<parent>` pointing to the root. Modules depend on each other like regular dependencies (`common-lib`). Maven computes the build order (the "reactor").

**When to share code in a library** (`common-lib`): cross-cutting technical code (error format, security defaults) and *published contracts* (event records). **Avoid** sharing domain logic/entities between services — that couples their deployments. In large organisations shared libraries live in their own repo with semantic versioning.

## 3.6 Package structure inside a service

This repo uses **package-by-feature** (recommended), not package-by-layer:

```
com.homefin.customer
├── CustomerServiceApplication.java   # must be in the ROOT package: component scan starts here
├── config/            # @Configuration, security, kafka
├── customer/          # the feature: Controller, Service, Repository, Entity, Mapper, dto/, validation/
├── kyc/               # integration with the 3rd-party KYC provider
└── messaging/         # Kafka listeners
```

vs package-by-layer (`controllers/`, `services/`, `repositories/`) which scatters one feature across the tree. Many existing projects use layers — follow the codebase you're in. Hexagonal/"ports & adapters" is another common variant (`domain/`, `application/`, `adapters/in/web`, `adapters/out/persistence`).

## Sources
- Maven in 5 minutes: <https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html>
- Introduction to the lifecycle: <https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html>
- Dependency scopes & BOMs: <https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html>
- Spring Boot Maven plugin: <https://docs.spring.io/spring-boot/maven-plugin/index.html>
- Structuring your code (Spring Boot docs): <https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html>
