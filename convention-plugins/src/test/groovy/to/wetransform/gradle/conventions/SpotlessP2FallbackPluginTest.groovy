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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import org.gradle.testkit.runner.TaskOutcome

import com.sun.net.httpserver.HttpServer

class SpotlessP2FallbackPluginTest extends PluginTest {

  static final String VALID_INDEX = '''\
    #Wed Sep 09 11:55:54 UTC 2026
    artifact.repository.factory.order=compositeArtifacts.xml,\\!
    metadata.repository.factory.order=compositeContent.xml,\\!
    version=1
    '''.stripIndent()

  static final String HTML = '<!DOCTYPE html><html><body>Temporary shutdown</body></html>'

  static final String WARNING = 'Groovy and Gradle file formatting was NOT checked'

  /**
   * Minimal local HTTP server. The handler maps a request path to [contentType, body],
   * or null for 404. Requests are counted per path.
   */
  static class TestServer {
    final HttpServer server
    final Map<String, AtomicInteger> requests = new ConcurrentHashMap<>()

    TestServer(Closure<List<String>> handler) {
      server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
      server.createContext('/') { exchange ->
        String path = exchange.requestURI.path
        requests.computeIfAbsent(path) { new AtomicInteger() }.incrementAndGet()
        List<String> resp = handler.call(path)
        if (resp == null) {
          exchange.sendResponseHeaders(404, -1)
        } else {
          byte[] body = resp[1].getBytes('UTF-8')
          exchange.responseHeaders.add('Content-Type', resp[0])
          exchange.sendResponseHeaders(200, body.length)
          exchange.responseBody.write(body)
        }
        exchange.close()
      }
      server.start()
    }

    String getUrl() {
      "http://127.0.0.1:${server.address.port}".toString()
    }

    void stop() {
      server.stop(0)
    }
  }

  static List<String> validIndex() {
    [
      'application/octet-stream',
      VALID_INDEX
    ]
  }

  static List<String> html() {
    [
      'text/html; charset=utf-8',
      HTML
    ]
  }

  TestServer server

  def cleanup() {
    server?.stop()
  }

  /** Server where the greclipse repo is healthy and everything else serves HTML. */
  TestServer startServer(Closure<List<String>> otherwise = { html() }) {
    server = new TestServer({ String path ->
      if (path.startsWith('/greclipse/')) {
        return path.endsWith('/p2.index') ? validIndex() : null
      }
      otherwise.call(path)
    })
    server
  }

  String[] p2Args(String... extra) {
    ([
      "-Pwetf.spotless.p2.originalBase=${server.url}",
      "-Pwetf.spotless.p2.greclipseRepo=${server.url}/greclipse/",
      "-Pwetf.spotless.p2.mirrors=${server.url}/mirror"
    ]
    + extra.toList()).collect { it.toString() } as String[]
  }

