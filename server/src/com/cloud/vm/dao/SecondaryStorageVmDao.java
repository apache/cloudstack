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

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine.State;

public interface SecondaryStorageVmDao extends GenericDao<SecondaryStorageVmVO, Long> {
	
    public List<SecondaryStorageVmVO> getSecStorageVmListInStates(SecondaryStorageVm.Role role, long dataCenterId, State... states);
    public List<SecondaryStorageVmVO> getSecStorageVmListInStates(SecondaryStorageVm.Role role, State... states);
    
    public List<SecondaryStorageVmVO> listByHostId(SecondaryStorageVm.Role role, long hostId);
    public List<SecondaryStorageVmVO> listByLastHostId(SecondaryStorageVm.Role role, long hostId);
    
    public List<SecondaryStorageVmVO> listUpByHostId(SecondaryStorageVm.Role role, long hostId);
    
    public List<SecondaryStorageVmVO> listByZoneId(SecondaryStorageVm.Role role, long zoneId);
    
    public List<Long> getRunningSecStorageVmListByMsid(SecondaryStorageVm.Role role, long msid);
    
    public List<Long> listRunningSecStorageOrderByLoad(SecondaryStorageVm.Role role, long zoneId);
    SecondaryStorageVmVO findByInstanceName(String instanceName);
}
