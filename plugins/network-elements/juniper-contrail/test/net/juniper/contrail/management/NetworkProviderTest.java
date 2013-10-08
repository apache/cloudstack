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

package net.juniper.contrail.management;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import junit.framework.TestCase;
import net.juniper.contrail.management.ContrailManager;
import com.cloud.offering.NetworkOffering;
import net.juniper.contrail.management.ServerDBSync;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorMock;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;
import net.juniper.contrail.api.types.NetworkPolicy;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VnSubnetsType;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.project.CreateProjectCmd;
import org.apache.cloudstack.api.command.user.project.DeleteProjectCmd;
import org.apache.cloudstack.api.command.user.address.AssociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.nat.EnableStaticNatCmd;
import org.apache.cloudstack.api.command.user.nat.DisableStaticNatCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.admin.vlan.CreateVlanIpRangeCmd;
import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import com.cloud.utils.db.Merovingian2;
import com.cloud.utils.mgmt.JmxUtil;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationService;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.network.Network.Provider;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.BroadcastDomainRange;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import org.apache.cloudstack.context.CallContext;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/serviceContext.xml")
/**
 * Exercise the public API.
 */
public class NetworkProviderTest extends TestCase {
    private static final Logger s_logger =
            Logger.getLogger(NetworkProviderTest.class);

    @Inject public ConfigurationService _configService;
    @Inject public DataCenterDao _zoneDao;

    @Inject public NetworkService _networkService;
    @Inject public ContrailManager _contrailMgr;
    @Inject public ServerDBSync _dbSync;
    @Inject public AccountManager _accountMgr;
    @Inject NetworkDao _networksDao;
    @Inject PhysicalNetworkDao _physicalNetworkDao;
    @Inject IPAddressDao _ipAddressDao;

    @Inject public VirtualMachineManager _vmMgr;

    private DataCenterVO _zone;
    private PhysicalNetwork _znet;
    @Inject public UserVmDao _userVmDao;
    @Inject public ServiceOfferingDao _serviceOfferingDao;
    @Inject public VMTemplateDao _vmTemplateDao;
    @Inject public DomainDao _domainDao;
    @Inject public ProjectDao _projectDao;
    @Inject public HostDao _hostDao;
    @Inject public AgentManager _agentMgr;
    private ApiConnector _api;
    private long _host_id = -1L;
    private static int _mysql_server_port;
    private static long _msId;
    private static Merovingian2 _lockMaster;
    public static boolean initDone = false;

    @BeforeClass
    public static void globalSetUp() throws Exception {

        s_logger.info("mysql server is getting launched ");
        _mysql_server_port = TestDbSetup.init(null);
        s_logger.info("mysql server launched on port " + _mysql_server_port);
        _msId = ManagementServerNode.getManagementServerId();
        _lockMaster = Merovingian2.createLockMaster(_msId);
    }

