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
package com.cloud.network.element;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.google.common.collect.Maps;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModelImpl;
import com.cloud.network.Networks;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.Ip;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

public class ConfigDriveNetworkElementTest {

    public static final String CLOUD_ID = "xx";
    public static final String PUBLIC_KEY = "publicKey";
    public static final String PASSWORD = "password";
    public static final long NETWORK_ID = 1L;
    private final long DATACENTERID = NETWORK_ID;
    private final String ZONENAME = "zone1";
    private final String VMINSTANCENAME = "i-x-y";
    private final String VMHOSTNAME = "vm-hostname";
    private final String VMOFFERING = "custom_instance";
    private final long VMID = 30L;
    private final String VMUSERDATA = "H4sIABCvw1oAAystTi1KSSxJ5AIAUPllwQkAAAA=";
    private final long SOID = 31L;
    private final long HOSTID = NETWORK_ID;

    @Mock private ConfigurationDao _configDao;
    @Mock private DataCenterDao _dcDao;
    @Mock private DataStoreManager _dataStoreMgr;
    @Mock private GuestOSCategoryDao _guestOSCategoryDao ;
    @Mock private GuestOSDao _guestOSDao;
    @Mock private HostDao _hostDao;
    @Mock private ServiceOfferingDao _serviceOfferingDao;
    @Mock private UserVmDao _vmDao;
    @Mock private VMInstanceDao _vmInstanceDao;
    @Mock private UserVmDetailsDao _userVmDetailsDao;
    @Mock private NetworkDao _networkDao;
    @Mock private NetworkServiceMapDao _ntwkSrvcDao;
    @Mock private IPAddressDao _ipAddressDao;

    @Mock private DataCenterVO dataCenterVO;
    @Mock private DataStore dataStore;
    @Mock private DeployDestination deployDestination;
    @Mock private EndPoint endpoint;
    @Mock private EndPointSelector _ep;
    @Mock private GuestOSCategoryVO guestOSCategoryVo;
    @Mock private GuestOSVO guestOSVO;
    @Mock private HostVO hostVO;
    @Mock private NetworkVO network;
    @Mock private Nic nic;
    @Mock private NicProfile nicp;
    @Mock private ServiceOfferingVO serviceOfferingVO;
    @Mock private UserVmVO virtualMachine;
    @Mock private IPAddressVO publicIp;

    @InjectMocks private final ConfigDriveNetworkElement _configDrivesNetworkElement = new ConfigDriveNetworkElement();
    @InjectMocks @Spy private NetworkModelImpl _networkModel = new NetworkModelImpl();

