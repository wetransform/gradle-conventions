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

import org.ec4j.core.ResourceProperties;
import org.ec4j.core.model.PropertyType;

import com.diffplug.spotless.LineEnding;

public record EditorConfigInfo(
  boolean indentWithSpaces,
  int indentSize,
  PropertyType.EndOfLineValue endOfLine,
  boolean trimTrailingWhitespace,
  String charset,
  boolean insertFinalNewline,
  ResourceProperties props) {
  // convert endOfLine to spotless LineEnding
  public LineEnding getLineEnding() {
    if (endOfLine == null) {
      return LineEnding.GIT_ATTRIBUTES_FAST_ALLSAME;
    }

    return switch (endOfLine) {
      case crlf -> LineEnding.WINDOWS;
      case lf -> LineEnding.UNIX;
      case cr -> LineEnding.MAC_CLASSIC;
    };
  }

  public boolean hasProperty(String property) {
    return props.getProperties().containsKey(property);
  }
}
