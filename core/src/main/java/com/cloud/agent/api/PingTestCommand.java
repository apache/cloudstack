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

package com.cloud.agent.api;

public class PingTestCommand extends Command {

    String _computingHostIp = null;
    String _routerIp = null;
    String _privateIp = null;

    public PingTestCommand() {
    }

    public PingTestCommand(String computingHostIp) {
        _computingHostIp = computingHostIp;
        setWait(20);
    }

    public PingTestCommand(String routerIp, String privateIp) {
        _routerIp = routerIp;
        _privateIp = privateIp;
        setWait(20);
    }

    public String getComputingHostIp() {
        return _computingHostIp;
    }

    public String getRouterIp() {
        return _routerIp;
    }

    public String getPrivateIp() {
        return _privateIp;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
