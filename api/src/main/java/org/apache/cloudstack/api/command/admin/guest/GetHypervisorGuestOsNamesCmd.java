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
package org.apache.cloudstack.api.command.admin.guest;

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HypervisorGuestOsNamesResponse;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = GetHypervisorGuestOsNamesCmd.APINAME, description = "Gets the guest OS names in the hypervisor", responseObject = HypervisorGuestOsNamesResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0", authorized = {RoleType.Admin})
public class GetHypervisorGuestOsNamesCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(GetHypervisorGuestOsNamesCmd.class.getName());

    public static final String APINAME = "getHypervisorGuestOsNames";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true, description = "Hypervisor type. One of : VMware, XenServer",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String hypervisor;

    @Parameter(name = ApiConstants.HYPERVISOR_VERSION, type = CommandType.STRING, required = true, description = "Hypervisor version to get the guest os names (atleast one hypervisor host with the version specified must be available)",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String hypervisorVersion;

    @Parameter(name = ApiConstants.KEYWORD, type = CommandType.STRING, required = false, description = "Keyword for guest os name")
    private String keyword;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getHypervisor() {
        return hypervisor;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public String getKeyword() {
        return keyword;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        List<Pair<String, String>> hypervisorGuestOsNames = _mgr.getHypervisorGuestOsNames(this);
        HypervisorGuestOsNamesResponse response = _responseGenerator.createHypervisorGuestOSNamesResponse(hypervisorGuestOsNames);
        response.setHypervisor(getHypervisor());
        response.setHypervisorVersion(getHypervisorVersion());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GUEST_OS_HYPERVISOR_NAME_FETCH;
    }

    @Override
    public String getEventDescription() {
        return "Getting guest OS names from hypervisor: " + getHypervisor() + ", version: " + getHypervisorVersion();
    }
}
