//
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
//

package org.apache.cloudstack.utils.auth;

import junit.framework.TestCase;
import org.junit.Test;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.impl.NameIDBuilder;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SAMLUtilsTest extends TestCase {

    @Test
    public void testSAMLId() throws Exception {
        assertEquals(SAMLUtils.createSAMLId(null), null);
        assertEquals(SAMLUtils.createSAMLId("someUserName"), "SAML-305e19dd2581f33fd90b3949298ec8b17de");

        assertTrue(SAMLUtils.checkSAMLUser(SAMLUtils.createSAMLId("someUserName"), "someUserName"));
        assertFalse(SAMLUtils.checkSAMLUser(SAMLUtils.createSAMLId("someUserName"), "someOtherUserName"));
        assertFalse(SAMLUtils.checkSAMLUser(SAMLUtils.createSAMLId(null), "someOtherUserName"));
        assertFalse(SAMLUtils.checkSAMLUser("randomUID", "randomUID"));
        assertFalse(SAMLUtils.checkSAMLUser(null, null));
    }

    @Test
    public void testGenerateSecureRandomId() throws Exception {
        assertTrue(SAMLUtils.generateSecureRandomId().length() > 0);
    }

    @Test
    public void testBuildAuthnRequestObject() throws Exception {
        String consumerUrl = "http://someurl.com";
        String idpUrl = "http://idp.domain.example";
        String spId = "cloudstack";
        AuthnRequest req = SAMLUtils.buildAuthnRequestObject(spId, idpUrl, consumerUrl);
        assertEquals(req.getAssertionConsumerServiceURL(), consumerUrl);
        assertEquals(req.getDestination(), idpUrl);
        assertEquals(req.getIssuer().getValue(), spId);
    }

    @Test
    public void testBuildLogoutRequest() throws Exception {
        String logoutUrl = "http://logoutUrl";
        String spId = "cloudstack";
        String sessionIndex = "12345";
        String nameIdString = "someNameID";
        NameID sessionNameId = new NameIDBuilder().buildObject();
        sessionNameId.setValue(nameIdString);
        LogoutRequest req = SAMLUtils.buildLogoutRequest(logoutUrl, spId, sessionNameId,  sessionIndex);
        assertEquals(req.getDestination(), logoutUrl);
        assertEquals(req.getIssuer().getValue(), spId);
        assertEquals(req.getNameID().getValue(), nameIdString);
        assertEquals(req.getSessionIndexes().get(0).getSessionIndex(), sessionIndex);
    }

    @Test
    public void testX509Helpers() throws Exception {
        KeyPair keyPair = SAMLUtils.generateRandomKeyPair();

        String privateKeyString = SAMLUtils.savePrivateKey(keyPair.getPrivate());
        String publicKeyString = SAMLUtils.savePublicKey(keyPair.getPublic());

        PrivateKey privateKey = SAMLUtils.loadPrivateKey(privateKeyString);
        PublicKey publicKey = SAMLUtils.loadPublicKey(publicKeyString);

        assertTrue(privateKey.equals(keyPair.getPrivate()));
        assertTrue(publicKey.equals(keyPair.getPublic()));
    }
}