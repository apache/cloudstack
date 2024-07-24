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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiConnectorMock;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.ConfigRoot;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.FirewallRule;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.GlobalSystemConfig;
import net.juniper.tungsten.api.types.GlobalVrouterConfig;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.Loadbalancer;
import net.juniper.tungsten.api.types.LoadbalancerHealthmonitor;
import net.juniper.tungsten.api.types.LoadbalancerListener;
import net.juniper.tungsten.api.types.LoadbalancerMember;
import net.juniper.tungsten.api.types.LoadbalancerPool;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.PolicyManagement;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.SecurityGroup;
import net.juniper.tungsten.api.types.ServiceGroup;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.TagType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class TungstenApiTest {

    private static final Logger s_logger = Logger.getLogger(TungstenApiTest.class);

    private final TungstenApi tungstenApi = new TungstenApi();
    private Project project;
    private String projectUuid;
    private final String tungstenNetworkName = "TungstenNetworkTest";
    private final String tungstenNetworkUuid = UUID.randomUUID().toString();
    private final String tungstenVmName = "TungstenVmTest";
    private final String tungstenVmUuid = UUID.randomUUID().toString();
    private final String vmiName = "TungstenVirtualMachineInterfaceTest";
    private final String vmiUuid = UUID.randomUUID().toString();
    private String tungstenPublicNetworkName = "TungstenPublicNetworkTest";
    private String tungstenPublicNetworkUuid = UUID.randomUUID().toString();
    private String tungstenSecurityGroupName = "TungstenSecurityGroup";
    private String tungstenSecurityGroupUuid = UUID.randomUUID().toString();
    private String tungstenSecurityGroupRuleUuid = UUID.randomUUID().toString();
    private String tungstenLoadbalancerName = "TungstenLoadbalancer";
    private String tungstenLoadbalancerListenerName = "TungstenLoadbalancerListener";
    private String tungstenLoadbalancerPoolName = "TungstenLoadbalancerPool";
    private final Comparator<ApiObjectBase> comparator = Comparator.comparing(ApiObjectBase::getUuid);

    @Before
    public void setUp() throws Exception {
        s_logger.debug("Create Tungsten-Fabric api connector mock.");
        ApiConnector api = new ApiConnectorMock(null, 0);

        tungstenApi.setApiConnector(api);
        String domainUuid = UUID.randomUUID().toString();
        projectUuid = UUID.randomUUID().toString();

        //create Tungsten-Fabric default domain
        s_logger.debug("Create default domain in Tungsten-Fabric.");
        Domain domain = new Domain();
        domain.setUuid(domainUuid);
        String defaultDomainName = "default-domain";
        domain.setName(defaultDomainName);
        api.create(domain);

        //create Tungsten-Fabric default project
        s_logger.debug("Create default project in Tungsten-Fabric.");
        Project project = new Project();
        project.setUuid(projectUuid);
        String defaultProjectName = "default-project";
        project.setName(defaultProjectName);
        project.setParent(domain);
        api.create(project);

        this.project = (Project) api.findById(Project.class, projectUuid);

        // create Tungsten-Fabric default policy management
        PolicyManagement policyManagement = new PolicyManagement();
        policyManagement.setParent(new ConfigRoot());
        policyManagement.setName(TungstenApi.TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
        api.create(policyManagement);

        // create Tungsten-Fabric global system config
        GlobalSystemConfig globalSystemConfig = new GlobalSystemConfig();
        globalSystemConfig.setName(TungstenApi.TUNGSTEN_GLOBAL_SYSTEM_CONFIG);
        globalSystemConfig.setParent(new ConfigRoot());
        api.create(globalSystemConfig);

        // create Tungsten-Fabric global vrouter config
        GlobalVrouterConfig globalVrouterConfig = new GlobalVrouterConfig();
        globalVrouterConfig.setName(TungstenApi.TUNGSTEN_GLOBAL_VROUTER_CONFIG);
        globalVrouterConfig.setParent(globalSystemConfig);
        api.create(globalVrouterConfig);
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
    public void createTungstenVirtualMachineTest() {
        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        s_logger.debug("Check if virtual machine was created in Tungsten-Fabric.");
        assertNotNull(tungstenApi.getTungstenObject(VirtualMachine.class, tungstenVmUuid));
    }

    @Test
    public void createTungstenVirtualMachineInterfaceTest() {
        s_logger.debug("Create fabric virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(null, "ip-fabric", "ip-fabric",
            projectUuid, true, false, null, 0, null, true, null, null, null, false, false,
            ""));

        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
            projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, true,
            ""));

        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        String vmiMacAddress = "02:fc:f3:d6:83:c3";
        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, vmiMacAddress, tungstenNetworkUuid,
                tungstenVmUuid, projectUuid, "10.0.0.1", true));
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
                tungstenVmUuid, projectUuid, "10.0.0.1", true);

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
        assertNotNull(
            tungstenApi.createTungstenLogicalRouter("TungstenLogicalRouter", projectUuid, tungstenPublicNetworkUuid));
    }

    @Test
    public void createTungstenSecurityGroupTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid, tungstenSecurityGroupName,
            "TungstenSecurityGroupDescription", projectFqn));

        s_logger.debug("Check if the security group was created in Tungsten-Fabric.");
        SecurityGroup securityGroup = (SecurityGroup) tungstenApi.getTungstenObject(SecurityGroup.class,
            tungstenSecurityGroupUuid);
        assertNotNull(securityGroup);
    }

    @Test
    public void addTungstenSecurityGroupRuleTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid, tungstenSecurityGroupName,
            "TungstenSecurityGroupDescription", projectFqn));

        //get Tungsten-Fabric security group
        SecurityGroup securityGroup = (SecurityGroup) tungstenApi.getTungstenObject(SecurityGroup.class,
            tungstenSecurityGroupUuid);
        assertNotNull(securityGroup);

        s_logger.debug("Add a Tungsten-Fabric security group rule to the security group added earlier");
        boolean result = tungstenApi.addTungstenSecurityGroupRule(tungstenSecurityGroupUuid,
            tungstenSecurityGroupRuleUuid, "ingress", 80, 90, "10.0.0.0/24", "IPv4", "tcp");
        assertTrue(result);
    }

    @Test
    public void removeTungstenSecurityGroupRuleTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid, "TungstenSecurityGroup",
            "TungstenSecurityGroupDescription", projectFqn));

        //get Tungsten-Fabric security group
        SecurityGroup securityGroup = (SecurityGroup) tungstenApi.getTungstenObject(SecurityGroup.class,
            tungstenSecurityGroupUuid);
        assertNotNull(securityGroup);

        s_logger.debug("Add a Tungsten-Fabric security group rule to the security group added earlier");
        boolean result1 = tungstenApi.addTungstenSecurityGroupRule(tungstenSecurityGroupUuid,
            "0a01e4c7-d912-4bd5-9786-5478e3dae7b2", "ingress", 80, 90, "10.0.0.0/24", "IPv4", "tcp");
        assertTrue(result1);

        s_logger.debug("Add a Tungsten-Fabric security group rule to the security group added earlier");
        boolean result2 = tungstenApi.addTungstenSecurityGroupRule(tungstenSecurityGroupUuid,
            "fe44b353-21e7-4e6c-af18-1325c5ef886a", "egress", 80, 90, "securitygroup", "IPv4", "tcp");
        assertTrue(result2);

        s_logger.debug("Delete the Tungsten-Fabric security group rule added earlier");
        assertTrue(
            tungstenApi.removeTungstenSecurityGroupRule(tungstenSecurityGroupUuid, "0a01e4c7-d912-4bd5-9786-5478e3dae7b2"));
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
        assertNotNull(tungstenApi.createTungstenLoadbalancer(projectUuid, tungstenLoadbalancerName, vmiUuid,
            tungstenApi.getSubnetUuid(tungstenNetworkUuid), "192.168.2.100"));

        s_logger.debug("Check if the loadbalancer was created in Tungsten-Fabric");
        Project project = (Project) tungstenApi.getTungstenObject(Project.class, projectUuid);
        assertNotNull(tungstenApi.getTungstenObjectByName(Loadbalancer.class, project.getQualifiedName(),
            tungstenLoadbalancerName));
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
        LoadbalancerListener loadbalancerListener =
            (LoadbalancerListener) tungstenApi.createTungstenLoadbalancerListener(
            projectUuid, loadbalancer.getUuid(), tungstenLoadbalancerListenerName, "tcp", 24);

        s_logger.debug("Check if the loadbalancer listener was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(LoadbalancerListener.class, loadbalancerListener.getUuid()));
    }

    @Test
    public void createTungstenLoadbalancerHealthMonitorTest() {
        s_logger.debug("Create a loadbalancer health monitor in Tungsten-Fabric");
        LoadbalancerHealthmonitor loadbalancerHealthmonitor =
            (LoadbalancerHealthmonitor) tungstenApi.createTungstenLoadbalancerHealthMonitor(
            projectUuid, "LoadbalancerHealthMonitor", "PING", 3, 5, 5, null, null, null);
        assertNotNull(loadbalancerHealthmonitor);

        s_logger.debug("Check if the loadbalancer health monitor was created in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.getTungstenObject(LoadbalancerHealthmonitor.class, loadbalancerHealthmonitor.getUuid()));
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
        LoadbalancerListener loadbalancerListener =
            (LoadbalancerListener) tungstenApi.createTungstenLoadbalancerListener(
            projectUuid, loadbalancer.getUuid(), tungstenLoadbalancerListenerName, "tcp", 24);
        assertNotNull(loadbalancerListener);

        s_logger.debug("Create a loadbalancer health monitor in Tungsten-Fabric");
        LoadbalancerHealthmonitor loadbalancerHealthmonitor =
            (LoadbalancerHealthmonitor) tungstenApi.createTungstenLoadbalancerHealthMonitor(
            projectUuid, "LoadbalancerHealthMonitor", "PING", 3, 5, 5, null, null, null);
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
        LoadbalancerPool loadbalancerPool = (LoadbalancerPool) tungstenApi.getTungstenObjectByName(
            LoadbalancerPool.class, project.getQualifiedName(), tungstenLoadbalancerPoolName);
        assertNotNull(loadbalancerPool);

        s_logger.debug("Create a loadbalancer member in Tungsten-Fabric");
        LoadbalancerMember loadbalancerMember = (LoadbalancerMember) tungstenApi.createTungstenLoadbalancerMember(
            loadbalancerPool.getUuid(), "TungstenLoadbalancerMember", "10.0.0.0", null, 24, 5);
        assertNotNull(loadbalancerMember);

        s_logger.debug("Check if the loadbalancer member was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(LoadbalancerMember.class, loadbalancerMember.getUuid()));
    }

    @Test
    public void createTungstenInstanceIpTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create a virtual machine in Tungsten-Fabric.");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if the instance ip is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObjectByName(InstanceIp.class, null, "TungstenInstanceIp"));

        s_logger.debug("Create instance ip in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.createTungstenInstanceIp("TungstenInstanceIp", "192.168.1.100", tungstenNetworkUuid,
                vmiUuid));

        s_logger.debug("Check if the instance ip was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObjectByName(InstanceIp.class, null, "TungstenInstanceIp"));
    }

    @Test
    public void createTungstenInstanceIpWithSubnetTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create a virtual machine in Tungsten-Fabric.");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if the instance ip is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObjectByName(InstanceIp.class, null, "TungstenInstanceIp"));

        s_logger.debug("Create instance ip in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.createTungstenInstanceIp("TungstenInstanceIp", "192.168.1.100", tungstenNetworkUuid,
                vmiUuid, tungstenApi.getSubnetUuid(tungstenNetworkUuid)));

        s_logger.debug("Check if the instance ip was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObjectByName(InstanceIp.class, null, "TungstenInstanceIp"));
    }

    @Test
    public void createTungstenFloatingIpPoolTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        VirtualNetwork virtualNetwork = tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName,
            tungstenNetworkName, projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
            "10.0.0.20", false, false, "");

        s_logger.debug("Check if the floating ip pool is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObjectByName(FloatingIpPool.class, virtualNetwork.getQualifiedName(),
            "TungstenFip"));

        s_logger.debug("Create instance ip in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenFloatingIpPool(tungstenNetworkUuid, "TungstenFip"));

        s_logger.debug("Check if the instance ip was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObjectByName(FloatingIpPool.class, virtualNetwork.getQualifiedName(),
            "TungstenFip"));
    }

    @Test
    public void createTungstenLbVmiTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Check if the lb vmi is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObjectByName(VirtualMachineInterface.class, project.getQualifiedName(),
            "TungstenLbVmi"));

        s_logger.debug("Create lb vmi in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenLbVmi("TungstenLbVmi", projectUuid, tungstenNetworkUuid));

        s_logger.debug("Check if the lb vmi was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObjectByName(VirtualMachineInterface.class, project.getQualifiedName(),
            "TungstenLbVmi"));
    }

    @Test
    public void updateTungstenObjectTest() {
        s_logger.debug("Create public network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenPublicNetworkName, tungstenPublicNetworkName,
            projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
            "");

        s_logger.debug("Creating a logical router in Tungsten-Fabric.");
        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.createTungstenLogicalRouter("TungstenLogicalRouter",
            projectUuid, tungstenNetworkUuid);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) tungstenApi.createTungstenGatewayVmi(
            vmiName, projectUuid, tungstenNetworkUuid);

        s_logger.debug("Check if the logical router vmi is not exist in Tungsten-Fabric");
        assertNull(logicalRouter.getVirtualMachineInterface());

        s_logger.debug("Update logical router with vmi");
        logicalRouter.setVirtualMachineInterface(virtualMachineInterface);
        tungstenApi.updateTungstenObject(logicalRouter);

        s_logger.debug("Check updated logical router have vmi uuid equals created vmi uuid");
        LogicalRouter updatedlogicalRouter = (LogicalRouter) tungstenApi.getTungstenObjectByName(LogicalRouter.class,
            project.getQualifiedName(), "TungstenLogicalRouter");
        assertEquals(virtualMachineInterface.getUuid(),
            updatedlogicalRouter.getVirtualMachineInterface().get(0).getUuid());
    }

    @Test
    public void createTungstenFloatingIpTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create instance ip in Tungsten-Fabric");
        FloatingIpPool floatingIpPool = (FloatingIpPool) tungstenApi.createTungstenFloatingIpPool(tungstenNetworkUuid,
            "TungstenFip");

        s_logger.debug("Check if the floating ip pool is not exist in Tungsten-Fabric");
        assertNull(
            tungstenApi.getTungstenObjectByName(FloatingIp.class, floatingIpPool.getQualifiedName(), "TungstenFi"));

        s_logger.debug("Create floating ip in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.createTungstenFloatingIp(projectUuid, tungstenNetworkUuid, "TungstenFip", "TungstenFi",
                "192.168.1.100"));

        s_logger.debug("Check if the lb vmi was created in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.getTungstenObjectByName(FloatingIp.class, floatingIpPool.getQualifiedName(), "TungstenFi"));
    }

    @Test
    public void assignTungstenFloatingIpTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create instance ip in Tungsten-Fabric");
        tungstenApi.createTungstenFloatingIpPool(tungstenNetworkUuid, "TungstenFip");

        s_logger.debug("Create floating ip in Tungsten-Fabric");
        tungstenApi.createTungstenFloatingIp(projectUuid, tungstenNetworkUuid, "TungstenFip", "TungstenFi",
            "192.168.1.100");

        s_logger.debug("Create vm in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if the floating ip was assigned in Tungsten-Fabric");
        Assert.assertTrue(
            tungstenApi.assignTungstenFloatingIp(tungstenNetworkUuid, vmiUuid, "TungstenFip", "TungstenFi",
                "192.168.1.100"));
    }

    @Test
    public void releaseTungstenFloatingIpTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create instance ip in Tungsten-Fabric");
        tungstenApi.createTungstenFloatingIpPool(tungstenNetworkUuid, "TungstenFip");

        s_logger.debug("Create floating ip in Tungsten-Fabric");
        tungstenApi.createTungstenFloatingIp(projectUuid, tungstenNetworkUuid, "TungstenFip", "TungstenFi",
            "192.168.1.100");

        s_logger.debug("Create vm in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if the floating ip was assigned in Tungsten-Fabric");
        tungstenApi.assignTungstenFloatingIp(tungstenNetworkUuid, vmiUuid, "TungstenFip", "TungstenFi",
            "192.168.1.100");

        s_logger.debug("Check if the floating ip was assigned in Tungsten-Fabric");
        Assert.assertTrue(tungstenApi.releaseTungstenFloatingIp(tungstenNetworkUuid, "TungstenFip", "TungstenFi"));
    }

    @Test
    public void createTungstenNetworkPolicyTest() {
        s_logger.debug("Prepare network policy rule 1");
        List<TungstenRule> tungstenRuleList1 = new ArrayList<>();
        TungstenRule tungstenRule1 = new TungstenRule("005f0dea-0196-11ec-a1ed-b42e99f6e187", "pass", ">", "tcp", null,
            "192.168.100.0", 24, 80, 80, null, "192.168.200.0", 24, 80, 80);
        tungstenRuleList1.add(tungstenRule1);

        s_logger.debug("Create a network policy in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createOrUpdateTungstenNetworkPolicy("policy1", projectUuid, tungstenRuleList1));

        s_logger.debug("Get created network policy and check if network policy rule has created");
        NetworkPolicy networkPolicy = (NetworkPolicy) tungstenApi.getTungstenObjectByName(NetworkPolicy.class,
            project.getQualifiedName(), "policy1");
        assertEquals("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            networkPolicy.getEntries().getPolicyRule().get(0).getRuleUuid());

        s_logger.debug("Prepare network policy rule 2");
        List<TungstenRule> tungstenRuleList2 = new ArrayList<>();
        TungstenRule tungstenRule2 = new TungstenRule("105f0dea-0196-11ec-a1ed-b42e99f6e187", "pass", ">", "tcp", null,
            "192.168.100.0", 24, 80, 80, null, "192.168.200.0", 24, 80, 80);
        tungstenRuleList2.add(tungstenRule2);

        s_logger.debug("update created network policy in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createOrUpdateTungstenNetworkPolicy("policy1", projectUuid, tungstenRuleList2));

        s_logger.debug("Get updated network policy and check if network policy rule has updated");
        NetworkPolicy networkPolicy1 = (NetworkPolicy) tungstenApi.getTungstenObjectByName(NetworkPolicy.class,
            project.getQualifiedName(), "policy1");
        assertEquals("105f0dea-0196-11ec-a1ed-b42e99f6e187",
            networkPolicy1.getEntries().getPolicyRule().get(1).getRuleUuid());
    }

    @Test
    public void applyTungstenNetworkPolicy() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Prepare network policy rule");
        List<TungstenRule> tungstenRuleList = new ArrayList<>();

        s_logger.debug("Create a network policy in Tungsten-Fabric.");
        NetworkPolicy networkPolicy = (NetworkPolicy) tungstenApi.createOrUpdateTungstenNetworkPolicy("policy",
            projectUuid, tungstenRuleList);

        s_logger.debug("Check if network policy was not applied in Tungsten-Fabric.");
        VirtualNetwork virtualNetwork1 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertNull(virtualNetwork1.getNetworkPolicy());

        s_logger.debug("Apply network policy to network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.applyTungstenNetworkPolicy(networkPolicy.getUuid(), tungstenNetworkUuid, 1, 1));

        s_logger.debug("Check if network policy was applied in Tungsten-Fabric.");
        VirtualNetwork virtualNetwork2 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertNotNull(virtualNetwork2.getNetworkPolicy());
    }

    @Test
    public void getTungstenFabricNetworkTest() {
        s_logger.debug("Create fabric virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(null, "ip-fabric", "ip-fabric",
            projectUuid, true, false, null, 0, null, true, null, null, null, false, false,
            ""));

        s_logger.debug("Check if fabric network was got in Tungsten-Fabric.");
        assertNotNull(tungstenApi.getTungstenFabricNetwork());
    }

    @Test
    public void createTungstenDomainTest() {
        s_logger.debug("Check if domain was created in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenDomain("domain", "0a01e4c7-d912-4bd5-9786-5478e3dae7b2"));
    }

    @Test
    public void createTungstenProjectTest() {
        s_logger.debug("Check if project was created in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenProject("project","fe44b353-21e7-4e6c-af18-1325c5ef886a","0a01e4c7-d912-4bd5-9786-5478e3dae7b2", "domain"));
    }

    @Test
    public void deleteTungstenDomainTest() {
        s_logger.debug("Create domain in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenDomain("domain", "0a01e4c7-d912-4bd5-9786-5478e3dae7b2"));

        s_logger.debug("Check if domain was deleted in Tungsten-Fabric.");
        assertTrue(tungstenApi.deleteTungstenDomain("0a01e4c7-d912-4bd5-9786-5478e3dae7b2"));
    }

    @Test
    public void deleteTungstenProjectTest() {
        s_logger.debug("Create project in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenProject("project","fe44b353-21e7-4e6c-af18-1325c5ef886a","0a01e4c7-d912-4bd5-9786-5478e3dae7b2", "domain"));

        s_logger.debug("Check if project was deleted in Tungsten-Fabric.");
        assertTrue(tungstenApi.deleteTungstenProject("fe44b353-21e7-4e6c-af18-1325c5ef886a"));
    }

    @Test
    public void getDefaultTungstenDomainTest() throws IOException {
        s_logger.debug("Check if default domain was got in Tungsten-Fabric.");
        assertNotNull(tungstenApi.getDefaultTungstenDomain());
    }

    @Test
    public void updateLoadBalancerMemberTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create a vm in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Create loadbalancer in Tungsten-Fabric");
        Loadbalancer loadbalancer = (Loadbalancer) tungstenApi.createTungstenLoadbalancer(projectUuid,
            tungstenLoadbalancerName, vmiUuid, tungstenApi.getSubnetUuid(tungstenNetworkUuid), "192.168.2.100");

        s_logger.debug("Create a loadbalancer listener in Tungsten-Fabric");
        LoadbalancerListener loadbalancerListener =
            (LoadbalancerListener) tungstenApi.createTungstenLoadbalancerListener(
            projectUuid, loadbalancer.getUuid(), tungstenLoadbalancerListenerName, "tcp", 24);

        s_logger.debug("Create a loadbalancer health monitor in Tungsten-Fabric");
        LoadbalancerHealthmonitor loadbalancerHealthmonitor =
            (LoadbalancerHealthmonitor) tungstenApi.createTungstenLoadbalancerHealthMonitor(
            projectUuid, "LoadbalancerHealthMonitor", "PING", 3, 5, 5, null, null, null);

        s_logger.debug("Create a loadbalancer pool in Tungsten-Fabric");
        LoadbalancerPool loadbalancerPool = (LoadbalancerPool) tungstenApi.createTungstenLoadbalancerPool(projectUuid,
            loadbalancerListener.getUuid(), loadbalancerHealthmonitor.getUuid(), tungstenLoadbalancerPoolName,
            "ROUND_ROBIN", "TCP");

        s_logger.debug("Update loadbalancer member 1 in Tungsten-Fabric");
        List<TungstenLoadBalancerMember> tungstenLoadBalancerMemberList1 = new ArrayList<>();
        tungstenLoadBalancerMemberList1.add(new TungstenLoadBalancerMember("member1", "192.168.100.100", 80, 1));
        assertTrue(tungstenApi.updateLoadBalancerMember(projectUuid, tungstenLoadbalancerPoolName,
            tungstenLoadBalancerMemberList1, tungstenApi.getSubnetUuid(tungstenNetworkUuid)));

        s_logger.debug("Check if loadbalancer member 2 was updated in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObjectByName(LoadbalancerMember.class, loadbalancerPool.getQualifiedName(),
            "member1"));

        s_logger.debug("Update loadbalancer member 2 in Tungsten-Fabric");
        List<TungstenLoadBalancerMember> tungstenLoadBalancerMemberList2 = new ArrayList<>();
        tungstenLoadBalancerMemberList2.add(new TungstenLoadBalancerMember("member2", "192.168.100.100", 80, 1));
        assertTrue(tungstenApi.updateLoadBalancerMember(projectUuid, tungstenLoadbalancerPoolName,
            tungstenLoadBalancerMemberList2, tungstenApi.getSubnetUuid(tungstenNetworkUuid)));

        s_logger.debug("Check if loadbalancer member 1 was deleted in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObjectByName(LoadbalancerMember.class, loadbalancerPool.getQualifiedName(),
            "member1"));

        s_logger.debug("Check if loadbalancer member 2 was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObjectByName(LoadbalancerMember.class, loadbalancerPool.getQualifiedName(),
            "member2"));
    }

    @Test
    public void updateLoadBalancerPoolTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create floating ip in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Create loadbalancer in Tungsten-Fabric");
        Loadbalancer loadbalancer = (Loadbalancer) tungstenApi.createTungstenLoadbalancer(projectUuid,
            tungstenLoadbalancerName, vmiUuid, tungstenApi.getSubnetUuid(tungstenNetworkUuid), "192.168.2.100");

        s_logger.debug("Create a loadbalancer listener in Tungsten-Fabric");
        LoadbalancerListener loadbalancerListener =
            (LoadbalancerListener) tungstenApi.createTungstenLoadbalancerListener(
            projectUuid, loadbalancer.getUuid(), tungstenLoadbalancerListenerName, "tcp", 24);

        s_logger.debug("Create a loadbalancer health monitor in Tungsten-Fabric");
        LoadbalancerHealthmonitor loadbalancerHealthmonitor =
            (LoadbalancerHealthmonitor) tungstenApi.createTungstenLoadbalancerHealthMonitor(
            projectUuid, "LoadbalancerHealthMonitor", "PING", 3, 5, 5, null, null, null);

        s_logger.debug("Create a loadbalancer pool in Tungsten-Fabric");
        tungstenApi.createTungstenLoadbalancerPool(projectUuid, loadbalancerListener.getUuid(),
            loadbalancerHealthmonitor.getUuid(), tungstenLoadbalancerPoolName, "ROUND_ROBIN", "TCP");

        s_logger.debug("Update loadbalancer pool in Tungsten-Fabric");
        assertTrue(
            tungstenApi.updateLoadBalancerPool(projectUuid, tungstenLoadbalancerPoolName, "SOURCE_IP", "APP_COOKIE",
                "cookie", "UDP", true, "80", "/stats", "admin:abc"));

        s_logger.debug("Check if loadbalancer pool was updated in Tungsten-Fabric");
        LoadbalancerPool loadbalancerPool = (LoadbalancerPool) tungstenApi.getTungstenObjectByName(
            LoadbalancerPool.class, project.getQualifiedName(), tungstenLoadbalancerPoolName);
        assertEquals("SOURCE_IP", loadbalancerPool.getProperties().getLoadbalancerMethod());
        assertEquals("APP_COOKIE", loadbalancerPool.getProperties().getSessionPersistence());
        assertEquals("cookie", loadbalancerPool.getProperties().getPersistenceCookieName());
        assertEquals("UDP", loadbalancerPool.getProperties().getProtocol());
    }

    @Test
    public void updateLoadBalancerListenerTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create floating ip in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Create loadbalancer in Tungsten-Fabric");
        Loadbalancer loadbalancer = (Loadbalancer) tungstenApi.createTungstenLoadbalancer(projectUuid,
            tungstenLoadbalancerName, vmiUuid, tungstenApi.getSubnetUuid(tungstenNetworkUuid), "192.168.2.100");

        s_logger.debug("Create a loadbalancer listener in Tungsten-Fabric");
        tungstenApi.createTungstenLoadbalancerListener(projectUuid, loadbalancer.getUuid(),
            tungstenLoadbalancerListenerName, "tcp", 24);

        s_logger.debug("update loadbalancer listener in Tungsten-Fabric");
        assertTrue(tungstenApi.updateLoadBalancerListener(projectUuid, tungstenLoadbalancerListenerName, "udp", 25,
            "http://host:8080/client/getLoadBalancerSslCertificate"));

        s_logger.debug("Check if loadbalancer listener was updated in Tungsten-Fabric");
        LoadbalancerListener loadbalancerListener = (LoadbalancerListener) tungstenApi.getTungstenObjectByName(
            LoadbalancerListener.class, project.getQualifiedName(), tungstenLoadbalancerListenerName);
        assertEquals("udp", loadbalancerListener.getProperties().getProtocol());
        assertEquals(Integer.valueOf(25), loadbalancerListener.getProperties().getProtocolPort());
        assertEquals("http://host:8080/client/getLoadBalancerSslCertificate",
            loadbalancerListener.getProperties().getDefaultTlsContainer());
    }

    @Test
    public void applyTungstenPortForwardingTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create instance ip in Tungsten-Fabric");
        tungstenApi.createTungstenFloatingIpPool(tungstenNetworkUuid, "TungstenFip");

        s_logger.debug("Create floating ip in Tungsten-Fabric");
        FloatingIp floatingIp = (FloatingIp) tungstenApi.createTungstenFloatingIp(projectUuid, tungstenNetworkUuid,
            "TungstenFip", "TungstenFi", "192.168.1.100");

        s_logger.debug("Create floating ip in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if the port mapping is not exist in Tungsten-Fabric");
        assertNull(floatingIp.getPortMappings());
        assertNull(floatingIp.getVirtualMachineInterface());
        assertNull(floatingIp.getPortMappingsEnable());

        s_logger.debug("Check if the port mapping was add in Tungsten-Fabric");
        assertTrue(
            tungstenApi.applyTungstenPortForwarding(true, tungstenNetworkUuid, "TungstenFip", "TungstenFi", vmiUuid,
                "tcp", 8080, 80));
        assertEquals("tcp", floatingIp.getPortMappings().getPortMappings().get(0).getProtocol());
        assertEquals(Integer.valueOf(8080), floatingIp.getPortMappings().getPortMappings().get(0).getSrcPort());
        assertEquals(Integer.valueOf(80), floatingIp.getPortMappings().getPortMappings().get(0).getDstPort());
        assertNotNull(floatingIp.getVirtualMachineInterface());
        assertTrue(floatingIp.getPortMappingsEnable());

        s_logger.debug("Check if the port mapping was remove in Tungsten-Fabric");
        assertTrue(tungstenApi.applyTungstenPortForwarding(false, tungstenNetworkUuid, "TungstenFip", "TungstenFi",
            vmiUuid, "tcp", 8080, 80));
        assertEquals(0, floatingIp.getPortMappings().getPortMappings().size());
        assertEquals(0, floatingIp.getVirtualMachineInterface().size());
        assertFalse(floatingIp.getPortMappingsEnable());
    }

    @Test
    public void addTungstenNetworkSubnetCommandTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        VirtualNetwork virtualNetwork = tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName,
            tungstenNetworkName, projectUuid, true, false, null, 0, null, false, null, null, null, false, false, null);

        s_logger.debug("Check if network ipam subnet is empty in Tungsten-Fabric");
        assertNull(virtualNetwork.getNetworkIpam());

        s_logger.debug("Check if network ipam subnet was added to network in Tungsten-Fabric");
        assertTrue(tungstenApi.addTungstenNetworkSubnetCommand(tungstenNetworkUuid, "10.0.0.0", 24, "10.0.0.1", true,
            "10.0.0.253", "10.0.0.10", "10.0.0.20", true, "subnetName"));
        VirtualNetwork virtualNetwork1 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertNotNull(virtualNetwork1.getNetworkIpam());
        assertEquals("10.0.0.0",
            virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().get(0).getSubnet().getIpPrefix());
        assertEquals(Integer.valueOf(24),
            virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().get(0).getSubnet().getIpPrefixLen());
        assertEquals("10.0.0.1",
            virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().get(0).getDefaultGateway());
        assertTrue(virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().get(0).getEnableDhcp());
        assertEquals("10.0.0.253",
            virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().get(0).getDnsServerAddress());
        assertTrue(virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().get(0).getAddrFromStart());
        assertEquals("10.0.0.10", virtualNetwork1.getNetworkIpam()
            .get(0)
            .getAttr()
            .getIpamSubnets()
            .get(0)
            .getAllocationPools()
            .get(0)
            .getStart());
        assertEquals("10.0.0.20", virtualNetwork1.getNetworkIpam()
            .get(0)
            .getAttr()
            .getIpamSubnets()
            .get(0)
            .getAllocationPools()
            .get(0)
            .getEnd());
        assertEquals("subnetName",
            virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().get(0).getSubnetName());
    }

    @Test
    public void removeTungstenNetworkSubnetCommandTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "192.168.100.0", 23, "192.168.100.1", false, null, null, null, false, false, "subnetName1");

        s_logger.debug("Check if network ipam subnet was added to network in Tungsten-Fabric");
        assertTrue(tungstenApi.addTungstenNetworkSubnetCommand(tungstenNetworkUuid, "10.0.0.0", 24, "10.0.0.1", true,
            "10.0.0.253", "10.0.0.10", "10.0.0.20", true, "subnetName2"));
        VirtualNetwork virtualNetwork1 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertEquals(2, virtualNetwork1.getNetworkIpam().get(0).getAttr().getIpamSubnets().size());

        s_logger.debug("Check if network ipam subnet was removed to network in Tungsten-Fabric");
        assertTrue(tungstenApi.removeTungstenNetworkSubnetCommand(tungstenNetworkUuid, "subnetName2"));
        VirtualNetwork virtualNetwork2 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertEquals(1, virtualNetwork2.getNetworkIpam().get(0).getAttr().getIpamSubnets().size());
    }

    @Test
    public void createTungstenTagTypeTest() {
        s_logger.debug("Check if tag type is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(TagType.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create tag type in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenTagType("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype"));

        s_logger.debug("Check if tag type was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(TagType.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void createTungstenTagTest() {
        s_logger.debug("Check if tag is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(Tag.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create tag in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenTag("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype", "tagvalue", "123"));

        s_logger.debug("Check if tag was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(Tag.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void createTungstenApplicationPolicySetTest() {
        s_logger.debug("Check if application policy set is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(ApplicationPolicySet.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create application policy set in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenApplicationPolicySet("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "applicationpolicyset"));

        s_logger.debug("Check if application policy set was created in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.getTungstenObject(ApplicationPolicySet.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void createTungstenFirewallPolicyTest() {
        s_logger.debug("Create application policy set in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenApplicationPolicySet("f5ba12c8-d4c5-4c20-a57d-67a9b6fca652",
            "applicationpolicyset"));

        s_logger.debug("Check if firewall policy is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(FirewallPolicy.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create firewall policy in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenFirewallPolicy("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "firewallpolicy", 1));

        s_logger.debug("Check if firewall policy was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(FirewallPolicy.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void createTungstenFirewallRuleTest() {
        s_logger.debug("Create application policy set in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenApplicationPolicySet("f5ba12c8-d4c5-4c20-a57d-67a9b6fca652",
            "applicationpolicyset"));

        s_logger.debug("Create firewall policy in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenFirewallPolicy("1ab1b179-8c6c-492a-868e-0493f4be175c",
            "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "firewallpolicy", 1));

        s_logger.debug("Check if firewall rule is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(FirewallRule.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create service group in Tungsten-Fabric");
        tungstenApi.createTungstenServiceGroup("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "servicegroup", "tcp", 80, 90);

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe", "tagtype1", "tagvalue1", "123");

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("7d5575eb-d029-467e-8b78-6056a8c94a71", "tagtype2", "tagvalue2", "123");

        s_logger.debug("Create address group in Tungsten-Fabric");
        tungstenApi.createTungstenAddressGroup("88729834-3ebd-413a-adf9-40aff73cf638", "addressgroup1", "10.0.0.0", 24);

        s_logger.debug("Create address group in Tungsten-Fabric");
        tungstenApi.createTungstenAddressGroup("9291ae28-56cf-448c-b848-f2334b3c86da", "addressgroup2", "10.0.0.0", 24);

        s_logger.debug("Create tag type in Tungsten-Fabric");
        tungstenApi.createTungstenTagType("c1680d93-2614-4f99-a8c5-d4f11b3dfc9d", "tagtype");

        s_logger.debug("Create firewall rule in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenFirewallRule("124d0792-e890-4b7e-8fe8-1b7a6d63c66a",
            "1ab1b179-8c6c-492a-868e-0493f4be175c", "firewallrule", "pass", "baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe", "88729834-3ebd-413a-adf9-40aff73cf638", null, ">",
            "7d5575eb-d029-467e-8b78-6056a8c94a71", "9291ae28-56cf-448c-b848-f2334b3c86da",
            null, "c1680d93-2614-4f99-a8c5-d4f11b3dfc9d", 1));

        s_logger.debug("Check if firewall rule was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(FirewallRule.class, "124d0792-e890-4b7e-8fe8-1b7a6d63c66a"));
    }

    @Test
    public void createTungstenServiceGroupTest() {
        s_logger.debug("Check if service group is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(ServiceGroup.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create service group in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.createTungstenServiceGroup("005f0dea-0196-11ec-a1ed-b42e99f6e187", "servicegroup", "tcp", 80,
                90));

        s_logger.debug("Check if service group was created in Tungsten-Fabric");
        ServiceGroup serviceGroup = (ServiceGroup) tungstenApi.getTungstenObject(ServiceGroup.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertNotNull(serviceGroup);
        assertEquals("tcp", serviceGroup.getFirewallServiceList().getFirewallService().get(0).getProtocol());
        assertEquals(Integer.valueOf(80),
            serviceGroup.getFirewallServiceList().getFirewallService().get(0).getDstPorts().getStartPort());
        assertEquals(Integer.valueOf(90),
            serviceGroup.getFirewallServiceList().getFirewallService().get(0).getDstPorts().getEndPort());
    }

    @Test
    public void createTungstenAddressGroupTest() {
        s_logger.debug("Check if address group is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(AddressGroup.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create address group in Tungsten-Fabric");
        assertNotNull(
            tungstenApi.createTungstenAddressGroup("005f0dea-0196-11ec-a1ed-b42e99f6e187", "addressgroup", "10.0.0.0",
                24));

        s_logger.debug("Check if address group was created in Tungsten-Fabric");
        AddressGroup addressGroup = (AddressGroup) tungstenApi.getTungstenObject(AddressGroup.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertNotNull(addressGroup);
        assertEquals("10.0.0.0", addressGroup.getPrefix().getSubnet().get(0).getIpPrefix());
        assertEquals(Integer.valueOf(24), addressGroup.getPrefix().getSubnet().get(0).getIpPrefixLen());
    }

    @Test
    public void applyTungstenNetworkTagTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        VirtualNetwork virtualNetwork = tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName,
            tungstenNetworkName, projectUuid, true, false, null, 0, null, false, null, null, null, false, false, null);

        s_logger.debug("Check if tag is not apply to network in Tungsten-Fabric");
        assertNull(virtualNetwork.getTag());

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype", "tagvalue", "123");

        s_logger.debug("Check if tag was applied to network in Tungsten-Fabric");
        assertTrue(tungstenApi.applyTungstenNetworkTag(List.of(tungstenNetworkUuid),
            "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
        VirtualNetwork virtualNetwork1 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertEquals("005f0dea-0196-11ec-a1ed-b42e99f6e187", virtualNetwork1.getTag().get(0).getUuid());
    }

    @Test
    public void applyTungstenVmTagTest() {
        s_logger.debug("Create vm in Tungsten-Fabric");
        VirtualMachine virtualMachine = tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Check if tag is not apply to vm in Tungsten-Fabric");
        assertNull(virtualMachine.getTag());

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype", "tagvalue", "123");

        s_logger.debug("Check if tag was applied to vm in Tungsten-Fabric");
        assertTrue(
            tungstenApi.applyTungstenVmTag(List.of(tungstenVmUuid), "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
        VirtualMachine virtualMachine1 = (VirtualMachine) tungstenApi.getTungstenObject(VirtualMachine.class,
            tungstenVmUuid);
        assertEquals("005f0dea-0196-11ec-a1ed-b42e99f6e187", virtualMachine1.getTag().get(0).getUuid());
    }

    @Test
    public void applyTungstenNicTagTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create vm in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        VirtualMachineInterface virtualMachineInterface = tungstenApi.createTungstenVmInterface(vmiUuid, vmiName,
            "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid, projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if tag is not apply to vmi in Tungsten-Fabric");
        assertNull(virtualMachineInterface.getTag());

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype", "tagvalue", "123");

        s_logger.debug("Check if tag was applied to vmi in Tungsten-Fabric");
        assertTrue(tungstenApi.applyTungstenNicTag(List.of(vmiUuid), "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
        VirtualMachineInterface virtualMachineInterface1 = (VirtualMachineInterface) tungstenApi.getTungstenObject(
            VirtualMachineInterface.class, vmiUuid);
        assertEquals("005f0dea-0196-11ec-a1ed-b42e99f6e187", virtualMachineInterface1.getTag().get(0).getUuid());
    }

    @Test
    public void applyTungstenPolicyTagTest() {
        s_logger.debug("Create a network policy in Tungsten-Fabric.");
        List<TungstenRule> tungstenRuleList1 = new ArrayList<>();
        NetworkPolicy networkPolicy = (NetworkPolicy) tungstenApi.createOrUpdateTungstenNetworkPolicy("policy",
            projectUuid, tungstenRuleList1);

        s_logger.debug("Check if tag is not apply to network policy in Tungsten-Fabric");
        assertNull(networkPolicy.getTag());

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype", "tagvalue", "123");

        s_logger.debug("Check if tag was applied to network policy in Tungsten-Fabric");
        assertTrue(tungstenApi.applyTungstenPolicyTag(networkPolicy.getUuid(), "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
        NetworkPolicy networkPolicy1 = (NetworkPolicy) tungstenApi.getTungstenObjectByName(NetworkPolicy.class,
            project.getQualifiedName(), "policy");
        assertEquals("005f0dea-0196-11ec-a1ed-b42e99f6e187", networkPolicy1.getTag().get(0).getUuid());
    }

    @Test
    public void removeTungstenTagTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create vm in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Creating a vmi in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);
        s_logger.debug("Create a network policy in Tungsten-Fabric.");

        s_logger.debug("Create a network policy in Tungsten-Fabric.");
        List<TungstenRule> tungstenRuleList1 = new ArrayList<>();
        NetworkPolicy networkPolicy = (NetworkPolicy) tungstenApi.createOrUpdateTungstenNetworkPolicy("policy",
            projectUuid, tungstenRuleList1);

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype", "tagvalue", "123");

        s_logger.debug("Apply tag to network in Tungsten-Fabric");
        tungstenApi.applyTungstenNetworkTag(List.of(tungstenNetworkUuid), "005f0dea-0196-11ec-a1ed-b42e99f6e187");

        s_logger.debug("Check if tag was applied to network in Tungsten-Fabric");
        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertEquals(1, virtualNetwork.getTag().size());

        s_logger.debug("Apply tag to vm in Tungsten-Fabric");
        tungstenApi.applyTungstenVmTag(List.of(tungstenVmUuid), "005f0dea-0196-11ec-a1ed-b42e99f6e187");

        s_logger.debug("Check if tag was applied to vm in Tungsten-Fabric");
        VirtualMachine virtualMachine = (VirtualMachine) tungstenApi.getTungstenObject(VirtualMachine.class,
            tungstenVmUuid);
        assertEquals(1, virtualMachine.getTag().size());

        s_logger.debug("Apply tag to nic in Tungsten-Fabric");
        tungstenApi.applyTungstenNicTag(List.of(vmiUuid), "005f0dea-0196-11ec-a1ed-b42e99f6e187");

        s_logger.debug("Check if tag was applied to nic in Tungsten-Fabric");
        VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) tungstenApi.getTungstenObject(
            VirtualMachineInterface.class, vmiUuid);
        assertEquals(1, virtualMachineInterface.getTag().size());

        s_logger.debug("Apply tag to policy in Tungsten-Fabric");
        tungstenApi.applyTungstenPolicyTag(networkPolicy.getUuid(), "005f0dea-0196-11ec-a1ed-b42e99f6e187");

        s_logger.debug("Check if tag was applied to policy in Tungsten-Fabric");
        NetworkPolicy networkPolicy1 = (NetworkPolicy) tungstenApi.getTungstenObject(NetworkPolicy.class,
            networkPolicy.getUuid());
        assertEquals(1, networkPolicy1.getTag().size());

        s_logger.debug("remove tag from network, vm, nic, policy in Tungsten-Fabric");
        assertNotNull(tungstenApi.removeTungstenTag(List.of(tungstenNetworkUuid), List.of(tungstenVmUuid),
                List.of(vmiUuid), networkPolicy.getUuid(), null, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Check if tag was removed from network in Tungsten-Fabric");
        VirtualNetwork virtualNetwork1 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertEquals(0, virtualNetwork1.getTag().size());

        s_logger.debug("Check if tag was removed from vm in Tungsten-Fabric");
        VirtualMachine virtualMachine1 = (VirtualMachine) tungstenApi.getTungstenObject(VirtualMachine.class,
            tungstenVmUuid);
        assertEquals(0, virtualMachine1.getTag().size());

        s_logger.debug("Check if tag was removed from nic in Tungsten-Fabric");
        VirtualMachineInterface virtualMachineInterface1 = (VirtualMachineInterface) tungstenApi.getTungstenObject(
            VirtualMachineInterface.class, vmiUuid);
        assertEquals(0, virtualMachineInterface1.getTag().size());

        s_logger.debug("Check if tag was removed from policy in Tungsten-Fabric");
        NetworkPolicy networkPolicy2 = (NetworkPolicy) tungstenApi.getTungstenObject(NetworkPolicy.class,
            networkPolicy.getUuid());
        assertEquals(0, networkPolicy2.getTag().size());
    }

    @Test
    public void removeTungstenPolicyTest() {
        s_logger.debug("Create a virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Prepare network policy rule");
        List<TungstenRule> tungstenRuleList = new ArrayList<>();

        s_logger.debug("Create a network policy in Tungsten-Fabric.");
        NetworkPolicy networkPolicy = (NetworkPolicy) tungstenApi.createOrUpdateTungstenNetworkPolicy("policy",
            projectUuid, tungstenRuleList);

        s_logger.debug("Apply network policy to network in Tungsten-Fabric.");
        tungstenApi.applyTungstenNetworkPolicy(networkPolicy.getUuid(), tungstenNetworkUuid, 1, 1);

        s_logger.debug("Check if network policy was applied in Tungsten-Fabric.");
        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertEquals(1, virtualNetwork.getNetworkPolicy().size());

        s_logger.debug("Apply network policy to network in Tungsten-Fabric.");
        tungstenApi.removeTungstenPolicy(tungstenNetworkUuid, networkPolicy.getUuid());

        s_logger.debug("Check if network policy was applied in Tungsten-Fabric.");
        VirtualNetwork virtualNetwork1 = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            tungstenNetworkUuid);
        assertEquals(0, virtualNetwork1.getNetworkPolicy().size());
    }

    @Test
    public void createTungstenPolicyTest() {
        s_logger.debug("Check if policy is not exist in Tungsten-Fabric");
        assertNull(tungstenApi.getTungstenObject(NetworkPolicy.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Create policy in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenPolicy("005f0dea-0196-11ec-a1ed-b42e99f6e187", "policy", projectUuid));

        s_logger.debug("Check if policy was created in Tungsten-Fabric");
        assertNotNull(tungstenApi.getTungstenObject(NetworkPolicy.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void addTungstenPolicyRuleTest() {
        s_logger.debug("Create policy in Tungsten-Fabric");
        NetworkPolicy networkPolicy = (NetworkPolicy) tungstenApi.createTungstenPolicy(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "policy", projectUuid);

        s_logger.debug("Check if policy was created in Tungsten-Fabric");
        assertNull(networkPolicy.getEntries());

        s_logger.debug("Check if policy rule was added in Tungsten-Fabric");
        assertNotNull(tungstenApi.addTungstenPolicyRule("c1680d93-2614-4f99-a8c5-d4f11b3dfc9d",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "pass", "tcp", ">", "network1", "192.168.100.0", 24, 8080, 8081,
            "network2", "10.0.0.0", 16, 80, 81));
        NetworkPolicy networkPolicy1 = (NetworkPolicy) tungstenApi.getTungstenObject(NetworkPolicy.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals("pass", networkPolicy1.getEntries().getPolicyRule().get(0).getActionList().getSimpleAction());
        assertEquals("tcp", networkPolicy1.getEntries().getPolicyRule().get(0).getProtocol());
        assertEquals(">", networkPolicy1.getEntries().getPolicyRule().get(0).getDirection());
        assertEquals("network1",
            networkPolicy1.getEntries().getPolicyRule().get(0).getSrcAddresses().get(0).getVirtualNetwork());
        assertEquals("192.168.100.0", networkPolicy1.getEntries()
            .getPolicyRule()
            .get(0)
            .getSrcAddresses()
            .get(0)
            .getSubnetList()
            .get(0)
            .getIpPrefix());
        assertEquals(Integer.valueOf(24), networkPolicy1.getEntries()
            .getPolicyRule()
            .get(0)
            .getSrcAddresses()
            .get(0)
            .getSubnetList()
            .get(0)
            .getIpPrefixLen());
        assertEquals(Integer.valueOf(8080),
            networkPolicy1.getEntries().getPolicyRule().get(0).getSrcPorts().get(0).getStartPort());
        assertEquals(Integer.valueOf(8081),
            networkPolicy1.getEntries().getPolicyRule().get(0).getSrcPorts().get(0).getEndPort());
        assertEquals("network2",
            networkPolicy1.getEntries().getPolicyRule().get(0).getDstAddresses().get(0).getVirtualNetwork());
        assertEquals("10.0.0.0", networkPolicy1.getEntries()
            .getPolicyRule()
            .get(0)
            .getDstAddresses()
            .get(0)
            .getSubnetList()
            .get(0)
            .getIpPrefix());
        assertEquals(Integer.valueOf(16), networkPolicy1.getEntries()
            .getPolicyRule()
            .get(0)
            .getDstAddresses()
            .get(0)
            .getSubnetList()
            .get(0)
            .getIpPrefixLen());
        assertEquals(Integer.valueOf(80),
            networkPolicy1.getEntries().getPolicyRule().get(0).getDstPorts().get(0).getStartPort());
        assertEquals(Integer.valueOf(81),
            networkPolicy1.getEntries().getPolicyRule().get(0).getDstPorts().get(0).getEndPort());
    }

    @Test
    public void listTungstenAddressPolicyTest() {
        s_logger.debug("Create policy in Tungsten-Fabric");
        ApiObjectBase networkPolicy1 = tungstenApi.createTungstenPolicy("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "policy1", projectUuid);

        s_logger.debug("Check if network policy was listed in Tungsten-Fabric");
        List<? extends ApiObjectBase> networkPolicyList = tungstenApi.listTungstenAddressPolicy(projectUuid, "policy1");
        assertEquals(List.of(networkPolicy1), networkPolicyList);
    }

    @Test
    public void listTungstenPolicyTest() {
        s_logger.debug("Create policy in Tungsten-Fabric");
        ApiObjectBase apiObjectBase1 = tungstenApi.createTungstenPolicy("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "policy1", projectUuid);
        ApiObjectBase apiObjectBase2 = tungstenApi.createTungstenPolicy("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "policy2", projectUuid);
        List<? extends ApiObjectBase> policyList1 = Arrays.asList(apiObjectBase1, apiObjectBase2);
        policyList1.sort(comparator);
        List<? extends ApiObjectBase> policyList2 = List.of(apiObjectBase1);

        s_logger.debug("Check if policy was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> policyList3 = tungstenApi.listTungstenPolicy(projectUuid, null);
        policyList3.sort(comparator);
        assertEquals(policyList1, policyList3);

        s_logger.debug("Check if policy was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> policyList4 = tungstenApi.listTungstenPolicy(projectUuid,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(policyList2, policyList4);
    }

    @Test
    public void listTungstenNetworkTest() {
        s_logger.debug("Create network in Tungsten-Fabric");
        VirtualNetwork virtualNetwork1 = tungstenApi.createTungstenNetwork("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "network1", "network1", projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
            "10.0.0.20", false, false, "");
        VirtualNetwork virtualNetwork2 = tungstenApi.createTungstenNetwork("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "network2", "network2", projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
            "10.0.0.20", false, false, "");
        List<? extends ApiObjectBase> networkList1 = Arrays.asList(virtualNetwork1, virtualNetwork2);
        networkList1.sort(comparator);
        List<? extends ApiObjectBase> networkList2 = List.of(virtualNetwork1);

        s_logger.debug("Check if network was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> networkList3 = tungstenApi.listTungstenNetwork(projectUuid, null);
        networkList3.sort(comparator);
        assertEquals(networkList1, networkList3);

        s_logger.debug("Check if network policy was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> networkList4 = tungstenApi.listTungstenNetwork(projectUuid,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(networkList2, networkList4);
    }

    @Test
    public void listTungstenVmTest() {
        s_logger.debug("Create vm in Tungsten-Fabric");
        VirtualMachine vm1 = tungstenApi.createTungstenVirtualMachine("005f0dea-0196-11ec-a1ed-b42e99f6e187", "vm1");
        VirtualMachine vm2 = tungstenApi.createTungstenVirtualMachine("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "vm2");
        List<? extends ApiObjectBase> vmList1 = Arrays.asList(vm1, vm2);
        vmList1.sort(comparator);
        List<? extends ApiObjectBase> vmList2 = List.of(vm1);

        s_logger.debug("Check if vm was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> vmList3 = tungstenApi.listTungstenVm(projectUuid, null);
        vmList3.sort(comparator);
        assertEquals(vmList1, vmList3);

        s_logger.debug("Check if policy was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> vmList4 = tungstenApi.listTungstenVm(projectUuid,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(vmList2, vmList4);
    }

    @Test
    public void listTungstenNicTest() {
        s_logger.debug("Create network in Tungsten-Fabric");
        tungstenApi.createTungstenNetwork("005f0dea-0196-11ec-a1ed-b42e99f6e187", "network1", "network1", projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");
        tungstenApi.createTungstenNetwork("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "network2", "network2", projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create vm in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine("7d5575eb-d029-467e-8b78-6056a8c94a71", "vm1");
        tungstenApi.createTungstenVirtualMachine("88729834-3ebd-413a-adf9-40aff73cf638", "vm2");

        s_logger.debug("Creating vmi in Tungsten-Fabric.");
        VirtualMachineInterface vmi1 = tungstenApi.createTungstenVmInterface("9291ae28-56cf-448c-b848-f2334b3c86da",
            "vmi1", "02:fc:f3:d6:83:c3", "005f0dea-0196-11ec-a1ed-b42e99f6e187", "7d5575eb-d029-467e-8b78-6056a8c94a71",
            projectUuid, "10.0.0.1", true);
        VirtualMachineInterface vmi2 = tungstenApi.createTungstenVmInterface("124d0792-e890-4b7e-8fe8-1b7a6d63c66a",
            "vmi2", "02:fc:f3:d6:83:c4", "baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "88729834-3ebd-413a-adf9-40aff73cf638",
            projectUuid, "10.0.0.1", true);
        List<? extends ApiObjectBase> vmiList1 = Arrays.asList(vmi1, vmi2);
        vmiList1.sort(comparator);
        List<? extends ApiObjectBase> vmiList2 = List.of(vmi1);

        s_logger.debug("Check if vmi was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> vmiList3 = tungstenApi.listTungstenNic(projectUuid, null);
        vmiList3.sort(comparator);
        assertEquals(vmiList1, vmiList3);

        s_logger.debug("Check if vmi was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> vmList4 = tungstenApi.listTungstenNic(projectUuid,
            "9291ae28-56cf-448c-b848-f2334b3c86da");
        assertEquals(vmiList2, vmList4);
    }

    @Test
    public void listTungstenTagTest() {
        s_logger.debug("Create tag in Tungsten-Fabric");
        ApiObjectBase apiObjectBase1 = tungstenApi.createTungstenTag("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype1",
            "tagvalue1", "123");
        ApiObjectBase apiObjectBase2 = tungstenApi.createTungstenTag("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe", "tagtype2",
            "tagvalue2", "123");
        ApiObjectBase apiObjectBase3 = tungstenApi.createTungstenTag("7d5575eb-d029-467e-8b78-6056a8c94a71", "tagtype3",
            "tagvalue3", "123");
        ApiObjectBase apiObjectBase4 = tungstenApi.createTungstenTag("88729834-3ebd-413a-adf9-40aff73cf638", "tagtype4",
            "tagvalue4", "123");
        ApiObjectBase apiObjectBase5 = tungstenApi.createTungstenTag("105f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype5",
            "tagvalue5", "123");
        ApiObjectBase apiObjectBase6 = tungstenApi.createTungstenTag("7b062909-ba9d-4cf3-bbd3-7db93cf6b4fe", "tagtype6",
            "tagvalue6", "123");
        ApiObjectBase apiObjectBase7 = tungstenApi.createTungstenTag("8d5575eb-d029-467e-8b78-6056a8c94a71", "tagtype7",
            "tagvalue7", "123");
        ApiObjectBase apiObjectBase8 = tungstenApi.createTungstenTag("98729834-3ebd-413a-adf9-40aff73cf638", "tagtype8",
            "tagvalue8", "123");
        List<ApiObjectBase> listTag = Arrays.asList(apiObjectBase1);
        List<ApiObjectBase> listTag1 = Arrays.asList(apiObjectBase1, apiObjectBase2);
        List<ApiObjectBase> listTag2 = Arrays.asList(apiObjectBase3, apiObjectBase4);
        List<ApiObjectBase> listTag3 = Arrays.asList(apiObjectBase5, apiObjectBase6);
        List<ApiObjectBase> listTag4 = Arrays.asList(apiObjectBase7, apiObjectBase8);
        List<ApiObjectBase> listTag5 = Arrays.asList(apiObjectBase1, apiObjectBase2, apiObjectBase3,
            apiObjectBase4, apiObjectBase5, apiObjectBase6, apiObjectBase7, apiObjectBase8);
        listTag1.sort(comparator);
        listTag2.sort(comparator);
        listTag3.sort(comparator);
        listTag4.sort(comparator);
        listTag5.sort(comparator);

        s_logger.debug("Create network and apply tag in Tungsten-Fabric");
        tungstenApi.createTungstenNetwork("9291ae28-56cf-448c-b848-f2334b3c86da", "network1", "network1", projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");
        tungstenApi.applyTungstenNetworkTag(List.of("9291ae28-56cf-448c-b848-f2334b3c86da"),
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        tungstenApi.applyTungstenNetworkTag(List.of("9291ae28-56cf-448c-b848-f2334b3c86da"),
            "6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe");

        s_logger.debug("Create vm and apply tag in Tungsten-Fabric");
        tungstenApi.createTungstenVirtualMachine("124d0792-e890-4b7e-8fe8-1b7a6d63c66a", "vm1");
        tungstenApi.applyTungstenVmTag(List.of("124d0792-e890-4b7e-8fe8-1b7a6d63c66a"),
            "7d5575eb-d029-467e-8b78-6056a8c94a71");
        tungstenApi.applyTungstenVmTag(List.of("124d0792-e890-4b7e-8fe8-1b7a6d63c66a"),
            "88729834-3ebd-413a-adf9-40aff73cf638");

        s_logger.debug("Creating vmi and apply tag in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface("c1680d93-2614-4f99-a8c5-d4f11b3dfc9d", "vmi1", "02:fc:f3:d6:83:c3",
            "9291ae28-56cf-448c-b848-f2334b3c86da", "124d0792-e890-4b7e-8fe8-1b7a6d63c66a", projectUuid, "10.0.0.1", true);
        tungstenApi.applyTungstenNicTag(List.of("c1680d93-2614-4f99-a8c5-d4f11b3dfc9d"),
            "105f0dea-0196-11ec-a1ed-b42e99f6e187");
        tungstenApi.applyTungstenNicTag(List.of("c1680d93-2614-4f99-a8c5-d4f11b3dfc9d"),
            "7b062909-ba9d-4cf3-bbd3-7db93cf6b4fe");

        s_logger.debug("Creating policy and apply tag in Tungsten-Fabric.");
        tungstenApi.createTungstenPolicy("205f0dea-0196-11ec-a1ed-b42e99f6e187", "policy", projectUuid);
        tungstenApi.applyTungstenPolicyTag("205f0dea-0196-11ec-a1ed-b42e99f6e187",
            "8d5575eb-d029-467e-8b78-6056a8c94a71");
        tungstenApi.applyTungstenPolicyTag("205f0dea-0196-11ec-a1ed-b42e99f6e187",
            "98729834-3ebd-413a-adf9-40aff73cf638");

        s_logger.debug("Check if tag was listed with network in Tungsten-Fabric");
        List<ApiObjectBase> listTag6 = tungstenApi.listTungstenTag("9291ae28-56cf-448c-b848-f2334b3c86da",
            null, null, null, null, null);
        listTag6.sort(comparator);
        assertEquals(listTag1, listTag6);

        s_logger.debug("Check if tag was listed with vm in Tungsten-Fabric");
        List<ApiObjectBase> listTag7 = tungstenApi.listTungstenTag(null,
            "124d0792-e890-4b7e-8fe8-1b7a6d63c66a", null, null, null
        , null);
        listTag7.sort(comparator);
        assertEquals(listTag2, listTag7);

        s_logger.debug("Check if tag was listed with nic in Tungsten-Fabric");
        List<ApiObjectBase> listTag8 = tungstenApi.listTungstenTag(null, null,
            "c1680d93-2614-4f99-a8c5-d4f11b3dfc9d", null, null,
            null);
        listTag8.sort(comparator);
        assertEquals(listTag3, listTag8);

        s_logger.debug("Check if tag was listed with policy in Tungsten-Fabric");
        List<ApiObjectBase> listTag9 = tungstenApi.listTungstenTag(null, null, null,
            "205f0dea-0196-11ec-a1ed-b42e99f6e187", null, null);
        listTag9.sort(comparator);
        assertEquals(listTag4, listTag9);

        s_logger.debug("Check if tag was listed all in Tungsten-Fabric");
        List<ApiObjectBase> listTag10 = tungstenApi.listTungstenTag(null, null, null, null, null, null);
        listTag10.sort(comparator);
        assertEquals(listTag5, listTag10);

        s_logger.debug("Check if tag was listed with uuid in Tungsten-Fabric");
        List<ApiObjectBase> listTag11 = tungstenApi.listTungstenTag(null, null, null, null,
            null, "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        listTag11.sort(comparator);
        assertEquals(listTag, listTag11);
    }

    @Test
    public void listTungstenTagTypeTest() {
        s_logger.debug("Create tag type in Tungsten-Fabric");
        ApiObjectBase tagType1 = tungstenApi.createTungstenTagType("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype1");
        ApiObjectBase tagType2 = tungstenApi.createTungstenTagType("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "tagtype2");
        List<? extends ApiObjectBase> tagTypeList1 = Arrays.asList(tagType1, tagType2);
        tagTypeList1.sort(comparator);
        List<? extends ApiObjectBase> tagTypeList2 = List.of(tagType1);

        s_logger.debug("Check if tag type was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> tagTypeList3 = tungstenApi.listTungstenTagType(null);
        tagTypeList3.sort(comparator);
        assertEquals(tagTypeList1, tagTypeList3);

        s_logger.debug("Check if tag type was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> tagTypeList4 = tungstenApi.listTungstenTagType(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(tagTypeList2, tagTypeList4);
    }

    @Test
    public void listTungstenNetworkPolicyTest() {
        s_logger.debug("Create network in Tungsten-Fabric");
        tungstenApi.createTungstenNetwork("005f0dea-0196-11ec-a1ed-b42e99f6e187", "network1", "network1", projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create policy in Tungsten-Fabric");
        ApiObjectBase apiObjectBase1 = tungstenApi.createTungstenPolicy("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe",
            "policy1", projectUuid);
        ApiObjectBase apiObjectBase2 = tungstenApi.createTungstenPolicy("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "policy2", projectUuid);
        List<? extends ApiObjectBase> policyList1 = Arrays.asList(apiObjectBase1, apiObjectBase2);
        List<? extends ApiObjectBase> policyList2 = List.of(apiObjectBase1);
        policyList1.sort(comparator);

        s_logger.debug("Apply network policy to network in Tungsten-Fabric.");
        tungstenApi.applyTungstenNetworkPolicy("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", 1, 1);
        tungstenApi.applyTungstenNetworkPolicy("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", 1, 2);

        s_logger.debug("Check if network policy was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> policyList3 = tungstenApi.listTungstenNetworkPolicy(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", null);
        assertEquals(policyList1, policyList3);

        s_logger.debug("Check if network policy was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> policyList4 = tungstenApi.listTungstenNetworkPolicy(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe");
        assertEquals(policyList2, policyList4);
    }

    @Test
    public void listTungstenApplicationPolicySetTest() {
        s_logger.debug("Create application policy set in Tungsten-Fabric");
        ApiObjectBase applicationPolicySet1 = tungstenApi.createTungstenApplicationPolicySet(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "aps1");
        ApiObjectBase applicationPolicySet2 = tungstenApi.createTungstenApplicationPolicySet(
            "baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "aps2");
        List<? extends ApiObjectBase> apsList1 = Arrays.asList(applicationPolicySet1, applicationPolicySet2);
        apsList1.sort(comparator);
        List<? extends ApiObjectBase> apsList2 = List.of(applicationPolicySet1);

        s_logger.debug("Check if application policy set was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> apsList3 = tungstenApi.listTungstenApplicationPolicySet(null);
        apsList3.sort(comparator);
        assertEquals(apsList1, apsList3);

        s_logger.debug("Check if application policy set was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> apsList4 = tungstenApi.listTungstenApplicationPolicySet(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(apsList2, apsList4);
    }

    @Test
    public void listTungstenFirewallPolicyTest() {
        s_logger.debug("Create application policy set in Tungsten-Fabric");
        tungstenApi.createTungstenApplicationPolicySet("f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "aps1");

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("7d5575eb-d029-467e-8b78-6056a8c94a71", "tagtype1", "tagvalue1", "123");

        s_logger.debug("Create firewall policy in Tungsten-Fabric");
        ApiObjectBase fwPolicy1 = tungstenApi.createTungstenFirewallPolicy("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "firewallpolicy1", 1);
        ApiObjectBase fwPolicy2 = tungstenApi.createTungstenFirewallPolicy("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe",
            "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "firewallpolicy2", 1);
        List<? extends ApiObjectBase> fwPolicyList1 = Arrays.asList(fwPolicy1, fwPolicy2);
        fwPolicyList1.sort(comparator);
        List<? extends ApiObjectBase> fwPolicyList2 = List.of(fwPolicy1);

        s_logger.debug("Check if firewall policy set was listed all with application policy set in Tungsten-Fabric");
        List<? extends ApiObjectBase> fwPolicyList3 = tungstenApi.listTungstenFirewallPolicy(
            "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", null);
        fwPolicyList3.sort(comparator);
        assertEquals(fwPolicyList1, fwPolicyList3);

        s_logger.debug(
            "Check if firewall policy set was listed with uuid and application policy set in Tungsten-Fabric");
        List<? extends ApiObjectBase> fwPolicyList4 = tungstenApi.listTungstenFirewallPolicy(
            "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "baf714fa-80a1-454f-9c32-c4d4a6f5c5a4");
        assertEquals(fwPolicyList2, fwPolicyList4);
    }

    @Test
    public void listTungstenFirewallRuleTest() {
        s_logger.debug("Create application policy set in Tungsten-Fabric");
        tungstenApi.createTungstenApplicationPolicySet("f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "aps");

        s_logger.debug("Create firewall policy in Tungsten-Fabric");
        tungstenApi.createTungstenFirewallPolicy("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "firewallpolicy", 1);

        s_logger.debug("Create service group in Tungsten-Fabric");
        tungstenApi.createTungstenServiceGroup("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "servicegroup1", "tcp", 80, 90);

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe", "tagtype1", "tagvalue1", "123");

        s_logger.debug("Create tag in Tungsten-Fabric");
        tungstenApi.createTungstenTag("7d5575eb-d029-467e-8b78-6056a8c94a71", "tagtype2", "tagvalue2", "123");

        s_logger.debug("Create address group in Tungsten-Fabric");
        tungstenApi.createTungstenAddressGroup("88729834-3ebd-413a-adf9-40aff73cf638", "addressgroup1", "10.0.0.0", 24);

        s_logger.debug("Create address group in Tungsten-Fabric");
        tungstenApi.createTungstenAddressGroup("9291ae28-56cf-448c-b848-f2334b3c86da", "addressgroup2", "10.0.0.0", 24);

        s_logger.debug("Create tag type in Tungsten-Fabric");
        tungstenApi.createTungstenTagType("c1680d93-2614-4f99-a8c5-d4f11b3dfc9d", "tagtype1");

        s_logger.debug("Create firewall rule in Tungsten-Fabric");
        ApiObjectBase firewallRule1 = tungstenApi.createTungstenFirewallRule("124d0792-e890-4b7e-8fe8-1b7a6d63c66a",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "firewallrule1", "pass", "baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe", "88729834-3ebd-413a-adf9-40aff73cf638", null, ">",
            "7d5575eb-d029-467e-8b78-6056a8c94a71", "9291ae28-56cf-448c-b848-f2334b3c86da",
            null, "c1680d93-2614-4f99-a8c5-d4f11b3dfc9d", 1);
        ApiObjectBase firewallRule2 = tungstenApi.createTungstenFirewallRule("224d0792-e890-4b7e-8fe8-1b7a6d63c66a",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "firewallrule2", "pass", "baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe", "88729834-3ebd-413a-adf9-40aff73cf638", null, ">",
            "7d5575eb-d029-467e-8b78-6056a8c94a71", "9291ae28-56cf-448c-b848-f2334b3c86da",
            null, "c1680d93-2614-4f99-a8c5-d4f11b3dfc9d", 1);

        List<? extends ApiObjectBase> fwRuleList1 = Arrays.asList(firewallRule1, firewallRule2);
        fwRuleList1.sort(comparator);
        List<? extends ApiObjectBase> fwRuleList2 = List.of(firewallRule1);

        s_logger.debug("Check if firewall rule set was listed all with firewall policy in Tungsten-Fabric");
        List<? extends ApiObjectBase> fwRuleList3 = tungstenApi.listTungstenFirewallRule(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", null);
        fwRuleList3.sort(comparator);
        assertEquals(fwRuleList1, fwRuleList3);

        s_logger.debug("Check if firewall rule set was listed with uuid and firewall policy in Tungsten-Fabric");
        List<? extends ApiObjectBase> fwRuleList4 = tungstenApi.listTungstenFirewallRule(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "124d0792-e890-4b7e-8fe8-1b7a6d63c66a");
        assertEquals(fwRuleList2, fwRuleList4);
    }

    @Test
    public void listTungstenServiceGroupTest() {
        s_logger.debug("Create service group in Tungsten-Fabric");
        ApiObjectBase serviceGroup1 = tungstenApi.createTungstenServiceGroup("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "serviceGroup1", "tcp", 80, 80);
        ApiObjectBase serviceGroup2 = tungstenApi.createTungstenServiceGroup("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "serviceGroup2", "tcp", 80, 80);
        List<? extends ApiObjectBase> serviceGroupList1 = Arrays.asList(serviceGroup1, serviceGroup2);
        serviceGroupList1.sort(comparator);
        List<? extends ApiObjectBase> serviceGroupList2 = List.of(serviceGroup1);

        s_logger.debug("Check if service group was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> serviceGroupList3 = tungstenApi.listTungstenServiceGroup(null);
        serviceGroupList3.sort(comparator);
        assertEquals(serviceGroupList1, serviceGroupList3);

        s_logger.debug("Check if tag type was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> serviceGroupList4 = tungstenApi.listTungstenServiceGroup(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(serviceGroupList2, serviceGroupList4);
    }

    @Test
    public void listTungstenAddressGroupTest() {
        s_logger.debug("Create address group in Tungsten-Fabric");
        ApiObjectBase addressGroup1 = tungstenApi.createTungstenAddressGroup("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "addressGroup1", "10.0.0.0", 24);
        ApiObjectBase addressGroup2 = tungstenApi.createTungstenAddressGroup("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "addressGroup2", "10.0.0.0", 24);
        List<? extends ApiObjectBase> addressGroupList1 = Arrays.asList(addressGroup1, addressGroup2);
        addressGroupList1.sort(comparator);
        List<? extends ApiObjectBase> addressGroupList2 = List.of(addressGroup1);

        s_logger.debug("Check if service group was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> addressGroupList3 = tungstenApi.listTungstenAddressGroup(null);
        addressGroupList3.sort(comparator);
        assertEquals(addressGroupList1, addressGroupList3);

        s_logger.debug("Check if service group was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> addressGroupList4 = tungstenApi.listTungstenAddressGroup(
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(addressGroupList2, addressGroupList4);
    }

    @Test
    public void removeTungstenNetworkPolicyRuleTest() {
        s_logger.debug("Create policy in Tungsten-Fabric");
        tungstenApi.createTungstenPolicy("005f0dea-0196-11ec-a1ed-b42e99f6e187", "policy", projectUuid);

        s_logger.debug("Add policy rule in Tungsten-Fabric");
        tungstenApi.addTungstenPolicyRule("c1680d93-2614-4f99-a8c5-d4f11b3dfc9d",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "pass", "tcp", ">", "network1", "192.168.100.0", 24, 8080, 8081,
            "network2", "10.0.0.0", 16, 80, 81);

        s_logger.debug("Check if policy rule was add to network policy in Tungsten-Fabric");
        NetworkPolicy networkPolicy1 = (NetworkPolicy) tungstenApi.getTungstenObject(NetworkPolicy.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(1, networkPolicy1.getEntries().getPolicyRule().size());

        s_logger.debug("Check if policy rule was remove from network policy in Tungsten-Fabric");
        assertNotNull(tungstenApi.removeTungstenNetworkPolicyRule("005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "c1680d93-2614-4f99-a8c5-d4f11b3dfc9d"));
        NetworkPolicy networkPolicy2 = (NetworkPolicy) tungstenApi.getTungstenObject(NetworkPolicy.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(0, networkPolicy2.getEntries().getPolicyRule().size());
    }

    @Test
    public void updateTungstenVrouterConfig() {
        GlobalVrouterConfig globalVrouterConfig = (GlobalVrouterConfig) tungstenApi.updateTungstenVrouterConfig("l3");
        assertEquals("l3", globalVrouterConfig.getForwardingMode());
    }

    @Test
    public void deleteTungstenObjectTest() {
        s_logger.debug("Create tag type in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenTagType("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype"));

        s_logger.debug("Check if tag type was deleted in Tungsten-Fabric");
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(TagType.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertTrue(tungstenApi.deleteTungstenObject(apiObjectBase));
        assertNull(tungstenApi.getTungstenObject(TagType.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void deleteTungstenObjectWithUuidTest() {
        s_logger.debug("Create tag type in Tungsten-Fabric");
        assertNotNull(tungstenApi.createTungstenTagType("005f0dea-0196-11ec-a1ed-b42e99f6e187", "tagtype"));

        s_logger.debug("Check if tag type was deleted in Tungsten-Fabric");
        assertTrue(tungstenApi.deleteTungstenObject(TagType.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
        assertNull(tungstenApi.getTungstenObject(TagType.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void getTungstenListObjectTest() {
        s_logger.debug("Create network in Tungsten-Fabric");
        VirtualNetwork network1 = tungstenApi.createTungstenNetwork("005f0dea-0196-11ec-a1ed-b42e99f6e187", "network1",
            "network1", projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20",
            false, false, "");
        VirtualNetwork network2 = tungstenApi.createTungstenNetwork("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "network2",
            "network2", projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20",
            false, false, "");
        List<? extends ApiObjectBase> list1 = Arrays.asList(network1, network2);
        list1.sort(comparator);
        List<? extends ApiObjectBase> list2 = List.of(network1);

        s_logger.debug("Check if network was listed all in Tungsten-Fabric");
        List<? extends ApiObjectBase> list3 = tungstenApi.getTungstenListObject(VirtualNetwork.class, project, null);
        list3.sort(comparator);
        assertEquals(list1, list3);

        s_logger.debug("Check if network was listed with uuid in Tungsten-Fabric");
        List<? extends ApiObjectBase> list4 = tungstenApi.getTungstenListObject(VirtualNetwork.class, null,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(list2, list4);
    }

    @Test
    public void addInstanceToSecurityGroupTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid, tungstenSecurityGroupName,
            "TungstenSecurityGroupDescription", projectFqn));

        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName,
            projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false,
            ""));

        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName));

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        assertNotNull(
            tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
                projectUuid, "10.0.0.1", true));

        s_logger.debug("Check if instance have no security group in Tungsten-Fabric.");
        VirtualMachineInterface virtualMachineInterface1 = (VirtualMachineInterface) tungstenApi.getTungstenObject(
            VirtualMachineInterface.class, vmiUuid);
        assertNull(virtualMachineInterface1.getSecurityGroup());
        assertFalse(virtualMachineInterface1.getPortSecurityEnabled());

        s_logger.debug("Add instance to security group in Tungsten-Fabric.");
        tungstenApi.addInstanceToSecurityGroup(vmiUuid, List.of(tungstenSecurityGroupUuid));

        s_logger.debug("Check if instance was added to security group in Tungsten-Fabric.");
        VirtualMachineInterface virtualMachineInterface2 = (VirtualMachineInterface) tungstenApi.getTungstenObject(
            VirtualMachineInterface.class, vmiUuid);
        assertEquals(1, virtualMachineInterface2.getSecurityGroup().size());
        assertEquals(tungstenSecurityGroupUuid, virtualMachineInterface2.getSecurityGroup().get(0).getUuid());
        assertTrue(virtualMachineInterface2.getPortSecurityEnabled());
    }

    @Test
    public void removeInstanceFromSecurityGroupTest() {
        String projectFqn = TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;

        s_logger.debug("Create a security group in Tungsten-Fabric.");
        tungstenApi.createTungstenSecurityGroup(tungstenSecurityGroupUuid, tungstenSecurityGroupName,
            "TungstenSecurityGroupDescription", projectFqn);

        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Add instance to security group in Tungsten-Fabric.");
        tungstenApi.addInstanceToSecurityGroup(vmiUuid, List.of(tungstenSecurityGroupUuid));

        s_logger.debug("Check if instance was added to security group in Tungsten-Fabric.");
        VirtualMachineInterface virtualMachineInterface1 = (VirtualMachineInterface) tungstenApi.getTungstenObject(
            VirtualMachineInterface.class, vmiUuid);
        assertEquals(1, virtualMachineInterface1.getSecurityGroup().size());

        s_logger.debug("Remove instance from security group in Tungsten-Fabric.");
        assertTrue(tungstenApi.removeInstanceFromSecurityGroup(vmiUuid, List.of(tungstenSecurityGroupUuid)));

        s_logger.debug("Check if instance was removed from security group in Tungsten-Fabric.");
        VirtualMachineInterface virtualMachineInterface2 = (VirtualMachineInterface) tungstenApi.getTungstenObject(
            VirtualMachineInterface.class, vmiUuid);
        assertEquals(0, virtualMachineInterface2.getSecurityGroup().size());
        assertFalse(virtualMachineInterface2.getPortSecurityEnabled());
    }

    @Test
    public void addSecondaryIpAddressTest() {
        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if secondary ip address was not exist in Tungsten-Fabric.");
        assertNull(tungstenApi.getTungstenObjectByName(InstanceIp.class, null, "secondaryip"));

        s_logger.debug("Check if secondary ip address was added to nic in Tungsten-Fabric.");
        assertTrue(tungstenApi.addSecondaryIpAddress(tungstenNetworkUuid, vmiUuid, "secondaryip1", "10.0.0.100"));
        InstanceIp instanceIp2 = (InstanceIp) tungstenApi.getTungstenObjectByName(InstanceIp.class, null,
            "secondaryip1");
        assertEquals("10.0.0.100", instanceIp2.getAddress());
        assertEquals(tungstenNetworkUuid, instanceIp2.getVirtualNetwork().get(0).getUuid());
        assertEquals(vmiUuid, instanceIp2.getVirtualMachineInterface().get(0).getUuid());
        assertTrue(instanceIp2.getSecondary());

        s_logger.debug("Check if secondary ip address with ip v6 was added to nic in Tungsten-Fabric.");
        assertTrue(tungstenApi.addSecondaryIpAddress(tungstenNetworkUuid, vmiUuid, "secondaryip2", "fd00::100"));
        InstanceIp instanceIp3 = (InstanceIp) tungstenApi.getTungstenObjectByName(InstanceIp.class, null,
            "secondaryip2");
        assertEquals("fd00::100", instanceIp3.getAddress());
        assertEquals("v6", instanceIp3.getFamily());
    }

    @Test
    public void removeSecondaryIpAddressTest() {
        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create virtual machine in Tungsten-Fabric.");
        tungstenApi.createTungstenVirtualMachine(tungstenVmUuid, tungstenVmName);

        s_logger.debug("Create virtual machine interface in Tungsten-Fabric.");
        tungstenApi.createTungstenVmInterface(vmiUuid, vmiName, "02:fc:f3:d6:83:c3", tungstenNetworkUuid, tungstenVmUuid,
            projectUuid, "10.0.0.1", true);

        s_logger.debug("Check if secondary ip address was added to nic in Tungsten-Fabric.");
        assertTrue(tungstenApi.addSecondaryIpAddress(tungstenNetworkUuid, vmiUuid, "secondaryip", "10.0.0.100"));
        assertNotNull(tungstenApi.getTungstenObjectByName(InstanceIp.class, null, "secondaryip"));

        s_logger.debug("Check if secondary ip address was removed from nic in Tungsten-Fabric.");
        assertTrue(tungstenApi.removeSecondaryIpAddress("secondaryip"));
        assertNull(tungstenApi.getTungstenObjectByName(InstanceIp.class, null, "secondaryip"));
    }

    @Test
    public void createRoutingLogicalRouterTest() {
        s_logger.debug("Check if logical router was not exist in Tungsten-Fabric.");
        assertNull(tungstenApi.getTungstenObject(LogicalRouter.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));

        s_logger.debug("Check if logical router was created in Tungsten-Fabric.");
        assertNotNull(tungstenApi.createRoutingLogicalRouter(projectUuid, "005f0dea-0196-11ec-a1ed-b42e99f6e187",
            "TungstenLogicalRouter"));
        assertNotNull(tungstenApi.getTungstenObject(LogicalRouter.class, "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
    }

    @Test
    public void listRoutingLogicalRouterTest() {
        s_logger.debug("Create logical router in Tungsten-Fabric.");
        ApiObjectBase apiObjectBase1 = tungstenApi.createRoutingLogicalRouter(projectUuid,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "logicalRouter1");
        ApiObjectBase apiObjectBase2 = tungstenApi.createRoutingLogicalRouter(projectUuid,
            "baf714fa-80a1-454f-9c32-c4d4a6f5c5a4", "logicalRouter2");
        List<? extends ApiObjectBase> list1 = Arrays.asList(apiObjectBase1, apiObjectBase2);
        list1.sort(comparator);
        List<? extends ApiObjectBase> list2 = List.of(apiObjectBase1);

        s_logger.debug("Check if logical router was listed all in Tungsten-Fabric.");
        List<? extends ApiObjectBase> list3 = tungstenApi.listRoutingLogicalRouter(null);
        list3.sort(comparator);
        assertEquals(list1, list3);
        List<? extends ApiObjectBase> list4 = tungstenApi.listRoutingLogicalRouter("005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(list2, list4);
    }

    @Test
    public void addNetworkGatewayToLogicalRouterTest() {
        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            false, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create logical router in Tungsten-Fabric.");
        tungstenApi.createRoutingLogicalRouter(projectUuid, "005f0dea-0196-11ec-a1ed-b42e99f6e187", "logicalRouter1");

        s_logger.debug("Check if logical router have no network gateway in Tungsten-Fabric.");
        LogicalRouter logicalRouter1 = (LogicalRouter) tungstenApi.getTungstenObject(LogicalRouter.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertNull(logicalRouter1.getVirtualMachineInterface());

        s_logger.debug("Check if network gateway was added to logical router in Tungsten-Fabric.");
        assertNotNull(
            tungstenApi.addNetworkGatewayToLogicalRouter(tungstenNetworkUuid, "005f0dea-0196-11ec-a1ed-b42e99f6e187",
                "192.168.100.100"));
        LogicalRouter logicalRouter2 = (LogicalRouter) tungstenApi.getTungstenObject(LogicalRouter.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(1, logicalRouter2.getVirtualMachineInterface().size());
    }

    @Test
    public void removeNetworkGatewayFromLogicalRouterTest() {
        s_logger.debug("Create virtual network in Tungsten-Fabric.");
        tungstenApi.createTungstenNetwork(tungstenNetworkUuid, tungstenNetworkName, tungstenNetworkName, projectUuid,
            false, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10", "10.0.0.20", false, false, "");

        s_logger.debug("Create logical router in Tungsten-Fabric.");
        tungstenApi.createRoutingLogicalRouter(projectUuid, "005f0dea-0196-11ec-a1ed-b42e99f6e187", "logicalRouter1");

        s_logger.debug("Check if network gateway was added to logical router in Tungsten-Fabric.");
        assertNotNull(
            tungstenApi.addNetworkGatewayToLogicalRouter(tungstenNetworkUuid, "005f0dea-0196-11ec-a1ed-b42e99f6e187",
                "192.168.100.100"));
        LogicalRouter logicalRouter1 = (LogicalRouter) tungstenApi.getTungstenObject(LogicalRouter.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(1, logicalRouter1.getVirtualMachineInterface().size());

        s_logger.debug("Check if network gateway was removed from logical router in Tungsten-Fabric.");
        assertNotNull(tungstenApi.removeNetworkGatewayFromLogicalRouter(tungstenNetworkUuid,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187"));
        LogicalRouter logicalRouter2 = (LogicalRouter) tungstenApi.getTungstenObject(LogicalRouter.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        assertEquals(0, logicalRouter2.getVirtualMachineInterface().size());
    }

    @Test
    public void listConnectedNetworkFromLogicalRouterTest() {
        s_logger.debug("Create network in Tungsten-Fabric");
        VirtualNetwork virtualNetwork1 = tungstenApi.createTungstenNetwork("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe",
            "network1", "network1", projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
            "10.0.0.20", false, false, "");
        VirtualNetwork virtualNetwork2 = tungstenApi.createTungstenNetwork("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "network2", "network2", projectUuid, true, false, "10.0.0.0", 24, "10.0.0.1", true, null, "10.0.0.10",
            "10.0.0.20", false, false, "");
        List<? extends ApiObjectBase> list1 = Arrays.asList(virtualNetwork1, virtualNetwork2);
        list1.sort(comparator);

        s_logger.debug("Create logical router in Tungsten-Fabric.");
        tungstenApi.createRoutingLogicalRouter(projectUuid, "005f0dea-0196-11ec-a1ed-b42e99f6e187", "logicalRouter");

        s_logger.debug("Add network gateway to logical router in Tungsten-Fabric.");
        tungstenApi.addNetworkGatewayToLogicalRouter("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "192.168.100.100");
        tungstenApi.addNetworkGatewayToLogicalRouter("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "005f0dea-0196-11ec-a1ed-b42e99f6e187", "192.168.100.101");

        s_logger.debug("Check if connected network in logical router was listed in Tungsten-Fabric.");
        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.getTungstenObject(LogicalRouter.class,
            "005f0dea-0196-11ec-a1ed-b42e99f6e187");
        List<? extends ApiObjectBase> list2 = tungstenApi.listConnectedNetworkFromLogicalRouter(logicalRouter);
        list2.sort(comparator);
        assertEquals(list1, list2);
    }
}
