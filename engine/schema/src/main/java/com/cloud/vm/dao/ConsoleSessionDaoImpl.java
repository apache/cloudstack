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
    private static final String ID = "id";
    private static final String DOMAIN_IDS = "domainIds";
    private static final String ACCOUNT_ID = "accountId";
    private static final String USER_ID = "userId";
    private static final String HOST_ID = "hostId";
    private static final String INSTANCE_ID = "instanceId";
    private static final String VM_IDS = "vmIds";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String CREATOR_ADDRESS = "creatorAddress";
    private static final String CLIENT_ADDRESS = "clientAddress";
    private static final String ACQUIRED = "acquired";
    private static final String CREATED = "created";
    private static final String REMOVED = "removed";
    private static final String REMOVED_NOT_NULL = "removedNotNull";

    private final SearchBuilder<ConsoleSessionVO> searchByRemovedDate;

    public ConsoleSessionDaoImpl() {
        searchByRemovedDate = createSearchBuilder();
        searchByRemovedDate.and(REMOVED_NOT_NULL, searchByRemovedDate.entity().getRemoved(), SearchCriteria.Op.NNULL);
        searchByRemovedDate.and(REMOVED, searchByRemovedDate.entity().getRemoved(), SearchCriteria.Op.LTEQ);
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
        searchCriteria.setParameters(REMOVED, date);
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
        sb.and(VM_IDS, sb.entity().getInstanceId(), SearchCriteria.Op.IN);
        SearchCriteria<ConsoleSessionVO> sc = sb.create();
        sc.setParameters(VM_IDS, vmIds.toArray());
        return batchExpunge(sc, batchSize);
    }

    @Override
    public Pair<List<ConsoleSessionVO>, Integer> listConsoleSessions(Long id, List<Long> domainIds, Long accountId, Long userId, Long hostId,
                                                                     Date startDate, Date endDate, Long instanceId,
                                                                     String consoleEndpointCreatorAddress, String clientAddress,
                                                                     boolean activeOnly, boolean acquired, Long pageSizeVal, Long startIndex) {
        Filter filter = new Filter(ConsoleSessionVO.class, CREATED, false, startIndex, pageSizeVal);
        SearchCriteria<ConsoleSessionVO> searchCriteria = createListConsoleSessionsSearchCriteria(id, domainIds, accountId, userId, hostId,
                startDate, endDate, instanceId, consoleEndpointCreatorAddress, clientAddress, activeOnly, acquired);

        return searchAndCount(searchCriteria, filter, true);
    }

    private SearchCriteria<ConsoleSessionVO> createListConsoleSessionsSearchCriteria(Long id, List<Long> domainIds, Long accountId, Long userId, Long hostId,
                                                                                     Date startDate, Date endDate, Long instanceId,
                                                                                     String consoleEndpointCreatorAddress, String clientAddress,
                                                                                     boolean activeOnly, boolean acquired) {
        SearchCriteria<ConsoleSessionVO> searchCriteria = createListConsoleSessionsSearchBuilder(activeOnly, acquired).create();

        searchCriteria.setParametersIfNotNull(ID, id);
        searchCriteria.setParametersIfNotNull(DOMAIN_IDS, domainIds.toArray());
        searchCriteria.setParametersIfNotNull(ACCOUNT_ID, accountId);
        searchCriteria.setParametersIfNotNull(USER_ID, userId);
        searchCriteria.setParametersIfNotNull(HOST_ID, hostId);
        searchCriteria.setParametersIfNotNull(INSTANCE_ID, instanceId);
        searchCriteria.setParametersIfNotNull(START_DATE, startDate);
        searchCriteria.setParametersIfNotNull(END_DATE, endDate);
        searchCriteria.setParametersIfNotNull(CREATOR_ADDRESS, consoleEndpointCreatorAddress);
        searchCriteria.setParametersIfNotNull(CLIENT_ADDRESS, clientAddress);

        return searchCriteria;
    }

    private SearchBuilder<ConsoleSessionVO> createListConsoleSessionsSearchBuilder(boolean activeOnly, boolean acquired) {
        SearchBuilder<ConsoleSessionVO> searchBuilder = createSearchBuilder();

        searchBuilder.and(ID, searchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        searchBuilder.and(DOMAIN_IDS, searchBuilder.entity().getDomainId(), SearchCriteria.Op.IN);
        searchBuilder.and(ACCOUNT_ID, searchBuilder.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchBuilder.and(USER_ID, searchBuilder.entity().getUserId(), SearchCriteria.Op.EQ);
        searchBuilder.and(HOST_ID, searchBuilder.entity().getHostId(), SearchCriteria.Op.EQ);
        searchBuilder.and(INSTANCE_ID, searchBuilder.entity().getInstanceId(), SearchCriteria.Op.EQ);
        searchBuilder.and(START_DATE, searchBuilder.entity().getCreated(), SearchCriteria.Op.GTEQ);
        searchBuilder.and(END_DATE, searchBuilder.entity().getCreated(), SearchCriteria.Op.LTEQ);
        searchBuilder.and(CREATOR_ADDRESS, searchBuilder.entity().getConsoleEndpointCreatorAddress(), SearchCriteria.Op.EQ);
        searchBuilder.and(CLIENT_ADDRESS, searchBuilder.entity().getClientAddress(), SearchCriteria.Op.EQ);

        if (activeOnly) {
            searchBuilder.and(ACQUIRED, searchBuilder.entity().getAcquired(), SearchCriteria.Op.NNULL);
            searchBuilder.and(REMOVED, searchBuilder.entity().getRemoved(), SearchCriteria.Op.NULL);
        } else if (acquired) {
            searchBuilder.and(ACQUIRED, searchBuilder.entity().getAcquired(), SearchCriteria.Op.NNULL);
        }

        searchBuilder.done();
        return searchBuilder;
    }
}
