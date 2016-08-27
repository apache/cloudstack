/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.storage.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.storage.VMSnapshotTemplateStoragePoolVO;
import com.cloud.storage.VMSnapshotTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;

@Component
@Local(value = {VMSnapshotTemplatePoolDao.class})
public class VMSnapshotTemplatePoolDaoImpl extends GenericDaoBase<VMSnapshotTemplateStoragePoolVO, Long> implements VMSnapshotTemplatePoolDao {

    public static final Logger s_logger = Logger.getLogger(VMSnapshotTemplatePoolDaoImpl.class.getName());

    protected final SearchBuilder<VMSnapshotTemplateStoragePoolVO> poolSearch;
    protected final SearchBuilder<VMSnapshotTemplateStoragePoolVO> vmSnapshotSearch;
    protected final SearchBuilder<VMSnapshotTemplateStoragePoolVO> poolVmSnapshotSearch;
    protected final SearchBuilder<VMSnapshotTemplateStoragePoolVO> updateStateSearch;

    public VMSnapshotTemplatePoolDaoImpl() {
        poolSearch = createSearchBuilder();
        poolSearch.and("pool_id", poolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        poolSearch.done();

        vmSnapshotSearch = createSearchBuilder();
        vmSnapshotSearch.and("vm_snapshot_id", vmSnapshotSearch.entity().getVmSnapshotId(), SearchCriteria.Op.EQ);
        vmSnapshotSearch.done();

        poolVmSnapshotSearch = createSearchBuilder();
        poolVmSnapshotSearch.and("pool_id", poolVmSnapshotSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        poolVmSnapshotSearch.and("vm_snapshot_id", poolVmSnapshotSearch.entity().getVmSnapshotId(), SearchCriteria.Op.EQ);
        poolVmSnapshotSearch.done();

        updateStateSearch = this.createSearchBuilder();
        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        //updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        updateStateSearch.done();
    }

    @Override
    public List<VMSnapshotTemplateStoragePoolVO> listByPoolId(long id) {
        SearchCriteria<VMSnapshotTemplateStoragePoolVO> sc = poolSearch.create();
        sc.setParameters("pool_id", id);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public boolean isVmSnapshottemplateAvailableInPool(long vmSnapshotId, long poolId) {
        VMSnapshotTemplateStoragePoolVO vmSnapshotTemplateOnPool = findByPoolVmSnapshot(poolId, vmSnapshotId);
        if (vmSnapshotTemplateOnPool == null)
            return false;

        return vmSnapshotTemplateOnPool.getStatus() == Status.CREATED;
    }

    @Override
    public List<VMSnapshotTemplateStoragePoolVO> listByVmSnapshotId(long vmSnapshotId) {
        SearchCriteria<VMSnapshotTemplateStoragePoolVO> sc = vmSnapshotSearch.create();
        sc.setParameters("vm_snapshot_id", vmSnapshotId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public VMSnapshotTemplateStoragePoolVO findByPoolVmSnapshot(long poolId, long vmSnapshotId) {
        SearchCriteria<VMSnapshotTemplateStoragePoolVO> sc = poolVmSnapshotSearch.create();
        sc.setParameters("pool_id", poolId);
        sc.setParameters("vm_snapshot_id", vmSnapshotId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataObjectInStore vo, Object data) {
        VMSnapshotTemplateStoragePoolVO vmSnapTemplatePool = (VMSnapshotTemplateStoragePoolVO)vo;
        //Long oldUpdated = vmSnapTemplatePool.getUpdatedCount();
        //Date oldUpdatedTime = vmSnapTemplatePool.getUpdated();

        SearchCriteria<VMSnapshotTemplateStoragePoolVO> sc = updateStateSearch.create();
        sc.setParameters("id", vmSnapTemplatePool.getId());
        sc.setParameters("state", currentState);
        //sc.setParameters("updatedCount", templatePool.getUpdatedCount());

        //vmSnapTemplatePool.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        //builder.set(vo, "updated", new Date());

        int rows = update((VMSnapshotTemplateStoragePoolVO)vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VMSnapshotTemplateStoragePoolVO dbVol = findByIdIncludingRemoved(vmSnapTemplatePool.getId());
            if (dbVol != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=")
                        .append(dbVol.getId())
                        .append("; state=")
                        .append(dbVol.getState());/*
                                                  .append("; updatecount=")
                                                  .append(dbVol.getUpdatedCount())
                                                  .append(";updatedTime=")
                                                  .append(dbVol.getUpdated());*/
                str.append(": New Data={id=")
                        .append(vmSnapTemplatePool.getId())
                        .append("; state=")
                        .append(nextState)
                        .append("; event=")
                        .append(event);/*
                                       .append("; updatecount=")
                                       .append(templatePool.getUpdatedCount())
                                       .append("; updatedTime=")
                                       .append(templatePool.getUpdated());*/
                str.append(": stale Data={id=")
                        .append(vmSnapTemplatePool.getId())
                        .append("; state=")
                        .append(currentState)
                        .append("; event=")
                        .append(event);/*
                                       .append("; updatecount=")
                                       .append(oldUpdated)
                                       .append("; updatedTime=")
                                       .append(oldUpdatedTime);*/
            } else {
                s_logger.debug("Unable to update objectIndatastore: id=" + vmSnapTemplatePool.getId() + ", as there is no such object exists in the database anymore");
            }
        }
        return rows > 0;
    }
}
