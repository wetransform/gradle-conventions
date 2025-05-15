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

import groovy.transform.CompileStatic

import org.gradle.api.Project

@CompileStatic
class ProjectHelper {

  static boolean hasJava(Project project) {
    return project.plugins.hasPlugin('java') || project.plugins.hasPlugin('java-libary') || hasFolder(project, 'src/main/java') || hasFolder(project, 'src/test/java')
  }

  static boolean hasKotlin(Project project) {
    return project.plugins.hasPlugin('org.jetbrains.kotlin.jvm') || hasFolder(project, 'src/main/kotlin') || hasFolder(project, 'src/test/kotlin')
  }

  static boolean hasGroovy(Project project) {
    return project.plugins.hasPlugin('groovy') || hasFolder(project, 'src/main/groovy') || hasFolder(project, 'src/test/groovy')
  }

  static boolean hasScala(Project project) {
    return project.plugins.hasPlugin('scala') || hasFolder(project, 'src/main/scala') || hasFolder(project, 'src/test/scala')
  }

  static boolean hasFolder(Project project, String folder) {
    return project.file(folder).exists()
  }
}
