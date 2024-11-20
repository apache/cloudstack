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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;

import com.google.gson.Gson;

public class LogUtils {
    protected static Logger LOGGER = LogManager.getLogger(LogUtils.class);
    private static final Gson GSON = new Gson();

    private static String configFileLocation = null;

    public static void initLog4j(String log4jConfigFileName) {
        assert (log4jConfigFileName != null);
        File file = PropertiesUtil.findConfigFile(log4jConfigFileName);
        if (file != null) {
            configFileLocation = file.getAbsolutePath();
            Configurator.initialize(null, configFileLocation);
        } else {
            String nameWithoutExtension = log4jConfigFileName.substring(0, log4jConfigFileName.lastIndexOf('.'));
            file = PropertiesUtil.findConfigFile(nameWithoutExtension + ".properties");
            if (file != null) {
                configFileLocation = file.getAbsolutePath();
                Configurator.initialize(null, configFileLocation);
            }
        }
        if (configFileLocation != null) {
            LOGGER.info("log4j configuration found at " + configFileLocation);
        }
    }
    public static Set<String> getLogFileNames() {
        Set<String> fileNames = new HashSet<>();
        org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger)LogManager.getRootLogger();
        Map<String, Appender> appenderMap = rootLogger.getAppenders();
        int appenderCount = 0;
        for (Appender appender : appenderMap.values()){
            ++appenderCount;
            if (appender instanceof FileAppender) {
                String fileName =((FileAppender) appender).getFileName();
                fileNames.add(fileName);
                LOGGER.debug("File for {} : {}", appender.getName(), fileName);
            } else if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Not counting {} as a file.", appender.getName());
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Out of {} appenders, {} are log files.", appenderCount, fileNames.size());
        }
        return fileNames;
    }

    /**
     * Tries to convert message parameters to JSON format and use them in the message.
     * @param formatMessage message to format.
     * @param objects objects to convert to JSON. A null object will be defaulted to the String "null";
     * if it is not possible to convert an object to JSON, the object's 'toString' will be used instead.
     * @return the formatted message.
     */
    public static String logGsonWithoutException(String formatMessage, Object ... objects) {
        List<String> gsons = new ArrayList<>();
        for (Object object : objects) {
            try {
                gsons.add(GSON.toJson(object));
            } catch (Exception e) {
                Object errObj = ObjectUtils.defaultIfNull(object, "null");
                LOGGER.trace(String.format("Failed to log object [%s] using GSON.", errObj.getClass().getSimpleName()));
                gsons.add("error decoding " + errObj);
            }
        }
        try {
            return String.format(formatMessage, gsons.toArray());
        } catch (Exception e) {
            String errorMsg = String.format("Failed to log objects using GSON due to: [%s].", e.getMessage());
            LOGGER.trace(errorMsg, e);
            return errorMsg;
        }
    }
}
