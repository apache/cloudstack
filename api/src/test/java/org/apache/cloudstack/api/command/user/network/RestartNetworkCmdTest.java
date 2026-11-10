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
package org.apache.cloudstack.api.command.user.network;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;

@RunWith(MockitoJUnitRunner.class)
public class RestartNetworkCmdTest {

    @Mock
    NetworkService networkService;
    @InjectMocks
    RestartNetworkCmd cmd = new RestartNetworkCmd();

    private ServerApiException executeExpectingFailure() {
        try {
            cmd.execute();
        } catch (ServerApiException e) {
            return e;
        }
        Assert.fail("Expected a ServerApiException");
        return null;
    }

    @Test
    public void executeSetsSuccessResponse() throws Exception {
        Mockito.when(networkService.restartNetwork(cmd)).thenReturn(true);
        cmd.execute();
        Assert.assertTrue(cmd.getResponseObject() instanceof SuccessResponse);
    }

    @Test
    public void executeFailsWithInternalErrorWhenRestartReturnsFalse() throws Exception {
        Mockito.when(networkService.restartNetwork(cmd)).thenReturn(false);
        ServerApiException e = executeExpectingFailure();
        Assert.assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
    }

    @Test
    public void executeMapsResourceUnavailableToResourceUnavailableError() throws Exception {
        ResourceUnavailableException cause = new ResourceUnavailableException("router host is not Up", Network.class, 1L);
        Mockito.when(networkService.restartNetwork(cmd)).thenThrow(cause);
        ServerApiException e = executeExpectingFailure();
        Assert.assertEquals(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, e.getErrorCode());
        Assert.assertEquals(cause.getMessage(), e.getDescription());
    }

    @Test
    public void executeMapsConcurrentOperationToInternalError() throws Exception {
        Mockito.when(networkService.restartNetwork(cmd)).thenThrow(new ConcurrentOperationException("busy"));
        ServerApiException e = executeExpectingFailure();
        Assert.assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
        Assert.assertEquals("busy", e.getDescription());
    }

    @Test
    public void executeMapsInsufficientCapacityToInsufficientCapacityError() throws Exception {
        Mockito.when(networkService.restartNetwork(cmd)).thenThrow(new InsufficientNetworkCapacityException("no capacity", Network.class, 1L));
        ServerApiException e = executeExpectingFailure();
        Assert.assertEquals(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, e.getErrorCode());
        Assert.assertEquals("no capacity", e.getDescription());
    }
}
