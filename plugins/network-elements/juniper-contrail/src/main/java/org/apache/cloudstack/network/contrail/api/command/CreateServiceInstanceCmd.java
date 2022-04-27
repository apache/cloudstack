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

package org.apache.cloudstack.network.contrail.api.command;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.contrail.api.response.ServiceInstanceResponse;
import org.apache.cloudstack.network.contrail.management.ServiceManager;
import org.apache.cloudstack.network.contrail.management.ServiceVirtualMachine;

import com.cloud.dc.DataCenter;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "createServiceInstance",
            description = "Creates a system virtual-machine that implements network services",
            responseObject = ServiceInstanceResponse.class,
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class CreateServiceInstanceCmd extends BaseAsyncCreateCmd {
    private static final String s_name = "createserviceinstanceresponse";

    /// API parameters
    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = true,
               description = "Availability zone for the service instance")
    private Long zoneId;

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "An optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "An optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Project ID for the service instance")
    private Long projectId;

    @Parameter(name = "leftnetworkid",
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               required = true,
               description = "The left (inside) network for service instance")
    private Long leftNetworkId;

    @Parameter(name = "rightnetworkid",
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               required = true,
               description = "The right (outside) network ID for the service instance")
    private Long rightNetworkId;

    @Parameter(name = ApiConstants.TEMPLATE_ID,
               type = CommandType.UUID,
               entityType = TemplateResponse.class,
               required = true,
               description = "The template ID that specifies the image for the service appliance")
    private Long templateId;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
               type = CommandType.UUID,
               entityType = ServiceOfferingResponse.class,
               required = true,
               description = "The service offering ID that defines the resources consumed by the service appliance")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING,
               required = true, description = "The name of the service instance")
    private String name;

    /// Implementation
    @Inject
    ServiceManager _vrouterService;

    @Override
    public void create() throws ResourceAllocationException {
        // Parameter validation
        try {
            DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find zone ID " + zoneId);
            }

            Account owner = _accountService.getActiveAccountById(getEntityOwnerId());

            VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, templateId);
            if (template == null) {
                throw new InvalidParameterValueException("Invalid template ID " + templateId);
            }

            ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Invalid service offering ID " + serviceOfferingId);
            }

            Network left = _networkService.getNetwork(leftNetworkId);
            if (left == null) {
                throw new InvalidParameterValueException("Invalid ID for left network " + leftNetworkId);
            }

            Network right = _networkService.getNetwork(rightNetworkId);
            if (right == null) {
                throw new InvalidParameterValueException("Invalid ID for right network " + rightNetworkId);
            }

            if (name.isEmpty()) {
                throw new InvalidParameterValueException("service instance name is empty");
            }

            ServiceVirtualMachine svm = _vrouterService.createServiceInstance(zone, owner, template, serviceOffering, name, left, right);
            if (svm == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to create service instance");
            }
            setEntityId(svm.getId());
            setEntityUuid(svm.getUuid());
        } catch (Exception ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Create service instance";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {
        try {
            _vrouterService.startServiceInstance(getEntityId());
            ServiceInstanceResponse response = _vrouterService.createServiceInstanceResponse(getEntityId());
            response.setObjectName("serviceinstance");
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (Exception ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.SystemVm;
    }
}
