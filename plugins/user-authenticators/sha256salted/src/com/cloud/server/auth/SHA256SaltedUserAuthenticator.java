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
package com.cloud.server.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={UserAuthenticator.class})
public class SHA256SaltedUserAuthenticator extends DefaultUserAuthenticator {
    public static final Logger s_logger = Logger.getLogger(SHA256SaltedUserAuthenticator.class);

    @Inject
    private UserAccountDao _userAccountDao;
    private static int s_saltlen = 20;

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.server.auth.UserAuthenticator#authenticate(java.lang.String, java.lang.String, java.lang.Long, java.util.Map)
     */
    @Override
    public boolean authenticate(String username, String password,
            Long domainId, Map<String, Object[]> requestParameters) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieving user: " + username);
        }
        UserAccount user = _userAccountDao.getUserAccount(username, domainId);
        if (user == null) {
            s_logger.debug("Unable to find user with " + username + " in domain " + domainId);
            return false;
        }

        try {
            String storedPassword[] = user.getPassword().split(":");
            if (storedPassword.length != 2) {
                s_logger.warn("The stored password for " + username + " isn't in the right format for this authenticator");
                return false;
            }
            byte salt[] = Base64.decode(storedPassword[0]);
            String hashedPassword = encode(password, salt);
            return storedPassword[1].equals(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (UnsupportedEncodingException e) {
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

            byte salt[] = new byte[s_saltlen];
            randomGen.nextBytes(salt);

            String saltString = new String(Base64.encode(salt));
            String hashString = encode(password, salt);

            // 3. concatenate the two and return
            return saltString + ":" + hashString;
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        }
    }

    public String encode(String password, byte[] salt) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] passwordBytes = password.getBytes("UTF-8");
        byte[] hashSource = new byte[passwordBytes.length + s_saltlen];
        System.arraycopy(passwordBytes, 0, hashSource, 0, passwordBytes.length);
        System.arraycopy(salt, 0, hashSource, passwordBytes.length, s_saltlen);

        // 2. Hash the password with the salt
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(hashSource);
        byte[] digest = md.digest();

        return new String(Base64.encode(digest));
    }
}
