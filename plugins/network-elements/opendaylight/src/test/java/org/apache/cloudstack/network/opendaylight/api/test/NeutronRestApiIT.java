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

package org.apache.cloudstack.network.opendaylight.api.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworkWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworksList;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNodeWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNodesList;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPortWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPortsList;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronNetworksNorthboundAction;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronNodesNorthboundAction;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronPortsNorthboundAction;
import org.junit.Test;

public class NeutronRestApiIT {

    @Test
    public void neutronListAllNodes() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://178.237.34.233:8080");

            NeutronNodesNorthboundAction neutron = new NeutronNodesNorthboundAction(url, "admin", "admin");
            NeutronNodesList<NeutronNodeWrapper> results = neutron.listAllNodes();

            Assert.assertNotNull(results);

        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    @Test
    public void neutronListAllNetworks() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://178.237.34.233:8080");

            NeutronNetworksNorthboundAction neutron = new NeutronNetworksNorthboundAction(url, "admin", "admin");
            NeutronNetworksList<NeutronNetworkWrapper> results = neutron.listAllNetworks();

            Assert.assertNotNull(results);

            List<NeutronNetworkWrapper> networks = results.getNetworks();
            Assert.assertNotNull(networks);

        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    @Test
    public void neutronListAllPorts() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://178.237.34.233:8080");

            NeutronPortsNorthboundAction neutron = new NeutronPortsNorthboundAction(url, "admin", "admin");
            NeutronPortsList<NeutronPortWrapper> results = neutron.listAllPorts();

            Assert.assertNotNull(results);

            List<NeutronPortWrapper> networks = results.getPorts();
            Assert.assertNotNull(networks);

        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }
}
