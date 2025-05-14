/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
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
