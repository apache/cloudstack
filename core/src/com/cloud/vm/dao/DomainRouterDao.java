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
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.DomainRouter.Role;

/**
 *
 *  DomainRouterDao implements
 */
public interface DomainRouterDao extends GenericDao<DomainRouterVO, Long> {
	//@Deprecated
    //public boolean updateIf(DomainRouterVO router, State state, State... ifStates);

    /**
     * gets the DomainRouterVO by user id and data center
     * @Param dcId data center Id.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listByDataCenter(long dcId);
    
    /**
     * gets the DomainRouterVO by account id and data center
     * @param account id of the user.
     * @Param dcId data center Id.
     * @return DomainRouterVO
     */
    public DomainRouterVO findBy(long accountId, long dcId);
    
    /**
     * gets the DomainRouterVO by user id.
     * @param userId id of the user.
     * @Param dcId data center Id.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listBy(long userId);
    
    /**
     * Update the domainrouterVO only if the state is correct and the hostId is set.
     * 
     * @param router router object
     * @param event event that forces this update
     * @param hostId host id to set to.
     * @return true if update worked; false if not.
     */
    public boolean updateIf(DomainRouterVO router, VirtualMachine.Event event, Long hostId);
    
    /**
     * list virtual machine routers by host id.  pass in null to get all
     * virtual machine routers.
     * @param hostId id of the host.  null if to get all.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listByHostId(Long hostId);
    
    /**
     * list virtual machine routers by host id.  exclude destroyed, stopped, expunging VM, 
     * pass in null to get all
     * virtual machine routers.
     * @param hostId id of the host.  null if to get all.
     * @return list of DomainRouterVO
     */
    public List<DomainRouterVO> listUpByHostId(Long hostId);
    
    /**
     * Finds a domain router based on the ip address it is assigned to.
     * @param ipAddress
     * @return DomainRouterVO or null if not found.
     */
    public DomainRouterVO findByPublicIpAddress(String ipAddress);


    public List<Long> findLonelyRouters();
    
    /**
     * Gets the next dhcp ip address to be used for vms from this domain router.
     * @param id domain router id
     * @return next ip address
     */
    long getNextDhcpIpAddress(long id);

	/**
	 * Find the list of domain routers for a domain
	 * @param id
	 * @return
	 */
	public List<DomainRouterVO> listByDomain(Long id);
	
	/**
	 * Find the list of domain routers on a vlan
	 * @param id the id of the vlan record in the vlan table
	 * @return
	 */
	public List<DomainRouterVO> listByVlanDbId(Long vlanId);

	DomainRouterVO findBy(long accountId, long dcId, Role role);
}
