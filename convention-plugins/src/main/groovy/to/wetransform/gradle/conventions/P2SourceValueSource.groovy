/*
 * Copyright 2026 wetransform GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package to.wetransform.gradle.conventions

import java.util.function.Function
import java.util.function.Predicate

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Decides which P2 source to use for the Groovy-Eclipse formatter.
 * As a ValueSource it is re-evaluated by Gradle on configuration cache hits.
 * The result is the {@link P2Source#encode() encoded} {@link P2Source}.
 */
abstract class P2SourceValueSource implements ValueSource<String, Params> {

  interface Params extends ValueSourceParameters {
    Property<String> getMode()

    Property<String> getOriginalBase()

    ListProperty<String> getMirrors()

    Property<String> getEclipseVersion()

    Property<String> getGreclipseRepoUrl()

    /** P2 data directory of spotless; if not set, the local cache is not probed. */
    Property<String> getP2DataDir()

    /** Whether Gradle runs with --offline; then no site is checked. */
    Property<Boolean> getOffline()
  }

  @Override
  String obtain() {
    Params p = getParameters()
    P2Mode mode = P2Mode.parse(p.mode.getOrNull())
    if (mode == P2Mode.AUTO && p.p2DataDir.isPresent()) {
      // a warm cache needs no network access
      File p2DataDir = new File(p.p2DataDir.get())
      String version = p.eclipseVersion.get()
      P2Source cached = P2CacheProbe.fromWarmCache(p.mirrors.get(), { Map<String, String> mirrorMap ->
        P2CacheProbe.isWarm(p2DataDir, GrEclipseRepos.queryCacheKey(version, mirrorMap))
      } as Predicate<Map<String, String>>)
      if (cached != null) {
        return cached.encode()
      }
    }
    if (mode == P2Mode.AUTO && p.offline.getOrElse(false)) {
      return P2Source.skip('offline and P2 cache is cold').encode()
    }
    P2SiteCheck siteCheck = new P2SiteCheck()
    new P2SourceDecider().decide(
      mode,
      p.originalBase.get(),
      p.mirrors.get(),
      p.eclipseVersion.getOrElse(''),
      p.greclipseRepoUrl.getOrElse(''),
      false,
      { String url -> siteCheck.check(url) } as Function<String, P2SiteCheck.Result>
      ).encode()
  }
}
