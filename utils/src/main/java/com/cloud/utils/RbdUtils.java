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

package com.cloud.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class RbdUtils {

    private RbdUtils() {
    }

    /**
     * Returns the Ceph monitors as expected by "--mon-host": the comma-separated monitors of the pool, trimmed
     * and without empty entries. If the pool has a monitor port, it is added to each monitor which has none yet.
     *
     * @param sourceHost the comma-separated monitors, IPv4 or IPv6 addresses or host names
     * @param sourcePort the port of the monitors, or 0 (or less) if the pool has none
     */
    public static String getRbdMonitors(String sourceHost, int sourcePort) {
        List<String> monitors = new ArrayList<>();
        for (String monitor : sourceHost.split(",")) {
            monitor = monitor.trim();
            if (monitor.isEmpty()) {
                continue;
            }
            monitors.add(sourcePort > 0 ? addPortToRbdMonitor(monitor, sourcePort) : monitor);
        }
        return String.join(",", monitors);
    }

    protected static String addPortToRbdMonitor(String monitor, int port) {
        if (monitor.startsWith("[")) {
            // IPv6 address in square brackets, which has a port if followed by ":<port>"
            return monitor.contains("]:") ? monitor : monitor + ":" + port;
        }
        int colons = StringUtils.countMatches(monitor, ":");
        if (colons == 0) {
            return monitor + ":" + port;
        }
        if (colons == 1) {
            // IPv4 address or host name, with a port
            return monitor;
        }
        // IPv6 address without square brackets, so without a port
        return "[" + monitor + "]:" + port;
    }

    /**
     * The monitor list as the {@code rbd:} option string of a disk needs it: ':' separates the
     * options of that string and ';' its monitors, so both are escaped inside the list, as is
     * every ':' of an IPv6 address.
     */
    public static String composeOptionForMonHosts(String monHost, int monPort) {
        List<String> hosts = new ArrayList<>();
        for (String monitor : getRbdMonitors(monHost, monPort).split(",")) {
            hosts.add(monitor.replace(":", "\\:"));
        }
        return StringUtils.join(hosts, "\\;");
    }
}
