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

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.Ip;

@Local(value={RemoteAccessVpnDao.class})
public class RemoteAccessVpnDaoImpl extends GenericDaoBase<RemoteAccessVpnVO, Ip> implements RemoteAccessVpnDao {
    private static final Logger s_logger = Logger.getLogger(RemoteAccessVpnDaoImpl.class);
    
    private final SearchBuilder<RemoteAccessVpnVO> AllFieldsSearch;


    protected RemoteAccessVpnDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getServerAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public RemoteAccessVpnVO findByPublicIpAddress(String ipAddress) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public RemoteAccessVpnVO findByAccountAndNetwork(Long accountId, Long networkId) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

	@Override
	public List<RemoteAccessVpnVO> findByAccount(Long accountId) {
		SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
	}
	
	@Override
	public RemoteAccessVpnVO findByPublicIpAddressAndState(String ipAddress, RemoteAccessVpn.State state) {
	    SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("state", state);
        return findOneBy(sc);
	}
}
