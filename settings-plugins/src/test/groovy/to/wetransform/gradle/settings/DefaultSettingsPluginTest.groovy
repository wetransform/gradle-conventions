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
package to.wetransform.gradle.settings

import org.gradle.testkit.runner.TaskOutcome

class DefaultSettingsPluginTest extends PluginTest {

  def setup() {
    settingsFile.text = """
    plugins {
      id 'to.wetransform.settings.default'
    }
    """.stripIndent()
  }

  def "can resolve a dependency from the test catalog"() {
    given: "A version catalog and a build file"
    createVersionCatalog()
    buildFile << """
    plugins { id 'java' }
    repositories { mavenCentral() }
    dependencies { testImplementation(testLibs.junit) }
    """.stripIndent()

    and: "A test class"
    createTestClass()

    when:
    def result = runTask('test')

    then:
    result.task(":test").outcome == TaskOutcome.SUCCESS
  }

  def "fails when no repositories are defined"() {
    given: "A version catalog and a build file without repositories"
    createVersionCatalog()
    buildFile << """
    plugins { id 'java' }
    dependencies { testImplementation(testLibs.junit) }
    """.stripIndent()

    and: "A test class"
    createTestClass()

    when:
    def result = runTaskWithFailure('test')

    then:
    result.output.contains("Could not resolve all files for configuration ':testCompileClasspath'")
    result.task(":compileTestJava").outcome == TaskOutcome.FAILED
  }

  private void createVersionCatalog() {
    def versionCatalogFile = new File(testProjectDir, "gradle/test-libs.versions.toml")
    versionCatalogFile.parentFile.mkdirs()
    versionCatalogFile.text = """
    [versions]
    junit = "5.9.2"
    [libraries]
    junit = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit" }
    """.stripIndent()
  }

  private void createTestClass() {
    def testFile = new File(testProjectDir, "src/test/java/com/myorg/MyTest.java")
    testFile.parentFile.mkdirs()
    testFile.text = """
    package com.myorg;
    import org.junit.jupiter.api.Test;
    import static org.junit.jupiter.api.Assertions.assertTrue;

    public class MyTest {
      @Test
      public void test() {
        assertTrue(true);
      }
    }
    """.stripIndent()
  }
}
