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

import spock.lang.Specification
import spock.lang.TempDir

class P2CacheProbeSpec extends Specification {

  @TempDir
  File p2Data

  static final List<String> MIRRORS = [
    'https://m1.example',
    'https://m2.example',
    'https://m3.example'
  ]

  /**
   * Creates a Solstice {@code P2QueryResult} without running a P2 query: its only constructor runs the query,
   * so the instance is created like Java deserialization does and its private fields are set reflectively.
   */
  static Object queryResult(List<String> mavenCoordinates, List<File> downloadedP2Jars) {
    Class<?> cls = Class.forName(P2CacheProbe.P2_QUERY_RESULT_CLASS)
    def constructor = sun.reflect.ReflectionFactory.reflectionFactory
      .newConstructorForSerialization(cls, Object.getDeclaredConstructor())
    Object result = constructor.newInstance()
    [mavenCoordinates: mavenCoordinates, downloadedP2Jars: downloadedP2Jars].each { name, value ->
      def field = cls.getDeclaredField(name)
      field.accessible = true
      field.set(result, new ArrayList<>(value))
    }
    result
  }

  File queryContent(String key) {
    File dir = new File(p2Data, "queries/${key}")
    dir.mkdirs()
    new File(dir, 'content')
  }

  /** Creates a cached query referencing a P2 jar; returns the jar. */
  File warmQuery(String key) {
    File jar = new File(p2Data, "bundle-pool/${key}.jar")
    jar.parentFile.mkdirs()
    jar.bytes = [0x50, 0x4b, 3, 4] as byte[]
    writeQuery(key, [jar])
    jar
  }

  void writeQuery(String key, List<File> jars) {
    queryContent(key).withObjectOutputStream { it.writeObject(queryResult(['org.example:lib:1.0'], jars)) }
  }

  def "p2 data dir is located in the Gradle user home"() {
    expect:
    P2CacheProbe.p2DataDir(new File('/guh')) == new File('/guh/caches/p2-data')
  }

  def "query key combines solstice version and model hash like solstice"() {
    expect:
    P2CacheProbe.queryKey('1.8.2', -1092931831) == '1.8.2-1092931831'
    P2CacheProbe.queryKey('1.8.2', 1852791274) == '1.8.21852791274'
    P2CacheProbe.queryKey('SNAPSHOT-', -5) == 'SNAPSHOT-5'
  }

  def "warm cache for key is detected"() {
    given:
    warmQuery('1.8.2-1092931831')

    expect:
    P2CacheProbe.isWarm(p2Data, '1.8.2-1092931831')
  }

  def "cache for other key is not warm"() {
    given:
    warmQuery('1.8.2-1092931831')

    expect:
    !P2CacheProbe.isWarm(p2Data, '1.8.2-109293183')
    !P2CacheProbe.isWarm(p2Data, '1.8.21092931831')
  }

  def "missing directory is not warm"() {
    expect:
    !P2CacheProbe.isWarm(new File(p2Data, 'missing'), '1.8.2-1')
  }

  def "empty queries directory is not warm"() {
    given:
    new File(p2Data, 'queries').mkdirs()

    expect:
    !P2CacheProbe.isWarm(p2Data, '1.8.2-1')
  }

  def "query directory without content is not warm"() {
    given:
    new File(p2Data, 'queries/1.8.2-1').mkdirs()

    expect:
    !P2CacheProbe.isWarm(p2Data, '1.8.2-1')
  }

  def "cache is warm when all referenced jars exist"() {
    given:
    File other = new File(p2Data, 'other.jar').tap { it.bytes = [1] as byte[] }
    writeQuery('1.8.2-1', [warmQuery('a'), other])

    expect:
    P2CacheProbe.isWarm(p2Data, '1.8.2-1')
  }

  def "cache without p2 jars is warm"() {
    given:
    writeQuery('1.8.2-1', [])

    expect:
    P2CacheProbe.isWarm(p2Data, '1.8.2-1')
  }

