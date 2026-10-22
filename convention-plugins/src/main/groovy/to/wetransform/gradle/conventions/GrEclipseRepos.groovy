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
import java.util.function.Supplier

import com.diffplug.spotless.Provisioner
import com.diffplug.spotless.extra.P2Provisioner
import com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep

/** Access to the P2 repositories spotless uses for the Groovy-Eclipse formatter. */
class GrEclipseRepos {

  /** Eclipse version and Groovy-Eclipse repository for the health check, or the failure to determine them. */
  static class Info {
    final String eclipseVersion
    final String greclipseRepoUrl
    /** Reason if the lookup failed, otherwise null. */
    final String failure

    Info(String eclipseVersion, String greclipseRepoUrl, String failure) {
      this.eclipseVersion = eclipseVersion
      this.greclipseRepoUrl = greclipseRepoUrl
      this.failure = failure
    }
  }

  /** Looks up version and repository via spotless internals, without failing if they changed. */
  static Info lookup(String greclipseRepoOverride) {
    lookup(greclipseRepoOverride,
      { defaultEclipseVersion() } as Supplier<String>,
      { String v -> greclipseRepoUrl(v) } as Function<String, String>)
  }

  static Info lookup(String greclipseRepoOverride, Supplier<String> versionLookup, Function<String, String> repoLookup) {
    try {
      String version = versionLookup.get()
      String url = greclipseRepoOverride ?: repoLookup.apply(version)
      return new Info(version, url, null)
    } catch (Exception | LinkageError e) {
      // e.g. NoSuchMethodError or MissingMethodException after a spotless update
      // toString() as the message may be null
      return new Info(null, null, "could not determine Groovy-Eclipse P2 repositories (${e})".toString())
    }
  }

  static String defaultEclipseVersion() {
    GrEclipseFormatterStep.defaultVersion()
  }

  /** The P2 model (dev.equo.solstice.p2.P2Model) of the Groovy-Eclipse formatter, without mirrors. */
  private static Object model(String eclipseVersion) {
    Provisioner provisioner = { boolean withTransitives, Collection<String> mavenCoordinates ->
      throw new IllegalStateException('Provisioning is not supported here')
    } as Provisioner
    def builder = GrEclipseFormatterStep.createBuilder(provisioner, P2Provisioner.createDefault())
    // model(String) is protected, Groovy allows to call it dynamically
    builder.model(eclipseVersion)
  }

  /** All P2 repository URLs of the Groovy-Eclipse formatter for the given Eclipse version. */
  static List<String> repoUrls(String eclipseVersion) {
    model(eclipseVersion).getP2repo() as List<String>
  }

  /**
   * Name of the Solstice query cache directory for the formatter with the given mirrors applied,
   * as spotless and Solstice compute it ({@code EquoBasedStepBuilder.createModelWithMirrors}, {@code QueryCacheOnDisk}).
   */
  static String queryCacheKey(String eclipseVersion, Map<String, String> mirrorMap) {
    def model = model(eclipseVersion)
    Collection<String> repos = model.getP2repo()
    List<String> mirrored = P2CacheProbe.applyMirrors(repos, mirrorMap)
    repos.clear()
    repos.addAll(mirrored)
    // the version of the Solstice library actually used, as part of the cache key
    def nestedJars = Class.forName('dev.equo.solstice.NestedJars', true, model.getClass().classLoader)
    P2CacheProbe.queryKey(nestedJars.solsticeVersion() as String, model.hashCode())
  }

  /** The Groovy-Eclipse repository URL, i.e. the one not hosted on download.eclipse.org. */
  static String greclipseRepoUrl(String eclipseVersion) {
    List<String> urls = repoUrls(eclipseVersion)
    String url = urls.find { !it.startsWith('https://download.eclipse.org/') }
    if (url == null) {
      throw new IllegalStateException("No Groovy-Eclipse P2 repository found in ${urls}")
    }
    url
  }
}