  void groovyProject(boolean trailingWhitespace = false) {
    buildFile << """
    plugins {
      id 'groovy'
      id 'to.wetransform.conventions.spotless'
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()

    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.groovy]
    indent_style = space
    indent_size = 4
    trim_trailing_whitespace = true
    """.stripIndent()

    new File(testProjectDir, 'src/main/groovy').mkdirs()
    new File(testProjectDir, 'src/main/groovy/Test.groovy') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    class Test {
    def method() {
    println "Indentation wrong"${trailingWhitespace ? '   ' : ''}
    }
    }
    """.stripIndent()
  }

  def "forced skip prints warning and does not apply greclipse"() {
    given:
    startServer()
    groovyProject()

    when:
    def result = runTask('spotlessCheck', p2Args('-Pwetf.spotless.p2.mode=skip'))

    then:
    result.task(':spotlessGroovyCheck').outcome == TaskOutcome.SUCCESS
    result.output.contains(WARNING)
    result.output.contains('Reason: forced via wetf.spotless.p2.mode=skip')
  }

  def "skip still enforces generic settings"() {
    given:
    startServer()
    groovyProject(true)

    when:
    def result = runTaskWithFailure('spotlessCheck', p2Args('-Pwetf.spotless.p2.mode=skip'))

    then:
    result.task(':spotlessGroovyCheck').outcome == TaskOutcome.FAILED
    result.output.contains('src/main/groovy/Test.groovy')
  }

  def "auto skips when original serves html and mirrors are unhealthy"() {
    given:
    // greclipse site healthy, original and mirror serve HTML
    startServer()
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()
    def version = GrEclipseRepos.defaultEclipseVersion()

    when:
    def result = runTask('help', p2Args())

    then:
    result.output.contains(WARNING)
    result.output.contains('==========')
    result.output.contains('Override with -Pwetf.spotless.p2.mode=original|mirror|skip')
    result.output.readLines().find { it.startsWith('Reason: ') }?.contains('text/html')
    server.requests.containsKey('/greclipse/p2.index')
    server.requests.containsKey("/eclipse/updates/${version}/p2.index".toString())
    server.requests.containsKey("/mirror/eclipse/updates/${version}/p2.index".toString())
  }

  def "auto skips when groovy-eclipse site is unhealthy"() {
    given:
    server = new TestServer({ String path -> html() })
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()

    when:
    def result = runTask('help', p2Args())

    then:
    result.output.contains(WARNING)
    result.output.readLines().find { it.startsWith('Reason: ') }?.contains('/greclipse/p2.index')
    server.requests.containsKey('/greclipse/p2.index')
    !server.requests.keySet().any { it.startsWith('/eclipse/') || it.startsWith('/mirror/') }
  }

  def "auto uses mirror when original serves html"() {
    given:
    startServer({ String path ->
      if (path.startsWith('/mirror/eclipse/updates/') && path.endsWith('/p2.index')) {
        return validIndex()
      }
      html()
    })
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()

    when:
    def result = runTask('help', p2Args())

    then:
    result.output.contains("using P2 mirror ${server.url}/mirror")
    !result.output.contains(WARNING)
  }

  def "auto skips without network access when offline"() {
    given:
    // all sites healthy, so only the offline check can lead to skipping;
    // with the test overrides the local P2 cache is not probed, i.e. it counts as cold
    startServer({ String path ->
      path.endsWith('/p2.index') ? validIndex() : null
    })
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()

    when:
    def result = runTask('help', p2Args('--offline'))

    then:
    result.output.contains(WARNING)
    result.output.contains('Reason: offline and P2 cache is cold')
    server.requests.isEmpty()
  }

  def "health check runs once per build in multi-project build"() {
    given:
    startServer({ String path ->
      path.endsWith('/p2.index') ? validIndex() : null
    })
    settingsFile << """
    include 'a', 'b'
    """.stripIndent()
    def plugin = """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()
    buildFile << plugin
    ['a', 'b'].each {
      new File(testProjectDir, it).mkdirs()
      new File(testProjectDir, "${it}/build.gradle") << plugin
    }

    when:
    def result = runTask('help', p2Args())

    then:
    !result.output.contains(WARNING)
    def indexRequests = server.requests.findAll { k, v -> k.endsWith('/p2.index') }
    indexRequests.keySet().any { it.startsWith('/greclipse/') }
    indexRequests.keySet().any { it.startsWith('/eclipse/updates/') }
    indexRequests.values().every { it.get() == 1 }
  }

  def "auto mode recovers from an outage despite configuration cache"() {
    given:
    boolean originalHealthy = false
    startServer({ String path ->
      originalHealthy && path.startsWith('/eclipse/updates/') && path.endsWith('/p2.index') ? validIndex() : html()
    })
    // no groovy plugin: the Eclipse JDT step of spotlessJava prevents configuration cache reuse (independent of P2 mode)
    // help instead of spotless tasks: the test URLs only affect the health check, spotless would provision from the real sites
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()
    def args = p2Args('--configuration-cache')

    when: 'the original site and the mirror are down'
    def outage = runTask('help', args)

    then:
    outage.output.contains(WARNING)

    when: 'the original site is back, nothing else changed'
    originalHealthy = true
    def recovered = runTask('help', args)

    then: 'the cached configuration is discarded and greclipse is no longer skipped'
    recovered.output.contains('configuration cache cannot be reused')
    recovered.output.contains(P2SourceValueSource.simpleName)
    !recovered.output.contains(WARNING)

    when: 'nothing changed'
    def indexRequests = server.requests.findAll { k, v -> k.startsWith('/eclipse/updates/') }.values()*.get().sum()
    def unchanged = runTask('help', args)

    then: 'the configuration cache is reused, but the site is checked again'
    unchanged.output.contains('Reusing configuration cache')
    !unchanged.output.contains(WARNING)
    server.requests.findAll { k, v -> k.startsWith('/eclipse/updates/') }.values()*.get().sum() > indexRequests
  }

