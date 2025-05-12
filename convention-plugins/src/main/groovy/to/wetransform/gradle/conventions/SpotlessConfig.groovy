/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import org.gradle.api.provider.Property

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

  /**
   * @return <code>true</code> if spotless should be disabled
   */
  Property<Boolean> getDisable()

  /**
   * @return the license header to use for Java, Kotlin and Groovy files
   */
  Property<String> getLicenseHeader()
}
