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
package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.dc.DataCenter;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import net.juniper.tungsten.api.types.AddressType;
import net.juniper.tungsten.api.types.PolicyRuleType;
import net.juniper.tungsten.api.types.PortType;
import net.juniper.tungsten.api.types.SubnetType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

public class TungstenFabricRuleResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric rule uuid")
    private String uuid;

    @SerializedName(ApiConstants.POLICY_UUID)
    @Param(description = "Tungsten-Fabric policy uuid")
    private String policyUuid;

    @SerializedName(ApiConstants.DIRECTION)
    @Param(description = "Tungsten-Fabric policy name")
    private String direction;

    @SerializedName(ApiConstants.ACTION)
    @Param(description = "Tungsten-Fabric policy action")
    private String action;

    @SerializedName(ApiConstants.PROTOCOL)
    @Param(description = "Tungsten-Fabric policy protocol")
    private String protocol;

    @SerializedName(ApiConstants.SRC_NETWORK)
    @Param(description = "Tungsten-Fabric policy source network")
    private String srcNetwork;

    @SerializedName(ApiConstants.SRC_IP_PREFIX)
    @Param(description = "Tungsten-Fabric policy source ip prefix")
    private String srcIpPrefix;

    @SerializedName(ApiConstants.SRC_IP_PREFIX_LEN)
    @Param(description = "Tungsten-Fabric policy source ip prefix length")
    private int srcIpPrefixLen;

    @SerializedName(ApiConstants.SRC_START_PORT)
    @Param(description = "Tungsten-Fabric policy source start port")
    private int srcStartPort;

    @SerializedName(ApiConstants.SRC_END_PORT)
    @Param(description = "Tungsten-Fabric policy source end port")
    private int srcEndPort;

    @SerializedName(ApiConstants.DEST_NETWORK)
    @Param(description = "Tungsten-Fabric policy destination network")
    private String destNetwork;

    @SerializedName(ApiConstants.DEST_IP_PREFIX)
    @Param(description = "Tungsten-Fabric policy destination ip prefix")
    private String destIpPrefix;

    @SerializedName(ApiConstants.DEST_IP_PREFIX_LEN)
    @Param(description = "Tungsten-Fabric policy destination ip prefix length")
    private int destIpPrefixLen;

    @SerializedName(ApiConstants.DEST_START_PORT)
    @Param(description = "Tungsten-Fabric policy destination start port")
    private int destStartPort;

    @SerializedName(ApiConstants.DEST_END_PORT)
    @Param(description = "Tungsten-Fabric policy destination end port")
    private int destEndPort;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricRuleResponse(String policyUuid, PolicyRuleType policyRuleType, DataCenter zone) {
        this.uuid = policyRuleType.getRuleUuid();
        this.policyUuid = policyUuid;
        this.action = policyRuleType.getActionList().getSimpleAction();
        this.direction = policyRuleType.getDirection();
        this.protocol = policyRuleType.getProtocol();
        List<AddressType> srcAddress = policyRuleType.getSrcAddresses();
        if (srcAddress != null && !srcAddress.isEmpty()) {
            this.srcNetwork = srcAddress.get(0).getVirtualNetwork();
            SubnetType subnetType = srcAddress.get(0).getSubnet();
            if (subnetType != null) {
                this.srcIpPrefix = subnetType.getIpPrefix();
                this.srcIpPrefixLen = subnetType.getIpPrefixLen();
            }
        }

        List<PortType> srcPortList = policyRuleType.getSrcPorts();
        if (srcPortList != null && !srcPortList.isEmpty()) {
            this.srcStartPort = srcPortList.get(0).getStartPort();
            this.srcEndPort = srcPortList.get(0).getEndPort();
        }

        List<AddressType> destAddress = policyRuleType.getDstAddresses();
        if (destAddress != null && !destAddress.isEmpty()) {
            this.destNetwork = policyRuleType.getDstAddresses().get(0).getVirtualNetwork();
            SubnetType subnetType = destAddress.get(0).getSubnet();
            if (subnetType != null) {
                this.destIpPrefix = subnetType.getIpPrefix();
                this.destIpPrefixLen = subnetType.getIpPrefixLen();
            }
        }

        List<PortType> destPortList = policyRuleType.getDstPorts();
        if (destPortList != null && !destPortList.isEmpty()) {
            this.destStartPort = destPortList.get(0).getStartPort();
            this.destEndPort = destPortList.get(0).getEndPort();
        }
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();

        this.setObjectName("rule");
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(final String policyUuid) {
        this.policyUuid = policyUuid;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(final String direction) {
        this.direction = direction;
    }

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String getSrcNetwork() {
        return srcNetwork;
    }

    public void setSrcNetwork(final String srcNetwork) {
        this.srcNetwork = srcNetwork;
    }

    public String getSrcIpPrefix() {
        return srcIpPrefix;
    }

    public void setSrcIpPrefix(final String srcIpPrefix) {
        this.srcIpPrefix = srcIpPrefix;
    }

    public int getSrcIpPrefixLen() {
        return srcIpPrefixLen;
    }

    public void setSrcIpPrefixLen(final int srcIpPrefixLen) {
        this.srcIpPrefixLen = srcIpPrefixLen;
    }

    public int getSrcStartPort() {
        return srcStartPort;
    }

    public void setSrcStartPort(final int srcStartPort) {
        this.srcStartPort = srcStartPort;
    }

    public int getSrcEndPort() {
        return srcEndPort;
    }

    public void setSrcEndPort(final int srcEndPort) {
        this.srcEndPort = srcEndPort;
    }

    public String getDestNetwork() {
        return destNetwork;
    }

    public void setDestNetwork(final String destNetwork) {
        this.destNetwork = destNetwork;
    }

    public String getDestIpPrefix() {
        return destIpPrefix;
    }

    public void setDestIpPrefix(final String destIpPrefix) {
        this.destIpPrefix = destIpPrefix;
    }

    public int getDestIpPrefixLen() {
        return destIpPrefixLen;
    }

    public void setDestIpPrefixLen(final int destIpPrefixLen) {
        this.destIpPrefixLen = destIpPrefixLen;
    }

    public int getDestStartPort() {
        return destStartPort;
    }

    public void setDestStartPort(final int destStartPort) {
        this.destStartPort = destStartPort;
    }

    public int getDestEndPort() {
        return destEndPort;
    }

    public void setDestEndPort(final int destEndPort) {
        this.destEndPort = destEndPort;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(final String zoneName) {
        this.zoneName = zoneName;
    }
}
