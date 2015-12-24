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

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.commons.lang.StringUtils;

import com.cloud.network.vpc.OSPFZoneConfig;
import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.serializer.Param;
import com.cloud.utils.net.cidr.BadCIDRException;
import com.google.gson.annotations.SerializedName;

public class VPCOSPFConfigResponse extends BaseResponse {

    @SerializedName("zoneid")
    @Param(description = "the ID of the Zone")
    private Long zoneid;

    @SerializedName("protocol")
    @Param(description = "the ospf protocol to use")
    private String protocol;

    @SerializedName("area")
    @Param(description = "the name of ospf area")
    private String area;

    @SerializedName("helloInterval")
    @Param(description = "number of seconds for HelloInterval timer value")
    private Short helloInterval;

    @SerializedName("deadInterval")
    @Param(description = "number of seconds for RouterDeadInterval timer value used for Wait Timer and Inactivity Timer.")
    private Short deadInterval;

    @SerializedName("retransmitInterval")
    @Param(description = "number of seconds for RxmtInterval timer value.")
    private Short retransmitInterval;

    @SerializedName("transitDelay")
    @Param(description = "number of seconds for InfTransDelay value. ")
    private Short transitDelay;

    @SerializedName("authentication")
    @Param(description = "the authentciation type MD5 or PlainText")
    private String authentication;

    @SerializedName("password")
    @Param(description = "the password used to secure inter quagga communication")
    private String password;

    @SerializedName("superCIDR")
    @Param(description = "the super zone level CIDR for ospf enabled VPCs")
    private String superCIDR;

    @SerializedName("enabled")
    @Param(description = "flag to enable or disable quagga for this zone")
    private Boolean enabled;

    @Inject
    public VpcProvisioningService _vpcProvSvc;

    public Long getZoneid() {
        return zoneid;
    }

    public void setZoneid(Long zoneid) {
        this.zoneid = zoneid;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String quaggaProtocol) {
        this.protocol = quaggaProtocol;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String ospfArea) {
        this.area = ospfArea;
    }

    public Short getHelloInterval() {
        return helloInterval;
    }

    public void setHelloInterval(Short quaggaHelloInterval) {
        this.helloInterval = quaggaHelloInterval;
    }

    public Short getDeadInterval() {
        return deadInterval;
    }

    public void setDeadInterval(Short quaggaDeadInterval) {
        this.deadInterval = quaggaDeadInterval;
    }

    public Short getRetransmitInterval() {
        return retransmitInterval;
    }

    public void setRetransmitInterval(Short quaggaRetransmitInterval) {
        this.retransmitInterval = quaggaRetransmitInterval;
    }

    public Short getTransitDelay() {
        return transitDelay;
    }

    public void setTransitDelay(Short quaggaTransitDelay) {
        this.transitDelay = quaggaTransitDelay;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String quaggaAuthentication) {
        this.authentication = quaggaAuthentication;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String quaggaPassword) {
        this.password = quaggaPassword;
    }

    public String getSuperCIDR() {
        return superCIDR;
    }

    public void setSuperCIDR(String ospfSuperCIDR) {
        this.superCIDR = ospfSuperCIDR;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean quaggaEnabled) {
        this.enabled = quaggaEnabled;
    }

    public VPCOSPFConfigResponse(Long zoneid, Map<String, String> details) throws BadCIDRException {
        super();
        this.zoneid = zoneid;
        OSPFZoneConfig qzc = new OSPFZoneConfig();
        qzc.setDefaultValues(details);
        this.protocol = qzc.getProtocol().name();
        this.area = qzc.getOspfArea();
        this.helloInterval = qzc.getHelloInterval();
        this.deadInterval = qzc.getDeadInterval();
        this.retransmitInterval = qzc.getRetransmitInterval();
        this.transitDelay = qzc.getTransitDelay();
        this.authentication = qzc.getAuthentication().name();
        this.password = qzc.getPassword();
        this.superCIDR = StringUtils.join(qzc.getSuperCIDR(), ',');
        this.enabled = qzc.getEnabled();
    }

}
