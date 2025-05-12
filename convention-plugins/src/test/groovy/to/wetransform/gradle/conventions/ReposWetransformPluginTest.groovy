/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import org.gradle.testkit.runner.TaskOutcome

class ReposWetransformPluginTest extends PluginTest {

  def setup() {
    buildFile.text = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions'
    }

    wetransform {
      repos {
        mavenCentral()
        wetfPublic()
      }
    }

    dependencies {
      implementation('eu.esdihumboldt.hale:eu.esdihumboldt.util.config:6.2.2')
      testImplementation('org.junit.jupiter:junit-jupiter-api:5.9.2')
    }
    """.stripIndent()
  }

  def "can resolve a dependency from the public wetransform repository"() {
    given: "A test class"
    createTestClass()

    when:
    def result = runTask('test')

    then:
    result.task(":test").outcome == TaskOutcome.SUCCESS
  }

  private void createTestClass() {
    def testFile = new File(testProjectDir, "src/test/java/com/myorg/MyTest.java")
    testFile.parentFile.mkdirs()
    testFile.text = """
    package com.myorg;
    import org.junit.jupiter.api.Test;
    import static org.junit.jupiter.api.Assertions.assertEquals;
    import eu.esdihumboldt.util.config.Config;

    public class MyTest {
      @Test
      public void test() {
        assertEquals("Config", Config.class.getSimpleName());
      }
    }
    """.stripIndent()
  }
}
