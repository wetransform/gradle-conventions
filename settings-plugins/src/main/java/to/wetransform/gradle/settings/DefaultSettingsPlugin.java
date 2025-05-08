package to.wetransform.gradle.settings;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

import java.io.File;

public class DefaultSettingsPlugin implements Plugin<Settings> {
  @Override
  public void apply(Settings settings) {
    // Add a version catalog for test-only dependencies
    // This file is treated special in the renovate configuration to prevent a test dependency update to result in a version update
    settings.dependencyResolutionManagement(dependencyResolutionManagement -> {
      dependencyResolutionManagement.versionCatalogs(versionCatalogs -> {
        versionCatalogs.create("testLibs", catalog -> {
          catalog.from(new SimpleFileCollection(new File(settings.getRootDir(), "gradle/test-libs.versions.toml")));
        });
      });
    });
  }
}
