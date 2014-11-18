package cn.dreampie;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Copyright 2011 Mark Derricutt.
 * <p/>
 * Contributing authors:
 * Daniel Bower
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * Run the Closure Compiler tool on a directory of Javascripts.
 * <p/>
 * This class supports no configuration in its current form.
 */
public class ClosureMinifier {

  private Log log = LogKit.getLog();
  private CompilationLevel compilationLevel = CompilationLevel.SIMPLE_OPTIMIZATIONS;
  private File sourceDirectory;
  private File outputDirectory;


  public void compile(List<File> filesToCompile, File destFile) {
    Compiler compiler = new Compiler();
    Result result = compiler.compile(getExterns(), getInputs(filesToCompile), getCompilerOptions());
    compile(result, destFile, compiler);
  }

  public void compile(List<File> filesToCompile) {
    File destFile = null;
    Result result = null;
    Compiler compiler = null;
    for (File file : filesToCompile) {
      compiler = new Compiler();
      destFile = prepareDestFile(file);
      log.info("Compile " + destFile.getAbsolutePath());
      result = compiler.compile(getExterns(), Lists.newArrayList(getInput(file)), getCompilerOptions());
      compile(result, destFile, compiler);
    }
  }

  private void compile(Result result, File destFile, Compiler compiler) {

    log.debug(result.debugLog);
    for (JSError error : result.errors) {
      log.error("Closure Minifier Error:  " + error.sourceName + "  Description:  " + error.description);
    }
    for (JSError warning : result.warnings) {
      log.info("Closure Minifier Warning:  " + warning.sourceName + "  Description:  " + warning.description);
    }

    if (result.success) {
      try {
        Files.write(compiler.toSource(), destFile, Charsets.UTF_8);
      } catch (IOException e) {
        throw new ClosureException("Failed to write minified file to " + destFile, e);
      }
    } else {
      throw new ClosureException("Closure Compiler Failed - See error messages on System.err");
    }
  }

  /**
   * Prepare the Destination File, Remove if it already exists
   */
  private File prepareDestFile(File file) {
    String path = file.getAbsolutePath().replace(sourceDirectory.getAbsolutePath(), "");
    File f = new File(outputDirectory, path.replace(".js", ".min.js"));
    File dir = f.getParentFile();
    if (!dir.exists())
      dir.mkdirs();
    return f;
  }

  /**
   * Prepare options for the Compiler.
   */
  private CompilerOptions getCompilerOptions() {
    CompilationLevel level = null;
    try {
      level = this.compilationLevel;
    } catch (IllegalArgumentException e) {
      throw new ClosureException("Compilation level is invalid", e);
    }

    CompilerOptions options = new CompilerOptions();
    level.setOptionsForCompilationLevel(options);

    return options;
  }

  /**
   * Externs are defined in the Closure documentations as:
   * External variables are declared in 'externs' files. For instance, the file may include
   * definitions for global javascript/browser objects such as window, document.
   * <p/>
   * This method sneaks into the CommandLineRunner class of the Closure command line tool
   * and pulls the default Externs there.  This class could be modified to instead look
   * somewhere more relevant to the project.
   */
  private List<SourceFile> getExterns() {
    try {
      return CommandLineRunner.getDefaultExterns();
    } catch (IOException e) {
      throw new ClosureException("Unable to load default External variables Files. The files include definitions for global javascript/browser objects such as window, document.", e);
    }
  }

  private List<SourceFile> getInputs(List<File> filesToProcess) {
    List<SourceFile> files = Lists.newArrayList();

    for (File file : filesToProcess) {
      files.add(getInput(file));
    }

    return files;
  }

  private SourceFile getInput(File fileToProcess) {
    return SourceFile.fromFile(fileToProcess);
  }

  public CompilationLevel getCompilationLevel() {
    return compilationLevel;
  }

  public void setCompilationLevel(CompilationLevel compilationLevel) {
    this.compilationLevel = compilationLevel;
  }

  public void setSourceDirectory(File sourceDirectory) {
    this.sourceDirectory = sourceDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

}
