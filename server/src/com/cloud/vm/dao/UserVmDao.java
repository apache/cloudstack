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

import com.cloud.uservm.UserVm;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.UserVmVO;

public interface UserVmDao extends GenericDao<UserVmVO, Long> {
    List<UserVmVO> listByAccountId(long id);

    List<UserVmVO> listByAccountAndPod(long accountId, long podId);
    List<UserVmVO> listByAccountAndDataCenter(long accountId, long dcId);
    List<UserVmVO> listByHostId(Long hostId);
    List<UserVmVO> listByLastHostId(Long hostId);
    List<UserVmVO> listUpByHostId(Long hostId);

    UserVmVO findByName(String name);
    
    /**
     * Updates display name and group for vm; enables/disables ha
     * @param id vm id.
     * @param displan name and enable for ha
     */
    void updateVM(long id, String displayName, boolean enable, Long osTypeId);
    
    List<UserVmVO> findDestroyedVms(Date date);

	/**
	 * List running VMs on the specified host
	 * @param id
	 * @return
	 */
	public List<UserVmVO> listRunningByHostId(long hostId);

	/**
	 * List user vm instances with virtualized networking (i.e. not direct attached networking) for the given account and datacenter
	 * @param accountId will search for vm instances belonging to this account
	 * @param dcId will search for vm instances in this zone
	 * @return the list of vm instances owned by the account in the given data center that have virtualized networking (not direct attached networking)
	 */
	List<UserVmVO> listVirtualNetworkInstancesByAcctAndZone(long accountId, long dcId, long networkId);
	
	List<UserVmVO> listByNetworkId(long networkId);
	
	UserVm findVmByZoneIdAndName(long zoneId, String name);
	List<UserVmVO> listByAccountIdAndHostId(long accountId, long hostId);
	
	void loadDetails(UserVmVO vm);

	void saveDetails(UserVmVO vm);
	
}
