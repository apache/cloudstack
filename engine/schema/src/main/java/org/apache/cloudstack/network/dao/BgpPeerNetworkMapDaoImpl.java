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
package org.apache.cloudstack.network.dao;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.network.BgpPeerNetworkMapVO;
import org.springframework.stereotype.Component;

import com.cloud.network.dao.NetworkDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class BgpPeerNetworkMapDaoImpl extends GenericDaoBase<BgpPeerNetworkMapVO, Long> implements BgpPeerNetworkMapDao {

    protected SearchBuilder<BgpPeerNetworkMapVO> BgpPeerIdSearch;
    protected SearchBuilder<BgpPeerNetworkMapVO> BgpPeerNetworkSearch;

    @Inject
    protected NetworkDao _networkDao;

    public BgpPeerNetworkMapDaoImpl() {
    }

    @PostConstruct
    public void init() {
        BgpPeerIdSearch = createSearchBuilder();
        BgpPeerIdSearch.and("bgpPeerId", BgpPeerIdSearch.entity().getBgpPeerId(), SearchCriteria.Op.EQ);
        BgpPeerIdSearch.done();

        BgpPeerNetworkSearch = createSearchBuilder();
        BgpPeerNetworkSearch.and("bgpPeerId", BgpPeerNetworkSearch.entity().getBgpPeerId(), SearchCriteria.Op.EQ);
        BgpPeerNetworkSearch.and("networkId", BgpPeerNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        BgpPeerNetworkSearch.done();

    }

    @Override
    public void persist(long bgpPeerId, List<Long> networks) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerIdSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        expunge(sc);

        for (Long networkId : networks) {
            BgpPeerNetworkMapVO vo = new BgpPeerNetworkMapVO(bgpPeerId, networkId);
            persist(vo);
        }

        txn.commit();
    }

    @Override
    public List<Long> listNetworksByBgpPeerId(long bgpPeerId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerIdSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);

        List<BgpPeerNetworkMapVO> results = search(sc, null);
        List<Long> networks = new ArrayList<Long>(results.size());
        for (BgpPeerNetworkMapVO result : results) {
            networks.add(result.getNetworkId());
        }
        return networks;
    }

    @Override
    public List<Long> listBgpPeersByNetworkId(long networkId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerIdSearch.create();
        sc.setParameters("networkId", networkId);

        List<BgpPeerNetworkMapVO> results = search(sc, null);
        List<Long> bgpPeers = new ArrayList<>(results.size());
        for (BgpPeerNetworkMapVO result : results) {
            bgpPeers.add(result.getBgpPeerId());
        }
        return bgpPeers;
    }

}
