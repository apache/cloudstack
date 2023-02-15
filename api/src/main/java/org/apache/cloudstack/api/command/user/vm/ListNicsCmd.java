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
package org.apache.cloudstack.api.command.user.vm;

import java.util.ArrayList;
import java.util.List;
import com.cloud.vm.NicSecondaryIp;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.vm.Nic;

@APICommand(name = "listNics", description = "list the vm nics  IP to NIC", responseObject = NicResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListNicsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListNicsCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NIC_ID, type = CommandType.UUID, entityType = NicResponse.class, required = false, description = "the ID of the nic to to list IPs")
    private Long nicId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "the ID of the vm")
    private Long vmId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list nic of the specific vm's network")
    private Long networkId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
        return "nics";
    }

    public String getAccountName() {
        return CallContext.current().getCallingAccount().getAccountName();
    }

    public long getDomainId() {
        return CallContext.current().getCallingAccount().getDomainId();
    }

    public Long getNicId() {
        return nicId;
    }

    public Long getVmId() {
        return vmId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        return caller.getAccountId();
    }


    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return true;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public static String getResultObjectName() {
        return "addressinfo";
    }

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {

        try {
            if (this.getKeyword() != null && !this.getKeyword().isEmpty() && this.getNicId() != null) {
                List<? extends NicSecondaryIp> results = _networkService.listVmNicSecondaryIps(this);
                ListResponse<NicSecondaryIpResponse> response = new ListResponse<NicSecondaryIpResponse>();
                List<NicSecondaryIpResponse> resList = new ArrayList<NicSecondaryIpResponse>();
                NicSecondaryIpResponse res = new NicSecondaryIpResponse();
                List<NicSecondaryIpResponse> res_List = new ArrayList<NicSecondaryIpResponse>();
                if (results != null) {
                    for (NicSecondaryIp r : results) {
                        NicSecondaryIpResponse ipRes = _responseGenerator.createSecondaryIPToNicResponse(r);
                        resList.add(ipRes);
                        res.setSecondaryIpsList(resList);
                        res.setObjectName("nic");
                    }

                    res_List.add(res);
                    response.setResponses(res_List);
                }
                response.setResponses(res_List);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);

            } else {
                List<? extends Nic> results = _networkService.listNics(this);
                ListResponse<NicResponse> response = new ListResponse<NicResponse>();
                List<NicResponse> resList = null;
                if (results != null) {
                    resList = new ArrayList<NicResponse>(results.size());
                    for (Nic r : results) {
                        NicResponse resp = _responseGenerator.createNicResponse(r);
                        resp.setObjectName("nic");
                        resList.add(resp);
                    }
                    response.setResponses(resList);
                }

                response.setResponses(resList);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            }
        } catch (Exception e) {
            s_logger.warn("Failed to list secondary ip address per nic ");
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.IpAddress;
    }

}
