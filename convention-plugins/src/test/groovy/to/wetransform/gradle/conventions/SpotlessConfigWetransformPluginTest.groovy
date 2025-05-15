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
