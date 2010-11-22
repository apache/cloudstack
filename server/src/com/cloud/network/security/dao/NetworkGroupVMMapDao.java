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

package com.cloud.network.security.dao;

import java.util.List;

import com.cloud.network.security.NetworkGroupVMMapVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.State;

public interface NetworkGroupVMMapDao extends GenericDao<NetworkGroupVMMapVO, Long> {
    List<NetworkGroupVMMapVO> listByIpAndInstanceId(String ipAddress, long instanceId);
    List<NetworkGroupVMMapVO> listByInstanceId(long instanceId);
    List<NetworkGroupVMMapVO> listByIp(String ipAddress);
    List<NetworkGroupVMMapVO> listByNetworkGroup(long networkGroupId);
    List<NetworkGroupVMMapVO> listByNetworkGroup(long networkGroupId, State ... vmStates);
    int deleteVM(long instanceid);
	List<Long> listVmIdsByNetworkGroup(long networkGroupId);
	NetworkGroupVMMapVO findByVmIdGroupId(long instanceId, long networkGroupId);
}
