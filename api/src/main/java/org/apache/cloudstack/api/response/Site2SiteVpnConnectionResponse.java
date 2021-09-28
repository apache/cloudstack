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
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Site2SiteVpnConnection.class)
@SuppressWarnings("unused")
public class Site2SiteVpnConnectionResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the connection ID")
    private String id;

    @SerializedName(ApiConstants.S2S_VPN_GATEWAY_ID)
    @Param(description = "the vpn gateway ID")
    private String vpnGatewayId;

    @SerializedName(ApiConstants.PUBLIC_IP)
    @Param(description = "the public IP address")
    //from VpnGateway
    private String ip;

    @SerializedName(ApiConstants.S2S_CUSTOMER_GATEWAY_ID)
    @Param(description = "the customer gateway ID")
    private String customerGatewayId;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "public ip address id of the customer gateway")
    //from CustomerGateway
    private String gatewayIp;

    @SerializedName(ApiConstants.CIDR_LIST)
    @Param(description = "guest cidr list of the customer gateway. Multiple entries are separated by a single comma character (,).")
    //from CustomerGateway
    private String guestCidrList;

    @SerializedName(ApiConstants.IPSEC_PSK)
    @Param(description = "IPsec Preshared-Key of the customer gateway", isSensitive = true)
    //from CustomerGateway
    private String ipsecPsk;

    @SerializedName(ApiConstants.IKE_POLICY)
    @Param(description = "IKE policy of the customer gateway")
    //from CustomerGateway
    private String ikePolicy;

    @SerializedName(ApiConstants.ESP_POLICY)
    @Param(description = "ESP policy of the customer gateway")
    //from CustomerGateway
    private String espPolicy;

    @SerializedName(ApiConstants.IKE_LIFETIME)
    @Param(description = "Lifetime of IKE SA of customer gateway")
    //from CustomerGateway
    private Long ikeLifetime;

    @SerializedName(ApiConstants.ESP_LIFETIME)
    @Param(description = "Lifetime of ESP SA of customer gateway")
    //from CustomerGateway
    private Long espLifetime;

    @SerializedName(ApiConstants.DPD)
    @Param(description = "if DPD is enabled for customer gateway")
    //from CustomerGateway
    private Boolean dpd;

    @SerializedName(ApiConstants.FORCE_ENCAP)
    @Param(description = "if Force NAT Encapsulation is enabled for customer gateway")
    //from CustomerGateway
    private Boolean encap;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "State of vpn connection")
    private String state;

    @SerializedName(ApiConstants.PASSIVE)
    @Param(description = "State of vpn connection")
    private boolean passive;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the owner")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the owner")
    private String domain;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date and time the host was created")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "the date and time the host was removed")
    private Date removed;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is connection for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    @SerializedName(ApiConstants.SPLIT_CONNECTIONS)
    @Param(description = "Split multiple remote networks into multiple phase 2 SAs. Often used with Cisco some products.")
    private Boolean splitConnections;

    @SerializedName(ApiConstants.IKE_VERSION)
    @Param(description = "Which IKE Version to use, one of ike (autoselect), ikev1, or ikev2. Defaults to ike")
    private String ikeVersion;

    public void setId(String id) {
        this.id = id;
    }

    public void setVpnGatewayId(String vpnGatewayId) {
        this.vpnGatewayId = vpnGatewayId;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setCustomerGatewayId(String customerGatewayId) {
        this.customerGatewayId = customerGatewayId;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public void setGuestCidrList(String guestCidrList) {
        this.guestCidrList = guestCidrList;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }

    public void setIkePolicy(String ikePolicy) {
        this.ikePolicy = ikePolicy;
    }

    public void setEspPolicy(String espPolicy) {
        this.espPolicy = espPolicy;
    }

    public void setIkeLifetime(Long ikeLifetime) {
        this.ikeLifetime = ikeLifetime;
    }

    public void setEspLifetime(Long espLifetime) {
        this.espLifetime = espLifetime;
    }

    public void setDpd(Boolean dpd) {
        this.dpd = dpd;
    }

    public void setEncap(Boolean encap) {
        this.encap = encap;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setSplitConnections(Boolean splitConnections) {
        this.splitConnections = splitConnections;
    }

    public void setIkeVersion(String ikeVersion) {
        this.ikeVersion = ikeVersion;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domain = domainName;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }

}
