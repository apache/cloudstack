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

import com.cloud.bgp.ASNumber;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;

@EntityReference(value = ASNumber.class)
public class ASNumberResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the AS Number")
    private String id;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "Account ID")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account name")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "Domain ID")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name")
    private String domainName;

    @SerializedName(ApiConstants.AS_NUMBER)
    @Param(description = "AS Number")
    private Long asNumber;

    @SerializedName(ApiConstants.ASN_RANGE_ID)
    @Param(description = "AS Number ID")
    private String asNumberRangeId;

    @SerializedName(ApiConstants.ASN_RANGE)
    @Param(description = "AS Number Range")
    private String asNumberRange;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the zone name of the AS Number range")
    private String zoneName;

    @SerializedName("allocated")
    @Param(description = "Allocated Date")
    private Date allocated;

    @SerializedName(ApiConstants.ALLOCATION_STATE)
    @Param(description = "Allocation state")
    private String allocationState;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_ID)
    @Param(description = "Network ID")
    private String associatedNetworkId;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_NAME)
    @Param(description = "Network Name")
    private String associatedNetworkName;

    @SerializedName((ApiConstants.VPC_ID))
    @Param(description = "VPC ID")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "VPC Name")
    private String vpcName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Created Date")
    private Date created;

    public ASNumberResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Long getAsNumber() {
        return asNumber;
    }

    public void setAsNumber(Long asNumber) {
        this.asNumber = asNumber;
    }

    public String getAsNumberRangeId() {
        return asNumberRangeId;
    }

    public void setAsNumberRangeId(String asNumberRangeId) {
        this.asNumberRangeId = asNumberRangeId;
    }

    public String getAsNumberRange() {
        return asNumberRange;
    }

    public void setAsNumberRange(String asNumberRange) {
        this.asNumberRange = asNumberRange;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Date getAllocated() {
        return allocated;
    }

    public void setAllocated(Date allocatedDate) {
        this.allocated = allocatedDate;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(String allocated) {
        allocationState = allocated;
    }

    public String getAssociatedNetworkId() {
        return associatedNetworkId;
    }

    public void setAssociatedNetworkId(String associatedNetworkId) {
        this.associatedNetworkId = associatedNetworkId;
    }

    public String getAssociatedNetworkName() {
        return associatedNetworkName;
    }

    public void setAssociatedNetworkName(String associatedNetworkName) {
        this.associatedNetworkName = associatedNetworkName;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
