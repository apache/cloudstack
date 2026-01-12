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

package org.apache.cloudstack.kms.dao;

import java.util.List;

import org.apache.cloudstack.kms.HSMProfileVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class HSMProfileDaoImpl extends GenericDaoBase<HSMProfileVO, Long> implements HSMProfileDao {

    protected SearchBuilder<HSMProfileVO> AccountSearch;
    protected SearchBuilder<HSMProfileVO> AdminSearch;
    protected SearchBuilder<HSMProfileVO> NameSearch;

    public HSMProfileDaoImpl() {
        super();
        
        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), Op.EQ);
        AccountSearch.and("removed", AccountSearch.entity().getRemoved(), Op.NULL);
        AccountSearch.done();
        
        AdminSearch = createSearchBuilder();
        AdminSearch.and("accountId", AdminSearch.entity().getAccountId(), Op.NULL);
        AdminSearch.and("zoneId", AdminSearch.entity().getZoneId(), Op.EQ);
        AdminSearch.and("removed", AdminSearch.entity().getRemoved(), Op.NULL);
        AdminSearch.done();
        
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), Op.EQ);
        NameSearch.and("removed", NameSearch.entity().getRemoved(), Op.NULL);
        NameSearch.done();
    }

    @Override
    public List<HSMProfileVO> listByAccountId(Long accountId) {
        SearchCriteria<HSMProfileVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public List<HSMProfileVO> listAdminProfiles() {
        SearchCriteria<HSMProfileVO> sc = AdminSearch.create();
        // Global admin profiles have zone_id = NULL
        sc.setParameters("zoneId", (Object)null); 
        return listBy(sc);
    }

    @Override
    public List<HSMProfileVO> listAdminProfiles(Long zoneId) {
        SearchCriteria<HSMProfileVO> sc = AdminSearch.create();
        sc.setParameters("zoneId", zoneId);
        return listBy(sc);
    }

    @Override
    public HSMProfileVO findByName(String name) {
        SearchCriteria<HSMProfileVO> sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }
}
