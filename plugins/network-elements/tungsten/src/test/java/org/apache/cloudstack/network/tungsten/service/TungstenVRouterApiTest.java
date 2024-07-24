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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnectorFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class TungstenVRouterApiTest {
    MockedStatic<VRouterApiConnectorFactory> vRouterApiConnectorFactoryMocked;

    @Before
    public void setup() {
        vRouterApiConnectorFactoryMocked = Mockito.mockStatic(VRouterApiConnectorFactory.class);
    }

    @After
    public void tearDown() {
        vRouterApiConnectorFactoryMocked.close();
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
