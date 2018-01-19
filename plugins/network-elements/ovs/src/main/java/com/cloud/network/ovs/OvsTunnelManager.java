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
package com.cloud.network.ovs;

import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.utils.component.Manager;

public interface OvsTunnelManager extends Manager {

    boolean isOvsTunnelEnabled();

    /**
     *  create a bridge on the host if not already created for the network and establish full tunnel mesh with
     *  the rest of the hosts on which network spans
     */
    public void checkAndPrepareHostForTunnelNetwork(Network nw, Host host);

    /**
     * remove the bridge and tunnels to the hosts on which network spans if there are no other VM's
     * belonging to the network are running on the host
     */
    public void checkAndRemoveHostFromTunnelNetwork(Network nw, Host host);

}
