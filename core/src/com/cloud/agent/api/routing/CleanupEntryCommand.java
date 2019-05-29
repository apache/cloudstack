//
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
//

package com.cloud.agent.api.routing;

public class CleanupEntryCommand extends NetworkElementCommand {

    public enum CleanupEntryType {
        DHCP_HOSTS("dhcphosts"), DNS_HOSTS("dnshosts");

        private String desc;

        CleanupEntryType(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return this.desc;
        }

    }

    private CleanupEntryType type;
    private String macAddress;
    private String ip;

    public CleanupEntryCommand(final CleanupEntryType type, final String macAddress, final String ip) {
        this.type = type;
        this.macAddress = macAddress;
        this.ip = ip;
    }

    public CleanupEntryType getType() {
        return type;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getIp() {
        return ip;
    }
}
