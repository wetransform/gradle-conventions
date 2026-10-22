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

import spock.lang.Specification

class P2SourceDeciderSpec extends Specification {

  static final String GREC = 'https://groovy.jfrog.io/artifactory/plugins-release/org/codehaus/groovy/groovy-eclipse-integration/6.3.0/e4.41/'
  static final String ORIG = 'https://download.eclipse.org'
  static final String ORIG_URL = 'https://download.eclipse.org/eclipse/updates/4.41/'
  static final List<String> MIRRORS = [
    'https://m1.example',
    'https://m2.example',
    'https://m3.example'
  ]

  List<String> calls = []

  Function<String, P2SiteCheck.Result> scripted(Map<String, Boolean> health) {
    return { String url ->
      calls << url
      boolean ok = health.getOrDefault(url, true)
      new P2SiteCheck.Result(ok, "reason for ${url}".toString())
    } as Function<String, P2SiteCheck.Result>
  }

  P2Source decide(P2Mode mode, boolean warm, Map<String, Boolean> health = [:]) {
    new P2SourceDecider().decide(mode, ORIG, MIRRORS, '4.41', GREC, warm, scripted(health))
  }

  def 'forced original does no checks'() {
    when:
    def s = decide(P2Mode.ORIGINAL, false)
    then:
    s.kind == P2Source.Kind.ORIGINAL
    calls.isEmpty()
  }

  def 'forced skip does no checks'() {
    when:
    def s = decide(P2Mode.SKIP, false)
    then:
    s.kind == P2Source.Kind.SKIP
    s.reason == 'forced via wetf.spotless.p2.mode=skip'
    calls.isEmpty()
  }

  def 'forced mirror uses first mirror without checks'() {
    when:
    def s = decide(P2Mode.MIRROR, false)
    then:
    s.kind == P2Source.Kind.MIRROR
    s.mirrorBase == MIRRORS[0]
    calls.isEmpty()
  }

  def 'forced mirror with no mirrors fails'() {
    when:
    new P2SourceDecider().decide(P2Mode.MIRROR, ORIG, [], '4.41', GREC, false, scripted([:]))
    then:
    thrown(IllegalArgumentException)
  }

  def 'auto with warm cache does no checks'() {
    when:
    def s = decide(P2Mode.AUTO, true)
    then:
    s.kind == P2Source.Kind.ORIGINAL
    calls == []
  }

  def 'auto uses original when healthy'() {
    when:
    def s = decide(P2Mode.AUTO, false)
    then:
    s.kind == P2Source.Kind.ORIGINAL
    calls == [GREC, ORIG_URL]
  }

  def 'auto falls back to first healthy mirror'() {
    when:
    def s = decide(P2Mode.AUTO, false, [(ORIG_URL): false, 'https://m1.example/eclipse/updates/4.41/': false])
    then:
    s.kind == P2Source.Kind.MIRROR
    s.mirrorBase == MIRRORS[1]
    !calls.contains('https://m3.example/eclipse/updates/4.41/')
  }

  def 'auto skips when nothing is healthy'() {
    when:
    def urls = [ORIG_URL] + MIRRORS.collect { P2SourceDecider.updatesUrl(it, '4.41') }
    def s = decide(P2Mode.AUTO, false, urls.collectEntries { [(it): false] })
    then:
    s.kind == P2Source.Kind.SKIP
    urls.every { s.reason.contains("reason for ${it}") }
  }

  def 'auto skips when groovy-eclipse site is unhealthy'() {
    when:
    def s = decide(P2Mode.AUTO, false, [(GREC): false])
    then:
    s.kind == P2Source.Kind.SKIP
    s.reason.contains(GREC)
    calls == [GREC]
  }

  def 'mirror map maps original prefix and groovy-eclipse identity'() {
    expect:
    P2Source.mirror('https://ftp.fau.de/eclipse', 'x').mirrorMap() == [
      'https://download.eclipse.org/eclipse/updates/': 'https://ftp.fau.de/eclipse/eclipse/updates/',
      'https://groovy.jfrog.io/'                     : 'https://groovy.jfrog.io/'
    ]
    P2Source.original('x').mirrorMap() == [:]
    P2Source.skip('x').mirrorMap() == [:]
  }

  def 'encode decode round trip'() {
    expect:
    [
      P2Source.original('r'),
      P2Source.mirror('https://a.example/eclipse', 'r: x|y'),
      P2Source.skip('why; not'),
      P2Source.skip('line 1\nline 2 with \\ and \\n literally')
    ].every {
      def d = P2Source.decode(it.encode())
      d.kind == it.kind && d.mirrorBase == it.mirrorBase && d.reason == it.reason
    }
  }

  def 'mode parsing'() {
    expect:
    P2Mode.parse(null) == P2Mode.AUTO
    P2Mode.parse('') == P2Mode.AUTO
    P2Mode.parse('Auto') == P2Mode.AUTO
    P2Mode.parse('SKIP') == P2Mode.SKIP

    when:
    P2Mode.parse('off')
    then:
    def e = thrown(IllegalArgumentException)
    e.message == "Invalid value 'off' for wetf.spotless.p2.mode, expected one of: auto, original, mirror, skip"
  }

  def 'mirror list is normalized'() {
    expect:
    P2SourceDecider.parseMirrors(' https://a.example/eclipse/ , https://b.example ,') == [
      'https://a.example/eclipse',
      'https://b.example'
    ]
    P2SourceDecider.updatesUrl('https://a.example/eclipse', '4.41') == 'https://a.example/eclipse/eclipse/updates/4.41/'
    P2SourceDecider.parseMirrors(null) == P2SourceDecider.DEFAULT_MIRRORS
    P2SourceDecider.parseMirrors(',') == []
    P2SourceDecider.DEFAULT_MIRRORS == [
      'https://ftp.fau.de/eclipse',
      'https://ftp.halifax.rwth-aachen.de/eclipse',
      'https://mirror.ibcp.fr/pub/eclipse'
    ]
  }
}
