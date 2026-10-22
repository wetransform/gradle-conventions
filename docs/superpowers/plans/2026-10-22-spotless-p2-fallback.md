# Spotless P2 Outage Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep builds of all projects using these conventions working while `download.eclipse.org` is
down, by choosing at configuration time between the original P2 site, a public mirror, or skipping
the Groovy-Eclipse formatter with a loud warning. Also cache spotless P2 data in the shared CI
workflows.

**Architecture:** A Gradle `ValueSource` runs a P2 health check, so the decision is re-evaluated
on every build, including configuration-cache hits. It returns an encoded `P2Source`
(ORIGINAL / MIRROR / SKIP). `SpotlessPlugin` resolves it once per build, memoized on the root
project, and applies it to both `greclipse()` usages. `gha-workflows` restores and saves
`~/.m2/repository/dev/equo/p2-data` around the Gradle steps.

**Tech Stack:** Groovy Gradle plugin (Gradle 9.8.1, Java 17 via mise), spotless-plugin-gradle 8.10.4,
Spock 2.5 + Gradle TestKit, JDK `com.sun.net.httpserver.HttpServer` for test servers, GitHub
Actions `actions/cache`.

**Spec:** `docs/superpowers/specs/2026-10-22-spotless-p2-fallback-design.md`

## Global Constraints

- Package for all new plugin classes: `to.wetransform.gradle.conventions`, in `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/`, written in Groovy like `SpotlessPlugin`.
- Every new source file carries the Apache license header used in existing files, with `Copyright 2026 wetransform GmbH`. Run `./gradlew spotlessApply` before each commit.
- Only the `greclipse()` steps (`groovyGradle`, `groovy`) are affected. The Java `eclipse()` step and all other formats stay unchanged.
- Original P2 prefix: `https://download.eclipse.org/eclipse/updates/`. Mirror replacement: `<mirrorBase>/eclipse/updates/`. Groovy-Eclipse identity mapping: `https://groovy.jfrog.io/` → `https://groovy.jfrog.io/`.
- Gradle properties: `wetf.spotless.p2.mode` = `auto` (default) | `original` | `mirror` | `skip` (case-insensitive). `wetf.spotless.p2.mirrors` = comma-separated base URLs, which replace the built-in list.
- Healthy = HTTP 200, content type not `text/html`, and the body is a real P2 index/repository file (see Task 2). Connect/read timeouts default to 3000 ms each.
- The decision is made at most once per build, and must work with `--configuration-cache`.
- Skip mode must print a loud warning on every build, including configuration-cache hits. Under `GITHUB_ACTIONS=true` it also prints a `::warning::` line.
- Mirrors that need credentials are out of scope. There is no `--continue` change in the workflows.
- Commits follow Conventional Commits. Plugin behaviour changes use `feat(spotless):`. No `Co-authored-by`.
- New GitHub Actions are pinned by commit SHA with a `# vX.Y.Z` comment, using the latest release.

## Review Focus

1. The original site answers `200` with an HTML "temporary shutdown" page for every path, which is the actual outage. It must be unhealthy, not ORIGINAL. Test: Task 2, `html page with status 200 is unhealthy`.
2. The original site redirects (`302`) to an HTML page. The redirect must be followed and the result must be unhealthy. Test: Task 2, `redirect to html page is unhealthy`.
3. A configuration-cache hit in skip mode still prints the warning on the second build. Test: Task 4, `skip warning is printed again on configuration cache hit`.
4. A multi-project build with several projects applying the plugin queries each site only once per build. Test: Task 4, `health check runs once per build in multi-project build`.
5. Mirror URLs given with a trailing slash or surrounding whitespace in `wetf.spotless.p2.mirrors` produce exactly `<base>/eclipse/updates/` without double slashes. Test: Task 3, `mirror list is normalized`.

---

### Task 1: Spike — verify spotless/Solstice behaviour (throwaway, no product code)

