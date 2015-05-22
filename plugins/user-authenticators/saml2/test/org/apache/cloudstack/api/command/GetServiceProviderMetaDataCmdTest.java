/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command;

import com.cloud.utils.HttpUtils;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.cloudstack.utils.auth.SAMLUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

@RunWith(MockitoJUnitRunner.class)
public class GetServiceProviderMetaDataCmdTest {

    @Mock
    ApiServerService apiServer;

    @Mock
    SAML2AuthManager samlAuthManager;

    @Mock
    HttpSession session;

    @Mock
    HttpServletResponse resp;

    @Mock
    HttpServletRequest req;

    @Test
    public void testAuthenticate() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, CertificateParsingException, CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        GetServiceProviderMetaDataCmd cmd = new GetServiceProviderMetaDataCmd();

        Field apiServerField = GetServiceProviderMetaDataCmd.class.getDeclaredField("_apiServer");
        apiServerField.setAccessible(true);
        apiServerField.set(cmd, apiServer);

        Field managerField = GetServiceProviderMetaDataCmd.class.getDeclaredField("_samlAuthManager");
        managerField.setAccessible(true);
        managerField.set(cmd, samlAuthManager);

        String spId = "someSPID";
        String url = "someUrl";
        X509Certificate cert = SAMLUtils.generateRandomX509Certificate(SAMLUtils.generateRandomKeyPair());
        Mockito.when(samlAuthManager.getServiceProviderId()).thenReturn(spId);
        Mockito.when(samlAuthManager.getIdpSigningKey()).thenReturn(cert);
        Mockito.when(samlAuthManager.getIdpSingleLogOutUrl()).thenReturn(url);
        Mockito.when(samlAuthManager.getSpSingleLogOutUrl()).thenReturn(url);

        String result = cmd.authenticate("command", null, session, "random", HttpUtils.RESPONSE_TYPE_JSON, new StringBuilder(), req, resp);
        Assert.assertTrue(result.contains("md:EntityDescriptor"));

        Mockito.verify(samlAuthManager, Mockito.atLeast(1)).getServiceProviderId();
        Mockito.verify(samlAuthManager, Mockito.atLeast(1)).getSpSingleSignOnUrl();
        Mockito.verify(samlAuthManager, Mockito.atLeast(1)).getSpSingleLogOutUrl();
        Mockito.verify(samlAuthManager, Mockito.never()).getIdpSingleSignOnUrl();
        Mockito.verify(samlAuthManager, Mockito.never()).getIdpSingleLogOutUrl();
    }

    @Test
    public void testGetAPIType() {
        Assert.assertTrue(new GetServiceProviderMetaDataCmd().getAPIType() == APIAuthenticationType.LOGIN_API);
    }
}