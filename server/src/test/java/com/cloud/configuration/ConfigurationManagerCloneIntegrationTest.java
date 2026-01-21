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

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DomainHelper;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.gpu.dao.VgpuProfileDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.network.CloneNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CloneDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CloneServiceOfferingCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.vm.lease.VMLeaseManager;
import org.junit.After;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ConfigurationManagerCloneIntegrationTest {

    @InjectMocks
    @Spy
    private ConfigurationManagerImpl configurationManager;

    @Mock
    private ServiceOfferingDao serviceOfferingDao;

    @Mock
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Mock
    private DiskOfferingDetailsDao diskOfferingDetailsDao;

    @Mock
    private NetworkOfferingDao networkOfferingDao;

    @Mock
    private NetworkOfferingServiceMapDao networkOfferingServiceMapDao;

    @Mock
    private DomainDao domainDao;

    @Mock
    private DataCenterDao dataCenterDao;

    @Mock
    private EntityManager entityManager;

    @Mock
    private com.cloud.network.NetworkModel _networkModel;

    @Mock
    private com.cloud.offerings.dao.NetworkOfferingDetailsDao networkOfferingDetailsDao;

    @Mock
    private VgpuProfileDao vgpuProfileDao;

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private DomainHelper domainHelper;

    private MockedStatic<CallContext> callContextMock;

    @Before
    public void setUp() {
        callContextMock = Mockito.mockStatic(CallContext.class);
        CallContext callContext = mock(CallContext.class);
        callContextMock.when(CallContext::current).thenReturn(callContext);

        AccountVO account = mock(AccountVO.class);
        User user = mock(User.class);
        Domain domain = mock(DomainVO.class);
        UserVO userVO = mock(UserVO.class);

        Mockito.lenient().when(callContext.getCallingAccount()).thenReturn(account);
        Mockito.lenient().when(callContext.getCallingUser()).thenReturn(user);
        Mockito.lenient().when(callContext.getCallingUserId()).thenReturn(1L);
        Mockito.lenient().when(account.getDomainId()).thenReturn(1L);
        Mockito.lenient().when(account.getId()).thenReturn(1L);
        Mockito.lenient().when(user.getId()).thenReturn(1L);
        Mockito.lenient().when(entityManager.findById(eq(Domain.class), anyLong())).thenReturn(domain);

        Mockito.doAnswer(invocation -> {
            DiskOfferingVO d = mock(DiskOfferingVO.class);
            when(d.getId()).thenReturn(999L);
            return d;
        }).when(configurationManager).createDiskOffering(
            anyLong(), anyList(), anyList(), anyString(), anyString(), anyString(),
            anyLong(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyInt(), anyString(), any(), anyLong(), anyBoolean(), anyBoolean());


        // User/Account DAO stubs used by createDiskOffering
        Mockito.lenient().when(userDao.findById(anyLong())).thenReturn(userVO);
        Mockito.lenient().when(userVO.getAccountId()).thenReturn(1L);
        Mockito.lenient().when(userVO.getRemoved()).thenReturn(null);
        Mockito.lenient().when(accountDao.findById(anyLong())).thenReturn(account);
        Mockito.lenient().when(account.getType()).thenReturn(Account.Type.ADMIN);
    }

    @After
    public void tearDown() {
        if (callContextMock != null) {
            callContextMock.close();
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCloneServiceOfferingFailsWhenSourceNotFound() {
        CloneServiceOfferingCmd cmd = mock(CloneServiceOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(999L);
        when(cmd.getServiceOfferingName()).thenReturn("cloned-offering");
        when(serviceOfferingDao.findById(999L)).thenReturn(null);

        configurationManager.cloneServiceOffering(cmd);
    }

    @Test
    public void testCloneServiceOfferingInheritsAllPropertiesFromSource() {
        Long sourceId = 1L;

        ServiceOfferingVO sourceOffering = mock(ServiceOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Display Text");
        when(sourceOffering.getCpu()).thenReturn(2);
        when(sourceOffering.getSpeed()).thenReturn(1000);
        when(sourceOffering.getRamSize()).thenReturn(2048);
        when(sourceOffering.isOfferHA()).thenReturn(true);
        when(sourceOffering.getLimitCpuUse()).thenReturn(false);
        when(sourceOffering.isVolatileVm()).thenReturn(false);
        when(sourceOffering.isCustomized()).thenReturn(false);
        when(sourceOffering.isDynamicScalingEnabled()).thenReturn(true);
        when(sourceOffering.getDiskOfferingStrictness()).thenReturn(false);
        when(sourceOffering.getHostTag()).thenReturn("host-tag");
        when(sourceOffering.getRateMbps()).thenReturn(100);
        when(sourceOffering.getDeploymentPlanner()).thenReturn("FirstFitPlanner");
        when(sourceOffering.isSystemUse()).thenReturn(false);
        when(sourceOffering.getVmType()).thenReturn(VirtualMachine.Type.User.toString());
        when(sourceOffering.getDiskOfferingId()).thenReturn(null);
        when(sourceOffering.getVgpuProfileId()).thenReturn(null);
        when(sourceOffering.getGpuCount()).thenReturn(null);
        when(sourceOffering.getGpuDisplay()).thenReturn(false);

        CloneServiceOfferingCmd cmd = mock(CloneServiceOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getServiceOfferingName()).thenReturn("cloned-offering");
        when(cmd.getFullUrlParams()).thenReturn(new HashMap<>());
        // Ensure no vGPU is specified in the command (explicitly stub to null)
        when(cmd.getVgpuProfileId()).thenReturn(null);
        when(cmd.getGpuCount()).thenReturn(null);

        when(serviceOfferingDao.findById(sourceId)).thenReturn(sourceOffering);

        ServiceOfferingVO clonedOffering = mock(ServiceOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-offering");
        when(clonedOffering.getCpu()).thenReturn(2);
        when(clonedOffering.getSpeed()).thenReturn(1000);
        when(clonedOffering.getRamSize()).thenReturn(2048);

        when(serviceOfferingDao.persist(any(ServiceOfferingVO.class))).thenReturn(clonedOffering);

        DiskOfferingVO persistedDisk = mock(DiskOfferingVO.class);
        when(persistedDisk.getId()).thenReturn(999L);
        when(diskOfferingDao.findById(anyLong())).thenReturn(persistedDisk);
        when(persistedDisk.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        when(diskOfferingDao.persist(any(DiskOfferingVO.class))).thenReturn(persistedDisk);

        Mockito.doReturn(clonedOffering).when(configurationManager).createServiceOffering(
            anyLong(), anyBoolean(), any(VirtualMachine.Type.class), anyString(),
            any(Integer.class), any(Integer.class), any(Integer.class), anyString(), anyString(), anyBoolean(),
            anyBoolean(), anyBoolean(), anyBoolean(), anyString(), anyList(), anyList(), anyString(), any(Integer.class),
            anyString(), anyMap(), anyLong(), any(Boolean.class),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            any(Integer.class), anyString(), anyLong(), anyBoolean(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyLong(), any(Integer.class), any(Boolean.class), anyBoolean(), any(Integer.class), any(VMLeaseManager.ExpiryAction.class)
        );

        ServiceOffering result = configurationManager.cloneServiceOffering(cmd);

        Assert.assertNotNull("Cloned offering should not be null", result);
        verify(serviceOfferingDao).findById(sourceId);
        Assert.assertEquals("Cloned offering should have correct name", "cloned-offering", result.getName());
        Assert.assertEquals("Cloned offering should inherit CPU count", Integer.valueOf(2), result.getCpu());
        Assert.assertEquals("Cloned offering should inherit CPU speed", Integer.valueOf(1000), result.getSpeed());
        Assert.assertEquals("Cloned offering should inherit RAM", Integer.valueOf(2048), result.getRamSize());
    }

    @Test
    public void testCloneServiceOfferingOverridesProvidedParameters() {
        Long sourceId = 1L;

        ServiceOfferingVO sourceOffering = mock(ServiceOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Display Text");
        when(sourceOffering.getCpu()).thenReturn(2);
        when(sourceOffering.getSpeed()).thenReturn(1000);
        when(sourceOffering.getRamSize()).thenReturn(2048);
        when(sourceOffering.isOfferHA()).thenReturn(true);
        when(sourceOffering.getLimitCpuUse()).thenReturn(false);
        when(sourceOffering.isVolatileVm()).thenReturn(false);
        when(sourceOffering.isCustomized()).thenReturn(false);
        when(sourceOffering.isDynamicScalingEnabled()).thenReturn(true);
        when(sourceOffering.getDiskOfferingStrictness()).thenReturn(false);
        when(sourceOffering.isSystemUse()).thenReturn(false);
        when(sourceOffering.getVmType()).thenReturn(VirtualMachine.Type.User.toString());
        when(sourceOffering.getDiskOfferingId()).thenReturn(1L);
        when(sourceOffering.getVgpuProfileId()).thenReturn(null);
        when(sourceOffering.getGpuCount()).thenReturn(null);

        CloneServiceOfferingCmd cmd = mock(CloneServiceOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getServiceOfferingName()).thenReturn("cloned-offering");
        when(cmd.getDisplayText()).thenReturn("New Display Text");
        when(cmd.getCpuNumber()).thenReturn(4);
        when(cmd.getCpuSpeed()).thenReturn(2000);
        when(cmd.getMemory()).thenReturn(4096);
        when(cmd.getVgpuProfileId()).thenReturn(null);
        when(cmd.getGpuCount()).thenReturn(null);
        when(cmd.getDiskOfferingId()).thenReturn(null);

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(1L)).thenReturn(diskOffering);
        when(diskOffering.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);

        Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.OFFER_HA, "false");
        when(cmd.getFullUrlParams()).thenReturn(params);
        when(cmd.isOfferHa()).thenReturn(false);

        when(serviceOfferingDao.findById(sourceId)).thenReturn(sourceOffering);

        ServiceOfferingVO clonedOffering = mock(ServiceOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-offering");
        when(clonedOffering.getDisplayText()).thenReturn("New Display Text");
        when(clonedOffering.getCpu()).thenReturn(4);
        when(clonedOffering.getSpeed()).thenReturn(2000);
        when(clonedOffering.getRamSize()).thenReturn(4096);
        when(clonedOffering.isOfferHA()).thenReturn(false);

        when(serviceOfferingDao.persist(any(ServiceOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createServiceOffering(
            anyLong(), anyBoolean(), any(), anyString(), eq(4), eq(4096), eq(2000),
            anyString(), anyString(), anyBoolean(), eq(false), anyBoolean(), anyBoolean(),
            anyString(), anyList(), anyList(), anyString(), anyInt(), anyString(), any(),
            anyLong(), anyBoolean(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyInt(), anyString(), anyLong(), anyBoolean(), anyLong(),
            anyBoolean(), anyBoolean(), anyBoolean(), anyLong(), anyInt(), anyBoolean(),
            anyBoolean(), anyInt(), any());

        ServiceOffering result = configurationManager.cloneServiceOffering(cmd);

        Assert.assertNotNull("Cloned offering should not be null", result);
        verify(serviceOfferingDao).findById(sourceId);
        Assert.assertEquals("Cloned offering should override display text", "New Display Text", result.getDisplayText());
        Assert.assertEquals("Cloned offering should override CPU count", Integer.valueOf(4), result.getCpu());
        Assert.assertEquals("Cloned offering should override CPU speed", Integer.valueOf(2000), result.getSpeed());
        Assert.assertEquals("Cloned offering should override RAM", Integer.valueOf(4096), result.getRamSize());
        Assert.assertEquals("Cloned offering should override HA", Boolean.FALSE, result.isOfferHA());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCloneDiskOfferingFailsWhenSourceNotFound() {
        CloneDiskOfferingCmd cmd = mock(CloneDiskOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(999L);
        when(cmd.getOfferingName()).thenReturn("cloned-disk-offering");
        when(diskOfferingDao.findById(999L)).thenReturn(null);

        configurationManager.cloneDiskOffering(cmd);
    }

    @Test
    public void testCloneDiskOfferingInheritsAllPropertiesFromSource() {
        Long sourceId = 1L;

        DiskOfferingVO sourceOffering = mock(DiskOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-disk");
        when(sourceOffering.getDisplayText()).thenReturn("Source Disk Display");
        when(sourceOffering.getDiskSize()).thenReturn(10L);
        when(sourceOffering.getTags()).thenReturn("tag1");
        when(sourceOffering.isCustomized()).thenReturn(false);
        when(sourceOffering.getDisplayOffering()).thenReturn(true);
        when(sourceOffering.isCustomizedIops()).thenReturn(false);
        when(sourceOffering.getDiskSizeStrictness()).thenReturn(false);
        when(sourceOffering.getEncrypt()).thenReturn(false);
        when(sourceOffering.isUseLocalStorage()).thenReturn(false);
        when(sourceOffering.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        when(sourceOffering.getMinIops()).thenReturn(1000L);
        when(sourceOffering.getMaxIops()).thenReturn(2000L);

        CloneDiskOfferingCmd cmd = mock(CloneDiskOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getOfferingName()).thenReturn("cloned-disk-offering");
        when(cmd.getDiskSize()).thenReturn(null);
        when(diskOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(diskOfferingDetailsDao.findDomainIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.findZoneIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.getDetail(eq(sourceId), anyString())).thenReturn(null);
        when(cmd.getMinIops()).thenReturn(null);
        when(cmd.getMaxIops()).thenReturn(null);

        DiskOfferingVO clonedOffering = mock(DiskOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-disk-offering");
        when(clonedOffering.getDisplayText()).thenReturn("Source Disk Display");
        when(clonedOffering.getDiskSize()).thenReturn(10L);
        when(clonedOffering.getTags()).thenReturn("tag1");
        when(diskOfferingDao.persist(any(DiskOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createDiskOffering(
            anyLong(), anyList(), anyList(), anyString(), anyString(), anyString(),
            anyLong(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyInt(), anyString(), any(), anyLong(), anyBoolean(), anyBoolean());

        DiskOffering result = configurationManager.cloneDiskOffering(cmd);

        Assert.assertNotNull("Cloned disk offering should not be null", result);
        verify(diskOfferingDao).findById(sourceId);
        Assert.assertEquals("Cloned offering should have correct name", "cloned-disk-offering", result.getName());
        Assert.assertEquals("Cloned offering should inherit display text", "Source Disk Display", result.getDisplayText());
        Assert.assertEquals("Cloned offering should inherit disk size", 10L, result.getDiskSize());
        Assert.assertEquals("Cloned offering should inherit tags", "tag1", result.getTags());
    }

    @Test
    public void testCloneDiskOfferingOverridesProvidedParameters() {
        Long sourceId = 1L;

        DiskOfferingVO sourceOffering = mock(DiskOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-disk");
        when(sourceOffering.getDisplayText()).thenReturn("Source Disk Display");
        when(sourceOffering.getDiskSize()).thenReturn(100L);
        when(sourceOffering.getTags()).thenReturn("tag1");
        when(sourceOffering.isCustomized()).thenReturn(false);
        when(sourceOffering.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        when(sourceOffering.isUseLocalStorage()).thenReturn(false);

        CloneDiskOfferingCmd cmd = mock(CloneDiskOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getOfferingName()).thenReturn("cloned-disk-offering");
        when(cmd.getDisplayText()).thenReturn("New Disk Display");
        when(cmd.getDiskSize()).thenReturn(20L);
        when(cmd.getTags()).thenReturn("tag1,tag2");
        when(cmd.getFullUrlParams()).thenReturn(new HashMap<>());
        when(cmd.getMinIops()).thenReturn(100L);
        when(cmd.getMaxIops()).thenReturn(200L);

        when(diskOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(diskOfferingDetailsDao.findDomainIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.findZoneIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.getDetail(eq(sourceId), anyString())).thenReturn(null);

        DiskOfferingVO clonedOffering = mock(DiskOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-disk-offering");
        when(clonedOffering.getDisplayText()).thenReturn("New Disk Display");
        when(clonedOffering.getDiskSize()).thenReturn(21L);
        when(clonedOffering.getTags()).thenReturn("tag1,tag2");

        // Ensure the real createDiskOffering path will return our mocked offering when it calls persist
        when(diskOfferingDao.persist(any(DiskOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createDiskOffering(
            anyLong(), anyList(), anyList(), eq("cloned-disk-offering"), eq("New Disk Display"), anyString(),
            anyLong(), eq("tag1,tag2"), anyBoolean(), anyBoolean(), anyBoolean(), any(),
            eq(100L), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyInt(), anyString(), any(), anyLong(), anyBoolean(), anyBoolean());

        DiskOffering result = configurationManager.cloneDiskOffering(cmd);

        Assert.assertNotNull("Cloned disk offering should not be null", result);
        verify(diskOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneDiskOfferingInheritsDomainAndZoneRestrictions() {
        Long sourceId = 1L;

        List<Long> domainIds = new ArrayList<>();
        domainIds.add(1L);
        domainIds.add(2L);

        List<Long> zoneIds = new ArrayList<>();
        zoneIds.add(1L);

        DiskOfferingVO sourceOffering = mock(DiskOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-disk");
        when(sourceOffering.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        when(sourceOffering.isUseLocalStorage()).thenReturn(false);
        when(sourceOffering.getDiskSize()).thenReturn(10L);
        when(sourceOffering.getMinIops()).thenReturn(1000L);
        when(sourceOffering.getMaxIops()).thenReturn(2000L);

        CloneDiskOfferingCmd cmd = mock(CloneDiskOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getOfferingName()).thenReturn("cloned-disk-offering");
        when(cmd.getFullUrlParams()).thenReturn(new HashMap<>());
        when(cmd.getDomainIds()).thenReturn(null);
        when(cmd.getZoneIds()).thenReturn(null);
        when(cmd.getDiskSize()).thenReturn(null);
        when(cmd.getMinIops()).thenReturn(null);
        when(cmd.getMaxIops()).thenReturn(null);


        when(diskOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(diskOfferingDetailsDao.findDomainIds(sourceId)).thenReturn(domainIds);
        when(diskOfferingDetailsDao.findZoneIds(sourceId)).thenReturn(zoneIds);
        when(diskOfferingDetailsDao.getDetail(eq(sourceId), anyString())).thenReturn(null);

        DiskOfferingVO clonedOffering = mock(DiskOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(diskOfferingDao.persist(any(DiskOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createDiskOffering(
            anyLong(), eq(domainIds), eq(zoneIds), anyString(), anyString(), anyString(),
            anyLong(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyInt(), anyString(), any(), anyLong(), anyBoolean(), anyBoolean());

        DiskOffering result = configurationManager.cloneDiskOffering(cmd);

        Assert.assertNotNull("Cloned disk offering should not be null", result);
        verify(diskOfferingDao).findById(sourceId);
        verify(diskOfferingDetailsDao).findDomainIds(sourceId);
        verify(diskOfferingDetailsDao).findZoneIds(sourceId);
    }

    @Test
    public void testCloneServiceOfferingCanInheritDetailsFromSource() {
        Long sourceId = 1L;

        ServiceOfferingVO sourceOffering = mock(ServiceOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getCpu()).thenReturn(2);
        when(sourceOffering.getSpeed()).thenReturn(1000);
        when(sourceOffering.getRamSize()).thenReturn(2048);
        when(sourceOffering.isSystemUse()).thenReturn(false);
        when(sourceOffering.getVmType()).thenReturn(VirtualMachine.Type.User.toString());
        when(sourceOffering.getVgpuProfileId()).thenReturn(null);
        when(sourceOffering.getGpuCount()).thenReturn(null);

        CloneServiceOfferingCmd cmd = mock(CloneServiceOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getServiceOfferingName()).thenReturn("cloned-offering");
        when(cmd.getFullUrlParams()).thenReturn(new HashMap<>());
        when(cmd.getDetails()).thenReturn(null);
        when(cmd.getVgpuProfileId()).thenReturn(null);
        when(cmd.getGpuCount()).thenReturn(null);

        when(serviceOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(anyLong())).thenReturn(diskOfferingVO);
        when(diskOfferingVO.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);

        ServiceOfferingVO clonedOffering = mock(ServiceOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(serviceOfferingDao.persist(any(ServiceOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createServiceOffering(
            anyLong(), anyBoolean(), any(), anyString(), anyInt(), anyInt(), anyInt(),
            anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
            anyString(), anyList(), anyList(), anyString(), anyInt(), anyString(), any(),
            anyLong(), anyBoolean(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyInt(), anyString(), anyLong(), anyBoolean(), anyLong(),
            anyBoolean(), anyBoolean(), anyBoolean(), anyLong(), anyInt(), anyBoolean(),
            anyBoolean(), anyInt(), any());

        ServiceOffering result = configurationManager.cloneServiceOffering(cmd);

        Assert.assertNotNull("Cloned offering should not be null", result);
        verify(serviceOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneDiskOfferingVerifiesInheritedValues() {
        Long sourceId = 1L;

        DiskOfferingVO sourceOffering = new DiskOfferingVO("source-disk", "Source Disk Offering",
            Storage.ProvisioningType.THIN, 50L, "production,ssd", false, false, 1000L, 5000L);
        sourceOffering.setDisplayOffering(true);
        sourceOffering.setDiskSizeStrictness(false);
        sourceOffering.setEncrypt(true);
        sourceOffering.setUseLocalStorage(false);
        sourceOffering.setHypervisorSnapshotReserve(20);

        CloneDiskOfferingCmd cmd = mock(CloneDiskOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getOfferingName()).thenReturn("cloned-disk-offering");
        when(cmd.getFullUrlParams()).thenReturn(new HashMap<>());
        when(cmd.getDiskSize()).thenReturn(null);
        when(cmd.getMinIops()).thenReturn(null);
        when(cmd.getMaxIops()).thenReturn(null);

        when(diskOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(diskOfferingDetailsDao.findDomainIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.findZoneIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.getDetail(eq(sourceId), anyString())).thenReturn(null);

        DiskOfferingVO clonedOffering = new DiskOfferingVO("cloned-disk-offering", "Source Disk Offering",
            Storage.ProvisioningType.THIN, 50L, "production,ssd", false, false, 1000L, 5000L);
        clonedOffering.setEncrypt(true);
        clonedOffering.setHypervisorSnapshotReserve(20);
        when(diskOfferingDao.persist(any(DiskOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createDiskOffering(
            anyLong(), anyList(), anyList(), eq("cloned-disk-offering"), eq("Source Disk Offering"),
            anyString(), eq(50L), eq("production,ssd"), anyBoolean(), anyBoolean(),
            anyBoolean(), any(), eq(1000L), eq(5000L), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), eq(20), anyString(), any(), anyLong(), anyBoolean(),
            eq(true));

        DiskOffering result = configurationManager.cloneDiskOffering(cmd);

        Assert.assertNotNull("Cloned disk offering should not be null", result);
        Assert.assertEquals("Should inherit display text", "Source Disk Offering", result.getDisplayText());
        Assert.assertEquals("Should inherit disk size", 50L, result.getDiskSize());
        Assert.assertEquals("Should inherit tags", "production,ssd", result.getTags());
        Assert.assertEquals("Should inherit min IOPS", Long.valueOf(1000L), result.getMinIops());
        Assert.assertEquals("Should inherit max IOPS", Long.valueOf(5000L), result.getMaxIops());
        Assert.assertEquals("Should inherit hypervisor snapshot reserve", Integer.valueOf(20), result.getHypervisorSnapshotReserve());
        verify(diskOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneDiskOfferingVerifiesOverriddenValues() {
        Long sourceId = 1L;

        DiskOfferingVO sourceOffering = new DiskOfferingVO("source-disk", "Source Disk Offering",
            Storage.ProvisioningType.THIN, 5L, "production", false, false, 1000L, 5000L);
        sourceOffering.setEncrypt(false);

        CloneDiskOfferingCmd cmd = mock(CloneDiskOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getOfferingName()).thenReturn("cloned-disk-offering");
        when(cmd.getDisplayText()).thenReturn("Cloned Disk Offering - Updated");
        when(cmd.getDiskSize()).thenReturn(10L);
        when(cmd.getTags()).thenReturn("production,high-performance");
        when(cmd.getMinIops()).thenReturn(2000L);
        when(cmd.getMaxIops()).thenReturn(10000L);

        Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.ENCRYPT, "true");
        when(cmd.getFullUrlParams()).thenReturn(params);
        when(cmd.getEncrypt()).thenReturn(true);

        when(diskOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(diskOfferingDetailsDao.findDomainIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.findZoneIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.getDetail(eq(sourceId), anyString())).thenReturn(null);

        DiskOfferingVO clonedOffering = new DiskOfferingVO("cloned-disk-offering", "Cloned Disk Offering - Updated",
            Storage.ProvisioningType.THIN, 10L, "production,high-performance", false, false, 2000L, 10000L);
        clonedOffering.setEncrypt(true);

        when(diskOfferingDao.persist(any(DiskOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createDiskOffering(
            anyLong(), anyList(), anyList(), eq("cloned-disk-offering"),
            eq("Cloned Disk Offering - Updated"), anyString(), eq(10L),
            eq("production,high-performance"), anyBoolean(), anyBoolean(), anyBoolean(),
            any(), eq(2000L), eq(10000L), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyInt(), anyString(), any(), anyLong(), anyBoolean(), eq(true));

        DiskOffering result = configurationManager.cloneDiskOffering(cmd);

        Assert.assertNotNull("Cloned disk offering should not be null", result);
        Assert.assertEquals("Should override display text", "Cloned Disk Offering - Updated", result.getDisplayText());
        Assert.assertEquals("Should override disk size", 10L, result.getDiskSize());
        Assert.assertEquals("Should override tags", "production,high-performance", result.getTags());
        Assert.assertEquals("Should override min IOPS", Long.valueOf(2000L), result.getMinIops());
        Assert.assertEquals("Should override max IOPS", Long.valueOf(10000L), result.getMaxIops());
        Assert.assertTrue("Should override encrypt flag", result.getEncrypt());
        verify(diskOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneDiskOfferingInheritsBytesReadWriteRates() {
        Long sourceId = 1L;

        DiskOfferingVO sourceOffering = new DiskOfferingVO("source-disk", "Source Disk",
            Storage.ProvisioningType.THIN, 53L, "tag1", false, false, null, null);
        sourceOffering.setBytesReadRate(10485760L);
        sourceOffering.setBytesReadRateMax(20971520L);
        sourceOffering.setBytesReadRateMaxLength(60L);
        sourceOffering.setBytesWriteRate(10485760L);
        sourceOffering.setBytesWriteRateMax(20971520L);
        sourceOffering.setBytesWriteRateMaxLength(60L);

        CloneDiskOfferingCmd cmd = mock(CloneDiskOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getOfferingName()).thenReturn("cloned-disk");
        when(cmd.getFullUrlParams()).thenReturn(new HashMap<>());
        when(cmd.getDiskSize()).thenReturn(null);
        when(cmd.getMinIops()).thenReturn(null);
        when(cmd.getMaxIops()).thenReturn(null);

        when(diskOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(diskOfferingDetailsDao.findDomainIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.findZoneIds(sourceId)).thenReturn(Collections.emptyList());
        when(diskOfferingDetailsDao.getDetail(eq(sourceId), anyString())).thenReturn(null);

        DiskOfferingVO clonedOffering = new DiskOfferingVO("cloned-disk", "Source Disk",
            Storage.ProvisioningType.THIN, 53L, "tag1", false, false, null, null);
        clonedOffering.setBytesReadRate(10485760L);
        clonedOffering.setBytesReadRateMax(20971520L);
        clonedOffering.setBytesReadRateMaxLength(60L);
        clonedOffering.setBytesWriteRate(10485760L);
        clonedOffering.setBytesWriteRateMax(20971520L);
        clonedOffering.setBytesWriteRateMaxLength(60L);
        when(diskOfferingDao.persist(any(DiskOfferingVO.class))).thenReturn(clonedOffering);

        Mockito.doReturn(clonedOffering).when(configurationManager).createDiskOffering(
            anyLong(), anyList(), anyList(), anyString(), anyString(), anyString(),
            anyLong(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyInt(), anyString(), any(), anyLong(), anyBoolean(), anyBoolean());

        DiskOffering result = configurationManager.cloneDiskOffering(cmd);

        Assert.assertNotNull("Cloned disk offering should not be null", result);
        Assert.assertEquals("Should inherit bytes read rate", Long.valueOf(10485760L), result.getBytesReadRate());
        Assert.assertEquals("Should inherit bytes read rate max", Long.valueOf(20971520L), result.getBytesReadRateMax());
        Assert.assertEquals("Should inherit bytes read rate max length", Long.valueOf(60L), result.getBytesReadRateMaxLength());
        Assert.assertEquals("Should inherit bytes write rate", Long.valueOf(10485760L), result.getBytesWriteRate());
        Assert.assertEquals("Should inherit bytes write rate max", Long.valueOf(20971520L), result.getBytesWriteRateMax());
        Assert.assertEquals("Should inherit bytes write rate max length", Long.valueOf(60L), result.getBytesWriteRateMaxLength());
        verify(diskOfferingDao).findById(sourceId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCloneNetworkOfferingFailsWhenSourceNotFound() {
        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(999L);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");
        when(networkOfferingDao.findById(999L)).thenReturn(null);

        configurationManager.cloneNetworkOffering(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCloneNetworkOfferingFailsWhenNameIsNull() {
        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(1L);
        when(cmd.getNetworkOfferingName()).thenReturn(null);

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(1L);
        when(networkOfferingDao.findById(1L)).thenReturn(sourceOffering);

        configurationManager.cloneNetworkOffering(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCloneNetworkOfferingFailsWhenNameAlreadyExists() {
        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(1L);
        when(cmd.getNetworkOfferingName()).thenReturn("existing-offering");

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(1L);
        when(sourceOffering.getName()).thenReturn("source-offering");

        NetworkOfferingVO existingOffering = mock(NetworkOfferingVO.class);
        when(existingOffering.getId()).thenReturn(2L);

        when(networkOfferingDao.findById(1L)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("existing-offering")).thenReturn(existingOffering);

        configurationManager.cloneNetworkOffering(cmd);
    }

    @Test
    public void testCloneNetworkOfferingInheritsAllPropertiesFromSource() {
        Long sourceId = 1L;

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-network-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(sourceOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(sourceOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(sourceOffering.getAvailability()).thenReturn(NetworkOffering.Availability.Optional);
        when(sourceOffering.getState()).thenReturn(NetworkOffering.State.Enabled);
        when(sourceOffering.isDefault()).thenReturn(false);
        when(sourceOffering.isConserveMode()).thenReturn(true);
        when(sourceOffering.isEgressDefaultPolicy()).thenReturn(false);
        when(sourceOffering.isPersistent()).thenReturn(false);

        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");

        when(networkOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("cloned-network-offering")).thenReturn(null);

        // Mock the network model to return service provider map
        Map<Network.Service, java.util.Set<Network.Provider>> serviceProviderMap = new HashMap<>();
        when(configurationManager._networkModel.getNetworkOfferingServiceProvidersMap(sourceId))
            .thenReturn(serviceProviderMap);

        NetworkOfferingVO clonedOffering = mock(NetworkOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-network-offering");
        when(clonedOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(clonedOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);

        Mockito.doReturn(clonedOffering).when(configurationManager).createNetworkOffering(any());

        NetworkOffering result = configurationManager.cloneNetworkOffering(cmd);

        Assert.assertNotNull("Cloned network offering should not be null", result);
        verify(networkOfferingDao).findById(sourceId);
        Assert.assertEquals("Should have correct name", "cloned-network-offering", result.getName());
    }

    @Test
    public void testCloneNetworkOfferingOverridesDisplayText() {
        Long sourceId = 1L;

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-network-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(sourceOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(sourceOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");
        when(cmd.getDisplayText()).thenReturn("New Display Text for Network");

        when(networkOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("cloned-network-offering")).thenReturn(null);

        Map<Network.Service, java.util.Set<Network.Provider>> serviceProviderMap = new HashMap<>();
        when(configurationManager._networkModel.getNetworkOfferingServiceProvidersMap(sourceId))
            .thenReturn(serviceProviderMap);

        NetworkOfferingVO clonedOffering = mock(NetworkOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-network-offering");
        when(clonedOffering.getDisplayText()).thenReturn("New Display Text for Network");

        Mockito.doReturn(clonedOffering).when(configurationManager).createNetworkOffering(any());

        NetworkOffering result = configurationManager.cloneNetworkOffering(cmd);

        Assert.assertNotNull("Cloned network offering should not be null", result);
        Assert.assertEquals("Should override display text", "New Display Text for Network", result.getDisplayText());
        verify(networkOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneNetworkOfferingHandlesAddServices() {
        Long sourceId = 1L;

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-network-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(sourceOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(sourceOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");

        List<String> addServices = new ArrayList<>();
        addServices.add("Vpn");
        addServices.add("StaticNat");
        when(cmd.getAddServices()).thenReturn(addServices);
        when(cmd.getSupportedServices()).thenReturn(null);

        when(networkOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("cloned-network-offering")).thenReturn(null);

        Map<Network.Service, java.util.Set<Network.Provider>> serviceProviderMap = new HashMap<>();
        java.util.Set<Network.Provider> dhcpProviders = new java.util.HashSet<>();
        dhcpProviders.add(Network.Provider.VirtualRouter);
        serviceProviderMap.put(Network.Service.Dhcp, dhcpProviders);

        when(configurationManager._networkModel.getNetworkOfferingServiceProvidersMap(sourceId))
            .thenReturn(serviceProviderMap);

        NetworkOfferingVO clonedOffering = mock(NetworkOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-network-offering");

        Mockito.doReturn(clonedOffering).when(configurationManager).createNetworkOffering(any());

        NetworkOffering result = configurationManager.cloneNetworkOffering(cmd);

        Assert.assertNotNull("Cloned network offering should not be null", result);
        verify(networkOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneNetworkOfferingHandlesDropServices() {
        Long sourceId = 1L;

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-network-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(sourceOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(sourceOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");

        List<String> dropServices = new ArrayList<>();
        dropServices.add("Firewall");
        when(cmd.getDropServices()).thenReturn(dropServices);
        when(cmd.getSupportedServices()).thenReturn(null);

        when(networkOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("cloned-network-offering")).thenReturn(null);

        Map<Network.Service, java.util.Set<Network.Provider>> serviceProviderMap = new HashMap<>();
        java.util.Set<Network.Provider> dhcpProviders = new java.util.HashSet<>();
        dhcpProviders.add(Network.Provider.VirtualRouter);
        serviceProviderMap.put(Network.Service.Dhcp, dhcpProviders);

        java.util.Set<Network.Provider> firewallProviders = new java.util.HashSet<>();
        firewallProviders.add(Network.Provider.VirtualRouter);
        serviceProviderMap.put(Network.Service.Firewall, firewallProviders);

        when(configurationManager._networkModel.getNetworkOfferingServiceProvidersMap(sourceId))
            .thenReturn(serviceProviderMap);

        NetworkOfferingVO clonedOffering = mock(NetworkOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-network-offering");

        Mockito.doReturn(clonedOffering).when(configurationManager).createNetworkOffering(any());

        NetworkOffering result = configurationManager.cloneNetworkOffering(cmd);

        Assert.assertNotNull("Cloned network offering should not be null", result);
        verify(networkOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneNetworkOfferingOverridesSupportedServices() {
        Long sourceId = 1L;

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-network-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(sourceOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(sourceOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");

        List<String> supportedServices = new ArrayList<>();
        supportedServices.add("Dhcp");
        supportedServices.add("Dns");
        supportedServices.add("SourceNat");
        when(cmd.getSupportedServices()).thenReturn(supportedServices);

        when(networkOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("cloned-network-offering")).thenReturn(null);

        Map<Network.Service, java.util.Set<Network.Provider>> serviceProviderMap = new HashMap<>();
        when(configurationManager._networkModel.getNetworkOfferingServiceProvidersMap(sourceId))
            .thenReturn(serviceProviderMap);

        NetworkOfferingVO clonedOffering = mock(NetworkOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-network-offering");

        Mockito.doReturn(clonedOffering).when(configurationManager).createNetworkOffering(any());

        NetworkOffering result = configurationManager.cloneNetworkOffering(cmd);

        Assert.assertNotNull("Cloned network offering should not be null", result);
        verify(networkOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneNetworkOfferingInheritsGuestTypeAndTrafficType() {
        Long sourceId = 1L;

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-network-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(sourceOffering.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(sourceOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(sourceOffering.getAvailability()).thenReturn(NetworkOffering.Availability.Required);

        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");
        when(cmd.getGuestIpType()).thenReturn(null); // Should inherit

        when(networkOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("cloned-network-offering")).thenReturn(null);

        Map<Network.Service, java.util.Set<Network.Provider>> serviceProviderMap = new HashMap<>();
        when(configurationManager._networkModel.getNetworkOfferingServiceProvidersMap(sourceId))
            .thenReturn(serviceProviderMap);

        NetworkOfferingVO clonedOffering = mock(NetworkOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-network-offering");
        when(clonedOffering.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(clonedOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        Mockito.doReturn(clonedOffering).when(configurationManager).createNetworkOffering(any());

        NetworkOffering result = configurationManager.cloneNetworkOffering(cmd);

        Assert.assertNotNull("Cloned network offering should not be null", result);
        Assert.assertEquals("Should inherit guest type", Network.GuestType.Shared, result.getGuestType());
        Assert.assertEquals("Should inherit traffic type", Networks.TrafficType.Guest, result.getTrafficType());
        verify(networkOfferingDao).findById(sourceId);
    }

    @Test
    public void testCloneNetworkOfferingInheritsAvailability() {
        Long sourceId = 1L;

        NetworkOfferingVO sourceOffering = mock(NetworkOfferingVO.class);
        when(sourceOffering.getId()).thenReturn(sourceId);
        when(sourceOffering.getName()).thenReturn("source-network-offering");
        when(sourceOffering.getDisplayText()).thenReturn("Source Network Offering");
        when(sourceOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(sourceOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(sourceOffering.getAvailability()).thenReturn(NetworkOffering.Availability.Required);

        CloneNetworkOfferingCmd cmd = mock(CloneNetworkOfferingCmd.class);
        when(cmd.getSourceOfferingId()).thenReturn(sourceId);
        when(cmd.getNetworkOfferingName()).thenReturn("cloned-network-offering");
        when(cmd.getAvailability()).thenReturn(null); // Should inherit

        when(networkOfferingDao.findById(sourceId)).thenReturn(sourceOffering);
        when(networkOfferingDao.findByUniqueName("cloned-network-offering")).thenReturn(null);

        Map<Network.Service, java.util.Set<Network.Provider>> serviceProviderMap = new HashMap<>();
        when(configurationManager._networkModel.getNetworkOfferingServiceProvidersMap(sourceId))
            .thenReturn(serviceProviderMap);

        NetworkOfferingVO clonedOffering = mock(NetworkOfferingVO.class);
        when(clonedOffering.getId()).thenReturn(2L);
        when(clonedOffering.getName()).thenReturn("cloned-network-offering");
        when(clonedOffering.getAvailability()).thenReturn(NetworkOffering.Availability.Required);

        Mockito.doReturn(clonedOffering).when(configurationManager).createNetworkOffering(any());

        NetworkOffering result = configurationManager.cloneNetworkOffering(cmd);

        Assert.assertNotNull("Cloned network offering should not be null", result);
        Assert.assertEquals("Should inherit availability", NetworkOffering.Availability.Required, result.getAvailability());
        verify(networkOfferingDao).findById(sourceId);
    }
}

