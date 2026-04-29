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
package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

@RunWith(MockitoJUnitRunner.class)
public class CitrixMigrateCommandWrapperTest {

    @Mock
    private CitrixResourceBase citrixResourceBase;
    @Spy
    private CitrixMigrateCommandWrapper citrixMigrateCommandWrapper;

    @Test
    public void destroyNetworkRulesOnSourceHostForMigratedVmTestSupportSecurityGroup() throws BadServerResponse, XenAPIException, XmlRpcException {
        configureExecuteAndVerifyDestroyNetworkRulesOnSourceHostForMigratedVmTest(true, 1);
    }

    @Test
    public void destroyNetworkRulesOnSourceHostForMigratedVmTestDoesNotSupportSecurityGroup() throws BadServerResponse, XenAPIException, XmlRpcException {
        configureExecuteAndVerifyDestroyNetworkRulesOnSourceHostForMigratedVmTest(false, 0);
    }

    private void configureExecuteAndVerifyDestroyNetworkRulesOnSourceHostForMigratedVmTest(boolean canBridgeFirewall, int timesCallHostPlugin)
            throws BadServerResponse, XenAPIException, XmlRpcException {

        final MigrateCommand command = new MigrateCommand("migratedVmName", "destHostIp", false, Mockito.mock(VirtualMachineTO.class), true);
        Connection conn = Mockito.mock(Connection.class);
        Host dsthost = Mockito.mock(Host.class);
        Mockito.doReturn("hostname").when(dsthost).getHostname(conn);

        Mockito.doReturn(canBridgeFirewall).when(citrixResourceBase).canBridgeFirewall();

        citrixMigrateCommandWrapper.destroyMigratedVmNetworkRulesOnSourceHost(command, citrixResourceBase, conn, dsthost);

        Mockito.verify(citrixResourceBase, Mockito.times(timesCallHostPlugin)).callHostPlugin(conn, "vmops", "destroy_network_rules_for_vm", "vmName", "migratedVmName");
    }

}
