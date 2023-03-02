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
package org.apache.cloudstack.network.tungsten.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnectorFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VRouterApiConnectorFactory.class)
public class TungstenVRouterApiTest {
    @Before
    public void setup() {
        mockStatic(VRouterApiConnectorFactory.class);
    }

    @Test
    public void addTungstenVrouterPortSuccessTest() throws IOException {
        Port port = mock(Port.class);
        VRouterApiConnector vRouterApiConnector = mock(VRouterApiConnector.class);

        when(VRouterApiConnectorFactory.getInstance(anyString(), anyString())).thenReturn(vRouterApiConnector);
        when(vRouterApiConnector.addPort(any(Port.class))).thenReturn(true);

        assertTrue(TungstenVRouterApi.addTungstenVrouterPort("192.168.100.100", "9091", port));
    }

    @Test
    public void addTungstenVrouterPortFailTest() throws IOException {
        Port port = mock(Port.class);
        VRouterApiConnector vRouterApiConnector = mock(VRouterApiConnector.class);

        when(VRouterApiConnectorFactory.getInstance(anyString(), anyString())).thenReturn(vRouterApiConnector);
        when(vRouterApiConnector.addPort(any(Port.class))).thenThrow(IOException.class);

        assertFalse(TungstenVRouterApi.addTungstenVrouterPort("192.168.100.100", "9091", port));
    }


    @Test
    public void deleteTungstenVrouterPortTest() {
        VRouterApiConnector vRouterApiConnector = mock(VRouterApiConnector.class);

        when(VRouterApiConnectorFactory.getInstance(anyString(), anyString())).thenReturn(vRouterApiConnector);
        when(vRouterApiConnector.deletePort(anyString())).thenReturn(true);

        assertTrue(TungstenVRouterApi.deleteTungstenVrouterPort("192.168.100.100", "9091", "1"));
    }
}
