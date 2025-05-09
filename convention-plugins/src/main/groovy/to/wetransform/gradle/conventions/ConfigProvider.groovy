package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic

@CompileStatic
interface ConfigProvider {
  SpotlessConfig getSpotlessConfig()
  PublishConfig getPublishConfig()
}
