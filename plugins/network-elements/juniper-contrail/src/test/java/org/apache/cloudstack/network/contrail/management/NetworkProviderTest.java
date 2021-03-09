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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.address.AssociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.nat.DisableStaticNatCmd;
import org.apache.cloudstack.api.command.user.nat.EnableStaticNatCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.project.CreateProjectCmd;
import org.apache.cloudstack.api.command.user.project.DeleteProjectCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.agent.AgentManager;
import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.CloudException;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.db.Merovingian2;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.vm.VirtualMachineManager;

import junit.framework.TestCase;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorFactory;
import net.juniper.contrail.api.ApiConnectorMock;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/providerContext.xml")
/**
 * Exercise the public API.
 */
public class NetworkProviderTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(NetworkProviderTest.class);

    @Inject
    public ContrailManager _contrailMgr;
    @Inject
    public ServerDBSync _dbSync;
    @Inject
    public AccountManager _accountMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    private NetworkService _networkService;

    @Inject
    public VirtualMachineManager _vmMgr;

    @Inject
    public DomainDao _domainDao;
    @Inject
    public ProjectDao _projectDao;
    @Inject
    public AgentManager _agentMgr;

    private ManagementServerMock _server;
    private ApiConnector _api;
    private static int s_mysqlSrverPort;
    private static long s_msId;
    private static Merovingian2 s_lockController;
    public static boolean s_initDone = false;

    @BeforeClass
    public static void globalSetUp() throws Exception {
        ApiConnectorFactory.setImplementation(ApiConnectorMock.class);
        s_logger.info("mysql server is getting launched ");
        s_mysqlSrverPort = TestDbSetup.init(null);
        s_logger.info("mysql server launched on port " + s_mysqlSrverPort);

        s_msId = ManagementServerNode.getManagementServerId();
        s_lockController = Merovingian2.createLockController(s_msId);
    }

    @AfterClass
    public static void globalTearDown() throws Exception {
        s_lockController.cleanupForServer(s_msId);
        JmxUtil.unregisterMBean("Locks", "Locks");
        s_lockController = null;

        AbstractApplicationContext ctx = (AbstractApplicationContext)ComponentContext.getApplicationContext();
        Map<String, ComponentLifecycle> lifecycleComponents = ctx.getBeansOfType(ComponentLifecycle.class);
        for (ComponentLifecycle bean : lifecycleComponents.values()) {
            bean.stop();
        }
        ctx.close();

        s_logger.info("destroying mysql server instance running at port <" + s_mysqlSrverPort + ">");
        TestDbSetup.destroy(s_mysqlSrverPort, null);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        try {
            ComponentContext.initComponentsLifeCycle();
        } catch (Exception ex) {
            ex.printStackTrace();
            s_logger.error(ex.getMessage());
        }
        Account system = _accountMgr.getSystemAccount();
        User user = _accountMgr.getSystemUser();
        CallContext.register(user, system);
        _server = ComponentContext.inject(new ManagementServerMock());

        _server.initialize(!s_initDone);
        s_initDone = false;
        _api = _contrailMgr.getApiConnector();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        _server.shutdown();
    }

    private void purgeTestNetwork() {
        Account system = _accountMgr.getSystemAccount();
        DataCenter zone = _server.getZone();
        List<? extends Network> list = _networkService.getIsolatedNetworksOwnedByAccountInZone(zone.getId(), system);
        for (Network net : list) {
            s_logger.debug("Delete network " + net.getName());
            _networkService.deleteNetwork(net.getId(), false);
        }
    }

    private Network lookupTestNetwork(String name) {
        Account system = _accountMgr.getSystemAccount();
        DataCenter zone = _server.getZone();
        List<? extends Network> list = _networkService.getIsolatedNetworksOwnedByAccountInZone(zone.getId(), system);
        for (Network net : list) {
            if (net.getName().equals(name)) {
                return net;
            }
        }
        return null;
    }

    private Network createTestNetwork(String name) {
        CreateNetworkCmd cmd = new CreateNetworkCmd();
        ComponentContext.inject(cmd);
        Account system = _accountMgr.getSystemAccount();
        DataCenter zone = _server.getZone();

        ManagementServerMock.setParameter(cmd, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        ManagementServerMock.setParameter(cmd, ApiConstants.NAME, BaseCmd.CommandType.STRING, name);
        ManagementServerMock.setParameter(cmd, "displayText", BaseCmd.CommandType.STRING, "test network");
        ManagementServerMock.setParameter(cmd, "networkOfferingId", BaseCmd.CommandType.LONG, _contrailMgr.getRouterOffering().getId());
        ManagementServerMock.setParameter(cmd, "zoneId", BaseCmd.CommandType.LONG, zone.getId());
        ManagementServerMock.setParameter(cmd, ApiConstants.GATEWAY, BaseCmd.CommandType.STRING, "10.0.1.254");
        ManagementServerMock.setParameter(cmd, ApiConstants.NETMASK, BaseCmd.CommandType.STRING, "255.255.255.0");
        // Physical network id can't be specified for Guest traffic type.
        // SetParameter(cmd, "physicalNetworkId", BaseCmd.CommandType.LONG, _znet.getId());

        Network result = null;
        try {
            result = _networkService.createGuestNetwork(cmd);
        } catch (CloudException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    @Test
    //@Ignore
        public
        void testCreateNetwork() {
        purgeTestNetwork();
        createTestNetwork("test");
    }

    @Test
    public void testConnectivity() {
        Network network = lookupTestNetwork("test");
        if (network == null) {
            network = createTestNetwork("test");
        }
        UserVm vm1 = _server.createVM("x01", network);
        UserVm vm2 = _server.createVM("x02", network);

        _server.deleteVM(vm1, network);
        _server.deleteVM(vm2, network);
    }

    @Test
    public void floatingIpTest() {
        Network network = lookupTestNetwork("test-fip-net");
        if (network == null) {
            network = createTestNetwork("test-fip-net");
        }
        UserVm vm = _server.createVM("test-fip-vm", network);
        try {
            IPAddressVO ip = createFloatingIp(network, vm);
            deleteFloatingIp(ip);
        } catch (Exception e) {
            fail("unable to create/delete floating ip");
        }
        _server.deleteVM(vm, network);
    }

    public void deleteFloatingIp(IPAddressVO ip) throws Exception {
        BaseCmd cmd = new DisableStaticNatCmd();
        BaseCmd proxy = ComponentContext.inject(cmd);
        ManagementServerMock.setParameter(proxy, "ipAddressId", BaseCmd.CommandType.LONG, ip.getId());
        try {
            proxy.execute();
        } catch (Exception e) {
            s_logger.debug("DisableStaticNatCmd exception: " + e);
            e.printStackTrace();
            throw e;
        }
    }

    public IPAddressVO createFloatingIp(Network network, UserVm vm) throws Exception {
        BaseCmd cmd = new AssociateIPAddrCmd();
        BaseCmd proxy = ComponentContext.inject(cmd);
        Account system = _accountMgr.getSystemAccount();
        DataCenter zone = _server.getZone();

        ManagementServerMock.setParameter(proxy, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        ManagementServerMock.setParameter(proxy, "domainId", BaseCmd.CommandType.LONG, Domain.ROOT_DOMAIN);
        ManagementServerMock.setParameter(proxy, "zoneId", BaseCmd.CommandType.LONG, zone.getId());
        ManagementServerMock.setParameter(proxy, "networkId", BaseCmd.CommandType.LONG, network.getId());
        try {
            ((AssociateIPAddrCmd)cmd).create();
            ((AssociateIPAddrCmd)cmd).execute();
        } catch (Exception e) {
            s_logger.debug("AssociateIPAddrCmd exception: " + e);
            e.printStackTrace();
            throw e;
        }

        SearchBuilder<IPAddressVO> searchBuilder = _ipAddressDao.createSearchBuilder();
        searchBuilder.and("sourceNat", searchBuilder.entity().isSourceNat(), Op.EQ);
        searchBuilder.and("network", searchBuilder.entity().getAssociatedWithNetworkId(), Op.EQ);
        searchBuilder.and("dataCenterId", searchBuilder.entity().getDataCenterId(), Op.EQ);
        searchBuilder.and("associatedWithVmId", searchBuilder.entity().getAssociatedWithVmId(), Op.NULL);
        SearchCriteria<IPAddressVO> sc = searchBuilder.create();
        sc.setParameters("sourceNat", false);
        sc.setParameters("network", network.getId());

        List<IPAddressVO> publicIps = _ipAddressDao.search(sc, null);
        assertNotNull(publicIps);

        cmd = new EnableStaticNatCmd();
        proxy = ComponentContext.inject(cmd);
        ManagementServerMock.setParameter(proxy, "ipAddressId", BaseCmd.CommandType.LONG, publicIps.get(0).getId());
        ManagementServerMock.setParameter(proxy, "networkId", BaseCmd.CommandType.LONG, network.getId());
        ManagementServerMock.setParameter(proxy, "virtualMachineId", BaseCmd.CommandType.LONG, vm.getId());

        try {
            proxy.execute();
        } catch (Exception e) {
            s_logger.debug("EnableStaticNatCmd exception: " + e);
            e.printStackTrace();
            throw e;
        }
        return publicIps.get(0);
    }

    public void createProject(String name) {
        BaseCmd cmd = new CreateProjectCmd();
        BaseCmd proxy = ComponentContext.inject(cmd);
        Account system = _accountMgr.getSystemAccount();

        ManagementServerMock.setParameter(proxy, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        ManagementServerMock.setParameter(proxy, "domainId", BaseCmd.CommandType.LONG, Domain.ROOT_DOMAIN);
        ManagementServerMock.setParameter(proxy, "name", BaseCmd.CommandType.STRING, name);
        ManagementServerMock.setParameter(proxy, "displayText", BaseCmd.CommandType.STRING, name);
        try {
            ((CreateProjectCmd)proxy).create();
            ((CreateProjectCmd)proxy).execute();
        } catch (Exception e) {
            s_logger.debug("CreateProjectCmd exception: " + e);
            e.printStackTrace();
            fail("create project cmd failed");
        }
        DomainVO domain = _domainDao.findById(Domain.ROOT_DOMAIN);
        try {
            net.juniper.contrail.api.types.Domain vncDomain =
                (net.juniper.contrail.api.types.Domain)_api.findById(net.juniper.contrail.api.types.Domain.class, domain.getUuid());
            if (_api.findByName(net.juniper.contrail.api.types.Project.class, vncDomain, name) == null) {
                fail("create project failed in vnc");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception while creating a project in vnc");
        }
    }

    public void deleteProject(String name) {
        BaseCmd cmd = new DeleteProjectCmd();
        BaseCmd proxy = ComponentContext.inject(cmd);

        ProjectVO project = _projectDao.findByNameAndDomain(name, Domain.ROOT_DOMAIN);
        try {
            ManagementServerMock.setParameter(proxy, "id", BaseCmd.CommandType.LONG, project.getId());
            ((DeleteProjectCmd)proxy).execute();
            if (_api.findById(net.juniper.contrail.api.types.Project.class, project.getUuid()) != null) {
                fail("unable to delete project in vnc");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception while deleting project");
        }
    }

    @Test
    public void testProject() {
        createProject("test-project");
        deleteProject("test-project");
    }

    @Test
    public void dbSyncTest() {
        Network network = lookupTestNetwork("test-db-only-net");
        if (network == null) {
            network = createTestNetwork("test-db-only-net");
        }
        UserVm vm = _server.createVM("test-db-only-vm", network);
        try {
            createFloatingIp(network, vm);
        } catch (Exception e) {
            fail("unable to create floating ip");
        }

        /* reset ApiServer objects to default config only, so above created objects
         * exists only in cludstack db but not in api server
         */
        ((ApiConnectorMock)_api).initConfig();
        /* reset model cached objects */
        _contrailMgr.getDatabase().initDb();

        /* Create one object of each type directly in api-server - these objects does not exist in cloudstack */
        net.juniper.contrail.api.types.Domain domain = new net.juniper.contrail.api.types.Domain();
        domain.setName("test-vnc-only-domain--1");
        domain.setUuid(UUID.randomUUID().toString());
        try {
            assertTrue(_api.create(domain));
        } catch (IOException ex) {
            fail(ex.getMessage());
        }

        Project project = new Project();
        project.setName("test-vnc-only-project-1");
        project.setUuid(UUID.randomUUID().toString());
        project.setParent(domain);
        try {
            assertTrue(_api.create(project));
        } catch (IOException ex) {
            fail(ex.getMessage());
        }

        VirtualNetwork net = new VirtualNetwork();
        net.setName("test-vnc-only-net-1");
        net.setUuid(UUID.randomUUID().toString());
        net.setParent(project);

        NetworkIpam ipam = null;
        try {
            // Find default-network-ipam
            String ipam_id = _api.findByName(NetworkIpam.class, null, "default-network-ipam");
            assertNotNull(ipam_id);
            ipam = (NetworkIpam)_api.findById(NetworkIpam.class, ipam_id);
            assertNotNull(ipam);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }

        VnSubnetsType subnet = new VnSubnetsType();
        subnet.addIpamSubnets(new SubnetType("10.0.2.0", 24), "10.0.2.254");

        net.addNetworkIpam(ipam, subnet);

        VirtualMachine vncVm = new VirtualMachine();
        vncVm.setName("test-vnc-only-vm-1");
        try {
            assertTrue(_api.create(vncVm));
        } catch (IOException ex) {
            fail(ex.getMessage());
        }

        VirtualMachineInterface vmi = new VirtualMachineInterface();
        vmi.setParent(vncVm);
        vmi.setName("test-vnc-only-vmi-1");

        try {
            assertTrue(_api.create(vmi));
            assertTrue(_api.create(net));
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
        InstanceIp ip_obj = new InstanceIp();
        ip_obj.setName(net.getName() + ":0");
        ip_obj.setVirtualNetwork(net);
        ip_obj.setVirtualMachineInterface(vmi);
        try {
            assertTrue(_api.create(ip_obj));
            // Must perform a GET in order to update the object contents.
            assertTrue(_api.read(ip_obj));
            assertNotNull(ip_obj.getAddress());

        } catch (IOException ex) {
            fail(ex.getMessage());
        }

        //now db sync
        if (_dbSync.syncAll(DBSyncGeneric.SYNC_MODE_UPDATE) == ServerDBSync.SYNC_STATE_OUT_OF_SYNC) {
            s_logger.info("# Cloudstack DB & VNC are out of sync - resync done");
        }

        if (_dbSync.syncAll(DBSyncGeneric.SYNC_MODE_CHECK) == ServerDBSync.SYNC_STATE_OUT_OF_SYNC) {
            s_logger.info("# Cloudstack DB & VNC are still out of sync");
            fail("DB Sync failed");
        }
    }

}
