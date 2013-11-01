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

import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.network.dao.AccountGuestVlanMapDao;

import java.util.List;
import javax.ejb.Local;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={AccountGuestVlanMapDao.class})
@DB
public class AccountGuestVlanMapDaoImpl extends GenericDaoBase<AccountGuestVlanMapVO, Long> implements AccountGuestVlanMapDao {

    protected SearchBuilder<AccountGuestVlanMapVO> AccountSearch;
    protected SearchBuilder<AccountGuestVlanMapVO> GuestVlanSearch;
    protected SearchBuilder<AccountGuestVlanMapVO> PhysicalNetworkSearch;

    @Override
    public List<AccountGuestVlanMapVO> listAccountGuestVlanMapsByAccount(long accountId) {
        SearchCriteria<AccountGuestVlanMapVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<AccountGuestVlanMapVO> listAccountGuestVlanMapsByVlan(long guestVlanId) {
        SearchCriteria<AccountGuestVlanMapVO> sc = GuestVlanSearch.create();
        sc.setParameters("guestVlanId", guestVlanId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<AccountGuestVlanMapVO> listAccountGuestVlanMapsByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<AccountGuestVlanMapVO> sc = GuestVlanSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public int removeByAccountId(long accountId) {
        SearchCriteria<AccountGuestVlanMapVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return expunge(sc);
    }

    public AccountGuestVlanMapDaoImpl() {
        super();
        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        GuestVlanSearch = createSearchBuilder();
        GuestVlanSearch.and("guestVlanId", GuestVlanSearch.entity().getId(), SearchCriteria.Op.EQ);
        GuestVlanSearch.done();

        PhysicalNetworkSearch = createSearchBuilder();
        PhysicalNetworkSearch.and("physicalNetworkId", PhysicalNetworkSearch.entity().getId(), SearchCriteria.Op.EQ);
        PhysicalNetworkSearch.done();
    }

}
