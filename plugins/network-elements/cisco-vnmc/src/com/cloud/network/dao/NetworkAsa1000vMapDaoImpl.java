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
package com.cloud.network.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.cisco.NetworkAsa1000vMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = NetworkAsa1000vMapDao.class)
public class NetworkAsa1000vMapDaoImpl extends GenericDaoBase<NetworkAsa1000vMapVO, Long> implements NetworkAsa1000vMapDao {

    protected final SearchBuilder<NetworkAsa1000vMapVO> networkSearch;
    protected final SearchBuilder<NetworkAsa1000vMapVO> asa1000vSearch;

    public NetworkAsa1000vMapDaoImpl() {
        networkSearch = createSearchBuilder();
        networkSearch.and("networkId", networkSearch.entity().getNetworkId(), Op.EQ);
        networkSearch.done();

        asa1000vSearch = createSearchBuilder();
        asa1000vSearch.and("asa1000vId", asa1000vSearch.entity().getAsa1000vId(), Op.EQ);
        asa1000vSearch.done();
    }

    @Override
    public NetworkAsa1000vMapVO findByNetworkId(long networkId) {
        SearchCriteria<NetworkAsa1000vMapVO> sc = networkSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public NetworkAsa1000vMapVO findByAsa1000vId(long asa1000vId) {
        SearchCriteria<NetworkAsa1000vMapVO> sc = asa1000vSearch.create();
        sc.setParameters("asa1000vId", asa1000vId);
        return findOneBy(sc);
    }

}
