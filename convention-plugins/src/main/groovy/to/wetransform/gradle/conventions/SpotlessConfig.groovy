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
interface SpotlessConfig {
  /**
   * The default license header.
   * The year is replaced with the current year.
   */
  static LICENSE_HEADER_DEFAULT = '''/*
 * Copyright (c) $YEAR wetransform GmbH
 * All rights reserved.
 */
'''

  static String APACHE = '''\
    /*
     * Copyright $YEAR wetransform GmbH
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
    '''.stripIndent()

  /**
   * @return <code>true</code> if spotless should be disabled
   */
  Property<Boolean> getDisable()

  /**
   * @return the license header to use for Java, Kotlin and Groovy files
   */
  Property<String> getLicenseHeader()
}
