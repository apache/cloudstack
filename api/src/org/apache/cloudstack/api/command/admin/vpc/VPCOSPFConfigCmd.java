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

import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.user.Account;
import com.cloud.utils.net.cidr.BadCIDRException;

@APICommand(name = "vpcOSPFConfig", description = "Return zone level ospf configuration", responseObject = VPCOSPFConfigResponse.class, since = "4.9.0", requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class VPCOSPFConfigCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(VPCOSPFConfigCmd.class);
    private static final String s_name = "vpcospfconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of the Zone")
    private Long zoneid;

    @Inject
    public VpcProvisioningService _vpcProvSvc;

    public Long getId() {
        return zoneid;
    }

    @Override
    public void execute() {
        Map<String, String> details = _vpcProvSvc.quaggaConfig(getId());
        try {
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

}
