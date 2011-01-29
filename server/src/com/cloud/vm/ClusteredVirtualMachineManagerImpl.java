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
package com.cloud.vm;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;

@Local(value=VirtualMachineManager.class)
public class ClusteredVirtualMachineManagerImpl extends VirtualMachineManagerImpl implements ClusterManagerListener {

    protected ClusteredVirtualMachineManagerImpl() {
    }
    
    @Override
    public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList, long selfNodeId) {
        
    }
    
    @Override
    public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList, long selfNodeId) {
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        super.configure(name, xmlParams);
        
        _clusterMgr.registerListener(this);
        
        return true;
    }

}
