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
package org.apache.cloudstack.storage.fileshare.dao;

import com.cloud.network.dao.NetworkDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;

import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMNetworkMapDao;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareVO;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class FileShareDaoImpl extends GenericDaoBase<FileShareVO, Long> implements FileShareDao {

    @Inject
    VMNetworkMapDao vmNetworkMapDao;

    @Inject
    NetworkDao networkDao;

    protected final SearchBuilder<FileShareVO> StateUpdateCountSearch;

    protected final SearchBuilder<FileShareVO> DestroyedByTimeSearch;

    public FileShareDaoImpl() {
        StateUpdateCountSearch = createSearchBuilder();
        StateUpdateCountSearch.and("id", StateUpdateCountSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateUpdateCountSearch.and("state", StateUpdateCountSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateUpdateCountSearch.and("updatedCount", StateUpdateCountSearch.entity().getUpdatedCount(), SearchCriteria.Op.EQ);
        StateUpdateCountSearch.done();

        DestroyedByTimeSearch = createSearchBuilder();
        DestroyedByTimeSearch.and("state", DestroyedByTimeSearch.entity().getState(), SearchCriteria.Op.EQ);
        DestroyedByTimeSearch.and("accountId", DestroyedByTimeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        DestroyedByTimeSearch.done();
    }

    @Override
    public boolean updateState(FileShare.State currentState, FileShare.Event event, FileShare.State nextState, FileShare vo, Object data) {

        Long oldUpdated = vo.getUpdatedCount();
        Date oldUpdatedTime = vo.getUpdated();

        SearchCriteria<FileShareVO> sc = StateUpdateCountSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", vo.getUpdatedCount());

        vo.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((FileShareVO) vo, sc);
        if (rows == 0 && logger.isDebugEnabled()) {
            FileShareVO dbFileShare = findByIdIncludingRemoved(vo.getId());
            if (dbFileShare != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(dbFileShare.getId()).append("; state=").append(dbFileShare.getState()).append("; updatecount=").append(dbFileShare.getUpdatedCount()).append(";updatedTime=")
                        .append(dbFileShare.getUpdated());
                str.append(": New Data={id=").append(vo.getId()).append("; state=").append(nextState).append("; event=").append(event).append("; updatecount=").append(vo.getUpdatedCount())
                        .append("; updatedTime=").append(vo.getUpdated());
                str.append(": stale Data={id=").append(vo.getId()).append("; state=").append(currentState).append("; event=").append(event).append("; updatecount=").append(oldUpdated)
                        .append("; updatedTime=").append(oldUpdatedTime);
            } else {
                logger.debug("Unable to update fileshare: id=" + vo.getId() + ", as it is not present in the database anymore");
            }
        }
        return rows > 0;
    }

    @Override
    public List<FileShareVO> listFileSharesToBeDestroyed(Date date) {
        SearchCriteria<FileShareVO> sc = DestroyedByTimeSearch.create();
        sc.setParameters("state", FileShare.State.Destroyed);
        sc.setParameters("updateTime", date);
        return listBy(sc);
    }
}
