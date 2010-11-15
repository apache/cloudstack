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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ConfigurationResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

@Implementation(description="Lists all configurations.", responseObject=ConfigurationResponse.class)
public class ListCfgsByCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListCfgsByCmd.class.getName());

    private static final String s_name = "listconfigurationsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CATEGORY, type=CommandType.STRING, description="lists configurations by category")
    private String category;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="lists configuration by name")
    private String configName;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCategory() {
        return category;
    }

    public String getConfigName() {
        return configName;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        List<ConfigurationVO> result = _mgr.searchForConfigurations(this);
        ListResponse<ConfigurationResponse> response = new ListResponse<ConfigurationResponse>();
        List<ConfigurationResponse> configResponses = new ArrayList<ConfigurationResponse>();
        for (ConfigurationVO cfg : result) {
            ConfigurationResponse cfgResponse = ApiResponseHelper.createConfigurationResponse(cfg);
            cfgResponse.setObjectName("configuration");
            configResponses.add(cfgResponse);
        }

        response.setResponses(configResponses);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}
