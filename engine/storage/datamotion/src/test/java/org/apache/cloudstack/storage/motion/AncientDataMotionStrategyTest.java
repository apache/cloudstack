/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.motion;

import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

import com.cloud.storage.clvm.ClvmPoolManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;


@RunWith(MockitoJUnitRunner.class)
public class AncientDataMotionStrategyTest {

    @Spy
    @InjectMocks
    private AncientDataMotionStrategy strategy = new AncientDataMotionStrategy();

    @Mock
    DataTO dataTO;
    @Mock
    PrimaryDataStoreTO dataStoreTO;
    @Mock
    StorageManager storageManager;
    @Mock
    StoragePool storagePool;
    @Mock
    StorageCacheManager cacheMgr;
    @Mock
    ConfigurationDao configDao;

    private static final long POOL_ID = 1l;
    private static final Boolean FULL_CLONE_FLAG = true;

    @Before
    public void setup() throws Exception {
        overrideDefaultConfigValue(StorageManager.VmwareCreateCloneFull, String.valueOf(FULL_CLONE_FLAG));

        when(dataTO.getHypervisorType()).thenReturn(HypervisorType.VMware);
        when(dataTO.getDataStore()).thenReturn(dataStoreTO);
        when(dataStoreTO.getId()).thenReturn(POOL_ID);
        when(storageManager.getStoragePool(POOL_ID)).thenReturn(storagePool);
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String value) throws IllegalAccessException, NoSuchFieldException {
        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(configKey, value);
    }

    private ClvmPoolManager injectMockedClvmPoolManager() throws Exception {
        ClvmPoolManager clvmPoolManager = Mockito.mock(ClvmPoolManager.class);
        Field clvmPoolManagerField = AncientDataMotionStrategy.class.getDeclaredField("clvmPoolManager");
        clvmPoolManagerField.setAccessible(true);
        clvmPoolManagerField.set(strategy, clvmPoolManager);
        return clvmPoolManager;
    }

    @Test
    public void testAddFullCloneFlagOnVMwareDest(){
        strategy.addFullCloneAndDiskprovisiongStrictnessFlagOnVMwareDest(dataTO);
        verify(dataStoreTO).setFullCloneFlag(FULL_CLONE_FLAG);
    }

    @Test
    public void testAddFullCloneFlagOnNotVmwareDest(){
        verify(dataStoreTO, never()).setFullCloneFlag(any(Boolean.class));
    }

