/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
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
