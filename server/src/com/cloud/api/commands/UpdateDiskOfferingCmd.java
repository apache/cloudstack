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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.storage.DiskOfferingVO;

@Implementation(method="updateDiskOffering", manager=Manager.ConfigManager)
public class UpdateDiskOfferingCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateDiskOfferingCmd.class.getName());
    private static final String s_name = "updatediskofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    

    @Parameter(name="displaytext", type=CommandType.STRING)
    private String displayText;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String diskOfferingName;

    @Parameter(name="tags", type=CommandType.STRING)
    private String tags;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public String getTags() {
        return tags;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @SuppressWarnings("unchecked")
    public DiskOfferingResponse getResponse() {
        DiskOfferingResponse response = new DiskOfferingResponse();
        DiskOfferingVO responseObject = (DiskOfferingVO)getResponseObject();
        if (responseObject != null) {
            response.setId(responseObject.getId());
            response.setCreated(responseObject.getCreated());
            response.setDiskSize(responseObject.getDiskSize());
            response.setDisplayText(responseObject.getDisplayText());
            response.setDomainId(responseObject.getDomainId());
            response.setName(responseObject.getName());
            response.setTags(responseObject.getTags());
            response.setDomainId(ApiDBUtils.findDomainById(responseObject.getDomainId()).getId());
            response.setDomain(ApiDBUtils.findDomainById(responseObject.getDomainId()).getName());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update disk offering");
        }

        response.setResponseName(getName());
        return response;
    }
}
