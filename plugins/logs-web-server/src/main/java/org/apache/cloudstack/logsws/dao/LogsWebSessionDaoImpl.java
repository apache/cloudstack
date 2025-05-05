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

package org.apache.cloudstack.logsws.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.logsws.vo.LogsWebSessionVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;

public class LogsWebSessionDaoImpl extends GenericDaoBase<LogsWebSessionVO, Long> implements LogsWebSessionDao {
    SearchBuilder<LogsWebSessionVO> accountIdSearch;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        accountIdSearch = createSearchBuilder();
        accountIdSearch.and("accountId", accountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);

        return true;
    }

    @Override
    public void deleteByAccount(long accountId) {
        SearchCriteria<LogsWebSessionVO> sc = accountIdSearch.create();
        sc.setParameters("accountId", accountId);
        remove(sc);
    }

    @Override
    public List<LogsWebSessionVO> listByAccount(long accountId) {
        SearchCriteria<LogsWebSessionVO> sc = accountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public void markAllActiveAsDisconnected() {
        SearchBuilder<LogsWebSessionVO> sb = createSearchBuilder();
        sb.and("connections", sb.entity().getConnections(), SearchCriteria.Op.GT);
        sb.done();
        SearchCriteria<LogsWebSessionVO> sc = sb.create();
        sc.setParameters("connections", 0);
        LogsWebSessionVO logsWebSessionVO = createForUpdate();
        logsWebSessionVO.setConnections(0);
        UpdateBuilder updateBuilder = getUpdateBuilder(logsWebSessionVO);
        update(updateBuilder, sc, null);
    }

    @Override
    public int removeStaleForCutOff(Date cutOff) {
        SearchBuilder<LogsWebSessionVO> sb = createSearchBuilder();
        sb.and("connections", sb.entity().getConnections(), SearchCriteria.Op.EQ);
        sb.and().op("connected_time", sb.entity().getConnectedTime(), SearchCriteria.Op.LT);
        sb.or().op("null_connected_time", sb.entity().getConnectedTime(), SearchCriteria.Op.NULL);
        sb.and("created", sb.entity().getCreated(), SearchCriteria.Op.LT);
        sb.cp();
        sb.cp();
        sb.done();
        SearchCriteria<LogsWebSessionVO> sc = sb.create();
        sc.setParameters("connections", 0);
        sc.setParameters("connected_time", cutOff);
        sc.setParameters("created", cutOff);
        return remove(sc);
    }

    @Override
    public int countConnected() {
        GenericSearchBuilder<LogsWebSessionVO, Integer> sb = createSearchBuilder(Integer.class);
        sb.and("connections", sb.entity().getConnections(), SearchCriteria.Op.GT);
        sb.select(null, SearchCriteria.Func.COUNT, sb.entity().getId());
        sb.done();
        SearchCriteria<Integer> sc = sb.create();
        sc.setParameters("connections", 0);
        return customSearch(sc, null).get(0);
    }
}
