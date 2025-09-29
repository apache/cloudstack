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
package com.cloud.configuration;

import com.cloud.alert.AlertManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetrisProviderDao;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.NsxProviderVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.command.admin.config.ResetCfgCmd;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.zone.DeleteZoneCmd;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.resourcedetail.DiskOfferingDetailVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.vm.UnmanagedVMsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerImplTest {

    @InjectMocks
    @Spy
    ConfigurationManagerImpl configurationManagerImplSpy;
    @Mock
    ConfigDepot configDepot;
    @Mock
    SearchCriteria<DiskOfferingDetailVO> searchCriteriaDiskOfferingDetailMock;
    @Mock
    DiskOffering diskOfferingMock;
    @Mock
    Account accountMock;
    @Mock
    User userMock;
    @Mock
    Domain domainMock;
    @Mock
    DomainDao domainDaoMock;
    @Mock
    EntityManager entityManagerMock;
    @Mock
    ConfigurationVO configurationVOMock;
    @Mock
    ConfigKey configKeyMock;
    @Spy
    DiskOfferingVO diskOfferingVOSpy;
    @Mock
    UpdateDiskOfferingCmd updateDiskOfferingCmdMock;
    @Mock
    NsxProviderDao nsxProviderDao;
    @Mock
    NetrisProviderDao netrisProviderDao;
    @Mock
    DataCenterDao zoneDao;
    @Mock
    HostDao hostDao;
    @Mock
    HostPodDao podDao;
    @Mock
    DataCenterIpAddressDao ipAddressDao;
    @Mock
    IPAddressDao publicIpAddressDao;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    ImageStoreDao imageStoreDao;
    @Mock
    VlanDao vlanDao;
    @Mock
    VMTemplateZoneDao vmTemplateZoneDao;
    @Mock
    CapacityDao capacityDao;
    @Mock
    DedicatedResourceDao dedicatedResourceDao;
    @Mock
    AnnotationDao annotationDao;
    @Mock
    ConfigurationDao configDao;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    NetworkService networkService;
    @Mock
    NetworkModel networkModel;
    @Mock
    PrimaryDataStoreDao storagePoolDao;
    @Mock
    StoragePoolDetailsDao storagePoolDetailsDao;

    DeleteZoneCmd deleteZoneCmd;
    CreateNetworkOfferingCmd createNetworkOfferingCmd;

    Long validId = 1L;
    Long invalidId = 100L;
    List<Long> filteredZoneIds = List.of(1L, 2L, 3L);
    List<Long> existingZoneIds = List.of(1L, 2L, 3L);
    List<Long> filteredDomainIds = List.of(1L, 2L, 3L);
    List<Long> existingDomainIds = List.of(1L, 2L, 3L);
    List<Long> emptyExistingZoneIds = new ArrayList<>();
    List<Long> emptyExistingDomainIds = new ArrayList<>();
    List<Long> emptyFilteredDomainIds = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        Mockito.when(configurationVOMock.getScopes()).thenReturn(List.of(ConfigKey.Scope.Global));
        Mockito.when(configDao.findByName(Mockito.anyString())).thenReturn(configurationVOMock);
        Mockito.when(configDepot.get(Mockito.anyString())).thenReturn(configKeyMock);

        configurationManagerImplSpy.populateConfigValuesForValidationSet();
        configurationManagerImplSpy.weightBasedParametersForValidation();
        configurationManagerImplSpy.overProvisioningFactorsForValidation();
        configurationManagerImplSpy.populateConfigKeysAllowedOnlyForDefaultAdmin();
        ReflectionTestUtils.setField(configurationManagerImplSpy, "templateZoneDao", vmTemplateZoneDao);
        ReflectionTestUtils.setField(configurationManagerImplSpy, "annotationDao", annotationDao);

        deleteZoneCmd = Mockito.mock(DeleteZoneCmd.class);
        createNetworkOfferingCmd = Mockito.mock(CreateNetworkOfferingCmd.class);
    }

    @Test
    public void validateIfIntValueIsInRangeTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "3", "1-5");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateIfIntValueIsInRangeTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "9", "1-5");
        Assert.assertNotNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestValidValuesReturnNull() {
        String testVariable;
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateIfStringValueIsInRangeTestInvalidValuesReturnString() {
        String testVariable;
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertEquals("The provided value is not returnMsg.", testVariable);
        }
    }


    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesValidValueReturnNull() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesInvalidValueReturnMessages() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg2").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg3").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertEquals("The provided value is neither returnMsg1 NOR returnMsg2 NOR returnMsg3.", testVariable);
    }


    @Test
    public void validateRangePrivateIpTestValidValueReturnNull() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(true);
            String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateRangePrivateIpTestInvalidValueReturnString() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(false);
            String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
            Assert.assertEquals("a valid site local IP address", testVariable);
        }
    }

    @Test
    public void validateRangeHypervisorListTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeHypervisorListTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware,Any,InvalidHypervisorName");
        Assert.assertEquals("a valid hypervisor type", testVariable);
    }

    @Test
    public void validateRangeInstanceNameTestValidValueReturnNull() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(true);
            String testVariable = configurationManagerImplSpy.validateRangeInstanceName("ThisStringShouldBeValid");
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateRangeInstanceNameTestInvalidValueReturnString() {
        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(false);
            String testVariable = configurationManagerImplSpy.validateRangeInstanceName("This string should not be valid.");
            Assert.assertEquals("a valid instance name (instance names cannot contain hyphens, spaces or plus signs)", testVariable);
        }
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringShould.Work");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotStartWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeast" +
                "OnePeriodEverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXhNOBNTNAoMCQqJMzcvFSBwHUhmWHftjfTNUaHR");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsNotAValidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueReturnNull() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithAStarAndAPeriod.ThisShouldWorkEvenThoughItIsOverTwoHundredAnd" +
                "ThirtyEight.CharactersLong.BecauseTheFirstTwoCharactersAreIgnored.TheRestOfThisStringWasRandomlyGenerated.MgTUerXPlLyMaUpKTjAhxasFYRCfNCXmtWDwqSDOcTjASWlAXS");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeastOnePeriod" +
                "EverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXNOBNTNAoMChQqJMzcvFSBwHUhmWHftjfTNUaHRKVyXm");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsNotAValidValueReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeOtherTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "SoShouldThis", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeOtherTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "ThisShouldNotWork", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNotNull(testVariable);
    }

    @Test
    public void testValidateIpAddressRelatedConfigValuesUnrelated() {
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues(StorageManager.PreferredStoragePool.key(), "something");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "");
        Mockito.when(configurationManagerImplSpy._configDepot.get("config.ip")).thenReturn(null);
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "something");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues(StorageManager.MountDisabledStoragePool.key(), "false");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateIpAddressRelatedConfigValuesInvalidIp() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.ip");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "abcdefg");
    }

    @Test
    public void testValidateIpAddressRelatedConfigValuesValidIp() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.ip");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.ip", "192.168.1.1");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateIpAddressRelatedConfigValuesInvalidIpRange() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type. RemoteAccessVpnManagerImpl.RemoteAccessVpnClientIpRange not accessible here
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.iprange");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.iprange", "xyz-192.168.1.20");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateIpAddressRelatedConfigValuesInvalidIpRange1() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type. RemoteAccessVpnManagerImpl.RemoteAccessVpnClientIpRange not accessible here
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.iprange");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.iprange", "192.168.1.20");
    }

    @Test
    public void testValidateIpAddressRelatedConfigValuesValidIpRange() {
        ConfigKey<String> key = StorageManager.PreferredStoragePool; // Any ConfigKey of String type. RemoteAccessVpnManagerImpl.RemoteAccessVpnClientIpRange not accessible here
        Mockito.doReturn(key).when(configurationManagerImplSpy._configDepot).get("config.iprange");
        configurationManagerImplSpy.validateIpAddressRelatedConfigValues("config.iprange", "192.168.1.1-192.168.1.100");
    }

    @Test
    public void testDeleteZoneInvokesDeleteNsxProviderWhenNSXIsEnabled() {
        NsxProviderVO nsxProviderVO = Mockito.mock(NsxProviderVO.class);
        DataCenterVO dataCenterVO = Mockito.mock(DataCenterVO.class);

        when(nsxProviderDao.findByZoneId(anyLong())).thenReturn(nsxProviderVO);
        when(netrisProviderDao.findByZoneId(anyLong())).thenReturn(null);
        when(zoneDao.findById(anyLong())).thenReturn(dataCenterVO);
        lenient().when(hostDao.findByDataCenterId(anyLong())).thenReturn(Collections.emptyList());
        when(podDao.listByDataCenterId(anyLong())).thenReturn(Collections.emptyList());
        when(ipAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        when(publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        when(vmInstanceDao.listByZoneId(anyLong())).thenReturn(Collections.emptyList());
        when(volumeDao.findByDc(anyLong())).thenReturn(Collections.emptyList());
        when(physicalNetworkDao.listByZone(anyLong())).thenReturn(Collections.emptyList());
        when(imageStoreDao.findByZone(any(ZoneScope.class), nullable(Boolean.class))).thenReturn(Collections.emptyList());
        when(vlanDao.listByZone(anyLong())).thenReturn(List.of(Mockito.mock(VlanVO.class)));
        when(nsxProviderVO.getId()).thenReturn(1L);
        when(zoneDao.remove(anyLong())).thenReturn(true);
        when(capacityDao.removeBy(nullable(Short.class), anyLong(), nullable(Long.class), nullable(Long.class), nullable(Long.class))).thenReturn(true);
        when(dedicatedResourceDao.findByZoneId(anyLong())).thenReturn(null);
        lenient().when(annotationDao.removeByEntityType(anyString(), anyString())).thenReturn(true);

        configurationManagerImplSpy.deleteZone(deleteZoneCmd);

        verify(nsxProviderDao, times(1)).remove(anyLong());
    }

    @Test
    public void testCreateNetworkOfferingForNsx() {
        NetworkOfferingVO offeringVO = Mockito.mock(NetworkOfferingVO.class);

        when(createNetworkOfferingCmd.isForNsx()).thenReturn(true);
        when(createNetworkOfferingCmd.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.NATTED.name());
        when(createNetworkOfferingCmd.getTraffictype()).thenReturn(Networks.TrafficType.Guest.name());
        when(createNetworkOfferingCmd.getGuestIpType()).thenReturn(Network.GuestType.Isolated.name());
        when(createNetworkOfferingCmd.getAvailability()).thenReturn(NetworkOffering.Availability.Optional.name());
        when(configDao.getValue(anyString())).thenReturn("1000");
        when(networkOfferingDao.persist(any(NetworkOfferingVO.class), anyMap())).thenReturn(offeringVO);
        doNothing().when(networkService).validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(anyLong());
        doNothing().when(networkModel).canProviderSupportServices(anyMap());

        NetworkOffering offering = configurationManagerImplSpy.createNetworkOffering(createNetworkOfferingCmd);

        Assert.assertNotNull(offering);
    }

    @Test
    public void testValidateInvalidConfiguration() {
        Mockito.doReturn(null).when(configDao).findByName(Mockito.anyString());
        String msg = configurationManagerImplSpy.validateConfigurationValue("test.config.name", "testvalue", ConfigKey.Scope.Global);
        Assert.assertEquals("Invalid configuration variable.", msg);
    }

    @Test
    public void testValidateInvalidScopeForConfiguration() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScopes()).thenReturn(List.of(ConfigKey.Scope.Account));
        Mockito.doReturn(cfg).when(configDao).findByName(Mockito.anyString());
        String msg = configurationManagerImplSpy.validateConfigurationValue("test.config.name", "testvalue", ConfigKey.Scope.Domain);
        Assert.assertEquals("Invalid scope id provided for the parameter test.config.name", msg);
    }

    @Test
    public void testValidateConfig_ThreadsOnKVMHostToTransferVMwareVMFiles_Failure() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScopes()).thenReturn(List.of(ConfigKey.Scope.Global));
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ThreadsOnKVMHostToImportVMwareVMFiles;
        Mockito.doReturn(cfg).when(configDao).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());

        String result = configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "11", configKey.getScopes().get(0));

        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateConfig_ThreadsOnKVMHostToTransferVMwareVMFiles_Success() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScopes()).thenReturn(List.of(ConfigKey.Scope.Global));
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ThreadsOnKVMHostToImportVMwareVMFiles;
        Mockito.doReturn(cfg).when(configDao).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());
        String msg = configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "10", configKey.getScopes().get(0));
        Assert.assertNull(msg);
    }

    @Test
    public void testValidateConfig_ConvertVmwareInstanceToKvmTimeout_Failure() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScopes()).thenReturn(List.of(ConfigKey.Scope.Global));
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ConvertVmwareInstanceToKvmTimeout;
        Mockito.doReturn(cfg).when(configDao).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());
        configurationManagerImplSpy.populateConfigValuesForValidationSet();

        String result = configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "0", configKey.getScopes().get(0));

        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateConfig_ConvertVmwareInstanceToKvmTimeout_Success() {
        ConfigurationVO cfg = mock(ConfigurationVO.class);
        when(cfg.getScopes()).thenReturn(List.of(ConfigKey.Scope.Global));
        ConfigKey<Integer> configKey = UnmanagedVMsManager.ConvertVmwareInstanceToKvmTimeout;
        Mockito.doReturn(cfg).when(configDao).findByName(Mockito.anyString());
        Mockito.doReturn(configKey).when(configurationManagerImplSpy._configDepot).get(configKey.key());
        configurationManagerImplSpy.populateConfigValuesForValidationSet();
        String msg = configurationManagerImplSpy.validateConfigurationValue(configKey.key(), "9", configKey.getScopes().get(0));
        Assert.assertNull(msg);
    }

    @Test
    public void validateDomainTestInvalidIdThrowException() {
        Mockito.doReturn(null).when(domainDaoMock).findById(invalidId);
        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.validateDomain(List.of(invalidId)));
    }

    @Test
    public void validateZoneTestInvalidIdThrowException() {
        Mockito.doReturn(null).when(zoneDao).findById(invalidId);
        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.validateZone(List.of(invalidId)));
    }

    @Test
    public void updateDiskOfferingIfCmdAttributeNotNullTestNotNullValueUpdateOfferingAttribute() {
        Mockito.doReturn("DiskOfferingName").when(updateDiskOfferingCmdMock).getDiskOfferingName();
        Mockito.doReturn("DisplayText").when(updateDiskOfferingCmdMock).getDisplayText();
        Mockito.doReturn(1).when(updateDiskOfferingCmdMock).getSortKey();
        Mockito.doReturn(false).when(updateDiskOfferingCmdMock).getDisplayOffering();

        configurationManagerImplSpy.updateDiskOfferingIfCmdAttributeNotNull(diskOfferingVOSpy, updateDiskOfferingCmdMock);

        Assert.assertEquals(updateDiskOfferingCmdMock.getDiskOfferingName(), diskOfferingVOSpy.getName());
        Assert.assertEquals(updateDiskOfferingCmdMock.getDisplayText(), diskOfferingVOSpy.getDisplayText());
        Assert.assertEquals(updateDiskOfferingCmdMock.getSortKey(), (Integer) diskOfferingVOSpy.getSortKey());
        Assert.assertEquals(updateDiskOfferingCmdMock.getDisplayOffering(), diskOfferingVOSpy.getDisplayOffering());
    }

    @Test
    public void updateDiskOfferingIfCmdAttributeNotNullTestNullValueDoesntUpdateOfferingAttribute() {
        Mockito.doReturn("Name").when(diskOfferingVOSpy).getName();
        Mockito.doReturn("DisplayText").when(diskOfferingVOSpy).getDisplayText();
        Mockito.doReturn(1).when(diskOfferingVOSpy).getSortKey();
        Mockito.doReturn(true).when(diskOfferingVOSpy).getDisplayOffering();

        configurationManagerImplSpy.updateDiskOfferingIfCmdAttributeNotNull(diskOfferingVOSpy, updateDiskOfferingCmdMock);

        Assert.assertNotEquals(updateDiskOfferingCmdMock.getDiskOfferingName(), diskOfferingVOSpy.getName());
        Assert.assertNotEquals(updateDiskOfferingCmdMock.getDisplayText(), diskOfferingVOSpy.getDisplayText());
        Assert.assertNotEquals(updateDiskOfferingCmdMock.getSortKey(), (Integer) diskOfferingVOSpy.getSortKey());
        Assert.assertNotEquals(updateDiskOfferingCmdMock.getDisplayOffering(), diskOfferingVOSpy.getDisplayOffering());
    }

    @Test
    public void updateDiskOfferingDetailsDomainIdsTestDifferentDomainIdsDiskOfferingDetailsAddDomainIds() {
        List<DiskOfferingDetailVO> detailsVO = new ArrayList<>();
        Long diskOfferingId = validId;

        configurationManagerImplSpy.updateDiskOfferingDetailsDomainIds(detailsVO, searchCriteriaDiskOfferingDetailMock, diskOfferingId, filteredDomainIds, existingDomainIds);

        for (int i = 0; i < detailsVO.size(); i++) {
            Assert.assertEquals(filteredDomainIds.get(i), (Long) Long.parseLong(detailsVO.get(i).getValue()));
        }
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestDifferentZoneIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, emptyExistingZoneIds, existingDomainIds, filteredDomainIds));
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestEmptyExistingDomainIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, existingZoneIds, emptyExistingDomainIds, filteredDomainIds));
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestEmptyFilteredDomainIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, existingZoneIds, existingDomainIds, emptyFilteredDomainIds));
    }

    @Test
    public void getAccountNonChildDomainsTestValidValuesReturnChildDomains() {
        Mockito.doReturn(null).when(updateDiskOfferingCmdMock).getSortKey();
        List<Long> nonChildDomains = configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdMock, existingDomainIds);

        for (int i = 0; i < existingDomainIds.size(); i++) {
            Assert.assertEquals(existingDomainIds.get(i), nonChildDomains.get(i));
        }
    }

    @Test
    public void getAccountNonChildDomainsTestAllDomainsAreChildDomainsReturnEmptyList() {
        for (Long existingDomainId : existingDomainIds) {
            Mockito.when(domainDaoMock.isChildDomain(accountMock.getDomainId(), existingDomainId)).thenReturn(true);
        }

        List<Long> nonChildDomains = configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdMock, existingDomainIds);

        Assert.assertTrue(nonChildDomains.isEmpty());
    }

    @Test
    public void getAccountNonChildDomainsTestNotNullCmdAttributeThrowException() {
        Mockito.doReturn("name").when(updateDiskOfferingCmdMock).getDiskOfferingName();

        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdMock, existingDomainIds));
    }

    @Test
    public void checkIfDomainIsChildDomainTestNonChildDomainThrowException() {
        Mockito.doReturn(false).when(domainDaoMock).isChildDomain(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(domainMock).when(entityManagerMock).findById(Domain.class, 1L);

        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.checkIfDomainIsChildDomain(diskOfferingMock, accountMock, userMock, filteredDomainIds));
    }

    @Test
    public void validateConfigurationValueTestValidatesValueType() {
        Mockito.when(configKeyMock.type()).thenReturn(Integer.class);
        configurationManagerImplSpy.validateConfigurationValue("validate.type", "100", ConfigKey.Scope.Global);
        Mockito.verify(configurationManagerImplSpy).validateValueType("100", Integer.class);
    }

    @Test
    public void validateConfigurationValueTestValidatesValueRange() {
        Mockito.when(configKeyMock.type()).thenReturn(Integer.class);
        configurationManagerImplSpy.validateConfigurationValue("validate.range", "100", ConfigKey.Scope.Global);
        Mockito.verify(configurationManagerImplSpy).validateValueRange("validate.range", "100", Integer.class, null);
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNullAndTypeIsString() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType(null, String.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNumericAndTypeIsString() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1", String.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsStringAndTypeIsString() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("test", String.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsBoolean() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNumericAndTypeIsBoolean() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1", Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsBoolean() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsTrueAndTypeIsBoolean() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("true", Boolean.class));

    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsFalseAndTypeIsBoolean() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("false", Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsInteger() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsIntegerAndTypeIsInteger() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("-2147483647", Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueExceedsIntegerLimitAndTypeIsInteger() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("2147483648", Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsDecimalAndTypeIsInteger() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1.1", Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsInteger() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsShort() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsIntegerAndTypeIsShort() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("-32768", Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueExceedsShortLimitAndTypeIsShort() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("32768", Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsDecimalAndTypeIsShort() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1.1", Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsShort() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsLong() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsIntegerAndTypeIsLong() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("-9223372036854775807", Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueExceedsLongLimitAndTypeIsLong() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("9223372036854775808", Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsDecimalAndTypeIsLong() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1.1", Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsLong() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsFloat() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Float.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsInfiniteAndTypeIsFloat() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("9999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999", Float.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNumericAndTypeIsFloat() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1.1", Float.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsFloat() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Float.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsDouble() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Double.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsInfiniteAndTypeIsDouble() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("9999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999", Double.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNumericAndTypeIsDouble() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1.1", Double.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsDouble() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Double.class));
    }

    @Test
    public void validateValueRangeTestReturnsNullWhenConfigKeyHasNoRange() {
        Assert.assertNull(configurationManagerImplSpy.validateValueRange("configkey.without.range", "0", Integer.class, null));
    }

    @Test
    public void validateValueRangeTestReturnsNullWhenConfigKeyHasRangeAndValueIsValid() {
        Assert.assertNull(configurationManagerImplSpy.validateValueRange(NetworkModel.MACIdentifier.key(), "100", Integer.class, null));
    }

    @Test
    public void validateValueRangeTestReturnsNotNullWhenConfigKeyHasRangeAndValueIsInvalid() {
        Assert.assertNotNull(configurationManagerImplSpy.validateValueRange(NetworkModel.MACIdentifier.key(), "-1", Integer.class, null));
    }

    @Test
    public void validateValueRangeTestValidatesValueWhenConfigHasRange() {
        Config config = Config.SecStorageEncryptCopy;
        String name = config.name();
        String value = "value";
        String expectedResult = "expectedResult";

        Mockito.doReturn(expectedResult).when(configurationManagerImplSpy).validateIfStringValueIsInRange(name, value, config.getRange().split(","));

        String result = configurationManagerImplSpy.validateValueRange(name, value, config.getType(), config);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateValueRangeTestValidatesIntValueWhenConfigHasNumericRange() {
        Config config = Config.RouterExtraPublicNics;
        String name = config.name();
        String value = "1";
        String expectedResult = "expectedResult";

        Mockito.doReturn(expectedResult).when(configurationManagerImplSpy).validateIfIntValueIsInRange(name, value, config.getRange());

        String result = configurationManagerImplSpy.validateValueRange(name, value, config.getType(), config);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testResetConfigurations() {
        Long poolId = 1L;
        ResetCfgCmd cmd = Mockito.mock(ResetCfgCmd.class);
        Mockito.when(cmd.getCfgName()).thenReturn("pool.storage.capacity.disablethreshold");
        Mockito.when(cmd.getStoragepoolId()).thenReturn(poolId);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getClusterId()).thenReturn(null);
        Mockito.when(cmd.getAccountId()).thenReturn(null);
        Mockito.when(cmd.getDomainId()).thenReturn(null);
        Mockito.when(cmd.getImageStoreId()).thenReturn(null);

        ConfigurationVO cfg = new ConfigurationVO("Advanced", "DEFAULT", "test", "pool.storage.capacity.disablethreshold", null, "description");
        cfg.setScope(10);
        cfg.setDefaultValue(".85");
        Mockito.when(configDao.findByName("pool.storage.capacity.disablethreshold")).thenReturn(cfg);
        Mockito.when(storagePoolDao.findById(poolId)).thenReturn(Mockito.mock(StoragePoolVO.class));

        Pair<Configuration, String> result = configurationManagerImplSpy.resetConfiguration(cmd);
        Assert.assertEquals(".85", result.second());
    }

    @Test
    public void testValidateConfigurationAllowedOnlyForDefaultAdmin_withAdminUser_shouldNotThrowException() {
        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingUserId()).thenReturn(User.UID_ADMIN);
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            when(CallContext.current()).thenReturn(callContext);
            configurationManagerImplSpy.validateConfigurationAllowedOnlyForDefaultAdmin(AccountManagerImpl.listOfRoleTypesAllowedForOperationsOfSameRoleType.key(), "Admin");
        }
    }

    @Test
    public void testValidateConfigurationAllowedOnlyForDefaultAdmin_withNonAdminUser_shouldThrowException() {
        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingUserId()).thenReturn(123L);
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            when(CallContext.current()).thenReturn(callContext);
            Assert.assertThrows(CloudRuntimeException.class, () ->
                    configurationManagerImplSpy.validateConfigurationAllowedOnlyForDefaultAdmin(AccountManagerImpl.allowOperationsOnUsersInSameAccount.key(), "Admin")
            );
        }
    }

    @Test
    public void testValidateConfigurationAllowedOnlyForDefaultAdmin_withNonRestrictedKey_shouldNotThrowException() {
        CallContext callContext = mock(CallContext.class);
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            when(CallContext.current()).thenReturn(callContext);
            configurationManagerImplSpy.validateConfigurationAllowedOnlyForDefaultAdmin("some.other.config.key", "Admin");
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testValidateConfigurationAllowedOnlyForDefaultAdmin_withValidConfigNameAndInvalidValue_shouldThrowException() {
        CallContext callContext = mock(CallContext.class);
        try (MockedStatic<CallContext> mockedCallContext = Mockito.mockStatic(CallContext.class)) {
            mockedCallContext.when(CallContext::current).thenReturn(callContext);
            when(callContext.getCallingUserId()).thenReturn(User.UID_ADMIN);
            String invalidValue = "Admin, SuperUser";
            configurationManagerImplSpy.validateConfigurationAllowedOnlyForDefaultAdmin(AccountManagerImpl.listOfRoleTypesAllowedForOperationsOfSameRoleType.key(), invalidValue);
        }
    }


    @Test
    public void getConfigurationTypeWrapperClassTestReturnsConfigType() {
        Config configuration = Config.AlertEmailAddresses;

        Assert.assertEquals(configuration.getType(), configurationManagerImplSpy.getConfigurationTypeWrapperClass(configuration.key()));
    }

    @Test
    public void getConfigurationTypeWrapperClassTestReturnsConfigKeyType() {
        String configurationName = "configuration.name";

        Mockito.when(configDepot.get(configurationName)).thenReturn(configKeyMock);
        Mockito.when(configKeyMock.type()).thenReturn(Integer.class);

        Assert.assertEquals(Integer.class, configurationManagerImplSpy.getConfigurationTypeWrapperClass(configurationName));
    }

    @Test
    public void getConfigurationTypeWrapperClassTestReturnsNullWhenConfigurationDoesNotExist() {
        String configurationName = "configuration.name";

        Mockito.when(configDepot.get(configurationName)).thenReturn(null);
        Assert.assertNull(configurationManagerImplSpy.getConfigurationTypeWrapperClass(configurationName));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsStringWhenTypeIsNull() {
        Assert.assertEquals(Configuration.ValueType.String.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(null, null));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsStringWhenTypeIsStringAndConfigurationKindIsNull() {
        Mockito.when(configurationVOMock.getKind()).thenReturn(null);
        Assert.assertEquals(Configuration.ValueType.String.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(String.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsKindWhenTypeIsStringAndKindIsNotNull() {
        Mockito.when(configurationVOMock.getKind()).thenReturn(ConfigKey.Kind.CSV.name());
        Assert.assertEquals(ConfigKey.Kind.CSV.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(String.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsKindWhenTypeIsCharacterAndKindIsNotNull() {
        Mockito.when(configurationVOMock.getKind()).thenReturn(ConfigKey.Kind.CSV.name());
        Assert.assertEquals(ConfigKey.Kind.CSV.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Character.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsNumberWhenTypeIsInteger() {
        Assert.assertEquals(Configuration.ValueType.Number.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Integer.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsNumberWhenTypeIsLong() {
        Assert.assertEquals(Configuration.ValueType.Number.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Long.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsNumberWhenTypeIsShort() {
        Assert.assertEquals(Configuration.ValueType.Number.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Short.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsDecimalWhenTypeIsFloat() {
        Assert.assertEquals(Configuration.ValueType.Decimal.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Float.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsDecimalWhenTypeIsDouble() {
        Assert.assertEquals(Configuration.ValueType.Decimal.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Double.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsBooleanWhenTypeIsBoolean() {
        Assert.assertEquals(Configuration.ValueType.Boolean.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Boolean.class, configurationVOMock));
    }

    @Test
    public void parseConfigurationTypeIntoStringTestReturnsStringWhenTypeDoesNotMatchAnyAvailableType() {
        Assert.assertEquals(Configuration.ValueType.String.name(), configurationManagerImplSpy.parseConfigurationTypeIntoString(Object.class, configurationVOMock));
    }

    @Test
    public void getConfigurationTypeTestReturnsStringWhenConfigurationDoesNotExist() {
        Mockito.when(configDao.findByName(Mockito.anyString())).thenReturn(null);
        Assert.assertEquals(Configuration.ValueType.String.name(), configurationManagerImplSpy.getConfigurationType(Mockito.anyString()));
    }

    @Test
    public void getConfigurationTypeTestReturnsRangeForConfigurationsThatAcceptIntervals() {
        String configurationName = AlertManager.CPUCapacityThreshold.key();

        Mockito.when(configDao.findByName(configurationName)).thenReturn(configurationVOMock);
        Assert.assertEquals(Configuration.ValueType.Range.name(), configurationManagerImplSpy.getConfigurationType(configurationName));
    }

    @Test
    public void getConfigurationTypeTestReturnsStringRepresentingConfigurationType() {
        ConfigKey<Boolean> configuration = RoleService.EnableDynamicApiChecker;

        Mockito.when(configDao.findByName(configuration.key())).thenReturn(configurationVOMock);
        Mockito.doReturn(configuration.type()).when(configurationManagerImplSpy).getConfigurationTypeWrapperClass(configuration.key());

        configurationManagerImplSpy.getConfigurationType(configuration.key());
        Mockito.verify(configurationManagerImplSpy).parseConfigurationTypeIntoString(configuration.type(), configurationVOMock);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsFalseWhenExternalDetailsIsEmpty() {
        Map<String, String> offeringDetails = Map.of("key1", "value1");
        Map<String, String> externalDetails = Collections.emptyMap();

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, false);

        Assert.assertFalse(result);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsFalseWhenExternalDetailsIsEmptyAndCleanupTrue() {
        Map<String, String> offeringDetails = Map.of("key1", "value1");
        Map<String, String> externalDetails = Collections.emptyMap();

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, true);

        Assert.assertFalse(result);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsTrueWhenExistingDetailsExistExternalDetailsIsEmptyAndCleanupTrue() {
        Map<String, String> offeringDetails = Map.of("External:key1", "value1");
        Map<String, String> externalDetails = Collections.emptyMap();

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, true);

        Assert.assertTrue(result);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsTrueWhenExistingExternalDetailsExistValidExternalDetailsAndCleanupTrue() {
        Map<String, String> offeringDetails = Map.of("External:key1", "value1");
        Map<String, String> externalDetails = Collections.emptyMap();

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, true);

        Assert.assertTrue(result);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsTrueWhenExistingExternalDetailsIsEmpty() {
        Map<String, String> offeringDetails = Map.of("key1", "value1");
        Map<String, String> externalDetails = Map.of("External:key1", "value1");

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, false);

        Assert.assertTrue(result);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsTrueWhenSizesDiffer() {
        Map<String, String> offeringDetails = Map.of("External:key1", "value1");
        Map<String, String> externalDetails = Map.of("External:key1", "value1", "External:key2", "value2");

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, false);

        Assert.assertTrue(result);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsTrueWhenValuesDiffer() {
        Map<String, String> offeringDetails = Map.of("External:key1", "value1");
        Map<String, String> externalDetails = Map.of("External:key1", "differentValue");

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, false);

        Assert.assertTrue(result);
    }

    @Test
    public void serviceOfferingExternalDetailsNeedUpdateReturnsFalseWhenDetailsMatch() {
        Map<String, String> offeringDetails = Map.of("External:key1", "value1", "External:key2", "value2");
        Map<String, String> externalDetails = Map.of("External:key1", "value1", "External:key2", "value2");

        boolean result = configurationManagerImplSpy.serviceOfferingExternalDetailsNeedUpdate(offeringDetails, externalDetails, false);

        Assert.assertFalse(result);
    }
}
