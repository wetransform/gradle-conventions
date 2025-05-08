package to.wetransform.gradle.conventions

import com.diffplug.gradle.spotless.FormatExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.diffplug.gradle.spotless.SpotlessExtension

class SpotlessPlugin implements Plugin<Project> {

  static LICENSE_HEADER_DEFAULT = '''/*
 * Copyright (c) $YEAR wetransform GmbH
 * All rights reserved.
 */
'''

  @Override
  void apply(Project project) {
    project.plugins.apply('com.diffplug.spotless')

    EditorConfigHelper ech = new EditorConfigHelper(project.rootProject.projectDir)

    project.extensions.configure(SpotlessExtension) { spotless ->
      def gradleConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'build.gradle'))

      spotless.groovyGradle {
        target '*.gradle'
        greclipse()/*.configProperties(
          ResourceUtil.getResourceAsString('gradle-format.properties')
        )*/

        applyGenericSettings(it, gradleConfig)
      }

      def genericConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'README.md'))

      spotless.format('other') {
        target '*.md', '.gitignore'

        applyGenericSettings(it, genericConfig)
      }

      def hasGroovy = project.plugins.hasPlugin('groovy')
      def hasJava = project.plugins.hasPlugin('java')

      // check if the groovy plugin is applied
      if (hasGroovy) {
        def groovyConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'src/main/groovy/test.groovy'))

        spotless.groovy {
          toggleOffOn()

          importOrder('groovy', 'java', 'javax', 'org', 'com', '')

          // https://github.com/diffplug/spotless/tree/main/plugin-gradle#eclipse-groovy
          greclipse()

          // excludes all Java sources within the Groovy source dirs from formatting
          excludeJava()

          applyLicenseHeader(it, project)
          applyGenericSettings(it, groovyConfig)
        }
      }

      if (hasJava || hasGroovy) {
        def javaConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'src/main/java/test.java'))

        spotless.java {
          toggleOffOn()

          palantirJavaFormat()

          importOrder('java', 'javax', 'org', 'com', '')

          removeUnusedImports()

          applyLicenseHeader(it, project)
          applyGenericSettings(it, javaConfig)

          // ignore generated files
          targetExclude 'src-gen*/**', 'build/**'
        }
      }

      def hasKotlin = project.plugins.hasPlugin('org.jetbrains.kotlin.jvm')
      if (hasKotlin) {
        def kotlinConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'src/main/kotlin/test.kt'))

        spotless.kotlin {
          // target '**/*.kt'

          def ecFile = project.rootProject.file('.editorconfig')
          if (ecFile.exists()) {
            ktlint().setEditorConfigPath(ecFile)
          } else {
            ktlint()

            applyGenericSettings(it, kotlinConfig)
          }

          applyLicenseHeader(it, project)
        }
      }
    }
  }

  void applyGenericSettings(FormatExtension ext, EditorConfigInfo config) {
    if (config.trimTrailingWhitespace()) {
      ext.trimTrailingWhitespace()
    }
    if (config.insertFinalNewline()) {
      ext.endWithNewline()
    }
    if (config.indentWithSpaces()) {
      ext.leadingTabsToSpaces(config.indentSize())
    } else {
      ext.leadingSpacesToTabs(config.indentSize())
    }
    if (config.endOfLine() != null) {
      ext.lineEndings = config.lineEnding
    }
    if (config.charset() != null) {
      ext.encoding(config.charset())
    }
  }

  def void applyLicenseHeader(FormatExtension ext, Project project) {
    //TODO configurable?
    ext.licenseHeader(LICENSE_HEADER_DEFAULT)
  }
}
