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
import com.cloud.api.ServerApiException;
import com.cloud.server.ManagementServer;
import com.cloud.utils.Pair;

public class DeletePreallocatedLunCmd extends BaseCmd {
    
    private static final Logger s_logger = Logger.getLogger(DeletePreallocatedLunCmd.class);
    
    private static final String s_name = "deletePreallocatedLunsResponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
    }

    @Override

    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long id = (Long) params.get(BaseCmd.Properties.ID.getName());

        boolean success = false;
        ManagementServer ms = getManagementServer();
        try {
            success = ms.unregisterPreallocatedLun(id);            
        } catch (Exception e) {
            s_logger.error("Unable to unregister lun ", e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to unregister lun due to: " + e.getMessage());
        }
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), Boolean.toString(success)));
        return returnValues;
    }

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

}
