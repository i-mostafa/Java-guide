# 1. Tooling setup

## 1.1 What to install

| Tool | Version | Notes |
|---|---|---|
| **JDK** | 21 (LTS) — match your team's project | Use **Eclipse Temurin** (free, TCK-certified). Java 25 is the newest LTS; 21 is fully supported and what this repo targets. |
| **SDKMAN!** | latest | Version manager for JDK/Maven/Gradle — the `nvm` of Java. <https://sdkman.io> |
| **Maven** | 3.9.x | Build tool. Or use the project's `./mvnw` wrapper if it has one (preferred — pins the version). |
| **IntelliJ IDEA** | latest | The de-facto Java IDE. Community edition is free and enough; Ultimate adds Spring/DB/HTTP tooling. VS Code works too (*Extension Pack for Java* + *Spring Boot Extension Pack*) but IntelliJ is far smoother for Java. |
| **Docker** | Docker Desktop / OrbStack / Colima | Needed for local infra and Testcontainers. |
| Git | any | |
| *Optional* | DBeaver (DB GUI), `httpie`/`jq`, `kcat` (Kafka CLI), Postman/Bruno | |

### macOS (Homebrew + SDKMAN)

```bash
# SDKMAN (manages JDK + Maven versions per project)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk list java | grep tem          # see available Temurin builds
sdk install java 21.0.8-tem       # pick the latest 21.x-tem listed
sdk install maven
sdk env install                   # in this repo: installs what .sdkmanrc asks for (update the version there if needed)

brew install --cask intellij-idea-ce docker    # or: brew install orbstack
brew install jq httpie
```

Add `sdkman_auto_env=true` to `~/.sdkman/etc/config` to switch JDKs automatically when you `cd` into a project (like `.nvmrc`).

### Verify

```bash
java -version
mvn -v            # must say "Java version: 21..."  — if not, JAVA_HOME points to another JDK
docker info
```

## 1.2 IntelliJ IDEA setup

1. **Open** the root folder (`homefin-platform`) → IntelliJ detects `pom.xml` and imports all modules. Accept "Trust project".
2. **Project SDK:** *File → Project Structure → Project → SDK* = 21. *Language level* = 21.
3. **Annotation processing** (for Lombok & MapStruct): *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing*.
4. **Lombok plugin**: bundled in recent versions; make sure it's enabled.
5. **Maven tool window** (right sidebar): run `clean`, `install`, reload after editing a `pom.xml` (or enable auto-reload).
6. **Run a service:** open `CustomerServiceApplication.java` → green ▶ next to `main`. Use *Run → Edit Configurations* to set *Active profiles* or env vars (`KYC_API_KEY=...`).
7. **Debug:** click the 🐞 instead of ▶, set breakpoints (click the gutter). *Evaluate Expression* (⌥F8) is your `console.log` superpower.
8. **Services tool window** (Ultimate / or *Run Dashboard*): start/stop all Spring Boot apps from one place.
9. **HTTP client**: open `http/homefin.http` and click ▶ next to a request.

### Shortcuts worth learning on day one (macOS)

| Action | Shortcut |
|---|---|
| Search everywhere (files, classes, actions) | ⇧⇧ |
| Go to class / file | ⌘O / ⌘⇧O |
| Go to implementation (interface → impl, e.g. a Spring Data repo) | ⌥⌘B |
| Find usages | ⌥F7 |
| Show quick fixes / imports | ⌥↩ |
| Rename (safe refactor) | ⇧F6 |
| Generate (constructor, getters, tests) | ⌘N |
| Reformat / optimize imports | ⌥⌘L / ⌃⌥O |
| Recent files | ⌘E |
| Run / Debug last | ⌃R / ⌃D |

## 1.3 Useful extras

- **Spring Initializr** — <https://start.spring.io> (also built into IntelliJ Ultimate: *New Project → Spring Boot*). Generates a correct skeleton with compatible versions. Use it whenever you start something new.
- **JDK tools you get for free:** `jshell` (a Java REPL, like `node`), `jps` (list JVMs), `jcmd <pid> Thread.print` (thread dump), `jfr` (Flight Recorder profiling).
- **Docker alternatives on Mac:** OrbStack is fast and lightweight; Colima is free/open-source. Both work with Testcontainers (Colima may need `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`, see Testcontainers docs).

## Sources
- SDKMAN: <https://sdkman.io/usage>
- Adoptium Temurin: <https://adoptium.net>
- IntelliJ IDEA docs: <https://www.jetbrains.com/help/idea/getting-started.html>
- Spring Boot system requirements: <https://docs.spring.io/spring-boot/system-requirements.html>