    @AfterClass
    public static void globalTearDown() throws Exception {

        _lockMaster.cleanupForServer(_msId);
        JmxUtil.unregisterMBean("Locks", "Locks");
        _lockMaster = null;
        Thread.sleep(3000);

        s_logger.info("destroying mysql server instance running at port <" + _mysql_server_port + ">");
        TestDbSetup.destroy(_mysql_server_port, null);
    }

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
        LocateZone();
        LocatePhysicalNetwork();
        LocateHost();
        _api = _contrailMgr.getApiConnector();
        if (NetworkProviderTest.initDone == false) {
            createPublicVlanIpRange();
            NetworkProviderTest.initDone = true;
        }
    }

    @After
    public void tearDown() throws Exception {
        DeleteHost();
    }

    // TODO: Use the name parameter to retrieve the @Parameter annotation.
    private void SetParameter(BaseCmd cmd, String name, BaseCmd.CommandType field_type, Object value) {
        Class<?> cls = cmd.getClass();
        Field field;
        try {
            field = cls.getDeclaredField(name);
        } catch (Exception ex) {
            s_logger.warn("class: " + cls.getName() + "\t" + ex);
            return;
        }
        field.setAccessible(true);
        switch (field_type) {
        case STRING:
            try {
                field.set(cmd, value);
            } catch (Exception ex) {
                s_logger.warn(ex);
                return;
            }
            break;
        case UUID:
            if (value.equals("-1")) {
                try {
                    field.setLong(cmd, -1L);
                } catch (Exception ex) {
                    s_logger.warn(ex);
                    return;
                }
            }
            break;
        case LONG:
            try {
                field.set(cmd, value);
            } catch (Exception ex) {
                s_logger.warn(ex);
                return;
            }
            break;
        default:
            try {
                field.set(cmd, value);
            } catch (Exception ex) {
                s_logger.warn(ex);
                return;
            }
            break;
        }
    }

    private void LocateZone() {
        _zone = _zoneDao.findByName("default");
        if (_zone == null) {
            ConfigurationManager mgr = (ConfigurationManager) _configService;
            _zone = mgr.createZone(User.UID_SYSTEM, "default", "8.8.8.8", null, "8.8.4.4", null,
                    null /* cidr */, "ROOT", Domain.ROOT_DOMAIN,
                    NetworkType.Advanced, null, null /* networkDomain */, false, false, null, null);
        }
    }
    private void LocatePhysicalNetwork() {
        // mandatory: name, zone-id
        try {
            long id = _networkService.findPhysicalNetworkId(_zone.getId(), "znet", TrafficType.Guest);
            _znet = _networkService.getPhysicalNetwork(id);
            List<PhysicalNetworkVO> nets = _physicalNetworkDao.listByZoneAndTrafficType(_zone.getId(), TrafficType.Public);
            if (nets == null || nets.isEmpty()) {
                _networkService.addTrafficTypeToPhysicalNetwork(_znet.getId(), TrafficType.Public.toString(), null, null, null, null, null);
            }
        } catch (InvalidParameterValueException e) {
            List<String> isolationMethods = new ArrayList<String>();
            isolationMethods.add("GRE");
            _znet = _networkService.createPhysicalNetwork(_zone.getId(), null, null, isolationMethods,
                    BroadcastDomainRange.ZONE.toString(), _zone.getDomainId(),
                    null, "znet");
            List<PhysicalNetworkVO> nets = _physicalNetworkDao.listByZoneAndTrafficType(_zone.getId(), TrafficType.Public);
            if (nets == null || nets.isEmpty()) {
                _networkService.addTrafficTypeToPhysicalNetwork(_znet.getId(), TrafficType.Public.toString(), null, null, null, null, null);
            }
        }
        if (_znet.getState() != PhysicalNetwork.State.Enabled) {
            _znet = _networkService.updatePhysicalNetwork(_znet.getId(), null, null, null,
                    PhysicalNetwork.State.Enabled.toString());
        }

        // Ensure that the physical network supports Guest traffic.
        Pair<List<? extends PhysicalNetworkTrafficType>, Integer> trafficTypes =
                _networkService.listTrafficTypes(_znet.getId());
        boolean found = false;
        for (PhysicalNetworkTrafficType ttype: trafficTypes.first()) {
            if (ttype.getTrafficType() == TrafficType.Guest) {
                found = true;
            }
        }
        if (!found) {
            _networkService.addTrafficTypeToPhysicalNetwork(_znet.getId(), TrafficType.Guest.toString(),
                    null, null, null, null, null);
        }

        Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> providers =
                _networkService.listNetworkServiceProviders(_znet.getId(), Provider.JuniperContrail.getName(),
                        null, null, null);
        if (providers.second() == 0) {
            s_logger.debug("Add " + Provider.JuniperContrail.getName() + " to network " + _znet.getName());
            PhysicalNetworkServiceProvider provider =
                    _networkService.addProviderToPhysicalNetwork(_znet.getId(), Provider.JuniperContrail.getName(),
                            null, null);
            _networkService.updateNetworkServiceProvider(provider.getId(),
                    PhysicalNetworkServiceProvider.State.Enabled.toString(), null);
        } else {
            PhysicalNetworkServiceProvider provider = providers.first().get(0);
            if (provider.getState() != PhysicalNetworkServiceProvider.State.Enabled) {
                _networkService.updateNetworkServiceProvider(provider.getId(),
                        PhysicalNetworkServiceProvider.State.Enabled.toString(), null); 
            }
        }

        providers = _networkService.listNetworkServiceProviders(_znet.getId(), null,
                PhysicalNetworkServiceProvider.State.Enabled.toString(), null, null);
        s_logger.debug(_znet.getName() + " has " + providers.second().toString() + " Enabled providers");
        for (PhysicalNetworkServiceProvider provider: providers.first()) {
            if (provider.getProviderName().equals(Provider.JuniperContrail.getName())) {
                continue;
            }
            s_logger.debug("Disabling " + provider.getProviderName());
            _networkService.updateNetworkServiceProvider(provider.getId(),
                    PhysicalNetworkServiceProvider.State.Disabled.toString(), null);
        }
    }

    private void LocateHost() {
        HostVO host = new HostVO(_host_id, "aa01", Type.BaremetalDhcp,
                "192.168.1.1", "255.255.255.0", null,
                null, null, null,
                null, null, null,
                null, null, null,
                UUID.randomUUID().toString(), Status.Up, "1.0", null,
                null, _zone.getId(), null, 0, 0, "aa", 0, StoragePoolType.NetworkFilesystem);
        host.setResourceState(ResourceState.Enabled);
        _hostDao.persist(host);
        _host_id = host.getId();
    }

    private void DeleteHost() {
        _hostDao.remove(_host_id);

    }
    private void purgeTestNetwork() {
        Account system = _accountMgr.getSystemAccount();
        List<? extends Network> list =
                _networkService.getIsolatedNetworksOwnedByAccountInZone(_zone.getId(), system);
        for (Network net : list) {
            s_logger.debug("Delete network " + net.getName());
            _networkService.deleteNetwork(net.getId());
        }
    }

    private Network lookupTestNetwork(String name) {
        Account system = _accountMgr.getSystemAccount();
        List<? extends Network> list =
                _networkService.getIsolatedNetworksOwnedByAccountInZone(_zone.getId(), system);
        for (Network net : list) {
            if (net.getName().equals(name)) {
                return net;
            }
        }
        return null;
    }

    private Network createTestNetwork(String name) {
        CreateNetworkCmd cmd = new CreateNetworkCmd();
        BaseCmd proxy = ComponentContext.inject(cmd);
        Account system = _accountMgr.getSystemAccount();

        SetParameter(cmd, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        SetParameter(cmd, ApiConstants.NAME, BaseCmd.CommandType.STRING, name);
        SetParameter(cmd, "displayText", BaseCmd.CommandType.STRING, "test network");
        SetParameter(cmd, "networkOfferingId", BaseCmd.CommandType.LONG, _contrailMgr.getOffering().getId());
        SetParameter(cmd, "zoneId", BaseCmd.CommandType.LONG, _zone.getId());
        SetParameter(cmd, ApiConstants.GATEWAY, BaseCmd.CommandType.STRING, "10.0.1.254");
        SetParameter(cmd, ApiConstants.NETMASK, BaseCmd.CommandType.STRING, "255.255.255.0");
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

    private void createPublicVlanIpRange() {
        CreateVlanIpRangeCmd cmd = new CreateVlanIpRangeCmd();
        BaseCmd proxy = ComponentContext.inject(cmd);
        Long public_net_id = null;

        List<NetworkVO> nets = _networksDao.listByZoneAndTrafficType(_zone.getId(), TrafficType.Public);
        if (nets != null && !nets.isEmpty()) {
            NetworkVO public_net = nets.get(0); 
            public_net_id = public_net.getId();
        } else {
            s_logger.debug("no public network found in the zone: " + _zone.getId());
        }
        Account system = _accountMgr.getSystemAccount();

        SetParameter(cmd, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        SetParameter(cmd, "domainId", BaseCmd.CommandType.LONG, Domain.ROOT_DOMAIN);
        SetParameter(cmd, "startIp", BaseCmd.CommandType.STRING, "10.84.60.200");
        SetParameter(cmd, "endIp", BaseCmd.CommandType.STRING, "10.84.60.250");
        SetParameter(cmd, ApiConstants.GATEWAY, BaseCmd.CommandType.STRING, "10.84.60.254");
        SetParameter(cmd, ApiConstants.NETMASK, BaseCmd.CommandType.STRING, "255.255.255.0");
        SetParameter(cmd, "networkID", BaseCmd.CommandType.LONG, public_net_id);
        SetParameter(cmd, "zoneId", BaseCmd.CommandType.LONG, _zone.getId());
        //SetParameter(cmd, "forVirtualNetwork", BaseCmd.CommandType.BOOLEAN, true);
        SetParameter(cmd, "vlan", BaseCmd.CommandType.STRING, "untagged");
        s_logger.debug("createPublicVlanIpRange execute : zone id: " + _zone.getId() + ", public net id: " + public_net_id);
        try {
           _configService.createVlanAndPublicIpRange(cmd);
        } catch (Exception e) {
           s_logger.debug("createPublicVlanIpRange: " + e);
        }
    }

    @Test
    //@Ignore
    public void testCreateNetwork() {
        purgeTestNetwork();
        createTestNetwork("test");
    }

    private VMTemplateVO getVMTemplate() {
        List<VMTemplateVO> tmpl_list = _vmTemplateDao.listDefaultBuiltinTemplates();
        for (VMTemplateVO tmpl: tmpl_list) {
            if (tmpl.getHypervisorType() == HypervisorType.XenServer) {
                return tmpl;
            }
        }    
        return null;
    }

    private ServiceOffering getServiceByName(String name) {
        List<ServiceOfferingVO> service_list = _serviceOfferingDao.findPublicServiceOfferings();
        for (ServiceOfferingVO service: service_list) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    private UserVm createVM(String name, Network network) {
        VMTemplateVO tmpl = getVMTemplate();
        assertNotNull(tmpl);
        ServiceOffering small = getServiceByName("Small Instance");
        assertNotNull(small);

        Answer<?> callback = new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Commands cmds = (Commands) args[1];
                if (cmds == null) {
                    return null;
                }
                PlugNicAnswer reply = new PlugNicAnswer(null, true, "PlugNic");
                com.cloud.agent.api.Answer[] answers = { reply };
                cmds.setAnswers(answers);
                return null;
            }
        };
        try {
            Mockito.when(_agentMgr.send(Mockito.anyLong(), Mockito.any(Commands.class))).thenAnswer(callback);
        } catch (AgentUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationTimedoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        long id = _userVmDao.getNextInSequence(Long.class, "id");
        UserVmVO vm = new UserVmVO(id, name, name, tmpl.getId(), HypervisorType.XenServer, tmpl.getGuestOSId(),
                false, false, _zone.getDomainId(), Account.ACCOUNT_ID_SYSTEM, small.getId(), null, name, null);
        vm.setState(State.Running);
        vm.setHostId(_host_id);
        vm.setDataCenterId(network.getDataCenterId());
        _userVmDao.persist(vm);

        NicProfile profile = new NicProfile();
        try {
            _vmMgr.addVmToNetwork(vm, network, profile);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            //ex.printStackTrace();
        }
        return vm;
    }

    private void deleteVM(UserVm vm, Network network) {
        Answer<?> callback = new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Commands cmds = (Commands) args[1];
                if (cmds == null) {
                    return null;
                }
                UnPlugNicAnswer reply = new UnPlugNicAnswer(null, true, "PlugNic");
                com.cloud.agent.api.Answer[] answers = { reply };
                cmds.setAnswers(answers);
                return null;
            }
        };

        try {
            Mockito.when(_agentMgr.send(Mockito.anyLong(), Mockito.any(Commands.class))).thenAnswer(callback);
        } catch (AgentUnavailableException e) {
            e.printStackTrace();
        } catch (OperationTimedoutException e) {
            e.printStackTrace();
        }

/*
        try {
             if (network == null) {
                s_logger.debug("network is NULL");
             } else {
                s_logger.debug("network data center: " + network.getDataCenterId());
             }
            //_vmMgr.removeVmFromNetwork(vm, network, null);
        } catch (ConcurrentOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
*/
        _userVmDao.remove(vm.getId());
    }

    @Test
    public void testConnectivity() {
        Network network = lookupTestNetwork("test");
        if (network == null) {
            network = createTestNetwork("test");
        }
        UserVm vm1 = createVM("x01", network);
        UserVm vm2 = createVM("x02", network);

        deleteVM(vm1, network);
        deleteVM(vm2, network);
    }

    @Test
    public void floatingIpTest() {
        Network network = lookupTestNetwork("test-fip-net");
        if (network == null) {
            network = createTestNetwork("test-fip-net");
        }
        UserVm vm = createVM("test-fip-vm", network);
        try {
            IPAddressVO ip = createFloatingIp(network, vm);
            deleteFloatingIp(ip);
        } catch (Exception e) {
            fail("unable to create/delete floating ip");
        }
        deleteVM(vm, network);
    }

    public void deleteFloatingIp(IPAddressVO ip) throws Exception{
        BaseCmd cmd = new DisableStaticNatCmd();
        BaseCmd proxy = ComponentContext.inject(cmd);
        SetParameter(proxy, "ipAddressId", BaseCmd.CommandType.LONG, ip.getId());
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

        SetParameter(proxy, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        SetParameter(proxy, "domainId", BaseCmd.CommandType.LONG, Domain.ROOT_DOMAIN);
        SetParameter(proxy, "zoneId", BaseCmd.CommandType.LONG, _zone.getId());
        SetParameter(proxy, "networkId", BaseCmd.CommandType.LONG, network.getId());
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
        SetParameter(proxy, "ipAddressId", BaseCmd.CommandType.LONG, publicIps.get(0).getId());
        SetParameter(proxy, "networkId", BaseCmd.CommandType.LONG, network.getId());
        SetParameter(proxy, "virtualMachineId", BaseCmd.CommandType.LONG, vm.getId());

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

        SetParameter(proxy, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        SetParameter(proxy, "domainId", BaseCmd.CommandType.LONG, Domain.ROOT_DOMAIN);
        SetParameter(proxy, "name", BaseCmd.CommandType.STRING, name);
        SetParameter(proxy, "displayText", BaseCmd.CommandType.STRING, name);
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
            net.juniper.contrail.api.types.Domain vncDomain = (net.juniper.contrail.api.types.Domain)
                    _api.findById(net.juniper.contrail.api.types.Domain.class, domain.getUuid());
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
            SetParameter(proxy, "id", BaseCmd.CommandType.LONG, project.getId());
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
        UserVm vm = createVM("test-db-only-vm", network);
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
            ipam = (NetworkIpam) _api.findById(NetworkIpam.class, ipam_id);
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
