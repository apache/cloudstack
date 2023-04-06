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
    public void acquireSession(String sessionUuid) {
        ConsoleSessionVO consoleSessionVO = findByUuid(sessionUuid);
        consoleSessionVO.setAcquired(new Date());
        update(consoleSessionVO.getId(), consoleSessionVO);
    }


}
