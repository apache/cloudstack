// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.vm.dao.UserVmDetailsDao;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;
import java.util.Map;

@Component
@Local(value=NetworkDetailsDao.class)
public class NetworkDetailsDaoImpl extends GenericDaoBase<NetworkDetailVO, Long> implements NetworkDetailsDao {

    protected final SearchBuilder<NetworkDetailVO> NetworkSearch;
    protected final SearchBuilder<NetworkDetailVO> DetailSearch;

    public NetworkDetailsDaoImpl() {
        NetworkSearch = createSearchBuilder();
        NetworkSearch.and("networkId", NetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("networkId", DetailSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }
    
    
    @Override
    public List<NetworkDetailVO> findDetails(long networkId) {
        SearchCriteria<NetworkDetailVO> sc = NetworkSearch.create();
        sc.setParameters("networkId", networkId);

        List<NetworkDetailVO> results = search(sc, null);
        return results;
    }

    @Override
    public void persist(long networkId, Map<String, String> details) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkDetailVO findDetail(long networkId, String name) {
        SearchCriteria<NetworkDetailVO> sc = DetailSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("name", name);

        return findOneBy(sc);    }

    @Override
    public void deleteDetails(long networkId) {
        SearchCriteria<NetworkDetailVO> sc = NetworkSearch.create();
        sc.setParameters("networkId", networkId);

        List<NetworkDetailVO> results = search(sc, null);
        for (NetworkDetailVO result : results) {
            remove(result.getId());
        }	
    }

    @Override
    public void removeDetails(Long networkId, String key) {
        if(key != null){
            NetworkDetailVO detail = findDetail(networkId, key);
            if(detail != null){
                remove(detail.getId());
            }
        }else {
            deleteDetails(networkId);
        }
    }
}
