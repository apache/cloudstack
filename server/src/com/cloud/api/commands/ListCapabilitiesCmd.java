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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.utils.Pair;

public class ListCapabilitiesCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(ListCapabilitiesCmd.class.getName());

    private static final String s_name = "listcapabilitiesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
            
        Map<String, String> capabilities = null;
	    capabilities = getManagementServer().listCapabilities();

        Iterator<String> iterator = capabilities.keySet().iterator();
        List<Pair<String, Object>> capabilityPair = new ArrayList<Pair<String, Object>>();
        while (iterator.hasNext()) {
            String capability = iterator.next();
            capabilityPair.add(new Pair<String, Object>(capability, capabilities.get(capability)));
        }
        return capabilityPair;
    }
}
