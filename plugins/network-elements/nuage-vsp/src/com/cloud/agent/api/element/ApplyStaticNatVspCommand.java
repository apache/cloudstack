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

import com.cloud.agent.api.Command;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;

import java.util.List;

public class ApplyStaticNatVspCommand extends Command {

    private final VspNetwork _network;
    private final List<VspStaticNat> _staticNatDetails;

    public ApplyStaticNatVspCommand(VspNetwork network, List<VspStaticNat> staticNatDetails) {
        super();
        this._network = network;
        this._staticNatDetails = staticNatDetails;
    }

    public VspNetwork getNetwork() {
        return _network;
    }

    public List<VspStaticNat> getStaticNatDetails() {
        return _staticNatDetails;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplyStaticNatVspCommand)) return false;
        if (!super.equals(o)) return false;

        ApplyStaticNatVspCommand that = (ApplyStaticNatVspCommand) o;

        if (_network != null ? !_network.equals(that._network) : that._network != null) return false;
        if (_staticNatDetails != null ? !_staticNatDetails.equals(that._staticNatDetails) : that._staticNatDetails != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_network != null ? _network.hashCode() : 0);
        result = 31 * result + (_staticNatDetails != null ? _staticNatDetails.hashCode() : 0);
        return result;
    }
}
