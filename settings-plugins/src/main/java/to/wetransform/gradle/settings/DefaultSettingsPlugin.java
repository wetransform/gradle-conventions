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
