/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import org.gradle.testkit.runner.TaskOutcome

class SpotlessPluginTest extends PluginTest {

  def setup() {
    buildFile << """
    plugins {
      id 'to.wetransform.conventions.spotless'
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }

  def "fails .gradle formatting error"() {
    given:
    new File(testProjectDir, 'test.gradle') << """
    if (true) {
        println 'Indentation wrong'
    }
    """.stripIndent()

    when:
    def result = runTaskWithFailure('spotlessCheck')

    then:
    result.task(":spotlessGroovyGradleCheck").outcome == TaskOutcome.FAILED
    result.output.contains('The following files had format violations:')
    result.output.contains('test.gradle')
  }

  def "succeeds for correct .gradle formatting"() {
    given:
    new File(testProjectDir, 'test.gradle') << """
        if (true) {
          println 'Indentation correct'
        }
        """.stripIndent()

    when:
    def result = runTask('spotlessCheck')

    then:
    // still fails because of settings.gradle
    result.task(":spotlessGroovyGradleCheck").outcome == TaskOutcome.SUCCESS
    !result.output.contains('test.gradle')
  }
}
