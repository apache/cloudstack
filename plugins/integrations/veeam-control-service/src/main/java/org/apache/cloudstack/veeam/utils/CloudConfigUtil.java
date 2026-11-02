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

package org.apache.cloudstack.veeam.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.net.NetUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public final class CloudConfigUtil {

    private static final String NETWORK_CONFIG_PATH = "network_config.yml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private CloudConfigUtil() {
    }

    public static Optional<String> extractIpv4Address(String userData) {
        if (userData == null || userData.isBlank()) {
            return Optional.empty();
        }

        String normalizedUserData = normalize(userData);

        Map<String, Object> cloudConfig = parseYamlMap(normalizedUserData);
        if (cloudConfig == null) {
            return Optional.empty();
        }

        Object writeFilesObj = cloudConfig.get("write_files");
        if (!(writeFilesObj instanceof List)) {
            return Optional.empty();
        }
        List<?> writeFiles = (List<?>) writeFilesObj;

        for (Object item : writeFiles) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> fileEntry = (Map<?, ?>) item;

            Object pathObj = fileEntry.get("path");
            Object contentObj = fileEntry.get("content");
            String configPath = String.valueOf(pathObj);

            if (StringUtils.isBlank(configPath) || !configPath.endsWith(NETWORK_CONFIG_PATH)) {
                continue;
            }

            if (contentObj == null) {
                return Optional.empty();
            }

            return extractIpv4FromNetworkConfig(String.valueOf(contentObj));
        }

        return Optional.empty();
    }

    private static Optional<String> extractIpv4FromNetworkConfig(String networkConfigYaml) {
        Map<String, Object> networkConfig = parseYamlMap(networkConfigYaml);
        if (networkConfig == null) {
            return Optional.empty();
        }

        Object interfacesObj = networkConfig.get("interfaces");
        if (!(interfacesObj instanceof List)) {
            return Optional.empty();
        }
        List<?> interfaces = (List<?>) interfacesObj;

        for (Object item : interfaces) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> iface = (Map<?, ?>) item;

            Object ipv4Obj = iface.get("ipv4");
            if (!(ipv4Obj instanceof Map)) {
                continue;
            }
            Map<?, ?> ipv4 = (Map<?, ?>) ipv4Obj;

            Object enabledObj = ipv4.get("enabled");
            if (Boolean.FALSE.equals(enabledObj)) {
                continue;
            }

            Object dhcpObj = ipv4.get("dhcp");
            if (Boolean.TRUE.equals(dhcpObj)) {
                continue;
            }

            Object addressObj = ipv4.get("address");
            if (!(addressObj instanceof List)) {
                continue;
            }
            List<?> addresses = (List<?>) addressObj;

            for (Object addressItem : addresses) {
                if (!(addressItem instanceof Map)) {
                    continue;
                }
                Map<?, ?> address = (Map<?, ?>) addressItem;

                Object ipObj = address.get("ip");
                if (ipObj != null && NetUtils.isIpv4(String.valueOf(ipObj))) {
                    return Optional.of(String.valueOf(ipObj));
                }
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseYamlMap(String yamlContent) {
        try {
            Object parsed = YAML_MAPPER.readValue(yamlContent, Object.class);
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            }
        } catch (Exception ignored) {
            // Invalid or unsupported YAML
        }
        return null;
    }

    private static String normalize(String data) {
        return data
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }
}
