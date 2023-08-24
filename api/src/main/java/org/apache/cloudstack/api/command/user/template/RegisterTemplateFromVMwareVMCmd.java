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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.template.VirtualMachineTemplate;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@APICommand(name = "registerTemplateFromVmware",
        description = "Registers a template from a stopped VM in an existing VMware vCenter.",
        responseObject = TemplateResponse.class, responseView = ResponseObject.ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RegisterTemplateFromVMwareVMCmd extends RegisterTemplateCmd {

    @Parameter(name = ApiConstants.HOST_IP,
            type = BaseCmd.CommandType.STRING,
            description = "VMware ESXi host IP/Name.")
    private String host;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_NAME,
            type = BaseCmd.CommandType.STRING,
            description = "VMware VM Name.")
    private String vmName;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "KVM zone where the end result template is registered")
    private Long zoneId;

    @Parameter(name = ApiConstants.EXISTING_VCENTER_ID,
            type = CommandType.UUID,
            entityType = VmwareDatacenterResponse.class,
            description = "UUID of a linked existing vCenter")
    private Long existingVcenterId;

    @Parameter(name = ApiConstants.VCENTER,
            type = CommandType.STRING,
            description = "The name/ip of vCenter. Make sure it is IP address or full qualified domain name for host running vCenter server.")
    private String vcenter;

    @Parameter(name = ApiConstants.DATACENTER_NAME, type = CommandType.STRING,
            description = "Name of VMware datacenter.")
    private String datacenterName;

    @Parameter(name = ApiConstants.CLUSTER_NAME, type = CommandType.STRING,
            description = "Name of VMware cluster.")
    private String clusterName;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING,
            description = "The Username required to connect to resource.")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING,
            description = "The password for the specified username.")
    private String password;

    public String getVcenter() {
        return vcenter;
    }

    public String getHost() {
        return host;
    }

    public String getVmName() {
        return vmName;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getClusterName() {
        return clusterName;
    }

    public Long getExistingVcenterId() {
        return existingVcenterId;
    }

    @Override
    protected void validateParameters() {
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
    public void execute() throws ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        validateParameters();
        VirtualMachineTemplate template = _templateService.registerTemplateFromVMwareVM(this);
        if (template != null) {
            ListResponse<TemplateResponse> response = new ListResponse<>();
            List<TemplateResponse> templateResponses = _responseGenerator.createTemplateResponses(ResponseObject.ResponseView.Restricted,
                    template, zoneId, false);
            response.setResponses(templateResponses);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to register template");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
