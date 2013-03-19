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

import com.cloud.network.rules.FirewallRule;
import com.cloud.serializer.Param;
import com.cloud.vm.NicSecondaryIp;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=NicSecondaryIp.class)
@SuppressWarnings("unused")
public class NicSecondaryIpResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID) @Param(description="the ID of the secondary private IP addr")
    private String id;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="Secondary IP address")
    private String ipAddr;

    @SerializedName(ApiConstants.NIC_ID) @Param(description="the ID of the nic")
    private String nicId;

    @SerializedName(ApiConstants.NETWORK_ID) @Param(description="the ID of the network")
    private  String nwId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="the ID of the vm")
    private String vmId;

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public String getId() {
        return id;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getNicId() {
        return nicId;
    }

    public void setNicId(String string) {
        this.nicId = string;
    }

    public String getNwId() {
        return nwId;
    }

    public void setNwId(String nwId) {
        this.nwId = nwId;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public void setId(String id) {
        this.id = id;
    }


}
