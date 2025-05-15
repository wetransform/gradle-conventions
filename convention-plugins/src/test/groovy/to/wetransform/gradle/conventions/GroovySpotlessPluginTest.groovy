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

class GroovySpotlessPluginTest extends PluginTest {

  def setup() {
    buildFile << """
    plugins {
      id 'groovy'
      id 'to.wetransform.conventions.spotless'
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }

  def "fails Groovy formatting error"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.groovy]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/groovy').mkdirs()
    new File(testProjectDir, 'src/main/groovy/Test.groovy') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    class Test {
    def method() {
    println "Indentation wrong"
    }
    }
    """.stripIndent()

    when:
    def result = runTaskWithFailure('spotlessCheck')

    then:
    result.task(":spotlessGroovyCheck").outcome == TaskOutcome.FAILED
    result.output.contains('The following files had format violations:')
    result.output.contains('src/main/groovy/Test.groovy')
  }

  def "succeeds for correct Groovy formatting"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.groovy]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/groovy').mkdirs()
    new File(testProjectDir, 'src/main/groovy/Test.groovy') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    class Test {
        def method() {
            println "Indentation correct"
        }
    }
    """.stripIndent()

    when:
    def result = runTask('spotlessCheck')

    then:
    result.task(":spotlessGroovyCheck").outcome == TaskOutcome.SUCCESS
  }
}
