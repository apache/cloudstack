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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.PreallocatedLunResponse;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;

@Implementation(method="registerPreallocatedLun")
public class RegisterPreallocatedLunCmd extends BaseCmd {
    private static final String s_name = "registerPreallocatedLunsResponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    //FIXME - add description
    @Parameter(name=ApiConstants.DISK_SIZE, type=CommandType.LONG, required=true)
    private Long diskSize;

    //FIXME - add description
    @Parameter(name=ApiConstants.LUN, type=CommandType.INTEGER, required=true)
    private Integer lun;

    //FIXME - add description
    @Parameter(name=ApiConstants.PORTAL, type=CommandType.STRING, required=true)
    private String portal;

    //FIXME - add description
    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING)
    private String tags;

    @Parameter(name=ApiConstants.TARGET_IQN, type=CommandType.STRING, required=true, description="the target IQN on the storage host where LUN is created")
    private String targetIqn;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="zone ID where LUN is going to be created")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDiskSize() {
        return diskSize;
    }

    public Integer getLun() {
        return lun;
    }

    public String getPortal() {
        return portal;
    }

    public String getTags() {
        return tags;
    }

    public String getTargetIqn() {
        return targetIqn;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public PreallocatedLunResponse getResponse() {
        PreallocatedLunVO preallocatedLun = (PreallocatedLunVO)getResponseObject();
        PreallocatedLunResponse response = ApiResponseHelper.createPreallocatedLunResponse(preallocatedLun);
        response.setResponseName(getName());
        return response;
    }
}
