/*
 * Copyright (c) 2025 wetransform GmbH
 * All rights reserved.
 */
package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic

@CompileStatic
class EclipseFormatProperties {

  static Map <String, String> getProperties(EditorConfigInfo editorConfig, boolean groovy) {
    Map<String, String> properties = [:]

    int maxLineLength = 120

    // Overview on options:
    // https://github.com/eclipse-jdt/eclipse.jdt.core/blob/master/org.eclipse.jdt.core/formatter/org/eclipse/jdt/core/formatter/DefaultCodeFormatterConstants.java

    // Whether to use 'space', 'tab' or 'mixed' (both) characters for indentation.
    // The default value is 'tab'.
    properties['org.eclipse.jdt.core.formatter.tabulation.char'] = editorConfig.indentWithSpaces() ? 'space' : 'tab'

    // Number of spaces used for indentation in case 'space' characters
    // have been selected. The default value is 4.
    properties['org.eclipse.jdt.core.formatter.tabulation.size'] = editorConfig.indentSize() as String

    // Number of spaces used for indentation in case 'mnixed' characters
    // have been selected. The default value is 4.
    properties['org.eclipse.jdt.core.formatter.indentation.size'] = editorConfig.indentSize() as String

    // Option to specify the length of the page. Beyond this length, the formatter will try to split the code
    properties['org.eclipse.jdt.core.formatter.lineSplit'] = maxLineLength as String

    // Option to specify the line length for comments.
    properties['org.eclipse.jdt.core.formatter.comment.line_length'] = maxLineLength as String

    // Option to specify whether the formatter can join wrapped lines or not
    properties['org.eclipse.jdt.core.formatter.join_wrapped_lines'] = 'false'

    // Option to specify whether the formatter can join text lines in comments or not
    properties['org.eclipse.jdt.core.formatter.join_lines_in_comments'] = 'false'

    // Whether or not indentation characters are inserted into empty lines.
    // The default value is 'true'.
    properties['org.eclipse.jdt.core.formatter.indent_empty_lines'] = !editorConfig.trimTrailingWhitespace() as String

    // Option to insert a new line before the else keyword in if statement
    properties['org.eclipse.jdt.core.formatter.insert_new_line_before_else_in_if_statement'] = 'insert'

    // Option to set the continuation indentation
    // The value (n) is interpreted a <n> times the indentation size (e.g. if the value is 2 and normal indentation is
    // 2 spaces, the indentation for the continuation is 4 spaces).
    int continuationIndentationFactor = 1
    properties['org.eclipse.jdt.core.formatter.continuation_indentation'] = continuationIndentationFactor as String

    // Option to insert a new line at the end of the current file if missing
    properties['org.eclipse.jdt.core.formatter.insert_new_line_at_end_of_file_if_missing'] = editorConfig.insertFinalNewline() ? 'insert' : 'do_not_insert'

    // Option to position parentheses in method invocations
    // properties['org.eclipse.jdt.core.formatter.parentheses_positions_in_method_invocation'] = 'separate_lines_if_wrapped'
    // Note: Looks bad because the parentheses at the end are not aligned with the method call

    if (groovy) {
      // Groovy specific settings

      // See https://github.com/groovy/groovy-eclipse/blob/master/ide/org.codehaus.groovy.eclipse.refactoring/src/org/codehaus/groovy/eclipse/refactoring/PreferenceConstants.java

      properties['groovy.formatter.remove.unnecessary.semicolons'] = 'true'

      properties['groovy.formatter.line.maxlength'] = maxLineLength as String

      properties['groovy.formatter.multiline.indentation'] = continuationIndentationFactor as String
    }
    return properties
  }

  static String getPropertiesAsString(EditorConfigInfo editorConfig, boolean groovy) {
    StringBuilder sb = new StringBuilder()
    Map<String, String> properties = getProperties(editorConfig, groovy)
    properties.each { key, value ->
      sb.append("$key=$value\n")
    }
    return sb.toString()
  }
}