  def "skip warning is printed again on configuration cache hit"() {
    given:
    startServer()
    // no groovy plugin: the Eclipse JDT step of spotlessJava prevents configuration cache reuse (independent of P2 mode)
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()

    when:
    runTask('spotlessCheck', p2Args('-Pwetf.spotless.p2.mode=skip', '--configuration-cache'))
    def result = runTask('spotlessCheck', p2Args('-Pwetf.spotless.p2.mode=skip', '--configuration-cache'))

    then:
    result.output.contains('Reusing configuration cache')
    result.output.contains(WARNING)
    result.output.contains('Reason: forced via wetf.spotless.p2.mode=skip')
  }

  def "skip warning is printed on configuration cache hit when a check fails first"() {
    given:
    startServer()
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.md]
    trim_trailing_whitespace = true
    """.stripIndent()
    new File(testProjectDir, 'README.md') << 'Trailing whitespace   \n'
    def args = p2Args('-Pwetf.spotless.p2.mode=skip', '--configuration-cache')

    when:
    runTaskWithFailure('spotlessCheck', args)
    def result = runTaskWithFailure('spotlessCheck', args)

    then:
    result.output.contains('Reusing configuration cache')
    result.task(':spotlessOtherCheck').outcome == TaskOutcome.FAILED
    result.task(':spotlessP2SkipWarning').outcome == TaskOutcome.SUCCESS
    result.output.contains(WARNING)
    result.output.indexOf(WARNING) < result.output.indexOf('README.md')
  }

  def "skip warning is printed on configuration cache hit when a single format task runs"() {
    given:
    startServer()
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()
    def args = p2Args('-Pwetf.spotless.p2.mode=skip', '--configuration-cache')

    when:
    runTask('spotlessGroovyGradleCheck', args)
    def result = runTask('spotlessGroovyGradleCheck', args)

    then:
    result.output.contains('Reusing configuration cache')
    result.task(':spotlessP2SkipWarning').outcome == TaskOutcome.SUCCESS
    result.task(':spotlessCheck') == null
    result.output.contains(WARNING)
    result.output.contains('Reason: forced via wetf.spotless.p2.mode=skip')
  }

  def "no skip warning task when greclipse is applied"() {
    given:
    startServer()
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()

    when:
    def result = runTask('spotlessGroovyGradleCheck', p2Args('-Pwetf.spotless.p2.mode=original', '--dry-run'))

    then:
    !result.output.contains('spotlessP2SkipWarning')
    !result.output.contains(WARNING)
  }

  def "github actions annotation in skip mode"() {
    given:
    startServer()
    groovyProject()

    when:
    def result = runTask(System.getenv() + [GITHUB_ACTIONS: 'true'], 'spotlessCheck', p2Args('-Pwetf.spotless.p2.mode=skip'))

    then:
    result.output.readLines().any { it.startsWith('::warning title=Spotless::') }
  }

  def "github actions annotation only once per build in multi-project build"() {
    given:
    startServer()
    settingsFile << """
    include 'a', 'b'
    """.stripIndent()
    def plugin = """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }
    """.stripIndent()
    buildFile << plugin
    ['a', 'b'].each {
      new File(testProjectDir, it).mkdirs()
      new File(testProjectDir, "${it}/build.gradle") << plugin
    }
    def env = System.getenv() + [GITHUB_ACTIONS: 'true']
    def args = p2Args('-Pwetf.spotless.p2.mode=skip', '--configuration-cache')

    when:
    def first = runTask(env, 'spotlessCheck', args)
    def second = runTask(env, 'spotlessCheck', args)

    then:
    second.output.contains('Reusing configuration cache')
    [first, second].every { result ->
      def lines = result.output.readLines()
      // one warning block per project at execution time
      lines.count { it.contains(WARNING) } >= 3 &&
      lines.count { it.startsWith('::warning title=Spotless::') } == 1
    }
  }

  def "invalid mode fails with clear message"() {
    given:
    startServer()
    groovyProject()

    when:
    def result = runTaskWithFailure('help', p2Args('-Pwetf.spotless.p2.mode=off'))

    then:
    result.output.contains("Invalid value 'off' for wetf.spotless.p2.mode")
  }
}
