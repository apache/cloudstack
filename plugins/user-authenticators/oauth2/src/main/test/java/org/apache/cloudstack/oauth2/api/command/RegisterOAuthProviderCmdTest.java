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

package org.apache.cloudstack.oauth2.api.command;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RegisterOAuthProviderCmdTest {

    @Mock
    private OAuth2AuthManager _oauth2mgr;

    @InjectMocks
    private RegisterOAuthProviderCmd _cmd;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecute() throws ServerApiException {
        OauthProviderVO provider = mock(OauthProviderVO.class);
        when(_oauth2mgr.registerOauthProvider(_cmd)).thenReturn(provider);

        _cmd.execute();
        assertEquals(ApiConstants.OAUTH_PROVIDER, ((OauthProviderResponse)_cmd.getResponseObject()).getObjectName());
    }
}