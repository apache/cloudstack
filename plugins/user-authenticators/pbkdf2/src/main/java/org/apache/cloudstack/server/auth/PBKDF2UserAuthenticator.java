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

package org.apache.cloudstack.server.auth;

import static java.lang.String.format;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.auth.UserAuthenticator;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Base64;

import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.ConstantTimeComparator;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class PBKDF2UserAuthenticator extends AdapterBase implements UserAuthenticator {
    private static final int s_saltlen = 64;
    private static final int s_rounds = 100000;
    private static final int s_keylen = 512;

    @Inject
    private UserAccountDao _userAccountDao;

    @Override
    public Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving user: " + username);
        }

        if (StringUtils.isAnyEmpty(username, password)) {
            logger.debug("Username or Password cannot be empty");
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        }

        boolean isValidUser = false;
        UserAccount user = this._userAccountDao.getUserAccount(username, domainId);
        if (user != null) {
            isValidUser = true;
        } else {
            logger.debug("Unable to find user with " + username + " in domain " + domainId);
        }

        byte[] salt = new byte[0];
        int rounds = s_rounds;
        try {
            if (isValidUser) {
                String[] storedPassword = user.getPassword().split(":");
                if ((storedPassword.length != 3) || (!StringUtils.isNumeric(storedPassword[2]))) {
                    logger.warn("The stored password for " + username + " isn't in the right format for this authenticator");
                    isValidUser = false;
                } else {
                    // Encoding format = <salt>:<password hash>:<rounds>
                    salt = decode(storedPassword[0]);
                    rounds = Integer.parseInt(storedPassword[2]);
                }
            }
            boolean result = false;
            if (isValidUser && validateCredentials(password, salt)) {
                result = ConstantTimeComparator.compareStrings(user.getPassword(), encode(password, salt, rounds));
            }

            UserAuthenticator.ActionOnFailedAuthentication action = null;
            if ((!result) && (isValidUser)) {
                action = UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT;
            }
            return new Pair(Boolean.valueOf(result), action);
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (InvalidKeySpecException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        }
    }

    @Override
    public String encode(String password)
    {
        try
        {
            return encode(password, makeSalt(), s_rounds);
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (InvalidKeySpecException e) {
            logger.error("Exception in EncryptUtil.createKey ", e);
            throw new CloudRuntimeException("Unable to hash password", e);
        }
    }

    public String encode(String password, byte[] salt, int rounds)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                        password.toCharArray()),
                salt,
                rounds);
        return format("%s:%s:%d", encode(salt),
                encode(((KeyParameter)generator.generateDerivedParameters(s_keylen)).getKey()), rounds);
    }

    public static byte[] makeSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[s_saltlen];
        sr.nextBytes(salt);
        return salt;
    }

    private static boolean validateCredentials(String plainPassword, byte[] hash) {
        return !(plainPassword == null || plainPassword.isEmpty() || hash == null || hash.length == 0);
    }

    private static String encode(byte[] input) throws UnsupportedEncodingException {
        return new String(Base64.encode(input), "UTF-8");
    }

    private static byte[] decode(String input) throws UnsupportedEncodingException {
        return Base64.decode(input.getBytes("UTF-8"));
    }
}
