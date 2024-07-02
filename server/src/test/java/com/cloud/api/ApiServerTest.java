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
package com.cloud.api;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ApiServer.class)
public class ApiServerTest {

    @InjectMocks
    ApiServer apiServer = new ApiServer();

    private List<ApiServer.ListenerThread> createdListeners;

    private void runTestSetupIntegrationPortListenerInvalidPorts(Integer port) {
        try {
            ApiServer.ListenerThread mocked = Mockito.mock(ApiServer.ListenerThread.class);
            PowerMockito.whenNew(ApiServer.ListenerThread.class).withAnyArguments().thenReturn(mocked);
            apiServer.setupIntegrationPortListener(port);
            Mockito.verify(mocked, Mockito.never()).start();
        } catch (Exception e) {
            Assert.fail(String.format("Exception occurred: %s", e.getMessage()));
        }
    }

    @Test
    public void testSetupIntegrationPortListenerInvalidPorts() {
        List<Integer> ports = new ArrayList<>(List.of(-1, -10, 0));
        ports.add(null);
        for (Integer port : ports) {
            runTestSetupIntegrationPortListenerInvalidPorts(port);
        }
    }

    @Test
    public void testSetupIntegrationPortListenerValidPort() {
        Integer validPort = 8080;
        try {
            ApiServer.ListenerThread mocked = Mockito.mock(ApiServer.ListenerThread.class);
            PowerMockito.whenNew(ApiServer.ListenerThread.class).withAnyArguments().thenReturn(mocked);
            apiServer.setupIntegrationPortListener(validPort);
            PowerMockito.verifyNew(ApiServer.ListenerThread.class).withArguments(apiServer, validPort);
            Mockito.verify(mocked).start();
        } catch (Exception e) {
            Assert.fail(String.format("Exception occurred: %s", e.getMessage()));
        }
    }
}
