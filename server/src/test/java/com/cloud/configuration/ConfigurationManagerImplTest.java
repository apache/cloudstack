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

import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.NsxProviderVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.zone.DeleteZoneCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerImplTest {
    @Mock
    ConfigDepot configDepot;
    @Mock
    NsxProviderDao nsxProviderDao;
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

    ConfigurationManagerImpl configurationManagerImplSpy = Mockito.spy(new ConfigurationManagerImpl());
    DeleteZoneCmd deleteZoneCmd;
    CreateNetworkOfferingCmd createNetworkOfferingCmd;

    @Before
    public void setUp() throws Exception {
        configurationManagerImplSpy._configDepot = configDepot;
        configurationManagerImplSpy.nsxProviderDao = nsxProviderDao;
        configurationManagerImplSpy._zoneDao = zoneDao;
        configurationManagerImplSpy._hostDao = hostDao;
        configurationManagerImplSpy._podDao = podDao;
        configurationManagerImplSpy._privateIpAddressDao = ipAddressDao;
        configurationManagerImplSpy._publicIpAddressDao = publicIpAddressDao;
        configurationManagerImplSpy._vmInstanceDao = vmInstanceDao;
        configurationManagerImplSpy._volumeDao = volumeDao;
        configurationManagerImplSpy._physicalNetworkDao = physicalNetworkDao;
        configurationManagerImplSpy._imageStoreDao = imageStoreDao;
        configurationManagerImplSpy._vlanDao = vlanDao;
        configurationManagerImplSpy._capacityDao = capacityDao;
        configurationManagerImplSpy._dedicatedDao = dedicatedResourceDao;
        configurationManagerImplSpy._configDao = configDao;
        configurationManagerImplSpy._networkOfferingDao = networkOfferingDao;
        configurationManagerImplSpy._networkSvc = networkService;
        configurationManagerImplSpy._networkModel = networkModel;
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
        when(zoneDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(hostDao.findByDataCenterId(anyLong())).thenReturn(Collections.emptyList());
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
        when(annotationDao.removeByEntityType(anyString(), anyString())).thenReturn(true);

        configurationManagerImplSpy.deleteZone(deleteZoneCmd);

        verify(nsxProviderDao, times(1)).remove(anyLong());
    }

    @Test
    public void testCreateNetworkOfferingForNsx() {
        NetworkOfferingVO offeringVO = Mockito.mock(NetworkOfferingVO.class);

        when(createNetworkOfferingCmd.isForNsx()).thenReturn(true);
        when(createNetworkOfferingCmd.getNsxMode()).thenReturn(NetworkOffering.NsxMode.NATTED.name());
        when(createNetworkOfferingCmd.getTraffictype()).thenReturn(Networks.TrafficType.Guest.name());
        when(createNetworkOfferingCmd.getGuestIpType()).thenReturn(Network.GuestType.Isolated.name());
        when(createNetworkOfferingCmd.getAvailability()).thenReturn(NetworkOffering.Availability.Optional.name());
        when(configurationManagerImplSpy.createNetworkOffering(anyString(), anyString(), any(Networks.TrafficType.class), anyString(),
                anyBoolean(), any(NetworkOffering.Availability.class), anyInt(), anyMap(), anyBoolean(), any(Network.GuestType.class),
                anyBoolean(), anyLong(), anyBoolean(), anyMap(), anyBoolean(), anyBoolean(), anyMap(), anyBoolean(), anyInt(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyString(), anyList(), anyList(), anyBoolean(), any(NetUtils.InternetProtocol.class)))
                .thenReturn(offeringVO);
        when(configDao.getValue(anyString())).thenReturn("1000");
        when(networkOfferingDao.persist(any(NetworkOfferingVO.class), anyMap())).thenReturn(offeringVO);
        doNothing().when(networkService).validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(anyLong());
        doNothing().when(networkModel).canProviderSupportServices(anyMap());

        NetworkOffering offering = configurationManagerImplSpy.createNetworkOffering(createNetworkOfferingCmd);

        Assert.assertNotNull(offering);
    }
}
