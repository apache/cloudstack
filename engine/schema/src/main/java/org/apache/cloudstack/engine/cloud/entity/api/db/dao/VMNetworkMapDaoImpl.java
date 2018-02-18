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
package org.apache.cloudstack.engine.cloud.entity.api.db.dao;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMNetworkMapVO;

import com.cloud.network.dao.NetworkDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class VMNetworkMapDaoImpl extends GenericDaoBase<VMNetworkMapVO, Long> implements VMNetworkMapDao {

    protected SearchBuilder<VMNetworkMapVO> VmIdSearch;
    protected SearchBuilder<VMNetworkMapVO> VmNetworkSearch;

    @Inject
    protected NetworkDao _networkDao;

    public VMNetworkMapDaoImpl() {
    }

    @PostConstruct
    public void init() {
        VmIdSearch = createSearchBuilder();
        VmIdSearch.and("vmId", VmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmIdSearch.done();

        VmNetworkSearch = createSearchBuilder();
        VmNetworkSearch.and("vmId", VmNetworkSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmNetworkSearch.and("networkId", VmNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        VmNetworkSearch.done();

    }

    @Override
    public void persist(long vmId, List<Long> networks) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<VMNetworkMapVO> sc = VmIdSearch.create();
        sc.setParameters("vmId", vmId);
        expunge(sc);

        for (Long networkId : networks) {
            VMNetworkMapVO vo = new VMNetworkMapVO(vmId, networkId);
            persist(vo);
        }

        txn.commit();
    }

    @Override
    public List<Long> getNetworks(long vmId) {

        SearchCriteria<VMNetworkMapVO> sc = VmIdSearch.create();
        sc.setParameters("vmId", vmId);

        List<VMNetworkMapVO> results = search(sc, null);
        List<Long> networks = new ArrayList<Long>(results.size());
        for (VMNetworkMapVO result : results) {
            networks.add(result.getNetworkId());
        }

        return networks;
    }

    @Override
    public VMNetworkMapVO findByVmAndNetworkId(long vmId, long networkId) {

        SearchCriteria<VMNetworkMapVO> sc = VmNetworkSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("networkId", networkId);
        VMNetworkMapVO network = findOneBy(sc);

        return network;
    }
}
