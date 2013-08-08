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

package com.cloud.hypervisor.vmware;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import org.apache.cloudstack.api.command.admin.zone.AddVmwareDcCmd;
import org.apache.cloudstack.api.command.admin.zone.RemoveVmwareDcCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterVSMMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.vmware.dao.LegacyZoneDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.hypervisor.vmware.manager.VmwareManagerImpl;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.org.Managed.ManagedState;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.server.ConfigurationServer;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VmwareDatacenterApiUnitTest {

    @Inject
    VmwareDatacenterService _vmwareDatacenterService;

    @Inject
    DataCenterDao _dcDao;

    @Inject
    HostPodDao _podDao;

    @Inject
    VmwareDatacenterDao _vmwareDcDao;

    @Inject
    VmwareDatacenterZoneMapDao _vmwareDcZoneMapDao;

    @Inject
    ClusterDao _clusterDao;

    @Inject
    ClusterDetailsDao _clusterDetailsDao;

    @Inject
    ConfigurationDao _configDao;

    @Inject
    AccountDao _accountDao;

    @Inject
    AccountManager _acctMgr;

    long zoneId;
    long podId;
    long clusterId;
    long vmwareDcId;
    private static long domainId = 5L;
    private static String vmwareDcName = "dc";
    private static String clusterName = "cluster";
    private static String vCenterHost = "10.1.1.100";
    private static String url = "http://" + vCenterHost + "/" + vmwareDcName + "/" + clusterName;
    private static String user = "administrator";
    private static String password = "password";
    private static String guid = vmwareDcName + "@" + vCenterHost;

    private static VmwareDatacenterVO dc;
    private static List<VmwareDatacenterVO> vmwareDcs;
    private static ClusterVO cluster;
    private static VmwareDatacenterZoneMapVO dcZoneMap;
    private static List<ClusterVO> clusterList;
    private static ClusterDetailsVO clusterDetails;

    @Mock
    private static AddVmwareDcCmd addCmd;
    @Mock
    private static RemoveVmwareDcCmd removeCmd;

    @BeforeClass
    public static void setUp() throws ConfigurationException {
    }

    @Before
    public void testSetUp() {
        Mockito.when(_configDao.isPremium()).thenReturn(true);
        ComponentContext.initComponentsLifeCycle();
        MockitoAnnotations.initMocks(this);

        DataCenterVO zone = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24",
                null, null, NetworkType.Basic, null, null, true,  true, null, null);
        zoneId = 1L;

        HostPodVO pod = new HostPodVO(UUID.randomUUID().toString(), zoneId, "192.168.56.1", "192.168.56.0/24", 8, "test");
        podId = 1L;

        AccountVO acct = new AccountVO(200L);
        acct.setType(Account.ACCOUNT_TYPE_ADMIN);
        acct.setAccountName("admin");
        acct.setDomainId(domainId);

        UserVO user1 = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user1, acct);

        when(_accountDao.findByIdIncludingRemoved(0L)).thenReturn(acct);

        dc = new VmwareDatacenterVO(guid, vmwareDcName, vCenterHost, user, password);
        vmwareDcs = new ArrayList<VmwareDatacenterVO>();
        vmwareDcs.add(dc);
        vmwareDcId = dc.getId();

        cluster = new ClusterVO(zone.getId(), pod.getId(), "vmwarecluster");
        cluster.setHypervisorType(HypervisorType.VMware.toString());
        cluster.setClusterType(ClusterType.ExternalManaged);
        cluster.setManagedState(ManagedState.Managed);
        clusterId = 1L;
        clusterList = new ArrayList<ClusterVO>();
        clusterList.add(cluster);

        clusterDetails = new ClusterDetailsVO(clusterId, "url", url);

        dcZoneMap = new VmwareDatacenterZoneMapVO(zoneId, vmwareDcId);

        Mockito.when(_dcDao.persist(Mockito.any(DataCenterVO.class))).thenReturn(zone);
        Mockito.when(_dcDao.findById(1L)).thenReturn(zone);
        Mockito.when(_podDao.persist(Mockito.any(HostPodVO.class))).thenReturn(pod);
        Mockito.when(_podDao.findById(1L)).thenReturn(pod);
        Mockito.when(_clusterDao.persist(Mockito.any(ClusterVO.class))).thenReturn(cluster);
        Mockito.when(_clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(_clusterDao.listByZoneId(1L)).thenReturn(null);
        Mockito.when(_clusterDao.expunge(1L)).thenReturn(true);
        Mockito.when(_clusterDetailsDao.persist(Mockito.any(ClusterDetailsVO.class))).thenReturn(clusterDetails);
        Mockito.when(_clusterDetailsDao.expunge(1L)).thenReturn(true);
        Mockito.when(_vmwareDcDao.persist(Mockito.any(VmwareDatacenterVO.class))).thenReturn(dc);
        Mockito.when(_vmwareDcDao.findById(1L)).thenReturn(null);
        Mockito.when(_vmwareDcDao.expunge(1L)).thenReturn(true);
        Mockito.when(_vmwareDcDao.getVmwareDatacenterByNameAndVcenter(vmwareDcName, vCenterHost)).thenReturn(null);
        Mockito.when(_vmwareDcZoneMapDao.persist(Mockito.any(VmwareDatacenterZoneMapVO.class))).thenReturn(dcZoneMap);
        Mockito.when(_vmwareDcZoneMapDao.findByZoneId(1L)).thenReturn(null);
        Mockito.when(_vmwareDcZoneMapDao.expunge(1L)).thenReturn(true);
        Mockito.when(addCmd.getZoneId()).thenReturn(1L);
        Mockito.when(addCmd.getVcenter()).thenReturn(vCenterHost);
        Mockito.when(addCmd.getUsername()).thenReturn(user);
        Mockito.when(addCmd.getPassword()).thenReturn(password);
        Mockito.when(addCmd.getName()).thenReturn(vmwareDcName);
        Mockito.when(removeCmd.getZoneId()).thenReturn(1L);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    //@Test(expected = InvalidParameterValueException.class)
    public void testAddVmwareDcToInvalidZone() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(addCmd.getZoneId()).thenReturn(2L);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    //@Test(expected = ResourceInUseException.class)
    public void testAddVmwareDcToZoneWithClusters() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(_clusterDao.listByZoneId(1L)).thenReturn(clusterList);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRemoveVmwareDcToInvalidZone() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(removeCmd.getZoneId()).thenReturn(2L);
        _vmwareDatacenterService.removeVmwareDatacenter(removeCmd);
    }

    @Test(expected = ResourceInUseException.class)
    public void testRemoveVmwareDcToZoneWithClusters() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(_clusterDao.listByZoneId(1L)).thenReturn(clusterList);
        _vmwareDatacenterService.removeVmwareDatacenter(removeCmd);
    }

    //@Test(expected = ResourceInUseException.class)
    public void testAddVmwareDcToZoneWithVmwareDc() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(_vmwareDcDao.getVmwareDatacenterByNameAndVcenter(vmwareDcName, vCenterHost)).thenReturn(vmwareDcs);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    //@Test(expected = InvalidParameterValueException.class)
    public void testAddVmwareDcWithNullUser() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(addCmd.getUsername()).thenReturn(null);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    //@Test(expected = InvalidParameterValueException.class)
    public void testAddVmwareDcWithNullPassword() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(addCmd.getPassword()).thenReturn(null);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    //@Test(expected = InvalidParameterValueException.class)
    public void testAddVmwareDcWithNullUrl() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(addCmd.getVcenter()).thenReturn(null);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    //@Test(expected = InvalidParameterValueException.class)
    public void testAddVmwareDcWithNullDcName() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(addCmd.getName()).thenReturn(null);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    //@Test(expected = CloudRuntimeException.class)
    public void testReAddVmwareDc() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(_vmwareDcZoneMapDao.findByZoneId(1L)).thenReturn(dcZoneMap);
        _vmwareDatacenterService.addVmwareDatacenter(addCmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testRemoveNonexistingVmwareDc() throws ResourceInUseException, IllegalArgumentException, DiscoveryException, Exception {
        Mockito.when(_vmwareDcZoneMapDao.findByZoneId(1L)).thenReturn(null);
        _vmwareDatacenterService.removeVmwareDatacenter(removeCmd);
    }

    @Configuration
    @ComponentScan(basePackageClasses = {VmwareManagerImpl.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public AccountDao accountDao() {
            return Mockito.mock(AccountDao.class);
        }

        @Bean
        public AccountService accountService() {
            return Mockito.mock(AccountService.class);
        }

        @Bean
        public DataCenterDao dataCenterDao() {
            return Mockito.mock(DataCenterDao.class);
        }

        @Bean
        public HostPodDao hostPodDao() {
            return Mockito.mock(HostPodDao.class);
        }

        @Bean
        public ClusterDao clusterDao() {
            return Mockito.mock(ClusterDao.class);
        }

        @Bean
        public ClusterDetailsDao clusterDetailsDao() {
            return Mockito.mock(ClusterDetailsDao.class);
        }

        @Bean
        public VmwareDatacenterDao vmwareDatacenterDao() {
            return Mockito.mock(VmwareDatacenterDao.class);
        }

        @Bean
        public VmwareDatacenterZoneMapDao vmwareDatacenterZoneMapDao() {
            return Mockito.mock(VmwareDatacenterZoneMapDao.class);
        }

        @Bean
        public AgentManager agentManager() {
            return Mockito.mock(AgentManager.class);
        }

        @Bean
        public HostDao hostDao() {
            return Mockito.mock(HostDao.class);
        }

        @Bean
        public NetworkModel networkModel() {
            return Mockito.mock(NetworkModel.class);
        }

        @Bean
        public ClusterManager clusterManager() {
            return Mockito.mock(ClusterManager.class);
        }

        @Bean
        public SecondaryStorageVmManager secondaryStorageVmManager() {
            return Mockito.mock(SecondaryStorageVmManager.class);
        }

        @Bean
        public CommandExecLogDao commandExecLogDao() {
            return Mockito.mock(CommandExecLogDao.class);
        }

        @Bean
        public CiscoNexusVSMDeviceDao ciscoNexusVSMDeviceDao() {
            return Mockito.mock(CiscoNexusVSMDeviceDao.class);
        }

        @Bean
        public ClusterVSMMapDao clusterVSMMapDao() {
            return Mockito.mock(ClusterVSMMapDao.class);
        }

        @Bean
        public LegacyZoneDao legacyZoneDao() {
            return Mockito.mock(LegacyZoneDao.class);
        }

        @Bean
        public ConfigurationDao configurationDao() {
            return Mockito.mock(ConfigurationDao.class);
        }

        @Bean
        public ConfigurationServer configurationServer() {
            return Mockito.mock(ConfigurationServer.class);
        }

        @Bean
        public HypervisorCapabilitiesDao hypervisorCapabilitiesDao() {
            return Mockito.mock(HypervisorCapabilitiesDao.class);
        }

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public EventDao eventDao() {
            return Mockito.mock(EventDao.class);
        }

        @Bean
        public UserVmDao userVMDao() {
            return Mockito.mock(UserVmDao.class);
        }

        public AddVmwareDcCmd addVmwareDatacenterCmd() {
            return Mockito.mock(AddVmwareDcCmd.class);
        }

        public RemoveVmwareDcCmd removeVmwareDcCmd() {
            return Mockito.mock(RemoveVmwareDcCmd.class);
        }

        @Bean
        public DataStoreManager dataStoreManager() {
            return Mockito.mock(DataStoreManager.class);
        }
        public static class Library implements TypeFilter {

            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }
}
