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
import com.cloud.host.Host;
import com.cloud.server.ManagementServer;
import com.cloud.utils.Pair;

public class ListStoragePoolsAndHostsCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(ListStoragePoolsAndHostsCmd.class.getName());

    private static final String s_name = "liststoragepoolsandhostsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
    private static final ListStoragePoolsCmd storagePoolsCmd = new ListStoragePoolsCmd();
    private static final ListHostsCmd storageHostsCmd = new ListHostsCmd();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IP_ADDRESS, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PATH, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TYPE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STATE, Boolean.FALSE));
    }

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        List<Pair<String, Object>> poolTags = storagePoolsCmd.execute(params);
        List<Pair<String, Object>> hostTags = new ArrayList<Pair<String, Object>>();
        String ipAddress = (String)params.get(BaseCmd.Properties.IP_ADDRESS.getName());
        String path = (String)params.get(BaseCmd.Properties.PATH.getName());
        if (ipAddress == null && path == null){
            params.put(BaseCmd.Properties.TYPE.getName(), Host.Type.Storage.toString());
            hostTags = storageHostsCmd.execute(params);
        }
        poolTags.addAll(hostTags);
        return poolTags;
    }

    @Override
    public void setManagementServer(ManagementServer ms) {
        storagePoolsCmd.setManagementServer(ms);
        storageHostsCmd.setManagementServer(ms);
    }

    @Override
    public Map<String, Object> validateParams(Map<String, Object> params, boolean decode) {
        Map<String, Object> result = storagePoolsCmd.validateParams(params, decode);
        result.putAll(storageHostsCmd.validateParams(params, decode));
        return result;
    }
}
