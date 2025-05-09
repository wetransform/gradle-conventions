package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

@CompileStatic
class WetransformPlugin implements Plugin<Project>, ConfigProvider {

  private final SpotlessConfig spotlessConfig

  private final PublishConfig publishConfig

  private final ObjectFactory objectFactory

  @Inject
  WetransformPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory

    this.spotlessConfig = objectFactory.newInstance(SpotlessConfig)
    this.publishConfig = objectFactory.newInstance(PublishConfig)
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
}
