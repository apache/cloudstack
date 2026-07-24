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


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import com.cloud.exception.CloudTwoFactorAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;

import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;

import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.component.AdapterBase;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class TotpUserTwoFactorAuthenticator extends AdapterBase implements UserTwoFactorAuthenticator, Configurable {

    public static final ConfigKey<Integer> totpWindowSteps = new ConfigKey<>("Advanced", Integer.class,
            "user.2fa.totp.window.steps",
            "1",
            "Number of 30-second TOTP time-steps, on each side of the current step, that are accepted during " +
                    "verification. This tolerates clock drift between the server and the user's authenticator device. " +
                    "Set to 0 to accept only the current step (strictest, no skew tolerance). This can also be " +
                    "configured at domain level.",
            true,
            ConfigKey.Scope.Domain);

    // RFC 6238 / RFC 4226 parameters. These match the values previously produced by the
    // de.taimos:totp library (HMAC-SHA1, 6 digits, 30-second time step), so codes from
    // authenticator apps enrolled before this change continue to verify unchanged.
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int SECRET_LENGTH_BYTES = 20;

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
        if (StringUtils.isBlank(code)) {
            String msg = "two-factor authentication code provided is empty";
            logger.error(msg);
            throw new CloudTwoFactorAuthenticationException(msg);
        }

        byte[] secretBytes = decodeSecret(get2FAKey(userAccount));
        int windowSteps = getWindowSteps(userAccount);
        long baseStep = currentTimeStep();

        for (long step = baseStep - windowSteps; step <= baseStep + windowSteps; step++) {
            String expectedCode = generateCode(secretBytes, step);
            if (constantTimeEquals(expectedCode, code)) {
                verifyNotReplayed(userAccount, step);
                recordUsedStep(userAccount, step);
                logger.info("2FA matches user's input");
                return;
            }
        }

        String msg = "two-factor authentication code provided is invalid";
        logger.error(msg);
        throw new CloudTwoFactorAuthenticationException(msg);
    }

    /**
     * Current TOTP time-step counter: Unix time divided by the 30-second step size.
     */
    protected long currentTimeStep() {
        return (System.currentTimeMillis() / 1000L) / TIME_STEP_SECONDS;
    }

    /**
     * Computes the RFC 6238 TOTP code for the given secret and time-step (RFC 4226 HOTP with
     * the step as the counter). HMAC-SHA1, 6 digits — matching the enrollment format.
     */
    protected String generateCode(byte[] secret, long timeStep) {
        byte[] counter = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) (timeStep & 0xff);
            timeStep >>= 8;
        }

        byte[] hash;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            hash = mac.doFinal(counter);
        } catch (GeneralSecurityException e) {
            throw new CloudRuntimeException("Unable to compute TOTP code", e);
        }

        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    /**
     * Rejects a code whose time-step is not strictly newer than the last accepted step, so a
     * valid code cannot be replayed within (or across) its acceptance window.
     */
    protected void verifyNotReplayed(UserAccount userAccount, long matchedStep) throws CloudTwoFactorAuthenticationException {
        Long lastUsedStep = userAccount.getLastUsed2faStep();
        if (lastUsedStep != null && matchedStep <= lastUsedStep) {
            String msg = "two-factor authentication code has already been used";
            logger.error(msg);
            throw new CloudTwoFactorAuthenticationException(msg);
        }
    }

    /**
     * Persists the accepted time-step so subsequent verifications reject reuse. Uses a fresh
     * update object (mirroring how 2FA setup persists) since {@code check2FA} receives a
     * read-only snapshot of the user.
     */
    protected void recordUsedStep(UserAccount userAccount, long matchedStep) {
        UserAccountVO forUpdate = _userAccountDao.createForUpdate();
        forUpdate.setLastUsed2faStep(matchedStep);
        _userAccountDao.update(userAccount.getId(), forUpdate);
    }

    /**
     * Reads the accepted verification window (in 30s steps) for the user's domain. A negative
     * configured value is treated as 0 (strict, current-step-only).
     */
    protected int getWindowSteps(UserAccount userAccount) {
        Integer configured = totpWindowSteps.valueIn(userAccount.getDomainId());
        if (configured == null || configured < 0) {
            return 0;
        }
        return configured;
    }

    /**
     * Constant-time comparison of the expected and supplied codes to avoid leaking information
     * through comparison timing.
     */
    protected boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }

    private String get2FAKey(UserAccount userAccount) {
        return userAccount.getKeyFor2fa();
    }

    /**
     * Decodes the stored Base32 secret into the raw key bytes used as the HMAC key.
     */
    private byte[] decodeSecret(String secretKey) {
        return new Base32().decode(secretKey);
    }

    @Override
    public String setup2FAKey(UserAccount userAccount) {
        if (StringUtils.isNotEmpty(userAccount.getKeyFor2fa())) {
            throw new CloudRuntimeException(String.format("2FA key is already setup for the user Account %s", userAccount.getAccountName()));
        }
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_LENGTH_BYTES];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        String key = base32.encodeToString(bytes);
        userAccount.setKeyFor2fa(key);
        return key;
    }

    @Override
    public String getConfigComponentName() {
        return TotpUserTwoFactorAuthenticator.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {totpWindowSteps};
    }
}
