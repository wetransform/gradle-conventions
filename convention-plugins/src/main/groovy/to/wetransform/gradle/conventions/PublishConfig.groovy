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
