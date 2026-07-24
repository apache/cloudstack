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
package com.cloud.upgrade.dao;

import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base32;
import org.junit.Assert;
import org.junit.Test;

public class Upgrade42210to42220Test {

    private final Upgrade42210to42220 upgrade = new Upgrade42210to42220();

    @Test
    public void testVersionChain() {
        Assert.assertArrayEquals(new String[] {"4.22.1.0", "4.22.2.0"}, upgrade.getUpgradableVersionRange());
        Assert.assertEquals("4.22.2.0", upgrade.getUpgradedVersion());
    }

    @Test
    public void testFreshlyGeneratedSecretsDetectedAsPlaintext() {
        // A key produced exactly as the TOTP provider produces it (20 random bytes, Base32).
        SecureRandom random = new SecureRandom();
        Base32 base32 = new Base32();
        for (int i = 0; i < 100; i++) {
            byte[] bytes = new byte[20];
            random.nextBytes(bytes);
            String secret = base32.encodeToString(bytes);
            Assert.assertTrue("Base32 secret must be treated as plaintext needing encryption: " + secret,
                    upgrade.isPlaintextSecret(secret));
        }
    }

    @Test
    public void testKnownBase32SecretIsPlaintext() {
        Assert.assertTrue(upgrade.isPlaintextSecret("JBSWY3DPEHPK3PXP"));
        Assert.assertTrue(upgrade.isPlaintextSecret("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"));
    }

    @Test
    public void testEncryptedLookingValueNotPlaintext() {
        // Base64 (encrypted) values contain characters outside the Base32 alphabet
        // (lowercase, '+', '/', digits 0/1/8/9) and must NOT be re-encrypted.
        Assert.assertFalse(upgrade.isPlaintextSecret("dGhpcyBpcyBub3QgYmFzZTMy"));
        Assert.assertFalse(upgrade.isPlaintextSecret("abc+/def=="));
        Assert.assertFalse(upgrade.isPlaintextSecret("V1+encryptedBlob0189"));
    }

    @Test
    public void testNullAndEmptyNotPlaintext() {
        Assert.assertFalse(upgrade.isPlaintextSecret(null));
        Assert.assertFalse(upgrade.isPlaintextSecret(""));
    }
}
