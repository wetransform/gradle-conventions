# Spotless: surviving `download.eclipse.org` outages

Date: 2026-10-22
Repos: `gradle-conventions` (this repo), `gha-workflows` (`../gha-workflows`)

**Erratum (spike finding):** the spotless Gradle plugin keeps the P2 cache in `<GRADLE_USER_HOME>/caches/p2-data`, not `~/.m2/repository/dev/equo/p2-data`; the `gha-workflows` cache change was therefore dropped, since `setup-gradle` already caches `~/.gradle/caches` (to be verified on CI).

## Why

`download.eclipse.org` has been unavailable for a long time. It does not time out. Instead it answers
every request (e.g. `https://download.eclipse.org/eclipse/updates/4.36/p2.index`) with an HTML
"temporary shutdown" page. Spotless can't provision the Groovy-Eclipse formatter, the spotless check
tasks fail, and because `check` fails early, tests and other checks never run. Many builds across many
repos are blocked by something that has nothing to do with the code.

This plugin is applied to many projects, so the fix belongs here first, with supporting changes in the
shared GitHub workflows.

### Where the dependency comes from

- Spotless 8.10.0+ (we are on 8.10.4) ships embedded lockfiles for Eclipse JDT 4.9–4.41, so the Java
  `eclipse()` step resolves from Maven Central and **no longer needs P2**.
- `greclipse()` (Groovy-Eclipse) has no lockfile and still provisions through P2. It uses two update sites
  (see spotless `GrEclipseFormatterStep.model()` / `EquoBasedStepBuilder.addPlatformRepo`):
  - `https://download.eclipse.org/eclipse/updates/<eclipseVersion>/`, which is the broken one
  - `https://groovy.jfrog.io/artifactory/plugins-release/org/codehaus/groovy/groovy-eclipse-integration/<greclipseVersion>/e<eclipseVersion>/`, currently unaffected (but it is checked too, see below)
- `SpotlessPlugin` applies `greclipse()` to `*.gradle` files in **every** project (`groovyGradle` block),
  and to Groovy sources when the Groovy plugin is present. So every consumer is affected, even pure
  Java/Kotlin projects.
- Spotless/Solstice caches P2 data under `~/.m2/repository/dev/equo/p2-data/` (`queries`, `metadata`,
  `bundle-pool`, `nested-jars`; fallback `$GRADLE_USER_HOME/caches/p2-data`). `setup-gradle` in our
  workflows only caches `~/.gradle`, so every CI run starts cold and goes to Eclipse.

### Why `withP2Mirrors` alone doesn't help

`withP2Mirrors` rewrites URL prefixes before anything is fetched. Each prefix maps to exactly one
replacement, and the original is never tried. Also, once any mirror is configured, a P2 repo URL with
no matching prefix fails with `no mirror configured for P2 repository: …`. So the map must also cover
the Groovy-Eclipse site, with an identity mapping. A fallback therefore has to be decided at
configuration time.

## Goals

1. When `download.eclipse.org` is down or serving garbage, builds of consuming projects behave as if
   nothing happened, or close to it, without editing each consuming repo. A plugin release, picked up
   via Renovate, plus a workflow update is enough.
2. Formatting stays enforced whenever it can be checked at all.
3. Works in the shared workflows (main case), in other CI setups (a few), and locally (including commit
   hooks).
4. Warm CI runs (cache restored in the same repo) don't contact Eclipse at all.

## Non-goals

- Making real formatting violations non-blocking. If formatting is wrong, failing early before tests is
  desired, because it saves compute and has to be fixed anyway. No `--continue` in the workflows.
