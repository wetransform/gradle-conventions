/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import org.gradle.testkit.runner.TaskOutcome

class DockerPublishWetransformPluginTest extends PluginTest {

  def setup() {
    buildFile << """
    plugins {
      id 'java' // required
      id 'to.wetransform.conventions'
    }

    wetransform {
      setup {
        publish {
          baseImage = 'eclipse-temurin:17-jre-jammy'
          mainClass = 'to.wetransform.test.Main'
        }
      }
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }

  def "docker image is built and tagged"() {
    given:
    // Copy gradle.properties from the user's home directory if it exists - to make sure any settings from there are considered
    // (e.g. dockerHost).
    File userGradleProperties = new File(System.getProperty("user.home"), ".gradle/gradle.properties")
    if (userGradleProperties.exists()) {
      File testGradleProperties = new File(testProjectDir, "gradle.properties")
      testGradleProperties << userGradleProperties.text
    }

    new File(testProjectDir, 'src/main/java/to/wetransform/test').mkdirs()
    new File(testProjectDir, 'src/main/java/to/wetransform/test/Main.java') << """
    package to.wetransform.test;
    public class Main {
      public static void main(String[] args) {
        System.out.println("Hello, World!");
      }
    }
    """.stripIndent()

    when:
    def result = runTask('dockerBuildImage')

    then:
    result.task(':dockerBuildImage').outcome == TaskOutcome.SUCCESS

    and:
    def dockerfile = new File("${testProjectDir}/build/docker/Dockerfile")
    dockerfile.exists()
    validateDockerfile(dockerfile)
  }

  void validateDockerfile(File dockerfile) {
    def content = dockerfile.text

    println "Dockerfile content:\n${content}"

    assert content.contains("FROM eclipse-temurin:17-jre-jammy"), "Base image is incorrect"
    assert content.contains("LABEL org.opencontainers.image.source"), "OCI annotations are missing"
    assert content.contains("org.opencontainers.image.version"), "OCI annotations are missing"

    // check source and version label values
    def sourceLabel = (content =~ /LABEL org\.opencontainers\.image\.source=([^\s]+)/)[0][1]
    def versionLabel = (content =~ /LABEL .+ org\.opencontainers\.image\.version=([^\s]+)/)[0][1]
    assert sourceLabel == 'https://github.com/wetransform/test'
    assert versionLabel == '1.0.0-SNAPSHOT'
  }
}
