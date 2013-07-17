/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.utils;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import java.io.File;


public class SwiftUtil {
    private static Logger logger = Logger.getLogger(SwiftUtil.class);
    private static long SWIFT_MAX_SIZE = 5L * 1024L * 1024L * 1024L;
    public interface SwiftClientCfg {
        String getAccount();
        String getUserName();
        String getKey();
        String getEndPoint();
    }

    private static String getSwiftCLIPath() {
        String swiftCLI = Script.findScript("scripts/storage/secondary", "swift");
        if (swiftCLI == null) {
            logger.debug("Can't find swift cli at scripts/storage/secondary/swift");
            throw new CloudRuntimeException("Can't find swift cli at scripts/storage/secondary/swift");
        }
        return swiftCLI;
    }

    public static String putObject(SwiftClientCfg cfg, File srcFile, String container) {
        String swiftCli = getSwiftCLIPath();
        String srcDirectory = srcFile.getParent();
        Script command = new Script("/bin/bash", logger);
        long size = srcFile.length();
        command.add("-c");
        if (size <= SWIFT_MAX_SIZE) {
            command.add("cd " + srcDirectory
                    + ";/usr/bin/python " + swiftCli + " -A "
                    + cfg.getEndPoint() + " -U " + cfg.getAccount() + ":" + cfg.getUserName() + " -K "
                    + cfg.getKey() + " upload " + container + " " + srcFile.getName());
        } else {
            command.add("cd " + srcDirectory
                    + ";/usr/bin/python " + swiftCli + " -A "
                    + cfg.getEndPoint() + " -U " + cfg.getAccount() + ":" + cfg.getUserName() + " -K "
                    + cfg.getKey() + " upload -S " + SWIFT_MAX_SIZE + " " + container + " " + srcFile.getName());
        }
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException("Failed to upload file: " + result);
        }

        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed")) {
                    throw new CloudRuntimeException("Failed to upload file: " + lines.toString());
                }
            }
        }
        return container + File.separator + srcFile.getName();
    }

    public static File getObject(SwiftClientCfg cfg, File destDirectory, String swiftPath) {
        int firstIndexOfSeparator = swiftPath.indexOf(File.separator);
        String container = swiftPath.substring(0, firstIndexOfSeparator);
        String srcPath = swiftPath.substring(firstIndexOfSeparator + 1);
        String destFilePath = destDirectory.getAbsolutePath() + File.separator + srcPath;
        String swiftCli = getSwiftCLIPath();
        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add("/usr/bin/python " + swiftCli + " -A " + cfg.getEndPoint()
                + " -U " + cfg.getAccount() + ":" + cfg.getUserName() + " -K " + cfg.getKey() + " download "
                + container + " " + srcPath + " -o " + destFilePath);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            String errMsg = "swiftDownload failed  err=" + result;
            logger.debug(errMsg);
            throw new CloudRuntimeException("failed to get object: " + swiftPath);
        }
        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed")) {
                    String errMsg = "swiftDownload failed , err=" + lines.toString();
                    logger.debug(errMsg);
                    throw new CloudRuntimeException("Failed to get object: " + swiftPath);
                }
            }
        }
        return new File(destFilePath);
    }
}
