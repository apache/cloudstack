//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import com.cloud.network.PublicIpQuarantine;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;

@EntityReference(value = {PublicIpQuarantine.class})
public class IpQuarantineResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the quarantine process.")
    private String id;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "The public IP address in quarantine.")
    private String publicIpAddress;

    @SerializedName(ApiConstants.PREVIOUS_OWNER_ID)
    @Param(description = "Account ID of the previous public IP address owner.")
    private String previousOwnerId;

    @SerializedName(ApiConstants.PREVIOUS_OWNER_NAME)
    @Param(description = "Account name of the previous public IP address owner.")
    private String previousOwnerName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "When the quarantine was created.")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "When the quarantine was removed.")
    private Date removed;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "End date for the quarantine.")
    private Date endDate;

    @SerializedName(ApiConstants.REMOVAL_REASON)
    @Param(description = "The reason for removing the IP from quarantine prematurely.")
    private String removalReason;

    @SerializedName(ApiConstants.REMOVER_ACCOUNT_ID)
    @Param(description = "ID of the account that removed the IP from quarantine.")
    private String removerAccountId;

    public IpQuarantineResponse() {
        super("quarantinedips");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getPreviousOwnerId() {
        return previousOwnerId;
    }

    public void setPreviousOwnerId(String previousOwnerId) {
        this.previousOwnerId = previousOwnerId;
    }

    public String getPreviousOwnerName() {
        return previousOwnerName;
    }

    public void setPreviousOwnerName(String previousOwnerName) {
        this.previousOwnerName = previousOwnerName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    public String getRemoverAccountId() {
        return removerAccountId;
    }

    public void setRemoverAccountId(String removerAccountId) {
        this.removerAccountId = removerAccountId;
    }
}
