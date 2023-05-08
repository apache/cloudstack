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

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Gateway {
    @SerializedName("interface")
    private String inf;

    @SerializedName("routing-instance")
    private String vrf;

    @SerializedName("subnets")
    private List<Subnet> subnets;

    @SerializedName("routes")
    private List<Subnet> routes;

    public Gateway(final String inf, final String vrf, final List<Subnet> subnets, final List<Subnet> routes) {
        this.inf = inf;
        this.vrf = vrf;
        this.subnets = subnets;
        this.routes = routes;
    }

    public String getInf() {
        return inf;
    }

    public void setInf(final String inf) {
        this.inf = inf;
    }

    public String getVrf() {
        return vrf;
    }

    public void setVrf(final String vrf) {
        this.vrf = vrf;
    }

    public List<Subnet> getSubnets() {
        return subnets;
    }

    public void setSubnets(final List<Subnet> subnets) {
        this.subnets = subnets;
    }

    public List<Subnet> getRoutes() {
        return routes;
    }

    public void setRoutes(final List<Subnet> routes) {
        this.routes = routes;
    }
}
