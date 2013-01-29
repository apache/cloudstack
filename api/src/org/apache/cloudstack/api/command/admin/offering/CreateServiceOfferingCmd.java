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
package org.apache.cloudstack.api.command.admin.offering;

import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;

@APICommand(name = "createServiceOffering", description="Creates a service offering.", responseObject=ServiceOfferingResponse.class)
public class CreateServiceOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateServiceOfferingCmd.class.getName());
    private static final String _name = "createserviceofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CPU_NUMBER, type=CommandType.LONG, required=true, description="the CPU number of the service offering")
    private Long cpuNumber;

    @Parameter(name=ApiConstants.CPU_SPEED, type=CommandType.LONG, required=true, description="the CPU speed of the service offering in MHz.")
    private Long cpuSpeed;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the service offering")
    private String displayText;

    @Parameter(name=ApiConstants.MEMORY, type=CommandType.LONG, required=true, description="the total memory of the service offering in MB")
    private Long memory;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the service offering")
    private String serviceOfferingName;

    @Parameter(name=ApiConstants.OFFER_HA, type=CommandType.BOOLEAN, description="the HA for the service offering")
    private Boolean offerHa;

    @Parameter(name=ApiConstants.LIMIT_CPU_USE, type=CommandType.BOOLEAN, description="restrict the CPU usage to committed service offering")
    private Boolean limitCpuUse;

    @Parameter(name=ApiConstants.STORAGE_TYPE, type=CommandType.STRING, description="the storage type of the service offering. Values are local and shared.")
    private String storageType;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for this service offering.")
    private String tags;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class,
            description="the ID of the containing domain, null for public offerings")
    private Long domainId;

    @Parameter(name=ApiConstants.HOST_TAGS, type=CommandType.STRING, description="the host tag for this service offering.")
    private String hostTag;

    @Parameter(name=ApiConstants.IS_SYSTEM_OFFERING, type=CommandType.BOOLEAN, description="is this a system vm offering")
    private Boolean isSystem;

    @Parameter(name=ApiConstants.SYSTEM_VM_TYPE, type=CommandType.STRING, description="the system VM type. Possible types are \"domainrouter\", \"consoleproxy\" and \"secondarystoragevm\".")
    private String systemVmType;

    @Parameter(name=ApiConstants.NETWORKRATE, type=CommandType.INTEGER, description="data transfer rate in megabits per second allowed. Supported only for non-System offering and system offerings having \"domainrouter\" systemvmtype")
    private Integer networkRate;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCpuNumber() {
        return cpuNumber;
    }

    public Long getCpuSpeed() {
        return cpuSpeed;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getMemory() {
        return memory;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Boolean getOfferHa() {
        return offerHa;
    }

    public Boolean GetLimitCpuUse() {
        return limitCpuUse;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getTags() {
        return tags;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getHostTag() {
        return hostTag;
    }

    public Boolean getIsSystem() {
        return  isSystem == null ? false : isSystem;
    }

    public String getSystemVmType() {
        return systemVmType;
    }

    public Integer getNetworkRate() {
        return networkRate;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return _name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        ServiceOffering result = _configService.createServiceOffering(this);
        if (result != null) {
            ServiceOfferingResponse response = _responseGenerator.createServiceOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create service offering");
        }
    }
}
