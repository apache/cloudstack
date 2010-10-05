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
import com.cloud.utils.Pair;

public class ListHypervisorsCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(UpgradeRouterCmd.class.getName());
	private static final String s_name = "listhypervisorsresponse";
	private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
	
	@Override
	public List<Pair<String, Object>> execute(Map<String, Object> params) {
		String[] hypervisors;
		try {
			hypervisors = getManagementServer().getHypervisors();
		} catch (Exception ex) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
		}
		
		Object[] soTag = null;
		if (hypervisors != null) {
			soTag = new Object[hypervisors.length];
			int i = 0;
			for (String hypervisor : hypervisors) {
				List<Pair<String, Object>> hypervisorList = new ArrayList<Pair<String, Object>>();
				hypervisorList.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), hypervisor));
				soTag[i++] = hypervisorList;
			}		 
		}
		
		Pair<String, Object> hypervisorTag = new Pair<String, Object>("hypervisor", soTag);
		List<Pair<String, Object>> hypervisorTags = new ArrayList<Pair<String, Object>>();
		hypervisorTags.add(hypervisorTag);
		return hypervisorTags;
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
