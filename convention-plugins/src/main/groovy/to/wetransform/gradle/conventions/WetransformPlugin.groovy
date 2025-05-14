/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import javax.inject.Inject

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLanguageVersion

@CompileStatic
class WetransformPlugin implements Plugin<Project>, ConfigProvider {

  private final SpotlessConfig spotlessConfig

  private final PublishConfig publishConfig

  private final Property<String> javaVersion

  private final Property<String> scalaVersion

  private final Property<Boolean> activateDependencyLocking

  private final ObjectFactory objectFactory

  @Inject
  WetransformPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory

    this.spotlessConfig = objectFactory.newInstance(SpotlessConfig)
    this.publishConfig = objectFactory.newInstance(PublishConfig)
    this.javaVersion = objectFactory.property(String)
    this.scalaVersion = objectFactory.property(String)
    this.activateDependencyLocking = objectFactory.property(Boolean)
  }

  @Override
  void apply(Project target) {
    PluginExtension ext = objectFactory.newInstance(PluginExtension, target, this, objectFactory)
    target.extensions.add("wetransform", ext)
  }

  @Override
  SpotlessConfig getSpotlessConfig() {
    return spotlessConfig
  }

  @Override
  PublishConfig getPublishConfig() {
    return publishConfig
  }

  @Override
  Property<String> getJavaVersion() {
    return javaVersion
  }

  @Override
  Property<String> getScalaVersion() {
    return scalaVersion
  }

  @Override
  Property<Boolean> getActivateDependencyLocking() {
    return activateDependencyLocking
  }

  @CompileStatic(TypeCheckingMode.SKIP) // skip because Kotlin extension type is not available at compile time
  def void setup(Project project) {
    if (javaVersion.isPresent()) {
      if (ProjectHelper.hasJava(project)) {
        project.extensions.configure(JavaPluginExtension) { java ->
          java.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.get()))
        }
      }

      if (ProjectHelper.hasKotlin(project)) {
        // org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
        project.extensions.configure('kotlin') { kotlin ->
          kotlin.jvmToolchain(JavaLanguageVersion.of(javaVersion.get()).asInt())
        }
      }
    }

    project.configurations.configureEach {
      // ensure SNAPSHOTs are updated every time if needed
      it.resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }

    // scala version
    if (scalaVersion.isPresent()) {
      def ext = project.extensions.findByName('ext')
      if (ext != null) {
        ext.scalaVersion = scalaVersion.get()
      }
    }

    // dependency locking for lockfile generation (used for trivy security scan)
    boolean hasConfigs = project.configurations.findByName('compileClasspath') != null &&
      project.configurations.findByName('runtimeClasspath') != null

    if (hasConfigs) {
      // by default only apply if 3 or less total projects, because of the overhead of generating the lockfile
      activateDependencyLocking.convention(project.rootProject.subprojects.size() <= 2)

      if (activateDependencyLocking.get()) {
        project.configurations {
          compileClasspath {
            resolutionStrategy.activateDependencyLocking()
          }
          runtimeClasspath {
            resolutionStrategy.activateDependencyLocking()
          }
        }
      }
    }
  }
}
