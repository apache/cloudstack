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
package org.apache.cloudstack.network.tungsten.vrouter;

import java.util.HashMap;
import java.util.Map;

public class VRouterApiConnectorFactory {
    private static String port = "9091";
    private static Map<String, VRouterApiConnector> vrouterApiConnectors = new HashMap<>();

    public static VRouterApiConnector getInstance(String host) {
        if (vrouterApiConnectors.get(host) == null) {
            vrouterApiConnectors.put(host, new VRouterApiConnectorImpl(host, port));
        }
        return vrouterApiConnectors.get(host);
    }
}
