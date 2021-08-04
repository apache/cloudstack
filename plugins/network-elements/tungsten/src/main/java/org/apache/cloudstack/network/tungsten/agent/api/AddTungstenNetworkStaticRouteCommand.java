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

public class AddTungstenNetworkStaticRouteCommand extends TungstenCommand {
    private final String routeTableUuid;
    private final String routePrefix;
    private final String routeNextHop;
    private final String routeNextHopType;
    private final String communities;

    public AddTungstenNetworkStaticRouteCommand(String routeTableUuid, String routePrefix, String routeNextHop,
                                                String routeNextHopType, String communities) {
        this.routeTableUuid = routeTableUuid;
        this.routePrefix = routePrefix;
        this.routeNextHop = routeNextHop;
        this.routeNextHopType = routeNextHopType;
        this.communities = communities;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public String getRoutePrefix() {
        return routePrefix;
    }

    public String getRouteNextHop() {
        return routeNextHop;
    }

    public String getRouteNextHopType() {
        return routeNextHopType;
    }

    public String getCommunities() {
        return communities;
    }
}
