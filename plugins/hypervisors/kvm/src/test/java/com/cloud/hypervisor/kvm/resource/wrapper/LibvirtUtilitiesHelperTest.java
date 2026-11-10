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
package com.cloud.hypervisor.kvm.resource.wrapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Error.ErrorNumber;
import org.libvirt.LibvirtException;
import org.libvirt.StorageVol;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.Pair;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtUtilitiesHelperTest extends TestCase {

    LibvirtUtilitiesHelper libvirtUtilitiesHelperSpy = Mockito.spy(LibvirtUtilitiesHelper.class);

    @Mock
    Connect connectMock;

    @Mock
    KVMStoragePool storagePoolMock;

    @Mock
    StorageVol storageVolMock;

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterExceptionOnRetrievingLibvirtVersionReturnsFalse() throws LibvirtException {
        Mockito.doThrow(LibvirtException.class).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 0l);

        Assert.assertEquals("Unknown due to [null]", result.first());
        Assert.assertFalse(result.second());
    }

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterLibvirtVersionIsLowerThanParameterReturnsFalse() throws LibvirtException {
        long libvirtVersion = 9l;
        Mockito.doReturn(libvirtVersion).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 10l);

        Assert.assertEquals(String.valueOf(libvirtVersion), result.first());
        Assert.assertFalse(result.second());
    }

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterLibvirtVersionIsEqualsToParameterReturnsTrue() throws LibvirtException {
        long libvirtVersion = 10l;
        Mockito.doReturn(libvirtVersion).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 10l);

        Assert.assertEquals(String.valueOf(libvirtVersion), result.first());
        Assert.assertTrue(result.second());
    }

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterLibvirtVersionIsHigherThanParameterReturnsTrue() throws LibvirtException {
        long libvirtVersion = 11l;
        Mockito.doReturn(libvirtVersion).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 10l);

        Assert.assertEquals(String.valueOf(libvirtVersion), result.first());
        Assert.assertTrue(result.second());
    }

    @Test
    public void generateVMSnapshotXMLEscapesSnapshotName() {
        VMSnapshotTO snapshot = Mockito.mock(VMSnapshotTO.class);
        Mockito.doReturn("i-2-3-VM_VS_</name><disks><disk name='vda'/></disks><name>x").when(snapshot).getSnapshotName();
        Mockito.doReturn(1000L).when(snapshot).getCreateTime();

        String xml = libvirtUtilitiesHelperSpy.generateVMSnapshotXML(snapshot, null, "<domain/>");

        Assert.assertTrue(xml.contains("<name>i-2-3-VM_VS_&lt;/name&gt;&lt;disks&gt;&lt;disk name=&apos;vda&apos;/&gt;&lt;/disks&gt;&lt;name&gt;x</name>"));
        Assert.assertFalse(xml.contains("<disks>"));
    }

    @Test
    public void generateVMSnapshotXMLEscapesParentSnapshotName() {
        VMSnapshotTO snapshot = Mockito.mock(VMSnapshotTO.class);
        Mockito.doReturn("child").when(snapshot).getSnapshotName();
        Mockito.doReturn(1000L).when(snapshot).getCreateTime();
        VMSnapshotTO parent = Mockito.mock(VMSnapshotTO.class);
        Mockito.doReturn("parent</name><disks/>").when(parent).getSnapshotName();

        String xml = libvirtUtilitiesHelperSpy.generateVMSnapshotXML(snapshot, parent, "<domain/>");

        Assert.assertTrue(xml.contains("<parent><name>parent&lt;/name&gt;&lt;disks/&gt;</name></parent>"));
        Assert.assertFalse(xml.contains("<disks/>"));
    }

    private static final String RBD_VOLUME_PATH = "cloudstack/0d3c2bd6-cb39-4c8b-a1d9-0e7fbd3b8a5b";

    // Not a spy: a spy rewrites the stack trace of exceptions passing through it, which a mocked LibvirtException does not have.
    private final LibvirtUtilitiesHelper libvirtUtilitiesHelper = new LibvirtUtilitiesHelper();

    private static LibvirtException libvirtException(final ErrorNumber code) {
        final org.libvirt.Error error = Mockito.mock(org.libvirt.Error.class);
        Mockito.when(error.getCode()).thenReturn(code);
        final LibvirtException e = Mockito.mock(LibvirtException.class);
        Mockito.when(e.getError()).thenReturn(error);
        return e;
    }

    @Test
    public void getStorageVolumeByPathVolumeInLibvirtPoolCacheDoesNotRefresh() throws LibvirtException {
        Mockito.when(connectMock.storageVolLookupByPath(RBD_VOLUME_PATH)).thenReturn(storageVolMock);

        Assert.assertSame(storageVolMock, libvirtUtilitiesHelper.getStorageVolumeByPath(connectMock, storagePoolMock, RBD_VOLUME_PATH));

        Mockito.verify(connectMock, Mockito.times(1)).storageVolLookupByPath(RBD_VOLUME_PATH);
        Mockito.verify(storagePoolMock, Mockito.never()).refresh();
    }

    /**
     * An RBD volume created through librbd is not in libvirt's cache of the pool until the pool is
     * refreshed, so the first lookup by path misses. The pool is refreshed and the volume is
     * looked up once more.
     */
    @Test
    public void getStorageVolumeByPathRbdVolumeNotInLibvirtPoolCacheRefreshesPool() throws LibvirtException {
        final LibvirtException notFound = libvirtException(ErrorNumber.VIR_ERR_NO_STORAGE_VOL);
        Mockito.when(storagePoolMock.getType()).thenReturn(StoragePoolType.RBD);
        Mockito.when(connectMock.storageVolLookupByPath(RBD_VOLUME_PATH)).thenThrow(notFound).thenReturn(storageVolMock);
        Mockito.when(storagePoolMock.refresh()).thenReturn(true);

        Assert.assertSame(storageVolMock, libvirtUtilitiesHelper.getStorageVolumeByPath(connectMock, storagePoolMock, RBD_VOLUME_PATH));

        InOrder inOrder = Mockito.inOrder(connectMock, storagePoolMock);
        inOrder.verify(connectMock).storageVolLookupByPath(RBD_VOLUME_PATH);
        inOrder.verify(storagePoolMock).refresh();
        inOrder.verify(connectMock).storageVolLookupByPath(RBD_VOLUME_PATH);
    }

    /**
     * If the RBD volume is still missing after the pool is refreshed, the lookup fails after a
     * single refresh and a single retry.
     */
    @Test
    public void getStorageVolumeByPathRbdVolumeNotFoundAfterRefreshThrows() throws LibvirtException {
        assertRbdLookupRetriedOnce(true);
    }

    /**
     * A refresh that fails does not stop the retry.
     */
    @Test
    public void getStorageVolumeByPathRbdVolumeRetriedAfterFailedRefresh() throws LibvirtException {
        assertRbdLookupRetriedOnce(false);
    }

    private void assertRbdLookupRetriedOnce(final boolean refreshSucceeds) throws LibvirtException {
        final LibvirtException notFound = libvirtException(ErrorNumber.VIR_ERR_NO_STORAGE_VOL);
        Mockito.when(storagePoolMock.getType()).thenReturn(StoragePoolType.RBD);
        Mockito.when(connectMock.storageVolLookupByPath(RBD_VOLUME_PATH)).thenThrow(notFound);
        Mockito.when(storagePoolMock.refresh()).thenReturn(refreshSucceeds);

        try {
            libvirtUtilitiesHelper.getStorageVolumeByPath(connectMock, storagePoolMock, RBD_VOLUME_PATH);
            Assert.fail("Expected the lookup to fail");
        } catch (final LibvirtException e) {
            Assert.assertSame(notFound, e);
        }

        Mockito.verify(connectMock, Mockito.times(2)).storageVolLookupByPath(RBD_VOLUME_PATH);
        Mockito.verify(storagePoolMock, Mockito.times(1)).refresh();
    }

    /**
     * A lookup on an RBD pool that fails for another reason than the volume not being found
     * fails without refreshing the pool.
     */
    @Test
    public void getStorageVolumeByPathRbdOtherLookupErrorDoesNotRefresh() throws LibvirtException {
        assertLookupFailsWithoutRefresh(StoragePoolType.RBD, RBD_VOLUME_PATH, libvirtException(ErrorNumber.VIR_ERR_INVALID_CONN));
    }

    /**
     * A LibvirtException without an error attached is not taken for a missing volume.
     */
    @Test
    public void getStorageVolumeByPathRbdLookupErrorWithoutCodeDoesNotRefresh() throws LibvirtException {
        assertLookupFailsWithoutRefresh(StoragePoolType.RBD, RBD_VOLUME_PATH, Mockito.mock(LibvirtException.class));
    }

    /**
     * A missing volume on a pool that is not RBD fails without refreshing the pool.
     */
    @Test
    public void getStorageVolumeByPathNonRbdVolumeNotFoundDoesNotRefresh() throws LibvirtException {
        assertLookupFailsWithoutRefresh(StoragePoolType.NetworkFilesystem, "/mnt/0d3c2bd6-cb39-4c8b-a1d9-0e7fbd3b8a5b/volume",
                libvirtException(ErrorNumber.VIR_ERR_NO_STORAGE_VOL));
    }

    private void assertLookupFailsWithoutRefresh(final StoragePoolType poolType, final String path, final LibvirtException lookupError)
            throws LibvirtException {
        Mockito.when(storagePoolMock.getType()).thenReturn(poolType);
        Mockito.when(connectMock.storageVolLookupByPath(path)).thenThrow(lookupError);

        try {
            libvirtUtilitiesHelper.getStorageVolumeByPath(connectMock, storagePoolMock, path);
            Assert.fail("Expected the lookup to fail");
        } catch (final LibvirtException e) {
            Assert.assertSame(lookupError, e);
        }

        Mockito.verify(connectMock, Mockito.times(1)).storageVolLookupByPath(path);
        Mockito.verify(storagePoolMock, Mockito.never()).refresh();
    }
}
