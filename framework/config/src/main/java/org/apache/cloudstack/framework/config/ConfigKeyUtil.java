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
package org.apache.cloudstack.framework.config;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class that helps with configuration key manipulation.
 *
 * @author mprokopchuk
 */
public final class ConfigKeyUtil {

    /**
     * Split by {@code ;} with optional space symbols (space, tab, new line, etc.) before and after.
     */
    private static Pattern ENTRY_SEPARATOR_PATTERN = Pattern.compile("\\s*;\\s*");
    /**
     * Split by {@code =} with optional space symbols (space, tab, new line, etc.) before and after.
     */
    private static Pattern KEY_VALUE_SEPARATOR_PATTERN = Pattern.compile("\\s*=\\s*");

    private ConfigKeyUtil() {
    }

    /**
     * Convert configuration value of format {@code key1=value1;key2=value2;...} to {@link Map<String, String>}.
     *
     * @param configValue configuration value string
     * @return configuration values map
     */
    public static Map<String, String> toMap(String configValue) {
        if (StringUtils.isEmpty(configValue)) {
            return Map.of();
        }

        return Arrays.stream(ENTRY_SEPARATOR_PATTERN.split(configValue))
                .map(pair -> KEY_VALUE_SEPARATOR_PATTERN.split(pair, 2))
                .filter(keyValue -> keyValue.length == 2)
                .collect(Collectors.toMap(keyValue -> keyValue[0], keyValue -> keyValue[1]));
    }
}
