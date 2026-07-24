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
package com.cloud.user;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TwoFactorAuthenticationBackupCodesTest {

    @Test
    public void testGenerateProducesRequestedCountAndStoresHashes() {
        TwoFactorAuthenticationBackupCodes.GeneratedCodes generated =
                TwoFactorAuthenticationBackupCodes.generate(TwoFactorAuthenticationBackupCodes.DEFAULT_CODE_COUNT);
        List<String> codes = generated.getPlaintextCodes();
        Assert.assertEquals(TwoFactorAuthenticationBackupCodes.DEFAULT_CODE_COUNT, codes.size());
        Assert.assertEquals(TwoFactorAuthenticationBackupCodes.DEFAULT_CODE_COUNT,
                TwoFactorAuthenticationBackupCodes.remainingCount(generated.getStoredValue()));
        // The stored blob must not contain any plaintext code.
        for (String code : codes) {
            Assert.assertFalse(generated.getStoredValue().contains(code));
        }
    }

    @Test
    public void testValidCodeIsConsumedOnce() {
        TwoFactorAuthenticationBackupCodes.GeneratedCodes generated =
                TwoFactorAuthenticationBackupCodes.generate(3);
        String firstCode = generated.getPlaintextCodes().get(0);

        String afterFirstUse = TwoFactorAuthenticationBackupCodes.consume(firstCode, generated.getStoredValue());
        Assert.assertNotNull("valid code should be accepted", afterFirstUse);
        Assert.assertEquals(2, TwoFactorAuthenticationBackupCodes.remainingCount(afterFirstUse));

        // The same code must not work a second time.
        String afterReuse = TwoFactorAuthenticationBackupCodes.consume(firstCode, afterFirstUse);
        Assert.assertNull("consumed code must not be reusable", afterReuse);
    }

    @Test
    public void testInvalidCodeRejected() {
        TwoFactorAuthenticationBackupCodes.GeneratedCodes generated =
                TwoFactorAuthenticationBackupCodes.generate(3);
        Assert.assertNull(TwoFactorAuthenticationBackupCodes.consume("NOTACODE00", generated.getStoredValue()));
    }

    @Test
    public void testCodeAcceptedIgnoringFormatting() {
        TwoFactorAuthenticationBackupCodes.GeneratedCodes generated =
                TwoFactorAuthenticationBackupCodes.generate(1);
        String code = generated.getPlaintextCodes().get(0);
        String formatted = " " + code.substring(0, 5) + "-" + code.substring(5).toLowerCase() + " ";
        Assert.assertNotNull("formatting/case differences should be tolerated",
                TwoFactorAuthenticationBackupCodes.consume(formatted, generated.getStoredValue()));
    }

    @Test
    public void testConsumeAgainstBlankOrNull() {
        Assert.assertNull(TwoFactorAuthenticationBackupCodes.consume("ABCDE23456", null));
        Assert.assertNull(TwoFactorAuthenticationBackupCodes.consume(null, "somehash"));
        Assert.assertEquals(0, TwoFactorAuthenticationBackupCodes.remainingCount(null));
    }
}
