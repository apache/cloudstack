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
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;

@Implementation(method="addConfig", manager=Manager.ManagementServer)
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

    /*
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	String instance = (String) params.get(BaseCmd.Properties.INSTANCE.getName());
    	String component = (String) params.get(BaseCmd.Properties.COMPONENT.getName()); 
    	String category = (String) params.get(BaseCmd.Properties.CATEGORY.getName());
    	String name = (String) params.get(BaseCmd.Properties.NAME.getName());
    	String value = (String) params.get(BaseCmd.Properties.VALUE.getName());
    	String description = (String) params.get(BaseCmd.Properties.DESCRIPTION.getName());
    	    	
		try 
		{
			boolean status = getManagementServer().addConfig(instance, component, category, name, value, description);
			List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
			
			if(status)
			{	
				returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), name));
				returnValues.add(new Pair<String, Object>(BaseCmd.Properties.VALUE.getName(), value));
			}
            
            return returnValues;
		}
		catch (Exception ex) {
			s_logger.error("Exception adding config value: ", ex);
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add config value : " + ex.getMessage());
		}

    }
    */
}
