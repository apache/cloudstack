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
import net.juniper.tungsten.api.types.AccessControlList;
import net.juniper.tungsten.api.types.AclEntriesType;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.Loadbalancer;
import net.juniper.tungsten.api.types.LoadbalancerHealthmonitor;
import net.juniper.tungsten.api.types.LoadbalancerListener;
import net.juniper.tungsten.api.types.LoadbalancerMember;
import net.juniper.tungsten.api.types.LoadbalancerPool;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.SecurityGroup;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TungstenApiTest {

    private static final Logger s_logger = Logger.getLogger(TungstenApiTest.class);

    private ApiConnector api;
    private TungstenApi tungstenApi = new TungstenApi();
    private String defaultDomainName = "default-domain";
    private String defaultProjectName = "default-project";
    private String domainUuid;
    private String projectUuid;
    private String tungstenNetworkName = "TungstenNetworkTest";
    private String tungstenNetworkUuid = UUID.randomUUID().toString();
    private String tungstenVmName = "TungstenVmTest";
    private String tungstenVmUuid = UUID.randomUUID().toString();
    private String vmiName = "TungstenVirtualMachineInterfaceTest";
    private String vmiUuid = UUID.randomUUID().toString();
    private String tungstenPublicNetworkName = "TungstenPublicNetworkTest";
    private String tungstenPublicNetworkUuid = UUID.randomUUID().toString();
    private String tungstenSecurityGroupName = "TungstenSecurityGroup";
    private String tungstenSecurityGroupUuid = UUID.randomUUID().toString();
    private String tungstenSecurityGroupRuleUuid = UUID.randomUUID().toString();
    private String tungstenLoadbalancerName = "TungstenLoadbalancer";
    private String tungstenLoadbalancerListenerName = "TungstenLoadbalancerListener";
    private String tungstenLoadbalancerPoolName = "TungstenLoadbalancerPool";

    @Before
    public void setUp() throws Exception {
        s_logger.debug("Create Tungsten-Fabric api connector mock.");
        api = new ApiConnectorMock(null, 0);

        tungstenApi.setApiConnector(api);
        domainUuid = UUID.randomUUID().toString();
        projectUuid = UUID.randomUUID().toString();

        //create Tungsten-Fabric default domain
        s_logger.debug("Create default domain in Tungsten-Fabric.");
        Domain domain = new Domain();
        domain.setUuid(domainUuid);
        domain.setName(defaultDomainName);
        api.create(domain);

        //create Tungsten-Fabric default project
        s_logger.debug("Create default project in Tungsten-Fabric.");
        Project project = new Project();
        project.setUuid(projectUuid);
        project.setName(defaultProjectName);
        project.setParent(domain);
        api.create(project);
    }

    @Test
    public void createTungstenNetworkTest() {
        s_logger.debug("Creating a virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
                projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
                ""));

        s_logger.debug("Get Tungsten-Fabric virtual network and check if it's not null.");
        assertNotNull(tungstenApi.getTungstenObject(VirtualNetwork.class, tungstenNetworkUuid));
    }

    @Test
    public void deleteTungstenNetworkTest() {
        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
                projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
                ""));

        s_logger.debug("Check if virtual network was created in Tungsten-Fabric.");
        VirtualNetwork tungstenVirtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class, tungstenNetworkUuid);
        assertNotNull(tungstenVirtualNetwork);

        s_logger.debug("Delete virtual network from Tungsten-Fabric.");
        assertTrue(tungstenApi.deleteTungstenNetwork(tungstenVirtualNetwork));
    }

    @Test
    public void createTungstenVirtualMachineTest() {
        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        s_logger.debug("Check if virtual machine was created in Tungsten-Fabric.");
        assertNotNull(tungstenApi.getTungstenObject(VirtualMachine.class, tungstenVmUuid));
    }

    @Test
    public void deleteTungstenVirtualMachineTest() {
        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        s_logger.debug("Check if virtual machine was created in Tungsten-Fabric.");
        VirtualMachine tungstenVm = (VirtualMachine) tungstenApi.getTungstenObject(VirtualMachine.class, tungstenVmUuid);
        assertNotNull(tungstenVm);

        s_logger.debug("Delete virtual machine from Tungsten-Fabric.");
        assertTrue(tungstenApi.deleteTungstenVm(tungstenVm));
    }

    @Test
    public void createTungstenVirtualMachineInterfaceTest() {
        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
                projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
                ""));

        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        String vmiMacAddress = "02:fc:f3:d6:83:c3";
        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, vmiMacAddress, tungstenNetworkUuid,
                tungstenVmUuid, projectUuid));
    }

    @Test
    public void deleteTungstenVirtualMachineInterfaceTest() {
        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
                projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
                ""));

        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        String vmiMacAddress = "02:fc:f3:d6:83:c3";

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, vmiMacAddress, tungstenNetworkUuid,
                tungstenVmUuid, projectUuid);

        s_logger.debug("Check if the virtual machine interface was created in Tungsten-Fabric.");
        VirtualMachineInterface vmi = (VirtualMachineInterface) tungstenApi.getTungstenObject(VirtualMachineInterface.class, vmiUuid);
        assertNotNull(vmi);

        s_logger.debug("Delete virtual machine interface from Tungsten-Fabric.");
        assertTrue(tungstenApi.deleteTungstenVmInterface(vmi));
    }

    @Test
    public void createTungstenLogicalRouterTest() {
        s_logger.debug("Create public network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenPublicNetworkUuid, tungstenPublicNetworkName,
                tungstenPublicNetworkName, projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
                "10.0.0.20", false, false, ""));

        s_logger.debug("Create logical router in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenLogicalRouter("TungstenLogicalRouter", projectUuid, tungstenPublicNetworkUuid));
    }

    @Test
    public void deleteTungstenLogicalRouterTest() {
        s_logger.debug("Create public network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenPublicNetworkUuid, tungstenPublicNetworkName,
                tungstenPublicNetworkName, projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
                "10.0.0.20", false, false, ""));

        s_logger.debug("Create logical router in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenLogicalRouter("TungstenLogicalRouter", projectUuid, tungstenPublicNetworkUuid));

        s_logger.debug("Check if logical router was created in Tungsten-Fabric.");
        Project project = (Project) tungstenApi.getTungstenObject(Project.class, projectUuid);
        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.getTungstenObjectByName(LogicalRouter.class,
                project.getQualifiedName(), "TungstenLogicalRouter");
        assertNotNull(logicalRouter);

        s_logger.debug("Delete logical router from Tungsten-Fabric");
        assertTrue(tungstenApi.deleteTungstenLogicalRouter(logicalRouter));
    }

    @Test
    public void createTungstenSecurityGroupTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid,
                tungstenSecurityGroupName, "TungstenSecurityGroupDescription", projectFqn));

        s_logger.debug("Check if the security group was created in Tungsten-Fabric.");
        SecurityGroup securityGroup = (SecurityGroup) tungstenApi.getTungstenObject(SecurityGroup.class, tungstenSecurityGroupUuid);
        assertNotNull(securityGroup);
    }

    @Test
    public void deleteTungstenSecurityGroupTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid,
                tungstenSecurityGroupName, "TungstenSecurityGroupDescription", projectFqn));

        s_logger.debug("Check if the security group was created in Tungsten-Fabric.");
        SecurityGroup securityGroup = (SecurityGroup) tungstenApi.getTungstenObject(SecurityGroup.class, tungstenSecurityGroupUuid);
        assertNotNull(securityGroup);

        s_logger.debug("Delete the security group from Tungsten-Fabric");
        assertTrue(tungstenApi.deleteTungstenSecurityGroup(tungstenSecurityGroupUuid));
    }

    @Test
    public void addTungstenSecurityGroupRuleTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid,
                tungstenSecurityGroupName, "TungstenSecurityGroupDescription", projectFqn));

        //get Tungsten-Fabric security group
        SecurityGroup securityGroup = (SecurityGroup) tungstenApi.getTungstenObject(SecurityGroup.class, tungstenSecurityGroupUuid);
        assertNotNull(securityGroup);

        //add Tungsten-Fabric access control list
        String ingressAclUuid = UUID.randomUUID().toString();
        AccessControlList ingressAcl = new AccessControlList();
        ingressAcl.setUuid(ingressAclUuid);
        ingressAcl.setDisplayName("ingress-access-control-list");
        ingressAcl.setName("ingress-access-control-list");
        ingressAcl.setEntries(new AclEntriesType());
        ingressAcl.setParent(securityGroup);

        String egressAclUuid = UUID.randomUUID().toString();
        AccessControlList egressAcl = new AccessControlList();
        egressAcl.setUuid(egressAclUuid);
        egressAcl.setDisplayName("egress-access-control-list");
        egressAcl.setName("egress-access-control-list");
        egressAcl.setEntries(new AclEntriesType());
        egressAcl.setParent(securityGroup);

        try {
            api.create(ingressAcl);
            api.create(egressAcl);
        } catch (IOException e) {
            assertTrue("Creation of security group acl rules failed", false);
        }

        s_logger.debug("Add a Tungsten-Fabric security group rule to the security group added earlier");
        boolean result = tungstenApi.addTungstenSecurityGroupRule(tungstenSecurityGroupUuid, tungstenSecurityGroupRuleUuid,
                "ingress", 80, 90, "10.0.0.0/24", "10.0.0.0", 24, "tcp");
        assertTrue(result);
    }

    @Test
    public void removeTungstenSecurityGroupRuleTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid,
                "TungstenSecurityGroup", "TungstenSecurityGroupDescription", projectFqn));

        //get Tungsten-Fabric security group
        SecurityGroup securityGroup = (SecurityGroup) tungstenApi.getTungstenObject(SecurityGroup.class, tungstenSecurityGroupUuid);
        assertNotNull(securityGroup);

        //add Tungsten-Fabric access control list
        String ingressAclUuid = UUID.randomUUID().toString();
        AccessControlList ingressAcl = new AccessControlList();
        ingressAcl.setUuid(ingressAclUuid);
        ingressAcl.setDisplayName("ingress-access-control-list");
        ingressAcl.setName("ingress-access-control-list");
        ingressAcl.setEntries(new AclEntriesType());
        ingressAcl.setParent(securityGroup);

        String egressAclUuid = UUID.randomUUID().toString();
        AccessControlList egressAcl = new AccessControlList();
        egressAcl.setUuid(egressAclUuid);
        egressAcl.setDisplayName("egress-access-control-list");
        egressAcl.setName("egress-access-control-list");
        egressAcl.setEntries(new AclEntriesType());
        egressAcl.setParent(securityGroup);

        try {
            api.create(ingressAcl);
            api.create(egressAcl);
        } catch (IOException e) {
            assertTrue("Creation of security group acl rules failed", false);
        }

        s_logger.debug("Add a Tungsten-Fabric security group rule to the security group added earlier");
        boolean result = tungstenApi.addTungstenSecurityGroupRule(tungstenSecurityGroupUuid, tungstenSecurityGroupRuleUuid,
                "ingress", 80, 90, "10.0.0.0/24", "10.0.0.0", 24, "tcp");
        assertTrue(result);

        s_logger.debug("Delete the Tungsten-Fabric security group rule added earlier");
        assertTrue(tungstenApi.removeTungstenSecurityGroupRule(tungstenSecurityGroupUuid,
                tungstenSecurityGroupRuleUuid, "ingress"));
    }

    @Test
    public void createTungstenLoadbalancerTest() {
        s_logger.debug("Creating a virtual network in Tungsten-Fabric.");
        createTungstenNetworkTest();

        s_logger.debug("Get tungsten virtual network and check if it's not null.");
        assertNotNull(tungstenApi.getTungstenObject(VirtualNetwork.class, tungstenNetworkUuid));

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        createTungstenVirtualMachineInterfaceTest();

        s_logger.debug("Create loadbalancer in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenLoadbalancer(projectUuid, tungstenLoadbalancerName,
                vmiUuid, tungstenApi.getSubnetUuid(tungstenNetworkUuid), "192.168.2.100"));

        s_logger.debug("Check if the loadbalancer was created in Tungsten-Fabric");
        Project project = (Project) tungstenApi.getTungstenObject(Project.class, projectUuid);
        assertNotNull(tungstenApi.getTungstenObjectByName(Loadbalancer.class,
                project.getQualifiedName(), tungstenLoadbalancerName));
    }

    @Test
    public void createTungstenLoadbalancerListenerTest() {
        s_logger.debug("Create a loadbalancer in Tungsten-Fabric");
        createTungstenLoadbalancerTest();

        s_logger.debug("Get loadbalancer from Tungsten-Fabric");
        Project project = (Project) tungstenApi.getTungstenObject(Project.class, projectUuid);
        Loadbalancer loadbalancer = (Loadbalancer) tungstenApi.getTungstenObjectByName(Loadbalancer.class,
                project.getQualifiedName(), tungstenLoadbalancerName);
        assertNotNull(loadbalancer);

        s_logger.debug("Create a loadbalancer listener in Tungsten-Fabric");
        LoadbalancerListener loadbalancerListener = (LoadbalancerListener) tungstenApi.createTungstenLoadbalancerListener(
                projectUuid, loadbalancer.getUuid(), tungstenLoadbalancerListenerName, "tcp", 24);

        s_logger.debug("Check if the loadbalancer listener was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(LoadbalancerListener.class, loadbalancerListener.getUuid()));
    }

    @Test
    public void createTungstenLoadbalancerHealthMonitorTest() {
        s_logger.debug("Create a loadbalancer health monitor in Tungsten-Fabric");
        LoadbalancerHealthmonitor loadbalancerHealthmonitor =
                (LoadbalancerHealthmonitor) tungstenApi.createTungstenLoadbalancerHealthMonitor(projectUuid,
                 "LoadbalancerHealthMonitor","PING", 3, 5, 5,
                 null, null, null);
        assertNotNull(loadbalancerHealthmonitor);

        s_logger.debug("Check if the loadbalancer health monitor was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(LoadbalancerHealthmonitor.class, loadbalancerHealthmonitor.getUuid()));
    }

    @Test
    public void createTungstenLoadbalancerPoolTest() {
        s_logger.debug("Create a loadbalancer in Tungsten-Fabric");
        createTungstenLoadbalancerTest();

        s_logger.debug("Get loadbalancer from Tungsten-Fabric");
        Project project = (Project) tungstenApi.getTungstenObject(Project.class, projectUuid);
        Loadbalancer loadbalancer = (Loadbalancer) tungstenApi.getTungstenObjectByName(Loadbalancer.class,
                project.getQualifiedName(), tungstenLoadbalancerName);
        assertNotNull(loadbalancer);

        s_logger.debug("Create a loadbalancer listener in Tungsten-Fabric");
        LoadbalancerListener loadbalancerListener = (LoadbalancerListener) tungstenApi.createTungstenLoadbalancerListener(
                projectUuid, loadbalancer.getUuid(), tungstenLoadbalancerListenerName, "tcp", 24);
        assertNotNull(loadbalancerListener);

        s_logger.debug("Create a loadbalancer health monitor in Tungsten-Fabric");
        LoadbalancerHealthmonitor loadbalancerHealthmonitor =
                (LoadbalancerHealthmonitor) tungstenApi.createTungstenLoadbalancerHealthMonitor(projectUuid,
                        "LoadbalancerHealthMonitor","PING", 3, 5, 5,
                        null, null, null);
        assertNotNull(loadbalancerHealthmonitor);

        s_logger.debug("Create a loadbalancer pool in Tungsten-Fabric");
        LoadbalancerPool loadbalancerPool = (LoadbalancerPool) tungstenApi.createTungstenLoadbalancerPool(projectUuid,
                loadbalancerListener.getUuid(), loadbalancerHealthmonitor.getUuid(), tungstenLoadbalancerPoolName,
                "ROUND_ROBIN", "TCP");
        assertNotNull(loadbalancerPool);

        s_logger.debug("Check if the loadbalancer pool was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(LoadbalancerPool.class, loadbalancerPool.getUuid()));
    }

    @Test
    public void createTungstenLoadbalancerMemberTest() {
        s_logger.debug("Create a loadbalancer pool in Tungsten-Fabric");
        createTungstenLoadbalancerPoolTest();

        s_logger.debug("Get the loadbalancer pool from Tungsten-Fabric");
        Project project = (Project) tungstenApi.getTungstenObject(Project.class, projectUuid);
        LoadbalancerPool loadbalancerPool = (LoadbalancerPool) tungstenApi.getTungstenObjectByName(LoadbalancerPool.class,
                project.getQualifiedName(), tungstenLoadbalancerPoolName);
        assertNotNull(loadbalancerPool);

        s_logger.debug("Create a loadbalancer member in Tungsten-Fabric");
        LoadbalancerMember loadbalancerMember = (LoadbalancerMember) tungstenApi.createTungstenLoadbalancerMember(
                loadbalancerPool.getUuid(), "TungstenLoadbalancerMember", "10.0.0.0",
                null, 24, 5);
        assertNotNull(loadbalancerMember);

        s_logger.debug("Check if the loadbalancer member was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(LoadbalancerMember.class, loadbalancerMember.getUuid()));
    }
}
