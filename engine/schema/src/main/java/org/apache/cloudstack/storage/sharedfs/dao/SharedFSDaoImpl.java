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
package org.apache.cloudstack.storage.sharedfs.dao;

import com.cloud.network.dao.NetworkDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;

import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMNetworkMapDao;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.apache.cloudstack.storage.sharedfs.SharedFSVO;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class SharedFSDaoImpl extends GenericDaoBase<SharedFSVO, Long> implements SharedFSDao {

    @Inject
    VMNetworkMapDao vmNetworkMapDao;

    @Inject
    NetworkDao networkDao;

    protected final SearchBuilder<SharedFSVO> StateUpdateCountSearch;

    protected final SearchBuilder<SharedFSVO> DestroyedByTimeSearch;

    protected final SearchBuilder<SharedFSVO> NameAccountDomainSearch;

    public SharedFSDaoImpl() {
        StateUpdateCountSearch = createSearchBuilder();
        StateUpdateCountSearch.and("id", StateUpdateCountSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateUpdateCountSearch.and("state", StateUpdateCountSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateUpdateCountSearch.and("updatedCount", StateUpdateCountSearch.entity().getUpdatedCount(), SearchCriteria.Op.EQ);
        StateUpdateCountSearch.done();

        DestroyedByTimeSearch = createSearchBuilder();
        DestroyedByTimeSearch.and("state", DestroyedByTimeSearch.entity().getState(), SearchCriteria.Op.IN);
        DestroyedByTimeSearch.and("accountId", DestroyedByTimeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        DestroyedByTimeSearch.done();

        NameAccountDomainSearch = createSearchBuilder();
        NameAccountDomainSearch.and("name", NameAccountDomainSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameAccountDomainSearch.and("accountId", NameAccountDomainSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        NameAccountDomainSearch.and("domainId", NameAccountDomainSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        NameAccountDomainSearch.done();
    }

    @Override
    public boolean updateState(SharedFS.State currentState, SharedFS.Event event, SharedFS.State nextState, SharedFS vo, Object data) {

        Long oldUpdated = vo.getUpdatedCount();
        Date oldUpdatedTime = vo.getUpdated();

        SearchCriteria<SharedFSVO> sc = StateUpdateCountSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", vo.getUpdatedCount());

        vo.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((SharedFSVO) vo, sc);
        if (rows == 0 && logger.isDebugEnabled()) {
            SharedFSVO dbSharedFS = findByIdIncludingRemoved(vo.getId());
            if (dbSharedFS != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(dbSharedFS.getId()).append("; state=").append(dbSharedFS.getState()).append("; updatecount=").append(dbSharedFS.getUpdatedCount()).append(";updatedTime=")
                        .append(dbSharedFS.getUpdated());
                str.append(": New Data={id=").append(vo.getId()).append("; state=").append(nextState).append("; event=").append(event).append("; updatecount=").append(vo.getUpdatedCount())
                        .append("; updatedTime=").append(vo.getUpdated());
                str.append(": stale Data={id=").append(vo.getId()).append("; state=").append(currentState).append("; event=").append(event).append("; updatecount=").append(oldUpdated)
                        .append("; updatedTime=").append(oldUpdatedTime);
            } else {
                logger.debug("Unable to update sharedfs: id=" + vo.getId() + ", as it is not present in the database anymore");
            }
        }
        return rows > 0;
    }

    @Override
    public List<SharedFSVO> listSharedFSToBeDestroyed(Date date) {
        SearchCriteria<SharedFSVO> sc = DestroyedByTimeSearch.create();
        sc.setParameters("state", SharedFS.State.Destroyed, SharedFS.State.Expunging, SharedFS.State.Error);
        sc.setParameters("updateTime", date);
        return listBy(sc);
    }

    @Override
    public SharedFSVO findSharedFSByNameAccountDomain(String name, Long accountId, Long domainId) {
        SearchCriteria<SharedFSVO> sc = NameAccountDomainSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("accountId", accountId);
        sc.setParameters("domainId", domainId);
        return findOneBy(sc);
    }
}
