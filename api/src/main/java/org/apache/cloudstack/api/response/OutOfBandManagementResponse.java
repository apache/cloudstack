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

import com.cloud.host.Host;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.commons.lang3.StringUtils;

@EntityReference(value = Host.class)
public class OutOfBandManagementResponse extends BaseResponse {
    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "the ID of the host")
    private String id;

    @SerializedName(ApiConstants.POWER_STATE)
    @Param(description = "the out-of-band management interface powerState of the host")
    private OutOfBandManagement.PowerState powerState;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "true if out-of-band management is enabled for the host")
    private Boolean enabled;

    @SerializedName(ApiConstants.DRIVER)
    @Param(description = "the out-of-band management driver for the host")
    private String driver;

    @SerializedName(ApiConstants.ADDRESS)
    @Param(description = "the out-of-band management interface address")
    private String ipAddress;

    @SerializedName(ApiConstants.PORT)
    @Param(description = "the out-of-band management interface port")
    private String port;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "the out-of-band management interface username")
    private String username;

    @SerializedName(ApiConstants.PASSWORD)
    @Param(description = "the out-of-band management interface password")
    private String password;

    @SerializedName(ApiConstants.ACTION)
    @Param(description = "the out-of-band management action (if issued)")
    private String outOfBandManagementAction;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the operation result description")
    private String resultDescription;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "the operation result")
    private Boolean success;

    public OutOfBandManagementResponse() {
        super("outofbandmanagement");
    }

    public OutOfBandManagementResponse(final OutOfBandManagement outOfBandManagementConfig) {
        this();
        if (outOfBandManagementConfig == null) {
            this.setEnabled(false);
            this.setPowerState(OutOfBandManagement.PowerState.Disabled);
            return;
        }
        this.setEnabled(outOfBandManagementConfig.isEnabled());
        if (outOfBandManagementConfig.getPowerState() != null) {
            this.setPowerState(outOfBandManagementConfig.getPowerState());
        } else {
            this.setPowerState(OutOfBandManagement.PowerState.Unknown);
        }
        this.setDriver(outOfBandManagementConfig.getDriver());
        this.setIpAddress(outOfBandManagementConfig.getAddress());
        if (outOfBandManagementConfig.getPort() != null) {
            this.setPort(outOfBandManagementConfig.getPort());
        }
        this.setUsername(outOfBandManagementConfig.getUsername());
        if (StringUtils.isNotEmpty(outOfBandManagementConfig.getPassword())) {
            this.setPassword(outOfBandManagementConfig.getPassword().substring(0, 1) + "****");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OutOfBandManagement.PowerState getPowerState() {
        return powerState;
    }

    public void setPowerState(OutOfBandManagement.PowerState powerState) {
        this.powerState = powerState;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOutOfBandManagementAction() {
        return outOfBandManagementAction;
    }

    public void setOutOfBandManagementAction(String outOfBandManagementAction) {
        this.outOfBandManagementAction = outOfBandManagementAction;
    }

    public String getResultDescription() {
        return resultDescription;
    }

    public void setResultDescription(String resultDescription) {
        this.resultDescription = resultDescription;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
