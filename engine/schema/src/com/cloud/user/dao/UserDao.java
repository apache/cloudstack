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

import com.cloud.user.UserVO;
import com.cloud.utils.db.GenericDao;

/*
 * Data Access Object for user table
 */
public interface UserDao extends GenericDao<UserVO, Long> {
    UserVO getUser(String username, String password);

    UserVO getUser(String username);

    UserVO getUser(long userId);

    List<UserVO> findUsersLike(String username);

    List<UserVO> listByAccount(long accountId);

    /**
     * Finds a user based on the secret key provided.
     * @param secretKey
     * @return
     */
    UserVO findUserBySecretKey(String secretKey);

    /**
     * Finds a user based on the registration token provided.
     * @param registrationToken
     * @return
     */
    UserVO findUserByRegistrationToken(String registrationToken);

    List<UserVO> findUsersByName(String username);

}
