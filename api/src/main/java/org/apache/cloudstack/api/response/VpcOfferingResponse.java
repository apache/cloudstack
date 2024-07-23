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
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.vpc.VpcOffering;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VpcOffering.class)
@SuppressWarnings("unused")
public class VpcOfferingResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the id of the vpc offering")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the vpc offering")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "an alternate display text of the vpc offering.")
    private String displayText;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date this vpc offering was created")
    private Date created;

    @SerializedName(ApiConstants.IS_DEFAULT)
    @Param(description = "true if vpc offering is default, false otherwise")
    private Boolean isDefault;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of the vpc offering. Can be Disabled/Enabled")
    private String state;

    @SerializedName(ApiConstants.SERVICE)
    @Param(description = "the list of supported services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;

    @SerializedName(ApiConstants.DISTRIBUTED_VPC_ROUTER)
    @Param(description = " indicates if the vpc offering supports distributed router for one-hop forwarding", since = "4.4")
    private Boolean supportsDistributedRouter;

    @SerializedName((ApiConstants.SUPPORTS_REGION_LEVEL_VPC))
    @Param(description = "indicated if the offering can support region level vpc", since = "4.4")
    private Boolean supportsRegionLevelVpc;

    @SerializedName(ApiConstants.FOR_NSX)
    @Param(description = "true if vpc offering can be used by NSX networks only")
    private Boolean forNsx;

    @SerializedName(ApiConstants.NETWORK_MODE)
    @Param(description = "Mode in which the network will operate. The valid values are NATTED and ROUTED")
    private String networkMode;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID(s) this disk offering belongs to. Ignore this information as it is not currently applicable.")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name(s) this disk offering belongs to. Ignore this information as it is not currently applicable.")
    private String domain;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone ID(s) this disk offering belongs to. Ignore this information as it is not currently applicable.", since = "4.13.0")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE)
    @Param(description = "the zone name(s) this disk offering belongs to. Ignore this information as it is not currently applicable.", since = "4.13.0")
    private String zone;

    @SerializedName(ApiConstants.INTERNET_PROTOCOL)
    @Param(description = "the internet protocol of the vpc offering")
    private String internetProtocol;

    @SerializedName(ApiConstants.SPECIFY_AS_NUMBER)
    @Param(description = "true if network offering supports choosing AS numbers")
    private Boolean specifyAsNumber;

    @SerializedName(ApiConstants.ROUTING_MODE)
    @Param(description = "the routing mode for the network offering, supported types are Static or Dynamic.")
    private String routingMode;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setServices(List<ServiceResponse> services) {
        this.services = services;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setSupportsDistributedRouter(Boolean supportsDistributedRouter) {
        this.supportsDistributedRouter = supportsDistributedRouter;
    }

    public void setSupportsRegionLevelVpc(Boolean supports) {
        this.supportsRegionLevelVpc = supports;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setForNsx(Boolean forNsx) {
        this.forNsx = forNsx;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getInternetProtocol() {
        return internetProtocol;
    }

    public void setInternetProtocol(String internetProtocol) {
        this.internetProtocol = internetProtocol;
    }

    public Boolean getSpecifyAsNumber() {
        return specifyAsNumber;
    }

    public void setSpecifyAsNumber(Boolean specifyAsNumber) {
        this.specifyAsNumber = specifyAsNumber;
    }

    public String getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(String routingMode) {
        this.routingMode = routingMode;
    }
}
