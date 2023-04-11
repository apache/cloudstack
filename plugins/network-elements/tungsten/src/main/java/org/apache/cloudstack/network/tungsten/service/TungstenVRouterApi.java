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

import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnectorFactory;
import org.apache.log4j.Logger;

import java.io.IOException;

public class TungstenVRouterApi {
    private static final Logger s_logger = Logger.getLogger(TungstenVRouterApi.class);

    private TungstenVRouterApi() {
    }

    private static VRouterApiConnector getvRouterApiConnector(String host, String vrouterPort) {
        return VRouterApiConnectorFactory.getInstance(host, vrouterPort);
    }

    public static boolean addTungstenVrouterPort(String host, String vrouterPort, Port port) {
        try {
            return getvRouterApiConnector(host, vrouterPort).addPort(port);
        } catch (IOException ex) {
            s_logger.error("Fail to add vrouter port : " + ex.getMessage());
            return false;
        }
    }

    public static boolean deleteTungstenVrouterPort(String host, String vrouterPort, String portId) {
        return getvRouterApiConnector(host, vrouterPort).deletePort(portId);
    }
}
