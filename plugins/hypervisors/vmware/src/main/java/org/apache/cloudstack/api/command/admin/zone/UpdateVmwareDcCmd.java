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

package org.apache.cloudstack.api.command.admin.zone;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.VmwareDatacenter;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;
import com.cloud.user.Account;

@APICommand(name = "updateVmwareDc", description = "Updates a VMware datacenter details for a zone",
        responseObject = VmwareDatacenterResponse.class, responseHasSensitiveInfo = false,
        since = "4.12.0", authorized = {RoleType.Admin})
public class UpdateVmwareDcCmd extends BaseCmd {
    public static final Logger LOG = Logger.getLogger(UpdateVmwareDcCmd.class);


    @Inject
    public VmwareDatacenterService vmwareDatacenterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID,
            entityType = ZoneResponse.class, required = true, description = "The zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING,
            description = "VMware datacenter name.")
    private String name;

    @Parameter(name = ApiConstants.VCENTER, type = CommandType.STRING,
            description = "The name/IP of vCenter. Make sure it is IP address or full qualified domain name for host running vCenter server.")
    private String vCenter;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING,
            description = "The username required to connect to resource.")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING,
            description = "The password for specified username.")
    private String password;

    @Parameter(name = ApiConstants.IS_RECURSIVE, type = CommandType.BOOLEAN,
            description = "Specify if cluster level username/password/url and host level guid need to be updated as well. By default this is true.")
    private Boolean recursive = true;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public String getVcenter() {
        return vCenter;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Boolean isRecursive() {
        return recursive;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        final VmwareDatacenter vmwareDatacenter = vmwareDatacenterService.updateVmwareDatacenter(this);
        if (vmwareDatacenter == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update VMware datacenter");
        }
        final VmwareDatacenterResponse response = new VmwareDatacenterResponse();
        response.setId(vmwareDatacenter.getUuid());
        response.setName(vmwareDatacenter.getVmwareDatacenterName());
        response.setResponseName(getCommandName());
        response.setObjectName("vmwaredc");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
