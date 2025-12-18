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

import com.cloud.agent.api.Command;

import java.util.Objects;

public class SetupTungstenVRouterCommand extends Command {
    private final String oper;
    private final String inf;
    private final String subnet;
    private final String route;
    private final String vrf;

    public SetupTungstenVRouterCommand(final String oper, final String inf, final String subnet, final String route,
        final String vrf) {
        this.oper = oper;
        this.inf = inf;
        this.subnet = subnet;
        this.route = route;
        this.vrf = vrf;
    }

    public String getOper() {
        return oper;
    }

    public String getInf() {
        return inf;
    }

    public String getSubnet() {
        return subnet;
    }

    public String getRoute() {
        return route;
    }

    public String getVrf() {
        return vrf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SetupTungstenVRouterCommand that = (SetupTungstenVRouterCommand) o;
        return Objects.equals(oper, that.oper) && Objects.equals(inf, that.inf) && Objects.equals(subnet, that.subnet) && Objects.equals(route, that.route) && Objects.equals(vrf, that.vrf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), oper, inf, subnet, route, vrf);
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
