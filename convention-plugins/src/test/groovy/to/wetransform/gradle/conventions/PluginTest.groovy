/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import org.gradle.testkit.runner.GradleRunner

import spock.lang.Specification
import spock.lang.TempDir

abstract class PluginTest extends Specification {
  @TempDir
  File testProjectDir
  File settingsFile
  File buildFile

  def setup() {
    settingsFile = new File(testProjectDir, 'settings.gradle').tap { it << "rootProject.name = 'test'" }
    buildFile = new File(testProjectDir, 'build.gradle')
  }

  def runTask(String task, String... args) {
    def arguments = [task, '--stacktrace'] + args.toList()

    return GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments(arguments)
      .withPluginClasspath()
      .build()
  }

  def runTaskWithFailure(String task) {
    return GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments(task, '--stacktrace')
      .withPluginClasspath()
      .buildAndFail()
  }
}
