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
    def scriptFileWin = new File(testProjectDir, 'spotless.bat')
    scriptFileWin.exists()
    scriptFileWin.text.contains('spotlessApply')
  }

  def "auto-generates watcher when property is set and script exists"() {
    given:
    buildFile.text = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions.spotless'
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()
    new File(testProjectDir, 'spotless.sh') << '#!/bin/sh'

    when:
    def result = runTask('classes', "-P${SpotlessPlugin.PROPERTY_AUTO_WATCHER}=true")

    then:
    result.task(':generateSpotlessWatcher').outcome == TaskOutcome.SUCCESS
    new File(testProjectDir, '.idea/watcherTasks.xml').exists()
  }

  def "does not auto-generate watcher when property is set but no script exists"() {
    given:
    buildFile.text = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions.spotless'
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()
    // spotless.sh intentionally NOT created

    when:
    def result = runTask('classes', "-P${SpotlessPlugin.PROPERTY_AUTO_WATCHER}=true")

    then:
    result.task(':generateSpotlessWatcher') == null
  }

  def "does not auto-generate watcher when script exists but property is not set"() {
    given:
    buildFile.text = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions.spotless'
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()
    new File(testProjectDir, 'spotless.sh') << '#!/bin/sh'
    // wetf.spotless.autoWatcher property intentionally NOT passed

    when:
    def result = runTask('classes')

    then:
    result.task(':generateSpotlessWatcher') == null
  }

  def "auto-generates watcher exactly once when plugin applied to multiple subprojects"() {
    given:
    settingsFile.text = """
    rootProject.name = 'test'
    include 'subA', 'subB'
    """.stripIndent()
    buildFile.text = ''

    def subBuild = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions.spotless'
    }
    repositories {
      mavenCentral()
    }
    """.stripIndent()
    new File(testProjectDir, 'subA').mkdirs()
    new File(testProjectDir, 'subA/build.gradle') << subBuild
    new File(testProjectDir, 'subB').mkdirs()
    new File(testProjectDir, 'subB/build.gradle') << subBuild
    new File(testProjectDir, 'spotless.sh') << '#!/bin/sh'

    when:
    def result = runTask(':subA:classes', ':subB:classes', '--parallel', "-P${SpotlessPlugin.PROPERTY_AUTO_WATCHER}=true")

    then:
    // exactly one watcher task at root level
    result.task(':generateSpotlessWatcher').outcome == TaskOutcome.SUCCESS
    // no per-subproject watcher tasks
    result.task(':subA:generateSpotlessWatcher') == null
    result.task(':subB:generateSpotlessWatcher') == null
    new File(testProjectDir, '.idea/watcherTasks.xml').exists()
  }
}
