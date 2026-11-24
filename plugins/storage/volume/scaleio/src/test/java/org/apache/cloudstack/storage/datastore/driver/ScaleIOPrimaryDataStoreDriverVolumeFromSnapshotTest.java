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
//

package org.apache.cloudstack.storage.datastore.driver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.datastore.api.Volume;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.manager.ScaleIOSDCManager;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOPrimaryDataStoreDriverVolumeFromSnapshotTest {

    private static final long POOL_ID = 7L;
    private static final long SNAPSHOT_ID = 42L;
    private static final long NEW_VOLUME_ID = 100L;
    private static final long BASE_VOLUME_ID = 99L;
    private static final long HOST_ID = 5L;
    private static final long NEW_VOLUME_SIZE = 16L << 30;
    private static final String SDC_ID = "c70c930300000001";
    private static final String SNAPSHOT_VOLUME_ID = "e44cfc4c00000002";
    private static final String SNAPSHOT_PATH = SNAPSHOT_VOLUME_ID + ":snap-42-abcd-sim";
    private static final String NEW_VOLUME_PF_ID = "e44cfc4c00000003";
    private static final String NEW_VOLUME_PATH = NEW_VOLUME_PF_ID + ":vol-100-abcd-sim";

    @Spy
    @InjectMocks
    ScaleIOPrimaryDataStoreDriver driver = new ScaleIOPrimaryDataStoreDriver();

    @Mock
    VolumeDetailsDao volumeDetailsDao;
    @Mock
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Mock
    SnapshotDao snapshotDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    PrimaryDataStoreDao storagePoolDao;
    @Mock
    DataStoreManager dataStoreMgr;
    @Mock
    VolumeDataFactory volumeDataFactory;
    @Mock
    EndPointSelector selector;
    @Mock
    HostDao hostDao;
    @Mock
    ConfigurationDao configDao;
    @Mock
    ResourceManager resourceMgr;
    @Mock
    ClusterDao clusterDao;
    @Mock
    ScaleIOSDCManager sdcManager;
    @Mock
    AlertManager alertMgr;

    @Mock
    ScaleIOGatewayClient client;
    @Mock
    StoragePoolVO storagePool;
    @Mock
    VolumeVO newVolume;
    @Mock
    VolumeVO baseVolume;
    @Mock
    SnapshotDataStoreVO snapshotStore;
    @Mock
    PrimaryDataStore dataStore;
    @Mock
    VolumeInfo newVolumeInfo;
    @Mock
    EndPoint endPoint;
    @Mock
    HostVO host;
    @Mock
    GlobalLock lock;

    @Before
    public void setUp() {
        lenient().when(storagePool.getId()).thenReturn(POOL_ID);
        lenient().when(newVolume.getId()).thenReturn(NEW_VOLUME_ID);
        lenient().when(newVolume.getPath()).thenReturn(NEW_VOLUME_PATH);
        lenient().when(newVolume.getSize()).thenReturn(NEW_VOLUME_SIZE);
        lenient().when(snapshotStore.getInstallPath()).thenReturn(SNAPSHOT_PATH);
        lenient().when(snapshotStore.getDataStoreId()).thenReturn(POOL_ID);
        lenient().when(snapshotStore.getSize()).thenReturn(16L << 30);
        lenient().when(snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(SNAPSHOT_ID, DataStoreRole.Primary)).thenReturn(snapshotStore);

        SnapshotVO snapshot = mock(SnapshotVO.class);
        lenient().when(snapshot.getVolumeId()).thenReturn(BASE_VOLUME_ID);
        lenient().when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(snapshot);
        // what KVM usually records for PowerFlex data volumes, although the PowerFlex device holds raw data
        lenient().when(baseVolume.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        lenient().when(volumeDao.findByIdIncludingRemoved(BASE_VOLUME_ID)).thenReturn(baseVolume);
        // unencrypted unless a test says otherwise (Mockito would return 0L for these Long getters)
        lenient().when(baseVolume.getPassphraseId()).thenReturn(null);
        lenient().when(baseVolume.getKmsKeyId()).thenReturn(null);
        lenient().when(newVolumeInfo.getPassphraseId()).thenReturn(null);
        lenient().when(newVolumeInfo.getKmsKeyId()).thenReturn(null);

        lenient().when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenReturn(dataStore);
        lenient().when(volumeDataFactory.getVolume(NEW_VOLUME_ID, dataStore)).thenReturn(newVolumeInfo);
        lenient().when(newVolumeInfo.getTO()).thenReturn(new VolumeObjectTO());
        lenient().when(endPoint.getId()).thenReturn(HOST_ID);
        lenient().when(hostDao.findById(HOST_ID)).thenReturn(host);
        lenient().when(storagePoolDao.findById(POOL_ID)).thenReturn(storagePool);
    }

    private void stubCopyPath() {
        doReturn(List.of(host)).when(driver).findHostsForCopy(newVolumeInfo, storagePool, dataStore);
        lenient().when(host.getId()).thenReturn(HOST_ID);
        lenient().doReturn(endPoint).when(driver).getEndPoint(host);
        lenient().doReturn(lock).when(driver).acquireSnapshotCopyLock(eq(SNAPSHOT_VOLUME_ID), eq(HOST_ID), anyInt());
        lenient().doNothing().when(driver).unprepareSdcIfUnused(any(), any(), anyString(), any());
        lenient().doNothing().when(driver).unprepareSdcAfterFailedPrepare(any(), any());
        lenient().doNothing().when(driver).releaseSnapshotCopyLock(lock);
        lenient().doReturn(SDC_ID).when(driver).prepareSdcOnHost(host, dataStore);
        lenient().doReturn(mock(PrimaryDataStoreTO.class)).when(driver).getPrimaryDataStoreTOForManagedVolume(any(), anyString(), any(), anyLong());
        lenient().doNothing().when(driver).revokeVolumeAccess(anyString(), any(), any());
        lenient().when(client.isVolumeMappedToSdc(SNAPSHOT_VOLUME_ID, SDC_ID)).thenReturn(false);
        lenient().when(client.mapVolumeToSdc(anyString(), eq(SDC_ID))).thenReturn(true);
        lenient().when(client.deleteVolume(NEW_VOLUME_PF_ID)).thenReturn(true);
    }

    private void copy() {
        driver.copySnapshotToNewVolume(SNAPSHOT_ID, snapshotStore, newVolume, storagePool, client);
    }

    private void assertCopyFailsAndNewVolumeIsRemoved() {
        Assert.assertThrows(CloudRuntimeException.class, this::copy);
        verify(client).deleteVolume(NEW_VOLUME_PF_ID);
    }

    // ---- getSnapshotIdToCreateVolumeFrom

    @Test
    public void getSnapshotIdToCreateVolumeFromWithoutDetail() {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(NEW_VOLUME_ID);
        Assert.assertNull(driver.getSnapshotIdToCreateVolumeFrom(volumeInfo));
    }

    @Test
    public void getSnapshotIdToCreateVolumeFromWithCloneOfSnapshotDetail() {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(NEW_VOLUME_ID);
        VolumeDetailVO detail = mock(VolumeDetailVO.class);
        when(detail.getValue()).thenReturn(String.valueOf(SNAPSHOT_ID));
        when(volumeDetailsDao.findDetail(NEW_VOLUME_ID, ScaleIOPrimaryDataStoreDriver.CLONE_OF_SNAPSHOT)).thenReturn(detail);
        Assert.assertEquals(Long.valueOf(SNAPSHOT_ID), driver.getSnapshotIdToCreateVolumeFrom(volumeInfo));
    }

    // ---- validateSnapshotToCreateVolumeFrom

    @Test
    public void validateSnapshotToCreateVolumeFromReturnsTheSnapshotStore() {
        Assert.assertSame(snapshotStore, driver.validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, newVolumeInfo, storagePool));
    }

    @Test
    public void validateSnapshotToCreateVolumeFromSnapshotNotOnPrimary() {
        when(snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(SNAPSHOT_ID, DataStoreRole.Primary)).thenReturn(null);
        Assert.assertThrows(CloudRuntimeException.class, () -> driver.validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, newVolumeInfo, storagePool));
    }

    @Test
    public void validateSnapshotToCreateVolumeFromSnapshotOnAnotherPool() {
        when(snapshotStore.getDataStoreId()).thenReturn(POOL_ID + 1);
        Assert.assertThrows(CloudRuntimeException.class, () -> driver.validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, newVolumeInfo, storagePool));
    }

    @Test
    public void validateSnapshotToCreateVolumeFromEncryptedNewVolume() {
        doReturn(true).when(driver).anyVolumeRequiresEncryption(newVolumeInfo);
        Assert.assertThrows(CloudRuntimeException.class, () -> driver.validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, newVolumeInfo, storagePool));
    }

    @Test
    public void validateSnapshotToCreateVolumeFromEncryptedSourceWithPassphrase() {
        when(baseVolume.getPassphraseId()).thenReturn(3L);
        Assert.assertThrows(CloudRuntimeException.class, () -> driver.validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, newVolumeInfo, storagePool));
    }

    @Test
    public void validateSnapshotToCreateVolumeFromRefusesWhenTheSnapshotVolumeIsGone() {
        when(volumeDao.findByIdIncludingRemoved(BASE_VOLUME_ID)).thenReturn(null);
        Assert.assertThrows(CloudRuntimeException.class, () -> driver.validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, newVolumeInfo, storagePool));
    }

    @Test
    public void validateSnapshotToCreateVolumeFromEncryptedSourceWithKmsKey() {
        when(baseVolume.getKmsKeyId()).thenReturn(4L);
        Assert.assertThrows(CloudRuntimeException.class, () -> driver.validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, newVolumeInfo, storagePool));
    }

    // ---- copySnapshotToNewVolume

    @Test
    public void copyMapsCopiesAsRawAndUnmapsUnderTheSnapshotLock() {
        stubCopyPath();
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, true, null));

        copy();

        ArgumentCaptor<CopyCommand> cmdCaptor = ArgumentCaptor.forClass(CopyCommand.class);
        InOrder order = inOrder(client, endPoint, driver);
        order.verify(driver).acquireSnapshotCopyLock(eq(SNAPSHOT_VOLUME_ID), eq(HOST_ID), anyInt());
        order.verify(driver).prepareSdcOnHost(host, dataStore);
        order.verify(client).mapVolumeToSdc(SNAPSHOT_VOLUME_ID, SDC_ID);
        order.verify(client).mapVolumeToSdc(NEW_VOLUME_PF_ID, SDC_ID);
        order.verify(endPoint).sendMessage(cmdCaptor.capture());
        order.verify(driver).revokeVolumeAccess(SNAPSHOT_PATH, host, dataStore);
        order.verify(driver).revokeVolumeAccess(NEW_VOLUME_PATH, host, dataStore);
        order.verify(driver).releaseSnapshotCopyLock(lock);

        CopyCommand cmd = cmdCaptor.getValue();
        Assert.assertEquals(SNAPSHOT_PATH, cmd.getSrcTO().getPath());
        Assert.assertEquals("the source must be RAW even when the volume record says QCOW2",
                Storage.ImageFormat.RAW, ((VolumeObjectTO) cmd.getSrcTO()).getFormat());
        Assert.assertEquals(NEW_VOLUME_PATH, cmd.getDestTO().getPath());
        Assert.assertEquals(Storage.ImageFormat.RAW, ((VolumeObjectTO) cmd.getDestTO()).getFormat());
        // the agent finds each device through the managed store target of its TO
        verify(driver).getPrimaryDataStoreTOForManagedVolume(eq(storagePool), eq(SNAPSHOT_PATH), any(), anyLong());
        verify(driver).getPrimaryDataStoreTOForManagedVolume(eq(storagePool), eq(NEW_VOLUME_PATH), any(), eq(NEW_VOLUME_SIZE));
        verify(client, never()).deleteVolume(anyString());
        verify(storagePoolDao, never()).update(anyLong(), any());
    }

    @Test
    public void copyFailedAnswerWithoutDetailsIsAFailure() {
        stubCopyPath();
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, false, null));
        assertCopyFailsAndNewVolumeIsRemoved();
    }

    @Test
    public void copyExceptionWithoutMessageIsAFailure() {
        stubCopyPath();
        when(endPoint.sendMessage(any(CopyCommand.class))).thenThrow(new RuntimeException());
        assertCopyFailsAndNewVolumeIsRemoved();
        verify(driver).revokeVolumeAccess(NEW_VOLUME_PATH, host, dataStore);
    }

    @Test
    public void copyLeavesAnExistingSnapshotMappingInPlace() {
        stubCopyPath();
        when(client.isVolumeMappedToSdc(SNAPSHOT_VOLUME_ID, SDC_ID)).thenReturn(true);
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, true, null));

        copy();

        verify(client, never()).mapVolumeToSdc(SNAPSHOT_VOLUME_ID, SDC_ID);
        verify(driver, never()).revokeVolumeAccess(eq(SNAPSHOT_PATH), any(), any());
        verify(driver).revokeVolumeAccess(NEW_VOLUME_PATH, host, dataStore);
    }

    @Test
    public void copyFailureRemovesNewVolumeUnmapsAndReleasesUsedBytes() {
        stubCopyPath();
        when(storagePool.getUsedBytes()).thenReturn(100L << 30);
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, false, "qemu-img convert failed"));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, this::copy);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("qemu-img convert failed"));
        verify(driver).revokeVolumeAccess(SNAPSHOT_PATH, host, dataStore);
        verify(driver).revokeVolumeAccess(NEW_VOLUME_PATH, host, dataStore);
        verify(driver).releaseSnapshotCopyLock(lock);
        verify(client).deleteVolume(NEW_VOLUME_PF_ID);
        verify(storagePool).setUsedBytes((100L << 30) - NEW_VOLUME_SIZE);
        verify(storagePoolDao).update(POOL_ID, storagePool);
    }

    @Test
    public void copyWithoutAnswerRemovesNewVolume() {
        stubCopyPath();
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(null);
        assertCopyFailsAndNewVolumeIsRemoved();
    }

    @Test
    public void copyMapFailureOfTheNewVolumeUnmapsOnlyTheSnapshot() {
        stubCopyPath();
        when(client.mapVolumeToSdc(NEW_VOLUME_PF_ID, SDC_ID)).thenReturn(false);

        assertCopyFailsAndNewVolumeIsRemoved();
        verify(endPoint, never()).sendMessage(any());
        verify(driver).revokeVolumeAccess(SNAPSHOT_PATH, host, dataStore);
        verify(driver, never()).revokeVolumeAccess(eq(NEW_VOLUME_PATH), any(), any());
    }

    @Test
    public void copyNoSdcOnTheOnlyHostRemovesNewVolume() {
        stubCopyPath();
        doReturn(null).when(driver).prepareSdcOnHost(host, dataStore);

        assertCopyFailsAndNewVolumeIsRemoved();
        verify(driver).releaseSnapshotCopyLock(lock);
        verify(driver).unprepareSdcAfterFailedPrepare(host, dataStore);
        verify(client, never()).mapVolumeToSdc(anyString(), anyString());
        verify(driver, never()).revokeVolumeAccess(anyString(), any(), any());
    }

    @Test
    public void copyMovesToTheNextHostWhenAHostHasNoSdc() {
        stubCopyPath();
        HostVO noSdcHost = mock(HostVO.class);
        when(noSdcHost.getId()).thenReturn(HOST_ID + 1);
        doReturn(List.of(noSdcHost, host)).when(driver).findHostsForCopy(newVolumeInfo, storagePool, dataStore);
        GlobalLock otherLock = mock(GlobalLock.class);
        doReturn(otherLock).when(driver).acquireSnapshotCopyLock(eq(SNAPSHOT_VOLUME_ID), eq(HOST_ID + 1), anyInt());
        doNothing().when(driver).releaseSnapshotCopyLock(otherLock);
        doThrow(new RuntimeException("connection limit reached")).when(driver).prepareSdcOnHost(noSdcHost, dataStore);
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, true, null));

        copy();

        verify(driver).releaseSnapshotCopyLock(otherLock);
        verify(client, never()).deleteVolume(anyString());
        verify(driver).revokeVolumeAccess(NEW_VOLUME_PATH, host, dataStore);
        // a short wait on a host when another host can be tried, the full copy wait on the last one
        verify(driver).acquireSnapshotCopyLock(SNAPSHOT_VOLUME_ID, HOST_ID + 1, 60);
        verify(driver).acquireSnapshotCopyLock(SNAPSHOT_VOLUME_ID, HOST_ID, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));
    }

    // ---- SDC release

    @Test
    public void unprepareSdcIfUnusedReleasesAnIdleSdc() {
        when(client.listVolumesMappedToSdc(SDC_ID)).thenReturn(List.of());
        try (MockedStatic<ComponentContext> context = mockStatic(ComponentContext.class)) {
            context.when(() -> ComponentContext.inject(any(ScaleIOSDCManager.class))).thenReturn(sdcManager);
            driver.unprepareSdcIfUnused(host, dataStore, SDC_ID, client);
        }
        verify(sdcManager).unprepareSDC(host, dataStore);
    }

    @Test
    public void unprepareSdcIfUnusedKeepsAnSdcInUse() {
        when(client.listVolumesMappedToSdc(SDC_ID)).thenReturn(List.of(mock(Volume.class)));
        driver.unprepareSdcIfUnused(host, dataStore, SDC_ID, client);
        verify(sdcManager, never()).unprepareSDC(any(), any());
    }

    @Test
    public void copyMovesToTheNextHostWhenAMappingFailsBeforeTheCopy() {
        stubCopyPath();
        HostVO firstHost = mock(HostVO.class);
        when(firstHost.getId()).thenReturn(HOST_ID + 1);
        doReturn(List.of(firstHost, host)).when(driver).findHostsForCopy(newVolumeInfo, storagePool, dataStore);
        GlobalLock firstLock = mock(GlobalLock.class);
        doReturn(firstLock).when(driver).acquireSnapshotCopyLock(eq(SNAPSHOT_VOLUME_ID), eq(HOST_ID + 1), anyInt());
        doNothing().when(driver).releaseSnapshotCopyLock(firstLock);
        doReturn("sdc-of-first-host").when(driver).prepareSdcOnHost(firstHost, dataStore);
        when(client.mapVolumeToSdc(anyString(), eq("sdc-of-first-host"))).thenReturn(false);
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, true, null));

        copy();

        verify(endPoint).sendMessage(any(CopyCommand.class));
        verify(client, never()).deleteVolume(anyString());
        verify(driver).releaseSnapshotCopyLock(firstLock);
    }

    @Test
    public void copyFailingAfterTheCopyStartedIsNotRetriedOnAnotherHost() {
        stubCopyPath();
        HostVO secondHost = mock(HostVO.class);
        doReturn(List.of(host, secondHost)).when(driver).findHostsForCopy(newVolumeInfo, storagePool, dataStore);
        when(endPoint.sendMessage(any(CopyCommand.class))).thenReturn(new Answer(null, false, "qemu-img convert failed"));

        assertCopyFailsAndNewVolumeIsRemoved();
        verify(driver, never()).prepareSdcOnHost(secondHost, dataStore);
    }

    @Test
    public void copyNoHostRemovesNewVolume() {
        stubCopyPath();
        doReturn(List.of()).when(driver).findHostsForCopy(newVolumeInfo, storagePool, dataStore);

        assertCopyFailsAndNewVolumeIsRemoved();
        verify(client, never()).mapVolumeToSdc(anyString(), anyString());
    }

    @Test
    public void copyLockTimeoutRemovesNewVolumeWithoutPreparingTheSdc() {
        stubCopyPath();
        doReturn(null).when(driver).acquireSnapshotCopyLock(eq(SNAPSHOT_VOLUME_ID), eq(HOST_ID), anyInt());

        assertCopyFailsAndNewVolumeIsRemoved();
        verify(driver, never()).prepareSdcOnHost(any(), any());
        verify(client, never()).mapVolumeToSdc(anyString(), anyString());
        verify(driver, never()).releaseSnapshotCopyLock(any());
    }

    @Test
    public void copySnapshotMapFailureReleasesTheIdleSdc() {
        stubCopyPath();
        when(client.mapVolumeToSdc(SNAPSHOT_VOLUME_ID, SDC_ID)).thenReturn(false);

        assertCopyFailsAndNewVolumeIsRemoved();
        verify(driver).unprepareSdcIfUnused(host, dataStore, SDC_ID, client);
        verify(driver, never()).revokeVolumeAccess(anyString(), any(), any());
    }

    // ---- findHostsForCopy

    private HostVO kvmHost(long id, long clusterId) {
        HostVO h = mock(HostVO.class);
        lenient().when(h.getId()).thenReturn(id);
        lenient().when(h.getClusterId()).thenReturn(clusterId);
        lenient().when(h.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        return h;
    }

    private void enabledCluster(long clusterId, boolean enabled) {
        ClusterVO cluster = mock(ClusterVO.class);
        lenient().when(cluster.getAllocationState()).thenReturn(enabled ? Grouping.AllocationState.Enabled : Grouping.AllocationState.Disabled);
        lenient().when(clusterDao.findById(clusterId)).thenReturn(cluster);
    }

    @Test
    public void findHostsForCopyPutsTheAttachedHostFirst() {
        when(selector.select(newVolumeInfo)).thenReturn(endPoint);
        HostVO attached = kvmHost(HOST_ID, 3L);
        when(hostDao.findById(HOST_ID)).thenReturn(attached);
        HostVO other = kvmHost(6L, 3L);
        enabledCluster(3L, true);
        when(storagePool.getScope()).thenReturn(ScopeType.ZONE);
        when(storagePool.getDataCenterId()).thenReturn(1L);
        when(resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, 1L, Hypervisor.HypervisorType.KVM)).thenReturn(List.of(other, attached));

        Assert.assertEquals(List.of(attached, other), driver.findHostsForCopy(newVolumeInfo, storagePool, dataStore));
    }

    @Test
    public void findHostsForCopyWithoutAttachedHostUsesEligibleHostsOfTheZone() {
        when(selector.select(newVolumeInfo)).thenReturn(null);
        when(storagePool.getScope()).thenReturn(ScopeType.ZONE);
        when(storagePool.getDataCenterId()).thenReturn(1L);
        HostVO vmwareHost = kvmHost(8L, 3L);
        when(vmwareHost.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.VMware);
        HostVO kvm = kvmHost(6L, 3L);
        HostVO inDisabledCluster = kvmHost(7L, 4L);
        enabledCluster(3L, true);
        enabledCluster(4L, false);
        when(resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, 1L, Hypervisor.HypervisorType.KVM)).thenReturn(List.of(vmwareHost, kvm, inDisabledCluster));

        Assert.assertEquals(List.of(kvm), driver.findHostsForCopy(newVolumeInfo, storagePool, dataStore));
    }

    @Test
    public void findHostsForCopyClusterPoolKeepsOnlyHostsOfThePoolsCluster() {
        when(selector.select(newVolumeInfo)).thenReturn(null);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePool.getClusterId()).thenReturn(3L);
        when(storagePool.getDataCenterId()).thenReturn(1L);
        HostVO inCluster = kvmHost(6L, 3L);
        HostVO otherCluster = kvmHost(7L, 4L);
        enabledCluster(3L, true);
        enabledCluster(4L, true);
        when(resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, 1L, Hypervisor.HypervisorType.KVM)).thenReturn(List.of(otherCluster, inCluster));

        Assert.assertEquals(List.of(inCluster), driver.findHostsForCopy(newVolumeInfo, storagePool, dataStore));
    }

    @Test
    public void findHostsForCopyTriesAtMostThreeHosts() {
        when(selector.select(newVolumeInfo)).thenReturn(null);
        when(storagePool.getScope()).thenReturn(ScopeType.ZONE);
        when(storagePool.getDataCenterId()).thenReturn(1L);
        enabledCluster(3L, true);
        List<HostVO> four = List.of(kvmHost(11L, 3L), kvmHost(12L, 3L), kvmHost(13L, 3L), kvmHost(14L, 3L));
        when(resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, 1L, Hypervisor.HypervisorType.KVM)).thenReturn(four);

        Assert.assertEquals(3, driver.findHostsForCopy(newVolumeInfo, storagePool, dataStore).size());
    }

    @Test
    public void findHostsForCopyNoHost() {
        when(selector.select(newVolumeInfo)).thenReturn(null);
        when(resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(any(), anyLong(), any())).thenReturn(List.of());
        Assert.assertTrue(driver.findHostsForCopy(newVolumeInfo, storagePool, dataStore).isEmpty());
    }

    // ---- deleteAsync

    @Test
    public void deleteAsyncOfAVolumeWithoutPowerFlexVolumeSucceedsWithoutTouchingThePool() {
        when(dataStore.getId()).thenReturn(POOL_ID);
        when(storagePool.getHostAddress()).thenReturn("192.0.2.10");
        VolumeInfo failed = mock(VolumeInfo.class);
        when(failed.getType()).thenReturn(DataObjectType.VOLUME);
        when(failed.getPath()).thenReturn(null);
        @SuppressWarnings("unchecked")
        AsyncCompletionCallback<CommandResult> callback = mock(AsyncCompletionCallback.class);

        driver.deleteAsync(dataStore, failed, callback);

        ArgumentCaptor<CommandResult> result = ArgumentCaptor.forClass(CommandResult.class);
        verify(callback).complete(result.capture());
        Assert.assertTrue(result.getValue().isSuccess());
        verify(client, never()).deleteVolume(any());
        verify(storagePoolDao, never()).update(anyLong(), any());
    }

    // ---- snapshot copy lock

    @Test
    public void acquireSnapshotCopyLockTimeoutReleasesTheReference() {
        try (MockedStatic<GlobalLock> globalLock = mockStatic(GlobalLock.class)) {
            globalLock.when(() -> GlobalLock.getInternLock("powerflex.snapshot.copy." + SNAPSHOT_VOLUME_ID + "." + HOST_ID)).thenReturn(lock);
            when(lock.lock(30)).thenReturn(false);

            Assert.assertNull(driver.acquireSnapshotCopyLock(SNAPSHOT_VOLUME_ID, HOST_ID, 30));
            verify(lock).releaseRef();
            verify(lock, never()).unlock();
        }
    }

    @Test
    public void acquireAndReleaseSnapshotCopyLock() {
        try (MockedStatic<GlobalLock> globalLock = mockStatic(GlobalLock.class)) {
            globalLock.when(() -> GlobalLock.getInternLock("powerflex.snapshot.copy." + SNAPSHOT_VOLUME_ID + "." + HOST_ID)).thenReturn(lock);
            when(lock.lock(30)).thenReturn(true);

            Assert.assertSame(lock, driver.acquireSnapshotCopyLock(SNAPSHOT_VOLUME_ID, HOST_ID, 30));
            verify(lock, never()).releaseRef();
            driver.releaseSnapshotCopyLock(lock);
            InOrder order = inOrder(lock);
            order.verify(lock).unlock();
            order.verify(lock).releaseRef();
        }
    }

    @Test
    public void copyUnexpectedExceptionBeforeTheCopyRemovesNewVolume() {
        when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenThrow(new RuntimeException("db down"));
        when(client.deleteVolume(NEW_VOLUME_PF_ID)).thenReturn(true);

        assertCopyFailsAndNewVolumeIsRemoved();
    }

    // ---- deleteVolumeAfterFailedCopy

    @Test
    public void deleteVolumeAfterFailedCopyKeepsUsedBytesAndAlertsWhenRemovalFails() {
        when(client.deleteVolume(NEW_VOLUME_PF_ID)).thenReturn(false);
        when(storagePool.getDataCenterId()).thenReturn(1L);
        when(storagePool.getPodId()).thenReturn(2L);
        driver.deleteVolumeAfterFailedCopy(client, newVolume, POOL_ID);
        verify(storagePoolDao, never()).update(anyLong(), any());
        verify(newVolume, never()).setPath(null);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_STORAGE_MISC), eq(1L), eq(2L), anyString(), anyString());
    }

    @Test
    public void deleteVolumeAfterFailedCopyReReadsThePoolAndNeverGoesNegative() {
        when(client.deleteVolume(NEW_VOLUME_PF_ID)).thenReturn(true);
        when(storagePool.getUsedBytes()).thenReturn(1L << 30);
        driver.deleteVolumeAfterFailedCopy(client, newVolume, POOL_ID);
        verify(storagePoolDao).findById(POOL_ID);
        verify(storagePool).setUsedBytes(0L);
        verify(storagePoolDao).update(POOL_ID, storagePool);
        // the failed volume row no longer points at the removed PowerFlex volume
        verify(newVolume).setPath(null);
        verify(newVolume).set_iScsiName(null);
        verify(newVolume).setFolder(null);
        verify(volumeDao).update(NEW_VOLUME_ID, newVolume);
    }

    // ---- getPrimaryDataStoreTOForManagedVolume

    @Test
    public void primaryDataStoreTOForManagedVolumeTargetsTheVolume() {
        when(storagePool.getHostAddress()).thenReturn("192.0.2.10");
        when(storagePool.getPort()).thenReturn(443);
        driver.getPrimaryDataStoreTOForManagedVolume(storagePool, SNAPSHOT_PATH, "snap", 16L << 30);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> details = ArgumentCaptor.forClass(Map.class);
        verify(dataStore).setDetails(details.capture());
        Assert.assertEquals(SNAPSHOT_PATH, details.getValue().get(PrimaryDataStore.MANAGED_STORE_TARGET));
        Assert.assertEquals("true", details.getValue().get(PrimaryDataStore.MANAGED));
        Assert.assertEquals("192.0.2.10", details.getValue().get(PrimaryDataStore.STORAGE_HOST));
        Assert.assertEquals(String.valueOf(16L << 30), details.getValue().get(PrimaryDataStore.VOLUME_SIZE));
    }

    // ---- createVolume

    private VolumeInfo stubCreateVolume(Long cloneOfSnapshot) {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(NEW_VOLUME_ID);
        lenient().when(volumeInfo.getSize()).thenReturn(10L << 30);
        if (cloneOfSnapshot != null) {
            VolumeDetailVO detail = mock(VolumeDetailVO.class);
            when(detail.getValue()).thenReturn(String.valueOf(cloneOfSnapshot));
            when(volumeDetailsDao.findDetail(NEW_VOLUME_ID, ScaleIOPrimaryDataStoreDriver.CLONE_OF_SNAPSHOT)).thenReturn(detail);
        }
        when(storagePool.getHostAddress()).thenReturn("192.0.2.10");
        lenient().when(storagePool.getUuid()).thenReturn("a1b2c3d4-0000-0000-0000-000000000000");
        lenient().when(storagePool.getPath()).thenReturn("pool-id-1");
        lenient().when(storagePool.getCapacityBytes()).thenReturn(1L << 40);
        lenient().doReturn(client).when(driver).getScaleIOClient(storagePool);
        lenient().doReturn(false).when(driver).anyVolumeRequiresEncryption(volumeInfo);
        Volume created = mock(Volume.class);
        lenient().when(created.getId()).thenReturn(NEW_VOLUME_PF_ID);
        lenient().when(created.getSizeInKb()).thenReturn(16L * 1024 * 1024);
        lenient().when(client.createVolume(anyString(), eq("pool-id-1"), eq(10), any())).thenReturn(created);
        lenient().when(volumeDao.findById(NEW_VOLUME_ID)).thenReturn(newVolume);
        return volumeInfo;
    }

    @Test
    public void createVolumeFromSnapshotCopiesTheSnapshotIntoTheNewVolume() {
        VolumeInfo volumeInfo = stubCreateVolume(SNAPSHOT_ID);
        doNothing().when(driver).copySnapshotToNewVolume(anyLong(), any(), any(), any(), any());
        try (MockedStatic<VolumeObject> volumeObjectMock = mockStatic(VolumeObject.class)) {
            VolumeObject createdObject = mock(VolumeObject.class);
            volumeObjectMock.when(() -> VolumeObject.getVolumeObject(any(), any())).thenReturn(createdObject);

            driver.createVolume(volumeInfo, POOL_ID, false, null);

            InOrder order = inOrder(client, createdObject, driver);
            order.verify(driver).validateSnapshotToCreateVolumeFrom(SNAPSHOT_ID, volumeInfo, storagePool);
            order.verify(client).createVolume(anyString(), eq("pool-id-1"), eq(10), any());
            order.verify(createdObject).update();
            order.verify(driver).copySnapshotToNewVolume(SNAPSHOT_ID, snapshotStore, newVolume, storagePool, client);
        }
    }

    @Test
    public void createVolumeFromSnapshotRefusedBeforeAnythingIsCreatedOnPowerFlex() {
        VolumeInfo volumeInfo = stubCreateVolume(SNAPSHOT_ID);
        when(snapshotStore.getDataStoreId()).thenReturn(POOL_ID + 1);

        Assert.assertThrows(CloudRuntimeException.class, () -> driver.createVolume(volumeInfo, POOL_ID, false, null));
        verify(client, never()).createVolume(anyString(), anyString(), anyInt(), any());
        verify(driver, never()).copySnapshotToNewVolume(anyLong(), any(), any(), any(), any());
    }

    @Test
    public void createVolumeWithoutSnapshotDoesNotCopy() {
        VolumeInfo volumeInfo = stubCreateVolume(null);
        try (MockedStatic<VolumeObject> volumeObjectMock = mockStatic(VolumeObject.class)) {
            volumeObjectMock.when(() -> VolumeObject.getVolumeObject(any(), any())).thenReturn(mock(VolumeObject.class));

            driver.createVolume(volumeInfo, POOL_ID, false, null);

            verify(client).createVolume(anyString(), eq("pool-id-1"), eq(10), any());
            verify(driver, never()).validateSnapshotToCreateVolumeFrom(anyLong(), any(), any());
            verify(driver, never()).copySnapshotToNewVolume(anyLong(), any(), any(), any(), any());
        }
    }
}
