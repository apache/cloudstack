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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.DiscoveryException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;
import com.cloud.hypervisor.vmware.VmwareDatacenterVO;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "addVmwareDc", description = "Adds a VMware datacenter to specified zone", responseObject = VmwareDatacenterResponse.class,
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class AddVmwareDcCmd extends BaseCmd {

    @Inject
    public VmwareDatacenterService _vmwareDatacenterService;

    public static final Logger s_logger = Logger.getLogger(AddVmwareDcCmd.class.getName());


    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of VMware datacenter to be added to specified zone.")
    private String name;

    @Parameter(name = ApiConstants.VCENTER,
               type = CommandType.STRING,
               required = true,
               description = "The name/ip of vCenter. Make sure it is IP address or full qualified domain name for host running vCenter server.")
    private String vCenter;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = false, description = "The Username required to connect to resource.")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = false, description = "The password for specified username.")
    private String password;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "The Zone ID.")
    private Long zoneId;

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

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        try {
            VmwareDatacenterResponse response = new VmwareDatacenterResponse();
            VmwareDatacenterVO result = _vmwareDatacenterService.addVmwareDatacenter(this);
            if (result != null) {
                response.setId(result.getUuid());
                response.setName(result.getVmwareDatacenterName());
                response.setResponseName(getCommandName());
                response.setObjectName("vmwaredc");
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add VMware Datacenter to zone.");
            }
            this.setResponseObject(response);
        } catch (DiscoveryException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (ResourceInUseException ex) {
            s_logger.warn("Exception: ", ex);
            ServerApiException e = new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
            for (String proxyObj : ex.getIdProxyList()) {
                e.addProxyObject(proxyObj);
            }
            throw e;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        } catch (CloudRuntimeException runtimeEx) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeEx.getMessage());
        }
    }
}
