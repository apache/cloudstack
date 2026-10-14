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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.cloud.utils.net.NetUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Utility class for management server address configuration detection.
 */
public class ManagementServerAddressUtil {
    private static final Logger logger = LogManager.getLogger(ManagementServerAddressUtil.class);

    private static final long RESOLUTION_CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);
    private static final Map<String, ResolvedAddress> resolutionCache = new ConcurrentHashMap<>();

    private static final class ResolvedAddress {
        private final String ip;
        private final long resolvedAt;

        private ResolvedAddress(String ip, long resolvedAt) {
            this.ip = ip;
            this.resolvedAt = resolvedAt;
        }
    }

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

    /**
     * Returns the entries of the 'host' configuration (ApiServiceConfiguration.ManagementServerAddresses) as a list,
     * in the configured order.
     */
    public static List<String> getConfiguredAddressList() {
        final String msServerAddresses = ApiServiceConfiguration.ManagementServerAddresses.value();
        if (StringUtils.isEmpty(msServerAddresses)) {
            return Collections.emptyList();
        }
        final List<String> addresses = new ArrayList<>();
        for (String address : msServerAddresses.replace(" ", "").split(",")) {
            if (StringUtils.isNotEmpty(address)) {
                addresses.add(address);
            }
        }
        return addresses;
    }

    /**
     * Convenience overload of {@link #getConfiguredAddresses(Collection, Collection)} for a single management server.
     */
    public static List<String> getConfiguredAddresses(String msHostname, String msIp) {
        return getConfiguredAddresses(msHostname != null ? List.of(msHostname) : List.of(),
                msIp != null ? List.of(msIp) : List.of());
    }

    /**
     * Returns the entries of the 'host' configuration that refer to any of the given management servers.
     * Agents only know the configured entries, so lists sent to them (avoid list, maintenance list) must use
     * this namespace rather than the hostnames and IPs persisted in the mshost table.
     * A configured entry matches when it equals one of the persisted IPs, equals one of the persisted hostnames
     * (case-insensitive) or, for a hostname alias, resolves to one of the persisted IPs. Resolutions are cached
     * for {@value #RESOLUTION_CACHE_TTL_MS} ms so this can be called on hot paths such as ping answers.
     *
     * @param msHostnames hostnames of the management servers as persisted in the mshost table
     * @param msIps service IPs of the management servers as persisted in the mshost table
     * @return the matching configured entries, in the configured order; never null
     */
    public static List<String> getConfiguredAddresses(Collection<String> msHostnames, Collection<String> msIps) {
        final Set<String> names = new HashSet<>();
        if (msHostnames != null) {
            for (String name : msHostnames) {
                if (StringUtils.isNotBlank(name)) {
                    names.add(name.toLowerCase());
                }
            }
        }
        final Set<String> ips = new HashSet<>();
        if (msIps != null) {
            for (String ip : msIps) {
                if (StringUtils.isNotBlank(ip)) {
                    ips.add(ip);
                }
            }
        }
        final List<String> matches = new ArrayList<>();
        if (names.isEmpty() && ips.isEmpty()) {
            return matches;
        }
        for (String address : getConfiguredAddressList()) {
            if (ips.contains(address) || names.contains(address.toLowerCase())) {
                matches.add(address);
                continue;
            }
            if (ips.isEmpty() || NetUtils.isValidIp4(address) || NetUtils.isValidIp6(address)) {
                continue;
            }
            final String resolvedIp = resolve(address);
            if (resolvedIp != null && ips.contains(resolvedIp)) {
                matches.add(address);
            }
        }
        return matches;
    }

    private static String resolve(String hostname) {
        final long now = System.currentTimeMillis();
        final ResolvedAddress cached = resolutionCache.get(hostname);
        if (cached != null && now - cached.resolvedAt < RESOLUTION_CACHE_TTL_MS) {
            return cached.ip;
        }
        String ip = null;
        try {
            ip = InetAddress.getByName(hostname).getHostAddress();
        } catch (UnknownHostException e) {
            logger.debug("Unable to resolve configured management server address {}", hostname);
        }
        resolutionCache.put(hostname, new ResolvedAddress(ip, now));
        return ip;
    }

    private ManagementServerAddressUtil() {
        // Utility class, prevent instantiation
    }
}
