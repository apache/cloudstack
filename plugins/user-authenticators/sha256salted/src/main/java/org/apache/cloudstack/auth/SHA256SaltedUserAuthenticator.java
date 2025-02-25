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
package org.apache.cloudstack.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;

import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class SHA256SaltedUserAuthenticator extends AdapterBase implements UserAuthenticator {
    private static final String s_defaultPassword = "000000000000000000000000000=";
    private static final String s_defaultSalt = "0000000000000000000000000000000=";
    @Inject
    private UserAccountDao _userAccountDao;
    private static final int s_saltlen = 32;

    /* (non-Javadoc)
     * @see com.cloud.server.auth.UserAuthenticator#authenticate(java.lang.String, java.lang.String, java.lang.Long, java.util.Map)
     */
    @Override
    public Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving user: " + username);
        }

        if (StringUtils.isAnyEmpty(username, password)) {
            logger.debug("Username or Password cannot be empty");
            return new Pair<>(false, null);
        }

        boolean realUser = true;
        UserAccount user = _userAccountDao.getUserAccount(username, domainId);
        if (user == null) {
            logger.debug("Unable to find user with " + username + " in domain " + domainId);
            realUser = false;
        }
        /* Fake Data */
        String realPassword = s_defaultPassword;
        byte[] salt = s_defaultSalt.getBytes();
        if (realUser) {
            String[] storedPassword = user.getPassword().split(":");
            if (storedPassword.length != 2) {
                logger.warn("The stored password for " + username + " isn't in the right format for this authenticator");
                realUser = false;
            } else {
                realPassword = storedPassword[1];
                salt = Base64.decode(storedPassword[0]);
            }
        }
        try {
            String hashedPassword = encode(password, salt);
            /* constantTimeEquals comes first in boolean since we need to thwart timing attacks */
            boolean result = constantTimeEquals(realPassword, hashedPassword) && realUser;
            ActionOnFailedAuthentication action = null;
            if (!result && realUser) {
                action = ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT;
            }
            return new Pair<>(result, action);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        }
    }

    /* (non-Javadoc)
     * @see com.cloud.server.auth.UserAuthenticator#encode(java.lang.String)
     */
    @Override
    public String encode(String password) {
        // 1. Generate the salt
        SecureRandom randomGen;
        try {
            randomGen = SecureRandom.getInstance("SHA1PRNG");

            byte[] salt = new byte[s_saltlen];
            randomGen.nextBytes(salt);

            String saltString = new String(Base64.encode(salt));
            String hashString = encode(password, salt);

            // 3. concatenate the two and return
            return saltString + ":" + hashString;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        }
    }

    public String encode(String password, byte[] salt) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] passwordBytes = password.getBytes("UTF-8");
        byte[] hashSource = new byte[passwordBytes.length + salt.length];
        System.arraycopy(passwordBytes, 0, hashSource, 0, passwordBytes.length);
        System.arraycopy(salt, 0, hashSource, passwordBytes.length, salt.length);

        // 2. Hash the password with the salt
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(hashSource);
        byte[] digest = md.digest();

        return new String(Base64.encode(digest));
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        int result = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
