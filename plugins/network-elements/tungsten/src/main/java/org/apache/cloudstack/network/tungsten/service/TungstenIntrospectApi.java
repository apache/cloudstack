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

import org.apache.cloudstack.network.tungsten.vrouter.IntrospectApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.IntrospectApiConnectorFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class TungstenIntrospectApi {
    private TungstenIntrospectApi() {
    }

    private static IntrospectApiConnector getIntrospectConnector(String host, String port) {
        return IntrospectApiConnectorFactory.getInstance(host, port);
    }

    public static String getLinkLocalIp(String host, String port, String uuid) {
        Document document = getIntrospectConnector(host, port).getSnhItfReq(uuid);
        NodeList nodeList = document.getElementsByTagName("mdata_ip_addr");
        if (nodeList.getLength() == 1) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
}
