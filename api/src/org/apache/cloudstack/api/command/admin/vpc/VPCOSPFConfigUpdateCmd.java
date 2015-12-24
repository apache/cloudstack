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

package org.apache.cloudstack.api.command.admin.vpc;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VPCOSPFConfigResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.network.vpc.OSPFZoneConfig;
import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.user.Account;
import com.cloud.utils.net.cidr.BadCIDRException;

@APICommand(name = "vpcOSPFConfigUpdate", description = "Captures config informaton for ospf zone level params", responseObject = VPCOSPFConfigResponse.class, since = "4.9.0", requestHasSensitiveInfo = true)
public class VPCOSPFConfigUpdateCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(VPCOSPFConfigUpdateCmd.class);
    private static final String s_name = "vpcospfconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of the Zone")
    private Long zoneid;

    @Parameter(name = OSPFZoneConfig.s_protocol, type = CommandType.STRING, required = false, description = "the password used to secure inter ospf communication(default ospf)")
    private String protocol;

    @Parameter(name = OSPFZoneConfig.s_area, type = CommandType.STRING, required = false, description = "Specify the OSPF Area ID")
    private String area;

    @Parameter(name = OSPFZoneConfig.s_helloInterval, type = CommandType.SHORT, required = false, description = "Set number of seconds for HelloInterval timer value.(default 10)")
    private Short helloInterval;

    @Parameter(name = OSPFZoneConfig.s_deadInterval, type = CommandType.SHORT, required = false, description = "Set number of seconds for RouterDeadInterval timer value used for Wait Timer and Inactivity Timer.(default 40)")
    private Short deadInterval;

    @Parameter(name = OSPFZoneConfig.s_retransmitInterval, type = CommandType.SHORT, required = false, description = "Set number of seconds for RxmtInterval timer value.(default 5)")
    private Short retransmitInterval;

    @Parameter(name = OSPFZoneConfig.s_transitDelay, type = CommandType.SHORT, required = false, description = "Set number of seconds for InfTransDelay value.(default 1)")
    private Short transitDelay;

    @Parameter(name = OSPFZoneConfig.s_authentication, type = CommandType.STRING, required = false, description = "Md5 or PlainText (default MD5)")
    private String authentication;

    @Parameter(name = OSPFZoneConfig.s_password, type = CommandType.STRING, required = false, description = "The password used to secure inter quagga communication")
    private String password;

    @Parameter(name = OSPFZoneConfig.s_superCIDR, type = CommandType.STRING, required = false, description = "The super zone level CIDR for ospf enabled VPCs")
    private String superCIDR;

    @Parameter(name = OSPFZoneConfig.s_enabled, type = CommandType.BOOLEAN, required = false, description = "The flag to enable or disable ospf for this zone")
    private Boolean enabled;

    @Inject
    public VpcProvisioningService _vpcProvSvc;

    public Long getZoneid() {
        return zoneid;
    }

    public void setZoneid(Long zoneid) {
        this.zoneid = zoneid;
    }

    public Long getId() {
        return zoneid;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Short getHelloInterval() {
        return helloInterval;
    }

    public void setHelloInterval(Short helloInterval) {
        this.helloInterval = helloInterval;
    }

    public Short getDeadInterval() {
        return deadInterval;
    }

    public void setDeadInterval(Short deadInterval) {
        this.deadInterval = deadInterval;
    }

    public Short getRetransmitInterval() {
        return retransmitInterval;
    }

    public void setRetransmitInterval(Short retransmitInterval) {
        this.retransmitInterval = retransmitInterval;
    }

    public Short getTransitDelay() {
        return transitDelay;
    }

    public void setTransitDelay(Short transitDelay) {
        this.transitDelay = transitDelay;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSuperCIDR() {
        return superCIDR;
    }

    public void setSuperCIDR(String superCIDR) {
        this.superCIDR = superCIDR;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void execute() {
        try {
            Map<String, String> details = _vpcProvSvc.updateQuaggaConfig(getId(), getProtocol(), getArea(), getHelloInterval(), getDeadInterval(), getRetransmitInterval(),
                    getTransitDelay(), getAuthentication(), getPassword(), getSuperCIDR(), getEnabled());
            VPCOSPFConfigResponse response = new VPCOSPFConfigResponse(getId(), details);
            response.setResponseName(getCommandName());
            response.setObjectName("ospfconfig");
            setResponseObject(response);
        } catch (BadCIDRException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to return a valid response due to problems with CIDR");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String toString() {
        return "VPCOSPFConfigUpdateCmd [zoneid=" + zoneid + ", protocol=" + protocol + ", area=" + area + ", helloInterval=" + helloInterval + ", deadInterval=" + deadInterval
                + ", retransmitInterval=" + retransmitInterval + ", transitDelay=" + transitDelay + ", authentication=" + authentication + ", password=" + password + ", superCIDR="
                + superCIDR + ", enabled=" + enabled + "]";
    }

}
