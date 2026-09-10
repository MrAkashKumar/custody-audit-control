package com.custody.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/** Parses source declarations rather than matching strings, comments or SQL aliases. */
class NamingConventionTest {
  @Test
  void variablesAndParametersMustNotUseProhibitedShorthand() throws Exception {
    Path projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (projectRoot != null && !Files.isDirectory(projectRoot.resolve("cc-core"))) {
      projectRoot = projectRoot.getParent();
    }
    assertThat(projectRoot).as("Maven reactor root").isNotNull();
    List<Path> sourceFiles;
    try (var paths = Files.walk(projectRoot)) {
      sourceFiles =
          paths
              .filter(path -> path.toString().endsWith(".java"))
              .filter(
                  path ->
                      path.toString().contains("/src/main/java/")
                          || path.toString().contains("/src/test/java/"))
              .toList();
    }
    var compiler = ToolProvider.getSystemJavaCompiler();
    assertThat(compiler).as("Tests require a JDK").isNotNull();
    List<String> violations = new ArrayList<>();
    try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
      var compilation =
          (JavacTask)
              compiler.getTask(
                  null,
                  fileManager,
                  null,
                  List.of("-proc:none"),
                  null,
                  fileManager.getJavaFileObjectsFromPaths(sourceFiles));
      for (CompilationUnitTree sourceUnit : compilation.parse()) {
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitVariable(VariableTree variable, Void unused) {
            String variableName = variable.getName().toString();
            boolean shorthand =
                variableName.matches("[a-z]{1,2}") && !Set.of("id", "to").contains(variableName);
            if (shorthand || Set.of("req", "res", "obj", "tmp").contains(variableName)) {
              violations.add(sourceUnit.getSourceFile().getName() + ": " + variableName);
            }
            return super.visitVariable(variable, unused);
          }
        }.scan(sourceUnit, null);
      }
    }
    assertThat(violations).as("Use names describing each variable's role").isEmpty();
  }
}
