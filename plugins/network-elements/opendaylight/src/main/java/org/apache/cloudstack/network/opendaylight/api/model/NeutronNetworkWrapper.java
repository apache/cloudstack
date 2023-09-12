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

package org.apache.cloudstack.network.opendaylight.api.model;

public class NeutronNetworkWrapper {

    private NeutronNetwork network;

    public NeutronNetworkWrapper() {
    }

    public NeutronNetworkWrapper(final NeutronNetwork network) {
        this.network = network;
    }

    public NeutronNetwork getNetwork() {
        return network;
    }

    public void setNetwork(final NeutronNetwork network) {
        this.network = network;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (network == null ? 0 : network.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NeutronNetworkWrapper other = (NeutronNetworkWrapper) obj;
        if (network == null) {
            if (other.network != null) {
                return false;
            }
        } else if (!network.equals(other.network)) {
            return false;
        }
        return true;
    }
}
