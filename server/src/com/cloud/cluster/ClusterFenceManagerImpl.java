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

package com.cloud.cluster;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.utils.component.Inject;

@Local(value={ClusterFenceManager.class})
public class ClusterFenceManagerImpl implements ClusterFenceManager, ClusterManagerListener {
    private static final Logger s_logger = Logger.getLogger(ClusterFenceManagerImpl.class);
	
	@Inject ClusterManager _clusterMgr;
	private String _name;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		
		_clusterMgr.registerListener(this);
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList, long selfNodeId) {
	}

	@Override
	public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList, long selfNodeId) {
	}

	@Override
	public void onManagementNodeIsolated() {
		s_logger.error("Received node isolation notification, will perform self-fencing and shut myself down");
		System.exit(SELF_FENCING_EXIT_CODE);
	}
}
