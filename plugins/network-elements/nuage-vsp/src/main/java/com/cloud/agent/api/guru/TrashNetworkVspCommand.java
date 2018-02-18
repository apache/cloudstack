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

import net.nuage.vsp.acs.client.api.model.VspNetwork;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.cloud.agent.api.Command;

public class TrashNetworkVspCommand extends Command {

    private final VspNetwork _network;

    public TrashNetworkVspCommand(VspNetwork network) {
        super();
        this._network = network;
    }

    public VspNetwork getNetwork() {
        return _network;
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

        if (!(o instanceof TrashNetworkVspCommand)) {
            return false;
        }

        TrashNetworkVspCommand that = (TrashNetworkVspCommand) o;

        return super.equals(that)
                && Objects.equals(_network, that._network);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(_network)
                .toHashCode();
    }
}
