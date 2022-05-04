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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class LogUtils {
    public static final Logger LOGGER = Logger.getLogger(LogUtils.class);

    private static String configFileLocation = null;

    public static void initLog4j(String log4jConfigFileName) {
        assert (log4jConfigFileName != null);
        File file = PropertiesUtil.findConfigFile(log4jConfigFileName);
        if (file != null) {
            configFileLocation = file.getAbsolutePath();
            DOMConfigurator.configureAndWatch(configFileLocation);
        } else {
            String nameWithoutExtension = log4jConfigFileName.substring(0, log4jConfigFileName.lastIndexOf('.'));
            file = PropertiesUtil.findConfigFile(nameWithoutExtension + ".properties");
            if (file != null) {
                configFileLocation = file.getAbsolutePath();
                DOMConfigurator.configureAndWatch(configFileLocation);
            }
        }
        if (configFileLocation != null) {
            LOGGER.info("log4j configuration found at " + configFileLocation);
        }
    }
    public static Set<String> getLogFileNames() {
        Set<String> fileNames = new HashSet<>();
        Enumeration appenders = LOGGER.getRootLogger().getAllAppenders();
        int appenderCount=0;
        while (appenders.hasMoreElements()) {
            ++appenderCount;
            Appender currAppender = (Appender) appenders.nextElement();
            if (currAppender instanceof FileAppender) {
                String fileName =((FileAppender) currAppender).getFile();
                fileNames.add(fileName);
                LOGGER.debug(String.format("file for %s : %s", currAppender.getName(), fileName));
            } else if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("not counting %s as a file.", currAppender.getName()));
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("out of %d appenders, %d are log files", appenderCount, fileNames.size()));
        }
        return fileNames;
    }
}
