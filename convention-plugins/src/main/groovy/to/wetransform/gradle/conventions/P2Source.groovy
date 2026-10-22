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

/** Result of the P2 source decision. Only holds strings/enum, so it is serializable-friendly. */
class P2Source implements Serializable {

  enum Kind {
    ORIGINAL,
    MIRROR,
    SKIP
  }

  private static final String SEP = '\n'

  final Kind kind
  /** MIRROR only, no trailing slash. */
  final String mirrorBase
  final String reason

  P2Source(Kind kind, String mirrorBase, String reason) {
    this.kind = kind
    this.mirrorBase = mirrorBase
    this.reason = reason
  }

  static P2Source original(String reason) {
    new P2Source(Kind.ORIGINAL, null, reason)
  }

  static P2Source mirror(String mirrorBase, String reason) {
    new P2Source(Kind.MIRROR, mirrorBase, reason)
  }

  static P2Source skip(String reason) {
    new P2Source(Kind.SKIP, null, reason)
  }

  Map<String, String> mirrorMap() {
    if (kind != Kind.MIRROR) {
      return [:]
    }
    [
      'https://download.eclipse.org/eclipse/updates/': mirrorBase + '/eclipse/updates/',
      'https://groovy.jfrog.io/'                     : 'https://groovy.jfrog.io/'
    ]
  }

  /** Encodes as three lines: kind, mirrorBase, reason; with the reason escaped. */
  String encode() {
    [
      kind.name(),
      mirrorBase ?: '',
      escape(reason ?: '')
    ].join(SEP)
  }

  static P2Source decode(String encoded) {
    List<String> parts = encoded.split(SEP, 3) as List<String>
    String base = parts[1]
    new P2Source(Kind.valueOf(parts[0]), base.isEmpty() ? null : base, unescape(parts[2]))
  }

  private static String escape(String s) {
    s.replace('\\', '\\\\').replace('\n', '\\n')
  }

  private static String unescape(String s) {
    StringBuilder sb = new StringBuilder()
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i)
      if (c == (char) '\\' && i + 1 < s.length()) {
        char n = s.charAt(++i)
        sb.append(n == (char) 'n' ? (char) '\n' : n)
      } else {
        sb.append(c)
      }
    }
    sb.toString()
  }

  @Override
  String toString() {
    "${kind}${mirrorBase ? ' ' + mirrorBase : ''} (${reason})"
  }
}
