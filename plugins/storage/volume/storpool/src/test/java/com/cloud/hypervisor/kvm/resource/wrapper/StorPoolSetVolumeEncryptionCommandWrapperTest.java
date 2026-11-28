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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.storage.StorPoolSetVolumeEncryptionAnswer;
import com.cloud.agent.api.storage.StorPoolSetVolumeEncryptionCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.cryptsetup.CryptSetup;
import org.apache.cloudstack.utils.cryptsetup.CryptSetupException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StorPoolSetVolumeEncryptionCommandWrapperTest {

    private static final String VOLUME_PATH = "/dev/storpool-byid/t.t.t";
    private static final String SRC_SNAPSHOT_PATH = "/dev/storpool-byid/s.s.s";
    private static final String PASSPHRASE = "passphrase";

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private KVMStoragePoolManager storagePoolMgr;
    @Mock
    private KVMStoragePool pool;
    @Mock
    private PrimaryDataStoreTO primaryStore;

    private MockedStatic<StorPoolStorageAdaptor> storPoolStorageAdaptor;
    private VolumeObjectTO volume;
    private final StorPoolSetVolumeEncryptionCommandWrapper wrapper = new StorPoolSetVolumeEncryptionCommandWrapper();

    @Before
    public void setUp() {
        storPoolStorageAdaptor = Mockito.mockStatic(StorPoolStorageAdaptor.class);

        KVMPhysicalDisk disk = new KVMPhysicalDisk(VOLUME_PATH, "t.t.t", pool);

        volume = new VolumeObjectTO();
        volume.setPath(VOLUME_PATH);
        volume.setDataStore(primaryStore);
        volume.setPassphrase(PASSPHRASE.getBytes());

        when(primaryStore.getPoolType()).thenReturn(StoragePoolType.StorPool);
        when(primaryStore.getUuid()).thenReturn("pool-uuid");
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePool(StoragePoolType.StorPool, "pool-uuid")).thenReturn(pool);
        when(pool.getPhysicalDisk(VOLUME_PATH)).thenReturn(disk);
    }

    @After
    public void tearDown() {
        storPoolStorageAdaptor.close();
    }

    @Test
    public void testEncryptDataDiskSucceeds() {
        try (MockedConstruction<CryptSetup> ignored = Mockito.mockConstruction(CryptSetup.class)) {
            StorPoolSetVolumeEncryptionAnswer answer = wrapper.execute(new StorPoolSetVolumeEncryptionCommand(volume, null, true), libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("luks", answer.getVolume().getEncryptFormat());
        }
        storPoolStorageAdaptor.verify(() -> StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", VOLUME_PATH));
        assertPassphraseCleared();
    }

    @Test
    public void testEncryptDataDiskReturnsFailureWhenLuksFormatFails() {
        try (MockedConstruction<CryptSetup> ignored = Mockito.mockConstruction(CryptSetup.class,
                (crypt, context) -> doThrow(new CryptSetupException("luksFormat failed")).when(crypt).luksFormat(any(), any(), anyString()))) {
            StorPoolSetVolumeEncryptionAnswer answer = wrapper.execute(new StorPoolSetVolumeEncryptionCommand(volume, null, true), libvirtComputingResource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("luksFormat failed"));
        }
        storPoolStorageAdaptor.verify(() -> StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", VOLUME_PATH));
        assertPassphraseCleared();
    }

    @Test
    public void testEncryptRootDiskReturnsFailureAndDetachesSourceSnapshot() {
        when(pool.getPhysicalDisk(SRC_SNAPSHOT_PATH)).thenThrow(new CloudRuntimeException("snapshot not found"));

        StorPoolSetVolumeEncryptionAnswer answer = wrapper.execute(new StorPoolSetVolumeEncryptionCommand(volume, SRC_SNAPSHOT_PATH, false), libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("snapshot not found"));
        storPoolStorageAdaptor.verify(() -> StorPoolStorageAdaptor.attachOrDetachVolume("detach", "snapshot", SRC_SNAPSHOT_PATH));
        storPoolStorageAdaptor.verify(() -> StorPoolStorageAdaptor.attachOrDetachVolume(eq("detach"), eq("volume"), eq(VOLUME_PATH)));
        assertPassphraseCleared();
    }

    private void assertPassphraseCleared() {
        Assert.assertArrayEquals(new byte[PASSPHRASE.length()], volume.getPassphrase());
    }
}
