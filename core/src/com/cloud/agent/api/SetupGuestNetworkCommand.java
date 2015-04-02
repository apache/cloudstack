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

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.NicTO;

public class SetupGuestNetworkCommand extends NetworkElementCommand {
    String dhcpRange;
    String networkDomain;
    String defaultDns1 = null;
    String defaultDns2 = null;
    boolean isRedundant = false;
    boolean add = true;
    NicTO nic;

    public NicTO getNic() {
        return nic;
    }

    public String getDefaultDns1() {
        return defaultDns1;
    }

    public String getDefaultDns2() {
        return defaultDns2;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public boolean isAdd() {
        return add;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    protected SetupGuestNetworkCommand() {
    }

    public SetupGuestNetworkCommand(final String dhcpRange, final String networkDomain, final boolean isRedundant, final String defaultDns1, final String defaultDns2, final boolean add,
            final NicTO nic) {
        this.dhcpRange = dhcpRange;
        this.networkDomain = networkDomain;
        this.defaultDns1 = defaultDns1;
        this.defaultDns2 = defaultDns2;
        this.isRedundant = isRedundant;
        this.add = add;
        this.nic = nic;
    }
}
