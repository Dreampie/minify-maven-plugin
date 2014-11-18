package cn.dreampie;

import com.google.common.collect.Lists;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
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
 * Minify JavaScript with Maven
 */
@Mojo(name = "minify", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class MinifierMojo extends AbstractMojo {

  /**
   * Location of the Files to Minify.  Defaults to ${build.directory}/coffee
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/javascript")
  private File sourceDirectory;

  /**
   * The set of files that should be minified.  Be sure to specify the path to the compiled
   * <p/>
   * Only one or the other of files or directoryOfFilesToMinify should be specified.  Only files is used if both are specified.
   */
  @Parameter
  private File[] includeFiles;

  @Parameter
  private File[] excludeFiles;

  @Parameter(defaultValue = "${project.basedir}/src/main/webapp/javascript/${project.artifactId}-${project.version}.min.js")
  private File outputFile;

  @Parameter(defaultValue = "${project.basedir}/src/main/webapp/javascript")
  private File outputDirectory;

  @Parameter(defaultValue = "false")
  private boolean merge;

  @Parameter(defaultValue = "false")
  private boolean skip;

  public void execute() throws MojoExecutionException, MojoFailureException {
    LogKit.setLog(getLog());
    try {
      getLog().info("Minifying all Javascript Files in the Output Directory");
      ClosureMinifier minifier = new ClosureMinifier();
      minifier.setSourceDirectory(sourceDirectory);
      List<File> filesToMinify;
      if (null != includeFiles) {
        filesToMinify = Lists.newArrayList(includeFiles);

        getLog().info("About to include the following files:  " + FileUtilities
            .getCommaSeparatedListOfFileNames(filesToMinify));
      } else {
        getLog().debug("No file to include,use sourceDirectory.");
        filesToMinify = FileUtilities.directoryToFileList(sourceDirectory);
      }
      List<File> filesToMinifyMinusDestFile = null;
      if (null != excludeFiles) {
        filesToMinifyMinusDestFile = getMinifyFiles(filesToMinify, Lists.newArrayList(excludeFiles));
      } else {
        filesToMinifyMinusDestFile = filesToMinify;
      }

      if (filesToMinifyMinusDestFile != null && filesToMinifyMinusDestFile.size() > 0) {
        getLog().info("About to minify the following files:  " + FileUtilities
            .getCommaSeparatedListOfFileNames(filesToMinifyMinusDestFile));
      } else {
        getLog().info("No file  to minify.");
      }
      if (merge) {
        minifier.compile(filesToMinifyMinusDestFile, outputFile);
      } else {
        minifier.setOutputDirectory(outputDirectory);
        minifier.compile(filesToMinifyMinusDestFile);

      }
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private List<File> getMinifyFiles(List<File> filesToMinify, List<File> outFiles) {
    if (outFiles != null && outFiles.size() > 0) {
      getLog().info("About to exclude the following files:  " + FileUtilities
          .getCommaSeparatedListOfFileNames(outFiles));

      List<File> filesToMinifyMinusDestFile = Lists.newArrayList();
      for (File file : filesToMinify) {
        if (file.isDirectory()) {
          filesToMinifyMinusDestFile.addAll(FileUtilities.directoryToFileList(file));
        } else {
          filesToMinifyMinusDestFile.add(file);
        }
      }

      for (File outFile : outFiles) {
        if (outFile.isDirectory()) {
          if (outFile.isDirectory()) {
            filesToMinifyMinusDestFile.removeAll(FileUtilities.directoryToFileList(outFile));
          } else {
            filesToMinifyMinusDestFile.remove(outFile);
          }
        }
      }
      return filesToMinifyMinusDestFile;
    } else {
      getLog().info("No file to exclude.");
      return filesToMinify;
    }
  }
}
