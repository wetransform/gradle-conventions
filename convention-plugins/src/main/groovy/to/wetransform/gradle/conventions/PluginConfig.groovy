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