  def "cache referencing a missing jar is not warm and is left unchanged"() {
    given:
    File present = warmQuery('a')
    writeQuery('1.8.2-1', [
      present,
      new File(p2Data, 'bundle-pool/missing.jar')
    ])
    File content = queryContent('1.8.2-1')
    byte[] before = content.bytes

    expect:
    !P2CacheProbe.isWarm(p2Data, '1.8.2-1')
    content.bytes == before
    present.isFile()
  }

  def "corrupt cache content is not warm and is left unchanged"() {
    given:
    File content = queryContent('1.8.2-1')
    content.bytes = [1, 2, 3] as byte[]

    expect:
    !P2CacheProbe.isWarm(p2Data, '1.8.2-1')
    content.bytes == [1, 2, 3] as byte[]
  }

  def "cache content of another type is not warm"() {
    given:
    queryContent('1.8.2-1').withObjectOutputStream { it.writeObject(new ArrayList<>(['x'])) }

    expect:
    !P2CacheProbe.isWarm(p2Data, '1.8.2-1')
  }

  def "unreadable cache content is not warm"() {
    given:
    File content = queryContent('1.8.2-1')
    content.mkdirs() // a directory instead of a file

    expect:
    !P2CacheProbe.isWarm(p2Data, '1.8.2-1')
  }

  def "mirrors are applied by prefix like spotless"() {
    given:
    def map = P2Source.mirror('https://m1.example', 'test').mirrorMap()

    expect:
    P2CacheProbe.applyMirrors([
      'https://download.eclipse.org/eclipse/updates/4.41/',
      'https://groovy.jfrog.io/artifactory/plugins-release/e4.41/'
    ], map) == [
      'https://m1.example/eclipse/updates/4.41/',
      'https://groovy.jfrog.io/artifactory/plugins-release/e4.41/'
    ]
    P2CacheProbe.applyMirrors(['https://a/x'], [:]) == ['https://a/x']
  }

  def "applying mirrors fails for a repository without matching prefix like spotless"() {
    when:
    P2CacheProbe.applyMirrors(['https://other.example/x/'], P2Source.mirror('https://m1.example', 'test').mirrorMap())

    then:
    thrown(IllegalStateException)
  }

  def "warm unmirrored cache selects original"() {
    given:
    List<Map<String, String>> probed = []
    Predicate<Map<String, String>> warm = { Map<String, String> m -> probed << m; m.isEmpty() } as Predicate

    when:
    def source = P2CacheProbe.fromWarmCache(MIRRORS, warm)

    then:
    source.kind == P2Source.Kind.ORIGINAL
    probed == [[:]]
  }

  def "warm cache of second mirror selects that mirror"() {
    given:
    String m2 = P2Source.mirror(MIRRORS[1], 'x').mirrorMap().values().first()
    Predicate<Map<String, String>> warm = { Map<String, String> m -> m.containsValue(m2) } as Predicate

    when:
    def source = P2CacheProbe.fromWarmCache(MIRRORS, warm)

    then:
    source.kind == P2Source.Kind.MIRROR
    source.mirrorBase == MIRRORS[1]
  }

  def "no warm cache falls through"() {
    given:
    int calls = 0
    Predicate<Map<String, String>> warm = { Map<String, String> m -> calls++; false } as Predicate

    expect:
    P2CacheProbe.fromWarmCache(MIRRORS, warm) == null
    calls == 4
  }

  def "failing probe is treated as not warm"() {
    given:
    Predicate<Map<String, String>> warm = { Map<String, String> m ->
      if (m.isEmpty()) {
        throw new NoSuchMethodError('model')
      }
      m.containsValue(P2Source.mirror(MIRRORS[0], 'x').mirrorMap().values().first())
    } as Predicate

    when:
    def source = P2CacheProbe.fromWarmCache(MIRRORS, warm)

    then:
    source.kind == P2Source.Kind.MIRROR
    source.mirrorBase == MIRRORS[0]
  }
}
