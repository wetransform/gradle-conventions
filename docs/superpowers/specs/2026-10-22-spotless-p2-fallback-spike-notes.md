# Spotless P2 fallback - spike notes

Date: 2026-10-22. Scratch project: `p2-spike` in the session scratchpad, applying `com.diffplug.spotless` 8.10.4
with `groovyGradle { greclipse() }`, Gradle 9.8.1, Java 17 (Temurin 17.0.20).
Resolved libs: `spotless-lib-extra` 4.10.4 (contains `GrEclipseFormatterStep`, `EquoBasedStepBuilder`).

Environment limits: the sandbox proxy returns `403 ERR_ACCESS_DENIED` for `ftp.fau.de`, `ftp.halifax.rwth-aachen.de`
and `mirror.ibcp.fr` (and other mirrors tried). So (e) could only be verified against a local simulated mirror, see (e).
`download.eclipse.org` is still in outage: `p2.index` returns an HTML "Temporary shutdown" page (HTTP 200, `text/html`).

## (a) `GrEclipseFormatterStep.defaultVersion()`

- Result: exists, `public static String`. Value on Java 17 with spotless 8.10.4: **`4.41`**.
- Evidence: reflection in a Gradle task: `PROBE a: public static=public static value=4.41`;
  `javap`: `public static java.lang.String defaultVersion();` in `com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep`.
- Consequence: Task 3/4 can call `GrEclipseFormatterStep.defaultVersion()` (the class is on the build classpath of the
  spotless plugin, i.e. `com.diffplug.spotless.extra.groovy`). Note the real signature of `createBuilder` has two params (see (c)).
- Addendum: `defaultVersion()` depends on the JVM; `spotless-lib-extra` 4.10.4 maps Java 17 to `4.36` and Java 21 to `4.41` (`javap -c`, static initializer of `GrEclipseFormatterStep`).
  The plugin tests (Temurin 17.0.20) observe `4.36`; the scratch runs that logged their JVM (`--info`) ran the Gradle daemon on Temurin 21.0.12, not Java 17 as stated above, hence `4.41`.

## (b) `withP2Mirrors(Map<String,String>)` on the Gradle DSL

- Result: yes. `greclipse()` returns `com.diffplug.gradle.spotless.BaseGroovyExtension$GrEclipseConfig` (shared by
  `GroovyExtension` and `GroovyGradleExtension`, which extends `BaseGroovyExtension`). Public methods:
  `configFile`, `configProperties`, `configXml`, `withP2Mirrors(Map<String,String>)`, `cacheDirectory(Object)`.
- Evidence: `javap -public 'com.diffplug.gradle.spotless.BaseGroovyExtension$GrEclipseConfig'`.
  `.withP2Mirrors(map)` was used successfully in `build.gradle` for all runs below.
- Consequence: the inner class is NOT `GroovyExtension.GrEclipseConfig`; any reflection or typing must use
  `BaseGroovyExtension.GrEclipseConfig`. Plain Groovy DSL calls `greclipse().withP2Mirrors(...)` work unchanged.
- Caveat: as soon as `withP2Mirrors` is used, EVERY P2 repo needs a prefix mapping, including the jfrog one
  (identity mapping `jfrog -> jfrog` is fine). Evidence of failure without it:
  `no mirror configured for P2 repository: https://groovy.jfrog.io/artifactory/plugins-release/org/codehaus/groovy/groovy-eclipse-integration/6.3.0/e4.41/`.
- Caveat 2: `cacheDirectory(...)` did NOT move the P2 cache (`p2-data`), which stays under the Gradle user home (see (d)).

## (c) P2 repo URLs for a version

- Result: works as expected, from Groovy, ignoring `protected`:
  ```groovy
  def cls = Class.forName('com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep')
  def prov = { a, b -> throw new IllegalStateException('no provisioner') } as com.diffplug.spotless.Provisioner
  def b = cls.createBuilder(prov, com.diffplug.spotless.extra.P2Provisioner.createDefault())  // TWO args
  def model = b.model(cls.defaultVersion())   // protected EquoBasedStepBuilder.model(String) -> dev.equo.solstice.p2.P2Model
  model.getP2repo()
  ```
