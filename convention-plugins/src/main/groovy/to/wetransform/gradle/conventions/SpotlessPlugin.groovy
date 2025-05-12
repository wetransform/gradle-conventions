/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import javax.inject.Inject

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension

class SpotlessPlugin implements Plugin<Project> {

  private final SpotlessConfig config

  @Inject
  SpotlessPlugin(ObjectFactory objectFactory) {
    this(objectFactory.newInstance(SpotlessConfig))
  }

  SpotlessPlugin(SpotlessConfig config) {
    this.config = config
  }

  @Override
  void apply(Project project) {
    config.disable.convention(false)
    config.licenseHeader.convention(SpotlessConfig.LICENSE_HEADER_DEFAULT)

    if (config.disable.get()) {
      return
    }

    project.plugins.apply('com.diffplug.spotless')

    EditorConfigHelper ech = new EditorConfigHelper(project.rootProject.projectDir)

    project.extensions.configure(SpotlessExtension) { spotless ->
      def gradleConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'build.gradle'))

      spotless.groovyGradle {
        it.target '*.gradle'
        it.greclipse().configProperties(
          EclipseFormatProperties.getPropertiesAsString(gradleConfig, true)
          )

        applyGenericSettings(it, gradleConfig)
      }

      def genericConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'README.md'))

      spotless.format('other') {
        target '*.md', '.gitignore'

        applyGenericSettings(it, genericConfig)
      }

      def hasGroovy = ProjectHelper.hasGroovy(project)
      def hasJava = ProjectHelper.hasJava(project)

      // check if the groovy plugin is applied
      if (hasGroovy) {
        def groovyConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'src/main/groovy/test.groovy'))

        spotless.groovy {
          it.toggleOffOn()

          it.importOrder('groovy', 'java', 'javax', 'org', 'com', '')

          // https://github.com/diffplug/spotless/tree/main/plugin-gradle#eclipse-groovy
          it.greclipse().configProperties(EclipseFormatProperties.getPropertiesAsString(groovyConfig, true))

          // excludes all Java sources within the Groovy source dirs from formatting
          it.excludeJava()

          applyLicenseHeader(it, project)
          applyGenericSettings(it, groovyConfig)
        }
      }

      if (hasJava || hasGroovy) {
        def javaConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'src/main/java/test.java'))

        spotless.java {
          it.toggleOffOn()

          it.eclipse().configProperties(
            EclipseFormatProperties.getPropertiesAsString(javaConfig, false)
            )

          it.importOrder('java', 'javax', 'org', 'com', '')

          it.removeUnusedImports()

          applyLicenseHeader(it, project)
          applyGenericSettings(it, javaConfig)

          // ignore generated files
          it.targetExclude 'src-gen*/**', 'build/**'
        }
      }

      def hasKotlin = ProjectHelper.hasKotlin(project)
      if (hasKotlin) {
        def kotlinConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'src/main/kotlin/test.kt'))

        spotless.kotlin {
          // target '**/*.kt'

          def ecFile = project.rootProject.file('.editorconfig')
          if (ecFile.exists()) {
            it.ktlint().setEditorConfigPath(ecFile)
          } else {
            it.ktlint()

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
    def header = config.licenseHeader.get()
    if (header) {
      ext.licenseHeader(header)
    }
  }
}
