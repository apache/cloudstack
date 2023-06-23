//
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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.configuration.Config;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteHostEndPoint.class)
public class ScaleIOPrimaryDataStoreDriverTest {

    @Spy
    @InjectMocks
    ScaleIOPrimaryDataStoreDriver scaleIOPrimaryDataStoreDriver = new ScaleIOPrimaryDataStoreDriver();
    @Mock
    StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock
    PrimaryDataStoreDao storagePoolDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    VolumeDetailsDao volumeDetailsDao;
    @Mock
    VolumeService volumeService;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    HostDao hostDao;
    @Mock
    ConfigurationDao configDao;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void testSameScaleIOStorageInstance() {
        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        when(destStore.getId()).thenReturn(2L);

        StoragePoolDetailVO srcPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String srcPoolSystemId = "610204d03e3ad60f";
        when(srcPoolSystemIdDetail.getValue()).thenReturn(srcPoolSystemId);

        StoragePoolDetailVO destPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String destPoolSystemId = "610204d03e3ad60f";
        when(destPoolSystemIdDetail.getValue()).thenReturn(destPoolSystemId);

        when(storagePoolDetailsDao.findDetail(1L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcPoolSystemIdDetail);
        when(storagePoolDetailsDao.findDetail(2L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(destPoolSystemIdDetail);

        boolean result = scaleIOPrimaryDataStoreDriver.isSameScaleIOStorageInstance(srcStore, destStore);

        Assert.assertTrue(result);
    }

    @Test
    public void testDifferentScaleIOStorageInstance() {
        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        when(destStore.getId()).thenReturn(2L);

        StoragePoolDetailVO srcPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String srcPoolSystemId = "610204d03e3ad60f";
        when(srcPoolSystemIdDetail.getValue()).thenReturn(srcPoolSystemId);

        StoragePoolDetailVO destPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String destPoolSystemId = "7332760565f6340f";
        when(destPoolSystemIdDetail.getValue()).thenReturn(destPoolSystemId);

        when(storagePoolDetailsDao.findDetail(1L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcPoolSystemIdDetail);
        when(storagePoolDetailsDao.findDetail(2L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(destPoolSystemIdDetail);

        boolean result = scaleIOPrimaryDataStoreDriver.isSameScaleIOStorageInstance(srcStore, destStore);

        Assert.assertFalse(result);
    }

    @Test (expected = CloudRuntimeException.class)
    public void testCheckVolumeOnDifferentScaleIOStorageInstanceSystemIdShouldNotBeNull() {
        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        when(destStore.getId()).thenReturn(2L);

        StoragePoolDetailVO srcPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        String srcPoolSystemId = "610204d03e3ad60f";
        when(srcPoolSystemIdDetail.getValue()).thenReturn(srcPoolSystemId);

        StoragePoolDetailVO destPoolSystemIdDetail = Mockito.mock(StoragePoolDetailVO.class);
        when(destPoolSystemIdDetail.getValue()).thenReturn(null);

        when(storagePoolDetailsDao.findDetail(1L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcPoolSystemIdDetail);
        when(storagePoolDetailsDao.findDetail(2L,ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(destPoolSystemIdDetail);

        scaleIOPrimaryDataStoreDriver.isSameScaleIOStorageInstance(srcStore, destStore);
    }

    @Test
    public void testMigrateVolumeWithinSameScaleIOClusterSuccess() throws Exception {
        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        VolumeInfo destData = Mockito.mock(VolumeInfo.class);

        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);

        when(srcData.getDataStore()).thenReturn(srcStore);
        when(destData.getDataStore()).thenReturn(destStore);

        fillSrcVolumeDetails(srcData, srcStore);
        fillDestVolumeDetails(destData, destStore);

        VolumeObjectTO destVolTO = Mockito.mock(VolumeObjectTO.class);
        when(destData.getTO()).thenReturn(destVolTO);
        Host host = prepareEndpointForVolumeOperation(srcData);
        PowerMockito.mockStatic(RemoteHostEndPoint.class);
        RemoteHostEndPoint ep = Mockito.mock(RemoteHostEndPoint.class);
        when(RemoteHostEndPoint.getHypervisorHostEndPoint(host)).thenReturn(ep);

        DataTO dataTO = Mockito.mock(DataTO.class);
        CreateObjectAnswer createAnswer = new CreateObjectAnswer(dataTO);
        doReturn(createAnswer).when(scaleIOPrimaryDataStoreDriver).createVolume(destData, 2L, true);
        when(dataTO.getPath()).thenReturn("bec0ba7700000007:vol-11-6aef-10ee");
        doReturn(true).when(scaleIOPrimaryDataStoreDriver)
                .grantAccess(any(), any(), any());

        when(configDao.getValue(Config.MigrateWait.key())).thenReturn("3600");
        MigrateVolumeAnswer migrateVolumeAnswer = Mockito.mock(MigrateVolumeAnswer.class);
        when(ep.sendMessage(any())).thenReturn(migrateVolumeAnswer);
        when(migrateVolumeAnswer.getResult()).thenReturn(true);

        Mockito.doNothing().when(scaleIOPrimaryDataStoreDriver)
                .updateVolumeAfterCopyVolume(any(), any());
        Mockito.doNothing().when(scaleIOPrimaryDataStoreDriver)
                .updateSnapshotsAfterCopyVolume(any(), any());
        Mockito.doNothing().when(scaleIOPrimaryDataStoreDriver)
                .deleteSourceVolumeAfterSuccessfulBlockCopy(any(), any());

        Answer answer = scaleIOPrimaryDataStoreDriver.liveMigrateVolume(srcData, destData);

        Assert.assertTrue(answer.getResult());
    }

    @Test
    public void testMigrateVolumeWithinSameScaleIOClusterFailure() throws Exception {
        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        VolumeInfo destData = Mockito.mock(VolumeInfo.class);

        DataStore srcStore = Mockito.mock(DataStore.class);
        DataStore destStore = Mockito.mock(DataStore.class);

        when(srcData.getDataStore()).thenReturn(srcStore);
        when(destData.getDataStore()).thenReturn(destStore);

        fillSrcVolumeDetails(srcData, srcStore);
        fillDestVolumeDetails(destData, destStore);

        VolumeObjectTO destVolTO = Mockito.mock(VolumeObjectTO.class);
        when(destData.getTO()).thenReturn(destVolTO);
        Host host = prepareEndpointForVolumeOperation(srcData);
        PowerMockito.mockStatic(RemoteHostEndPoint.class);
        RemoteHostEndPoint ep = Mockito.mock(RemoteHostEndPoint.class);
        when(RemoteHostEndPoint.getHypervisorHostEndPoint(host)).thenReturn(ep);

        DataTO dataTO = Mockito.mock(DataTO.class);
        CreateObjectAnswer createAnswer = new CreateObjectAnswer(dataTO);
        doReturn(createAnswer).when(scaleIOPrimaryDataStoreDriver).createVolume(destData, 2L, true);
        when(dataTO.getPath()).thenReturn("bec0ba7700000007:vol-11-6aef-10ee");
        doReturn(true).when(scaleIOPrimaryDataStoreDriver)
                .grantAccess(any(), any(), any());

        when(configDao.getValue(Config.MigrateWait.key())).thenReturn("3600");
        MigrateVolumeAnswer migrateVolumeAnswer = Mockito.mock(MigrateVolumeAnswer.class);
        when(ep.sendMessage(any())).thenReturn(migrateVolumeAnswer);
        when(migrateVolumeAnswer.getResult()).thenReturn(false);
        Mockito.doNothing().when(scaleIOPrimaryDataStoreDriver)
                .revertBlockCopyVolumeOperations(any(), any(), any(), any());

        Answer answer = scaleIOPrimaryDataStoreDriver.liveMigrateVolume(srcData, destData);

        Assert.assertFalse(answer.getResult());
    }

    private void fillSrcVolumeDetails(VolumeInfo srcData, DataStore srcStore) {
        when(srcStore.getId()).thenReturn(1L);
        when(srcData.getId()).thenReturn(1L);

        StoragePoolVO storagePoolVO = Mockito.mock(StoragePoolVO.class);
        when(storagePoolDao.findById(1L)).thenReturn(storagePoolVO);
        when(storagePoolVO.isManaged()).thenReturn(true);

        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        when(volumeDao.findById(1L)).thenReturn(volumeVO);

        when(volumeDetailsDao.findDetail(1L,  DiskTO.SCSI_NAA_DEVICE_ID)).thenReturn(null);
        when(volumeService.getChapInfo(srcData, srcStore)).thenReturn(null);

        StoragePoolDetailVO srcStoragePoolDetail = Mockito.mock(StoragePoolDetailVO.class);
        when(srcStoragePoolDetail.getValue()).thenReturn("610204d03e3ad60f");
        when(storagePoolDetailsDao.findDetail(1L, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcStoragePoolDetail);
    }

    private void fillDestVolumeDetails(VolumeInfo srcData, DataStore srcStore) {
        when(srcStore.getId()).thenReturn(2L);
        when(srcData.getId()).thenReturn(2L);

        StoragePoolVO storagePoolVO = Mockito.mock(StoragePoolVO.class);
        when(storagePoolDao.findById(2L)).thenReturn(storagePoolVO);
        when(storagePoolVO.isManaged()).thenReturn(true);

        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        when(volumeDao.findById(2L)).thenReturn(volumeVO);

        when(volumeDetailsDao.findDetail(2L,  DiskTO.SCSI_NAA_DEVICE_ID)).thenReturn(null);
        when(volumeService.getChapInfo(srcData, srcStore)).thenReturn(null);

        StoragePoolDetailVO srcStoragePoolDetail = Mockito.mock(StoragePoolDetailVO.class);
        when(srcStoragePoolDetail.getValue()).thenReturn("7332760565f6340f");
        when(storagePoolDetailsDao.findDetail(2L, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(srcStoragePoolDetail);
    }

    private Host prepareEndpointForVolumeOperation(VolumeInfo srcData) {
        VMInstanceVO instance = Mockito.mock(VMInstanceVO.class);
        when(srcData.getAttachedVmName()).thenReturn("i-2-VM");
        when(vmInstanceDao.findVMByInstanceName("i-2-VM")).thenReturn(instance);
        when(instance.getHostId()).thenReturn(4L);
        when(instance.getState()).thenReturn(VirtualMachine.State.Running);
        HostVO host = Mockito.mock(HostVO.class);
        when(hostDao.findById(4L)).thenReturn(host);

        return host;
    }

    @Test
    public void updateVolumeAfterCopyVolumeLiveMigrate() {
        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        VolumeInfo destData = Mockito.mock(VolumeInfo.class);

        when(srcData.getId()).thenReturn(1L);
        when(destData.getId()).thenReturn(1L);

        VolumeVO volume = new VolumeVO("root", 1L, 1L, 1L, 1L, 1L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
        volume.setPoolId(2L);
        when(volumeDao.findById(1L)).thenReturn(volume);
        when(volumeDao.update(1L, volume)).thenReturn(true);

        scaleIOPrimaryDataStoreDriver.updateVolumeAfterCopyVolume(srcData, destData);

        Assert.assertEquals(Optional.of(2L), Optional.of(volume.getLastPoolId()));
    }

    @Test
    public void updateVolumeAfterCopyVolumeOffline() {
        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        VolumeInfo destData = Mockito.mock(VolumeInfo.class);

        when(srcData.getId()).thenReturn(1L);
        when(destData.getId()).thenReturn(2L);

        VolumeVO volume = new VolumeVO("root", 1L, 1L, 1L, 1L, 1L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
        when(volumeDao.findById(1L)).thenReturn(volume);
        when(volumeDao.update(1L, volume)).thenReturn(true);

        scaleIOPrimaryDataStoreDriver.updateVolumeAfterCopyVolume(srcData, destData);

        Assert.assertNull(volume.get_iScsiName());
        Assert.assertNull(volume.getPath());
        Assert.assertNull(volume.getFolder());
    }

    @Test
    public void revertBlockCopyVolumeOperationsOnDeleteSuccess() throws Exception{
        //Either destination volume delete success or failure, DB operations should get revert

        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        VolumeInfo destData = Mockito.mock(VolumeInfo.class);
        Host host = Mockito.mock(Host.class);
        String destVolumePath = "01b332b300000007:vol-11-b9e2-10ee";

        when(srcData.getId()).thenReturn(1L);
        when(srcData.getPoolId()).thenReturn(1L);
        when(destData.getId()).thenReturn(1L);

        when(srcData.getPath()).thenReturn("bec0ba7700000007:vol-11-6aef-10ee");
        when(srcData.getFolder()).thenReturn("921c364500000007");
        DataStore destStore = Mockito.mock(DataStore.class);
        when(destStore.getId()).thenReturn(2L);
        when(destData.getDataStore()).thenReturn(destStore);
        doNothing().when(scaleIOPrimaryDataStoreDriver)
                .revokeAccess(any(), any(), any());

        ScaleIOGatewayClient client = Mockito.mock(ScaleIOGatewayClient.class);
        doReturn(client).when(scaleIOPrimaryDataStoreDriver)
                .getScaleIOClient(any());
        when(client.deleteVolume(any())).thenReturn(true);

        VolumeVO volume = new VolumeVO("root", 1L, 1L, 1L, 1L, 1L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
        when(volumeDao.findById(1L)).thenReturn(volume);
        when(volumeDao.update(1L, volume)).thenReturn(true);

        scaleIOPrimaryDataStoreDriver.revertBlockCopyVolumeOperations(srcData, destData, host, destVolumePath);

        Assert.assertEquals("bec0ba7700000007:vol-11-6aef-10ee", volume.get_iScsiName());
        Assert.assertEquals("bec0ba7700000007:vol-11-6aef-10ee", volume.getPath());
        Assert.assertEquals("921c364500000007", volume.getFolder());
    }

    @Test
    public void revertBlockCopyVolumeOperationsOnDeleteFailure() throws Exception{
        //Either destination volume delete success or failure, DB operations should get revert

        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        VolumeInfo destData = Mockito.mock(VolumeInfo.class);
        Host host = Mockito.mock(Host.class);
        String srcVolumePath = "bec0ba7700000007:vol-11-6aef-10ee";
        String destVolumePath = "01b332b300000007:vol-11-b9e2-10ee";

        when(srcData.getId()).thenReturn(1L);
        when(srcData.getPoolId()).thenReturn(1L);
        when(destData.getId()).thenReturn(1L);

        when(srcData.getPath()).thenReturn(srcVolumePath);
        when(srcData.getFolder()).thenReturn("921c364500000007");
        DataStore destStore = Mockito.mock(DataStore.class);
        when(destStore.getId()).thenReturn(2L);
        when(destData.getDataStore()).thenReturn(destStore);
        doNothing().when(scaleIOPrimaryDataStoreDriver).revokeAccess(any(), any(), any());

        ScaleIOGatewayClient client = Mockito.mock(ScaleIOGatewayClient.class);
        doReturn(client).when(scaleIOPrimaryDataStoreDriver)
                .getScaleIOClient(any());
        when(client.deleteVolume(any())).thenReturn(false);

        VolumeVO volume = new VolumeVO("root", 1L, 1L, 1L, 1L, 1L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
        when(volumeDao.findById(1L)).thenReturn(volume);
        when(volumeDao.update(1L, volume)).thenReturn(true);

        scaleIOPrimaryDataStoreDriver.revertBlockCopyVolumeOperations(srcData, destData, host, destVolumePath);

        Assert.assertEquals(srcVolumePath, volume.get_iScsiName());
        Assert.assertEquals(srcVolumePath, volume.getPath());
        Assert.assertEquals("921c364500000007", volume.getFolder());
    }

    @Test
    public void deleteSourceVolumeSuccessScenarioAfterSuccessfulBlockCopy() throws Exception {
        // Either Volume deletion success or failure method should complete

        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        Host host = Mockito.mock(Host.class);
        String srcVolumePath = "bec0ba7700000007:vol-11-6aef-10ee";

        DataStore srcStore = Mockito.mock(DataStore.class);
        DataTO volumeTO = Mockito.mock(DataTO.class);
        when(srcData.getDataStore()).thenReturn(srcStore);
        when(srcData.getTO()).thenReturn(volumeTO);
        when(volumeTO.getPath()).thenReturn(srcVolumePath);
        doNothing().when(scaleIOPrimaryDataStoreDriver).revokeVolumeAccess(any(), any(), any());

        ScaleIOGatewayClient client = Mockito.mock(ScaleIOGatewayClient.class);
        doReturn(client).when(scaleIOPrimaryDataStoreDriver)
                .getScaleIOClient(any());
        when(client.deleteVolume(any())).thenReturn(true);

        scaleIOPrimaryDataStoreDriver.deleteSourceVolumeAfterSuccessfulBlockCopy(srcData, host);
    }

    @Test
    public void deleteSourceVolumeFailureScenarioAfterSuccessfulBlockCopy() throws Exception {
        // Either Volume deletion success or failure method should complete

        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        Host host = Mockito.mock(Host.class);
        when(host.getId()).thenReturn(1L);
        String srcVolumePath = "bec0ba7700000007:vol-11-6aef-10ee";

        DataStore srcStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        DataTO volumeTO = Mockito.mock(DataTO.class);
        when(srcData.getDataStore()).thenReturn(srcStore);
        when(srcData.getTO()).thenReturn(volumeTO);
        when(volumeTO.getPath()).thenReturn(srcVolumePath);
        String sdcId = "7332760565f6340f";
        doReturn(sdcId).when(scaleIOPrimaryDataStoreDriver).getConnectedSdc(1L, 1L);

        ScaleIOGatewayClient client = Mockito.mock(ScaleIOGatewayClient.class);
        doReturn(client).when(scaleIOPrimaryDataStoreDriver)
                .getScaleIOClient(any());
        doReturn(true).when(client).unmapVolumeFromSdc(any(), any());
        when(client.deleteVolume(any())).thenReturn(false);

        scaleIOPrimaryDataStoreDriver.deleteSourceVolumeAfterSuccessfulBlockCopy(srcData, host);
    }

    @Test
    public void deleteSourceVolumeFailureScenarioWhenNoSDCisFound() {
        // Either Volume deletion success or failure method should complete

        VolumeInfo srcData = Mockito.mock(VolumeInfo.class);
        Host host = Mockito.mock(Host.class);
        when(host.getId()).thenReturn(1L);
        String srcVolumePath = "bec0ba7700000007:vol-11-6aef-10ee";

        DataStore srcStore = Mockito.mock(DataStore.class);
        when(srcStore.getId()).thenReturn(1L);
        DataTO volumeTO = Mockito.mock(DataTO.class);
        when(srcData.getDataStore()).thenReturn(srcStore);
        when(srcData.getTO()).thenReturn(volumeTO);
        when(volumeTO.getPath()).thenReturn(srcVolumePath);
        String sdcId = "7332760565f6340f";
        doReturn(null).when(scaleIOPrimaryDataStoreDriver).getConnectedSdc(1L, 1L);

        scaleIOPrimaryDataStoreDriver.deleteSourceVolumeAfterSuccessfulBlockCopy(srcData, host);
    }

    @Test
    public void testCopyOfflineVolume() {
        when(configDao.getValue(Config.CopyVolumeWait.key())).thenReturn("3600");

        DataObject srcData = Mockito.mock(DataObject.class);
        DataTO srcDataTO = Mockito.mock(DataTO.class);
        when(srcData.getTO()).thenReturn(srcDataTO);
        DataObject destData = Mockito.mock(DataObject.class);
        DataTO destDataTO = Mockito.mock(DataTO.class);
        when(destData.getTO()).thenReturn(destDataTO);
        Host destHost = Mockito.mock(Host.class);

        doReturn(false).when(scaleIOPrimaryDataStoreDriver).anyVolumeRequiresEncryption(srcData, destData);
        PowerMockito.mockStatic(RemoteHostEndPoint.class);
        RemoteHostEndPoint ep = Mockito.mock(RemoteHostEndPoint.class);
        when(RemoteHostEndPoint.getHypervisorHostEndPoint(destHost)).thenReturn(ep);
        Answer answer = Mockito.mock(Answer.class);
        when(ep.sendMessage(any())).thenReturn(answer);

        Answer expectedAnswer = scaleIOPrimaryDataStoreDriver.copyOfflineVolume(srcData, destData, destHost);

        Assert.assertEquals(expectedAnswer, answer);
    }

    @Test
    public void testCopyOfflineVolumeFailureWhenNoEndpointFound() {
        when(configDao.getValue(Config.CopyVolumeWait.key())).thenReturn("3600");

        DataObject srcData = Mockito.mock(DataObject.class);
        DataTO srcDataTO = Mockito.mock(DataTO.class);
        when(srcData.getTO()).thenReturn(srcDataTO);
        DataObject destData = Mockito.mock(DataObject.class);
        DataTO destDataTO = Mockito.mock(DataTO.class);
        when(destData.getTO()).thenReturn(destDataTO);
        Host destHost = Mockito.mock(Host.class);

        doReturn(false).when(scaleIOPrimaryDataStoreDriver).anyVolumeRequiresEncryption(srcData, destData);
        PowerMockito.mockStatic(RemoteHostEndPoint.class);
        when(RemoteHostEndPoint.getHypervisorHostEndPoint(destHost)).thenReturn(null);

        Answer answer = scaleIOPrimaryDataStoreDriver.copyOfflineVolume(srcData, destData, destHost);

        Assert.assertEquals(false, answer.getResult());
    }
}