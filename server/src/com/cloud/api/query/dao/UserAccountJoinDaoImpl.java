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
package com.cloud.api.query.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;


import com.cloud.api.query.vo.UserAccountJoinVO;

import org.apache.cloudstack.api.response.UserResponse;
import org.springframework.stereotype.Component;

import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Component
@Local(value={UserAccountJoinDao.class})
public class UserAccountJoinDaoImpl extends GenericDaoBase<UserAccountJoinVO, Long> implements UserAccountJoinDao {
    public static final Logger s_logger = Logger.getLogger(UserAccountJoinDaoImpl.class);


    private SearchBuilder<UserAccountJoinVO> vrIdSearch;

    private SearchBuilder<UserAccountJoinVO> vrAcctIdSearch;


    protected UserAccountJoinDaoImpl() {

        vrIdSearch = createSearchBuilder();
        vrIdSearch.and("id", vrIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        vrIdSearch.done();

        vrAcctIdSearch = createSearchBuilder();
        vrAcctIdSearch.and("accountId", vrAcctIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        vrAcctIdSearch.done();


        this._count = "select count(distinct id) from user_view WHERE ";
    }



    @Override
    public UserResponse newUserResponse(UserAccountJoinVO usr) {
        UserResponse userResponse = new UserResponse();
        userResponse.setAccountId(usr.getAccountUuid());
        userResponse.setAccountName(usr.getAccountName());
        userResponse.setAccountType(usr.getAccountType());
        userResponse.setCreated(usr.getCreated());
        userResponse.setDomainId(usr.getDomainUuid());
        userResponse.setDomainName(usr.getDomainName());
        userResponse.setEmail(usr.getEmail());
        userResponse.setFirstname(usr.getFirstname());
        userResponse.setId(usr.getUuid());
        userResponse.setLastname(usr.getLastname());
        userResponse.setState(usr.getState().toString());
        userResponse.setTimezone(usr.getTimezone());
        userResponse.setUsername(usr.getUsername());
        userResponse.setApiKey(usr.getApiKey());
        userResponse.setSecretKey(usr.getSecretKey());

        // set async job
        userResponse.setJobId(usr.getJobUuid());
        userResponse.setJobStatus(usr.getJobStatus());

        userResponse.setObjectName("user");

        return userResponse;
    }


    @Override
    public UserAccountJoinVO newUserView(User usr) {
        SearchCriteria<UserAccountJoinVO> sc = vrIdSearch.create();
        sc.setParameters("id", usr.getId());
        List<UserAccountJoinVO> users = searchIncludingRemoved(sc, null, null, false);
        assert users != null && users.size() == 1 : "No user found for user id " + usr.getId();
        return users.get(0);
    }




    @Override
    public UserAccountJoinVO newUserView(UserAccount usr) {
        SearchCriteria<UserAccountJoinVO> sc = vrIdSearch.create();
        sc.setParameters("id", usr.getId());
        List<UserAccountJoinVO> users = searchIncludingRemoved(sc, null, null, false);
        assert users != null && users.size() == 1 : "No user found for user id " + usr.getId();
        return users.get(0);
    }


    @Override
    public List<UserAccountJoinVO> searchByAccountId(Long accountId) {
        SearchCriteria<UserAccountJoinVO> sc = vrAcctIdSearch.create();
        sc.setParameters("accountId", accountId);
        return searchIncludingRemoved(sc, null, null, false);
    }




}
