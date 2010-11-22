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

package com.cloud.vm.dao;

import java.util.Date;
import java.util.List;

import com.cloud.info.ConsoleProxyLoadInfo;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachine;

public interface ConsoleProxyDao extends GenericDao<ConsoleProxyVO, Long> {
	
    public void update(long id, int activeSession, Date updateTime, byte[] sessionDetails);

    public List<ConsoleProxyVO> getProxyListInStates(long dataCenterId, State... states);
    public List<ConsoleProxyVO> getProxyListInStates(State... states);
    
    public List<ConsoleProxyVO> listByHostId(long hostId);
    public List<ConsoleProxyVO> listUpByHostId(long hostId);
    
    public List<ConsoleProxyLoadInfo> getDatacenterProxyLoadMatrix();
    public List<ConsoleProxyLoadInfo> getDatacenterVMLoadMatrix();
    public List<ConsoleProxyLoadInfo> getDatacenterSessionLoadMatrix();
    public List<Pair<Long, Integer>> getDatacenterStoragePoolHostInfo(long dcId, boolean countAllPoolTypes);
    public List<Pair<Long, Integer>> getProxyLoadMatrix();
    public int getProxyStaticLoad(long proxyVmId);
    public int getProxyActiveLoad(long proxyVmId);
    public List<Long> getRunningProxyListByMsid(long msid);
    
    public boolean updateIf(ConsoleProxyVO vm, VirtualMachine.Event event, Long hostId);
}
