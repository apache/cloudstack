/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;

@Implementation(description="Creates a service offering.", responseObject=ServiceOfferingResponse.class)
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

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the containing domain, null for public offerings")
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
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create service offering");
        }
    }
}
