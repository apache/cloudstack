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

package org.apache.cloudstack.auth;


import javax.inject.Inject;

import com.cloud.exception.CloudTwoFactorAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;
import de.taimos.totp.TOTP;

import com.cloud.user.UserAccount;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.component.AdapterBase;

import java.security.SecureRandom;

public class TotpUserTwoFactorAuthenticator extends AdapterBase implements UserTwoFactorAuthenticator {

    @Inject
    private UserAccountDao _userAccountDao;

    @Override
    public String getName() {
        return "totp";
    }

    @Override
    public String getDescription() {
        return "TOTP user two factor authentication provider Plugin";
    }

    @Override
    public void check2FA(String code, UserAccount userAccount) throws CloudTwoFactorAuthenticationException {
        String expectedCode = get2FACode(get2FAKey(userAccount));
        if (expectedCode.equals(code)) {
            logger.info("2FA matches user's input");
            return;
        }
        throw new CloudTwoFactorAuthenticationException("two-factor authentication code provided is invalid");
    }

    private String get2FAKey(UserAccount userAccount) {
        return userAccount.getKeyFor2fa();
    }

    private String get2FACode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }

    @Override
    public String setup2FAKey(UserAccount userAccount) {
        if (StringUtils.isNotEmpty(userAccount.getKeyFor2fa())) {
            throw new CloudRuntimeException(String.format("2FA key is already setup for the user account %s", userAccount.getAccountName()));
        }
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        String key = base32.encodeToString(bytes);
        userAccount.setKeyFor2fa(key);
        return key;
    }
}
