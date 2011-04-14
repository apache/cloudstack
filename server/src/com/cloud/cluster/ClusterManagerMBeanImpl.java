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

import java.util.Date;
import java.util.TimeZone;

import javax.management.StandardMBean;

import com.cloud.utils.DateUtil;

public class ClusterManagerMBeanImpl extends StandardMBean implements ClusterManagerMBean {
	private ClusterManagerImpl _clusterMgr;
	private ManagementServerHostVO _mshostVo;
	
	public ClusterManagerMBeanImpl(ClusterManagerImpl clusterMgr, ManagementServerHostVO mshostVo) {
		super(ClusterManagerMBean.class, false);
		
		_clusterMgr = clusterMgr;
		_mshostVo = mshostVo;
	}
	
	public long getMsid() {
		return _mshostVo.getMsid();
	}
	
	public String getLastUpdateTime() {
		Date date = _mshostVo.getLastUpdateTime();
		return DateUtil.getDateDisplayString(TimeZone.getDefault(), date);
	}
	
	public String getClusterNodeIP() {
		return _mshostVo.getServiceIP();
	}
	
	public String getVersion() {
		return _mshostVo.getVersion();
	}
	
	public int getHeartbeatInterval() {
		return _clusterMgr.getHeartbeatInterval();
	}
	
	public int getHeartbeatThreshold() {
		return _clusterMgr.getHeartbeatThreshold();
	}
	
	public void setHeartbeatThreshold(int threshold) {
		// to avoid accidentally screwing up cluster manager, we put some guarding logic here
    	if(threshold >= ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD)
    		_clusterMgr.setHeartbeatThreshold(threshold);
	}
}
