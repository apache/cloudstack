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
package org.apache.cloudstack.network.tungsten.service;

import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiConnectorMock;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TungstenApiTest {

    private static final Logger s_logger = Logger.getLogger(TungstenApiTest.class);

    private ApiConnector _api;
    private TungstenApi tungstenApi = new TungstenApi();
    private String defaultDomainName = "default-domain";
    private String defaultProjectName = "default-project";
    private String domainUuid;
    private String projectUuid;

    @Before
    public void setUp() throws Exception {
        s_logger.debug("Create tungsten api connector mock.");
        _api = new ApiConnectorMock(null, 0);

        tungstenApi.setApiConnector(_api);
        domainUuid = UUID.randomUUID().toString();
        projectUuid = UUID.randomUUID().toString();

        //create tungsten default domain
        s_logger.debug("Create default domain in tungsten.");
        Domain domain = new Domain();
        domain.setUuid(domainUuid);
        domain.setName(defaultDomainName);
        _api.create(domain);

        //create tungsten default project
        s_logger.debug("Create default project in tungsten.");
        Project project = new Project();
        project.setUuid(projectUuid);
        project.setName(defaultProjectName);
        project.setParent(domain);
        _api.create(project);

    }

    @Test
    public void createTungstenNetworkTest(){
        //create tungsten network
        String tungstenNetworkUuid = UUID.randomUUID().toString();
        String tungstenNetworkName = "TungstenNetworkTest";

        s_logger.debug("Creating a virtual network in tungsten.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
            projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
            ""));

        // get tungsten network
        s_logger.debug("Get tungsten virtual network and check if it's not null.");
        assertNotNull(tungstenApi.getTungstenObject(VirtualNetwork.class, tungstenNetworkUuid));
    }

    @Test
    public void deleteTungstenNetworkTest(){
        //create tungsten network
        String tungstenNetworkUuid = UUID.randomUUID().toString();
        String tungstenNetworkName = "TungstenNetworkTest";

        s_logger.debug("Create virtual network in tungsten.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
            projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
            ""));

        //get tungsten network
        s_logger.debug("Check if virtual network was created in tungsten.");
        VirtualNetwork tungstenVirtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class, tungstenNetworkUuid);
        assertNotNull(tungstenVirtualNetwork);

        //delete tungsten network
        s_logger.debug("Delete virtual network from tungsten.");
        assertTrue(tungstenApi.deleteTungstenNetwork(tungstenVirtualNetwork));
    }

    @Test
    public void createTungstenVirtualMachineTest(){
        //create tungsten vm
        String tungstenVmName = "TungstenVmTest";
        String tungstenVmUuid = UUID.randomUUID().toString();

        s_logger.debug("Create virtual machine in tungsten.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        //get tungsten vm
        s_logger.debug("Check if virtual machine was created in tungsten.");
        assertNotNull(tungstenApi.getTungstenObject(VirtualMachine.class, tungstenVmUuid));
    }

    @Test
    public void deleteTungstenVirtualMachineTest(){
        //create tungsten vm
        String tungstenVmName = "TungstenVmTest";
        String tungstenVmUuid = UUID.randomUUID().toString();

        s_logger.debug("Create virtual machine in tungsten.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        //get tungsten vm
        s_logger.debug("Check if virtual machine was created in tungsten.");
        VirtualMachine tungstenVm = (VirtualMachine) tungstenApi.getTungstenObject(VirtualMachine.class, tungstenVmUuid);
        assertNotNull(tungstenVm);

        // delete tungsten vm
        s_logger.debug("Delete virtual machine from tungsten.");
        assertTrue(tungstenApi.deleteTungstenVm(tungstenVm));
    }

    @Test
    public void createTungstenVirtualMachineInterfaceTest(){
        //create tungsten network
        String tungstenNetworkUuid = UUID.randomUUID().toString();
        String tungstenNetworkName = "TungstenNetworkTest";

        s_logger.debug("Create virtual network in tungsten.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
            projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
            ""));

        //create tungsten vm
        String tungstenVmName = "TungstenVmTest";
        String tungstenVmUuid = UUID.randomUUID().toString();

        s_logger.debug("Create virtual machine in tungsten.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        String vmiName = "TungstenVirtualMachineInterfaceTest";
        String vmiUuid = UUID.randomUUID().toString();
        String vmiMacAddress = "02:fc:f3:d6:83:c3";

        s_logger.debug("Create virtual machine interface in tungsten.");
        assertNotNull(tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, vmiMacAddress, tungstenNetworkUuid,
                tungstenVmUuid, projectUuid));
    }

    @Test
    public void deleteTungstenVirtualMachineInterfaceTest(){
        //create tungsten network
        String tungstenNetworkUuid = UUID.randomUUID().toString();
        String tungstenNetworkName = "TungstenNetworkTest";

        s_logger.debug("Create virtual network in tungsten.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
            projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
            ""));

        //create tungsten vm
        String tungstenVmName = "TungstenVmTest";
        String tungstenVmUuid = UUID.randomUUID().toString();

        s_logger.debug("Create virtual machine in tungsten.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        String vmiName = "TungstenVirtualMachineInterfaceTest";
        String vmiUuid = UUID.randomUUID().toString();
        String vmiMacAddress = "02:fc:f3:d6:83:c3";

        //create tungsten virtual machine interface
        s_logger.debug("Create virtual machine interface in tungsten.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, vmiMacAddress, tungstenNetworkUuid,
                tungstenVmUuid, projectUuid);

        //check if tungsten vmi was created
        s_logger.debug("Check if the virtual machine interface was created in tungsten.");
        VirtualMachineInterface vmi = (VirtualMachineInterface) tungstenApi.getTungstenObject(VirtualMachineInterface.class, vmiUuid);
        assertNotNull(vmi);

        //delete vmi from tungsten
        s_logger.debug("Delete virtual machine interface from tungsten.");
        assertTrue(tungstenApi.deleteTungstenVmInterface(vmi));
    }

    @Test
    public void createTungstenLogicalRouter(){
        //create public network in tungsten
        String tungstenPublicNetworkUuid = UUID.randomUUID().toString();
        String tungstenPublicNetworkName = "TungstenPublicNetworkTest";

        s_logger.debug("Create public network in tungsten.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenPublicNetworkUuid, tungstenPublicNetworkName,
            tungstenPublicNetworkName, projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
            "10.0.0.20", false, false, ""));

        //create logical router in tungsten
        s_logger.debug("Create logical router in tungsten.");
        assertNotNull(tungstenApi.createTungstenLogicalRouter("TungstenLogicalRouter", projectUuid, tungstenPublicNetworkUuid));
    }

    @Test
    public void deleteTungstenLogicalRouter(){
        //create public network in tungsten
        String tungstenPublicNetworkUuid = UUID.randomUUID().toString();
        String tungstenPublicNetworkName = "TungstenPublicNetworkTest";

        s_logger.debug("Create public network in tungsten.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenPublicNetworkUuid, tungstenPublicNetworkName,
            tungstenPublicNetworkName, projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
            "10.0.0.20", false, false, ""));

        //create logical router in tungsten
        s_logger.debug("Create logical router in tungsten.");
        assertNotNull(tungstenApi.createTungstenLogicalRouter("TungstenLogicalRouter", projectUuid, tungstenPublicNetworkUuid));

        //get logical router from tungsten
        s_logger.debug("Check if logical router was created in tungsten.");
        Project project = (Project) tungstenApi.getTungstenObject(Project.class, projectUuid);
        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.getTungstenObjectByName(LogicalRouter.class,
                project.getQualifiedName(), "TungstenLogicalRouter");
        assertNotNull(logicalRouter);

        //delete logical router from tungsten
        assertTrue(tungstenApi.deleteTungstenLogicalRouter(logicalRouter));
    }
}
