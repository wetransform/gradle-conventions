package to.wetransform.gradle.conventions

import org.gradle.testkit.runner.TaskOutcome

class JavaSpotlessPluginTest extends PluginTest {

  def setup() {
    buildFile << """
    plugins {
      id 'java'
      id 'to.wetransform.conventions.spotless'
    }

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }

  def "fails Java formatting error"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.java]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/java').mkdirs()
    new File(testProjectDir, 'src/main/java/Test.java') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    public class Test {
      public static void main(String[] args) {
        System.out.println("Indentation wrong");
      }
    }
    """.stripIndent()

    when:
    def result = runTaskWithFailure('spotlessCheck')

    then:
    result.task(":spotlessJavaCheck").outcome == TaskOutcome.FAILED
    result.output.contains('The following files had format violations:')
    result.output.contains('src/main/java/Test.java')
  }

  def "succeeds for correct Java formatting"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.java]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/java').mkdirs()
    new File(testProjectDir, 'src/main/java/Test.java') << """\
    /*
     * Copyright (c) 2025 wetransform GmbH
     * All rights reserved.
     */
    public class Test {
        public static void main(String[] args) {
            System.out.println("Indentation correct");
        }
    }
    """.stripIndent()

    when:
    def result = runTask('spotlessCheck')

    then:
    result.task(":spotlessJavaCheck").outcome == TaskOutcome.SUCCESS
  }

}
