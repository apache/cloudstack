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
package org.apache.cloudstack.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.cloud.exception.InvalidParameterValueException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NsxVpnCryptoUtilsTest {

    @Test
    public void testEncryptionAlgorithmMapping() {
        assertEquals(List.of("AES_128"), NsxVpnCryptoUtils.getEncryptionAlgorithms("aes128-sha256;modp2048"));
        assertEquals(List.of("AES_256"), NsxVpnCryptoUtils.getEncryptionAlgorithms("aes256-sha1"));
        assertEquals(List.of("AES_128", "AES_256"), NsxVpnCryptoUtils.getEncryptionAlgorithms("aes128-sha256,aes256-sha512;modp2048"));
    }

    @Test
    public void testDigestAlgorithmMapping() {
        assertEquals(List.of("SHA1"), NsxVpnCryptoUtils.getDigestAlgorithms("aes128-sha1"));
        assertEquals(List.of("SHA2_256"), NsxVpnCryptoUtils.getDigestAlgorithms("aes128-sha256"));
        assertEquals(List.of("SHA2_384"), NsxVpnCryptoUtils.getDigestAlgorithms("aes128-sha384"));
        assertEquals(List.of("SHA2_512"), NsxVpnCryptoUtils.getDigestAlgorithms("aes128-sha512"));
    }

    @Test
    public void testDhGroupMapping() {
        assertEquals(List.of("GROUP2"), NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp1024"));
        assertEquals(List.of("GROUP5"), NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp1536"));
        assertEquals(List.of("GROUP14"), NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp2048"));
        assertEquals(List.of("GROUP15"), NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp3072"));
        assertEquals(List.of("GROUP16"), NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp4096"));
    }

    @Test
    public void testDhGroupsEmptyWhenPolicyHasNoDhPart() {
        assertTrue(NsxVpnCryptoUtils.getDhGroups("aes128-sha256").isEmpty());
    }

    @Test
    public void testIkeMultiProposalPolicyWithDhGroupPerEntry() {
        String policy = "aes128-sha1;modp2048,aes256-sha256;modp2048";
        assertEquals(List.of("AES_128", "AES_256"), NsxVpnCryptoUtils.getEncryptionAlgorithms(policy));
        assertEquals(List.of("SHA1", "SHA2_256"), NsxVpnCryptoUtils.getDigestAlgorithms(policy));
        assertEquals(List.of("GROUP14"), NsxVpnCryptoUtils.getDhGroups(policy));
    }

    @Test
    public void testMultiProposalPolicyCollectsAllDhGroups() {
        String policy = "aes128-sha1;modp2048,aes256-sha256;modp3072";
        assertEquals(List.of("GROUP14", "GROUP15"), NsxVpnCryptoUtils.getDhGroups(policy));
    }

    @Test
    public void testEspPolicyWithoutDhGroup() {
        assertEquals(List.of("AES_128"), NsxVpnCryptoUtils.getEncryptionAlgorithms("aes128-sha256"));
        assertEquals(List.of("SHA2_256"), NsxVpnCryptoUtils.getDigestAlgorithms("aes128-sha256"));
        assertTrue(NsxVpnCryptoUtils.getDhGroups("aes128-sha256").isEmpty());
    }

    @Test
    public void testIkeVersionMapping() {
        assertEquals("IKE_FLEX", NsxVpnCryptoUtils.getIkeVersion("ike"));
        assertEquals("IKE_FLEX", NsxVpnCryptoUtils.getIkeVersion(null));
        assertEquals("IKE_V1", NsxVpnCryptoUtils.getIkeVersion("ikev1"));
        assertEquals("IKE_V2", NsxVpnCryptoUtils.getIkeVersion("ikev2"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejects3des() {
        NsxVpnCryptoUtils.getEncryptionAlgorithms("3des-sha1;modp2048");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsAes192() {
        NsxVpnCryptoUtils.getEncryptionAlgorithms("aes192-sha256;modp2048");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsMd5() {
        NsxVpnCryptoUtils.getDigestAlgorithms("aes128-md5;modp2048");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsModp6144() {
        NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp6144");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsModp8192() {
        NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp8192");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsModp1024s160() {
        NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp1024s160");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsModp2048s224() {
        NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp2048s224");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsModp2048s256() {
        NsxVpnCryptoUtils.getDhGroups("aes128-sha256;modp2048s256");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsCurve25519() {
        NsxVpnCryptoUtils.getDhGroups("aes128-sha256;curve25519");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsUnsupportedIkeVersion() {
        NsxVpnCryptoUtils.getIkeVersion("ikev3");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsIkeLifetimeBelowNsxMinimum() {
        NsxVpnCryptoUtils.validateIkeLifetime(3600L);
    }

    @Test
    public void testAcceptsIkeLifetimeAtNsxMinimum() {
        NsxVpnCryptoUtils.validateIkeLifetime(21600L);
        NsxVpnCryptoUtils.validateIkeLifetime(86400L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsEspLifetimeBelowNsxMinimum() {
        NsxVpnCryptoUtils.validateEspLifetime(300L);
    }

    @Test
    public void testAcceptsDefaultEspLifetime() {
        NsxVpnCryptoUtils.validateEspLifetime(3600L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRejectsPresharedKeyOverNsxMaximumLength() {
        NsxVpnCryptoUtils.validatePresharedKey(StringUtils.repeat('k', 129));
    }

    @Test
    public void testValidateAcceptsSupportedParameters() {
        NsxVpnCryptoUtils.validate("aes256-sha256;modp2048", "aes128-sha1", "ikev2", 86400L, 3600L, "presharedkey");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateRejectsUnsupportedEspPolicy() {
        NsxVpnCryptoUtils.validate("aes256-sha256;modp2048", "3des-md5", "ikev2", 86400L, 3600L, "presharedkey");
    }
}
