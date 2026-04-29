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
package com.cloud.cluster.agentlb.dao;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.cloud.cluster.agentlb.HostTransferMapVO;
import com.cloud.cluster.agentlb.HostTransferMapVO.HostTransferState;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class HostTransferMapDaoImpl extends GenericDaoBase<HostTransferMapVO, Long> implements HostTransferMapDao {

    protected SearchBuilder<HostTransferMapVO> AllFieldsSearch;
    protected SearchBuilder<HostTransferMapVO> IntermediateStateSearch;
    protected SearchBuilder<HostTransferMapVO> ActiveSearch;

    public HostTransferMapDaoImpl() {
        super();
    }

    @PostConstruct
    public void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("initialOwner", AllFieldsSearch.entity().getInitialOwner(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("futureOwner", AllFieldsSearch.entity().getFutureOwner(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        IntermediateStateSearch = createSearchBuilder();
        IntermediateStateSearch.and("futureOwner", IntermediateStateSearch.entity().getFutureOwner(), SearchCriteria.Op.EQ);
        IntermediateStateSearch.and("initialOwner", IntermediateStateSearch.entity().getInitialOwner(), SearchCriteria.Op.EQ);
        IntermediateStateSearch.and("state", IntermediateStateSearch.entity().getState(), SearchCriteria.Op.IN);
        IntermediateStateSearch.done();

        ActiveSearch = createSearchBuilder();
        ActiveSearch.and("created", ActiveSearch.entity().getCreated(), SearchCriteria.Op.GT);
        ActiveSearch.and("id", ActiveSearch.entity().getId(), SearchCriteria.Op.EQ);
        ActiveSearch.and("state", ActiveSearch.entity().getState(), SearchCriteria.Op.EQ);
        ActiveSearch.done();

    }

    @Override
    public List<HostTransferMapVO> listHostsLeavingCluster(long currentOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = IntermediateStateSearch.create();
        sc.setParameters("initialOwner", currentOwnerId);

        return listBy(sc);
    }

    @Override
    public List<HostTransferMapVO> listHostsJoiningCluster(long futureOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = IntermediateStateSearch.create();
        sc.setParameters("futureOwner", futureOwnerId);

        return listBy(sc);
    }

    @Override
    public HostTransferMapVO startAgentTransfering(long hostId, long initialOwner, long futureOwner) {
        HostTransferMapVO transfer = new HostTransferMapVO(hostId, initialOwner, futureOwner);
        return persist(transfer);
    }

    @Override
    public boolean completeAgentTransfer(long hostId) {
        return remove(hostId);
    }

    @Override
    public List<HostTransferMapVO> listBy(long futureOwnerId, HostTransferState state) {
        SearchCriteria<HostTransferMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("futureOwner", futureOwnerId);
        sc.setParameters("state", state);

        return listBy(sc);
    }

    @Override
    public HostTransferMapVO findActiveHostTransferMapByHostId(long hostId, Date cutTime) {
        SearchCriteria<HostTransferMapVO> sc = ActiveSearch.create();
        sc.setParameters("id", hostId);
        sc.setParameters("state", HostTransferState.TransferRequested);
        sc.setParameters("created", cutTime);

        return findOneBy(sc);

    }

    @Override
    public boolean startAgentTransfer(long hostId) {
        HostTransferMapVO transfer = findById(hostId);
        transfer.setState(HostTransferState.TransferStarted);
        return update(hostId, transfer);
    }

    @Override
    public HostTransferMapVO findByIdAndFutureOwnerId(long id, long futureOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("futureOwner", futureOwnerId);
        sc.setParameters("id", id);

        return findOneBy(sc);
    }

    @Override
    public HostTransferMapVO findByIdAndCurrentOwnerId(long id, long currentOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("initialOwner", currentOwnerId);
        sc.setParameters("id", id);

        return findOneBy(sc);
    }

}
