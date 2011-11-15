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

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.NetworkExternalLoadBalancerVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=NetworkExternalLoadBalancerDao.class) @DB(txn=false)
public class NetworkExternalLoadBalancerDaoImpl extends GenericDaoBase<NetworkExternalLoadBalancerVO, Long> implements NetworkExternalLoadBalancerDao {

    final SearchBuilder<NetworkExternalLoadBalancerVO> networkIdSearch;
    final SearchBuilder<NetworkExternalLoadBalancerVO> deviceIdSearch;
    
    protected NetworkExternalLoadBalancerDaoImpl() {
        super();
        networkIdSearch = createSearchBuilder();
        networkIdSearch.and("networkId", networkIdSearch.entity().getNetworkId(), Op.EQ);
        networkIdSearch.done();

        deviceIdSearch = createSearchBuilder();
        deviceIdSearch.and("externalLBDeviceId", deviceIdSearch.entity().getExternalLBDeviceId(), Op.EQ);
        deviceIdSearch.done();
    }

    @Override
    public NetworkExternalLoadBalancerVO findByNetworkId(long networkId) {
        SearchCriteria<NetworkExternalLoadBalancerVO> sc = networkIdSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public List<NetworkExternalLoadBalancerVO> listByLoadBalancerDeviceId(long lbDeviceId) {
        SearchCriteria<NetworkExternalLoadBalancerVO> sc = deviceIdSearch.create();
        sc.setParameters("externalLBDeviceId", lbDeviceId);
        return search(sc, null);
    }
}
