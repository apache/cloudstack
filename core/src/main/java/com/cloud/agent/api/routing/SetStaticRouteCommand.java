//
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
//

package com.cloud.agent.api.routing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.utils.net.NetUtils;

public class SetStaticRouteCommand extends NetworkElementCommand {
    StaticRouteProfile[] staticRoutes;

    protected SetStaticRouteCommand() {
    }

    public SetStaticRouteCommand(List<StaticRouteProfile> staticRoutes) {
        this.staticRoutes = staticRoutes.toArray(new StaticRouteProfile[staticRoutes.size()]);
    }

    public StaticRouteProfile[] getStaticRoutes() {
        return staticRoutes;
    }

    public String[] generateSRouteRules() {
        Set<String> toAdd = new HashSet<String>();
        for (StaticRouteProfile route : staticRoutes) {
            /*  example  :  ip:gateway:cidr,
             */
            String cidr = route.getCidr();
            String subnet = NetUtils.getCidrSubNet(cidr);
            String cidrSize = cidr.split("\\/")[1];
            String entry;
            if (route.getState() == StaticRoute.State.Active || route.getState() == StaticRoute.State.Add) {
                entry = route.getIp4Address() + ":" + route.getGateway() + ":" + subnet + "/" + cidrSize;
            } else {
                entry = "Revoke:" + route.getGateway() + ":" + subnet + "/" + cidrSize;
            }
            toAdd.add(entry);
        }
        return toAdd.toArray(new String[toAdd.size()]);
    }
}