Answers the spec's "Must verify first" items, and pins the facts later tasks rely on. Work in a
scratch Gradle project outside the repo, e.g. under the session scratchpad. It applies
`com.diffplug.spotless` 8.10.4 directly, with a `groovyGradle { greclipse() }` step.

**Files:**
- Create: `docs/superpowers/specs/2026-10-22-spotless-p2-fallback-spike-notes.md` (findings, committed)

**Interfaces:**
- Produces, in the notes file, the facts Tasks 3–5 consume:
  - (a) Does `GrEclipseFormatterStep.defaultVersion()` exist and is it public static? Record its value on Java 17.
  - (b) Does `greclipse()` in the Gradle DSL (`GroovyExtension.GrEclipseConfig` / `GroovyGradleExtension`) expose `withP2Mirrors(Map<String,String>)`?
  - (c) The exact call that returns the P2 repo URLs for a version. Expected approach: `GrEclipseFormatterStep.createBuilder(provisioner).model(version).getP2repo()`, called from Groovy, which ignores `protected`. Use a dummy `Provisioner` that throws. Record the URLs for the default version.
  - (d) Offline cache: with a warm `~/.m2/repository/dev/equo/p2-data` and `download.eclipse.org` blocked, does `spotlessGroovyGradleCheck` succeed? Block it with `-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9` or an `/etc/hosts`-style override, whichever the environment allows. If it succeeds, record a cheap, reliable rule for "cache is warm for version X", e.g. a file under `p2-data/queries/` whose name or content identifies the model.
  - (e) Mirror end to end: with `withP2Mirrors` mapping the original prefix to each candidate (`https://ftp.fau.de/eclipse`, `https://ftp.halifax.rwth-aachen.de/eclipse`, `https://mirror.ibcp.fr/pub/eclipse`), plus the jfrog identity mapping, and a cold `p2-data`, does provisioning succeed? Does it avoid `download.eclipse.org` entirely, e.g. via child repos or `p2.mirrorsURL`? Keep the 2–3 working candidates as the built-in list, in order.
  - (f) The validity rule for the Groovy-Eclipse repo URL from (c): which of `p2.index`, `compositeContent.jar`, `content.jar` it serves.

- [ ] **Step 1: Set up the scratch project and answer (a)–(c), (f)** by running small Groovy snippets in a `build.gradle` `tasks.register('probe') { doLast { … } }`.
- [ ] **Step 2: Answer (e)** by running `./gradlew spotlessGroovyGradleCheck --info` once per candidate mirror, with cold `p2-data` (delete it first), and grepping the `--info` output and failures for `download.eclipse.org`.
- [ ] **Step 3: Answer (d)** by warming the cache with one successful run, then blocking the host and running again.
- [ ] **Step 4: Write the notes file** with one section per letter. Each section gives the result, the evidence (command and relevant output lines), and the consequence for the plan.
- [ ] **Step 5: Gate.**
  - If (a), (b) or (c) is "no": stop and report back. The integration approach in Task 4 needs to change.
  - If (e) fails for all candidates: stop and report back. Per the spec, the mirror step is ineffective.
  - If (d) fails: mark Task 5 and the "cache warm" branch of Task 3 as dropped (pass `cacheWarm = false` always), and continue.
- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/specs/2026-10-22-spotless-p2-fallback-spike-notes.md
git commit -m "docs: record spotless P2 spike findings"
```

---

### Task 2: P2 site health check

**Files:**
- Create: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/P2SiteCheck.groovy`
- Test: `convention-plugins/src/test/groovy/to/wetransform/gradle/conventions/P2SiteCheckSpec.groovy`

**Interfaces:**
- Produces:
  - `class P2SiteCheck { P2SiteCheck(int connectTimeoutMs = 3000, int readTimeoutMs = 3000); P2SiteCheck.Result check(String repoUrl) }`. `repoUrl` is a P2 repo URL; a missing trailing `/` is added.
  - `static class Result { final boolean healthy; final String reason }`. `reason` is a short human-readable text, e.g. `"https://…/p2.index: HTTP 200 text/html"`, used in warnings.

