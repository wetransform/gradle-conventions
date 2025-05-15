/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
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

  def "succeeds for correct Java formatting 2 spaces"() {
    given:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.java]
    indent_style = space
    indent_size = 2
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

  def "succeeds for correct Java formatting with custom header"() {
    setup:
    buildFile.text = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions'
    }

    repositories {
      mavenCentral()
    }

    wetransform {
      setup {
        spotless {
          licenseHeader = APACHE
        }
      }
    }
    """.stripIndent()

    and:
    new File(testProjectDir, '.editorconfig') << """
    root = true

    [*.java]
    indent_style = space
    indent_size = 4
    """.stripIndent()

    new File(testProjectDir, 'src/main/java').mkdirs()
    new File(testProjectDir, 'src/main/java/Test.java') << """\
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
