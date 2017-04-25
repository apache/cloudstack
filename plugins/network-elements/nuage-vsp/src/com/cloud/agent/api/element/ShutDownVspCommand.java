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

package com.cloud.agent.api.element;

import java.util.Objects;

import net.nuage.vsp.acs.client.api.model.VspDhcpDomainOption;
import net.nuage.vsp.acs.client.api.model.VspNetwork;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.cloud.agent.api.Command;

public class ShutDownVspCommand extends Command {

    private final VspNetwork _network;
    private final VspDhcpDomainOption _dhcpOptions;

    public ShutDownVspCommand(VspNetwork network, VspDhcpDomainOption dhcpOptions) {
        super();
        this._network = network;
        this._dhcpOptions = dhcpOptions;
    }

    public VspNetwork getNetwork() {
        return _network;
    }

    public VspDhcpDomainOption getDhcpOptions() {
        return _dhcpOptions;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShutDownVspCommand)) {
            return false;
        }

        ShutDownVspCommand that = (ShutDownVspCommand) o;

        return super.equals(that)
                && Objects.equals(_dhcpOptions, that._dhcpOptions)
                && Objects.equals(_network, that._network);
    }



    @Override
    public int hashCode() {
        return Objects.hash(_network, _dhcpOptions);
    }

    public String toDetailString() {
        return new ToStringBuilder(this)
                .append("network", _network)
                .append("dhcpOptions", _dhcpOptions)
                .toString();
    }
}
