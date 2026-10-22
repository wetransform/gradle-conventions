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

/** Mode for selecting the P2 source (property wetf.spotless.p2.mode). */
enum P2Mode {
  AUTO,
  ORIGINAL,
  MIRROR,
  SKIP

  static P2Mode parse(String value) {
    if (value == null || value.trim().isEmpty()) {
      return AUTO
    }
    String v = value.trim().toUpperCase(Locale.ROOT)
    P2Mode found = values().find { it.name() == v }
    if (found == null) {
      throw new IllegalArgumentException("Invalid value '${value}' for wetf.spotless.p2.mode, expected one of: auto, original, mirror, skip")
    }
    found
  }
}
