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
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspVm;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.cloud.agent.api.Command;


public class DeallocateVmVspCommand extends Command {

    private final VspNetwork _network;
    private final VspVm _vm;
    private final VspNic _nic;

    public DeallocateVmVspCommand(VspNetwork network, VspVm vm, VspNic nic) {
        super();
        this._network = network;
        this._vm = vm;
        this._nic = nic;
    }

    public VspNetwork getNetwork() {
        return this._network;
    }

    public VspVm getVm() {
        return this._vm;
    }

    public VspNic getNic() {
        return this._nic;
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

        if (!(o instanceof DeallocateVmVspCommand)) {
            return false;
        }

        DeallocateVmVspCommand that = (DeallocateVmVspCommand) o;

        return super.equals(that)
                && Objects.equals(_network, that._network)
                && Objects.equals(_nic, that._nic)
                && Objects.equals(_vm, that._vm);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(_network)
            .append(_vm)
            .append(_nic)
            .toHashCode();
    }
}
