package to.wetransform.gradle.settings;

import org.gradle.api.internal.file.AbstractFileCollection;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple file collection that wraps a set of files.
 *
 * This class is defined because the Gradle API does not provide a simple way to create a file collection from Settings.
 * It is available in settings.gradle, but not in the Gradle API.
 * See also this discussion:
 * <a href="https://discuss.gradle.org/t/use-filecollection-apis-in-settings-gradle/23946/7">Use FileCollection APIs in settings.gradle</a>
 */
public class SimpleFileCollection extends AbstractFileCollection {
  private final Set<File> files;

  public SimpleFileCollection(File... files) {
    this.files = Set.of(files);
  }

  @Override
  public String getDisplayName() {
    return "SimpleFileCollection";
  }

  @Override
  public Iterator<File> iterator() {
    return files.iterator();
  }
}
