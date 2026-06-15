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

class JacocoWetransformPluginTest extends PluginTest {

  def "jacoco is applied by default for Java projects"() {
    given:
    buildFile << """
    plugins {
      id 'java'
      id 'to.wetransform.conventions'
    }
    wetransform {
      setup {
        spotless { disable = true }
      }
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()

    when:
    def result = runTask('tasks', '--all')

    then:
    result.task(':tasks').outcome == TaskOutcome.SUCCESS
    result.output.contains('jacocoTestReport')
  }

  def "jacocoTestReport runs as part of the test task"() {
    given:
    buildFile << """
    plugins {
      id 'java'
      id 'to.wetransform.conventions'
    }
    wetransform {
      setup {
        spotless { disable = true }
      }
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()
    def srcDir = new File(testProjectDir, 'src/main/java/example')
    srcDir.mkdirs()
    new File(srcDir, 'Hello.java') << '''
    package example;
    public class Hello {
      public String greet() { return "hi"; }
    }
    '''.stripIndent()
    def testDir = new File(testProjectDir, 'src/test/java/example')
    testDir.mkdirs()
    new File(testDir, 'HelloTest.java') << '''
    package example;
    import org.junit.jupiter.api.Test;
    import static org.junit.jupiter.api.Assertions.assertEquals;
    public class HelloTest {
      @Test
      public void greet() { assertEquals("hi", new Hello().greet()); }
    }
    '''.stripIndent()
    buildFile << """
    test { useJUnitPlatform() }
    dependencies {
      testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
      testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    }
    """.stripIndent()

    when:
    def result = runTask('test')

    then:
    result.task(':test').outcome == TaskOutcome.SUCCESS
    result.task(':jacocoTestReport').outcome == TaskOutcome.SUCCESS
    new File(testProjectDir, 'build/reports/jacoco/test/jacocoTestReport.xml').exists()
  }

  def "jacoco can be disabled via configuration"() {
    given:
    buildFile << """
    plugins {
      id 'java'
      id 'to.wetransform.conventions'
    }
    wetransform {
      setup {
        spotless { disable = true }
        jacoco { disable = true }
      }
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()

    when:
    def result = runTask('tasks', '--all')

    then:
    result.task(':tasks').outcome == TaskOutcome.SUCCESS
    !result.output.contains('jacocoTestReport')
  }

  def "jacoco is not applied for non-JVM projects"() {
    given:
    buildFile << """
    plugins {
      id 'to.wetransform.conventions'
    }
    wetransform {
      setup {
        spotless { disable = true }
      }
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()

    when:
    def result = runTask('tasks', '--all')

    then:
    result.task(':tasks').outcome == TaskOutcome.SUCCESS
    !result.output.contains('jacocoTestReport')
  }
}
