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

  private final ObjectFactory objectFactory

  @Inject
  WetransformPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory

    this.spotlessConfig = objectFactory.newInstance(SpotlessConfig)
    this.publishConfig = objectFactory.newInstance(PublishConfig)
    this.javaVersion = objectFactory.property(String)
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
          kotlin.jvmToolChain.languageVersion.set(JavaLanguageVersion.of(javaVersion.get()))
        }
      }
    }

    project.configurations.configureEach {
      // ensure SNAPSHOTs are updated every time if needed
      it.resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }
  }
}