- An Artifactory P2 proxy. Public mirrors come first. The override (below) accepts any base URL, so an
  anonymous proxy could be used later without a plugin release. *(Claude's call:)* mirrors that need
  credentials are out of scope. If an authenticated proxy is wanted later, that is a follow-up and may
  need a plugin release.
- A mirror for the Groovy-Eclipse site (`groovy.jfrog.io`). It is health-checked, but has no fallback
  beyond skipping.
- Changing the Java formatter setup (already Maven Central based).

## Anti-goals

- Formatting silently not being checked while everyone believes it is.
- Skipping the check when the tooling is actually available and the formatting is just wrong.
- Noticeably slowing down local builds and commit hooks.

## Design: `gradle-conventions`

### Decision order (per build, at configuration time)

Applies only to the `greclipse()` steps (`groovyGradle` and `groovy`):

1. **Local cache is sufficient:** if the Solstice P2 cache already contains what spotless needs for the
   greclipse version in use, configure as today and do no network check. This keeps local builds and
   warm CI fast. See "Must verify first".
2. **Original is healthy:** configure as today, with no mirror.
3. **A mirror is healthy:** apply `withP2Mirrors` with
   `https://download.eclipse.org/eclipse/updates/` → `<mirror>/eclipse/updates/`, plus an identity
   mapping for the Groovy-Eclipse site prefix `https://groovy.jfrog.io/`. Use the first healthy mirror
   from a short built-in list of public Eclipse mirrors (e.g. `https://ftp.fau.de/eclipse`; the builder
   picks 2–3 that actually carry `eclipse/updates/<version>/`).
4. **Nothing is reachable** (no healthy Eclipse source, **or** the Groovy-Eclipse site is unhealthy): don't add the `greclipse()` step. Keep all other steps in those blocks
   (generic whitespace/newline/indent settings, license header, import order) and all other formats
   (Java, Kotlin, Scala, Markdown). Print a **loud warning** that Groovy/Gradle-file formatting was NOT
   checked, and why.

### Health check

"Healthy" means a real P2 repository answers, not merely that an HTTP response arrives:

- `GET <site>/eclipse/updates/<version>/p2.index`
- HTTP 200, the content type is not `text/html`, and the body looks like a P2 index (Java properties
  containing `version=` and `metadata.repository.factory.order` / `artifact.repository.factory.order`).
- *(Claude's call:)* the Groovy-Eclipse site is checked as well (steps 2–3 require it to be healthy). It
  is a P2 repo too. The builder picks the validity rule for its layout (e.g. `p2.index` or
  `content.jar`/`compositeContent` present, not HTML). If it is unhealthy, the result is step 4.
- Short connect/read timeouts (around 2–3 s each), so a down host costs a few seconds and a healthy one
  well under a second.
- Must go through the JVM's configured HTTP proxy settings (CI and the sandbox use proxies).

### Running once per build

The plugin is applied per project. The decision must be made at most once per build (shared via the
root project or a shared build service), not once per subproject.

It must work with the configuration cache. The network check is an external input. Model it, for
example, as a Gradle `ValueSource`, so a cached configuration doesn't freeze an old "skip" or
"mirror" decision forever. The builder decides the exact mechanism.

### Which version to check

The P2 URL depends on the greclipse/Eclipse version spotless will actually use. Today the plugin doesn't
pin it, so it follows spotless's default, which depends on the JVM: e.g. 4.41 on Java 21, 4.36 on Java 17.
The check must use the same version. Derive it from spotless's own default rather than hard-coding or
pinning it, so Renovate bumps of spotless keep working.

### Overrides

Gradle properties, following the existing `wetf.spotless.*` naming (`wetf.spotless.autoWatcher`). They can
also be set via `ORG_GRADLE_PROJECT_…` env vars in CI:

- `wetf.spotless.p2.mode` = `auto` (default, the order above) | `original` (no checks, today's behaviour) |
  `mirror` (always use the mirror, no checks) | `skip` (skip greclipse with the loud warning)
- `wetf.spotless.p2.mirrors` = comma-separated mirror base URLs, replacing the built-in list (e.g. a future
  Artifactory P2 proxy)

### Loud warning

- A `logger.warn` at configuration time with a clear, multi-line message: what was skipped, why
  (original and mirror results), and how to override.
- When running under GitHub Actions (`GITHUB_ACTIONS=true`), also emit a `::warning::` workflow command, so
  the run shows an annotation.
- Also log at lifecycle level which path was taken when a mirror is used. Being on a mirror should be
  visible, but it isn't a warning-level event.

### Tests

The existing Spock/TestKit setup (`*SpotlessPluginTest`). Health check and decision logic are tested against a
local HTTP server that serves: a valid `p2.index`, an HTML shutdown page with 200, an error status, and
a hang (timeout). The decision order and overrides are tested without real network access. Tests that
need real P2 provisioning (existing Groovy tests) stay as they are.

## Design: `gha-workflows`

- In the Gradle workflows that run spotless (`gradle-library.yml`, `gradle-service.yml`, and any other
  workflow that runs Gradle builds using these conventions; the builder checks), cache
  `~/.m2/repository/dev/equo/p2-data` with `actions/cache` (latest release, pinned by SHA like the
  other actions in the repo).
- The cache must not be poisoned: only data from successful provisioning should end up in it. Spotless
  only writes successful results, but the builder verifies this.
- *(Claude's call:)* the cache is per repository. That is the only option with `actions/cache`, which
  can't share caches across repos. So the first run per repo, and runs on new branches without a base
  cache, are cold and rely on the fallback logic. That is accepted, and nothing like an Artifactory or
  runner-baked cache is built.
- Key scheme left to the builder. Use `restore-keys`, so a run can pick up an older cache from the same repo.
- No change to `--continue` / task ordering (see non-goals).

## Must verify first (spike before building)

1. **Does spotless use the P2 cache offline?** With a warm `p2-data` folder and
   `download.eclipse.org` unreachable or returning HTML, does `spotlessGroovyGradleCheck` succeed? Or does
   Solstice still query the update site (e.g. a freshness check on the `queries` cache)? This
   decides whether step 1 of the decision order and the CI cache actually work, and how "cache is
   sufficient" can be detected cheaply (e.g. presence of a query cache entry for the model).
2. **Does `withP2Mirrors` plus a public mirror work end to end today?** The Eclipse updates repo
   for a version is a composite repo, and its children or `p2.mirrorsURL` may still point to
   `download.eclipse.org`. Confirm that provisioning via the mirror doesn't reach the original host.
   If it does, the mirror path needs more prefix mappings, or a mirror that is self-contained.

If (1) fails, drop the cache fast path and the CI cache item, and always do the health check, with short
timeouts. If (2) fails for all public mirrors, the mirror step is ineffective, and the outcome is "skip
with loud warning" until an Artifactory proxy exists. Report back before building in that case.

## Left to the builder

- Exact mirror list, timeouts, property parsing, warning wording.
- Mechanism for once-per-build and configuration-cache compatibility.
- Cache key scheme in the workflows.
- How "cache is sufficient" is detected, based on spike result (1).

## Delivery

- `gradle-conventions`: `feat(spotless): …` commit(s), so a release is created and Renovate rolls it out.
- `gha-workflows`: separate `feat:` / `fix:` commit(s) in that repo.
