//  Licensed to the Apache Software Foundation (ASF) under one or more
//  contributor license agreements.  See the NOTICE file distributed with
//  this work for additional information regarding copyright ownership.
//  The ASF licenses this file to You under the Apache License, Version 2.0
//  (the "License"); you may not use this file except in compliance with
//  the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.apache.cloudstack.auth;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Simple UserAuthenticator that performs a MD5 hash of the password before
 * comparing it against the local database.
 *
 */
public class MD5UserAuthenticator extends AdapterBase implements UserAuthenticator {

    @Inject
    private UserAccountDao _userAccountDao;

    @Override
    public Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving user: " + username);
        }

        if (StringUtils.isAnyEmpty(username, password)) {
            logger.debug("Username or Password cannot be empty");
            return new Pair<>(false, null);
        }

        UserAccount user = _userAccountDao.getUserAccount(username, domainId);
        if (user == null) {
            logger.debug("Unable to find user with " + username + " in domain " + domainId);
            return new Pair<>(false, null);
        }

        if (!user.getPassword().equals(encode(password))) {
            logger.debug("Password does not match");
            return new Pair<>(false, ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);
        }
        return new Pair<>(true, null);
    }

    @Override
    public String encode(final String password) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            BigInteger pwInt = new BigInteger(1, md5.digest(password.getBytes()));
            String pwStr = pwInt.toString(16);
            int padding = 32 - pwStr.length();
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < padding; i++) {
                sb.append('0'); // make sure the MD5 password is 32 digits long
            }
            sb.append(pwStr);
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        }

    }
}
