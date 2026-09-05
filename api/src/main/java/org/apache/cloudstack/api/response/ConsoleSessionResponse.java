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
// under the License.import org.apache.cloudstack.context.CallContext;
package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import com.cloud.serializer.Param;
import org.apache.cloudstack.consoleproxy.ConsoleSession;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;

@EntityReference(value = ConsoleSession.class)
public class ConsoleSessionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the console session.")
    private String id;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Date when the console session's endpoint was created.")
    private Date created;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "Domain of the account that created the console endpoint.")
    private String domain;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Domain path of the account that created the console endpoint.")
    private String domainPath;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "Domain ID of the account that created the console endpoint.")
    private String domainId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "Account that created the console endpoint.")
    private String account;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "ID of the account that created the console endpoint.")
    private String accountId;

    @SerializedName(ApiConstants.USER)
    @Param(description = "User that created the console endpoint.")
    private String user;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "ID of the user that created the console endpoint.")
    private String userId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "ID of the virtual machine.")
    private String vmId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "Name of the virtual machine.")
    private String vmName;

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "ID of the host.")
    private String hostId;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "Name of the host.")
    private String hostName;

    @SerializedName(ApiConstants.ACQUIRED)
    @Param(description = "Date when the console session was acquired.")
    private Date acquired;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "Date when the console session was removed.")
    private Date removed;

    @SerializedName(ApiConstants.CONSOLE_ENDPOINT_CREATOR_ADDRESS)
    @Param(description = "IP address of the creator of the console endpoint.")
    private String consoleEndpointCreatorAddress;

    @SerializedName(ApiConstants.CLIENT_ADDRESS)
    @Param(description = "IP address of the client that created the console session.")
    private String clientAddress;

    public void setId(String id) {
        this.id = id;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setAcquired(Date acquired) {
        this.acquired = acquired;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setConsoleEndpointCreatorAddress(String consoleEndpointCreatorAddress) {
        this.consoleEndpointCreatorAddress = consoleEndpointCreatorAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public String getDomain() {
        return domain;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getAccount() {
        return account;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUser() {
        return user;
    }

    public String getUserId() {
        return userId;
    }

    public String getVmId() {
        return vmId;
    }

    public String getVmName() {
        return vmName;
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public Date getAcquired() {
        return acquired;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getConsoleEndpointCreatorAddress() {
        return consoleEndpointCreatorAddress;
    }

    public String getClientAddress() {
        return clientAddress;
    }
}
