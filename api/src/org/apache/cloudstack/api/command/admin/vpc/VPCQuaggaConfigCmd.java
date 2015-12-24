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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.VPCQuaggaConfigResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;
import com.cloud.user.Account;

@APICommand(name = "vpcQuaggaConfigCmd", description = "Captures config informaton for quagga", responseObject = VPCQuaggaConfigResponse.class, since = "4.8.0", requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class VPCQuaggaConfigCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(VPCQuaggaConfigCmd.class.getName());
    private static final String s_name = "vpcquaggaconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of the Zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "the password used to secure inter quagga communication")
    private String quaggaPassword;

    @Parameter(name = ApiConstants.CIDR, type = CommandType.STRING, required = true, description = "the super zone level CIDR for ospf enabled VPCs")
    private String superCIDR;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, required = true, description = "flag to enable or disable quagga for this zone")
    private Boolean quaggaEnabled;

    public Long getId() {
        return zoneId;
    }

    public Boolean getQuaggaEnabled() {
        return quaggaEnabled;
    }

    public void setQuaggaEnabled(Boolean quaggaEnabled) {
        this.quaggaEnabled = quaggaEnabled;
    }

    public String getQuaggaPassword() {
        return quaggaPassword;
    }

    public void setQuaggaPassword(String quaggaPassword) {
        this.quaggaPassword = quaggaPassword;
    }

    public String getSuperCIDR() {
        return superCIDR;
    }

    public void setSuperCIDR(String superCIDR) {
        this.superCIDR = superCIDR;
    }

    @Override
    public void execute() {
        _vpcProvSvc.quaggaConfig(getId(), getQuaggaPassword(), getSuperCIDR(), getQuaggaEnabled());
        VPCQuaggaConfigResponse response = new VPCQuaggaConfigResponse();
        response.setResult(true);
        response.setResponseName(getCommandName());
        response.setObjectName("quaggaconfig");
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
