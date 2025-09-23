/*
 * Copyright 2025 wetransform GmbH
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

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import io.github.wasabithumb.jtoml.JToml

@CompileStatic
class JavaVersionHelper {

  private static final Logger log = LoggerFactory.getLogger(JavaVersionHelper)

  static String determineDefaultJavaVersion(File projectDir) {
    // 1. Check for mise.toml in project root
    File miseToml = new File(projectDir, "mise.toml")
    String versionString = null
    if (miseToml.exists()) {
      try {
        var toml = JToml.jToml()
        var doc = toml.read(miseToml)
        var tools = doc.get("tools")
        if (tools.isTable()) {
          var toolsTable = tools.asTable()
          if (toolsTable.contains("java")) {
            var java = toolsTable.get("java")
            if (java.isPrimitive()) {
              versionString = java.asPrimitive().asString()
            }
          }
        }
      } catch (Exception e) {
        log.warn("Could not parse mise.toml for Java version: ${e.message}", e)
      }
    }
    // 2. If not found, check for .java-version
    if (!versionString) {
      File javaVersionFile = new File(projectDir, ".java-version")
      if (javaVersionFile.exists()) {
        versionString = javaVersionFile.text.trim()
      }
    }
    // 3. Extract major version
    if (versionString) {
      return extractMajorJavaVersion(versionString)
    }
    // 4. Not found
    return null
  }

  static String extractMajorJavaVersion(String versionString) {
    if (!versionString) return null
    // Remove vendor prefix if present (e.g., "openjdk-17.0.8")
    String v = versionString.replaceAll("^[^0-9]*", "")
    // Split by dot or dash, take first number
    def matcher = v =~ /^(\d{1,2})/
    if (matcher.find()) {
      return matcher.group(1)
    }
    return null
  }
}
