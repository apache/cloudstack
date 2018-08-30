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

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.user.UserVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class UserDaoImpl extends GenericDaoBase<UserVO, Long> implements UserDao {
    protected SearchBuilder<UserVO> UsernamePasswordSearch;
    protected SearchBuilder<UserVO> UsernameSearch;
    protected SearchBuilder<UserVO> UsernameLikeSearch;
    protected SearchBuilder<UserVO> UserIdSearch;
    protected SearchBuilder<UserVO> AccountIdSearch;
    protected SearchBuilder<UserVO> SecretKeySearch;
    protected SearchBuilder<UserVO> RegistrationTokenSearch;

    protected UserDaoImpl() {
        UsernameSearch = createSearchBuilder();
        UsernameSearch.and("username", UsernameSearch.entity().getUsername(), SearchCriteria.Op.EQ);
        UsernameSearch.done();

        UsernameLikeSearch = createSearchBuilder();
        UsernameLikeSearch.and("username", UsernameLikeSearch.entity().getUsername(), SearchCriteria.Op.LIKE);
        UsernameLikeSearch.done();

        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("account", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();

        UsernamePasswordSearch = createSearchBuilder();
        UsernamePasswordSearch.and("username", UsernamePasswordSearch.entity().getUsername(), SearchCriteria.Op.EQ);
        UsernamePasswordSearch.and("password", UsernamePasswordSearch.entity().getPassword(), SearchCriteria.Op.EQ);
        UsernamePasswordSearch.done();

        UserIdSearch = createSearchBuilder();
        UserIdSearch.and("id", UserIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        UserIdSearch.done();

        SecretKeySearch = createSearchBuilder();
        SecretKeySearch.and("secretKey", SecretKeySearch.entity().getSecretKey(), SearchCriteria.Op.EQ);
        SecretKeySearch.done();

        RegistrationTokenSearch = createSearchBuilder();
        RegistrationTokenSearch.and("registrationToken", RegistrationTokenSearch.entity().getRegistrationToken(), SearchCriteria.Op.EQ);
        RegistrationTokenSearch.done();
    }

    @Override
    public UserVO getUser(String username, String password) {
        SearchCriteria<UserVO> sc = UsernamePasswordSearch.create();
        sc.setParameters("username", username);
        sc.setParameters("password", password);
        return findOneBy(sc);
    }

    @Override
    public List<UserVO> listByAccount(long accountId) {
        SearchCriteria<UserVO> sc = AccountIdSearch.create();
        sc.setParameters("account", accountId);
        return listBy(sc, null);
    }

    @Override
    public UserVO getUser(String username) {
        SearchCriteria<UserVO> sc = UsernameSearch.create();
        sc.setParameters("username", username);
        return findOneBy(sc);
    }

    @Override
    public UserVO getUser(long userId) {
        SearchCriteria<UserVO> sc = UserIdSearch.create();
        sc.setParameters("id", userId);
        return findOneBy(sc);
    }

    @Override
    public List<UserVO> findUsersLike(String username) {
        SearchCriteria<UserVO> sc = UsernameLikeSearch.create();
        sc.setParameters("username", "%" + username + "%");
        return listBy(sc);
    }

    @Override
    public UserVO findUserBySecretKey(String secretKey) {
        SearchCriteria<UserVO> sc = SecretKeySearch.create();
        sc.setParameters("secretKey", secretKey);
        return findOneBy(sc);
    }

    @Override
    public UserVO findUserByRegistrationToken(String registrationToken) {
        SearchCriteria<UserVO> sc = RegistrationTokenSearch.create();
        sc.setParameters("registrationToken", registrationToken);
        return findOneBy(sc);
    }

    @Override
    public List<UserVO> findUsersByName(String username) {
        SearchCriteria<UserVO> sc = UsernameSearch.create();
        sc.setParameters("username", username);
        return listBy(sc);
    }

}
