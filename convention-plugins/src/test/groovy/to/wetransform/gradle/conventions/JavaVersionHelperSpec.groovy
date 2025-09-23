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

import java.nio.file.Files

import spock.lang.Specification

class JavaVersionHelperSpec extends Specification {

  def "extractMajorJavaVersion should extract major version from simple version string"() {
    expect:
    JavaVersionHelper.extractMajorJavaVersion("17.0.8") == "17"
    JavaVersionHelper.extractMajorJavaVersion("8") == "8"
    JavaVersionHelper.extractMajorJavaVersion("11.0.2") == "11"
    JavaVersionHelper.extractMajorJavaVersion("openjdk-17.0.8") == "17"
    JavaVersionHelper.extractMajorJavaVersion("temurin-17.0.16+8") == "17"
    JavaVersionHelper.extractMajorJavaVersion("zulu-21.0.1") == "21"
    JavaVersionHelper.extractMajorJavaVersion(null) == null
    JavaVersionHelper.extractMajorJavaVersion("") == null
  }

  def "determineDefaultJavaVersion should read from .java-version file"() {
    given:
    File tmpDir = Files.createTempDirectory("javaversiontest").toFile()
    File javaVersionFile = new File(tmpDir, ".java-version")
    javaVersionFile.text = "17.0.8"

    expect:
    JavaVersionHelper.determineDefaultJavaVersion(tmpDir) == "17"

    cleanup:
    javaVersionFile.delete()
    tmpDir.delete()
  }

  def "determineDefaultJavaVersion should read from mise.toml file"() {
    given:
    File tmpDir = Files.createTempDirectory("javaversiontest").toFile()
    File miseToml = new File(tmpDir, "mise.toml")
    miseToml.text = """
[tools]
java = \"21.0.1\"
"""

    expect:
    JavaVersionHelper.determineDefaultJavaVersion(tmpDir) == "21"

    cleanup:
    miseToml.delete()
    tmpDir.delete()
  }

  def "determineDefaultJavaVersion prefers mise.toml over .java-version"() {
    given:
    File tmpDir = Files.createTempDirectory("javaversiontest").toFile()
    File miseToml = new File(tmpDir, "mise.toml")
    miseToml.text = """
[tools]
java = \"11.0.2\"
"""
    File javaVersionFile = new File(tmpDir, ".java-version")
    javaVersionFile.text = "17.0.8"

    expect:
    JavaVersionHelper.determineDefaultJavaVersion(tmpDir) == "11"

    cleanup:
    miseToml.delete()
    javaVersionFile.delete()
    tmpDir.delete()
  }

  def "determineDefaultJavaVersion returns null if no version files exist"() {
    given:
    File tmpDir = Files.createTempDirectory("javaversiontest").toFile()

    expect:
    JavaVersionHelper.determineDefaultJavaVersion(tmpDir) == null

    cleanup:
    tmpDir.delete()
  }
}
