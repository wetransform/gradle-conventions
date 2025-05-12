/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.settings;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.model.ObjectFactory;

public class DefaultSettingsPlugin implements Plugin<Settings> {

  private final ObjectFactory objectFactory;

  @Inject
  public DefaultSettingsPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void apply(Settings settings) {
    // Add a version catalog for test-only dependencies
    // This file is treated special in the renovate configuration to prevent a test dependency update to result in a
    // version update
    settings.dependencyResolutionManagement(dependencyResolutionManagement -> {
      dependencyResolutionManagement.versionCatalogs(versionCatalogs -> {
        versionCatalogs.create("testLibs", catalog -> {
          catalog.from(
            objectFactory.fileCollection().from(new File(settings.getRootDir(), "gradle/test-libs.versions.toml")));
        });
      });
    });
  }
}
