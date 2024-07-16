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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.db.JoinBuilder;
import org.apache.cloudstack.network.BgpPeer;
import org.apache.cloudstack.network.BgpPeerNetworkMapVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class BgpPeerNetworkMapDaoImpl extends GenericDaoBase<BgpPeerNetworkMapVO, Long> implements BgpPeerNetworkMapDao {

    protected SearchBuilder<BgpPeerNetworkMapVO> BgpPeerNetworkSearch;
    protected SearchBuilder<BgpPeerNetworkMapVO> DomainAccountNeqSearch;

    @Inject
    NetworkDao networkDao;

    public BgpPeerNetworkMapDaoImpl() {
    }

    @PostConstruct
    public void init() {
        BgpPeerNetworkSearch = createSearchBuilder();
        BgpPeerNetworkSearch.and("bgpPeerId", BgpPeerNetworkSearch.entity().getBgpPeerId(), SearchCriteria.Op.EQ);
        BgpPeerNetworkSearch.and("networkId", BgpPeerNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        BgpPeerNetworkSearch.done();

        final SearchBuilder<NetworkVO> networkSearchBuilder = networkDao.createSearchBuilder();
        networkSearchBuilder.and("domainId", networkSearchBuilder.entity().getDomainId(), SearchCriteria.Op.NEQ);
        networkSearchBuilder.and("accountId", networkSearchBuilder.entity().getAccountId(), SearchCriteria.Op.NEQ);
        DomainAccountNeqSearch = createSearchBuilder();
        DomainAccountNeqSearch.and("bgpPeerId", DomainAccountNeqSearch.entity().getBgpPeerId(), SearchCriteria.Op.EQ);
        DomainAccountNeqSearch.join("network", networkSearchBuilder, networkSearchBuilder.entity().getId(),
                DomainAccountNeqSearch.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        DomainAccountNeqSearch.done();

    }

    @Override
    public void persist(long networkId, List<Long> bgpPeerIds) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkSearch.create();
        sc.setParameters("networkId", networkId);
        expunge(sc);

        for (Long bgpPeerId : bgpPeerIds) {
            BgpPeerNetworkMapVO vo = new BgpPeerNetworkMapVO(bgpPeerId, networkId, BgpPeer.State.Active);
            persist(vo);
        }

        txn.commit();
    }

    @Override
    public BgpPeerNetworkMapVO findByBgpPeerIdAndNetworkId(long bgpPeerId, long networkId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        sc.setParameters("networkId", networkId);
        return findOneBy(sc, null);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listByBgpPeerId(long bgpPeerId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);

        return search(sc, null);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listByNetworkId(long networkId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkSearch.create();
        sc.setParameters("networkId", networkId);

        return search(sc, null);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listUsedByOtherDomains(long bgpPeerId, Long domainId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = DomainAccountNeqSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        sc.setJoinParameters("network", "domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listUsedByOtherAccounts(long bgpPeerId, Long accountId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = DomainAccountNeqSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        sc.setJoinParameters("network", "accountId", accountId);
        return listBy(sc);
    }

    @Override
    public int removeByNetworkId(long networkId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkSearch.create();
        sc.setParameters("networkId", networkId);

        return remove(sc);
    }
}
