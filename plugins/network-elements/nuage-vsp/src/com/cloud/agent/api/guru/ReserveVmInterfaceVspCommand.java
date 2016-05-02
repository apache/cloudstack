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

import com.cloud.agent.api.Command;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;

public class ReserveVmInterfaceVspCommand extends Command {

    private final VspNetwork _network;
    private final VspVm _vm;
    private final VspNic _nic;
    private final VspStaticNat _staticNat;

    public ReserveVmInterfaceVspCommand(VspNetwork network, VspVm vm, VspNic nic, VspStaticNat staticNat) {
        super();
        this._network = network;
        this._vm = vm;
        this._nic = nic;
        this._staticNat = staticNat;
    }

    public VspNetwork getNetwork() {
        return _network;
    }

    public VspVm getVm() {
        return _vm;
    }

    public VspNic getNic() {
        return _nic;
    }

    public VspStaticNat getStaticNat() {
        return _staticNat;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReserveVmInterfaceVspCommand)) return false;
        if (!super.equals(o)) return false;

        ReserveVmInterfaceVspCommand that = (ReserveVmInterfaceVspCommand) o;

        if (_network != null ? !_network.equals(that._network) : that._network != null) return false;
        if (_nic != null ? !_nic.equals(that._nic) : that._nic != null) return false;
        if (_staticNat != null ? !_staticNat.equals(that._staticNat) : that._staticNat != null) return false;
        if (_vm != null ? !_vm.equals(that._vm) : that._vm != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_network != null ? _network.hashCode() : 0);
        result = 31 * result + (_vm != null ? _vm.hashCode() : 0);
        result = 31 * result + (_nic != null ? _nic.hashCode() : 0);
        result = 31 * result + (_staticNat != null ? _staticNat.hashCode() : 0);
        return result;
    }
}
