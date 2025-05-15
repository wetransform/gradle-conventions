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

import javax.inject.Inject

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

class PublishPlugin implements Plugin<Project> {

  private final PublishConfig config

  private final Property<String> scalaVersion

  @Inject
  PublishPlugin(ObjectFactory objectFactory) {
    this(objectFactory, objectFactory.newInstance(PublishConfig), null)
  }

  PublishPlugin(ObjectFactory objectFactory, PublishConfig config, ConfigProvider configProvider) {
    this.config = config
    this.scalaVersion = configProvider?.scalaVersion ?: objectFactory.property(String)
  }

  @Override
  void apply(Project target) {
    config.disable.convention(false)

    if (config.disable.get()) {
      return
    }

    // is maven-publish plugin applied?
    def hasMavenPlugin = target.plugins.hasPlugin('maven-publish')
    // or the java-library plugin?
    def hasJavaLibPlugin = target.plugins.hasPlugin('java-library')

    config.enableMaven.convention(hasMavenPlugin || hasJavaLibPlugin)

    // is docker plugin applied?
    def hasDockerPlugin = target.plugins.hasPlugin('com.bmuschko.docker-java-application')
    // is a base image property configured?
    def isBaseImageSet = config.baseImage.isPresent()

    config.enableDocker.convention(hasDockerPlugin || isBaseImageSet)

    config.privateRepo.convention(true)

    config.repoOwner.convention("wetransform")
    config.repoName.convention(target.rootProject.name)
    config.sourceUrl.convention(target.providers.provider {
      def repoOwner = config.repoOwner.get()
      def repoName = config.repoName.get()
      return "https://github.com/${repoOwner}/${repoName}"
    })

    config.group.convention('to.wetransform')

    config.imageName.convention("wetransform/${target.rootProject.name}")

    config.skipDefineMavenPublication.convention(false)

    // enable semantic versioning plugin
    target.plugins.apply('to.wetransform.semantic-release-version')

    // set group only when not already set
    // by default the value seems to be the root project name
    if (!target.group || target.group == target.rootProject.name) {
      target.group = config.group.get()
    }

    // configure for Maven publishing
    if (config.enableMaven.get()) {
      if (!hasMavenPlugin) {
        target.plugins.apply('maven-publish')
      }
      configureMavenPublish(target)
    }

    // configure for Docker publishing
    if (config.enableDocker.get()) {
      if (!hasDockerPlugin) {
        target.plugins.apply('com.bmuschko.docker-java-application')
      }
      target.plugins.apply('org.ajoberstar.grgit')
      configureDockerPublish(target)
    }
  }

  def void configureMavenPublish(Project project) {
    // Create sourcesJar task
    project.tasks.register('sourcesJar', Jar) { task ->
      task.from(project.sourceSets.main.allSource)
      task.archiveClassifier.set('sources')
    }

    // Configure publishing
    project.publishing {
      publications {
        if (!config.skipDefineMavenPublication.get()) {
          maven(MavenPublication) {
            from(project.components.java)
            artifact(project.tasks.named('sourcesJar').get())

            if (scalaVersion.isPresent()) {
              artifactId = "${artifactId}_${scalaVersion.get()}"
            }

            pom {
              scm {
                // add SCM info - required for changelogs in Renovate
                url = config.sourceUrl.get()
                def scmUrl = "scm:git:git@github.com:${config.repoOwner.get()}/${config.repoName.get()}.git"
                connection = scmUrl
                developerConnection = scmUrl
              }
            }
          }
        }
      }

      repositories {
        maven {
          def prefix = config.privateRepo.get() ? 'private' : 'libs'
          url = project.uri(
            "https://artifactory.wetransform.to/artifactory/" +
            (project.version.toString().endsWith('-SNAPSHOT') ? "${prefix}-snapshot-local" : "${prefix}-release-local")
            )
          credentials {
            username = project.hasProperty('wetfArtifactoryUser') ? project.property('wetfArtifactoryUser') : ''
            password = project.hasProperty('wetfArtifactoryPassword') ? project.property('wetfArtifactoryPassword') : ''
          }
        }
      }
    }
  }

  def void configureDockerPublish(Project project) {
    project.extensions.configure(com.bmuschko.gradle.docker.DockerExtension) { docker ->
      docker.javaApplication {
        if (config.baseImage.isPresent()) {
          baseImage = config.baseImage.get()
        }
        if (config.mainClass.isPresent()) {
          mainClassName = config.mainClass
        }
        ports = config.ports.convention([])
        images = [
          "${config.imageName.get()}:${project.version}",
          "${config.imageName.get()}:latest"
        ]
      }

      docker.url = project.hasProperty('dockerHost') ? project.property('dockerHost') : 'http://localdocker:2375'

      docker.registryCredentials {
        url = 'https://registry-1.docker.io/v2'
        username = project.hasProperty('dockerHubUsername') ? project.property('dockerHubUsername') : ''
        password = project.hasProperty('dockerHubPassword') ? project.property('dockerHubPassword') : ''
        email = project.hasProperty('dockerHubEmail') ? project.property('dockerHubEmail') : ''
      }
    }

    def buildTime = java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_INSTANT)

    project.tasks.named('dockerCreateDockerfile') { task ->
      task.doFirst {
        def grgit = project.grgit
        task.label(
          // OCI annotations
          'org.opencontainers.image.source': config.sourceUrl.get(),
          'org.opencontainers.image.revision': grgit?.head()?.id ?: 'unknown',
          'org.opencontainers.image.version': project.version,
          'org.opencontainers.image.created': buildTime,
          // custom
          'git.branch': grgit?.branch?.current?.name ?: 'unknown'
          )
      }
    }

    // define default derived tasks

    project.tasks.register('dockerTagLatest') {
      it.dependsOn 'dockerBuildImage'
    }

    project.tasks.register('dockerPushLatest') {
      it.dependsOn 'dockerPushImage'
    }
  }
}
