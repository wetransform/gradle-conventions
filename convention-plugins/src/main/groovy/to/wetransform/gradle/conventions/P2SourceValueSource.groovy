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

    Property<Boolean> getCacheWarm()
  }

  @Override
  String obtain() {
    Params p = getParameters()
    P2SiteCheck siteCheck = new P2SiteCheck()
    new P2SourceDecider().decide(
      P2Mode.parse(p.mode.getOrNull()),
      p.originalBase.get(),
      p.mirrors.get(),
      p.eclipseVersion.getOrElse(''),
      p.greclipseRepoUrl.getOrElse(''),
      p.cacheWarm.getOrElse(false),
      { String url -> siteCheck.check(url) } as Function<String, P2SiteCheck.Result>
      ).encode()
  }
}
