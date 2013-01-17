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
package com.cloud.api.response;

import org.apache.cloudstack.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.CiscoNexusVSMDevice;

@EntityReference(value=CiscoNexusVSMDevice.class)
public class CiscoNexusVSMResponse extends BaseResponse {

    @SerializedName(ApiConstants.EXTERNAL_SWITCH_MGMT_DEVICE_ID) @Param(description="device id of the Cisco N1KV VSM device")
    private String id;

    @SerializedName(ApiConstants.EXTERNAL_SWITCH_MGMT_DEVICE_NAME) @Param(description="device name")
    private String deviceName;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the management IP address of the external Cisco Nexus 1000v Virtual Supervisor Module")
    private String vsmmgmtIpAddress;

    @SerializedName(ApiConstants.EXTERNAL_SWITCH_MGMT_DEVICE_STATE) @Param(description="device state")
    private String deviceState;

    @SerializedName(ApiConstants.VSM_MGMT_VLAN_ID) @Param(description="management vlan id of the VSM")
    private String vsmmgmtvlanid;

    @SerializedName(ApiConstants.VSM_CTRL_VLAN_ID) @Param(description="control vlan id of the VSM")
    private int vsmctrlvlanid;

    @SerializedName(ApiConstants.VSM_PKT_VLAN_ID) @Param(description="packet vlan id of the VSM")
    private int vsmpktvlanid;

    @SerializedName(ApiConstants.VSM_STORAGE_VLAN_ID) @Param(description="storage vlan id of the VSM")
    private int vsmstoragevlanid;

    @SerializedName(ApiConstants.VSM_DOMAIN_ID) @Param(description="The VSM is a switch supervisor. This is the VSM's switch domain id")
    private String vsmdomainid;

    @SerializedName(ApiConstants.VSM_CONFIG_MODE) @Param(description="The mode of the VSM (standalone/HA)")
    private String vsmconfigmode;

    @SerializedName(ApiConstants.VSM_CONFIG_STATE) @Param(description="The Config State (Primary/Standby) of the VSM")
    private String vsmconfigstate;

    @SerializedName(ApiConstants.VSM_DEVICE_STATE) @Param(description="The Device State (Enabled/Disabled) of the VSM")
    private String vsmdevicestate;

    // Setter methods.
    public void setId(String vsmDeviceId) {
        this.id = vsmDeviceId;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setMgmtIpAddress(String ipAddress) {
        this.vsmmgmtIpAddress = ipAddress;
    }

    public void setDeviceState(String deviceState) {
    	this.deviceState = deviceState;
    }

    public void setVSMMgmtVlanId(String vlanId) {
    	this.vsmmgmtvlanid = vlanId;
    }

    public void setVSMCtrlVlanId(int vlanId) {
    	this.vsmctrlvlanid = vlanId;
    }

    public void setVSMPktVlanId(int vlanId) {
    	this.vsmpktvlanid = vlanId;
    }

    public void setVSMStorageVlanId(int vlanId) {
    	this.vsmstoragevlanid = vlanId;
    }

    public void setVSMDomainId(String domId) {
    	this.vsmdomainid = domId;
    }

    public void setVSMConfigMode(String configMode) {
    	this.vsmconfigmode = configMode;
    }

    public void setVSMConfigState(String configState) {
    	this.vsmconfigstate = configState;
    }

    public void setVSMDeviceState(String devState) {
    	this.vsmdevicestate = devState;
    }
}
