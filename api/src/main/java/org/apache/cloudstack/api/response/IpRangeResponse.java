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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class IpRangeResponse extends BaseResponse {

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "the gateway for the range")
    private String gateway;

    @SerializedName(ApiConstants.CIDR)
    @Param(description = "the CIDR for the range")
    private String cidr;

    @SerializedName(ApiConstants.START_IP)
    @Param(description = "the starting IP for the range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP)
    @Param(description = "the ending IP for the range")
    private String endIp;

    @SerializedName(ApiConstants.FOR_SYSTEM_VMS)
    @Param(description = "indicates if range is dedicated for CPVM and SSVM")
    private String forSystemVms;

    @SerializedName(ApiConstants.VLAN_ID)
    @Param(description = "indicates Vlan ID for the range")
    private String vlanId;

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setForSystemVms(String forSystemVms) {
        this.forSystemVms = forSystemVms;
    }

    public String getForSystemVms() {
        return forSystemVms;
    }

    public String getVlanId() {
        return vlanId;
    }

    public void setVlanId(String vlanId) {
        this.vlanId = vlanId;
    }
}
