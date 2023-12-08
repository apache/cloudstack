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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceResponse;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listVmwareDcVms", responseObject = UnmanagedInstanceResponse.class,
        description = "Lists the VMs in a VMware Datacenter",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVmwareDcVmsCmd extends BaseListCmd {

    @Inject
    public VmwareDatacenterService _vmwareDatacenterService;

    @Parameter(name = ApiConstants.EXISTING_VCENTER_ID,
            type = CommandType.UUID,
            entityType = VmwareDatacenterResponse.class,
            description = "UUID of a linked existing vCenter")
    private Long existingVcenterId;

    @Parameter(name = ApiConstants.VCENTER,
            type = CommandType.STRING,
            description = "The name/ip of vCenter. Make sure it is IP address or full qualified domain name for host running vCenter server.")
    private String vcenter;

    @Parameter(name = ApiConstants.DATACENTER_NAME, type = CommandType.STRING, description = "Name of VMware datacenter.")
    private String datacenterName;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "The Username required to connect to resource.")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, description = "The password for specified username.")
    private String password;

    public String getVcenter() {
        return vcenter;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public Long getExistingVcenterId() {
        return existingVcenterId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        checkParameters();
        try {
            List<UnmanagedInstanceTO> vms = _vmwareDatacenterService.listVMsInDatacenter(this);
            List<BaseResponse> baseResponseList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(vms)) {
                for (UnmanagedInstanceTO vmwareVm : vms) {
                    UnmanagedInstanceResponse resp = _responseGenerator.createUnmanagedInstanceResponse(vmwareVm, null, null);
                    baseResponseList.add(resp);
                }
            }
            List<BaseResponse> pagingList = com.cloud.utils.StringUtils.applyPagination(baseResponseList, this.getStartIndex(), this.getPageSizeVal());
            if (CollectionUtils.isEmpty(pagingList)) {
                pagingList = baseResponseList;
            }
            ListResponse<BaseResponse> response = new ListResponse<>();
            response.setResponses(pagingList, baseResponseList.size());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            String errorMsg = String.format("Error retrieving VMs from VMware VC: %s", e.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, errorMsg);
        }
    }

    private void checkParameters() {
        if ((existingVcenterId == null && vcenter == null) || (existingVcenterId != null && vcenter != null)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please provide an existing vCenter ID or a vCenter IP/Name, parameters are mutually exclusive");
        }
        if (existingVcenterId == null && StringUtils.isAnyBlank(vcenter, datacenterName, username, password)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please set all the information for a vCenter IP/Name, datacenter, username and password");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getCommandName() {
        return "listvmwaredcvmsresponse";
    }
}