Rule: `GET <repoUrl>p2.index`. Healthy if the status is 200, the content type does not start with
`text/html`, and the body contains `metadata.repository.factory.order=`. If `p2.index` returns
404, try `compositeContent.jar` and then `content.jar`. Each is healthy if the status is 200, the
content type is not HTML, and the first two bytes are `PK`. Follow redirects with
`HttpURLConnection` (default proxy selection, so JVM proxy settings apply). Any exception
(timeout, unknown host, TLS) is unhealthy, with the exception message as the reason.

- [ ] **Step 1: Write failing tests** with a JDK `HttpServer` on an ephemeral port, started in `setup()` and stopped in `cleanup()`:
  - `valid p2 index is healthy`: serves `version=1\nmetadata.repository.factory.order=compositeContent.xml,\\!\nartifact.repository.factory.order=compositeArtifacts.xml,\\!\n` as `text/plain` → `healthy`
  - `html page with status 200 is unhealthy`: serves `<html><body>Temporary shutdown</body></html>` as `text/html` for every path → `!healthy`, and `reason.contains('text/html')`
  - `redirect to html page is unhealthy`: `p2.index` returns 302 to `/shutdown.html`, which serves HTML → `!healthy`
  - `index without p2 keys is unhealthy`: 200 `text/plain` `hello` → `!healthy`
  - `missing p2 index falls back to content jar`: `p2.index` returns 404, `content.jar` returns 200 `application/java-archive`, body starting `PK\u0003\u0004` → `healthy`
  - `server error is unhealthy`: 503 → `!healthy`, `reason.contains('503')`
  - `timeout is unhealthy`: the handler sleeps 2 s; `new P2SiteCheck(500, 500)` → `!healthy`, and the call returns in under 1.5 s
  - `unreachable host is unhealthy`: `http://127.0.0.1:<closed port>/` → `!healthy`
