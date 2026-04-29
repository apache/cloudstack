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

package org.apache.cloudstack.network.topology;

import java.util.Hashtable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;

public class NetworkTopologyContext {

    private final Hashtable<NetworkType, NetworkTopology> _flyweight = new Hashtable<DataCenter.NetworkType, NetworkTopology>();;

    @Autowired
    @Qualifier("basicNetworkTopology")
    private BasicNetworkTopology _basicNetworkTopology;

    @Autowired
    @Qualifier("advancedNetworkTopology")
    private AdvancedNetworkTopology _advancedNetworkTopology;

    public void init() {
        _flyweight.put(NetworkType.Basic, _basicNetworkTopology);
        _flyweight.put(NetworkType.Advanced, _advancedNetworkTopology);
    }

    public NetworkTopology retrieveNetworkTopology(final DataCenter dc) {
        if (!_flyweight.containsKey(dc.getNetworkType())) {
            throw new IllegalArgumentException("The given type cannot be related to a NetworkTopology implementation. "
                    + "Please, give a correct type.");
        }
        return _flyweight.get(dc.getNetworkType());
    }

    /**
     * Method used for tests purpose only. Please do not use it to set the AdvanceNetworkTopology and it is managed by Spring.
     * @param advancedNetworkTopology
     */
    public void setAdvancedNetworkTopology(final AdvancedNetworkTopology advancedNetworkTopology) {
        _advancedNetworkTopology = advancedNetworkTopology;
    }

    /**
     *  Method used for tests purpose only. Please do not use it to set the BasicNetworkTopology and it is managed by Spring.
     * @param basicNetworkTopology
     */
    public void setBasicNetworkTopology(final BasicNetworkTopology basicNetworkTopology) {
        _basicNetworkTopology = basicNetworkTopology;
    }
}
