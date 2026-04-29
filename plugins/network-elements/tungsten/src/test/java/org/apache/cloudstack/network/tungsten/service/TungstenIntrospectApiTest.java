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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.network.tungsten.vrouter.IntrospectApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.IntrospectApiConnectorFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@RunWith(MockitoJUnitRunner.class)
public class TungstenIntrospectApiTest {
    MockedStatic<IntrospectApiConnectorFactory> introspectApiConnectorFactoryMocked;

    @Before
    public void setup() {
        introspectApiConnectorFactoryMocked = mockStatic(IntrospectApiConnectorFactory.class);
    }

    @After
    public void tearDown() {
        introspectApiConnectorFactoryMocked.close();
    }

    @Test
    public void getLinkLocalIpTest() {
        IntrospectApiConnector introspectApiConnector = mock(IntrospectApiConnector.class);
        Document document = mock(Document.class);
        NodeList nodeList = mock(NodeList.class);
        Node node = mock(Node.class);

        introspectApiConnectorFactoryMocked.when(() -> IntrospectApiConnectorFactory.getInstance(anyString(), anyString())).thenReturn(introspectApiConnector);
        when(introspectApiConnector.getSnhItfReq(anyString())).thenReturn(document);
        when(document.getElementsByTagName(anyString())).thenReturn(nodeList);
        when(nodeList.getLength()).thenReturn(1);
        when(nodeList.item(0)).thenReturn(node);
        assertEquals(node.getTextContent(), TungstenIntrospectApi.getLinkLocalIp("192.168.100.100", "8085", "948f421c-edde-4518-a391-09299cc25dc2"));
    }
}
