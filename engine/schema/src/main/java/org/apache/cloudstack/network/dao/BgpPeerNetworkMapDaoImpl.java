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
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
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

    protected SearchBuilder<BgpPeerNetworkMapVO> BgpPeerNetworkVpcSearch;
    protected SearchBuilder<BgpPeerNetworkMapVO> NetworkDomainAccountNeqSearch;
    protected SearchBuilder<BgpPeerNetworkMapVO> VpcDomainAccountNeqSearch;

    @Inject
    NetworkDao networkDao;
    @Inject
    VpcDao vpcDao;

    public BgpPeerNetworkMapDaoImpl() {
    }

    @PostConstruct
    public void init() {
        BgpPeerNetworkVpcSearch = createSearchBuilder();
        BgpPeerNetworkVpcSearch.and("bgpPeerId", BgpPeerNetworkVpcSearch.entity().getBgpPeerId(), SearchCriteria.Op.EQ);
        BgpPeerNetworkVpcSearch.and("networkId", BgpPeerNetworkVpcSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        BgpPeerNetworkVpcSearch.and("vpcId", BgpPeerNetworkVpcSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        BgpPeerNetworkVpcSearch.done();

        final SearchBuilder<NetworkVO> networkSearchBuilder = networkDao.createSearchBuilder();
        networkSearchBuilder.and("domainId", networkSearchBuilder.entity().getDomainId(), SearchCriteria.Op.NEQ);
        networkSearchBuilder.and("accountId", networkSearchBuilder.entity().getAccountId(), SearchCriteria.Op.NEQ);
        NetworkDomainAccountNeqSearch = createSearchBuilder();
        NetworkDomainAccountNeqSearch.and("bgpPeerId", NetworkDomainAccountNeqSearch.entity().getBgpPeerId(), SearchCriteria.Op.EQ);
        NetworkDomainAccountNeqSearch.join("network", networkSearchBuilder, networkSearchBuilder.entity().getId(),
                NetworkDomainAccountNeqSearch.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        NetworkDomainAccountNeqSearch.done();

        final SearchBuilder<VpcVO> vpcSearchBuilder = vpcDao.createSearchBuilder();
        vpcSearchBuilder.and("domainId", vpcSearchBuilder.entity().getDomainId(), SearchCriteria.Op.NEQ);
        vpcSearchBuilder.and("accountId", vpcSearchBuilder.entity().getAccountId(), SearchCriteria.Op.NEQ);
        VpcDomainAccountNeqSearch = createSearchBuilder();
        VpcDomainAccountNeqSearch.and("bgpPeerId", VpcDomainAccountNeqSearch.entity().getBgpPeerId(), SearchCriteria.Op.EQ);
        VpcDomainAccountNeqSearch.join("vpc", vpcSearchBuilder, vpcSearchBuilder.entity().getId(),
                VpcDomainAccountNeqSearch.entity().getVpcId(), JoinBuilder.JoinType.INNER);
        VpcDomainAccountNeqSearch.done();
    }

    @Override
    public void persistForNetwork(long networkId, List<Long> bgpPeerIds) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkVpcSearch.create();
        sc.setParameters("networkId", networkId);
        expunge(sc);

        for (Long bgpPeerId : bgpPeerIds) {
            BgpPeerNetworkMapVO vo = new BgpPeerNetworkMapVO(bgpPeerId, networkId, null, BgpPeer.State.Active);
            persist(vo);
        }

        txn.commit();
    }

    @Override
    public List<BgpPeerNetworkMapVO> listByBgpPeerId(long bgpPeerId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkVpcSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);

        return search(sc, null);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listByNetworkId(long networkId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkVpcSearch.create();
        sc.setParameters("networkId", networkId);

        return search(sc, null);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listUsedNetworksByOtherDomains(long bgpPeerId, Long domainId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = NetworkDomainAccountNeqSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        sc.setJoinParameters("network", "domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listUsedNetworksByOtherAccounts(long bgpPeerId, Long accountId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = NetworkDomainAccountNeqSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        sc.setJoinParameters("network", "accountId", accountId);
        return listBy(sc);
    }

    @Override
    public int removeByNetworkId(long networkId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkVpcSearch.create();
        sc.setParameters("networkId", networkId);

        return remove(sc);
    }

    @Override
    public void persistForVpc(long vpcId, List<Long> bgpPeerIds) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkVpcSearch.create();
        sc.setParameters("vpcId", vpcId);
        expunge(sc);

        for (Long bgpPeerId : bgpPeerIds) {
            BgpPeerNetworkMapVO vo = new BgpPeerNetworkMapVO(bgpPeerId, null, vpcId, BgpPeer.State.Active);
            persist(vo);
        }

        txn.commit();
    }

    @Override
    public List<BgpPeerNetworkMapVO> listByVpcId(long vpcId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkVpcSearch.create();
        sc.setParameters("vpcId", vpcId);

        return search(sc, null);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listUsedVpcsByOtherDomains(long bgpPeerId, Long domainId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = VpcDomainAccountNeqSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        sc.setJoinParameters("vpc", "domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<BgpPeerNetworkMapVO> listUsedVpcsByOtherAccounts(long bgpPeerId, Long accountId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = VpcDomainAccountNeqSearch.create();
        sc.setParameters("bgpPeerId", bgpPeerId);
        sc.setJoinParameters("vpc", "accountId", accountId);
        return listBy(sc);
    }

    @Override
    public int removeByVpcId(long vpcId) {
        SearchCriteria<BgpPeerNetworkMapVO> sc = BgpPeerNetworkVpcSearch.create();
        sc.setParameters("vpcId", vpcId);

        return remove(sc);
    }
}
