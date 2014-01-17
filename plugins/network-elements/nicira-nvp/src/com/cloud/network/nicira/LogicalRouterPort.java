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
package com.cloud.network.nicira;

import java.util.List;

/**
 *
 */
public class LogicalRouterPort {
    private String displayName;
    private List<NiciraNvpTag> tags;
    private Integer portno;
    private boolean adminStatusEnabled;
    private List<String> ipAddresses;
    private String macAddress;
    private final String type = "LogicalRouterPortConfig";
    private String uuid;

    public int getPortno() {
        return portno;
    }

    public void setPortno(int portno) {
        this.portno = portno;
    }

    public boolean isAdminStatusEnabled() {
        return adminStatusEnabled;
    }

    public void setAdminStatusEnabled(boolean adminStatusEnabled) {
        this.adminStatusEnabled = adminStatusEnabled;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<NiciraNvpTag> getTags() {
        return tags;
    }

    public void setTags(List<NiciraNvpTag> tags) {
        this.tags = tags;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}