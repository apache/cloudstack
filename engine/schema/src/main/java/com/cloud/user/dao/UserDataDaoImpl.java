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

import com.cloud.user.UserDataVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class UserDataDaoImpl extends GenericDaoBase<UserDataVO, Long> implements UserDataDao  {

    @Override
    public UserDataVO findByUserData(long accountId, long domainId, String userData) {
        SearchCriteria<UserDataVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("userData", SearchCriteria.Op.EQ, userData);
        return findOneBy(sc);
    }

    @Override
    public UserDataVO findByName(long accountId, long domainId, String name) {
        SearchCriteria<UserDataVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        return findOneBy(sc);
    }
}
