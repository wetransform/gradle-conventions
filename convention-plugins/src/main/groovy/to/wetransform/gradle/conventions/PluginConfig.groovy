package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic
import org.gradle.api.Action

import javax.inject.Inject

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
}