- Difference to the brief: `createBuilder` takes `(Provisioner, P2Provisioner)`, so pass `P2Provisioner.createDefault()` as second arg
  (the dummy `Provisioner` is never called). Nothing is downloaded by building the model.
- Evidence: `PROBE c p2repo=[https://download.eclipse.org/eclipse/updates/4.41/, https://groovy.jfrog.io/artifactory/plugins-release/org/codehaus/groovy/groovy-eclipse-integration/6.3.0/e4.41/]`
- Consequence: Task 4 can derive the URL list per version this way. The Eclipse platform repo is the first entry
  (URL prefix `https://download.eclipse.org/eclipse/updates/`), the Groovy-Eclipse repo the second (prefix
  `https://groovy.jfrog.io/artifactory/plugins-release`). `model.hashCode()` is also available (see (d)).

## (d) Offline cache

- Result: YES, a warm cache lets `spotlessGroovyGradleCheck` succeed with no network, but with caveats:
  1. Location is `<GRADLE_USER_HOME>/caches/p2-data` (e.g. `~/.gradle/caches/p2-data`), NOT `~/.m2/repository/dev/equo/p2-data`
     (that is a legacy location holding older data). `greclipse().cacheDirectory()` does not change it.
  2. The cache is keyed by the exact URL set (mirror prefixes included). A cache warmed through mirror URLs
     is not used when the default `download.eclipse.org` URLs are configured, and vice versa.
  3. I could not warm the cache with the real default URLs because `download.eclipse.org/.../p2.index` is broken today. The warm/offline
     test therefore used the simulated mirror URLs (`http://127.0.0.1:8094/...`) for both runs. The mechanism is URL-independent.
- Evidence: warm run (cold GUH, mirror up) printed formatting violations (i.e. formatter ran), 43 requests to the mirror.
  Then the mirror was dead (connection refused) and the JVM proxy pointed to a dead port (`-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9`),
  `./gradlew --offline spotlessGroovyGradleCheck` ran in 0.5s with the same violations report and no further mirror requests (request log unchanged).
  The same project with default URLs (no mirror) against the mirror-warmed cache failed:
  `Failed to provision P2 dependencies > java.net.ConnectException: Failed to connect to /127.0.0.1:9`.
