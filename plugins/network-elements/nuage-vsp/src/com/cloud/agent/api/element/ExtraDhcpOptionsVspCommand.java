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

import net.nuage.vsp.acs.client.api.model.VspNetwork;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Map;
import java.util.Objects;

import com.cloud.agent.api.Command;

public class ExtraDhcpOptionsVspCommand extends Command {
    private final VspNetwork network;
    private final String nicUuid;
    private final Map<Integer, String> dhcpOptions;

    public ExtraDhcpOptionsVspCommand (VspNetwork network, String nicUuid, Map<Integer, String> dhcpOptions) {
        super();
        this.network = network;
        this.nicUuid = nicUuid;
        this.dhcpOptions = dhcpOptions;
    }

    public VspNetwork getNetwork() {
        return network;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public Map<Integer, String> getDhcpOptions() {
        return dhcpOptions;
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

        if (!(o instanceof ExtraDhcpOptionsVspCommand)) {
            return false;
        }

        ExtraDhcpOptionsVspCommand that = (ExtraDhcpOptionsVspCommand) o;

        return super.equals(that)
                && Objects.equals(network, that.network)
                && Objects.equals(nicUuid, that.nicUuid)
                && Objects.equals(dhcpOptions, that.dhcpOptions);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(nicUuid)
                .append(network)
                .append(dhcpOptions)
                .toHashCode();
    }
}
