package to.wetransform.gradle.conventions

import org.gradle.api.provider.Property

interface SpotlessConfig {
  static LICENSE_HEADER_DEFAULT = '''/*
 * Copyright (c) $YEAR wetransform GmbH
 * All rights reserved.
 */
'''

  Property<Boolean> getDisable()
  Property<String> getLicenseHeader()
}
