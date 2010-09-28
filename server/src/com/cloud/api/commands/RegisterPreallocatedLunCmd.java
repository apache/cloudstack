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
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.utils.Pair;

public class RegisterPreallocatedLunCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(RegisterPreallocatedLunCmd.class);
    
    private static final String s_name = "registerPreallocatedLunsResponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TARGET_IQN, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PORTAL, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISK_SIZE, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.LUN, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TAGS, Boolean.FALSE));
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        String targetIqn = (String)params.get(BaseCmd.Properties.TARGET_IQN.getName());
        String portal = (String)params.get(BaseCmd.Properties.PORTAL.getName());
        Long size = (Long)params.get(BaseCmd.Properties.DISK_SIZE.getName());
        Long dcId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        Integer lun = (Integer)params.get(BaseCmd.Properties.LUN.getName());
        String t = (String)params.get(BaseCmd.Properties.TAGS.getName());
        
        PreallocatedLunVO registeredLun = null;
        ManagementServer ms = getManagementServer();        
        try {
            registeredLun = ms.registerPreallocatedLun(targetIqn, portal, lun, size, dcId, t);            
        } catch (Exception e) {
            s_logger.error("Unable to register lun", e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to register lun due to " + e.getMessage());
        }
        
        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        if (registeredLun == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to register LUN.");
        } else {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), registeredLun.getId()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), registeredLun.getVolumeId()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), registeredLun.getDataCenterId()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.LUN.getName(), registeredLun.getLun()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PORTAL.getName(), registeredLun.getPortal()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SIZE.getName(), registeredLun.getSize()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TAKEN.getName(), registeredLun.getTaken()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TARGET_IQN.getName(), registeredLun.getTargetIqn()));
            
            embeddedObject.add(new Pair<String, Object>("preallocatedlun", new Object[] { returnValues } ));
        }
        
        return embeddedObject;
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
