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
package org.apache.cloudstack.network.tungsten.model;

public class TungstenLoadBalancerMember {
    private final String name;
    private final String ipAddress;
    private final int port;
    private final int weight;

    public TungstenLoadBalancerMember(final String name, final String ipAddress, final int port, final int weight) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getWeight() {
        return weight;
    }
}
