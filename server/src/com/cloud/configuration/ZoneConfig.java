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
package com.cloud.configuration;

import java.util.ArrayList;
import java.util.List;

public enum ZoneConfig {
    EnableSecStorageVm(Boolean.class, "enable.secstorage.vm", "true", "Enables secondary storage vm service", null),
    EnableConsoleProxyVm(Boolean.class, "enable.consoleproxy.vm", "true", "Enables console proxy vm service", null),
    MaxHosts(Long.class, "max.hosts", null, "Maximum number of hosts the Zone can have", null),
    MaxVirtualMachines(Long.class, "max.vms", null, "Maximum number of VMs the Zone can have", null),
    ZoneMode(String.class, "zone.mode", null, "Mode of the Zone", "Free,Basic,Advanced"),
    HasNoPublicIp(Boolean.class, "has.no.public.ip", "false", "True if Zone has no public IP", null),
    DhcpStrategy(String.class, "zone.dhcp.strategy", "cloudstack-systemvm", "Who controls DHCP", "cloudstack-systemvm,cloudstack-external,external"),
    DnsSearchOrder(String.class, "network.guestnetwork.dns.search.order", null, "Domains list to be used for domain search order", null);

    private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;

    private static final List<String> ZoneConfigKeys = new ArrayList<String>();

    static {
        // Add keys into List
        for (ZoneConfig c : ZoneConfig.values()) {
            String key = c.key();
            ZoneConfigKeys.add(key);
        }
    }

    private ZoneConfig(Class<?> type, String name, String defaultValue, String description, String range) {

        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _range = range;
    }

    public Class<?> getType() {
        return _type;
    }

    public String getName() {
        return _name;
    }

    public String getDefaultValue() {
        return _defaultValue;
    }

    public String getDescription() {
        return _description;
    }

    public String getRange() {
        return _range;
    }

    public String key() {
        return _name;
    }

    public static boolean doesKeyExist(String key) {
        return ZoneConfigKeys.contains(key);
    }

}
