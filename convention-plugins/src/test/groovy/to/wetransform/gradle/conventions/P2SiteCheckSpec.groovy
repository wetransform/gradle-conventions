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

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

import spock.lang.Specification

class P2SiteCheckSpec extends Specification {

  HttpServer server

  def setup() {
    server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
  }

  def cleanup() {
    server?.stop(0)
  }

  private static void respond(HttpExchange ex, int status, String type, byte[] body) {
    if (type) {
      ex.responseHeaders.add('Content-Type', type)
    }
    ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length)
    if (body.length > 0) {
      ex.responseBody.write(body)
    }
    ex.close()
  }

  private String start() {
    server.start()
    "http://127.0.0.1:${server.address.port}/repo/"
  }

  def 'valid p2 index is healthy'() {
    given:
    server.createContext('/') { ex ->
      respond(ex, 200, 'text/plain', ('version=1\nmetadata.repository.factory.order=compositeContent.xml,\\!\n' +
        'artifact.repository.factory.order=compositeArtifacts.xml,\\!\n').bytes)
    }

    expect:
    new P2SiteCheck().check(start()).healthy
  }

  def 'html page with status 200 is unhealthy'() {
    given:
    server.createContext('/') { ex ->
      respond(ex, 200, 'text/html', '<html><body>Temporary shutdown</body></html>'.bytes)
    }

    when:
    def result = new P2SiteCheck().check(start())

    then:
    !result.healthy
    result.reason.contains('text/html')
  }

  def 'redirect to html page is unhealthy'() {
    given:
    server.createContext('/') { ex ->
      if (ex.requestURI.path.endsWith('p2.index')) {
        ex.responseHeaders.add('Location', '/shutdown.html')
        respond(ex, 302, null, new byte[0])
      } else {
        respond(ex, 200, 'text/html', '<html><body>Temporary shutdown</body></html>'.bytes)
      }
    }

    expect:
    !new P2SiteCheck().check(start()).healthy
  }

  def 'index without p2 keys is unhealthy'() {
    given:
    server.createContext('/') { ex -> respond(ex, 200, 'text/plain', 'hello'.bytes) }

    expect:
    !new P2SiteCheck().check(start()).healthy
  }

  def 'p2 index with whitespace around the separator is healthy'() {
    given:
    server.createContext('/') { ex ->
      respond(ex, 200, 'text/plain', 'version=1\nmetadata.repository.factory.order = content.xml,\\!\n'.bytes)
    }

    expect:
    new P2SiteCheck().check(start()).healthy
  }

  def 'p2 index metadata entry without separator comma is unhealthy'() {
    given:
    // Solstice cannot parse the metadata entry without the trailing comma
    server.createContext('/') { ex ->
      respond(ex, 200, 'text/plain', 'version=1\nmetadata.repository.factory.order=content.xml\n'.bytes)
    }

    expect:
    !new P2SiteCheck().check(start()).healthy
  }

  def 'missing p2 index falls back to content jar'() {
    given:
    server.createContext('/') { ex ->
      if (ex.requestURI.path.endsWith('content.jar')) {
        respond(ex, 200, 'application/java-archive', ([0x50, 0x4b, 3, 4, 0, 0] as byte[]))
      } else {
        respond(ex, 404, 'text/plain', 'not found'.bytes)
      }
    }

    expect:
    new P2SiteCheck().check(start()).healthy
  }

  def 'p2 index 404 falls back to content jar'() {
    given:
    server.createContext('/') { ex ->
      if (ex.requestURI.path.endsWith('/content.jar')) {
        respond(ex, 200, 'application/java-archive', ([0x50, 0x4b, 3, 4, 0, 0] as byte[]))
      } else {
        respond(ex, 404, null, new byte[0])
      }
    }

    when:
    def result = new P2SiteCheck().check(start())

    then:
    result.healthy
    result.reason.contains('content.jar')
  }

  def 'p2 index 403 is unhealthy'() {
    given:
    List<String> paths = []
    server.createContext('/') { ex ->
      paths << ex.requestURI.path
      if (ex.requestURI.path.endsWith('content.jar')) {
        respond(ex, 200, 'application/java-archive', ([0x50, 0x4b, 3, 4, 0, 0] as byte[]))
      } else {
        respond(ex, 403, 'text/plain', 'forbidden'.bytes)
      }
    }

    when:
    def result = new P2SiteCheck().check(start())

    then:
    !result.healthy
    result.reason.contains('403')
    paths == ['/repo/p2.index']
  }

  def '403 html error page falls back to content jar'() {
    given:
    server.createContext('/') { ex ->
      if (ex.requestURI.path.endsWith('/content.jar')) {
        respond(ex, 200, 'application/java-archive', ([0x50, 0x4b, 3, 4, 0, 0] as byte[]))
      } else {
        respond(ex, 403, contentType, body.bytes)
      }
    }

    expect:
    new P2SiteCheck().check(start()).healthy

    where:
    contentType   | body
    'text/plain'  | '  \n<!doctype HTML><html><body>Forbidden</body></html>'
    'text/html'   | '<html><body>Forbidden</body></html>'
    null          | '<!DOCTYPE html><html><body>Forbidden</body></html>'
  }

  def '502 html error page is unhealthy'() {
    given:
    server.createContext('/') { ex ->
      if (ex.requestURI.path.endsWith('content.jar')) {
        respond(ex, 200, 'application/java-archive', ([0x50, 0x4b, 3, 4, 0, 0] as byte[]))
      } else {
        respond(ex, 502, 'text/html', '<!DOCTYPE html><html><body>Bad gateway</body></html>'.bytes)
      }
    }

    when:
    def result = new P2SiteCheck().check(start())

    then:
    !result.healthy
    result.reason.contains('502')
  }

  def 'html page with status 200 is unhealthy even if content jar exists'() {
    given:
    server.createContext('/') { ex ->
      if (ex.requestURI.path.endsWith('content.jar')) {
        respond(ex, 200, 'application/java-archive', ([0x50, 0x4b, 3, 4, 0, 0] as byte[]))
      } else {
        respond(ex, 200, 'text/plain', '<!DOCTYPE html><html><body>Temporary shutdown</body></html>'.bytes)
      }
    }

    expect:
    !new P2SiteCheck().check(start()).healthy
  }

  def 'fallback jar served as html is unhealthy'() {
    given:
    server.createContext('/') { ex ->
      if (ex.requestURI.path.endsWith('p2.index')) {
        respond(ex, 404, 'text/plain', 'not found'.bytes)
      } else {
        respond(ex, 200, 'text/html', 'PK<html></html>'.bytes)
      }
    }

    when:
    def result = new P2SiteCheck().check(start())

    then:
    !result.healthy
    result.reason.contains('text/html')
  }

  def 'server error is unhealthy'() {
    given:
    server.createContext('/') { ex -> respond(ex, 503, 'text/plain', 'unavailable'.bytes) }

    when:
    def result = new P2SiteCheck().check(start())

    then:
    !result.healthy
    result.reason.contains('503')
  }

  def 'timeout is unhealthy'() {
    given:
    server.createContext('/') { ex ->
      Thread.sleep(2000)
      try {
        respond(ex, 200, 'text/plain', 'x'.bytes)
      } catch (IOException ignored) {
      }
    }
    def url = start()

    when:
    def begin = System.currentTimeMillis()
    def result = new P2SiteCheck(500, 500).check(url)
    def elapsed = System.currentTimeMillis() - begin

    then:
    !result.healthy
    elapsed < 1500
  }

  def 'unreachable host is unhealthy'() {
    given:
    def socket = new ServerSocket(0, 1, InetAddress.getByName('127.0.0.1'))
    int port = socket.localPort
    socket.close()

    expect:
    !new P2SiteCheck().check("http://127.0.0.1:${port}/").healthy
  }
}
