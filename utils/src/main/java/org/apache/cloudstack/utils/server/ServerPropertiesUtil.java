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
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.utils.PropertiesUtil;

public class ServerPropertiesUtil {
    public static final String SHARE_DIR = "share";
    private static final Logger logger = LoggerFactory.getLogger(ServerPropertiesUtil.class);
    protected static final String PROPERTIES_FILE = "server.properties";

    private static final String CONTEXT_PATH = "context.path";
    private static final String SHARE_ENABLED = "share.enabled";
    private static final String SHARE_BASE_DIR = "share.base.dir";
    private static final String SHARE_CACHE_CONTROL = "share.cache.control";
    private static final String SHARE_SECRET = "share.secret";

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

    public static boolean getShareEnabled() {
        return Boolean.parseBoolean(getProperty(SHARE_ENABLED, "true"));
    }

    protected static boolean isMavenRun() {
        String args = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
        String sunCmd = System.getProperty("sun.java.command", "");
        String combined = args + " " + sunCmd;

        String[] mavenMarkers = new String[] {
                "org.codehaus.plexus.classworlds.launcher.Launcher",
                "org.apache.maven.wrapper.MavenWrapperMain",
                "org.apache.maven.cli.MavenCli",
                "org.apache.maven.surefire.booter.ForkedBooter",
                "org.apache.maven.surefire.booter.SurefireBooter"
        };
        for (String marker : mavenMarkers) {
            if (combined.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public static String getShareBaseDirectory() {
        String shareBaseDir = getProperty(SHARE_BASE_DIR);
        if (StringUtils.isNotBlank(shareBaseDir)) {
            return shareBaseDir;
        }
        if (isMavenRun()) {
            // when running from maven, use a share directory from client/target in the current working directory
            return String.format("%1$s%2$sclient%2$starget%2$s%3$s", System.getProperty("user.dir"), File.separator,
                    SHARE_DIR);
        }
        return System.getProperty("user.home") + File.separator + SHARE_DIR;
    }

    public static String getShareCacheControl() {
        return getProperty(SHARE_CACHE_CONTROL, "public,max-age=86400,immutable");
    }

    public static String getShareSecret() {
        return getProperty(SHARE_SECRET);
    }

    public static String getShareUriPath() {
        String sharePath = String.format("/%s", SHARE_DIR);
        if (isMavenRun()) {
            // when running from maven, share context is under root context - /client/share
            return String.format("%s%s", getProperty(CONTEXT_PATH, ""), sharePath);
        }
        return sharePath;
    }
}
