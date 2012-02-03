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

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;


/*
 * Data Access Object for vm_instance table
 */
public interface VMInstanceDao extends GenericDao<VMInstanceVO, Long>, StateDao<State, VirtualMachine.Event, VirtualMachine> {
    /**
     * What are the vms running on this host?
     * @param hostId host.
     * @return list of VMInstanceVO running on that host.
     */
	List<VMInstanceVO> listByHostId(long hostId);
	
	/**
	 * List VMs by zone ID
	 * @param zoneId
	 * @return list of VMInstanceVO in the specified zone
	 */
	List<VMInstanceVO> listByZoneId(long zoneId);
	
	/**
	 * Lists non-expunged VMs by zone ID and templateId
	 * @param zoneId
	 * @return list of VMInstanceVO in the specified zone, deployed from the specified template, that are not expunged
	 */
	public List<VMInstanceVO> listNonExpungedByZoneAndTemplate(long zoneId, long templateId);
	
    /**
     * Find vm instance with names like.
     * 
     * @param name name that fits SQL like.
     * @return list of VMInstanceVO
     */
    List<VMInstanceVO> findVMInstancesLike(String name);
    
    List<VMInstanceVO> findVMInTransition(Date time, State... states);

    List<VMInstanceVO> listByTypes(VirtualMachine.Type... types);
    
    VMInstanceVO findByIdTypes(long id, VirtualMachine.Type... types);
    
    void updateProxyId(long id, Long proxyId, Date time);

    List<VMInstanceVO> listByHostIdTypes(long hostid, VirtualMachine.Type... types);
    
    List<VMInstanceVO> listUpByHostIdTypes(long hostid, VirtualMachine.Type... types);
    List<VMInstanceVO> listByZoneIdAndType(long zoneId, VirtualMachine.Type type);
	List<VMInstanceVO> listUpByHostId(Long hostId);
	List<VMInstanceVO> listByLastHostId(Long hostId);
	
	List<VMInstanceVO> listByTypeAndState(State state, VirtualMachine.Type type);

    List<VMInstanceVO> listByAccountId(long accountId);
    public Long countAllocatedVirtualRoutersForAccount(long accountId);

    List<VMInstanceVO> listByClusterId(long clusterId);
    List<VMInstanceVO> listLHByClusterId(long clusterId);  // get all the VMs even starting one on this cluster
    List<VMInstanceVO> listVmsMigratingFromHost(Long hostId);
}
