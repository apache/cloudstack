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
import java.io.IOException;
import java.util.Properties;

import javax.naming.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class ProcessUtil {
    private static final Logger s_logger = Logger.getLogger(ProcessUtil.class.getName());

    // paths cannot be hardcoded
    public static void pidCheck(String pidDir, String run) throws ConfigurationException {

        String dir = pidDir == null ? "/var/run" : pidDir;

        try {
            final File propsFile = PropertiesUtil.findConfigFile("environment.properties");
            if (propsFile == null) {
                s_logger.debug("environment.properties could not be opened");
            } else {
                final Properties props = PropertiesUtil.loadFromFile(propsFile);
                dir = props.getProperty("paths.pid");
                if (dir == null) {
                    dir = pidDir == null ? "/var/run" : pidDir;
                }
            }
        } catch (IOException e) {
            s_logger.debug("environment.properties could not be opened");
        }

        final File pidFile = new File(dir + File.separator + run);
        try {
            if (!pidFile.createNewFile()) {
                if (!pidFile.exists()) {
                    throw new ConfigurationException("Unable to write to " + pidFile.getAbsolutePath() + ".  Are you sure you're running as root?");
                }

                final String pidLine = FileUtils.readFileToString(pidFile).trim();
                if (pidLine.isEmpty()) {
                    throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
                }
                try {
                    final long pid = Long.parseLong(pidLine);
                    final Script script = new Script("bash", 120000, s_logger);
                    script.add("-c", "ps -p " + pid);
                    final String result = script.execute();
                    if (result == null) {
                        throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
                    }
                    if (!pidFile.delete()) {
                        throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
                    }
                    if (!pidFile.createNewFile()) {
                        throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
                    }
                } catch (final NumberFormatException e) {
                    throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
                }
            }
            pidFile.deleteOnExit();

            final Script script = new Script("bash", 120000, s_logger);
            script.add("-c", "echo $PPID");
            final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
            script.execute(parser);

            final String pid = parser.getLine();

            FileUtils.writeStringToFile(pidFile, pid + "\n");
        } catch (final IOException e) {
            throw new CloudRuntimeException("Unable to create the " + pidFile.getAbsolutePath() + ".  Are you running as root?", e);
        }
    }

    public static String dumpStack() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elems = Thread.currentThread().getStackTrace();
        if (elems != null && elems.length > 0) {
            for (StackTraceElement elem : elems) {
                sb.append("\tat ").append(elem.getMethodName()).append("(").append(elem.getFileName()).append(":").append(elem.getLineNumber()).append(")\n");
            }
        }
        return sb.toString();
    }
}
