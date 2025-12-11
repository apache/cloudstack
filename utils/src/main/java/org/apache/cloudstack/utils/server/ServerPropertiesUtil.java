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

package org.apache.cloudstack.utils.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;

public class ServerPropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(ServerPropertiesUtil.class);

    public static final String KEY_HTTP_ENABLE = "http.enable";
    public static final String KEY_HTTP_PORT = "http.port";
    public static final String KEY_HTTPS_ENABLE = "https.enable";
    public static final String KEY_HTTPS_PORT = "https.port";
    public static final String KEY_KEYSTORE_FILE = "https.keystore";
    public static final String KEY_KEYSTORE_PASSWORD = "https.keystore.password";
    public static final String KEY_WEBSOCKET_ENABLE = "websocket.enable";
    public static final String KEY_WEBSOCKET_PORT = "websocket.port";


    public static final int HTTP_PORT = 8080;
    public static final int HTTPS_PORT = 8443;

    protected static final String PROPERTIES_FILE = "server.properties";
    protected static final AtomicReference<Properties> propertiesRef = new AtomicReference<>();

    public static String getProperty(String name) {
        Properties props = propertiesRef.get();
        if (props != null) {
            return props.getProperty(name);
        }
        File propsFile = PropertiesUtil.findConfigFile(PROPERTIES_FILE);
        if (propsFile == null) {
            logger.error("{} file not found", PROPERTIES_FILE);
            return null;
        }
        Properties tempProps = new Properties();
        try (FileInputStream is = new FileInputStream(propsFile)) {
            tempProps.load(is);
        } catch (IOException e) {
            logger.error("Error loading {}: {}", PROPERTIES_FILE, e.getMessage(), e);
            return null;
        }
        if (!propertiesRef.compareAndSet(null, tempProps)) {
            tempProps = propertiesRef.get();
        }
        return tempProps.getProperty(name);
    }

    public static String getProperty(String name, String defaultValue) {
        String value = getProperty(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public static Pair<Boolean, Integer> getServerModeAndPort() {
        boolean httpsEnabled = Boolean.parseBoolean(getProperty(KEY_HTTPS_ENABLE, "false"));
        if (!httpsEnabled) {
            return new Pair<>(false, getIntegerProperty(KEY_HTTP_PORT, HTTP_PORT));
        }
        return new Pair<>(true, getIntegerProperty(KEY_HTTPS_PORT, HTTPS_PORT));
    }

    protected static int getIntegerProperty(String key, int defaultValue) {
        String portStr = getProperty(key);
        if (portStr == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid value for {}: {}, using default {}", key, portStr, defaultValue);
            return defaultValue;
        }
    }
}
