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

  void validatePom(File pomFile) {
    def pom = new XmlSlurper().parse(pomFile)
    assert pom.scm.url.text() == "https://github.com/wetransform/test", "SCM URL in POM is incorrect"
  }

}
