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

package com.cloud.agent.resource.virtualnetwork.model;

public abstract class ConfigBase {
    public final static String UNKNOWN = "unknown";
    public final static String VM_DHCP = "dhcpentry";
    public final static String IP_ASSOCIATION = "ips";
    public final static String GUEST_NETWORK = "guestnetwork";
    public static final String NETWORK_ACL = "networkacl";
    public static final String VM_METADATA = "vmdata";
    public static final String VM_PASSWORD = "vmpassword";
    public static final String FORWARDING_RULES = "forwardrules";

    private String type = UNKNOWN;

    private ConfigBase() {
        // Empty constructor for (de)serialization
    }

    protected ConfigBase(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
