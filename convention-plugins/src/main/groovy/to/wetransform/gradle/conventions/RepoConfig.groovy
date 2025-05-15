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

import org.gradle.api.Project

class RepoConfig {

  private final Project project

  RepoConfig(Project project) {
    this.project = project
  }

  /**
   * Add the wetransform public repository.
   */
  void wetfPublic() {
    project.repositories {
      maven {
        url 'https://artifactory.wetransform.to/artifactory/local'
      }
    }
  }

  /**
   * Add the wetransform private repository.
   */
  void wetfPrivate() {
    project.repositories {
      maven {
        // wetransform internal release repository
        url 'https://artifactory.wetransform.to/artifactory/private'
        credentials {
          username project.hasProperty('wetfArtifactoryUser') ? project.property('wetfArtifactoryUser') : ''
          password project.hasProperty('wetfArtifactoryPassword') ? project.property('wetfArtifactoryPassword') : ''
        }
      }
    }
  }

  // commonly used general repos for convenience

  void mavenCentral() {
    project.repositories {
      mavenCentral()
    }
  }

  void mavenLocal() {
    project.repositories {
      mavenLocal()
    }
  }
}
