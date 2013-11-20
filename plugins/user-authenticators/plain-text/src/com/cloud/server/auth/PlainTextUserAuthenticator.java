// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloud.server.auth;

import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;

@Local(value = {UserAuthenticator.class})
public class PlainTextUserAuthenticator extends DefaultUserAuthenticator {
    public static final Logger s_logger = Logger.getLogger(PlainTextUserAuthenticator.class);

    @Inject
    private UserAccountDao _userAccountDao;

    @Override
    public boolean authenticate(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieving user: " + username);
        }

        UserAccount user = _userAccountDao.getUserAccount(username, domainId);
        if (user == null) {
            s_logger.debug("Unable to find user with " + username + " in domain " + domainId);
            return false;
        }

        if (!user.getPassword().equals(password)) {
            s_logger.debug("Password does not match");
            return false;
        }
        return true;
    }

    @Override
    public String encode(String password) {
        // Plaintext so no encoding at all
        return password;
    }
}