- Cheap "cache warm for version X" rule (verified by experiment): the directory `<GUH>/caches/p2-data/queries/1.8.2<H>/` exists and contains a `content` file, where
  `1.8.2` is the literal prefix observed in every run (8 directories across runs, never any other prefix; it is probably Solstice's cache version, so treat as a constant that may change) and
  `H = String.valueOf(model.hashCode())` (Java int, may be negative, e.g. `-1606393269`) computed on the P2Model AFTER the mirror prefixes are applied to its repo URLs (the MIRRORED model).
  So the dir name is exactly `"1.8.2" + H`, e.g. `queries/1.8.2-911005171`. Verified: a manually mirrored model (repos rewritten to `http://127.0.0.1:8094/...`) had hash `-911005171` and
  matched dir `queries/1.8.2-911005171`. The unmirrored default model of 4.41 has `hashCode()` `-1606393269`, so its warm dir would be `queries/1.8.2-1606393269`
  (not observable today, no real warm run is possible). Since the prefix is not guaranteed, a robust variant is: any `queries/*<H>/content` whose dir name ends with `H`.
  Supporting files: `bundle-pool/<url-munged-repo>/*.jar`, `metadata/offline/*`, `nested-jars/`.
- Consequence: the "cache warm" branch (Task 3/5) is feasible but must compute the hash for the URL set it is about to use, and cannot assume that a cache
  warmed from `download.eclipse.org` helps a mirror configuration. Since only a cache from a prior successful run with the default
  URLs helps in the "default URLs, p2.index broken" case, a "warm" check should be done on the default (unmirrored) URL set first, and
  on each mirror URL set second. Not verified with the real default URLs (see 3.). Gate: (d) is "yes", so Task 5 is NOT dropped on this evidence alone;
  the caveat is that real-URL warmness is unverified.

## (e) Mirror end to end

- Result: UNVERIFIED for the three real candidates (blocked), VERIFIED for the mechanism with a local simulated mirror.
- Evidence, real candidates: `curl -x http://localhost:3128 https://ftp.fau.de/` -> `HTTP/1.1 403 Forbidden ... X-Squid-Error: ERR_ACCESS_DENIED`; the same for
  `ftp.halifax.rwth-aachen.de` and `mirror.ibcp.fr`. Domains the user needs to allow to finish this: `ftp.fau.de`, `ftp.halifax.rwth-aachen.de`, `mirror.ibcp.fr`.
  `archive.eclipse.org` is allowed but is the same infrastructure and returns the same shutdown page, so it is no mirror.
- Evidence, mechanism: a local HTTP server on `127.0.0.1` forwards to `download.eclipse.org` / `groovy.jfrog.io` (it serves a hand-written `p2.index` per repo, converts `*.xml` requests to the upstream `*.jar`), see the worked example below.
  Earlier runs (warm proxy, default `GRADLE_USER_HOME`/`user.home` variations) already succeeded cold with 43 mirror requests and formatter output (format violations of the scratch `build.gradle`).
  Request sequence: `<mirror>/eclipse/updates/4.41/p2.index` -> `compositeContent.xml(.xz|.jar)` -> child `R-4.41-202608281142/p2.index` -> `content.xml.xz`
  -> `plugins/*.jar`. The composite child location is relative (`R-4.41-202608281142`), so no `p2.mirrorsURL`/absolute URL rewriting is needed.
- Evidence, `download.eclipse.org` is not contacted on the cold path (re-run for this review). Setup: copy of the warmed scratch `GRADLE_USER_HOME` (Maven deps, plugin, wrapper) with only
  `caches/p2-data` deleted, fresh `user.home`, `./gradlew --offline`, daemon jvmargs `-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=9`
  (dead proxy; `127.0.0.1` stays direct via `-Dhttp.nonProxyHosts=localhost|127.0.0.1` from the environment's `JAVA_TOOL_OPTIONS`, and the run succeeding against the loopback mirror confirms that).
  Command: `GRADLE_USER_HOME=<scratch>/guh2 ./gradlew --offline spotlessGroovyGradleCheck --info -PmirrorBase=http://127.0.0.1:8092/eclipse -PmirrorJ=http://127.0.0.1:8092/jfrog`.
  Outcome: provisioning succeeded (formatter ran, format violations reported), 43 requests to the fake mirror, 0 occurrences of `Failed to connect`, new `queries/1.8.2-575479663/`.
  The only `download.eclipse.org` string in the `--info` output is the scratch `build.gradle` text inside the violations diff. The fake mirror's own upstream fetches (python, outside the dead proxy) worked.
  Control (proves the dead proxy catches direct access): same setup with cold `p2-data` and NO mirror properties (default URLs) failed with `Failed to connect to /127.0.0.1:9`.
  Caveat: the fake mirror itself fetched from `download.eclipse.org`, so this shows Gradle/Solstice does not contact it directly, not that real mirrors are healthy.
- Requirements a real mirror must meet (derived from the simulation): serve `p2.index` (real one, not an HTML page) at
  `<mirror>/eclipse/updates/<ver>/p2.index`, `compositeContent.xml(.xz)` or `.jar` listed by it, and the child `R-<ver>-<ts>/` repo with `content.xml.xz`.
  Stock Eclipse mirrors (`/eclipse/updates/<ver>/`) do this normally.
- Extra finding: during today's outage only `p2.index` (and HTML pages) are broken on `download.eclipse.org`; `compositeContent.jar`, child `content.jar`,
  `artifacts.jar` and `plugins/*.jar` are served normally (HTTP 200, java-archive). Spotless still fails because Solstice needs `p2.index`.
- Worked example (convention used by the plan: key `https://download.eclipse.org/eclipse/updates/` -> value `<mirrorBase>/eclipse/updates/`). Exact map used in the final cold run
  (`build.gradle`: `greclipse().withP2Mirrors(mirrors)`), with `mirrorBase = http://127.0.0.1:8092/eclipse`:
  ```groovy
  [ 'https://download.eclipse.org/eclipse/updates/': 'http://127.0.0.1:8092/eclipse/eclipse/updates/',
    'https://groovy.jfrog.io/artifactory/plugins-release': 'http://127.0.0.1:8092/jfrog' ]   // jfrog also routed via the local fake; the plan's identity mapping value would be the same URL as the key
  ```
  Resulting requests (from the fake mirror's log): `GET /eclipse/eclipse/updates/4.41/p2.index`, `.../compositeContent.xml.xz`, `.../compositeContent.jar`,
  `.../R-4.41-202608281142/p2.index`, `.../R-4.41-202608281142/content.xml.xz`, `.../R-4.41-202608281142/plugins/*.jar`, `GET /jfrog/org/codehaus/groovy/groovy-eclipse-integration/6.3.0/e4.41/p2.index`, `.../content.xml.xz`.
  For ftp.fau.de the analogous map value would be `https://ftp.fau.de/eclipse/eclipse/updates/`, giving the request `https://ftp.fau.de/eclipse/eclipse/updates/4.41/p2.index`.
  The REAL mirrors' directory layout is UNVERIFIED: the proxy answers 403 (`ERR_ACCESS_DENIED`) for them, which says nothing about their layout. It must be confirmed with curl once allowed.
  (Earlier runs used the shorter key `https://download.eclipse.org` with value `http://127.0.0.1:P/eclipse`, which produces the same request URLs.)
- Consequence: keep all three candidates in the built-in list in the given order (`ftp.fau.de`, `ftp.halifax.rwth-aachen.de`, `mirror.ibcp.fr`) as the plan
  has it; their validity must be confirmed once the proxy allows them (re-run `curl -s -o /dev/null -w '%{http_code}' <base>/eclipse/updates/4.41/p2.index`
  and expect 200 with `text/plain`-ish content, not `text/html`). The health check should therefore test `p2.index` content, not just the HTTP status
  (the outage page is status 200 `text/html`).

## (f) Validity rule for the Groovy-Eclipse (jfrog) repo URL

- Result: the jfrog repo is a plain (non composite) repo. Responses for
  `https://groovy.jfrog.io/artifactory/plugins-release/org/codehaus/groovy/groovy-eclipse-integration/6.3.0/e4.41/`:
  | file | result |
  | --- | --- |
  | `p2.index` | 200 `application/octet-stream`, 176 bytes, valid index (`artifact.repository.factory.order=artifacts.xml.xz,artifacts.xml,\!`) |
  | `compositeContent.jar` | 404 (JSON error) |
  | `content.jar` | 200 `application/java-archive`, 25391 bytes |
  | `content.xml.xz` | 200 `application/x-xz` |
  | `artifacts.jar` | 200 `application/java-archive` |
- For comparison `download.eclipse.org/eclipse/updates/4.41/`: `p2.index` 200 `text/html` (outage page, 7949 bytes), `compositeContent.jar` 200 java-archive (431 bytes),
  `content.jar` 404.
- Rule: a Groovy-Eclipse repo URL is valid if `<url>p2.index` returns HTTP 200 with a non-HTML body and `<url>content.xml.xz` or `content.jar` returns 200.
  `compositeContent.jar` is NOT served (404), so do not require it. Real `p2.index` of the jfrog repo (fetched with curl today), verbatim:
  ```
  #Wed Sep 30 08:25:33 CDT 2026
  artifact.repository.factory.order=artifacts.xml.xz,artifacts.xml,\!
  metadata.repository.factory.order=content.xml.xz,content.xml,\!
  version=1
  ```
  So the jfrog `p2.index` DOES contain `metadata.repository.factory.order=`. For `download.eclipse.org` (composite parent `updates/4.41/` and child `R-4.41-.../`) I could NOT see a real `p2.index`:
  it returns the HTML outage page today, so whether it contains that key is unverified (the fake mirror's `p2.index` files were hand-written by me and prove nothing about it).
  Safer rule for the health check: status 200, `Content-Type` not `text/html`, body does not contain `<html`/`<!DOCTYPE`, and contains `version=1` or `factory.order=`.
- Consequence: for the health check, test `p2.index` for a non-HTML, non-empty `.index` payload (check `Content-Type` is not `text/html` or body does not contain `<html`).
  Do not use `compositeContent.jar` as the signal for the jfrog repo.
