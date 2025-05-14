/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic

import org.gradle.api.provider.Property

@CompileStatic
interface ConfigProvider {
  SpotlessConfig getSpotlessConfig()
  PublishConfig getPublishConfig()

  Property<String> getJavaVersion()
  Property<String> getScalaVersion()

  Property<Boolean> getActivateDependencyLocking()
}