    @Test
    public void testCanBypassSecondaryStorageForDirectDownload() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(true).when(srcVolumeInfo).isDirectDownload();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageForUnsupportedDataObject() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());

        TemplateObject destTemplateInfo = Mockito.spy(new TemplateObject());

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destTemplateInfo);
        Assert.assertFalse(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageForUnsupportedSrcPoolType() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(srcDataStore).getScope();
        Mockito.doReturn(srcDataStore).when(srcVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.PowerFlex).when(srcVolumeInfo).getStoragePoolType();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(destDataStore).getScope();
        Mockito.doReturn(destDataStore).when(destVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(destVolumeInfo).getStoragePoolType();

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertFalse(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageForUnsupportedDestPoolType() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(srcDataStore).getScope();
        Mockito.doReturn(srcDataStore).when(srcVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(srcVolumeInfo).getStoragePoolType();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(destDataStore).getScope();
        Mockito.doReturn(destDataStore).when(destVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.Iscsi).when(destVolumeInfo).getStoragePoolType();

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertFalse(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageWithZoneWideNFSPoolsInSameZone() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(srcDataStore).getScope();
        Mockito.doReturn(srcDataStore).when(srcVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(srcVolumeInfo).getStoragePoolType();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(destDataStore).getScope();
        Mockito.doReturn(destDataStore).when(destVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(destVolumeInfo).getStoragePoolType();

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageWithClusterWideNFSPoolsInSameCluster() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ClusterScope(5L, 2L, 1L)).when(srcDataStore).getScope();
        Mockito.doReturn(srcDataStore).when(srcVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(srcVolumeInfo).getStoragePoolType();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ClusterScope(5L, 2L, 1L)).when(destDataStore).getScope();
        Mockito.doReturn(destDataStore).when(destVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(destVolumeInfo).getStoragePoolType();

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageWithLocalAndClusterWideNFSPoolsInSameCluster() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new HostScope(1L, 1L, 1L)).when(srcDataStore).getScope();
        Mockito.doReturn(srcDataStore).when(srcVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.Filesystem).when(srcVolumeInfo).getStoragePoolType();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ClusterScope(1L, 1L, 1L)).when(destDataStore).getScope();
        Mockito.doReturn(destDataStore).when(destVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(destVolumeInfo).getStoragePoolType();

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);

        canBypassSecondaryStorage = (boolean) method.invoke(strategy, destVolumeInfo, srcVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageWithLocalAndZoneWideNFSPoolsInSameZone() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new HostScope(1L, 1L, 1L)).when(srcDataStore).getScope();
        Mockito.doReturn(srcDataStore).when(srcVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.Filesystem).when(srcVolumeInfo).getStoragePoolType();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(destDataStore).getScope();
        Mockito.doReturn(destDataStore).when(destVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(destVolumeInfo).getStoragePoolType();

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);

        canBypassSecondaryStorage = (boolean) method.invoke(strategy, destVolumeInfo, srcVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);
    }

    @Test
    public void testCanBypassSecondaryStorageWithClusterWideNFSAndZoneWideNFSPoolsInSameZone() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore srcDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ClusterScope(5L, 2L, 1L)).when(srcDataStore).getScope();
        Mockito.doReturn(srcDataStore).when(srcVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(srcVolumeInfo).getStoragePoolType();

        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(new ZoneScope(1L)).when(destDataStore).getScope();
        Mockito.doReturn(destDataStore).when(destVolumeInfo).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(destVolumeInfo).getStoragePoolType();

        Method method;
        method = AncientDataMotionStrategy.class.getDeclaredMethod("canBypassSecondaryStorage", DataObject.class, DataObject.class);
        method.setAccessible(true);
        boolean canBypassSecondaryStorage = (boolean) method.invoke(strategy, srcVolumeInfo, destVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);

        canBypassSecondaryStorage = (boolean) method.invoke(strategy, destVolumeInfo, srcVolumeInfo);
        Assert.assertTrue(canBypassSecondaryStorage);
    }

    @Test
    public void testUpdateLockHostForVolume_CLVMPool_SetsLockHost() throws Exception {
        Method method = AncientDataMotionStrategy.class.getDeclaredMethod(
                "updateLockHostForVolume",
                EndPoint.class,
                DataObject.class);
        method.setAccessible(true);

        EndPoint endPoint = Mockito.mock(EndPoint.class);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        DataStore dataStore = Mockito.mock(DataStore.class, Mockito.withSettings().extraInterfaces(StoragePool.class));
        ClvmPoolManager clvmPoolManager = injectMockedClvmPoolManager();

        Long hostId = 123L;
        Long volumeId = 456L;
        String volumeUuid = "test-volume-uuid";

        Mockito.when(endPoint.getId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(dataStore);
        Mockito.when(volumeInfo.getId()).thenReturn(volumeId);
        Mockito.when(volumeInfo.getUuid()).thenReturn(volumeUuid);
        Mockito.when(volumeInfo.getPath()).thenReturn("test-volume-path");
        Mockito.when(((StoragePool) dataStore).getPoolType()).thenReturn(Storage.StoragePoolType.CLVM);
        Mockito.when(clvmPoolManager.getClvmLockHostId(Mockito.eq(volumeId), Mockito.eq(volumeUuid),
                Mockito.anyString(), Mockito.any(StoragePool.class), Mockito.eq(true))).thenReturn(null);

        method.invoke(strategy, endPoint, volumeInfo);

        Mockito.verify(clvmPoolManager).setClvmLockHostId(volumeId, hostId);
    }

    @Test
    public void testUpdateLockHostForVolume_CLVM_NG_Pool_SetsLockHost() throws Exception {
        Method method = AncientDataMotionStrategy.class.getDeclaredMethod(
                "updateLockHostForVolume",
                EndPoint.class,
                DataObject.class);
        method.setAccessible(true);

        EndPoint endPoint = Mockito.mock(EndPoint.class);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        DataStore dataStore = Mockito.mock(DataStore.class, Mockito.withSettings().extraInterfaces(StoragePool.class));
        ClvmPoolManager clvmPoolManager = injectMockedClvmPoolManager();

        Long hostId = 789L;
        Long volumeId = 101L;
        String volumeUuid = "test-clvm-ng-volume-uuid";

        Mockito.when(endPoint.getId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(dataStore);
        Mockito.when(volumeInfo.getId()).thenReturn(volumeId);
        Mockito.when(volumeInfo.getUuid()).thenReturn(volumeUuid);
        Mockito.when(volumeInfo.getPath()).thenReturn("test-clvm-ng-volume-path");
        Mockito.when(((StoragePool) dataStore).getPoolType()).thenReturn(Storage.StoragePoolType.CLVM_NG);
        Mockito.when(clvmPoolManager.getClvmLockHostId(Mockito.eq(volumeId), Mockito.eq(volumeUuid),
                Mockito.anyString(), Mockito.any(StoragePool.class), Mockito.eq(true))).thenReturn(null);

        try {
            method.invoke(strategy, endPoint, volumeInfo);
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
            throw e;
        }

        Mockito.verify(clvmPoolManager).setClvmLockHostId(volumeId, hostId);
    }

    @Test
    public void testUpdateLockHostForVolume_NonCLVMPool_DoesNotSetLockHost() throws Exception {
        Method method = AncientDataMotionStrategy.class.getDeclaredMethod(
                "updateLockHostForVolume",
                EndPoint.class,
                DataObject.class);
        method.setAccessible(true);

        EndPoint endPoint = Mockito.mock(EndPoint.class);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        // Create mock that implements both DataStore and StoragePool interfaces
        DataStore dataStore = Mockito.mock(DataStore.class, Mockito.withSettings().extraInterfaces(StoragePool.class));
        ClvmPoolManager clvmPoolManager = injectMockedClvmPoolManager();

        Mockito.when(volumeInfo.getDataStore()).thenReturn(dataStore);
        Mockito.when(((StoragePool) dataStore).getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);

        method.invoke(strategy, endPoint, volumeInfo);

        Mockito.verify(clvmPoolManager, never()).setClvmLockHostId(any(Long.class), any(Long.class));
        Mockito.verify(clvmPoolManager, never()).getClvmLockHostId(any(Long.class), any(String.class),
                any(String.class), any(StoragePool.class), Mockito.anyBoolean());
    }

    @Test
    public void testUpdateLockHostForVolume_ExistingLockHost_DoesNotOverwrite() throws Exception {
        Method method = AncientDataMotionStrategy.class.getDeclaredMethod(
                "updateLockHostForVolume",
                EndPoint.class,
                DataObject.class);
        method.setAccessible(true);

        EndPoint endPoint = Mockito.mock(EndPoint.class);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        DataStore dataStore = Mockito.mock(DataStore.class, Mockito.withSettings().extraInterfaces(StoragePool.class));
        ClvmPoolManager clvmPoolManager = injectMockedClvmPoolManager();

        Long hostId = 555L;
        Long existingHostId = 666L;
        Long volumeId = 777L;
        String volumeUuid = "existing-lock-volume-uuid";

        Mockito.when(endPoint.getId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(dataStore);
        Mockito.when(volumeInfo.getId()).thenReturn(volumeId);
        Mockito.when(volumeInfo.getUuid()).thenReturn(volumeUuid);
        Mockito.when(volumeInfo.getPath()).thenReturn("existing-lock-volume-path");
        Mockito.when(((StoragePool) dataStore).getPoolType()).thenReturn(Storage.StoragePoolType.CLVM);
        Mockito.when(clvmPoolManager.getClvmLockHostId(Mockito.eq(volumeId), Mockito.eq(volumeUuid),
                Mockito.anyString(), Mockito.any(StoragePool.class), Mockito.eq(true))).thenReturn(existingHostId);

        method.invoke(strategy, endPoint, volumeInfo);

        Mockito.verify(clvmPoolManager, never()).setClvmLockHostId(any(Long.class), any(Long.class));
        Mockito.verify(clvmPoolManager).getClvmLockHostId(Mockito.eq(volumeId), Mockito.eq(volumeUuid),
                Mockito.anyString(), Mockito.any(StoragePool.class), Mockito.eq(true));
    }

    @Test
    public void testUpdateLockHostForVolume_NullEndPoint_DoesNotSetLockHost() throws Exception {
        Method method = AncientDataMotionStrategy.class.getDeclaredMethod(
                "updateLockHostForVolume",
                EndPoint.class,
                DataObject.class);
        method.setAccessible(true);

        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        ClvmPoolManager clvmPoolManager = injectMockedClvmPoolManager();

        method.invoke(strategy, null, volumeInfo);

        Mockito.verify(clvmPoolManager, never()).setClvmLockHostId(any(Long.class), any(Long.class));
        Mockito.verify(clvmPoolManager, never()).getClvmLockHostId(any(Long.class), any(String.class),
                any(String.class), any(StoragePool.class), Mockito.anyBoolean());
    }

    @Test
    public void testUpdateLockHostForVolume_NonVolumeDataObject_DoesNotSetLockHost() throws Exception {
        Method method = AncientDataMotionStrategy.class.getDeclaredMethod(
                "updateLockHostForVolume",
                EndPoint.class,
                DataObject.class);
        method.setAccessible(true);

        EndPoint endPoint = Mockito.mock(EndPoint.class);
        SnapshotInfo snapshotInfo = Mockito.mock(SnapshotInfo.class);
        ClvmPoolManager clvmPoolManager = injectMockedClvmPoolManager();

        method.invoke(strategy, endPoint, snapshotInfo);

        Mockito.verify(clvmPoolManager, never()).setClvmLockHostId(any(Long.class), any(Long.class));
        Mockito.verify(clvmPoolManager, never()).getClvmLockHostId(any(Long.class), any(String.class),
                any(String.class), any(StoragePool.class), Mockito.anyBoolean());
    }

    @Test
    public void testUpdateLockHostForVolume_NullPool_DoesNotSetLockHost() throws Exception {
        Method method = AncientDataMotionStrategy.class.getDeclaredMethod(
                "updateLockHostForVolume",
                EndPoint.class,
                DataObject.class);
        method.setAccessible(true);

        EndPoint endPoint = Mockito.mock(EndPoint.class);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        ClvmPoolManager clvmPoolManager = injectMockedClvmPoolManager();

        method.invoke(strategy, endPoint, volumeInfo);

        Mockito.verify(clvmPoolManager, never()).setClvmLockHostId(any(Long.class), any(Long.class));
        Mockito.verify(clvmPoolManager, never()).getClvmLockHostId(any(Long.class), any(String.class),
                any(String.class), any(StoragePool.class), Mockito.anyBoolean());
    }
}
