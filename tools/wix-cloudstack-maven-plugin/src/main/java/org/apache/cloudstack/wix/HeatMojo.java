// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.wix;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.bitbucket.joxley.wix.AbstractWixMojo;

/**
 *
 * @goal heat
 *
 * @phase package
 *
 */
public class HeatMojo extends AbstractWixMojo {

  /**
   * Directory name to be harvested
   *
   * @parameter expression="${dir}"
   * @required
   *
   */
  private String dir;

  /**
   * use template, one of: fragment,module,product
   * @parameter expression="${template}" defaults to fragment
   *
   */
  private String template;

  /**
   * Output file
   *
   * @parameter expression="${outputFile}"
   */
  private File outputFile;

  /**
  *
  * variable names to be passed to heat command
  * @parameter expression="${vars}"
  */
  private String vars;

  /**
  *
  * variable names to be passed to heat command
  * @parameter expression="${workingDirectory}"
  */
  private File workingDirectory;

  /**
  *
  * variable names to be passed to heat command
  * @parameter expression="${componentGroup}"
  */
  private String componentGroup;

  /**
   *
   * <DirectoryName>  directory reference to root directories
   * @parameter expression="${directoryName}"
   */
  private String directoryName;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      CommandLine commandLine = new CommandLine("heat");

      if(dir != null && !dir.trim().isEmpty()) {
        commandLine.addArgument("dir");
        commandLine.addArgument(dir);
      }

      commandLine.addArgument("-gg");
      commandLine.addArgument("-cg");
      commandLine.addArgument(componentGroup);
      commandLine.addArgument("-ke");
      commandLine.addArgument("-sfrag");

      if(template == null || template.trim().isEmpty()) {
        commandLine.addArgument("-template");
        commandLine.addArgument("fragment");
      } else {
        commandLine.addArgument("-template");
        commandLine.addArgument(template);
      }

      if (outputFile != null) {
        commandLine.addArgument("-out");
        commandLine.addArgument(outputFile.getAbsolutePath());
      }

      if (directoryName != null) {
        commandLine.addArgument("-dr");
        commandLine.addArgument(directoryName);
      }

      if (vars != null) {
        commandLine.addArguments(vars, false);
      }

      DefaultExecutor executor = new DefaultExecutor();
      getLog().debug("working directory " + commandLine.toString());
      executor.setWorkingDirectory(getWorkingDirectory(workingDirectory));
      int exitValue = executor.execute(commandLine);

      if (exitValue != 0) {
        throw new MojoExecutionException(
            "Problem executing heat, return code " + exitValue);
      }

    } catch (ExecuteException e) {
      throw new MojoExecutionException("Problem executing heat", e);
    } catch (IOException e) {
      throw new MojoExecutionException("Problem executing heat", e);
    }
  }

}
