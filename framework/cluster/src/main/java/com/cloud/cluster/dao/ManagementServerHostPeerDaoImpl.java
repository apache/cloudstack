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
package com.cloud.cluster.dao;

import java.util.Date;
import java.util.List;


import org.apache.cloudstack.management.ManagementServerHost;
import com.cloud.cluster.ManagementServerHostPeerVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

public class ManagementServerHostPeerDaoImpl extends GenericDaoBase<ManagementServerHostPeerVO, Long> implements ManagementServerHostPeerDao {

    private final SearchBuilder<ManagementServerHostPeerVO> ClearPeerSearch;
    private final SearchBuilder<ManagementServerHostPeerVO> FindForUpdateSearch;
    private final SearchBuilder<ManagementServerHostPeerVO> CountSearch;
    private final SearchBuilder<ManagementServerHostPeerVO> ActiveSearch;
    private final SearchBuilder<ManagementServerHostPeerVO> FindByOwnerAndPeerMsSearch;
    private final SearchBuilder<ManagementServerHostPeerVO> FindByPeerMsAndStateSearch;


    public ManagementServerHostPeerDaoImpl() {
        ClearPeerSearch = createSearchBuilder();
        ClearPeerSearch.and("ownerMshost", ClearPeerSearch.entity().getOwnerMshost(), SearchCriteria.Op.EQ);
        ClearPeerSearch.or("peerMshost", ClearPeerSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        ClearPeerSearch.done();

        FindForUpdateSearch = createSearchBuilder();
        FindForUpdateSearch.and("ownerMshost", FindForUpdateSearch.entity().getOwnerMshost(), SearchCriteria.Op.EQ);
        FindForUpdateSearch.and("peerMshost", FindForUpdateSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        FindForUpdateSearch.and("peerRunid", FindForUpdateSearch.entity().getPeerRunid(), SearchCriteria.Op.EQ);
        FindForUpdateSearch.done();

        CountSearch = createSearchBuilder();
        CountSearch.and("peerMshost", CountSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        CountSearch.and("peerRunid", CountSearch.entity().getPeerRunid(), SearchCriteria.Op.EQ);
        CountSearch.and("peerState", CountSearch.entity().getPeerState(), SearchCriteria.Op.EQ);
        CountSearch.done();

        ActiveSearch = createSearchBuilder();
        ActiveSearch.and("ownerMshost", ActiveSearch.entity().getOwnerMshost(), SearchCriteria.Op.EQ);
        ActiveSearch.and("peerMshost", ActiveSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        ActiveSearch.and("peerState", ActiveSearch.entity().getPeerState(), SearchCriteria.Op.EQ);
        ActiveSearch.and("lastUpdateTime", ActiveSearch.entity().getLastUpdateTime(), SearchCriteria.Op.GT);
        ActiveSearch.done();

        FindByOwnerAndPeerMsSearch = createSearchBuilder();
        FindByOwnerAndPeerMsSearch.and("ownerMshost", FindByOwnerAndPeerMsSearch.entity().getOwnerMshost(), SearchCriteria.Op.EQ);
        FindByOwnerAndPeerMsSearch.and("peerMshost", FindByOwnerAndPeerMsSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        FindByOwnerAndPeerMsSearch.and("peerState", FindByOwnerAndPeerMsSearch.entity().getPeerState(), SearchCriteria.Op.EQ);
        FindByOwnerAndPeerMsSearch.done();

        FindByPeerMsAndStateSearch = createSearchBuilder();
        FindByPeerMsAndStateSearch.and("peerMshost", FindByPeerMsAndStateSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        FindByPeerMsAndStateSearch.and("peerState", FindByPeerMsAndStateSearch.entity().getPeerState(), SearchCriteria.Op.EQ);
        FindByPeerMsAndStateSearch.done();
    }

    @Override
    @DB
    public void clearPeerInfo(long ownerMshost) {
        SearchCriteria<ManagementServerHostPeerVO> sc = ClearPeerSearch.create();
        sc.setParameters("ownerMshost", ownerMshost);
        sc.setParameters("peerMshost", ownerMshost);

        expunge(sc);
    }

    @Override
    @DB
    public void updatePeerInfo(long ownerMshost, long peerMshost, long peerRunid, ManagementServerHost.State peerState) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();

            SearchCriteria<ManagementServerHostPeerVO> sc = FindForUpdateSearch.create();
            sc.setParameters("ownerMshost", ownerMshost);
            sc.setParameters("peerMshost", peerMshost);
            List<ManagementServerHostPeerVO> l = listBy(sc);
            if (l.size() == 1) {
                ManagementServerHostPeerVO peer = l.get(0);
                peer.setPeerRunid(peerRunid);
                peer.setPeerState(peerState);
                peer.setLastUpdateTime(new Date());
                update(peer.getId(), peer);
            } else {
                ManagementServerHostPeerVO peer = new ManagementServerHostPeerVO(ownerMshost, peerMshost, peerRunid, peerState);
                persist(peer);
            }
            txn.commit();
        } catch (Exception e) {
            logger.warn("Unexpected exception, ", e);
            txn.rollback();
        }
    }

    @Override
    @DB
    public int countStateSeenInPeers(long peerMshost, long runid, ManagementServerHost.State state) {
        SearchCriteria<ManagementServerHostPeerVO> sc = CountSearch.create();
        sc.setParameters("peerMshost", peerMshost);
        sc.setParameters("peerRunid", runid);
        sc.setParameters("peerState", state);

        return getCount(sc);
    }

    @Override
    @DB
    public boolean isPeerUpState(long peerMshost, Date cutTime) {
        SearchCriteria<ManagementServerHostPeerVO> sc = ActiveSearch.create();
        sc.setParameters("peerMshost", peerMshost);
        sc.setParameters("peerState", ManagementServerHost.State.Up);
        sc.setParameters("lastUpdateTime", cutTime);

        return listBy(sc).size() > 0;
    }

    @Override
    @DB
    public boolean isPeerUpState(long ownerMshost, long peerMshost, Date cutTime) {
        SearchCriteria<ManagementServerHostPeerVO> sc = ActiveSearch.create();
        sc.setParameters("ownerMshost", ownerMshost);
        sc.setParameters("peerMshost", peerMshost);
        sc.setParameters("peerState", ManagementServerHost.State.Up);
        sc.setParameters("lastUpdateTime", cutTime);

        return listBy(sc).size() > 0;
    }

    @Override
    public ManagementServerHostPeerVO findByOwnerAndPeerMsHost(long ownerMshost, long peerMshost, ManagementServerHost.State peerState) {
        SearchCriteria<ManagementServerHostPeerVO> sc = FindByOwnerAndPeerMsSearch.create();
        sc.setParameters("ownerMshost", ownerMshost);
        sc.setParameters("peerMshost", peerMshost);
        sc.setParameters("peerState", peerState);

        return findOneBy(sc);
    }

    @Override
    public ManagementServerHostPeerVO findByPeerMsAndState(long peerMshost, ManagementServerHost.State peerState) {
        SearchCriteria<ManagementServerHostPeerVO> sc = FindByPeerMsAndStateSearch.create();
        sc.setParameters("peerMshost", peerMshost);
        sc.setParameters("peerState", peerState);

        return findOneBy(sc);
    }
}
