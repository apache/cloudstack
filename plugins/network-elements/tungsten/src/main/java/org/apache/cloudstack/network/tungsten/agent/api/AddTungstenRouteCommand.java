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
package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.List;

public class AddTungstenRouteCommand extends TungstenCommand {
    private final String inf;
    private final List<String> subnetList;
    private final List<String> routeList;
    private final String vrf;
    private final String host;

    public AddTungstenRouteCommand(final String inf, final List<String> subnetList, final List<String> routeList,
        final String vrf, final String host) {
        this.inf = inf;
        this.subnetList = subnetList;
        this.routeList = routeList;
        this.vrf = vrf;
        this.host = host;
    }

    public String getInf() {
        return inf;
    }

    public List<String> getSubnetList() {
        return subnetList;
    }

    public List<String> getRouteList() {
        return routeList;
    }

    public String getVrf() {
        return vrf;
    }

    public String getHost() {
        return host;
    }
}