- [ ] **Step 2: Run them and confirm they fail.** Run: `./gradlew :convention-plugins:test --tests '*P2SiteCheckSpec'`. Expected: compilation failure (`P2SiteCheck` missing).
- [ ] **Step 3: Implement `P2SiteCheck`** per the rule above.
- [ ] **Step 4: Run the tests and confirm they pass.** Same command. Expected: 8 tests, PASS.
- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/P2SiteCheck.groovy convention-plugins/src/test/groovy/to/wetransform/gradle/conventions/P2SiteCheckSpec.groovy
git commit -m "feat(spotless): add P2 update site health check"
```

---

### Task 3: Decision logic

**Files:**
- Create: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/P2Source.groovy` (value type, with `P2Mode` as a nested enum or a sibling file `P2Mode.groovy`)
- Create: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/P2SourceDecider.groovy`
- Test: `convention-plugins/src/test/groovy/to/wetransform/gradle/conventions/P2SourceDeciderSpec.groovy`

**Interfaces:**
- Consumes: `P2SiteCheck.Result` (Task 2).
- Produces:
  - `enum P2Mode { AUTO, ORIGINAL, MIRROR, SKIP; static P2Mode parse(String value) }`. `null`/blank → `AUTO`. Parsing is case-insensitive. An unknown value throws `IllegalArgumentException` with the message `Invalid value '<v>' for wetf.spotless.p2.mode, expected one of: auto, original, mirror, skip`.
  - `class P2Source { enum Kind { ORIGINAL, MIRROR, SKIP }; final Kind kind; final String mirrorBase /* MIRROR only, no trailing slash */; final String reason; String encode(); static P2Source decode(String) ; Map<String,String> mirrorMap() }`
    - `mirrorMap()` for MIRROR returns `['https://download.eclipse.org/eclipse/updates/': mirrorBase + '/eclipse/updates/', 'https://groovy.jfrog.io/': 'https://groovy.jfrog.io/']`; for other kinds it returns an empty map.
    - `encode`/`decode` round-trip all fields; the format is the implementer's choice.
  - `class P2SourceDecider`:
    - `static final String ORIGINAL_BASE = 'https://download.eclipse.org'`
    - `static final List<String> DEFAULT_MIRRORS`: the ordered list from spike (e).
    - `static List<String> parseMirrors(String property)`: `null`/blank → `DEFAULT_MIRRORS`. Otherwise split on `,`, trim each entry, drop empty entries, and strip trailing `/`. If that leaves nothing, the result is an empty list.
    - `static String updatesUrl(String base, String eclipseVersion)` → `"${base}/eclipse/updates/${eclipseVersion}/"`
    - `P2Source decide(P2Mode mode, String originalBase, List<String> mirrors, String eclipseVersion, String greclipseRepoUrl, boolean cacheWarm, Function<String, P2SiteCheck.Result> check)`

Decision order (from the spec):
- ORIGINAL → ORIGINAL; MIRROR → MIRROR with `mirrors[0]`, no checks (an empty list throws `IllegalArgumentException`); SKIP → SKIP with reason `forced via wetf.spotless.p2.mode=skip`.
- AUTO:
  1. `cacheWarm` → ORIGINAL, with no checks.
  2. If `check(greclipseRepoUrl)` is unhealthy → SKIP, with that reason.
  3. If `check(updatesUrl(originalBase, v))` is healthy → ORIGINAL.
  4. Otherwise use the first mirror whose `check(updatesUrl(mirror, v))` is healthy → MIRROR.
  5. Otherwise → SKIP, with a reason joining all individual reasons with `; `.

- [ ] **Step 1: Write failing tests.** Use a scripted `check` closure that records the URLs it was called with:
  - `forced original does no checks`, `forced skip does no checks`, `forced mirror uses first mirror without checks`
  - `auto with warm cache does no checks` → ORIGINAL, and the recorded calls are empty
  - `auto uses original when healthy` → ORIGINAL; calls == `[greclipseRepoUrl, 'https://download.eclipse.org/eclipse/updates/4.41/']`
  - `auto falls back to first healthy mirror`: the original and `mirrors[0]` are unhealthy, `mirrors[1]` is healthy → MIRROR with `mirrorBase == mirrors[1]`, and `mirrors[2]` is never checked
  - `auto skips when nothing is healthy`: SKIP; the reason contains every individual reason
  - `auto skips when groovy-eclipse site is unhealthy`: SKIP; the original is never checked
  - `mirror map maps original prefix and groovy-eclipse identity`: assert the exact map from the Interfaces block for `mirrorBase = 'https://ftp.fau.de/eclipse'`
  - `encode decode round trip` for each kind
  - `mode parsing`: `null`, `''`, `'Auto'`, `'SKIP'` parse; `'off'` throws with the exact message above
  - `mirror list is normalized`: `' https://a.example/eclipse/ , https://b.example ,'` → `['https://a.example/eclipse', 'https://b.example']`, and `updatesUrl('https://a.example/eclipse', '4.41') == 'https://a.example/eclipse/eclipse/updates/4.41/'`; `null` → `DEFAULT_MIRRORS`; `','` → `[]`
- [ ] **Step 2: Run them and confirm they fail.** Run: `./gradlew :convention-plugins:test --tests '*P2SourceDeciderSpec'`. Expected: compilation failure.
- [ ] **Step 3: Implement `P2Mode`, `P2Source` and `P2SourceDecider`.**
- [ ] **Step 4: Run the tests and confirm they pass.** Same command. Expected: PASS.
- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/P2*.groovy convention-plugins/src/test/groovy/to/wetransform/gradle/conventions/P2SourceDeciderSpec.groovy
git commit -m "feat(spotless): add P2 source decision logic"
```

---

### Task 4: Plugin integration (ValueSource, once per build, mirror/skip, warning, docs)

