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
    }

    @Test
    public void supportsOverprovisioningStoragePool() {
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
        Assert.assertFalse(StoragePoolType.SharedMountPoint.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.VMFS.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.PreSetup.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.EXT.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.OCFS2.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.SMB.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.Gluster.supportsOverProvisioning());
        Assert.assertFalse(StoragePoolType.ManagedNFS.supportsOverProvisioning());
        Assert.assertTrue(StoragePoolType.DatastoreCluster.supportsOverProvisioning());
    }
}
