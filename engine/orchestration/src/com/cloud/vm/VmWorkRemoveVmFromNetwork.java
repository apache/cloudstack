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
package com.cloud.vm;

import java.net.URI;

import com.cloud.network.Network;

public class VmWorkRemoveVmFromNetwork extends VmWork {
    private static final long serialVersionUID = -5070392905642149925L;

    Network network;
    URI broadcastUri;

    public VmWorkRemoveVmFromNetwork(long userId, long accountId, long vmId, String handlerName, Network network, URI broadcastUri) {
        super(userId, accountId, vmId, handlerName);

        this.network = network;
        this.broadcastUri = broadcastUri;
    }

    public Network getNetwork() {
        return network;
    }

    public URI getBroadcastUri() {
        return broadcastUri;
    }
}
