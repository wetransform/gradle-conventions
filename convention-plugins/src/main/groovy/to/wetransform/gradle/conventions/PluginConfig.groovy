/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic

import javax.inject.Inject

import org.gradle.api.Action

@CompileStatic
class PluginConfig {

  private final ConfigProvider configProvider

  @Inject
  PluginConfig(ConfigProvider configProvider) {
    this.configProvider = configProvider
  }

  void spotless(Action<SpotlessConfig> action) {
    action.execute(configProvider.spotlessConfig)
  }

  void publish(Action<PublishConfig> action) {
    action.execute(configProvider.publishConfig)
  }

  void javaVersion(def version) {
    configProvider.javaVersion.set(version as String)
  }

  void scalaVersion(def version) {
    configProvider.scalaVersion.set(version as String)
  }

  void activateDependencyLocking() {
    configProvider.activateDependencyLocking.set(true)
  }

  void deactivateDependencyLocking() {
    configProvider.activateDependencyLocking.set(false)
  }
}
