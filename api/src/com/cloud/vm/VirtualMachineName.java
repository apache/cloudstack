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
package com.cloud.vm;

import java.util.Formatter;

/**
 * VM Name.
 */
public class VirtualMachineName {
    public static final String SEPARATOR = "-";

    public boolean isValidCloudStackVmName(String name, String instance) {
        String[] parts = name.split(SEPARATOR);
        if (parts.length <= 1) {
            return false;
        }

        if (!parts[parts.length - 1].equals(instance)) {
            return false;
        }

        return true;
    }

    public String getVnetName(long vnetId) {
        StringBuilder vnet = new StringBuilder();
        Formatter formatter = new Formatter(vnet);
        formatter.format("%04x", vnetId);
        return vnet.toString();
    }

    public boolean isValidVmName(String vmName) {
        return isValidVmName(vmName, null);
    }

    public boolean isValidVmName(String vmName, String instance) {
        String[] tokens = vmName.split(SEPARATOR);

        if (tokens.length <= 1) {
            return false;
        }

        if (!tokens[0].equals("i")) {
            return false;
        }

        return true;
    }

    public String getVmName(long vmId, long userId, String instance) {
        StringBuilder vmName = new StringBuilder("i");
        vmName.append(SEPARATOR).append(userId).append(SEPARATOR).append(vmId);
        vmName.append(SEPARATOR).append(instance);
        return vmName.toString();
    }

    public long getVmId(String vmName) {
        int begin = vmName.indexOf(SEPARATOR);
        begin = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        int end = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(vmName.substring(begin + 1, end));
    }

    public long getRouterId(String routerName) {
        int begin = routerName.indexOf(SEPARATOR);
        int end = routerName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(routerName.substring(begin + 1, end));
    }

    public long getConsoleProxyId(String vmName) {
        int begin = vmName.indexOf(SEPARATOR);
        int end = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(vmName.substring(begin + 1, end));
    }

    public long getSystemVmId(String vmName) {
        int begin = vmName.indexOf(SEPARATOR);
        int end = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(vmName.substring(begin + 1, end));
    }

    public String getRouterName(long routerId, String instance) {
        StringBuilder builder = new StringBuilder("r");
        builder.append(SEPARATOR).append(routerId).append(SEPARATOR).append(instance);
        return builder.toString();
    }

    public String getConsoleProxyName(long vmId, String instance) {
        StringBuilder builder = new StringBuilder("v");
        builder.append(SEPARATOR).append(vmId).append(SEPARATOR).append(instance);
        return builder.toString();
    }

    public String getSystemVmName(long vmId, String instance, String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        builder.append(SEPARATOR).append(vmId).append(SEPARATOR).append(instance);
        return builder.toString();
    }

    public String attachVnet(String name, String vnet) {
        return name + SEPARATOR + vnet;
    }

    public boolean isValidRouterName(String name) {
        return isValidRouterName(name, null);
    }

    public boolean isValidRouterName(String name, String instance) {
        String[] tokens = name.split(SEPARATOR);
        if (tokens.length != 3 && tokens.length != 4) {
            return false;
        }

        if (!tokens[0].equals("r")) {
            return false;
        }

        try {
            Long.parseLong(tokens[1]);
        } catch (NumberFormatException ex) {
            return false;
        }

        return instance == null || tokens[2].equals(instance);
    }

    public boolean isValidConsoleProxyName(String name) {
        return isValidConsoleProxyName(name, null);
    }

    public boolean isValidConsoleProxyName(String name, String instance) {
        String[] tokens = name.split(SEPARATOR);
        if (tokens.length != 3) {
            return false;
        }

        if (!tokens[0].equals("v")) {
            return false;
        }

        try {
            Long.parseLong(tokens[1]);
        } catch (NumberFormatException ex) {
            return false;
        }

        return instance == null || tokens[2].equals(instance);
    }

    public boolean isValidSecStorageVmName(String name, String instance) {
        return isValidSystemVmName(name, instance, "s");
    }

    public boolean isValidSystemVmName(String name, String instance, String prefix) {
        String[] tokens = name.split(SEPARATOR);
        if (tokens.length != 3) {
            return false;
        }

        if (!tokens[0].equals(prefix)) {
            return false;
        }

        try {
            Long.parseLong(tokens[1]);
        } catch (NumberFormatException ex) {
            return false;
        }

        return instance == null || tokens[2].equals(instance);
    }
}
