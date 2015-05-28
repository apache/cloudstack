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
package com.cloud.user.dao;

import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value = {UserAccountDao.class})
public class UserAccountDaoImpl extends GenericDaoBase<UserAccountVO, Long> implements UserAccountDao {

    protected final SearchBuilder<UserAccountVO> userAccountSearch;

    public UserAccountDaoImpl() {
        userAccountSearch = createSearchBuilder();
        userAccountSearch.and("apiKey", userAccountSearch.entity().getApiKey(), SearchCriteria.Op.EQ);
        userAccountSearch.done();
    }

    @Override
    public List<UserAccountVO> getAllUsersByNameAndEntity(String username, String entity) {
        if (username == null) {
            return null;
        }
        SearchCriteria<UserAccountVO> sc = createSearchCriteria();
        sc.addAnd("username", SearchCriteria.Op.EQ, username);
        sc.addAnd("externalEntity", SearchCriteria.Op.EQ, entity);
        return listBy(sc);
    }

    @Override
    public UserAccount getUserAccount(String username, Long domainId) {
        if ((username == null) || (domainId == null)) {
            return null;
        }

        SearchCriteria<UserAccountVO> sc = createSearchCriteria();
        sc.addAnd("username", SearchCriteria.Op.EQ, username);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        return findOneBy(sc);
    }

    @Override
    public boolean validateUsernameInDomain(String username, Long domainId) {
        UserAccount userAcct = getUserAccount(username, domainId);
        if (userAcct == null) {
            return true;
        }
        return false;
    }

    @Override
    public UserAccount getUserByApiKey(String apiKey) {
        SearchCriteria<UserAccountVO> sc = userAccountSearch.create();
        sc.setParameters("apiKey", apiKey);
        return findOneBy(sc);
    }

}
