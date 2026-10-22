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

import java.util.function.Predicate

/**
 * Checks whether the local P2 query cache of spotless (Solstice) already holds the
 * Groovy-Eclipse formatter query, so no network access is needed.
 */
class P2CacheProbe {

  /** P2 data directory as used by the spotless Gradle plugin ({@code GradleProvisioner.defaultP2CacheDirectory}). */
  static File p2DataDir(File gradleUserHome) {
    new File(gradleUserHome, 'caches/p2-data')
  }

  /** Name of the query cache directory, as Solstice's {@code QueryCacheOnDisk} derives it. */
  static String queryKey(String solsticeVersion, int modelHash) {
    // same as OfflineCache.filenameSafe (keys are short, so no truncation needed)
    (solsticeVersion + modelHash).replaceAll('[^a-zA-Z0-9-+_.]', '-').replaceAll('-+', '-')
  }

  static boolean isWarm(File p2DataDir, String queryKey) {
    new File(p2DataDir, "queries/${queryKey}/content").isFile()
  }

  /**
   * Rewrites repository URLs with a mirror map, like spotless' {@code EquoBasedStepBuilder}
   * (first matching prefix wins, a repository without matching prefix is an error).
   */
  static List<String> applyMirrors(Collection<String> repos, Map<String, String> mirrorMap) {
    if (mirrorMap.isEmpty()) {
      return repos as List<String>
    }
    repos.collect { String url ->
      def entry = mirrorMap.find { prefix, replacement -> url.startsWith(prefix) }
      if (entry == null) {
        throw new IllegalStateException("No mirror configured for ${url}")
      }
      entry.value + url.substring(entry.key.length())
    }
  }

  /**
   * Determines a P2 source from a warm cache: the original sites first, then each mirror in order.
   *
   * @param warm checks whether the cache is warm for a mirror map (empty for the original sites)
   * @return the source, or null if no cache is warm
   */
  static P2Source fromWarmCache(List<String> mirrors, Predicate<Map<String, String>> warm) {
    if (safeTest(warm, [:])) {
      return P2Source.original('P2 cache is warm')
    }
    for (String mirror : mirrors) {
      P2Source candidate = P2Source.mirror(mirror, "P2 cache for mirror ${mirror} is warm".toString())
      if (safeTest(warm, candidate.mirrorMap())) {
        return candidate
      }
    }
    null
  }

  private static boolean safeTest(Predicate<Map<String, String>> warm, Map<String, String> mirrorMap) {
    try {
      return warm.test(mirrorMap)
    } catch (Exception | LinkageError e) {
      // never fail the build because of the probe
      return false
    }
  }
}
