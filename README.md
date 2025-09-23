gradle-conventions
==================

Gradle plugins for applying conventions to Java, Kotlin, and Groovy projects.

To be able to use the plugins, you need to add the following to your `settings.gradle` file in the root project:

```groovy
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      url 'https://artifactory.wetransform.to/artifactory/local'
    }
  }
}
```

When using `settings.gradle.kts`:

```kotlin
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      url = uri("https://artifactory.wetransform.to/artifactory/local")
    }
  }
}
```

## wetransform project conventions

The plugin `to.wetransform.conventions` can be used to apply a number of conventions to a project:

- source code formatting using spotless
- convenient configuration of Maven repositories and Java version
- configuration of publishing to wetransform's Artifactory or Docker Hub
- publishing configuration includes use of semantic versioning based on git tags (uses [gradle-semantic-release-version](https://github.com/wetransform-os/gradle-semantic-release-version/))

A minimal configuration of the plugin looks like this:

```groovy
plugins {
  id 'to.wetransform.conventions' version '<version>'
}

wetransform {
  setup()
}
```

Please note that the call to `setup` is required to apply the conventions, except for the configuration of repositories.
If you need to split the configuration of the plugin into multiple blocks you should make sure to call `setup` only once.
For previous cases you can use the `config` method to configure the plugin in multiple blocks.

Here two examples that are equivalent related to the result, each of them sets the Java version to 17 and applies the conventions.

Configuration in setup call:

```groovy
wetransform {
    setup {
        javaVersion(17)
    }
}
```

Do configuration and later call setup:

```groovy
wetransform {
    config {
        javaVersion(17)
    }
}

wetransform {
    setup()
}
```

### Configure wetransform repositories

```groovy
wetransform {
  repos {
    wetfPublic()  // public wetransform releases and snapshots
    wetfPrivate() // private wetransform releases and snapshots
  }
}
```

See [RepoConfig](./convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/RepoConfig.groovy) for more information on the available repositories.

### Configure the Java version

Preferred way to configure the Java version is to use a `mise.toml` file or `.java-version` file in the root of the project.

Example `mise.toml` file:

```toml
[tools]
java = "temurin-17"
```

For more information see the [mise documentation](https://mise.jdx.dev/lang/java.html).

Alternatively, you can use a `.java-version` file containing the Java version.

If you do not use any of these files, you can set the Java version using the plugin configuration:

```groovy
wetransform {
  setup {
    javaVersion(17)
  }
}
```

### Configure spotless

Formatting with spotless is by default enabled for:

- Java using Eclipse formatter
- Groovy and Groovy Gradle files using Groovy-Eclipse formatter
- Kotlin using ktlint
- Scala using scalafmt (only 2 space indentation supported)

Basic settings can be adapted by providing an `.editorconfig` file in the root of a project.
This can be used to influence the indentation, line endings, and other formatting options.

For configuration options see [SpotlessConfig](./convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/SpotlessConfig.groovy).

Disable spotless formatting:

```groovy
wetransform {
  setup {
    spotless {
      disable = true
    }
  }
}
```

Use a custom license header:

```groovy
wetransform {
  setup {
    spotless {
      licenseHeader = '''\
        /*
         * Copyright (c) $YEAR wetransform GmbH
         */
        '''.stripIndent()
    }
  }
}
```

#### General use of spotless

To check if the rules are met run:

```bash
./gradlew spotlessCheck
```

To automatically format the code run:

```bash
./gradlew spotlessApply
```

You can run the task `generateSpotlessScript` to generate a script that can be used to run apply the formatting:

```bash
./spotless.sh
```

You can also format a single file using the script:

```bash
./spotless.sh <file>
```

#### Integration in IntellJ

There are different ways to integrate the formatting into IntelliJ:

1. Add a file watcher (requires file watchers plugin) to run spotless for individual changed files **(recommended)**, this requires the generated `spotless.sh` script
2. Manually run the Gradle task `spotlessApply` in the root project from the UI to format all files
3. Automatically run `spotlessApply` before building (right click on Gradle task in UI, select respective option)
4. Add the call to Gradle as external tool and assign a key binding (Settings -> Tools -> External tools; Settings -> Keymap)

**Related to Option 1:**

You can use the `generateSpotlessWatcher` task to generate a file watcher configuration for IntelliJ (`.idea/watcherTasks.xml`).
Be careful, this will overwrite the existing file watcher configuration.
This file contains the configuration for the file watcher.
If you have the plugin installed, it will automatically pick up the configuration and run the `spotless.sh` script when a file is changed.

For more information on how to set up the file watcher, see the documentation in the generated `spotless.sh` script.

When generating the `spotless.sh` script using the `generateSpotlessScript` task, if a `.idea` folder is present, a `watcherTasks.xml` file is generated automatically, if it is not already present.

### Configure publishing

For an overview on all available publishing options, see [PublishConfig](./convention-plugins/src/main/groovy/to/wetransform/gradle/conventions/PublishConfig.groovy).

#### Publishing to Artifactory

Publishing to wetransform's Artifactory is automatically enabled with a standard configuration in case the `maven-publish` or `java-library` plugins are applied as well to the project.

You can manually enable publishing by adding the following to the plugin configurartion:

```groovy
wetransform {
  setup {
    publish {
      enableMaven = true
    }
  }
}
```

By default, the group name for artifacts is set to `to.wetransform` and the version is set using the `gradle-semantic-release-version` plugin.

You can override the group name by providing a custom configuration:

```groovy
wetransform {
  setup {
    publish {
      group = 'to.wetransform.custom'
    }
  }
}
```

#### Publishing to Docker Hub

Publishing to Docker Hub is automatically enabled with a standard configuration in case the `com.bmuschko.docker-java-application` plugin is applied as well to the project, or if the `baseImage` configuration is set.

For example:

```groovy
wetransform {
  setup {
    publish {
      baseImage = 'eclipse-temurin:17-jre-alpine'
    }
  }
}
```

You can adapt the image name being used. By default, the image name is set to `to.wetransform/${project.name}`.

You can override the image name by providing a custom configuration:

```groovy
wetransform {
  setup {
    publish {
      imageName = 'wetransform/my-image'
    }
  }
}
```

#### Local Docker settings

To configure your local environment for accessing Docker and Docker Hub you can set the following Gradle properties:

- **dockerHost** - Address for connecting to Docker, defaults to <http://localhost:2375>, on Linux usually should be set to the Docker socket (`unix:///var/run/docker.sock`) to which the user needs access to
- **dockerHubUsername** - User name for Docker Hub
- **dockerHubPassword** - Password or access token for Docker Hub, highly recommend to use an access token

For these cases it is recommended to place a `gradle.properties` file in the `.gradle/` folder in your home directory (`~/.gradle/gradle.properties`).

For example:

```
dockerHost=unix:///var/run/docker.sock
dockerHubUsername=<username>
dockerHubPassword=<access-token>
```

## wetransform project settings conventions

The plugin `to.wetransform.settings.default` can be used to apply a number of conventions to a project:

- configuration of a version catalog file (`gradle/test-libs.versions.toml`) for test-only dependencies (this serves to correctly classify updates using Renovate)

Possible functionality to be added in the future could be, to add support for automatically configuring certain multi-project patterns.

To use the plugin, add the following to your `settings.gradle` file in the root project:

```groovy
// (after pluginManagement section)

plugins {
  id 'to.wetransform.settings.default' version '<version>'
}
```
