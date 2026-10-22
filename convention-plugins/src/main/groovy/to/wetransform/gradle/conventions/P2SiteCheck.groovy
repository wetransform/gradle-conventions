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

import java.util.regex.Pattern

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

  /** Same pattern Solstice uses to read the metadata entry of a p2.index ({@code P2Client}). */
  private static final Pattern P2_METADATA = Pattern.compile('metadata\\.repository\\.factory\\.order\\s*=(.*),')

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
      if (index.ok) {
        if (P2_METADATA.matcher(new String(index.body, 'UTF-8')).find()) {
          return new Result(true, "${indexUrl}: ${index.describe()}")
        }
        return new Result(false, "${indexUrl}: ${index.describe()} without p2 metadata")
      }
      if (!indexMissing(index)) {
        return new Result(false, "${indexUrl}: ${index.describe()}")
      }

      // like Solstice, guess the repository files if there is no p2.index
      String last = "${indexUrl}: ${index.describe()}"
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

  /**
   * Whether Solstice treats the p2.index as missing and guesses the repository files ({@code P2Client.getBytes}):
   * on HTTP 404, or on an HTML error page with another status except 502.
   * An HTML page with HTTP 200 is not treated as missing, since spotless still fails on it in practice
   * (e.g. the maintenance page of download.eclipse.org).
   */
  private static boolean indexMissing(Response index) {
    index.status == 404 || (index.status != 200 && index.status != 502 && index.html)
  }

  private static class Response {
    int status
    String contentType
    byte[] body

    boolean isOk() {
      status == 200 && !htmlContentType
    }

    boolean isHtmlContentType() {
      (contentType ?: '').toLowerCase(Locale.ROOT).startsWith('text/html')
    }

    /** HTML by content type or, like Solstice, by a body starting with {@code <!DOCTYPE html}. */
    boolean isHtml() {
      htmlContentType || new String(body, 'UTF-8').stripLeading().toLowerCase(Locale.ROOT).startsWith('<!doctype html')
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
      InputStream is = null
      try {
        is = status >= 400 ? conn.errorStream : conn.inputStream
      } catch (IOException ignored) {
        // no body
      }
      if (is != null) {
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
