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
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.response.UpgradeVmResponse;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

@Implementation(method="updateVirtualMachine", manager=Manager.UserVmManager)
public class UpdateVMCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateVMCmd.class.getName());
    private static final String s_name = "updatevirtualmachineresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="displayname", type=CommandType.STRING)
    private String displayName;

    @Parameter(name="group", type=CommandType.STRING)
    private String group;

    @Parameter(name="haenable", type=CommandType.BOOLEAN)
    private Boolean haEnable;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayName() {
        return displayName;
    }

    public String getGroup() {
        return group;
    }

    public Boolean getHaEnable() {
        return haEnable;
    }

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    private UserVmVO responseObject = null;
    
    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "virtualmachine";
    }    
    
    @Override
    public String getResponse() 
    {
        UpgradeVmResponse response = new UpgradeVmResponse();
        UserVmVO userVm = (UserVmVO)getResponseObject();
        
        UserVmVO responseObject = (UserVmVO)getResponseObject();
        if (responseObject != null) 
        {
        	//just pass back success or failure from here
        }
        
        return null;
    }

}
