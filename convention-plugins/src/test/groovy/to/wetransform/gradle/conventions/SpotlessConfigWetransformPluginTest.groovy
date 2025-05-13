/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

class SpotlessConfigWetransformPluginTest extends SpotlessWetransformPluginTest {

  def setup() {
    buildFile.text = """
    plugins {
      id 'java'
      id 'to.wetransform.conventions'
    }

    wetransform {
      config {
        spotless {
          licenseHeader = '/*\\n * Copyright (c) \$YEAR Test organization\\n */'
        }
      }
    }

    wetransform.setup()

    repositories {
      mavenCentral()
    }
    """.stripIndent()
  }
}
