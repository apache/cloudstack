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
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.ResourceInUseException;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "removeVmwareDc", responseObject=SuccessResponse.class, description="Remove a VMware datacenter from a zone.")
public class RemoveVmwareDcCmd extends BaseCmd {

    @Inject public VmwareDatacenterService _vmwareDatacenterService;

    public static final Logger s_logger = Logger.getLogger(RemoveVmwareDcCmd.class.getName());

    private static final String s_name = "removevmwaredcresponse";

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType=ZoneResponse.class, required=true,
                description="The id of Zone from which VMware datacenter has to be removed.")

    private Long zoneId;

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public void execute() {
        SuccessResponse response = new SuccessResponse();
        try {
            boolean result = _vmwareDatacenterService.removeVmwareDatacenter(this);
            if (result) {
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove VMware datacenter from zone");
            }
        } catch (ResourceInUseException ex) {
            s_logger.warn("The zone has one or more resources (like cluster), hence not able to remove VMware datacenter from zone."
                        + " Please remove all resource from zone, and retry. Exception: ", ex);
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

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
