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

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.vlan.CreateVlanIpRangeCmd;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationService;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.BroadcastDomainRange;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;

public class ManagementServerMock {
    private static final Logger s_logger = Logger.getLogger(ManagementServerMock.class);

    @Inject
    private AccountManager _accountMgr;
    @Inject
    private ConfigurationService _configService;
    @Inject
    private DataCenterDao _zoneDao;
    @Inject
    private NetworkService _networkService;
    @Inject
    private NetworkDao _networksDao;
    @Inject
    private PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private VMTemplateDao _vmTemplateDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    public AgentManager _agentMgr;
    @Inject
    public VirtualMachineManager _vmMgr;

    private DataCenterVO _zone;
    private PhysicalNetwork _znet;

    private long _hostId = -1L;

    // TODO: Use the name parameter to retrieve the @Parameter annotation.
    static void setParameter(BaseCmd cmd, String name, BaseCmd.CommandType fieldType, Object value) {
        Class<?> cls = cmd.getClass();
        Field field;
        try {
            field = cls.getDeclaredField(name);
        } catch (Exception ex) {
            s_logger.warn("class: " + cls.getName() + "\t" + ex);
            return;
        }
        field.setAccessible(true);
        switch (fieldType) {
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

    private void createHost() {
        HostVO host =
            new HostVO(_hostId, "aa01", Type.BaremetalDhcp, "192.168.1.1", "255.255.255.0", null, null, null, null, null, null, null, null, null, null,
                UUID.randomUUID().toString(), Status.Up, "1.0", null, null, _zone.getId(), null, 0, 0, "aa", 0, StoragePoolType.NetworkFilesystem);
        host.setResourceState(ResourceState.Enabled);
        _hostDao.persist(host);
        _hostId = host.getId();
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

        setParameter(cmd, "accountName", BaseCmd.CommandType.STRING, system.getAccountName());
        setParameter(cmd, "domainId", BaseCmd.CommandType.LONG, Domain.ROOT_DOMAIN);
        setParameter(cmd, "startIp", BaseCmd.CommandType.STRING, "10.84.60.200");
        setParameter(cmd, "endIp", BaseCmd.CommandType.STRING, "10.84.60.250");
        setParameter(cmd, ApiConstants.GATEWAY, BaseCmd.CommandType.STRING, "10.84.60.254");
        setParameter(cmd, ApiConstants.NETMASK, BaseCmd.CommandType.STRING, "255.255.255.0");
        setParameter(cmd, "networkID", BaseCmd.CommandType.LONG, public_net_id);
        setParameter(cmd, "zoneId", BaseCmd.CommandType.LONG, _zone.getId());
        setParameter(cmd, "vlan", BaseCmd.CommandType.STRING, "untagged");
        s_logger.debug("createPublicVlanIpRange execute : zone id: " + _zone.getId() + ", public net id: " + public_net_id);
        try {
            _configService.createVlanAndPublicIpRange(cmd);
        } catch (Exception e) {
            s_logger.debug("createPublicVlanIpRange: " + e);
        }
    }

    public UserVm createVM(String name, Network network) {
        VMTemplateVO tmpl = getVMTemplate();
        assertNotNull(tmpl);
        ServiceOffering small = getServiceByName("Small Instance");
        assertNotNull(small);

        Answer<?> callback = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Commands cmds = (Commands)args[1];
                if (cmds == null) {
                    return null;
                }
                PlugNicAnswer reply = new PlugNicAnswer(null, true, "PlugNic");
                com.cloud.agent.api.Answer[] answers = {reply};
                cmds.setAnswers(answers);
                return null;
            }
        };
        try {
            Mockito.when(_agentMgr.send(Matchers.anyLong(), Matchers.any(Commands.class))).thenAnswer(callback);
        } catch (AgentUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationTimedoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        long id = _userVmDao.getNextInSequence(Long.class, "id");
        UserVmVO vm =
            new UserVmVO(id, name, name, tmpl.getId(), HypervisorType.XenServer, tmpl.getGuestOSId(), false, false, _zone.getDomainId(), Account.ACCOUNT_ID_SYSTEM,
                    1, small.getId(), null, name);
        vm.setState(com.cloud.vm.VirtualMachine.State.Running);
        vm.setHostId(_hostId);
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

    private void deleteHost() {
        _hostDao.remove(_hostId);

    }

    public void deleteVM(UserVm vm, Network network) {
        Answer<?> callback = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Commands cmds = (Commands)args[1];
                if (cmds == null) {
                    return null;
                }
                UnPlugNicAnswer reply = new UnPlugNicAnswer(null, true, "PlugNic");
                com.cloud.agent.api.Answer[] answers = {reply};
                cmds.setAnswers(answers);
                return null;
            }
        };

        try {
            Mockito.when(_agentMgr.send(Matchers.anyLong(), Matchers.any(Commands.class))).thenAnswer(callback);
        } catch (AgentUnavailableException e) {
            e.printStackTrace();
        } catch (OperationTimedoutException e) {
            e.printStackTrace();
        }

        _userVmDao.remove(vm.getId());
    }

