package to.wetransform.gradle.conventions;

import org.ec4j.core.*;
import org.ec4j.core.model.PropertyType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EditorConfigHelper {

  private final File rootDir;
  private final ResourcePropertiesService propService;

  public EditorConfigHelper(File rootDir) {
    this.rootDir = rootDir;

    Cache myCache = Cache.Caches.permanent();
    EditorConfigLoader myLoader = EditorConfigLoader.default_();
    this.propService = ResourcePropertiesService.builder()
      .cache(myCache)
      .loader(myLoader)
      .rootDirectory(ResourcePath.ResourcePaths.ofPath(rootDir.toPath(), StandardCharsets.UTF_8))
      .build();
  }

  public EditorConfigInfo getEditorConfigInfo(File file) throws IOException {
    ResourceProperties props = propService.queryProperties(Resource.Resources.ofPath(file.toPath(), StandardCharsets.UTF_8));

    PropertyType.IndentStyleValue indentStyleValue = props.getValue(PropertyType.indent_style, PropertyType.IndentStyleValue.space, true);
    boolean indentWithSpaces = indentStyleValue == PropertyType.IndentStyleValue.space;

    int indentSize = props.getValue(PropertyType.indent_size, 2, true);

    PropertyType.EndOfLineValue endOfLine = props.getValue(PropertyType.end_of_line, null, true);

    boolean trimTrailingWhitespace = props.getValue(PropertyType.trim_trailing_whitespace, false, true);

    String charset = props.getValue(PropertyType.charset, "utf-8", true);

    boolean insertFinalNewline = props.getValue(PropertyType.insert_final_newline, false, true);

    return new EditorConfigInfo(
      indentWithSpaces,
      indentSize,
      endOfLine,
      trimTrailingWhitespace,
      charset,
      insertFinalNewline
    );
  }

  public EditorConfigInfo getEditorConfigInfo(String file) throws IOException {
    return getEditorConfigInfo(new File(rootDir, file));
  }

}
