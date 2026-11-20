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
package com.cloud.storage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloud.storage.Storage.StoragePoolType;

public class StorageTest {
    @Before
    public void setUp() {
    }

    @Test
    public void isSharedStoragePool() {
        Assert.assertFalse(StoragePoolType.Filesystem.isShared());
        Assert.assertTrue(StoragePoolType.NetworkFilesystem.isShared());
        Assert.assertTrue(StoragePoolType.IscsiLUN.isShared());
        Assert.assertTrue(StoragePoolType.Iscsi.isShared());
        Assert.assertFalse(StoragePoolType.ISO.isShared());
        Assert.assertTrue(StoragePoolType.Iscsi.isShared());
        Assert.assertFalse(StoragePoolType.LVM.isShared());
        Assert.assertTrue(StoragePoolType.CLVM.isShared());
        Assert.assertTrue(StoragePoolType.RBD.isShared());
        Assert.assertTrue(StoragePoolType.PowerFlex.isShared());
        Assert.assertTrue(StoragePoolType.SharedMountPoint.isShared());
        Assert.assertTrue(StoragePoolType.VMFS.isShared());
        Assert.assertTrue(StoragePoolType.PreSetup.isShared());
        Assert.assertFalse(StoragePoolType.EXT.isShared());
        Assert.assertTrue(StoragePoolType.OCFS2.isShared());
        Assert.assertTrue(StoragePoolType.SMB.isShared());
        Assert.assertTrue(StoragePoolType.Gluster.isShared());
        Assert.assertTrue(StoragePoolType.ManagedNFS.isShared());
        Assert.assertTrue(StoragePoolType.DatastoreCluster.isShared());
        Assert.assertTrue(StoragePoolType.Linstor.isShared());
    }

    @Test
    public void supportsOverProvisioningTestAllStoragePoolTypes() {
        Assert.assertTrue(StoragePoolType.Filesystem.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.NetworkFilesystem.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.IscsiLUN.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.Iscsi.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.ISO.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.Iscsi.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.LVM.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.CLVM.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.RBD.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.PowerFlex.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.SharedMountPoint.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.VMFS.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.PreSetup.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.EXT.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.OCFS2.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.SMB.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.Gluster.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.ManagedNFS.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.DatastoreCluster.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.Linstor.supportsOverProvisioning());
    }

    @Test
    public void equalityTest() {
        StoragePoolType t1 = StoragePoolType.NetworkFilesystem;
        StoragePoolType t2 = StoragePoolType.NetworkFilesystem;
        Assert.assertTrue(t1 == StoragePoolType.NetworkFilesystem);
        Assert.assertTrue(t1.equals(StoragePoolType.NetworkFilesystem));
        Assert.assertFalse(t1.equals(StoragePoolType.EXT));
    }

    @Test
    public void arePoolsSameTypeForTwoPoolsOfThatType() {
        for (StoragePoolType poolType : StoragePoolType.values()) {
            Assert.assertTrue(poolType + " -> " + poolType, Storage.arePoolsSameType(poolType, poolType));
            for (StoragePoolType other : StoragePoolType.values()) {
                if (other.equals(poolType)) {
                    continue;
                }
                Assert.assertFalse(poolType + " -> " + other, Storage.arePoolsSameType(poolType, other));
            }
        }
    }

    @Test
    public void isAnyPoolFileBasedForOneFileBasedPool() {
        for (StoragePoolType fileBased : Storage.KVM_FILE_BASED_POOL_TYPES) {
            Assert.assertTrue("RBD -> " + fileBased, Storage.isAnyPoolFileBased(StoragePoolType.RBD, fileBased));
            Assert.assertTrue(fileBased + " -> RBD", Storage.isAnyPoolFileBased(fileBased, StoragePoolType.RBD));
            for (StoragePoolType otherFileBased : Storage.KVM_FILE_BASED_POOL_TYPES) {
                Assert.assertTrue(fileBased + " -> " + otherFileBased, Storage.isAnyPoolFileBased(fileBased, otherFileBased));
            }
        }
        Assert.assertFalse("RBD -> RBD", Storage.isAnyPoolFileBased(StoragePoolType.RBD, StoragePoolType.RBD));
    }

    @Test
    public void isCopiedInPlaceOnKvmForRbdAndForRbdAndFileBasedPools() {
        Assert.assertTrue("RBD -> RBD", Storage.isCopiedInPlaceOnKvm(StoragePoolType.RBD, StoragePoolType.RBD));
        for (StoragePoolType fileBased : Storage.KVM_FILE_BASED_POOL_TYPES) {
            Assert.assertTrue("RBD -> " + fileBased, Storage.isCopiedInPlaceOnKvm(StoragePoolType.RBD, fileBased));
            Assert.assertTrue(fileBased + " -> RBD", Storage.isCopiedInPlaceOnKvm(fileBased, StoragePoolType.RBD));
        }
    }

    @Test
    public void isCopiedInPlaceOnKvmNotBetweenTwoFileBasedPools() {
        for (StoragePoolType srcType : Storage.KVM_FILE_BASED_POOL_TYPES) {
            for (StoragePoolType destType : Storage.KVM_FILE_BASED_POOL_TYPES) {
                Assert.assertFalse(srcType + " -> " + destType, Storage.isCopiedInPlaceOnKvm(srcType, destType));
            }
        }
    }

    @Test
    public void isCopiedInPlaceOnKvmNotWithOtherPoolTypes() {
        for (StoragePoolType other : StoragePoolType.values()) {
            if (StoragePoolType.RBD.equals(other) || Storage.KVM_FILE_BASED_POOL_TYPES.contains(other)) {
                continue;
            }
            Assert.assertFalse(other + " -> " + other, Storage.isCopiedInPlaceOnKvm(other, other));
            Assert.assertFalse("RBD -> " + other, Storage.isCopiedInPlaceOnKvm(StoragePoolType.RBD, other));
            Assert.assertFalse(other + " -> RBD", Storage.isCopiedInPlaceOnKvm(other, StoragePoolType.RBD));
        }
    }

    @Test
    public void areBothPoolsFileBasedBetweenTwoFileBasedPools() {
        for (StoragePoolType srcType : Storage.KVM_FILE_BASED_POOL_TYPES) {
            for (StoragePoolType destType : Storage.KVM_FILE_BASED_POOL_TYPES) {
                Assert.assertTrue(srcType + " -> " + destType, Storage.areBothPoolsFileBased(srcType, destType));
            }
        }
    }

    @Test
    public void areBothPoolsFileBasedNotWithOtherPoolTypes() {
        for (StoragePoolType other : StoragePoolType.values()) {
            if (Storage.KVM_FILE_BASED_POOL_TYPES.contains(other)) {
                continue;
            }
            Assert.assertFalse(other + " -> " + other, Storage.areBothPoolsFileBased(other, other));
            for (StoragePoolType fileBased : Storage.KVM_FILE_BASED_POOL_TYPES) {
                Assert.assertFalse(fileBased + " -> " + other, Storage.areBothPoolsFileBased(fileBased, other));
                Assert.assertFalse(other + " -> " + fileBased, Storage.areBothPoolsFileBased(other, fileBased));
            }
        }
    }
}
