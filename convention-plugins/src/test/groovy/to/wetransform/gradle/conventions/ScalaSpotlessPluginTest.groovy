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

import spock.lang.Ignore

class ScalaSpotlessPluginTest extends PluginTest {

  def setup() {
    buildFile << """
    plugins {
      id 'scala'
      id 'to.wetransform.conventions.spotless'
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }

  def "fails Scala formatting error"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.scala]
    indent_style = space
    indent_size = 2
    """.stripIndent()

    new File(testProjectDir, 'src/main/scala').mkdirs()
    new File(testProjectDir, 'src/main/scala/Test.scala') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    package com.example

    object Test {
        def main(args: Array[String]): Unit = {
            println("Indentation wrong")
        }
    }
    """.stripIndent()

    when:
    def result = runTaskWithFailure('spotlessCheck')

    then:
    result.task(":spotlessScalaCheck").outcome == TaskOutcome.FAILED
    result.output.contains('The following files had format violations:')
    result.output.contains('src/main/scala/Test.scala')
  }

  @Ignore("Scala formatting does not support 4 space indentation")
  def "succeeds for correct Scala formatting"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.scala]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/scala').mkdirs()
    new File(testProjectDir, 'src/main/scala/Test.scala') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    package com.example

    object Test {
        def main(args: Array[String]): Unit = {
            println("Indentation correct")
        }
    }
    """.stripIndent()

    when:
    def result = runTask('spotlessCheck')

    then:
    result.task(":spotlessScalaCheck").outcome == TaskOutcome.SUCCESS
  }

  def "succeeds for correct Scala formatting 2 spaces"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.scala]
    indent_style = space
    indent_size = 2
    """.stripIndent()

    new File(testProjectDir, 'src/main/scala').mkdirs()
    new File(testProjectDir, 'src/main/scala/Test.scala') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    package com.example

    object Test {
      def main(args: Array[String]): Unit = {
        println("Indentation wrong")
      }
    }
    """.stripIndent()

    when:
    def result = runTask('spotlessCheck')

    then:
    result.task(":spotlessScalaCheck").outcome == TaskOutcome.SUCCESS
  }
}
