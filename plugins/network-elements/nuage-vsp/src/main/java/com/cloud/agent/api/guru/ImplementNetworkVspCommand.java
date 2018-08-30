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

package com.cloud.agent.api.guru;

import java.util.Objects;

import net.nuage.vsp.acs.client.api.model.VspDhcpDomainOption;
import net.nuage.vsp.acs.client.api.model.VspNetwork;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.cloud.agent.api.Command;

public class ImplementNetworkVspCommand extends Command {

    private final VspNetwork _network;
    private final VspDhcpDomainOption _dhcpOption;
    private final boolean _isVsdManaged;

    public ImplementNetworkVspCommand(VspNetwork network, VspDhcpDomainOption dhcpOption, boolean isVsdManaged) {
        super();
        this._network = network;
        this._dhcpOption = dhcpOption;
        this._isVsdManaged = isVsdManaged;
    }

    public VspNetwork getNetwork() {
        return _network;
    }

    public VspDhcpDomainOption getDhcpOption() {
        return _dhcpOption;
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

        if (!(o instanceof ImplementNetworkVspCommand)) {
            return false;
        }

        ImplementNetworkVspCommand that = (ImplementNetworkVspCommand) o;

        return super.equals(that)
            && Objects.equals(_dhcpOption, that._dhcpOption)
            && Objects.equals(_network, that._network);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(_network)
                .append(_dhcpOption)
                .toHashCode();
    }

    public String toDetailString() {
        return new ToStringBuilder(this)
                .append("network", _network)
                .append("dhcpOption", _dhcpOption)
                .toString();
    }

    public boolean isVsdManaged() {
        return _isVsdManaged;
    }
}
