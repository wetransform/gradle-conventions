package to.wetransform.gradle.conventions

import org.gradle.api.Project

class RepoConfig {

  private final Project project

  RepoConfig(Project project) {
    this.project = project
  }

  void wetfPublic() {
    project.repositories {
      maven {
        url 'https://artifactory.wetransform.to/artifactory/local'
      }
    }
  }

  void wetfPrivate() {
    project.repositories {
      maven { // wetransform internal release repository
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
