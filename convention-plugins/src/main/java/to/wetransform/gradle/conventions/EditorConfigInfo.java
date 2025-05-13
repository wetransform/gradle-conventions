/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
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
