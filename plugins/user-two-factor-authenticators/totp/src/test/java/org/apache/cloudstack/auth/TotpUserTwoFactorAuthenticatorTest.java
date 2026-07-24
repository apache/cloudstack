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

import static org.junit.Assert.assertThrows;

import com.cloud.exception.CloudTwoFactorAuthenticationException;
import com.cloud.user.UserAccount;
import org.apache.commons.codec.binary.Base32;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TotpUserTwoFactorAuthenticatorTest {

    private static final String BASE32_KEY = "JBSWY3DPEHPK3PXP";

    private UserAccount userAccount;

    /**
     * Test subclass that lets each test drive the accepted window without touching the
     * global {@link org.apache.cloudstack.framework.config.ConfigKey}, and simulates the DAO's
     * atomic "record step if newer" in memory instead of hitting the (un-injected) DAO.
     */
    private static class TestableAuthenticator extends TotpUserTwoFactorAuthenticator {
        private int window;
        Long recordedStep;

        void setWindow(int window) {
            this.window = window;
        }

        void seedLastUsedStep(Long step) {
            this.recordedStep = step;
        }

        @Override
        protected int getWindowSteps(UserAccount userAccount) {
            return window;
        }

        @Override
        protected boolean recordUsedStepIfNewer(UserAccount userAccount, long matchedStep) {
            if (recordedStep != null && matchedStep <= recordedStep) {
                return false;
            }
            recordedStep = matchedStep;
            return true;
        }
    }

    private TestableAuthenticator authenticator;

    @Before
    public void setUp() {
        authenticator = new TestableAuthenticator();
        userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(userAccount.getKeyFor2fa()).thenReturn(BASE32_KEY);
    }

    private byte[] secretBytes() {
        return new Base32().decode(BASE32_KEY);
    }

    private String codeForStep(long step) {
        // Use the authenticator's own RFC 6238 implementation to derive the expected code,
        // exactly as a compliant authenticator app would for the same secret and step.
        return authenticator.generateCode(secretBytes(), step);
    }

    private long currentStep() {
        return authenticator.currentTimeStep();
    }

    @Test
    public void testCurrentStepAcceptedWithWindowOne() throws CloudTwoFactorAuthenticationException {
        authenticator.setWindow(1);
        authenticator.check2FA(codeForStep(currentStep()), userAccount);
    }

    @Test
    public void testCurrentStepAcceptedWithWindowZero() throws CloudTwoFactorAuthenticationException {
        // With window 0 the code for the current step must be accepted. To avoid a rare failure
        // when the 30s step rolls over between generating the code and verifying it, retry once.
        authenticator.setWindow(0);
        try {
            authenticator.check2FA(codeForStep(currentStep()), userAccount);
        } catch (CloudTwoFactorAuthenticationException boundaryRollover) {
            authenticator.check2FA(codeForStep(currentStep()), userAccount);
        }
    }

    @Test
    public void testPreviousStepAcceptedWithinWindow() throws CloudTwoFactorAuthenticationException {
        authenticator.setWindow(1);
        authenticator.check2FA(codeForStep(currentStep() - 1), userAccount);
    }

    @Test
    public void testNextStepAcceptedWithinWindow() throws CloudTwoFactorAuthenticationException {
        authenticator.setWindow(1);
        authenticator.check2FA(codeForStep(currentStep() + 1), userAccount);
    }

    @Test
    public void testStepFarOutsideWindowRejectedWhenWindowZero() {
        // A code from 5 steps ago can never be accepted with window 0, regardless of any
        // single-step boundary rollover during the test.
        authenticator.setWindow(0);
        assertThrows(CloudTwoFactorAuthenticationException.class,
                () -> authenticator.check2FA(codeForStep(currentStep() - 5), userAccount));
    }

    @Test
    public void testStepOutsideWindowRejected() {
        authenticator.setWindow(1);
        assertThrows(CloudTwoFactorAuthenticationException.class,
                () -> authenticator.check2FA(codeForStep(currentStep() - 5), userAccount));
    }

    @Test
    public void testInvalidCodeRejected() {
        authenticator.setWindow(1);
        assertThrows(CloudTwoFactorAuthenticationException.class,
                () -> authenticator.check2FA("000000", userAccount));
    }

    @Test
    public void testBlankCodeRejected() {
        authenticator.setWindow(1);
        assertThrows(CloudTwoFactorAuthenticationException.class,
                () -> authenticator.check2FA("", userAccount));
    }

    @Test
    public void testAcceptedStepIsRecorded() throws CloudTwoFactorAuthenticationException {
        authenticator.setWindow(1);
        long step = currentStep();
        authenticator.check2FA(codeForStep(step), userAccount);
        // The recorded step is whichever step matched; with a fresh code it is the current one
        // (allowing for a single-step boundary rollover during the test).
        org.junit.Assert.assertNotNull(authenticator.recordedStep);
        org.junit.Assert.assertTrue(authenticator.recordedStep >= step - 1 && authenticator.recordedStep <= step + 1);
    }

    @Test
    public void testReplayOfAlreadyUsedStepRejected() {
        // A step at or before the last recorded one must be rejected (the atomic record-if-newer
        // returns false).
        authenticator.setWindow(1);
        long step = currentStep();
        authenticator.seedLastUsedStep(step + 1);
        assertThrows(CloudTwoFactorAuthenticationException.class,
                () -> authenticator.check2FA(codeForStep(step), userAccount));
    }

    @Test
    public void testNewerStepAfterPreviousUseAccepted() throws CloudTwoFactorAuthenticationException {
        // A code strictly newer than the last used step is accepted.
        authenticator.setWindow(1);
        long step = currentStep();
        authenticator.seedLastUsedStep(step - 5);
        authenticator.check2FA(codeForStep(step), userAccount);
    }

    @Test
    public void testSameCodeCannotBeUsedTwice() throws CloudTwoFactorAuthenticationException {
        // First use succeeds and records the step; an immediate second use of the same code is
        // rejected as replay.
        authenticator.setWindow(1);
        long step = currentStep();
        String code = codeForStep(step);
        authenticator.check2FA(code, userAccount);
        assertThrows(CloudTwoFactorAuthenticationException.class,
                () -> authenticator.check2FA(code, userAccount));
    }
}
