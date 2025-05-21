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

import groovy.xml.XmlSlurper

import org.gradle.testkit.runner.TaskOutcome

class MavenPublishWetransformPluginTest extends PluginTest {

  def setup() {
    buildFile << """
    plugins {
      id 'java-library'
      id 'to.wetransform.conventions'
    }

    wetransform {
      setup {
        spotless {
          disable = true
        }
      }
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }

  def "artifacts are published to local Maven repo"() {
    when:
    def result = runTask('publishToMavenLocal')

    then:
    result.task(':publishToMavenLocal').outcome == TaskOutcome.SUCCESS

    and:
    new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test/1.0.0-SNAPSHOT/test-1.0.0-SNAPSHOT.jar").exists()
    new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test/1.0.0-SNAPSHOT/test-1.0.0-SNAPSHOT-sources.jar").exists()
    def pomFile = new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test/1.0.0-SNAPSHOT/test-1.0.0-SNAPSHOT.pom")
    pomFile.exists()
    validatePom(pomFile)
  }

  def "Scala artifacts are published to local Maven repo"() {
    setup:
    settingsFile.text = "rootProject.name = 'test-scala'"
    buildFile.text = """
    plugins {
      id 'scala'
      id 'maven-publish'
      id 'to.wetransform.conventions'
    }
    wetransform {
      setup {
        scalaVersion('2.13')
      }
    }
    repositories {
      mavenCentral()
    }
    dependencies {
      implementation 'org.scala-lang:scala-library:2.13.6'

      implementation "org.clapper:grizzled-slf4j_\${scalaVersion}:1.3.4"
    }
    """.stripIndent()

    when:
    def result = runTask('publishToMavenLocal')

    then:
    result.task(':publishToMavenLocal').outcome == TaskOutcome.SUCCESS

    and:
    new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test-scala_2.13/1.0.0-SNAPSHOT/test-scala_2.13-1.0.0-SNAPSHOT.jar").exists()
    new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test-scala_2.13/1.0.0-SNAPSHOT/test-scala_2.13-1.0.0-SNAPSHOT-sources.jar").exists()
    def pomFile = new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test-scala_2.13/1.0.0-SNAPSHOT/test-scala_2.13-1.0.0-SNAPSHOT.pom")
    pomFile.exists()
    validatePom(pomFile, 'test-scala')
  }

  def "BOM artifacts are published to local Maven repo"() {
    setup:
    settingsFile.text = "rootProject.name = 'test-bom'"
    buildFile.text = """
    plugins {
      id 'java-platform'
      id 'maven-publish'
      id 'to.wetransform.conventions'
    }
    wetransform {
      setup()
    }
    repositories {
      mavenCentral()
    }
    javaPlatform {
      allowDependencies() // for including other BOMs
    }
    dependencies {
      api(platform("org.slf4j:slf4j-bom:2.0.17"))

      api "ch.qos.logback:logback-classic:1.5.18"
    }
    """.stripIndent()

    when:
    def result = runTask('publishToMavenLocal')

    then:
    result.task(':publishToMavenLocal').outcome == TaskOutcome.SUCCESS

    and:
    !new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test-bom/1.0.0-SNAPSHOT/test-bom-1.0.0-SNAPSHOT.jar").exists()
    !new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test-bom/1.0.0-SNAPSHOT/test-bom-1.0.0-SNAPSHOT-sources.jar").exists()
    def pomFile = new File("${System.getProperty('user.home')}/.m2/repository/to/wetransform/test-bom/1.0.0-SNAPSHOT/test-bom-1.0.0-SNAPSHOT.pom")
    pomFile.exists()
    validatePom(pomFile, 'test-bom')
  }

  void validatePom(File pomFile, String projectName = "test") {
    def pom = new XmlSlurper().parse(pomFile)
    assert pom.scm.url.text() == "https://github.com/wetransform/${projectName}", "SCM URL in POM is incorrect"
  }
}
