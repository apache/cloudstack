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
package org.apache.cloudstack.resource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.api.command.admin.resource.PurgeExpungedResourcesCmd;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.dao.VmWorkJobDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.OpRouterMonitorServiceDao;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ItWorkDao;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.ConsoleSessionDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.NicExtraDhcpOptionDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class ResourceCleanupServiceImplTest {

    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    VolumeDetailsDao volumeDetailsDao;
    @Mock
    VolumeDataStoreDao volumeDataStoreDao;
    @Mock
    SnapshotDao snapshotDao;
    @Mock
    SnapshotDetailsDao snapshotDetailsDao;
    @Mock
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Mock
    NicDao nicDao;
    @Mock
    NicDetailsDao nicDetailsDao;
    @Mock
    NicExtraDhcpOptionDao nicExtraDhcpOptionDao;
    @Mock
    InlineLoadBalancerNicMapDao inlineLoadBalancerNicMapDao;
    @Mock
    VMSnapshotDao vmSnapshotDao;
    @Mock
    VMSnapshotDetailsDao vmSnapshotDetailsDao;
    @Mock
    UserVmDetailsDao userVmDetailsDao;
    @Mock
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;
    @Mock
    CommandExecLogDao commandExecLogDao;
    @Mock
    NetworkOrchestrationService networkOrchestrationService;
    @Mock
    LoadBalancerVMMapDao loadBalancerVMMapDao;
    @Mock
    NicSecondaryIpDao nicSecondaryIpDao;
    @Mock
    HighAvailabilityManager highAvailabilityManager;
    @Mock
    ItWorkDao itWorkDao;
    @Mock
    OpRouterMonitorServiceDao opRouterMonitorServiceDao;
    @Mock
    PortForwardingRulesDao portForwardingRulesDao;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    VmWorkJobDao vmWorkJobDao;
    @Mock
    ConsoleSessionDao consoleSessionDao;
    @Mock
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Spy
    @InjectMocks
    ResourceCleanupServiceImpl resourceCleanupService = Mockito.spy(new ResourceCleanupServiceImpl());

    List<Long> ids = List.of(1L, 2L);
    Long batchSize = 100L;

    private void overrideConfigValue(final ConfigKey configKey, final Object value) {
        try {
            Field f = ConfigKey.class.getDeclaredField("_value");
            f.setAccessible(true);
            f.set(configKey, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void testPurgeLinkedSnapshotEntitiesNoSnapshots() {
        resourceCleanupService.purgeLinkedSnapshotEntities(new ArrayList<>(), batchSize);
        Mockito.verify(snapshotDetailsDao, Mockito.never())
                .batchExpungeForResources(Mockito.anyList(), Mockito.anyLong());
        Mockito.verify(snapshotDataStoreDao, Mockito.never())
                .expungeBySnapshotList(Mockito.anyList(), Mockito.anyLong());
    }


    @Test
    public void testPurgeLinkedSnapshotEntities() {
        Mockito.when(snapshotDetailsDao.batchExpungeForResources(ids, batchSize)).thenReturn(2L);
        Mockito.when(snapshotDataStoreDao.expungeBySnapshotList(ids, batchSize)).thenReturn(2);
        resourceCleanupService.purgeLinkedSnapshotEntities(ids, batchSize);
        Mockito.verify(snapshotDetailsDao, Mockito.times(1))
                .batchExpungeForResources(ids, batchSize);
        Mockito.verify(snapshotDataStoreDao, Mockito.times(1))
                .expungeBySnapshotList(ids, batchSize);
    }

    @Test
    public void testPurgeVolumeSnapshotsNoVolumes() {
        Assert.assertEquals(0, resourceCleanupService.purgeVolumeSnapshots(new ArrayList<>(), 50L));
        Mockito.verify(snapshotDao, Mockito.never()).createSearchBuilder();
    }

    @Test
    public void testPurgeVolumeSnapshots() {
        SearchBuilder<SnapshotVO> sb = Mockito.mock(SearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(Mockito.mock(SnapshotVO.class));
        Mockito.when(sb.create()).thenReturn(Mockito.mock(SearchCriteria.class));
        Mockito.when(snapshotDao.createSearchBuilder()).thenReturn(sb);
        Assert.assertEquals(0, resourceCleanupService.purgeVolumeSnapshots(new ArrayList<>(), 50L));
        Mockito.when(snapshotDao.searchIncludingRemoved(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(List.of(Mockito.mock(SnapshotVO.class), Mockito.mock(SnapshotVO.class)));
        Mockito.when(snapshotDao.expungeList(Mockito.anyList())).thenReturn(2);
        Assert.assertEquals(2, resourceCleanupService.purgeVolumeSnapshots(ids, batchSize));
    }

    @Test
    public void testPurgeLinkedVolumeEntitiesNoVolumes() {
        resourceCleanupService.purgeLinkedVolumeEntities(new ArrayList<>(), 50L);
        Mockito.verify(volumeDetailsDao, Mockito.never()).batchExpungeForResources(Mockito.anyList(),
                Mockito.anyLong());
    }

    @Test
    public void testPurgeLinkedVolumeEntities() {
        Mockito.when(volumeDetailsDao.batchExpungeForResources(ids, batchSize)).thenReturn(2L);
        Mockito.when(volumeDataStoreDao.expungeByVolumeList(ids, batchSize)).thenReturn(2);
        Mockito.doReturn(2L).when(resourceCleanupService).purgeVolumeSnapshots(ids, batchSize);
        resourceCleanupService.purgeLinkedVolumeEntities(ids, batchSize);
        Mockito.verify(volumeDetailsDao, Mockito.times(1))
                .batchExpungeForResources(ids, batchSize);
        Mockito.verify(volumeDataStoreDao, Mockito.times(1))
                .expungeByVolumeList(ids, batchSize);
        Mockito.verify(resourceCleanupService, Mockito.times(1))
                .purgeVolumeSnapshots(ids, batchSize);
    }

    @Test
    public void testPurgeVMVolumesNoVms() {
        Assert.assertEquals(0, resourceCleanupService.purgeVMVolumes(new ArrayList<>(), 50L));
        Mockito.verify(volumeDao, Mockito.never()).searchRemovedByVms(Mockito.anyList(), Mockito.anyLong());
    }

    @Test
    public void testPurgeVMVolumes() {
        Mockito.when(volumeDao.searchRemovedByVms(ids, batchSize))
                .thenReturn(List.of(Mockito.mock(VolumeVO.class), Mockito.mock(VolumeVO.class)));
        Mockito.when(volumeDao.expungeList(Mockito.anyList())).thenReturn(2);
        Mockito.doNothing().when(resourceCleanupService).purgeLinkedVolumeEntities(Mockito.anyList(),
                Mockito.eq(batchSize));
        Assert.assertEquals(2, resourceCleanupService.purgeVMVolumes(ids, batchSize));
    }

    @Test
    public void testPurgeLinkedNicEntitiesNoNics() {
        resourceCleanupService.purgeLinkedNicEntities(new ArrayList<>(), batchSize);
        Mockito.verify(nicDetailsDao, Mockito.never())
                .batchExpungeForResources(ids, batchSize);
        Mockito.verify(nicExtraDhcpOptionDao, Mockito.never())
                .expungeByNicList(ids, batchSize);
        Mockito.verify(inlineLoadBalancerNicMapDao, Mockito.never())
                .expungeByNicList(ids, batchSize);
    }

    @Test
    public void testPurgeLinkedNicEntities() {
        Mockito.when(nicDetailsDao.batchExpungeForResources(ids, batchSize)).thenReturn(2L);
        Mockito.when(nicExtraDhcpOptionDao.expungeByNicList(ids, batchSize)).thenReturn(2);
        Mockito.when(inlineLoadBalancerNicMapDao.expungeByNicList(ids, batchSize)).thenReturn(2);
        resourceCleanupService.purgeLinkedNicEntities(ids, batchSize);
        Mockito.verify(nicDetailsDao, Mockito.times(1))
                .batchExpungeForResources(ids, batchSize);
        Mockito.verify(nicExtraDhcpOptionDao, Mockito.times(1))
                .expungeByNicList(ids, batchSize);
        Mockito.verify(inlineLoadBalancerNicMapDao, Mockito.times(1))
                .expungeByNicList(ids, batchSize);
    }

    @Test
    public void testPurgeVMNicsNoVms() {
        Assert.assertEquals(0, resourceCleanupService.purgeVMNics(new ArrayList<>(), 50L));
        Mockito.verify(nicDao, Mockito.never()).searchRemovedByVms(Mockito.anyList(), Mockito.anyLong());
    }

    @Test
    public void testPurgeVMNics() {
        Mockito.when(nicDao.searchRemovedByVms(ids, batchSize))
                .thenReturn(List.of(Mockito.mock(NicVO.class), Mockito.mock(NicVO.class)));
        Mockito.when(nicDao.expungeList(Mockito.anyList())).thenReturn(2);
        Mockito.doNothing().when(resourceCleanupService).purgeLinkedNicEntities(Mockito.anyList(),
                Mockito.eq(batchSize));
        Assert.assertEquals(2, resourceCleanupService.purgeVMNics(ids, batchSize));
    }

    @Test
    public void testPurgeVMSnapshotsNoVms() {
        Assert.assertEquals(0, resourceCleanupService.purgeVMSnapshots(new ArrayList<>(), 50L));
        Mockito.verify(vmSnapshotDao, Mockito.never()).searchRemovedByVms(Mockito.anyList(), Mockito.anyLong());
    }

    @Test
    public void testPurgeVMSnapshots() {
        Mockito.when(vmSnapshotDao.searchRemovedByVms(ids, batchSize))
                .thenReturn(List.of(Mockito.mock(VMSnapshotVO.class), Mockito.mock(VMSnapshotVO.class)));
        Mockito.when(vmSnapshotDao.expungeList(Mockito.anyList())).thenReturn(2);
        Mockito.when(vmSnapshotDetailsDao.batchExpungeForResources(Mockito.anyList(),
                Mockito.eq(batchSize))).thenReturn(2L);
        Assert.assertEquals(2, resourceCleanupService.purgeVMSnapshots(ids, batchSize));
    }

    @Test
    public void testPurgeLinkedVMEntitiesNoVms() {
        resourceCleanupService.purgeLinkedVMEntities(new ArrayList<>(), 50L);
        Mockito.verify(resourceCleanupService, Mockito.never()).purgeVMVolumes(Mockito.anyList(),
                Mockito.anyLong());
        Mockito.verify(userVmDetailsDao, Mockito.never())
                .batchExpungeForResources(Mockito.anyList(), Mockito.anyLong());
    }

    @Test
    public void testPurgeLinkedVMEntities() {
        Mockito.doReturn(2L).when(resourceCleanupService).purgeVMVolumes(Mockito.anyList(),
                Mockito.eq(batchSize));
        Mockito.doReturn(2L).when(resourceCleanupService).purgeVMNics(Mockito.anyList(),
                Mockito.eq(batchSize));
        Mockito.when(userVmDetailsDao.batchExpungeForResources(Mockito.anyList(), Mockito.anyLong())).thenReturn(2L);
        Mockito.doReturn(2L).when(resourceCleanupService).purgeVMSnapshots(Mockito.anyList(),
                Mockito.eq(batchSize));
        Mockito.when(autoScaleVmGroupVmMapDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(commandExecLogDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(loadBalancerVMMapDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(nicSecondaryIpDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(highAvailabilityManager.expungeWorkItemsByVmList(Mockito.anyList(), Mockito.anyLong()))
                .thenReturn(2);
        Mockito.when(itWorkDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(opRouterMonitorServiceDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(portForwardingRulesDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(ipAddressDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(vmWorkJobDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);
        Mockito.when(consoleSessionDao.expungeByVmList(Mockito.anyList(), Mockito.anyLong())).thenReturn(2);

        resourceCleanupService.purgeLinkedVMEntities(ids, batchSize);

        Mockito.verify(resourceCleanupService, Mockito.times(1)).purgeVMVolumes(ids, batchSize);
        Mockito.verify(resourceCleanupService, Mockito.times(1)).purgeVMNics(ids, batchSize);
        Mockito.verify(userVmDetailsDao, Mockito.times(1))
                .batchExpungeForResources(ids, batchSize);
        Mockito.verify(resourceCleanupService, Mockito.times(1))
                .purgeVMSnapshots(ids, batchSize);
        Mockito.verify(autoScaleVmGroupVmMapDao, Mockito.times(1))
                .expungeByVmList(ids, batchSize);
        Mockito.verify(commandExecLogDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
        Mockito.verify(loadBalancerVMMapDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
        Mockito.verify(nicSecondaryIpDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
        Mockito.verify(highAvailabilityManager, Mockito.times(1)).
                expungeWorkItemsByVmList(ids, batchSize);
        Mockito.verify(itWorkDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
        Mockito.verify(opRouterMonitorServiceDao, Mockito.times(1))
                .expungeByVmList(ids, batchSize);
        Mockito.verify(portForwardingRulesDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
        Mockito.verify(ipAddressDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
        Mockito.verify(vmWorkJobDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
        Mockito.verify(consoleSessionDao, Mockito.times(1)).expungeByVmList(ids, batchSize);
    }

    @Test
    public void testGetVmIdsWithActiveVolumeSnapshotsNoVms() {
        Assert.assertTrue(CollectionUtils.isEmpty(
                resourceCleanupService.getVmIdsWithActiveVolumeSnapshots(new ArrayList<>())));
    }

    @Test
    public void testGetVmIdsWithActiveVolumeSnapshots() {
        VolumeVO vol1 = Mockito.mock(VolumeVO.class);
        Mockito.when(vol1.getId()).thenReturn(1L);
        Mockito.when(vol1.getInstanceId()).thenReturn(1L);
        VolumeVO vol2 = Mockito.mock(VolumeVO.class);
        Mockito.when(vol2.getId()).thenReturn(2L);
        Mockito.when(volumeDao.searchRemovedByVms(ids, null)).thenReturn(List.of(vol1, vol2));
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getVolumeId()).thenReturn(1L);
        Mockito.when(snapshotDao.searchByVolumes(Mockito.anyList())).thenReturn(List.of(snapshotVO));
        HashSet<Long> vmIds = resourceCleanupService.getVmIdsWithActiveVolumeSnapshots(ids);
        Assert.assertTrue(CollectionUtils.isNotEmpty(vmIds));
        Assert.assertEquals(1, vmIds.size());
        Assert.assertEquals(1L, vmIds.toArray()[0]);
    }

    @Test
    public void testGetFilteredVmIdsForSnapshots() {
        Long skippedVmIds = ids.get(0);
        Long notSkippedVmIds = ids.get(1);
        VMSnapshotVO vmSnapshotVO = Mockito.mock(VMSnapshotVO.class);
        Mockito.when(vmSnapshotVO.getVmId()).thenReturn(1L);
        Mockito.when(vmSnapshotDao.searchByVms(Mockito.anyList())).thenReturn(List.of(vmSnapshotVO));
        HashSet<Long> set = new HashSet<>();
        set.add(1L);
        Mockito.doReturn(set).when(resourceCleanupService).getVmIdsWithActiveVolumeSnapshots(ids);
        Pair<List<Long>, List<Long>> result = resourceCleanupService.getFilteredVmIdsForSnapshots(new ArrayList<>(ids));
        Assert.assertEquals(1, result.first().size());
        Assert.assertEquals(1, result.second().size());
        Assert.assertEquals(notSkippedVmIds, result.first().get(0));
        Assert.assertEquals(skippedVmIds, result.second().get(0));
    }

    @Test
    public void testGetVmIdsWithNoActiveSnapshots() {
        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(ids.get(0));
        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getId()).thenReturn(ids.get(1));
        Mockito.when(vmInstanceDao.searchRemovedByRemoveDate(Mockito.any(), Mockito.any(),
                        Mockito.anyLong(), Mockito.anyList())).thenReturn(List.of(vm1, vm2));
        Long skippedVmIds = ids.get(0);
        Long notSkippedVmIds = ids.get(1);
        VMSnapshotVO vmSnapshotVO = Mockito.mock(VMSnapshotVO.class);
        Mockito.when(vmSnapshotVO.getVmId()).thenReturn(1L);
        Mockito.when(vmSnapshotDao.searchByVms(Mockito.anyList())).thenReturn(List.of(vmSnapshotVO));
        HashSet<Long> set = new HashSet<>();
        set.add(1L);
        Mockito.doReturn(set).when(resourceCleanupService).getVmIdsWithActiveVolumeSnapshots(Mockito.anyList());
        Pair<List<Long>, List<Long>> result =
                resourceCleanupService.getVmIdsWithNoActiveSnapshots(new Date(), new Date(), batchSize,
                        new ArrayList<>());
        Assert.assertEquals(1, result.first().size());
        Assert.assertEquals(1, result.second().size());
        Assert.assertEquals(notSkippedVmIds, result.first().get(0));
        Assert.assertEquals(skippedVmIds, result.second().get(0));
    }

    @Test
    public void testPurgeVMEntitiesNoVms() {
        Mockito.when(vmInstanceDao.searchRemovedByRemoveDate(Mockito.any(), Mockito.any(),
                Mockito.anyLong(), Mockito.anyList())).thenReturn(new ArrayList<>());
        Assert.assertEquals(0, resourceCleanupService.purgeVMEntities(batchSize, new Date(), new Date()));
    }

    @Test
    public void testPurgeVMEntities() {
        Mockito.doReturn(new Pair<>(ids, new ArrayList<>())).when(resourceCleanupService)
                .getVmIdsWithNoActiveSnapshots(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyList());
        Mockito.when(vmInstanceDao.expungeList(ids)).thenReturn(ids.size());
        Assert.assertEquals(ids.size(), resourceCleanupService.purgeVMEntities(batchSize, new Date(), new Date()));
    }

    @Test
    public void testExpungeVMEntityFiltered() {
        Mockito.doReturn(new Pair<>(new ArrayList<>(), List.of(ids.get(0)))).when(resourceCleanupService)
                .getFilteredVmIdsForSnapshots(Mockito.anyList());
        Assert.assertFalse(resourceCleanupService.purgeVMEntity(ids.get(0)));
    }

    @Test
    public void testPurgeVMEntityFiltered() {
        Mockito.doReturn(new Pair<>(List.of(ids.get(0)), new ArrayList<>())).when(resourceCleanupService)
                .getFilteredVmIdsForSnapshots(Mockito.anyList());
        Mockito.doNothing().when(resourceCleanupService)
                .purgeLinkedVMEntities(Mockito.anyList(), Mockito.anyLong());
        Mockito.when(vmInstanceDao.expunge(ids.get(0))).thenReturn(true);
        Assert.assertTrue(resourceCleanupService.purgeVMEntity(ids.get(0)));
    }

    @Test
    public void testPurgeVMEntity() {
        Mockito.doReturn(new Pair<>(List.of(ids.get(0)), new ArrayList<>())).when(resourceCleanupService)
                .getFilteredVmIdsForSnapshots(Mockito.anyList());
        Mockito.doNothing().when(resourceCleanupService)
                .purgeLinkedVMEntities(Mockito.anyList(), Mockito.anyLong());
        Mockito.when(vmInstanceDao.expunge(ids.get(0))).thenReturn(true);
        Assert.assertTrue(resourceCleanupService.purgeVMEntity(ids.get(0)));
    }

    @Test
    public void testPurgeEntities() {
        Mockito.doReturn((long)ids.size()).when(resourceCleanupService)
                .purgeVMEntities(Mockito.anyLong(), Mockito.any(), Mockito.any());
        long result = resourceCleanupService.purgeEntities(
                List.of(ResourceCleanupService.ResourceType.VirtualMachine), batchSize, new Date(), new Date());
        Assert.assertEquals(ids.size(), result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetResourceTypeAndValidatePurgeExpungedResourcesCmdParamsInvalidResourceType() {
        resourceCleanupService.getResourceTypeAndValidatePurgeExpungedResourcesCmdParams("Volume",
                new Date(), new Date(), batchSize);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetResourceTypeAndValidatePurgeExpungedResourcesCmdParamsInvalidBatchSize() {
        resourceCleanupService.getResourceTypeAndValidatePurgeExpungedResourcesCmdParams(
                ResourceCleanupService.ResourceType.VirtualMachine.toString(),
                new Date(), new Date(), -1L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetResourceTypeAndValidatePurgeExpungedResourcesCmdParamsInvalidDates() {
        Calendar cal = Calendar.getInstance();
        Date startDate = new Date();
        cal.setTime(startDate);
        cal.add(Calendar.DATE, -1);
        Date endDate = cal.getTime();
        resourceCleanupService.getResourceTypeAndValidatePurgeExpungedResourcesCmdParams(
                ResourceCleanupService.ResourceType.VirtualMachine.toString(),
                startDate, endDate, 100L);
    }

    @Test
    public void testGetResourceTypeAndValidatePurgeExpungedResourcesCmdParams() {
        Calendar cal = Calendar.getInstance();
        Date endDate = new Date();
        cal.setTime(endDate);
        cal.add(Calendar.DATE, -1);
        Date startDate = cal.getTime();
        ResourceCleanupService.ResourceType type =
                resourceCleanupService.getResourceTypeAndValidatePurgeExpungedResourcesCmdParams(
                ResourceCleanupService.ResourceType.VirtualMachine.toString(),
                startDate, endDate, 100L);
        Assert.assertEquals(ResourceCleanupService.ResourceType.VirtualMachine, type);
    }

    @Test
    public void testGetResourceTypeAndValidatePurgeExpungedResourcesCmdParamsNoValues() {
        ResourceCleanupService.ResourceType type =
                resourceCleanupService.getResourceTypeAndValidatePurgeExpungedResourcesCmdParams(
                        null, null, null, null);
        Assert.assertNull(type);
    }

    @Test
    public void testIsVmOfferingPurgeResourcesEnabled() {
        Mockito.when(serviceOfferingDetailsDao.getDetail(1L,
                ServiceOffering.PURGE_DB_ENTITIES_KEY)).thenReturn(null);
        Assert.assertFalse(resourceCleanupService.isVmOfferingPurgeResourcesEnabled(1L));
        Mockito.when(serviceOfferingDetailsDao.getDetail(2L,
                ServiceOffering.PURGE_DB_ENTITIES_KEY)).thenReturn("false");
        Assert.assertFalse(resourceCleanupService.isVmOfferingPurgeResourcesEnabled(2L));
        Mockito.when(serviceOfferingDetailsDao.getDetail(3L,
                ServiceOffering.PURGE_DB_ENTITIES_KEY)).thenReturn("true");
        Assert.assertTrue(resourceCleanupService.isVmOfferingPurgeResourcesEnabled(3L));
    }

    @Test
    public void testPurgeExpungedResource() {
        Assert.assertFalse(resourceCleanupService.purgeExpungedResource(1L, null));

        Mockito.doReturn(true).when(resourceCleanupService)
                .purgeExpungedResource(Mockito.anyLong(), Mockito.any());
        Assert.assertTrue(resourceCleanupService.purgeExpungedResource(1L,
                ResourceCleanupService.ResourceType.VirtualMachine));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testPurgeExpungedResourcesInvalidResourceType() {
        PurgeExpungedResourcesCmd cmd = Mockito.mock(PurgeExpungedResourcesCmd.class);
        Mockito.when(cmd.getResourceType()).thenReturn("Volume");
        resourceCleanupService.purgeExpungedResources(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testPurgeExpungedResourcesInvalidBatchSize() {
        PurgeExpungedResourcesCmd cmd = Mockito.mock(PurgeExpungedResourcesCmd.class);
        Mockito.when(cmd.getBatchSize()).thenReturn(-1L);
        resourceCleanupService.purgeExpungedResources(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testPurgeExpungedResourcesInvalidDates() {
        Calendar cal = Calendar.getInstance();
        Date startDate = new Date();
        cal.setTime(startDate);
        cal.add(Calendar.DATE, -1);
        Date endDate = cal.getTime();
        PurgeExpungedResourcesCmd cmd = Mockito.mock(PurgeExpungedResourcesCmd.class);
        Mockito.when(cmd.getStartDate()).thenReturn(startDate);
        Mockito.when(cmd.getEndDate()).thenReturn(endDate);
        resourceCleanupService.purgeExpungedResources(cmd);
    }

    @Test
    public void testPurgeExpungedResources() {
        Mockito.doReturn((long)ids.size()).when(resourceCleanupService).purgeExpungedResourceUsingJob(
                ResourceCleanupService.ResourceType.VirtualMachine, batchSize, null, null);
        PurgeExpungedResourcesCmd cmd = Mockito.mock(PurgeExpungedResourcesCmd.class);
        Mockito.when(cmd.getResourceType()).thenReturn(ResourceCleanupService.ResourceType.VirtualMachine.toString());
        Mockito.when(cmd.getBatchSize()).thenReturn(batchSize);
        long result = resourceCleanupService.purgeExpungedResources(cmd);
        Assert.assertEquals(ids.size(), result);
    }

    @Test
    public void testExpungedVmResourcesLaterIfNeededFalse() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(1L);
        Mockito.doReturn(false).when(resourceCleanupService).isVmOfferingPurgeResourcesEnabled(1L);
        resourceCleanupService.purgeExpungedVmResourcesLaterIfNeeded(vm);
        Mockito.verify(resourceCleanupService, Mockito.never()).purgeExpungedResourceLater(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void testExpungedVmResourcesLaterIfNeeded() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(1L);
        Mockito.doReturn(true).when(resourceCleanupService).isVmOfferingPurgeResourcesEnabled(1L);
        Mockito.doNothing().when(resourceCleanupService).purgeExpungedResourceLater(Mockito.anyLong(), Mockito.any());
        resourceCleanupService.purgeExpungedVmResourcesLaterIfNeeded(vm);
        Mockito.verify(resourceCleanupService, Mockito.times(1))
                .purgeExpungedResourceLater(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void testGetBatchSizeFromConfig() {
        int value = 50;
        overrideConfigValue(ResourceCleanupService.ExpungedResourcesPurgeBatchSize, String.valueOf(value));
        Assert.assertEquals(value, resourceCleanupService.getBatchSizeFromConfig());
    }

    @Test
    public void testGetResourceTypesFromConfigEmpty() {
        overrideConfigValue(ResourceCleanupService.ExpungedResourcePurgeResources, "");
        Assert.assertNull(resourceCleanupService.getResourceTypesFromConfig());
    }

    @Test
    public void testGetResourceTypesFromConfig() {
        overrideConfigValue(ResourceCleanupService.ExpungedResourcePurgeResources, "VirtualMachine");
        List<ResourceCleanupService.ResourceType> types  = resourceCleanupService.getResourceTypesFromConfig();
        Assert.assertEquals(1, types.size());
    }

    @Test
    public void testCalculatePastDateFromConfigNull() {
        Assert.assertNull(resourceCleanupService.calculatePastDateFromConfig(
                ResourceCleanupService.ExpungedResourcesPurgeKeepPastDays.key(),
                null));
        Assert.assertNull(resourceCleanupService.calculatePastDateFromConfig(
                ResourceCleanupService.ExpungedResourcesPurgeKeepPastDays.key(),
                0));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCalculatePastDateFromConfigFail() {
        Assert.assertNull(resourceCleanupService.calculatePastDateFromConfig(
                ResourceCleanupService.ExpungedResourcesPurgeKeepPastDays.key(),
                -1));
    }

    @Test
    public void testCalculatePastDateFromConfig() {
        int days = 10;
        Date result = resourceCleanupService.calculatePastDateFromConfig(
                ResourceCleanupService.ExpungedResourcesPurgeKeepPastDays.key(),
                days);
        Date today = new Date();
        long diff = today.getTime() - result.getTime();
        Assert.assertEquals(days, TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testParseDateFromConfig() {
        Assert.assertNull(resourceCleanupService.parseDateFromConfig(
                ResourceCleanupService.ExpungedResourcesPurgeStartTime.key(), ""));
        Date date = resourceCleanupService.parseDateFromConfig(
                ResourceCleanupService.ExpungedResourcesPurgeStartTime.key(), "2020-01-01");
        Assert.assertNotNull(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Assert.assertEquals(2020, calendar.get(Calendar.YEAR));
        Assert.assertEquals(0, calendar.get(Calendar.MONTH));
        Assert.assertEquals(1, calendar.get(Calendar.DATE));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testParseDateFromConfigFail() {
        resourceCleanupService.parseDateFromConfig(
                ResourceCleanupService.ExpungedResourcesPurgeStartTime.key(), "ABC");
    }
}
