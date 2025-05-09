package to.wetransform.gradle.conventions

import org.gradle.api.provider.Property

interface PublishConfig {
  Property<Boolean> getDisable()

  Property<Boolean> getPrivate()
  Property<Boolean> getEnableMaven()
  Property<Boolean> getEnableDocker()

  Property<String> getSourceUrl()
  Property<String> getRepoOwner()
  Property<String> getRepoName()

  Property<String> getGroup()

  Property<String> getBaseImage()
  Property<String> getImageName()

  Property<String> getMainClass()
}
