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
package to.wetransform.gradle.conventions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.ec4j.core.*;
import org.ec4j.core.model.PropertyType;

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
    ResourceProperties props = propService
      .queryProperties(Resource.Resources.ofPath(file.toPath(), StandardCharsets.UTF_8));

    PropertyType.IndentStyleValue indentStyleValue = props.getValue(PropertyType.indent_style,
      PropertyType.IndentStyleValue.space, true);
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
      insertFinalNewline,
      props);
  }

  public EditorConfigInfo getEditorConfigInfo(String file) throws IOException {
    return getEditorConfigInfo(new File(rootDir, file));
  }

}
