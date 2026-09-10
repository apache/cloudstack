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
package com.cloud.cluster;

import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.commons.lang3.StringUtils;
import com.cloud.utils.net.NetUtils;

import java.util.regex.Pattern;

/**
 * Utility class for management server address configuration detection.
 */
public class ManagementServerAddressUtil {

    // RFC 1123 compliant hostname pattern
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9]))*$"
    );

    /**
     * Validates if a string is a valid hostname according to RFC 1123.
     *
     * @param hostname the string to validate
     * @return true if the string is a valid hostname format, false otherwise
     */
    private static boolean isValidHostname(String hostname) {
        if (StringUtils.isEmpty(hostname) || hostname.length() > 253) {
            return false;
        }

        // Check each label doesn't exceed 63 characters
        String[] labels = hostname.split("\\.");
        for (String label : labels) {
            if (label.length() > 63) {
                return false;
            }
        }

        return HOSTNAME_PATTERN.matcher(hostname).matches();
    }

    /**
     * Detects if the management server address list configuration uses hostnames or IP addresses.
     * Checks all entries in the 'host' configuration (ApiServiceConfiguration.ManagementServerAddresses)
     * to determine the format.
     *
     * @return true if ALL addresses are valid hostnames, false otherwise (including if any are IPs or invalid)
     */
    public static boolean isManagementServerAddressListUsingHostnames() {
        final String msServerAddresses = ApiServiceConfiguration.ManagementServerAddresses.value();
        if (StringUtils.isEmpty(msServerAddresses)) {
            return false; // Default to IP format for safety
        }

        final String[] addresses = msServerAddresses.replace(" ", "").split(",");

        boolean hasHostname = false;
        for (String address : addresses) {
            if (StringUtils.isEmpty(address)) {
                continue;
            }

            // If it's an IP address, return false
            if (NetUtils.isValidIp4(address) || NetUtils.isValidIp6(address)) {
                return false;
            }

            // If it's not a valid hostname format, return false
            if (!isValidHostname(address)) {
                return false;
            }

            hasHostname = true;
        }

        // Only treat as hostname format if at least one valid hostname was found
        return hasHostname;
    }

    private ManagementServerAddressUtil() {
        // Utility class, prevent instantiation
    }
}
