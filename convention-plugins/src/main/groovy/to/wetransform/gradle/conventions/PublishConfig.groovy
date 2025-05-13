/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface PublishConfig {
  /**
   * @return <code>true</code> if no publishing configuration should be applied, default is <code>false</code>
   */
  Property<Boolean> getDisable()

  /**
   * @return <code>true</code> if the project should be published to to a private repository, which is the default
   */
  Property<Boolean> getPrivateRepo()
  /**
   * Set to manually enable Maven publishing.
   */
  Property<Boolean> getEnableMaven()
  /**
   * Set to manually enable Docker publishing.
   */
  Property<Boolean> getEnableDocker()

  /**
   * Skip defining the MavenPublication, but still publish the artifact.
   * Intended for cases where the MavenPublication is intended to be defined in a different way or is already defined.
   */
  Property<Boolean> getSkipDefineMavenPublication()

  /**
   * The URL pointing to the source code of the project, by default derived from repo owner and repo name.
   */
  Property<String> getSourceUrl()
  /**
   * The repository owner (on GitHub), defaults to wetransform.
   */
  Property<String> getRepoOwner()
  /**
   * The repository name (on GitHub), defaults to the project name.
   */
  Property<String> getRepoName()

  /**
   * The project artifact group, defaults to 'org.wetransform'.
   */
  Property<String> getGroup()

  /**
   * The base image for the Docker image, if set also enables Docker publishing.
   */
  Property<String> getBaseImage()
  /**
   * The Docker image name, defaults to 'wetransform/<project-name>'.
   */
  Property<String> getImageName()

  /**
   * The main Java class to run in the Docker image.
   */
  Property<String> getMainClass()

  /**
   * The ports to expose in the Docker image.
   */
  SetProperty<Integer> getPorts()
}
