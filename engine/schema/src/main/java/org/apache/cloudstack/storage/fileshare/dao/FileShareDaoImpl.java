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

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMNetworkMapVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMNetworkMapDao;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareVO;

import javax.inject.Inject;
import java.util.List;

public class FileShareDaoImpl extends GenericDaoBase<FileShareVO, Long> implements FileShareDao {

    @Inject
    VMNetworkMapDao vmNetworkMapDao;

    public FileShareDaoImpl() {
    }

    @Override
    public boolean updateState(FileShare.State currentState, FileShare.Event event, FileShare.State nextState, FileShare vo, Object data) {
        return false;
    }

    @Override
    public Pair<List<FileShareVO>, Integer> searchAndCount(Long fileShareId, Long accountId, Long networkId, Long startIndex, Long pageSizeVal) {
        final SearchBuilder<FileShareVO> sb = createSearchBuilder();
        if (fileShareId != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        }
        if (accountId != null) {
            sb.and("account_id", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        }
        if (networkId != null) {
            SearchBuilder<VMNetworkMapVO> vmNetSearch = vmNetworkMapDao.createSearchBuilder();
            vmNetSearch.and("network_id", vmNetSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
            sb.join("vmNetSearch", vmNetSearch, vmNetSearch.entity().getVmId(), sb.entity().getVmId(), JoinBuilder.JoinType.INNER);
        }

        Filter searchFilter = new Filter(FileShareVO.class, "id", Boolean.TRUE, startIndex, pageSizeVal);
        final SearchCriteria<FileShareVO> sc = sb.create();

        if (fileShareId != null) {
            sc.setParameters("id", fileShareId);
        }
        if (accountId != null) {
            sc.setParameters("account_id", accountId);
        }
        if (networkId != null) {
            sc.setJoinParameters("vmNetSearch", "network_id", networkId);
        }

        return searchAndCount(sc, searchFilter);
    }
}
