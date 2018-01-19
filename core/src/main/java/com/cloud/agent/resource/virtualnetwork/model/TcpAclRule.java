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

package com.cloud.agent.resource.virtualnetwork.model;

public class TcpAclRule extends AclRule {
    private final String type = "tcp";
    private int firstPort;
    private int lastPort;

    public TcpAclRule() {
        // Empty contructor for (de)serialization
    }

    public TcpAclRule(String cidr, boolean allowed, int firstPort, int lastPort) {
        super(cidr, allowed);
        this.firstPort = firstPort;
        this.lastPort = lastPort;
    }

    public int getFirstPort() {
        return firstPort;
    }

    public void setFirstPort(int firstPort) {
        this.firstPort = firstPort;
    }

    public int getLastPort() {
        return lastPort;
    }

    public void setLastPort(int lastPort) {
        this.lastPort = lastPort;
    }

}
