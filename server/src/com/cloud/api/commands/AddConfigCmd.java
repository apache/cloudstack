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

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ConfigurationResponse;
import com.cloud.configuration.ConfigurationVO;

@Implementation(method="addConfig", manager=Manager.ConfigManager)
public class AddConfigCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddConfigCmd.class.getName());
    private static final String s_name = "addconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="category", type=CommandType.STRING, required=true)
    private String category;

    @Parameter(name="component", type=CommandType.STRING, required=true)
    private String component;

    @Parameter(name="description", type=CommandType.STRING)
    private String description;

    @Parameter(name="instance", type=CommandType.STRING, required=true)
    private String instance;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String name;

    @Parameter(name="value", type=CommandType.STRING)
    private String value;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCategory() {
        return category;
    }

    public String getComponent() {
        return component;
    }

    public String getDescription() {
        return description;
    }

    public String getInstance() {
        return instance;
    }

    public String getConfigPropName() {
        return name;
    }

    public String getValue() {
        return value;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public ResponseObject getResponse() {
        ConfigurationResponse response = new ConfigurationResponse();
        ConfigurationVO responseObject = (ConfigurationVO)getResponseObject();
        if (responseObject != null) {
            response.setName(responseObject.getName());
            response.setValue(responseObject.getValue());
            //TODO - return description and category if needed (didn't return in 2.1 release)

        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add config");
        }

        response.setResponseName(getName());
        return response;
        //return ApiResponseSerializer.toSerializedString(response);
    }
}
