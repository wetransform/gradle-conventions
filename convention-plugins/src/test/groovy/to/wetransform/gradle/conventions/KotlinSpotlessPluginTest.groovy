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

class KotlinSpotlessPluginTest extends PluginTest {

  def setup() {
    buildFile << """
    plugins {
      id 'org.jetbrains.kotlin.jvm' version '1.8.0'
      id 'to.wetransform.conventions.spotless'
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }

  def "fails Kotlin formatting error"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.kt]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/kotlin').mkdirs()
    new File(testProjectDir, 'src/main/kotlin/Test.kt') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    package com.example
    fun main() {
    println("Indentation wrong")
    }
    """.stripIndent()

    when:
    def result = runTaskWithFailure('spotlessCheck')

    then:
    result.task(":spotlessKotlinCheck").outcome == TaskOutcome.FAILED
    result.output.contains('The following files had format violations:')
    result.output.contains('src/main/kotlin/Test.kt')
  }

  def "succeeds for correct Kotlin formatting"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.kt]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/kotlin').mkdirs()
    new File(testProjectDir, 'src/main/kotlin/Test.kt') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    package com.example

    fun main() {
        println("Indentation correct")
    }
    """.stripIndent()

    when:
    def result = runTask('spotlessCheck')

    then:
    result.task(":spotlessKotlinCheck").outcome == TaskOutcome.SUCCESS
  }
}
