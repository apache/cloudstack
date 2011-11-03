/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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


import javax.ejb.Local;

import com.cloud.network.NetworkExternalFirewallVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=NetworkExternalFirewallDao.class) @DB(txn=false)
public class NetworkExternalFirewallDaoImpl extends GenericDaoBase<NetworkExternalFirewallVO, Long> implements NetworkExternalFirewallDao {

    final SearchBuilder<NetworkExternalFirewallVO> networkIdSearch;
    
    protected NetworkExternalFirewallDaoImpl() {
        super();
        networkIdSearch = createSearchBuilder();
        networkIdSearch.and("networkId", networkIdSearch.entity().getNetworkId(), Op.EQ);
        networkIdSearch.done();
    }

    @Override
    public NetworkExternalFirewallVO findByNetworkId(long networkId) {
        SearchCriteria<NetworkExternalFirewallVO> sc = networkIdSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }
}
