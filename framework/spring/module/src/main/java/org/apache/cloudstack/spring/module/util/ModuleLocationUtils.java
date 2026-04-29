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
package org.apache.cloudstack.spring.module.util;

public class ModuleLocationUtils {

    private static final String ALL_MODULE_PROPERTIES = "classpath*:%s/*/module.properties";
    private static final String MODULE_PROPERTIES = "classpath:%s/%s/module.properties";
    private static final String CONTEXT_LOCATION = "classpath*:%s/%s/*context.xml";
    private static final String INHERTIABLE_CONTEXT_LOCATION = "classpath*:%s/%s/*context-inheritable.xml";
    private static final String OVERRIDE_CONTEXT_LOCATION = "classpath*:%s/%s/*context-override.xml";
    private static final String DEFAULTS_LOCATION = "classpath*:%s/%s/*defaults.properties";

    public static String getModulesLocation(String baseDir) {
        return String.format(ALL_MODULE_PROPERTIES, baseDir);
    }

    public static String getModuleLocation(String baseDir, String name) {
        return String.format(MODULE_PROPERTIES, baseDir, name);
    }

    public static String getContextLocation(String baseDir, String name) {
        return String.format(CONTEXT_LOCATION, baseDir, name);
    }

    public static String getInheritableContextLocation(String baseDir, String name) {
        return String.format(INHERTIABLE_CONTEXT_LOCATION, baseDir, name);
    }

    public static String getOverrideContextLocation(String baseDir, String name) {
        return String.format(OVERRIDE_CONTEXT_LOCATION, baseDir, name);
    }

    public static String getDefaultsLocation(String baseDir, String name) {
        return String.format(DEFAULTS_LOCATION, baseDir, name);
    }
}
