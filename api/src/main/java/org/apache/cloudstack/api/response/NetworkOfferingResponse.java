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
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.offering.NetworkOffering;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = NetworkOffering.class)
@SuppressWarnings("unused")
public class NetworkOfferingResponse extends BaseResponseWithAnnotations {
    @SerializedName("id")
    @Param(description = "the id of the network offering")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the network offering")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "an alternate display text of the network offering.")
    private String displayText;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the tags for the network offering")
    private String tags;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date this network offering was created")
    private Date created;

    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    @Param(description = "the traffic type for the network offering, supported types are Public, Management, Control, Guest, Vlan or Storage.")
    private String trafficType;

    @SerializedName(ApiConstants.IS_DEFAULT)
    @Param(description = "true if network offering is default, false otherwise")
    private Boolean isDefault;

    @SerializedName(ApiConstants.SPECIFY_VLAN)
    @Param(description = "true if network offering supports vlans, false otherwise")
    private Boolean specifyVlan;

    @SerializedName(ApiConstants.CONSERVE_MODE)
    @Param(description = "true if network offering is ip conserve mode enabled")
    private Boolean conserveMode;

    @SerializedName(ApiConstants.SPECIFY_IP_RANGES)
    @Param(description = "true if network offering supports specifying ip ranges, false otherwise")
    private Boolean specifyIpRanges;

    @SerializedName(ApiConstants.AVAILABILITY)
    @Param(description = "availability of the network offering")
    private String availability;

    @SerializedName(ApiConstants.NETWORKRATE)
    @Param(description = "data transfer rate in megabits per second allowed.")
    private Integer networkRate;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of the network offering. Can be Disabled/Enabled/Inactive")
    private String state;

    @SerializedName(ApiConstants.GUEST_IP_TYPE)
    @Param(description = "guest type of the network offering, can be Shared or Isolated")
    private String guestIpType;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "the ID of the service offering used by virtual router provider")
    private String serviceOfferingId;

    @SerializedName(ApiConstants.SERVICE)
    @Param(description = "the list of supported services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;

    @SerializedName(ApiConstants.FOR_VPC)
    @Param(description = "true if network offering can be used by VPC networks only")
    private Boolean forVpc;

    @SerializedName(ApiConstants.FOR_TUNGSTEN)
    @Param(description = "true if network offering can be used by Tungsten-Fabric networks only")
    private Boolean forTungsten;

    @SerializedName(ApiConstants.IS_PERSISTENT)
    @Param(description = "true if network offering supports persistent networks, false otherwise")
    private Boolean isPersistent;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "additional key/value details tied with network offering", since = "4.2.0")
    private Map details;

    @SerializedName(ApiConstants.EGRESS_DEFAULT_POLICY)
    @Param(description = "true if guest network default egress policy is allow; false if default egress policy is deny")
    private Boolean egressDefaultPolicy;

    @SerializedName(ApiConstants.MAX_CONNECTIONS)
    @Param(description = "maximum number of concurrents connections to be handled by lb")
    private Integer concurrentConnections;

    @SerializedName(ApiConstants.SUPPORTS_STRECHED_L2_SUBNET)
    @Param(description = "true if network offering supports network that span multiple zones", since = "4.4")
    private Boolean supportsStrechedL2Subnet;

    @SerializedName(ApiConstants.SUPPORTS_PUBLIC_ACCESS)
    @Param(description = "true if network offering supports public access for guest networks", since = "4.10.0")
    private Boolean supportsPublicAccess;

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
    @Param(description = "the internet protocol of the network offering")
    private String internetProtocol;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setSpecifyVlan(Boolean specifyVlan) {
        this.specifyVlan = specifyVlan;
    }

    public void setConserveMode(Boolean conserveMode) {
        this.conserveMode = conserveMode;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public void setNetworkRate(Integer networkRate) {
        this.networkRate = networkRate;
    }

    public void setServices(List<ServiceResponse> services) {
        this.services = services;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setGuestIpType(String type) {
        this.guestIpType = type;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setSpecifyIpRanges(Boolean specifyIpRanges) {
        this.specifyIpRanges = specifyIpRanges;
    }

    public void setForVpc(Boolean forVpc) {
        this.forVpc = forVpc;
    }

    public void setForTungsten(Boolean forTungsten) {
        this.forTungsten = forTungsten;
    }

    public void setIsPersistent(Boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    public void setDetails(Map details) {
        this.details = details;
    }

    public void setEgressDefaultPolicy(Boolean egressDefaultPolicy) {
        this.egressDefaultPolicy = egressDefaultPolicy;
    }

    public void setConcurrentConnections(Integer concurrentConnections) {
        this.concurrentConnections = concurrentConnections;
    }

    public void setSupportsStrechedL2Subnet(Boolean supportsStrechedL2Subnet) {
        this.supportsStrechedL2Subnet = supportsStrechedL2Subnet;
    }

    public void setSupportsPublicAccess(Boolean supportsPublicAccess) {
        this.supportsPublicAccess = supportsPublicAccess;
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
}