    @org.junit.Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);

        _configDrivesNetworkElement._networkModel = _networkModel;

        when(_dataStoreMgr.getImageStore(DATACENTERID)).thenReturn(dataStore);
        when(_ep.select(dataStore)).thenReturn(endpoint);
        when(_vmDao.findById(VMID)).thenReturn(virtualMachine);
        when(_dcDao.findById(DATACENTERID)).thenReturn(dataCenterVO);
        when(_hostDao.findById(HOSTID)).thenReturn(hostVO);
        doReturn(nic).when(_networkModel).getDefaultNic(VMID);
        when(_serviceOfferingDao.findByIdIncludingRemoved(VMID, SOID)).thenReturn(serviceOfferingVO);
        when(_guestOSDao.findById(anyLong())).thenReturn(guestOSVO);
        when(_guestOSCategoryDao.findById(anyLong())).thenReturn(guestOSCategoryVo);
        when(_configDao.getValue("cloud.identifier")).thenReturn(CLOUD_ID);
        when(network.getDataCenterId()).thenReturn(DATACENTERID);
        when(guestOSCategoryVo.getName()).thenReturn("Linux");
        when(dataCenterVO.getName()).thenReturn(ZONENAME);
        when(serviceOfferingVO.getDisplayText()).thenReturn(VMOFFERING);
        when(guestOSVO.getCategoryId()).thenReturn(0L);
        when(virtualMachine.getGuestOSId()).thenReturn(0L);
        when(virtualMachine.getType()).thenReturn(VirtualMachine.Type.User);
        when(virtualMachine.getId()).thenReturn(VMID);
        when(virtualMachine.getServiceOfferingId()).thenReturn(SOID);
        when(virtualMachine.getDataCenterId()).thenReturn(DATACENTERID);
        when(virtualMachine.getInstanceName()).thenReturn(VMINSTANCENAME);
        when(virtualMachine.getUserData()).thenReturn(VMUSERDATA);
        when(virtualMachine.getHostName()).thenReturn(VMHOSTNAME);
        when(deployDestination.getHost()).thenReturn(hostVO);
        when(hostVO.getId()).thenReturn(HOSTID);
        when(nic.isDefaultNic()).thenReturn(true);
        when(nic.getNetworkId()).thenReturn(NETWORK_ID);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(_networkModel.getNetwork(NETWORK_ID)).thenReturn(network);
        //when(_networkModel.getUserDataUpdateProvider(network)).thenReturn(_configDrivesNetworkElement);

        when(_ntwkSrvcDao.getProviderForServiceInNetwork(NETWORK_ID, Network.Service.UserData)).thenReturn(_configDrivesNetworkElement.getProvider().getName());

        _networkModel.setNetworkElements(Arrays.asList(_configDrivesNetworkElement));
        _networkModel.start();

    }

    @Test
    public void testCanHandle() throws InsufficientCapacityException, ResourceUnavailableException {
        final NetworkOfferingVO ntwkoffer = mock(NetworkOfferingVO.class);
        when(ntwkoffer.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        assertTrue(_configDrivesNetworkElement.implement(null, ntwkoffer, null,null));

        when(ntwkoffer.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        assertFalse(_configDrivesNetworkElement.implement(null, ntwkoffer, null, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExpunge() throws NoTransitionException, NoSuchFieldException, IllegalAccessException {
        final StateMachine2<VirtualMachine.State, VirtualMachine.Event, VirtualMachine> stateMachine = VirtualMachine.State.getStateMachine();

        final Field listenersField = StateMachine2.class.getDeclaredField("_listeners");
        listenersField.setAccessible(true);
        List<StateListener<VirtualMachine.State, VirtualMachine.Event, VirtualMachine>> listeners =
                (List<StateListener<VirtualMachine.State, VirtualMachine.Event, VirtualMachine>>)listenersField.get(stateMachine);

        listeners.clear();

        _configDrivesNetworkElement.start();

        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(_vmInstanceDao.updateState(VirtualMachine.State.Stopped, VirtualMachine.Event.ExpungeOperation, VirtualMachine.State.Expunging, virtualMachine, null)).thenReturn(true);

        final Answer answer = mock(Answer.class);
        when(endpoint.sendMessage(any(HandleConfigDriveIsoCommand.class))).thenReturn(answer);
        when(answer.getResult()).thenReturn(true);

        stateMachine.transitTo(virtualMachine, VirtualMachine.Event.ExpungeOperation, null, _vmInstanceDao);

        ArgumentCaptor<HandleConfigDriveIsoCommand> commandCaptor = ArgumentCaptor.forClass(HandleConfigDriveIsoCommand.class);
        verify(endpoint, times(1)).sendMessage(commandCaptor.capture());
        HandleConfigDriveIsoCommand deleteCommand = commandCaptor.getValue();

        assertThat(deleteCommand.isCreate(), is(false));
        assertThat(deleteCommand.isUpdate(), is(false));


    }

    @Test
    public void testRelease() {
        final Answer answer = mock(Answer.class);
        when(endpoint.sendMessage(any(HandleConfigDriveIsoCommand.class))).thenReturn(answer);
        when(answer.getResult()).thenReturn(true);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(virtualMachine, null, serviceOfferingVO, null, null);
        assertTrue(_configDrivesNetworkElement.release(network, nicp, profile, null));
    }

    @Test
    public void testGetCapabilities () {
        assertThat(_configDrivesNetworkElement.getCapabilities(), hasEntry(Network.Service.UserData, null));
    }

    @Test
    public void testAddPasswordAndUserdata() throws InsufficientCapacityException, ResourceUnavailableException {
        List<String[]> actualVmData = getVmData();

        assertThat(actualVmData, containsInAnyOrder(
                new String[]{"userdata", "user_data", VMUSERDATA},
                new String[]{"metadata", "service-offering", VMOFFERING},
                new String[]{"metadata", "availability-zone", ZONENAME},
                new String[]{"metadata", "local-hostname", VMHOSTNAME},
                new String[]{"metadata", "local-ipv4", "192.168.111.111"},
                new String[]{"metadata", "public-hostname", null},
                new String[]{"metadata", "public-ipv4", "192.168.111.111"},
                new String[]{"metadata", "vm-id", String.valueOf(VMID)},
                new String[]{"metadata", "instance-id", VMINSTANCENAME},
                new String[]{"metadata", "public-keys", PUBLIC_KEY},
                new String[]{"metadata", "cloud-identifier", String.format("CloudStack-{%s}", CLOUD_ID)},
                new String[]{PASSWORD, "vm_password", PASSWORD}
        ));
    }

    @Test
    public void testAddPasswordAndUserdataStaticNat() throws InsufficientCapacityException, ResourceUnavailableException {
        when(_ipAddressDao.findByAssociatedVmId(VMID)).thenReturn(publicIp);
        when(publicIp.getAddress()).thenReturn(new Ip("7.7.7.7"));

        List<String[]> actualVmData = getVmData();

        assertThat(actualVmData, containsInAnyOrder(
                new String[]{"userdata", "user_data", VMUSERDATA},
                new String[]{"metadata", "service-offering", VMOFFERING},
                new String[]{"metadata", "availability-zone", ZONENAME},
                new String[]{"metadata", "local-hostname", VMHOSTNAME},
                new String[]{"metadata", "local-ipv4", "192.168.111.111"},
                new String[]{"metadata", "public-hostname", "7.7.7.7"},
                new String[]{"metadata", "public-ipv4", "7.7.7.7"},
                new String[]{"metadata", "vm-id", String.valueOf(VMID)},
                new String[]{"metadata", "instance-id", VMINSTANCENAME},
                new String[]{"metadata", "public-keys", PUBLIC_KEY},
                new String[]{"metadata", "cloud-identifier", String.format("CloudStack-{%s}", CLOUD_ID)},
                new String[]{PASSWORD, "vm_password", PASSWORD}
        ));
    }


    @Test
    public void testAddPasswordAndUserdataUuid() throws InsufficientCapacityException, ResourceUnavailableException {
        when(virtualMachine.getUuid()).thenReturn("vm-uuid");

        List<String[]> actualVmData = getVmData();

        assertThat(actualVmData, containsInAnyOrder(
                new String[]{"userdata", "user_data", VMUSERDATA},
                new String[]{"metadata", "service-offering", VMOFFERING},
                new String[]{"metadata", "availability-zone", ZONENAME},
                new String[]{"metadata", "local-hostname", VMHOSTNAME},
                new String[]{"metadata", "local-ipv4", "192.168.111.111"},
                new String[]{"metadata", "public-hostname", null},
                new String[]{"metadata", "public-ipv4", "192.168.111.111"},
                new String[]{"metadata", "vm-id", "vm-uuid"},
                new String[]{"metadata", "instance-id", "vm-uuid"},
                new String[]{"metadata", "public-keys", PUBLIC_KEY},
                new String[]{"metadata", "cloud-identifier", String.format("CloudStack-{%s}", CLOUD_ID)},
                new String[]{PASSWORD, "vm_password", PASSWORD}
        ));
    }

    private List<String[]> getVmData() throws InsufficientCapacityException, ResourceUnavailableException {
        final Answer answer = mock(Answer.class);
        final UserVmDetailVO userVmDetailVO = mock(UserVmDetailVO.class);
        when(endpoint.sendMessage(any(HandleConfigDriveIsoCommand.class))).thenReturn(answer);
        when(answer.getResult()).thenReturn(true);
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(userVmDetailVO.getValue()).thenReturn(PUBLIC_KEY);
        when(nicp.getIPv4Address()).thenReturn("192.168.111.111");
        when(_userVmDetailsDao.findDetail(anyLong(), anyString())).thenReturn(userVmDetailVO);
        Map<VirtualMachineProfile.Param, Object> parms = Maps.newHashMap();
        parms.put(VirtualMachineProfile.Param.VmPassword, PASSWORD);
        parms.put(VirtualMachineProfile.Param.VmSshPubKey, PUBLIC_KEY);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(virtualMachine, null, serviceOfferingVO, null, parms);
        assertTrue(_configDrivesNetworkElement.addPasswordAndUserdata(
                network, nicp, profile, deployDestination, null));

        ArgumentCaptor<HandleConfigDriveIsoCommand> commandCaptor = ArgumentCaptor.forClass(HandleConfigDriveIsoCommand.class);
        verify(endpoint, times(1)).sendMessage(commandCaptor.capture());
        HandleConfigDriveIsoCommand result = commandCaptor.getValue();
        return result.getVmData();
    }
}
