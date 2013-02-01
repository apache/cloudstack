// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.network.ovs.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = { OvsTunnelNetworkDao.class })
public class OvsTunnelNetworkDaoImpl extends
		GenericDaoBase<OvsTunnelNetworkVO, Long> implements OvsTunnelNetworkDao {

	protected final SearchBuilder<OvsTunnelNetworkVO> fromToNetworkSearch;
	protected final SearchBuilder<OvsTunnelNetworkVO> fromNetworkSearch;
	protected final SearchBuilder<OvsTunnelNetworkVO> toNetworkSearch;
	
	public OvsTunnelNetworkDaoImpl() {
		fromToNetworkSearch = createSearchBuilder();
		fromToNetworkSearch.and("from", fromToNetworkSearch.entity().getFrom(), Op.EQ);
		fromToNetworkSearch.and("to", fromToNetworkSearch.entity().getTo(), Op.EQ);
		fromToNetworkSearch.and("network_id", fromToNetworkSearch.entity().getNetworkId(), Op.EQ);
		fromToNetworkSearch.done();
		
		fromNetworkSearch = createSearchBuilder();
		fromNetworkSearch.and("from", fromNetworkSearch.entity().getFrom(), Op.EQ);
		fromNetworkSearch.and("network_id", fromNetworkSearch.entity().getNetworkId(), Op.EQ);
		fromNetworkSearch.done();
		
		toNetworkSearch = createSearchBuilder();
		toNetworkSearch.and("to", toNetworkSearch.entity().getTo(), Op.EQ);
		toNetworkSearch.and("network_id", toNetworkSearch.entity().getNetworkId(), Op.EQ);
		toNetworkSearch.done();
	}
	
	@Override
	public OvsTunnelNetworkVO getByFromToNetwork(long from, long to,
			long networkId) {
		SearchCriteria<OvsTunnelNetworkVO> sc = fromToNetworkSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("network_id", networkId);
		return findOneBy(sc);
	}

    @Override
    public void removeByFromNetwork(long from, long networkId) {
        SearchCriteria<OvsTunnelNetworkVO> sc = fromNetworkSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("network_id", networkId);
        remove(sc);
    }

    @Override
    public List<OvsTunnelNetworkVO> listByToNetwork(long to, long networkId) {
        SearchCriteria<OvsTunnelNetworkVO> sc = toNetworkSearch.create();
        sc.setParameters("to", to);
        sc.setParameters("network_id", networkId);
        return listBy(sc);
    }

    @Override
    public void removeByFromToNetwork(long from, long to, long networkId) {
        SearchCriteria<OvsTunnelNetworkVO> sc = fromToNetworkSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("network_id", networkId);
        remove(sc);
    }

}
