//
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
//

package com.cloud.vm.dao;

import java.util.Date;
import java.util.List;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.ConsoleSessionVO;

public class ConsoleSessionDaoImpl extends GenericDaoBase<ConsoleSessionVO, Long> implements ConsoleSessionDao {

    private final SearchBuilder<ConsoleSessionVO> searchByRemovedDate;

    public ConsoleSessionDaoImpl() {
        searchByRemovedDate = createSearchBuilder();
        searchByRemovedDate.and("removedNotNull", searchByRemovedDate.entity().getRemoved(), SearchCriteria.Op.NNULL);
        searchByRemovedDate.and("removed", searchByRemovedDate.entity().getRemoved(), SearchCriteria.Op.LTEQ);
    }

    @Override
    public void removeSession(String sessionUuid) {
        ConsoleSessionVO session = findByUuid(sessionUuid);
        remove(session.getId());
    }

    @Override
    public boolean isSessionAllowed(String sessionUuid) {
        ConsoleSessionVO consoleSessionVO = findByUuid(sessionUuid);
        if (consoleSessionVO == null) {
            return false;
        }
        return consoleSessionVO.getAcquired() == null;
    }

    @Override
    public int expungeSessionsOlderThanDate(Date date) {
        SearchCriteria<ConsoleSessionVO> searchCriteria = searchByRemovedDate.create();
        searchCriteria.setParameters("removed", date);
        return expunge(searchCriteria);
    }

    @Override
    public void acquireSession(String sessionUuid, String clientAddress) {
        ConsoleSessionVO consoleSessionVO = findByUuid(sessionUuid);
        consoleSessionVO.setAcquired(new Date());
        consoleSessionVO.setClientAddress(clientAddress);
        update(consoleSessionVO.getId(), consoleSessionVO);
    }

    @Override
    public int expungeByVmList(List<Long> vmIds, Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return 0;
        }
        SearchBuilder<ConsoleSessionVO> sb = createSearchBuilder();
        sb.and("vmIds", sb.entity().getInstanceId(), SearchCriteria.Op.IN);
        SearchCriteria<ConsoleSessionVO> sc = sb.create();
        sc.setParameters("vmIds", vmIds.toArray());
        return batchExpunge(sc, batchSize);
    }

    @Override
    public Pair<List<ConsoleSessionVO>, Integer> listConsoleSessions(Long id, List<Long> domainIds, Long accountId, Long userId, Long hostId,
                                                                     Date startDate, Date endDate, Long instanceId,
                                                                     String consoleEndpointCreatorAddress, String clientAddress,
                                                                     boolean activeOnly, Long pageSizeVal, Long startIndex) {
        Filter filter = new Filter(ConsoleSessionVO.class, "created", false, startIndex, pageSizeVal);
        SearchCriteria<ConsoleSessionVO> searchCriteria = createListConsoleSessionsSearchCriteria(id, domainIds, accountId, userId, hostId,
                startDate, endDate, instanceId, consoleEndpointCreatorAddress, clientAddress, activeOnly);

        return searchAndCount(searchCriteria, filter, true);
    }

    private SearchCriteria<ConsoleSessionVO> createListConsoleSessionsSearchCriteria(Long id, List<Long> domainIds, Long accountId, Long userId, Long hostId,
                                                                                     Date startDate, Date endDate, Long instanceId,
                                                                                     String consoleEndpointCreatorAddress, String clientAddress,
                                                                                     boolean activeOnly) {
        SearchCriteria<ConsoleSessionVO> searchCriteria = createListConsoleSessionsSearchBuilder(activeOnly).create();

        searchCriteria.setParametersIfNotNull("id", id);
        searchCriteria.setParametersIfNotNull("domainIds", domainIds.toArray());
        searchCriteria.setParametersIfNotNull("accountId", accountId);
        searchCriteria.setParametersIfNotNull("userId", userId);
        searchCriteria.setParametersIfNotNull("hostId", hostId);
        searchCriteria.setParametersIfNotNull("instanceId", instanceId);
        searchCriteria.setParametersIfNotNull("startDate", startDate);
        searchCriteria.setParametersIfNotNull("endDate", endDate);
        searchCriteria.setParametersIfNotNull("creatorAddress", consoleEndpointCreatorAddress);
        searchCriteria.setParametersIfNotNull("clientAddress", clientAddress);

        return searchCriteria;
    }

    private SearchBuilder<ConsoleSessionVO> createListConsoleSessionsSearchBuilder(boolean activeOnly) {
        SearchBuilder<ConsoleSessionVO> searchBuilder = createSearchBuilder();

        searchBuilder.and("id", searchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        searchBuilder.and("domainIds", searchBuilder.entity().getDomainId(), SearchCriteria.Op.IN);
        searchBuilder.and("accountId", searchBuilder.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchBuilder.and("userId", searchBuilder.entity().getUserId(), SearchCriteria.Op.EQ);
        searchBuilder.and("hostId", searchBuilder.entity().getHostId(), SearchCriteria.Op.EQ);
        searchBuilder.and("instanceId", searchBuilder.entity().getInstanceId(), SearchCriteria.Op.EQ);
        searchBuilder.and("startDate", searchBuilder.entity().getAcquired(), SearchCriteria.Op.GTEQ);
        searchBuilder.and("endDate", searchBuilder.entity().getAcquired(), SearchCriteria.Op.LTEQ);
        searchBuilder.and("creatorAddress", searchBuilder.entity().getConsoleEndpointCreatorAddress(), SearchCriteria.Op.EQ);
        searchBuilder.and("clientAddress", searchBuilder.entity().getClientAddress(), SearchCriteria.Op.EQ);

        if (activeOnly) {
            searchBuilder.and("acquired", searchBuilder.entity().getAcquired(), SearchCriteria.Op.NNULL);
            searchBuilder.and("removed", searchBuilder.entity().getRemoved(), SearchCriteria.Op.NULL);
        }

        searchBuilder.done();
        return searchBuilder;
    }
}
