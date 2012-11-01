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
package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.VpnUser.State;
import com.cloud.network.VpnUserVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

@Component
@Local(value={VpnUserDao.class})
public class VpnUserDaoImpl extends GenericDaoBase<VpnUserVO, Long> implements VpnUserDao {
    private final SearchBuilder<VpnUserVO> AccountSearch;
    private final SearchBuilder<VpnUserVO> AccountNameSearch;
    private final GenericSearchBuilder<VpnUserVO, Long> VpnUserCount;


    protected VpnUserDaoImpl() {

        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
        
        AccountNameSearch = createSearchBuilder();
        AccountNameSearch.and("accountId", AccountNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountNameSearch.and("username", AccountNameSearch.entity().getUsername(), SearchCriteria.Op.EQ);
        AccountNameSearch.done();
        
        VpnUserCount = createSearchBuilder(Long.class);
        VpnUserCount.and("accountId", VpnUserCount.entity().getAccountId(), SearchCriteria.Op.EQ);
        VpnUserCount.and("state", VpnUserCount.entity().getState(), SearchCriteria.Op.NEQ);
        VpnUserCount.select(null, Func.COUNT, null);
        VpnUserCount.done();
    }

    @Override
    public List<VpnUserVO> listByAccount(Long accountId) {
        SearchCriteria<VpnUserVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

	@Override
	public VpnUserVO findByAccountAndUsername(Long accountId, String userName) {
		SearchCriteria<VpnUserVO> sc = AccountNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("username", userName);

        return findOneBy(sc);
	}

	@Override
	public long getVpnUserCount(Long accountId) {
		SearchCriteria<Long> sc = VpnUserCount.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("state", State.Revoke);
		List<Long> rs = customSearch(sc, null);
		if (rs.size() == 0) {
            return 0;
        }
        
        return rs.get(0);
	}
}
