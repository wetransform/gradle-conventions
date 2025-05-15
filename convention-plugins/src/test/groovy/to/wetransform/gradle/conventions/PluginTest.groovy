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