**Files:**
- Create: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/P2SourceValueSource.groovy`
- Create: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/GrEclipseRepos.groovy`
- Modify: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/SpotlessPlugin.groovy`, around both `greclipse()` calls (`:68-75`, `:92-105`), plus the constants at `:31`
- Modify: `README.md`, in the "Common Gradle properties" (`:82`) and "Configure spotless" (`:151`) sections
- Test: `convention-plugins/src/test/groovy/to/wetransform/gradle/conventions/SpotlessP2FallbackPluginTest.groovy` (extends `PluginTest`)

**Interfaces:**
- Consumes: `P2SiteCheck` (Task 2); `P2Mode`, `P2Source`, `P2SourceDecider` (Task 3); spike facts (a), (b), (c).
- Produces:
  - `class GrEclipseRepos { static String defaultEclipseVersion(); static String greclipseRepoUrl(String eclipseVersion) }`, using the calls recorded in spike (a) and (c). `greclipseRepoUrl` returns the repo URL that does not start with `https://download.eclipse.org/`.
  - `abstract class P2SourceValueSource implements ValueSource<String, Params>`. `interface Params extends ValueSourceParameters { Property<String> getMode(); Property<String> getOriginalBase(); ListProperty<String> getMirrors(); Property<String> getEclipseVersion(); Property<String> getGreclipseRepoUrl(); Property<Boolean> getCacheWarm() }`. `obtain()` returns `new P2SourceDecider().decide(…, new P2SiteCheck()::check).encode()`.
  - In `SpotlessPlugin`:
    - `static final String PROPERTY_P2_MODE = 'wetf.spotless.p2.mode'`
    - `static final String PROPERTY_P2_MIRRORS = 'wetf.spotless.p2.mirrors'`
    - `static final String PROPERTY_P2_ORIGINAL_BASE = 'wetf.spotless.p2.originalBase'`: internal; overrides `P2SourceDecider.ORIGINAL_BASE`, and is used by the tests
    - `static final String PROPERTY_P2_GRECLIPSE_REPO = 'wetf.spotless.p2.greclipseRepo'`: internal; overrides `GrEclipseRepos.greclipseRepoUrl(…)` for the health check only, and is used by the tests
    - `P2Source resolveP2Source(Project project)`: memoized on `rootProject.extensions.extraProperties` under `_spotlessP2Source`, following the `_spotlessWatcherRegistered` pattern at `:250`. The value is read via `project.providers.of(P2SourceValueSource) { … }.get()`. Properties are read through `project.providers.gradleProperty(…)`. `cacheWarm` is `false` until Task 5.
    - `void applyGreclipse(FormatExtension ext, String configProperties, P2Source source)`: for SKIP, adds nothing. For ORIGINAL, `ext.greclipse().configProperties(configProperties)`. For MIRROR, the same plus `.withP2Mirrors(source.mirrorMap())`. Both existing `greclipse()` call sites go through it.

Warning behaviour:
- MIRROR: `logger.lifecycle("Spotless: download.eclipse.org unavailable, using P2 mirror <mirrorBase>")`, once per build.
- SKIP: register on each affected project's `spotlessCheck` and `spotlessApply` tasks a `doFirst` action. It must be configuration-cache safe: implement it as a small `Action<Task>` class holding only the message `String`, not a closure that captures the plugin. The action logs at `warn` level:

```
==========================================================================
WARNING: Groovy and Gradle file formatting was NOT checked (greclipse skipped).
Reason: <reason>
Override with -Pwetf.spotless.p2.mode=original|mirror|skip
and -Pwetf.spotless.p2.mirrors=<comma-separated mirror base URLs>.
==========================================================================
```

  If the `GITHUB_ACTIONS` environment variable is `true` (read at configuration time via `providers.environmentVariable`), the action also logs `::warning title=Spotless::Groovy and Gradle file formatting was NOT checked (greclipse skipped): <reason>`. The same block is also logged once at configuration time.

