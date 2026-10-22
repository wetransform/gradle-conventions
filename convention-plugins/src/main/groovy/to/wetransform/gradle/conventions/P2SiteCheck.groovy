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

/**
 * Checks whether a P2 update site is actually serving P2 content, as opposed to
 * an HTML maintenance page served with HTTP 200.
 */
class P2SiteCheck {

  static class Result {
    final boolean healthy
    final String reason

    Result(boolean healthy, String reason) {
      this.healthy = healthy
      this.reason = reason
    }

    @Override
    String toString() {
      "${healthy ? 'healthy' : 'unhealthy'} (${reason})"
    }
  }

  private static final int MAX_BODY = 64 * 1024

  private final int connectTimeoutMs
  private final int readTimeoutMs

  P2SiteCheck(int connectTimeoutMs = 3000, int readTimeoutMs = 3000) {
    this.connectTimeoutMs = connectTimeoutMs
    this.readTimeoutMs = readTimeoutMs
  }

  Result check(String repoUrl) {
    String base = repoUrl.endsWith('/') ? repoUrl : repoUrl + '/'
    try {
      String indexUrl = base + 'p2.index'
      Response index = get(indexUrl)
      if (index.status < 400 || index.status > 499) {
        if (!index.ok) {
          return new Result(false, "${indexUrl}: ${index.describe()}")
        }
        if (new String(index.body, 'UTF-8').contains('metadata.repository.factory.order=')) {
          return new Result(true, "${indexUrl}: ${index.describe()}")
        }
        return new Result(false, "${indexUrl}: ${index.describe()} without p2 metadata")
      }

      String last = "${indexUrl}: HTTP ${index.status}"
      for (String name : [
          'compositeContent.jar',
          'content.jar'
        ]) {
        String url = base + name
        Response resp = get(url)
        if (resp.ok && resp.body.length >= 2 && resp.body[0] == (byte) 0x50 && resp.body[1] == (byte) 0x4b) {
          return new Result(true, "${url}: ${resp.describe()}")
        }
        last = "${url}: ${resp.describe()}"
      }
      return new Result(false, last)
    } catch (Exception e) {
      return new Result(false, "${base}: ${e.class.simpleName}: ${e.message}")
    }
  }

  private static class Response {
    int status
    String contentType
    byte[] body

    boolean isOk() {
      status == 200 && !(contentType ?: '').toLowerCase(Locale.ROOT).startsWith('text/html')
    }

    String describe() {
      "HTTP ${status}${contentType ? ' ' + contentType : ''}"
    }
  }

  private Response get(String url) {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
    try {
      conn.connectTimeout = connectTimeoutMs
      conn.readTimeout = readTimeoutMs
      conn.instanceFollowRedirects = true
      conn.requestMethod = 'GET'
      int status = conn.responseCode
      Response resp = new Response(status: status, contentType: conn.contentType, body: new byte[0])
      if (status == 200) {
        InputStream is = conn.inputStream
        try {
          ByteArrayOutputStream out = new ByteArrayOutputStream()
          byte[] buf = new byte[8192]
          int n
          while (out.size() < MAX_BODY && (n = is.read(buf)) != -1) {
            out.write(buf, 0, n)
          }
          resp.body = out.toByteArray()
        } finally {
          is.close()
        }
      }
      return resp
    } finally {
      conn.disconnect()
    }
  }
}
