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

import groovy.transform.CompileStatic

import org.gradle.api.provider.Property

@CompileStatic
interface JacocoConfig {
  /**
   * @return <code>true</code> if JaCoCo code coverage should be disabled, default is <code>false</code>
   */
  Property<Boolean> getDisable()

  /**
   * @return the JaCoCo tool version to use, if not set the Gradle default is used
   */
  Property<String> getToolVersion()
}
