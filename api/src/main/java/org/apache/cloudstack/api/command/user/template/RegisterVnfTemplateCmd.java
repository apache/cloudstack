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
package org.apache.cloudstack.api.command.user.template;

import java.util.List;
import java.util.Map;

import com.cloud.network.VNF;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.storage.template.VnfTemplateUtils;

@APICommand(name = "registerVnfTemplate",
        description = "Registers an existing VNF template into the CloudStack cloud. ",
        responseObject = TemplateResponse.class, responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.19.0")
public class RegisterVnfTemplateCmd extends RegisterTemplateCmd implements UserCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VNF_NICS,
            type = CommandType.MAP,
            description = "VNF nics in key/value pairs using format vnfnics[i].keyname=keyvalue. "
                    + " Example: vnfnics[0].deviceid=0&&vnfnics[0].name=FirstNIC&&vnfnics[0].required=true"
                    + "&&vnfnics[1].deviceid=1&&vnfnics[1].name=SecondNIC")
    protected Map vnfNics;

    @Parameter(name = ApiConstants.VNF_DETAILS,
            type = CommandType.MAP,
            description = "VNF details in key/value pairs using format vnfdetails[i].keyname=keyvalue. "
                    + "Example: vnfdetails[0].vendor=xxx&&vnfdetails[0].version=2.0")
    protected Map vnfDetails;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<VNF.VnfNic> getVnfNics() {
        return VnfTemplateUtils.getVnfNicsList(this.vnfNics);
    }

    public Map<String, String> getVnfDetails() {
        return convertDetailsToMap(vnfDetails);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    protected void validateParameters() {
        super.validateParameters();

        VnfTemplateUtils.validateApiCommandParams(this, null);
    }
}
