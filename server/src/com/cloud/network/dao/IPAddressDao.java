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

package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.IPAddressVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.Ip;

public interface IPAddressDao extends GenericDao<IPAddressVO, Ip> {
	
    IPAddressVO markAsUnavailable(Ip ipAddress, long ownerId);
    
	void unassignIpAddress(Ip ipAddress);	

	List<IPAddressVO> listByAccount(long accountId);
	
	List<IPAddressVO> listByDcIdIpAddress(long dcId, String ipAddress);
	
	List<IPAddressVO> listByNetwork(long networkId);
	
	int countIPs(long dcId, long vlanDbId, boolean onlyCountAllocated);
	
	int countIPs(long dcId, Long accountId, String vlanId, String vlanGateway, String vlanNetmask);
	
	boolean mark(long dcId, Ip ip);
	
	List<String> assignAcccountSpecificIps(long accountId, long longValue, Long vlanDbId, boolean sourceNat);

	int countIPsForDashboard(long dcId, boolean onlyCountAllocated);
}
