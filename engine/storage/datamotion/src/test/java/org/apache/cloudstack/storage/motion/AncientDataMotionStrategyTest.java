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

import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
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
}
