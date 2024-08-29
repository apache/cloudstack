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

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.network.BgpPeer;
import org.apache.cloudstack.network.BgpPeerDetailsVO;
import org.apache.cloudstack.network.BgpPeerNetworkMapVO;
import org.apache.cloudstack.network.BgpPeerVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@DB
public class BgpPeerDaoImpl extends GenericDaoBase<BgpPeerVO, Long> implements BgpPeerDao {
    protected SearchBuilder<BgpPeerVO> NetworkIdSearch;
    protected SearchBuilder<BgpPeerVO> VpcIdSearch;
    protected SearchBuilder<BgpPeerVO> AllFieldsSearch;

    private static final String LIST_ALL_BGP_PEERS_IDS_FOR_ACCOUNT = "SELECT id FROM `cloud`.`bgp_peers` WHERE removed IS NULL AND data_center_id = ? " +
            "AND ((domain_id IS NULL AND account_id IS NULL) " +
            "OR (domain_id = ? AND account_id IS NULL) " +
            "OR (domain_id = ? AND account_id = ?))";

    private static final String LIST_DEDICATED_BGP_PEERS_IDS_FOR_ACCOUNT = "SELECT id FROM `cloud`.`bgp_peers` WHERE removed IS NULL AND data_center_id = ? " +
            "AND ((domain_id = ? AND account_id IS NULL) " +
            "OR (domain_id = ? AND account_id = ?))";

    @Inject
    BgpPeerNetworkMapDao bgpPeerNetworkMapDao;
    @Inject
    BgpPeerDetailsDao bgpPeerDetailsDao;

    @PostConstruct
    public void init() {
        final SearchBuilder<BgpPeerNetworkMapVO> networkSearchBuilder = bgpPeerNetworkMapDao.createSearchBuilder();
        networkSearchBuilder.and("networkId", networkSearchBuilder.entity().getNetworkId(), SearchCriteria.Op.EQ);
        networkSearchBuilder.and("state", networkSearchBuilder.entity().getState(), SearchCriteria.Op.IN);
        networkSearchBuilder.and("removed", networkSearchBuilder.entity().getRemoved(), SearchCriteria.Op.NULL);
        NetworkIdSearch = createSearchBuilder();
        NetworkIdSearch.join("network", networkSearchBuilder, networkSearchBuilder.entity().getBgpPeerId(),
                NetworkIdSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        NetworkIdSearch.done();

        final SearchBuilder<BgpPeerNetworkMapVO> vpcSearchBuilder = bgpPeerNetworkMapDao.createSearchBuilder();
        vpcSearchBuilder.and("vpcId", vpcSearchBuilder.entity().getVpcId(), SearchCriteria.Op.EQ);
        vpcSearchBuilder.and("state", vpcSearchBuilder.entity().getState(), SearchCriteria.Op.IN);
        vpcSearchBuilder.and("removed", vpcSearchBuilder.entity().getRemoved(), SearchCriteria.Op.NULL);
        VpcIdSearch = createSearchBuilder();
        VpcIdSearch.join("vpc", vpcSearchBuilder, vpcSearchBuilder.entity().getBgpPeerId(),
                VpcIdSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        VpcIdSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("zoneId", AllFieldsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("domainId", AllFieldsSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("asNumber", AllFieldsSearch.entity().getAsNumber(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ip4Address", AllFieldsSearch.entity().getIp4Address(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ip6Address", AllFieldsSearch.entity().getIp6Address(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public List<BgpPeerVO> listNonRevokeByNetworkId(long networkId) {
        SearchCriteria<BgpPeerVO> sc = NetworkIdSearch.create();
        sc.setJoinParameters("network", "networkId", networkId);
        sc.setJoinParameters("network", "state", BgpPeer.State.Active, BgpPeer.State.Add);
        return listBy(sc);
    }

    @Override
    public List<BgpPeerVO> listNonRevokeByVpcId(long vpcId) {
        SearchCriteria<BgpPeerVO> sc = VpcIdSearch.create();
        sc.setJoinParameters("vpc", "vpcId", vpcId);
        sc.setJoinParameters("vpc", "state", BgpPeer.State.Active, BgpPeer.State.Add);
        return listBy(sc);
    }

    @Override
    public BgpPeerVO findByZoneAndAsNumberAndAddress(long zoneId, Long asNumber, String ip4Address, String ip6Address) {
        SearchCriteria<BgpPeerVO> sc = AllFieldsSearch.create();
        sc.setParameters( "zoneId", zoneId);
        sc.setParameters( "asNumber", asNumber);
        if (ip4Address != null) {
            sc.setParameters( "ip4Address", ip4Address);
        }
        if (ip6Address != null) {
            sc.setParameters( "ip6Address", ip6Address);
        }
        return findOneBy(sc);
    }

    @Override
    public BgpPeerVO persist(BgpPeerVO bgpPeerVO, Map<BgpPeer.Detail, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        BgpPeerVO vo = super.persist(bgpPeerVO);

        // persist the details
        if (details != null && !details.isEmpty()) {
            for (BgpPeer.Detail detail : details.keySet()) {
                bgpPeerDetailsDao.persist(new BgpPeerDetailsVO(bgpPeerVO.getId(), detail, details.get(detail), true));
            }
        }

        txn.commit();
        return vo;
    }

    @Override
    public List<Long> listAvailableBgpPeerIdsForAccount(long zoneId, long domainId, long accountId, boolean useSystemBgpPeers) {
        if (useSystemBgpPeers) {
            return listBgpPeerIdsForAccount(zoneId, domainId, accountId, false);
        } else {
            List<Long> dedicatedBgpPeerIds = listBgpPeerIdsForAccount(zoneId, domainId, accountId, true);
            if (CollectionUtils.isNotEmpty(dedicatedBgpPeerIds)) {
                return dedicatedBgpPeerIds;
            }
            return listBgpPeerIdsForAccount(zoneId, domainId, accountId, false);
        }
    }

    private List<Long> listBgpPeerIdsForAccount(long zoneId, long domainId, long accountId, boolean isDedicated) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        StringBuilder sql = isDedicated ? new StringBuilder(LIST_DEDICATED_BGP_PEERS_IDS_FOR_ACCOUNT): new StringBuilder(LIST_ALL_BGP_PEERS_IDS_FOR_ACCOUNT);

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, zoneId);
            pstmt.setLong(2, domainId);
            pstmt.setLong(3, domainId);
            pstmt.setLong(4, accountId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public int removeByAccountId(long accountId) {
        SearchCriteria<BgpPeerVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        return remove(sc);
    }

    @Override
    public int removeByDomainId(long domainId) {
        SearchCriteria<BgpPeerVO> sc = createSearchCriteria();
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        return remove(sc);
    }
}