- [ ] **Step 1: Write failing TestKit tests.** Each starts a local `HttpServer` (reuse a small helper, e.g. a static method in the spec). It points `-Pwetf.spotless.p2.originalBase`, `-Pwetf.spotless.p2.greclipseRepo` and `-Pwetf.spotless.p2.mirrors` at that server, so none of the new tests touch the real network.
  - `forced skip prints warning and does not apply greclipse`: `groovy` plugin; a `src/main/groovy/Test.groovy` with wrong indentation (the content from `GroovySpotlessPluginTest` "fails Groovy formatting error") but correct license header and whitespace; `-Pwetf.spotless.p2.mode=skip` → `spotlessCheck` succeeds, and the output contains `Groovy and Gradle file formatting was NOT checked`
  - `skip still enforces generic settings`: same setup, but the file also has trailing whitespace → `spotlessCheck` fails on `:spotlessGroovyCheck`
  - `auto skips when original serves html and mirrors are unhealthy`: the server returns 200 `text/html` for everything; the original base and the single mirror point at it → `help` succeeds, and the output contains the warning block with reason text containing `text/html`
  - `auto uses mirror when original serves html`: the original path serves HTML, `/mirror/eclipse/updates/<v>/p2.index` serves a valid index, and `wetf.spotless.p2.mirrors=<server>/mirror` → `help` output contains `using P2 mirror <server>/mirror`. `-Pwetf.spotless.p2.greclipseRepo` points at a healthy path on the same server.
  - `health check runs once per build in multi-project build`: root plus two subprojects `a`, `b`, all applying the plugin; the server counts requests per path → after `help`, each `p2.index` path was requested exactly once
  - `skip warning is printed again on configuration cache hit`: forced skip; run `spotlessCheck --configuration-cache` twice. The second output contains `Reusing configuration cache` and the warning block
  - `github actions annotation in skip mode`: `GradleRunner.withEnvironment(System.getenv() + [GITHUB_ACTIONS: 'true'])`, forced skip → output contains `::warning title=Spotless::`
  - `invalid mode fails with clear message`: `-Pwetf.spotless.p2.mode=off` → `buildAndFail`; the output contains `Invalid value 'off' for wetf.spotless.p2.mode`

  `PluginTest.runTask`/`runTaskWithFailure` only take a task name plus args. Extend `runTaskWithFailure` with `String... args`, like `runTask`, and add an overload accepting an environment map, keeping the current callers working.
- [ ] **Step 2: Run them and confirm they fail.** Run: `./gradlew :convention-plugins:test --tests '*SpotlessP2FallbackPluginTest'`. Expected: FAIL (no warning output, unknown property ignored).
- [ ] **Step 3: Implement `GrEclipseRepos`, `P2SourceValueSource` and the `SpotlessPlugin` changes** per the Interfaces block.
- [ ] **Step 4: Run the new tests and the full suite.** Run: `./gradlew :convention-plugins:test`. Expected: all PASS, including the existing `GroovySpotlessPluginTest` and `SpotlessPluginTest`. These use AUTO mode against the real sites; if `download.eclipse.org` is still down, they now pass via a mirror.
- [ ] **Step 5: Document in `README.md`.** Under "Common Gradle properties", add the two public properties with one-line comments. Under "Configure spotless", add a subsection "Eclipse P2 outages" (≤ 15 lines) explaining the auto behaviour, the modes, the mirrors property, and that skip mode prints a warning. The internal properties are not documented.
- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add convention-plugins/src README.md
git commit -m "feat(spotless): fall back to P2 mirror or skip greclipse when Eclipse download site is unavailable"
```

---

### Task 5: Warm-cache fast path (only if spike (d) succeeded)

Skip this task entirely if the spike notes mark (d) as failed.

**Files:**
- Create: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/P2CacheProbe.groovy`
- Modify: `convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/SpotlessPlugin.groovy` (`resolveP2Source`, `cacheWarm` parameter)
- Test: `convention-plugins/src/test/groovy/to/wetransform/gradle/conventions/P2CacheProbeSpec.groovy`

