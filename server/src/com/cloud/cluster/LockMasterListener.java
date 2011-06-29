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

import com.cloud.utils.db.Merovingian2;

/**
 * This listener is specifically written to cause cleanups in the Merovingian
 * when a management server is down.
 *
 */
public class LockMasterListener implements ClusterManagerListener {
    Merovingian2 _lockMaster;
    
    public LockMasterListener(long msId) {
        _lockMaster = Merovingian2.createLockMaster(msId);
    }

    @Override
    public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList, long selfNodeId) {
        for (ManagementServerHostVO node : nodeList) {
            _lockMaster.cleanupForServer(node.getMsid());
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

}
