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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteOAuthProviderCmdTest {

    @Mock
    private OAuth2AuthManager _oauthMgr;

    @InjectMocks
    private DeleteOAuthProviderCmd cmd;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteFailure() {
        when(_oauthMgr.deleteOauthProvider(cmd.getId())).thenReturn(false);
        cmd.execute();
    }

    @Test
    public void testExecuteSuccess() {
        when(_oauthMgr.deleteOauthProvider(cmd.getId())).thenReturn(true);
        cmd.execute();
    }

    @Test
    public void testGetApiResourceType() {
        assert (cmd.getApiResourceType() == org.apache.cloudstack.api.ApiCommandResourceType.User);
    }

    @Test
    public void testDeleteOAuthProvider() {
        when(_oauthMgr.deleteOauthProvider(null)).thenReturn(true);
        cmd.execute();

        assertTrue(cmd.getResponseObject() instanceof SuccessResponse);
    }

    @Test(expected = ServerApiException.class)
    public void testDeleteOAuthProviderExpectFailure() {
        when(_oauthMgr.deleteOauthProvider(null)).thenReturn(false);
        cmd.execute();
    }
}
