//
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
//

package com.cloud.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class SwiftUtil {
    private static Logger logger = Logger.getLogger(SwiftUtil.class);
    private static final long SWIFT_MAX_SIZE = 5L * 1024L * 1024L * 1024L;

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

    public static boolean postMeta(SwiftClientCfg cfg, String container, String object, Map<String, String> metas) {
        String swiftCli = getSwiftCLIPath();
        StringBuilder cms = new StringBuilder();
        for (Map.Entry<String, String> entry : metas.entrySet()) {
            cms.append(" -m ");
            cms.append(entry.getKey());
            cms.append(":");
            cms.append(entry.getValue());
            cms.append(" ");
        }
        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add("/usr/bin/python " + swiftCli + " -A " + cfg.getEndPoint() + " -U " + cfg.getAccount() + ":" + cfg.getUserName() + " -K " + cfg.getKey() + " post " +
            container + " " + object + " " + cms.toString());
        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = command.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException("Failed to post meta" + result);
        }
        return true;
    }

    public static String putObject(SwiftClientCfg cfg, File srcFile, String container, String fileName) {
        String swiftCli = getSwiftCLIPath();
        if (fileName == null) {
            fileName = srcFile.getName();
        }
        String srcDirectory = srcFile.getParent();
        Script command = new Script("/bin/bash", logger);
        long size = srcFile.length();
        command.add("-c");
        if (size <= SWIFT_MAX_SIZE) {
            command.add("cd " + srcDirectory + ";/usr/bin/python " + swiftCli + " -A " + cfg.getEndPoint() + " -U " + cfg.getAccount() + ":" + cfg.getUserName() +
                " -K " + cfg.getKey() + " upload " + container + " " + fileName);
        } else {
            command.add("cd " + srcDirectory + ";/usr/bin/python " + swiftCli + " -A " + cfg.getEndPoint() + " -U " + cfg.getAccount() + ":" + cfg.getUserName() +
                " -K " + cfg.getKey() + " upload -S " + SWIFT_MAX_SIZE + " " + container + " " + fileName);
        }
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException("Failed to upload file: " + result);
        }

        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed") || line.contains("not found")) {
                    throw new CloudRuntimeException("Failed to upload file: " + Arrays.toString(lines));
                }
            }
        }

        return container + File.separator + srcFile.getName();
    }

    private static StringBuilder buildSwiftCmd(SwiftClientCfg swift) {
        String swiftCli = getSwiftCLIPath();
        StringBuilder sb = new StringBuilder();
        sb.append(" /usr/bin/python ");
        sb.append(swiftCli);
        sb.append(" -A ");
        sb.append(swift.getEndPoint());
        sb.append(" -U ");
        sb.append(swift.getAccount());
        sb.append(":");
        sb.append(swift.getUserName());
        sb.append(" -K ");
        sb.append(swift.getKey());
        sb.append(" ");
        return sb;
    }

    public static String[] list(SwiftClientCfg swift, String container, String rFilename) {
        getSwiftCLIPath();
        Script command = new Script("/bin/bash", logger);
        command.add("-c");

        StringBuilder swiftCmdBuilder = buildSwiftCmd(swift);
        swiftCmdBuilder.append(" list ");
        swiftCmdBuilder.append(container);

        if (rFilename != null) {
            swiftCmdBuilder.append(" -p ");
            swiftCmdBuilder.append(rFilename);
        }

        command.add(swiftCmdBuilder.toString());
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result == null && parser.getLines() != null && !parser.getLines().equalsIgnoreCase("")) {
            String[] lines = parser.getLines().split("\\n");
            return lines;
        } else {
            if (result != null) {
                String errMsg = "swiftList failed , err=" + result;
                logger.debug("Failed to list " + errMsg);
            } else {
                String errMsg = "swiftList failed, no lines returns";
                logger.debug("Failed to list " + errMsg);
            }
        }
        return new String[0];
    }

    public static File getObject(SwiftClientCfg cfg, File destDirectory, String swiftPath) {
        int firstIndexOfSeparator = swiftPath.indexOf(File.separator);
        String container = swiftPath.substring(0, firstIndexOfSeparator);
        String srcPath = swiftPath.substring(firstIndexOfSeparator + 1);
        String destFilePath = null;
        if (destDirectory.isDirectory()) {
            destFilePath = destDirectory.getAbsolutePath() + File.separator + srcPath;
        } else {
            destFilePath = destDirectory.getAbsolutePath();
        }
        String swiftCli = getSwiftCLIPath();
        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add("/usr/bin/python " + swiftCli + " -A " + cfg.getEndPoint() + " -U " + cfg.getAccount() + ":" + cfg.getUserName() + " -K " + cfg.getKey() +
            " download " + container + " " + srcPath + " -o " + destFilePath);
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
                    String errMsg = "swiftDownload failed , err=" + Arrays.toString(lines);
                    logger.debug(errMsg);
                    throw new CloudRuntimeException("Failed to get object: " + swiftPath);
                }
            }
        }
        return new File(destFilePath);
    }

    public static String getContainerName(String type, Long id) {
        if (type.startsWith("T")) {
            return "T-" + id;
        } else if (type.startsWith("S")) {
            return "S-" + id;
        } else if (type.startsWith("V")) {
            return "V-" + id;
        }
        return null;
    }

    public static String[] splitSwiftPath(String path) {
        int index = path.indexOf(File.separator);
        if (index == -1) {
            return null;
        }
        String[] paths = new String[2];
        paths[0] = path.substring(0, index);
        paths[1] = path.substring(index + 1);
        return paths;
    }

    public static boolean deleteObject(SwiftClientCfg cfg, String path) {
        Script command = new Script("/bin/bash", logger);
        command.add("-c");

        String[] paths = splitSwiftPath(path);
        if (paths == null) {
            return false;
        }
        String container = paths[0];
        String objectName = paths[1];

        StringBuilder swiftCmdBuilder = buildSwiftCmd(cfg);
        swiftCmdBuilder.append(" delete ");
        swiftCmdBuilder.append(container);
        swiftCmdBuilder.append(" ");
        swiftCmdBuilder.append(objectName);

        command.add(swiftCmdBuilder.toString());
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        command.execute(parser);
        return true;
    }
}
