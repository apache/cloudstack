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

package com.cloud.vm.snapshot.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshot.Event;
import com.cloud.vm.snapshot.VMSnapshot.State;
import com.cloud.vm.snapshot.VMSnapshotVO;

@Component
public class VMSnapshotDaoImpl extends GenericDaoBase<VMSnapshotVO, Long> implements VMSnapshotDao {
    private final SearchBuilder<VMSnapshotVO> SnapshotSearch;
    private final SearchBuilder<VMSnapshotVO> ExpungingSnapshotSearch;
    private final SearchBuilder<VMSnapshotVO> SnapshotStatusSearch;
    private final SearchBuilder<VMSnapshotVO> AllFieldsSearch;

    protected VMSnapshotDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.and("vm_id", AllFieldsSearch.entity().getVmId(), Op.EQ);
        AllFieldsSearch.and("deviceId", AllFieldsSearch.entity().getVmId(), Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("removed", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("parent", AllFieldsSearch.entity().getParent(), Op.EQ);
        AllFieldsSearch.and("current", AllFieldsSearch.entity().getCurrent(), Op.EQ);
        AllFieldsSearch.and("vm_snapshot_type", AllFieldsSearch.entity().getType(), Op.EQ);
        AllFieldsSearch.and("updatedCount", AllFieldsSearch.entity().getUpdatedCount(), Op.EQ);
        AllFieldsSearch.and("display_name", AllFieldsSearch.entity().getDisplayName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        SnapshotSearch = createSearchBuilder();
        SnapshotSearch.and("vm_id", SnapshotSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        SnapshotSearch.done();

        ExpungingSnapshotSearch = createSearchBuilder();
        ExpungingSnapshotSearch.and("state", ExpungingSnapshotSearch.entity().getState(), SearchCriteria.Op.EQ);
        ExpungingSnapshotSearch.and("removed", ExpungingSnapshotSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        ExpungingSnapshotSearch.done();

        SnapshotStatusSearch = createSearchBuilder();
        SnapshotStatusSearch.and("vm_id", SnapshotStatusSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        SnapshotStatusSearch.and("state", SnapshotStatusSearch.entity().getState(), SearchCriteria.Op.IN);
        SnapshotStatusSearch.done();
    }

    @Override
    public List<VMSnapshotVO> findByVm(Long vmId) {
        SearchCriteria<VMSnapshotVO> sc = SnapshotSearch.create();
        sc.setParameters("vm_id", vmId);
        return listBy(sc, null);
    }

    @Override
    public List<VMSnapshotVO> listExpungingSnapshot() {
        SearchCriteria<VMSnapshotVO> sc = ExpungingSnapshotSearch.create();
        sc.setParameters("state", State.Expunging);
        return listBy(sc, null);
    }

    @Override
    public List<VMSnapshotVO> listByInstanceId(Long vmId, State... status) {
        SearchCriteria<VMSnapshotVO> sc = SnapshotStatusSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("state", (Object[])status);
        return listBy(sc, null);
    }

    @Override
    public VMSnapshotVO findCurrentSnapshotByVmId(Long vmId) {
        SearchCriteria<VMSnapshotVO> sc = AllFieldsSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("current", 1);
        return findOneBy(sc);
    }

    @Override
    public List<VMSnapshotVO> listByParent(Long vmSnapshotId) {
        SearchCriteria<VMSnapshotVO> sc = AllFieldsSearch.create();
        sc.setParameters("parent", vmSnapshotId);
        sc.setParameters("state", State.Ready);
        return listBy(sc, null);
    }

    @Override
    public VMSnapshotVO findByName(Long vmId, String name) {
        SearchCriteria<VMSnapshotVO> sc = AllFieldsSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("display_name", name);
        return null;
    }

    public List<VMSnapshotVO> listByAccountId(Long accountId) {
        SearchCriteria sc = this.AllFieldsSearch.create();
        sc.setParameters("accountId", new Object[] { accountId });
        return listBy(sc, null);
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, VMSnapshot vo, Object data) {

        Long oldUpdated = vo.getUpdatedCount();
        Date oldUpdatedTime = vo.getUpdated();

        SearchCriteria<VMSnapshotVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", vo.getUpdatedCount());

        vo.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((VMSnapshotVO)vo, sc);
        if (rows == 0 && logger.isDebugEnabled()) {
            VMSnapshotVO dbVol = findByIdIncludingRemoved(vo.getId());
            if (dbVol != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=")
                    .append(dbVol.getId())
                    .append("; state=")
                    .append(dbVol.getState())
                    .append("; updatecount=")
                    .append(dbVol.getUpdatedCount())
                    .append(";updatedTime=")
                    .append(dbVol.getUpdated());
                str.append(": New Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(nextState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(vo.getUpdatedCount())
                    .append("; updatedTime=")
                    .append(vo.getUpdated());
                str.append(": stale Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(currentState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(oldUpdated)
                    .append("; updatedTime=")
                    .append(oldUpdatedTime);
            } else {
                logger.debug("Unable to update VM snapshot: id=" + vo.getId() + ", as there is no such snapshot exists in the database anymore");
            }
        }
        return rows > 0;
    }

    @Override
    public List<VMSnapshotVO> searchByVms(List<Long> vmIds) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return new ArrayList<>();
        }
        SearchBuilder<VMSnapshotVO> sb = createSearchBuilder();
        sb.and("vmIds", sb.entity().getVmId(), SearchCriteria.Op.IN);
        SearchCriteria<VMSnapshotVO> sc = sb.create();
        sc.setParameters("vmIds", vmIds.toArray());
        return search(sc, null);
    }

    @Override
    public List<VMSnapshotVO> searchRemovedByVms(List<Long> vmIds, Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return new ArrayList<>();
        }
        SearchBuilder<VMSnapshotVO> sb = createSearchBuilder();
        sb.and("vmIds", sb.entity().getVmId(), SearchCriteria.Op.IN);
        sb.and("removed", sb.entity().getRemoved(), SearchCriteria.Op.NNULL);
        SearchCriteria<VMSnapshotVO> sc = sb.create();
        sc.setParameters("vmIds", vmIds.toArray());
        Filter filter = new Filter(VMSnapshotVO.class, "id", true, 0L, batchSize);
        return searchIncludingRemoved(sc, filter, null, false);
    }
}
