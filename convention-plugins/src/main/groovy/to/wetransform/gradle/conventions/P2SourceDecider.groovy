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

/** Pure logic choosing between the original P2 site, a mirror, or skipping the formatter. */
class P2SourceDecider {

  static final String ORIGINAL_BASE = 'https://download.eclipse.org'

  static final List<String> DEFAULT_MIRRORS = [
    'https://ftp.fau.de/eclipse',
    'https://ftp.halifax.rwth-aachen.de/eclipse',
    'https://mirror.ibcp.fr/pub/eclipse'
  ].asImmutable()

  static List<String> parseMirrors(String property) {
    if (property == null || property.trim().isEmpty()) {
      return DEFAULT_MIRRORS
    }
    property.split(',').collect { it.trim() }.findAll { !it.isEmpty() }.collect {
      String s = it
      while (s.endsWith('/')) {
        s = s.substring(0, s.length() - 1)
      }
      s
    }.findAll { !it.isEmpty() } as List<String>
  }

  static String updatesUrl(String base, String eclipseVersion) {
    "${base}/eclipse/updates/${eclipseVersion}/".toString()
  }

  P2Source decide(P2Mode mode, String originalBase, List<String> mirrors, String eclipseVersion,
    String greclipseRepoUrl, boolean cacheWarm, Function<String, P2SiteCheck.Result> check) {
    switch (mode) {
      case P2Mode.ORIGINAL:
        return P2Source.original('forced via wetf.spotless.p2.mode=original')
      case P2Mode.SKIP:
        return P2Source.skip('forced via wetf.spotless.p2.mode=skip')
      case P2Mode.MIRROR:
        if (!mirrors) {
          throw new IllegalArgumentException('wetf.spotless.p2.mode=mirror requires at least one mirror in wetf.spotless.p2.mirrors')
        }
        return P2Source.mirror(mirrors[0], 'forced via wetf.spotless.p2.mode=mirror')
      default:
        break
    }

    if (cacheWarm) {
      return P2Source.original('P2 cache is warm')
    }
    P2SiteCheck.Result grec = check.apply(greclipseRepoUrl)
    if (!grec.healthy) {
      return P2Source.skip(grec.reason)
    }
    P2SiteCheck.Result orig = check.apply(updatesUrl(originalBase, eclipseVersion))
    if (orig.healthy) {
      return P2Source.original(orig.reason)
    }
    List<String> reasons = [orig.reason]
    for (String mirror : mirrors) {
      P2SiteCheck.Result r = check.apply(updatesUrl(mirror, eclipseVersion))
      if (r.healthy) {
        return P2Source.mirror(mirror, "original unhealthy (${orig.reason}); using mirror ${mirror}".toString())
      }
      reasons << r.reason
    }
    P2Source.skip(reasons.join('; '))
  }
}
