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

  def "generates script to run spotless"() {
    when:
    def result = runTask('generateSpotlessScript')

    then:
    result.task(":generateSpotlessScript").outcome == TaskOutcome.SUCCESS
    def scriptFile = new File(testProjectDir, 'spotless.sh')
    scriptFile.exists()
    scriptFile.text.contains('spotlessApply')
  }
}
