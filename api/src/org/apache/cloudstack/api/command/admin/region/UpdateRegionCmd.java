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
package org.apache.cloudstack.api.command.admin.region;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.region.RegionService;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "updateRegion", description="Updates a region", responseObject=RegionResponse.class)
public class UpdateRegionCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateRegionCmd.class.getName());
    private static final String s_name = "updateregionresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.INTEGER, required=true, description="Id of region to update")
    private Integer id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="updates region with this name")
    private String regionName;
    
    @Parameter(name=ApiConstants.END_POINT, type=CommandType.STRING, description="updates region with this end point")
    private String endPoint;

    @Inject RegionService _regionService;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getId() {
        return id;
    }

    public String getRegionName() {
        return regionName;
    }
    
    public String getEndPoint() {
        return endPoint;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
    	Region region = _regionService.updateRegion(getId(), getRegionName(), getEndPoint());
    	if (region != null) {
    		RegionResponse response = _responseGenerator.createRegionResponse(region);
    		response.setResponseName(getCommandName());
    		this.setResponseObject(response);
    	} else {
    		throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update Region");
    	}
    }
}