**Interfaces:**
- Consumes: spike fact (d), the "cache is warm" rule; `P2SourceValueSource.Params.getCacheWarm()` (Task 4).
- Produces: `class P2CacheProbe { static File defaultP2DataDir(); static boolean isWarm(File p2DataDir, String eclipseVersion) }`. `defaultP2DataDir()` mirrors Solstice `CacheLocations.p2data()`: `${user.home}/.m2/repository/dev/equo/p2-data/`.

- [ ] **Step 1: Write failing tests** using a `@TempDir` `p2-data` laid out per the spike rule: `warm cache for version is detected`, `cache for other version is not warm`, `missing directory is not warm`, `empty queries directory is not warm`.
- [ ] **Step 2: Run them and confirm they fail.** Run: `./gradlew :convention-plugins:test --tests '*P2CacheProbeSpec'`. Expected: compilation failure.
- [ ] **Step 3: Implement `P2CacheProbe`** per the spike rule, and wire `cacheWarm = P2CacheProbe.isWarm(P2CacheProbe.defaultP2DataDir(), version)` into `resolveP2Source`. Compute it inside `P2SourceValueSource.obtain()`, not as a parameter, so it is re-evaluated on configuration-cache hits; drop `getCacheWarm()` from `Params` accordingly.
- [ ] **Step 4: Run the tests and confirm they pass.** Run: `./gradlew :convention-plugins:test`. Expected: all PASS.
- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add convention-plugins/src
git commit -m "feat(spotless): skip P2 health check when local P2 cache is warm"
```

---

### Task 6: Cache spotless P2 data in shared Gradle workflows (`../gha-workflows`)

Work in `/home/simon/repos/wetf/gha-workflows` on a new branch, e.g. `spotless-p2-cache`.

**Files:**
- Modify: `.github/workflows/gradle-library.yml`, `.github/workflows/gradle-service.yml`. These are the only workflows running `./gradlew`; recheck with `grep -ln gradlew .github/workflows/*.yml`.

**Interfaces:**
- Consumes: the cache location `~/.m2/repository/dev/equo/p2-data`.

Steps to insert in each workflow:
1. Directly after `Setup Gradle`: `actions/cache/restore` with
   - `path: ~/.m2/repository/dev/equo/p2-data`
   - `key: spotless-p2-${{ runner.os }}-${{ hashFiles('**/*.gradle', '**/*.gradle.kts', 'gradle/*.versions.toml', 'mise.toml', '.java-version') }}`
   - `restore-keys: spotless-p2-${{ runner.os }}-`
   - `id: spotless-p2-cache`
2. After the build step, if `success() && steps.spotless-p2-cache.outputs.cache-hit != 'true'`: a shell step that sets output `has-data=true` only if `~/.m2/repository/dev/equo/p2-data/queries` exists and is non-empty.
3. If that output is `true`: `actions/cache/save` with the same `path` and `key`.

Use the same `actions/cache` release for `restore` and `save`. Check `gh api repos/actions/cache/releases/latest`, and pin by SHA. `mise.yml` currently pins `55cc8345…` (v6.1.0); update it only if a newer release exists, and only in the new steps.

- [ ] **Step 1: Add the steps to both workflows.**
- [ ] **Step 2: Validate the syntax.** Run: `mise x -- hk check --all` (the repo's pre-commit setup), or `actionlint` if hk doesn't include it. Expected: no errors.
- [ ] **Step 3: Commit** in `gha-workflows`:

```bash
git -C /home/simon/repos/wetf/gha-workflows add .github/workflows/gradle-library.yml .github/workflows/gradle-service.yml
git -C /home/simon/repos/wetf/gha-workflows commit -m "feat: cache spotless P2 data in Gradle workflows"
```

- [ ] **Step 4: Verify end to end.** Ask your human partner to run a consuming repo's workflow against the branch twice (pushing is their call). Expected: the second run's restore step reports a cache hit. With Task 5 done, its build log contains no `download.eclipse.org` / mirror lifecycle line.
