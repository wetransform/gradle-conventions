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
import org.gradle.api.provider.Property

import com.diffplug.gradle.spotless.BaseKotlinExtension
import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension

class SpotlessPlugin implements Plugin<Project> {

  private final SpotlessConfig config

  private final Property<String> scalaVersion

  @Inject
  SpotlessPlugin(ObjectFactory objectFactory) {
    this(objectFactory, objectFactory.newInstance(SpotlessConfig), null)
  }

  SpotlessPlugin(ObjectFactory objectFactory, SpotlessConfig config, ConfigProvider configProvider) {
    this.config = config
    this.scalaVersion = configProvider?.scalaVersion ?: objectFactory.property(String)
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
          def BaseKotlinExtension.KtlintConfig ktConfig
          if (ecFile.exists()) {
            ktConfig = it.ktlint()
            // Note: Setting the path like this did not work correctly (e.g. instructions to disable ktlint rules did not work)
            // For spotless the default location is the root project anyway, so we do not need to set it
            //.setEditorConfigPath(ecFile)
          } else {
            ktConfig = it.ktlint()

            applyGenericSettings(it, kotlinConfig)
          }

          def defaultOverrides = [
            // by default allow wildcard imports because IntelliJ adds them
            'ktlint_standard_no-wildcard-imports': 'disabled',
          ]

          // filter overrides - don't override if a custom config is present
          def overrides = defaultOverrides.findAll { key, value ->
            !kotlinConfig.hasProperty(key)
          }

          ktConfig.editorConfigOverride(overrides)

          applyLicenseHeader(it, project)
        }
      }

      if (ProjectHelper.hasScala(project)) {
        def scalaConfig = ech.getEditorConfigInfo(new File(project.projectDir, 'src/main/scala/test.scala'))

        spotless.scala {
          it.target '**/*.scala'

          def scf = it.scalafmt()

          if (scalaVersion.isPresent()) {
            scf.scalaMajorVersion(scalaVersion.get())
          }

          def header = config.licenseHeader.get()
          if (header) {
            it.licenseHeader(header, 'package ')
          }

          applyGenericSettings(it, scalaConfig)
        }
      }
    }

    setupTasks(project)
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

  void setupTasks(Project project) {
    project.tasks.register('generateSpotlessScript') {
      it.description = 'Generates a script to run spotless in the root project'

      it.doLast {
        // create spotless.sh script
        def targetFile = project.rootProject.file('spotless.sh')
        copyResourceToFile('/spotless.sh', targetFile)

        // create spotless.bat script for Windows
        def targetFileWin = project.rootProject.file('spotless.bat')
        copyResourceToFile('/spotless.bat', targetFileWin)

        // automatically create the .idea/watcherTasks.xml file
        def ideaFolder = project.rootProject.file('.idea')
        if (ideaFolder.exists()) {
          def ideaFile = new File(ideaFolder, 'watcherTasks.xml')
          if (!ideaFile.exists()) {
            configureWatcherTasks(project, ideaFile)
          }
          else {
            project.logger.warn("File ${ideaFile} already exists. Skipping creation.")
          }
        }
      }
    }

    project.tasks.register('generateSpotlessWatcher') {
      it.description = 'Generates a configuration for the IntelliJ file watcher plugin to run spotless.sh (overrides .idea/watcherTasks.xml)'

      it.doLast {
        def ideaFolder = project.rootProject.file('.idea')
        ideaFolder.mkdirs()
        def ideaFile = new File(ideaFolder, 'watcherTasks.xml')
        configureWatcherTasks(project, ideaFile)
      }
    }
  }

  void copyResourceToFile(String resourcePath, File targetFile) throws IOException {
    def resource = getClass().getResourceAsStream(resourcePath)
    try {
      if (resource) {
        targetFile.withOutputStream { out ->
          out << resource
        }

        // make the script executable for all
        targetFile.setExecutable(true, false)
      } else {
        throw new IllegalStateException("Resource not found: ${resourcePath}")
      }
    } finally {
      resource?.close()
    }
  }

  void configureWatcherTasks(Project project, File targetFile) {
    copyResourceToFile('/watcherTasks.xml', targetFile)

    // determine if Windows
    def isWindows = System.getProperty('os.name').toLowerCase().contains('windows')
    if (isWindows) {
      // replace the sh script with the bat script in the configuration
      def text = targetFile.text
      text = text.replace('spotless.sh', 'spotless.bat')
      targetFile.text = text
    }
  }
}
