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

@Implementation(method="createDiskOffering", manager=Manager.ConfigManager, description="Creates a disk offering.")
public class CreateDiskOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDiskOfferingCmd.class.getName());

    private static final String s_name = "creatediskofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="disksize", type=CommandType.LONG, required=true)
    private Long diskSize;

    @Parameter(name="displaytext", type=CommandType.STRING, required=true)
    private String displayText;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String offeringName;

    @Parameter(name="tags", type=CommandType.STRING)
    private String tags;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDiskSize() {
        return diskSize;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getOfferingName() {
        return offeringName;
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

    @Override @SuppressWarnings("unchecked")
    public DiskOfferingResponse getResponse() {
        DiskOfferingResponse response = new DiskOfferingResponse();
        DiskOfferingVO responseObject = (DiskOfferingVO)getResponseObject();
        if (responseObject != null) {
            response.setId(responseObject.getId());
            response.setCreated(responseObject.getCreated());
            response.setDiskSize(responseObject.getDiskSize());
            response.setDisplayText(responseObject.getDisplayText());
            response.setDomainId(responseObject.getDomainId());
            response.setDomain(ApiDBUtils.findDomainById(responseObject.getDomainId()).getName());
            response.setName(responseObject.getName());
            response.setTags(responseObject.getTags());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create disk offering");
        }

        response.setResponseName(getName());
        return response;
    }
}
