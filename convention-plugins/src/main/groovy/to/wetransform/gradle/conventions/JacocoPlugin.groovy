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

import javax.inject.Inject

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoPlugin implements Plugin<Project> {

  private final JacocoConfig config

  @Inject
  JacocoPlugin(ObjectFactory objectFactory) {
    this(objectFactory, objectFactory.newInstance(JacocoConfig))
  }

  JacocoPlugin(ObjectFactory objectFactory, JacocoConfig config) {
    this.config = config
  }

  @Override
  void apply(Project project) {
    config.disable.convention(false)

    if (config.disable.get()) {
      return
    }

    // JaCoCo only makes sense for JVM projects
    boolean hasJvmSources = ProjectHelper.hasJava(project) ||
      ProjectHelper.hasKotlin(project) ||
      ProjectHelper.hasGroovy(project) ||
      ProjectHelper.hasScala(project)

    if (!hasJvmSources) {
      return
    }

    project.plugins.apply('jacoco')

    if (config.toolVersion.isPresent()) {
      project.extensions.configure(JacocoPluginExtension) { jacoco ->
        jacoco.toolVersion = config.toolVersion.get()
      }
    }

    project.tasks.withType(JacocoReport).configureEach { report ->
      report.reports {
        it.xml.required.set(true)
        it.html.required.set(true)
      }
    }

    // Ensure the jacoco report is generated as part of the test task lifecycle
    project.tasks.matching { it.name == 'test' }.configureEach { testTask ->
      testTask.finalizedBy(project.tasks.matching { it.name == 'jacocoTestReport' })
    }
  }
}