    public void initialize(boolean oneShot) {
        locateZone();
        locatePhysicalNetwork();
        createHost();
        if (oneShot) {
            createPublicVlanIpRange();
        }
    }

    private VMTemplateVO getVMTemplate() {
        List<VMTemplateVO> tmpl_list = _vmTemplateDao.listDefaultBuiltinTemplates();
        for (VMTemplateVO tmpl : tmpl_list) {
            if (tmpl.getHypervisorType() == HypervisorType.XenServer) {
                return tmpl;
            }
        }
        return null;
    }

    private ServiceOffering getServiceByName(String name) {
        List<ServiceOfferingVO> service_list = Collections.emptyList();
        for (ServiceOfferingVO service : service_list) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    public DataCenter getZone() {
        return _zone;
    }

    private void locatePhysicalNetwork() {
        // mandatory: name, zone-id
        try {
            long id = _networkService.findPhysicalNetworkId(_zone.getId(), "znet", TrafficType.Guest);
            _znet = _networkService.getPhysicalNetwork(id);
            List<PhysicalNetworkVO> nets = _physicalNetworkDao.listByZoneAndTrafficType(_zone.getId(), TrafficType.Public);
            if (nets == null || nets.isEmpty()) {
                _networkService.addTrafficTypeToPhysicalNetwork(_znet.getId(), TrafficType.Public.toString(), "vlan", null, null, null, null, null, null, null);
            }
        } catch (InvalidParameterValueException e) {
            List<String> isolationMethods = new ArrayList<String>();
            isolationMethods.add("L3VPN");
            _znet =
                _networkService.createPhysicalNetwork(_zone.getId(), null, null, isolationMethods, BroadcastDomainRange.ZONE.toString(), _zone.getDomainId(), null,
                    "znet");
            List<PhysicalNetworkVO> nets = _physicalNetworkDao.listByZoneAndTrafficType(_zone.getId(), TrafficType.Public);
            if (nets == null || nets.isEmpty()) {
                _networkService.addTrafficTypeToPhysicalNetwork(_znet.getId(), TrafficType.Public.toString(), "vlan", null, null, null, null, null, null, null);
            }
        }
        if (_znet.getState() != PhysicalNetwork.State.Enabled) {
            _znet = _networkService.updatePhysicalNetwork(_znet.getId(), null, null, null, PhysicalNetwork.State.Enabled.toString());
        }

        // Ensure that the physical network supports Guest traffic.
        Pair<List<? extends PhysicalNetworkTrafficType>, Integer> trafficTypes = _networkService.listTrafficTypes(_znet.getId());
        boolean found = false;
        for (PhysicalNetworkTrafficType ttype : trafficTypes.first()) {
            if (ttype.getTrafficType() == TrafficType.Guest) {
                found = true;
            }
        }
        if (!found) {
            _networkService.addTrafficTypeToPhysicalNetwork(_znet.getId(), TrafficType.Guest.toString(), "vlan", null, null, null, null, null, null, null);
        }

        Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> providers =
            _networkService.listNetworkServiceProviders(_znet.getId(), Provider.JuniperContrailRouter.getName(), null, null, null);
        if (providers.second() == 0) {
            s_logger.debug("Add " + Provider.JuniperContrailRouter.getName() + " to network " + _znet.getName());
            PhysicalNetworkServiceProvider provider = _networkService.addProviderToPhysicalNetwork(_znet.getId(), Provider.JuniperContrailRouter.getName(), null, null);
            _networkService.updateNetworkServiceProvider(provider.getId(), PhysicalNetworkServiceProvider.State.Enabled.toString(), null);
        } else {
            PhysicalNetworkServiceProvider provider = providers.first().get(0);
            if (provider.getState() != PhysicalNetworkServiceProvider.State.Enabled) {
                _networkService.updateNetworkServiceProvider(provider.getId(), PhysicalNetworkServiceProvider.State.Enabled.toString(), null);
            }
        }

        providers = _networkService.listNetworkServiceProviders(_znet.getId(), null, PhysicalNetworkServiceProvider.State.Enabled.toString(), null, null);
        s_logger.debug(_znet.getName() + " has " + providers.second().toString() + " Enabled providers");
        for (PhysicalNetworkServiceProvider provider : providers.first()) {
            if (provider.getProviderName().equals(Provider.JuniperContrailRouter.getName())) {
                continue;
            }
            s_logger.debug("Disabling " + provider.getProviderName());
            _networkService.updateNetworkServiceProvider(provider.getId(), PhysicalNetworkServiceProvider.State.Disabled.toString(), null);
        }
    }

    private void locateZone() {
        _zone = _zoneDao.findByName("default");
        if (_zone == null) {
            ConfigurationManager mgr = (ConfigurationManager)_configService;
            _zone =
                mgr.createZone(User.UID_SYSTEM, "default", "8.8.8.8", null, "8.8.4.4", null, null /* cidr */, "ROOT", Domain.ROOT_DOMAIN, NetworkType.Advanced, null,
                    null /* networkDomain */, false, false, null, null);
        }
    }

    public void shutdown() {
        deleteHost();
    }
}
