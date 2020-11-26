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
package org.apache.cloudstack.network.tungsten.vrouter;

import com.google.gson.annotations.SerializedName;

public class Port {
    private static final String NONE = "None";

    @SerializedName("id")
    private String id = NONE;

    @SerializedName("instance-id")
    private String instanceId = NONE;

    @SerializedName("display-name")
    private String displayName = NONE;

    @SerializedName("vn-id")
    private String vnId = NONE;

    @SerializedName("ip-address")
    private String ipAddress = NONE;

    @SerializedName("mac-address")
    private String macAddress = NONE;

    @SerializedName("vm-project-id")
    private String vmProjectId = NONE;

    @SerializedName("rx-vlan-id")
    private short rxVlanId = -1;

    @SerializedName("tx-vlan-id")
    private short txVlanId = -1;

    @SerializedName("system-name")
    private String tapInterfaceName = NONE;

    @SerializedName("type")
    private int type = 0;

    @SerializedName("ip6-address")
    private String ipv6Address = NONE;

    @SerializedName("vhostuser-mode")
    private int vifType = 0;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(final String instanceId) {
        this.instanceId = instanceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getVnId() {
        return vnId;
    }

    public void setVnId(final String vnId) {
        this.vnId = vnId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(final String macAddress) {
        this.macAddress = macAddress;
    }

    public String getVmProjectId() {
        return vmProjectId;
    }

    public void setVmProjectId(final String vmProjectId) {
        this.vmProjectId = vmProjectId;
    }

    public short getRxVlanId() {
        return rxVlanId;
    }

    public void setRxVlanId(final short rxVlanId) {
        this.rxVlanId = rxVlanId;
    }

    public short getTxVlanId() {
        return txVlanId;
    }

    public void setTxVlanId(final short txVlanId) {
        this.txVlanId = txVlanId;
    }

    public String getTapInterfaceName() {
        return tapInterfaceName;
    }

    public void setTapInterfaceName(final String tapInterfaceName) {
        this.tapInterfaceName = tapInterfaceName;
    }

    public int getType() {
        return type;
    }

    public void setType(final int type) {
        this.type = type;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(final String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public int getVifType() {
        return vifType;
    }

    public void setVifType(final int vifType) {
        this.vifType = vifType;
    }
}
