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

import org.gradle.testkit.runner.TaskOutcome

class ReposWetransformPluginTest extends PluginTest {

  def setup() {
    buildFile.text = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions'
    }

    wetransform {
      repos {
        mavenCentral()
        wetfPublic()
      }
    }

    dependencies {
      implementation('eu.esdihumboldt.hale:eu.esdihumboldt.util.config:6.2.2')
      testImplementation('org.junit.jupiter:junit-jupiter-api:5.9.2')
    }
    """.stripIndent()
  }

  def "can resolve a dependency from the public wetransform repository"() {
    given: "A test class"
    createTestClass()

    when:
    def result = runTask('test')

    then:
    result.task(":test").outcome == TaskOutcome.SUCCESS
  }

  def "creates a version lockfile if prompted to write locks"() {
    setup:
    buildFile << """
    wetransform {
      setup()
    }
    """.stripIndent()

    when:
    def result = runTask('dependencies', '--write-locks')

    then:
    result.task(":dependencies").outcome == TaskOutcome.SUCCESS
    def lockFile = new File(testProjectDir, "gradle.lockfile")
    assert lockFile.exists()
    assert lockFile.text.contains("eu.esdihumboldt.hale:eu.esdihumboldt.util.config:6.2.2=")
  }

  def "creates no version lockfile if disabled"() {
    setup:
    buildFile << """
    wetransform {
      setup {
        deactivateDependencyLocking()
      }
    }
    """.stripIndent()

    when:
    def result = runTask('dependencies', '--write-locks')

    then:
    result.task(":dependencies").outcome == TaskOutcome.SUCCESS
    def lockFile = new File(testProjectDir, "gradle.lockfile")
    assert !lockFile.exists()
  }

  def "creates no version lockfile if not prompted to write locks"() {
    setup:
    buildFile << """
    wetransform {
      setup()
    }
    """.stripIndent()

    when:
    def result = runTask('dependencies')

    then:
    result.task(":dependencies").outcome == TaskOutcome.SUCCESS
    def lockFile = new File(testProjectDir, "gradle.lockfile")
    assert !lockFile.exists()
  }

  private void createTestClass() {
    def testFile = new File(testProjectDir, "src/test/java/com/myorg/MyTest.java")
    testFile.parentFile.mkdirs()
    testFile.text = """
    package com.myorg;
    import org.junit.jupiter.api.Test;
    import static org.junit.jupiter.api.Assertions.assertEquals;
    import eu.esdihumboldt.util.config.Config;

    public class MyTest {
      @Test
      public void test() {
        assertEquals("Config", Config.class.getSimpleName());
      }
    }
    """.stripIndent()
  }
}
