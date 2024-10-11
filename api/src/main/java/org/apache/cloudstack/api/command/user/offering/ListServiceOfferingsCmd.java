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
package org.apache.cloudstack.api.command.user.offering;

import static com.cloud.offering.ServiceOffering.State.Active;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.offering.ServiceOffering.State;

@APICommand(name = "listServiceOfferings", description = "Lists all available service offerings.", responseObject = ServiceOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListServiceOfferingsCmd extends BaseListProjectAndAccountResourcesCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class, description = "ID of the service offering")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the service offering")
    private String serviceOfferingName;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               description = "the ID of the virtual machine. Pass this in if you want to see the available service offering that a virtual machine can be changed to.")
    private Long virtualMachineId;

    @Parameter(name=ApiConstants.IS_SYSTEM_OFFERING, type=CommandType.BOOLEAN, description="is this a system vm offering")
    private Boolean isSystem;

    @Parameter(name = ApiConstants.SYSTEM_VM_TYPE,
               type = CommandType.STRING,
               description = "the system VM type. Possible types are \"consoleproxy\", \"secondarystoragevm\" or \"domainrouter\".")
    private String systemVmType;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "id of zone disk offering is associated with",
            since = "4.13")
    private Long zoneId;

    @Parameter(name = ApiConstants.CPU_NUMBER,
            type = CommandType.INTEGER,
            description = "the CPU number that listed offerings must support",
            since = "4.15")
    private Integer cpuNumber;

    @Parameter(name = ApiConstants.MEMORY,
            type = CommandType.INTEGER,
            description = "the RAM memory that listed offering must support",
            since = "4.15")
    private Integer memory;

    @Parameter(name = ApiConstants.CPU_SPEED,
            type = CommandType.INTEGER,
            description = "the CPU speed that listed offerings must support",
            since = "4.15")
    private Integer cpuSpeed;

    @Parameter(name = ApiConstants.ENCRYPT_ROOT,
        type = CommandType.BOOLEAN,
        description = "listed offerings support root disk encryption",
        since = "4.18")
    private Boolean encryptRoot;

    @Parameter(name = ApiConstants.STORAGE_TYPE,
            type = CommandType.STRING,
            description = "the storage type of the service offering. Values are local and shared.",
            since = "4.19")
    private String storageType;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING,
               description = "Filter by state of the service offering. Defaults to 'Active'. If set to 'all' shows both Active & Inactive offerings.",
               since = "4.19")
    private String serviceOfferingState;

    @Parameter(name = ApiConstants.TEMPLATE_ID,
            type = CommandType.UUID,
            entityType = TemplateResponse.class,
            description = "The ID of the template that listed offerings must support",
            since = "4.20.0")
    private Long templateId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Boolean getIsSystem() {
        return isSystem == null ? false : isSystem;
    }

    public String getSystemVmType() {
        return systemVmType;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Integer getCpuNumber() {
        return cpuNumber;
    }

    public Integer getMemory() {
        return memory;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public Boolean getEncryptRoot() { return encryptRoot; }

    public String getStorageType() {
        return storageType;
    }

    public State getState() {
        if (StringUtils.isBlank(serviceOfferingState)) {
            return Active;
        }
        State state = EnumUtils.getEnumIgnoreCase(State.class, serviceOfferingState);
        if (!serviceOfferingState.equalsIgnoreCase("all") && state == null) {
            throw new IllegalArgumentException("Invalid state value: " + serviceOfferingState);
        }
        return state;
    }

    public Long getTemplateId() {
        return templateId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<ServiceOfferingResponse> response = _queryService.searchForServiceOfferings(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }
}
