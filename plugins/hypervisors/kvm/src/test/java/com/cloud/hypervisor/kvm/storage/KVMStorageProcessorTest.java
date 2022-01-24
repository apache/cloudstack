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
package com.cloud.hypervisor.kvm.storage;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtUtilitiesHelper;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import javax.naming.ConfigurationException;

import com.cloud.utils.script.Script;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({ Script.class })
@RunWith(PowerMockRunner.class)
public class KVMStorageProcessorTest {

    @Mock
    KVMStoragePoolManager storagePoolManager;
    @Mock
    LibvirtComputingResource resource;

    @InjectMocks
    private KVMStorageProcessor storageProcessor;

    @Spy
    KVMStorageProcessor storageProcessorSpy = new KVMStorageProcessor(storagePoolManager, resource);

    @Mock
    Pair<String, Set<String>> diskToSnapshotAndDisksToAvoidMock;

    @Mock
    Domain domainMock;

    @Mock
    KVMStoragePool kvmStoragePoolMock;

    @Mock
    VolumeObjectTO volumeObjectToMock;

    @Mock
    SnapshotObjectTO snapshotObjectToMock;

    @Mock
    Connect connectMock;

    private static final String directDownloadTemporaryPath = "/var/lib/libvirt/images/dd";
    private static final long templateSize = 80000L;

    @Before
    public void setUp() throws ConfigurationException {
        MockitoAnnotations.initMocks(this);
        storageProcessor = new KVMStorageProcessor(storagePoolManager, resource);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationAssumeEnoughSpaceWhenNotProvided() {
        PowerMockito.mockStatic(Script.class);
        Mockito.when(resource.getDirectDownloadTemporaryDownloadPath()).thenReturn(directDownloadTemporaryPath);
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(null);
        Assert.assertTrue(result);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationNotEnoughSpace() {
        PowerMockito.mockStatic(Script.class);
        Mockito.when(resource.getDirectDownloadTemporaryDownloadPath()).thenReturn(directDownloadTemporaryPath);
        String output = String.valueOf(templateSize - 30000L);
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).thenReturn(output);
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(templateSize);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationEnoughSpace() {
        PowerMockito.mockStatic(Script.class);
        Mockito.when(resource.getDirectDownloadTemporaryDownloadPath()).thenReturn(directDownloadTemporaryPath);
        String output = String.valueOf(templateSize + 30000L);
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).thenReturn(output);
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(templateSize);
        Assert.assertTrue(result);
    }

    @Test
    public void testIsEnoughSpaceForDownloadTemplateOnTemporaryLocationNotExistingLocation() {
        PowerMockito.mockStatic(Script.class);
        Mockito.when(resource.getDirectDownloadTemporaryDownloadPath()).thenReturn(directDownloadTemporaryPath);
        String output = String.format("df: ‘%s’: No such file or directory", directDownloadTemporaryPath);
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).thenReturn(output);
        boolean result = storageProcessor.isEnoughSpaceForDownloadTemplateOnTemporaryLocation(templateSize);
        Assert.assertFalse(result);
    }

    @Test
    public void validateGetSnapshotTemporaryPath(){
        String path = "/path/to/disk";
        String snapshotName = "snapshot";
        String expectedResult = "/path/to/snapshot";

        String result = storageProcessor.getSnapshotTemporaryPath(path, snapshotName);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetSnapshotPathInPrimaryStorage(){
        String path = "/path/to/disk";
        String snapshotName = "snapshot";
        String expectedResult = String.format("%s%s%s%s%s", path, File.separator, TemplateConstants.DEFAULT_SNAPSHOT_ROOT_DIR, File.separator, snapshotName);

        String result = storageProcessor.getSnapshotPathInPrimaryStorage(path, snapshotName);
        Assert.assertEquals(expectedResult, result);
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateValidateAvailableSizeOnPoolToTakeVolumeSnapshotAvailabeSizeLessThanMinRateThrowCloudRuntimeException(){
        KVMPhysicalDisk kvmPhysicalDiskMock = Mockito.mock(KVMPhysicalDisk.class);

        Mockito.doReturn(104l).when(kvmStoragePoolMock).getAvailable();
        Mockito.doReturn(100l).when(kvmPhysicalDiskMock).getSize();

        storageProcessor.validateAvailableSizeOnPoolToTakeVolumeSnapshot(kvmStoragePoolMock, kvmPhysicalDiskMock);
    }

    @Test
    public void validateValidateAvailableSizeOnPoolToTakeVolumeSnapshotAvailabeSizeEqualOrHigherThanMinRateDoNothing(){
        KVMPhysicalDisk kvmPhysicalDiskMock = Mockito.mock(KVMPhysicalDisk.class);

        Mockito.doReturn(105l, 106l).when(kvmStoragePoolMock).getAvailable();
        Mockito.doReturn(100l).when(kvmPhysicalDiskMock).getSize();

        storageProcessor.validateAvailableSizeOnPoolToTakeVolumeSnapshot(kvmStoragePoolMock, kvmPhysicalDiskMock);
        storageProcessor.validateAvailableSizeOnPoolToTakeVolumeSnapshot(kvmStoragePoolMock, kvmPhysicalDiskMock);
    }

    private List<LibvirtVMDef.DiskDef> createDiskDefs(int iterations, boolean duplicatePath) {
        List<LibvirtVMDef.DiskDef> disks = new ArrayList<>();

        for (int i = 1; i <= iterations; i++) {
            LibvirtVMDef.DiskDef disk = new LibvirtVMDef.DiskDef();
            disk.defFileBasedDisk(String.format("path%s", duplicatePath ? "" : i), String.format("label%s", i), LibvirtVMDef.DiskDef.DiskBus.USB, LibvirtVMDef.DiskDef.DiskFmtType.RAW);
            disks.add(disk);
        }

        return disks;
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateGetDiskToSnapshotAndDisksToAvoidDuplicatePathThrowsCloudRuntimeException() throws LibvirtException{
        List<LibvirtVMDef.DiskDef> disks = createDiskDefs(2, true);

        storageProcessor.getDiskToSnapshotAndDisksToAvoid(disks, "path", domainMock);
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateGetDiskToSnapshotAndDisksToAvoidPathNotFoundThrowsCloudRuntimeException() throws LibvirtException{
        List<LibvirtVMDef.DiskDef> disks = createDiskDefs(5, false);

        storageProcessor.getDiskToSnapshotAndDisksToAvoid(disks, "path6", domainMock);
    }

    @Test
    public void validateGetDiskToSnapshotAndDisksToAvoidPathFoundReturnLabels() throws LibvirtException{
        List<LibvirtVMDef.DiskDef> disks = createDiskDefs(5, false);

        String expectedLabelResult = "label2";
        long expectedDisksSizeResult = disks.size() - 1;

        Pair<String, Set<String>> result = storageProcessor.getDiskToSnapshotAndDisksToAvoid(disks, "path2", domainMock);

        Assert.assertEquals(expectedLabelResult, result.first());
        Assert.assertEquals(expectedDisksSizeResult, result.second().size());
    }

    @Test (expected = LibvirtException.class)
    public void validateTakeVolumeSnapshotFailToCreateSnapshotThrowLibvirtException() throws LibvirtException{
        Mockito.doReturn(diskToSnapshotAndDisksToAvoidMock).when(storageProcessorSpy).getDiskToSnapshotAndDisksToAvoid(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.doReturn("").when(domainMock).getName();
        Mockito.doReturn(new HashSet<>()).when(diskToSnapshotAndDisksToAvoidMock).second();
        Mockito.doThrow(LibvirtException.class).when(domainMock).snapshotCreateXML(Mockito.anyString(), Mockito.anyInt());

        storageProcessorSpy.takeVolumeSnapshot(new ArrayList<>(), "", "", domainMock);
    }

    @Test
    public void validateTakeVolumeSnapshotSuccessReturnDiskLabel() throws LibvirtException{
        String expectedResult = "label";

        Mockito.doReturn(diskToSnapshotAndDisksToAvoidMock).when(storageProcessorSpy).getDiskToSnapshotAndDisksToAvoid(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.doReturn("").when(domainMock).getName();
        Mockito.doReturn(expectedResult).when(diskToSnapshotAndDisksToAvoidMock).first();
        Mockito.doReturn(new HashSet<>()).when(diskToSnapshotAndDisksToAvoidMock).second();
        Mockito.doReturn(null).when(domainMock).snapshotCreateXML(Mockito.anyString(), Mockito.anyInt());

        String result = storageProcessorSpy.takeVolumeSnapshot(new ArrayList<>(), "", "", domainMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @PrepareForTest(KVMStorageProcessor.class)
    public void validateCopySnapshotToPrimaryStorageDirFailToCopyReturnErrorMessage() throws Exception {
        String baseFile = "baseFile";
        String snapshotPath = "snapshotPath";
        String errorMessage = "error";
        String expectedResult = String.format("Unable to copy %s snapshot [%s] to [%s] due to [%s].", volumeObjectToMock, baseFile, snapshotPath, errorMessage);

        Mockito.doReturn(true).when(kvmStoragePoolMock).createFolder(Mockito.anyString());
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.copy(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any())).thenThrow(new IOException(errorMessage));

        String result = storageProcessorSpy.copySnapshotToPrimaryStorageDir(kvmStoragePoolMock, baseFile, snapshotPath, volumeObjectToMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @PrepareForTest(KVMStorageProcessor.class)
    public void validateCopySnapshotToPrimaryStorageDirCopySuccessReturnNull() throws Exception {
        String baseFile = "baseFile";
        String snapshotPath = "snapshotPath";

        Mockito.doReturn(true).when(kvmStoragePoolMock).createFolder(Mockito.anyString());
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.copy(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any())).thenReturn(null);

        String result = storageProcessorSpy.copySnapshotToPrimaryStorageDir(kvmStoragePoolMock, baseFile, snapshotPath, volumeObjectToMock);

        Assert.assertNull(result);
    }

    @Test (expected = CloudRuntimeException.class)
    @PrepareForTest({Script.class, LibvirtUtilitiesHelper.class})
    public void validateMergeSnapshotIntoBaseFileErrorOnMergeThrowCloudRuntimeException() throws Exception {
        PowerMockito.mockStatic(Script.class, LibvirtUtilitiesHelper.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.anyString())).thenReturn("");
        PowerMockito.when(LibvirtUtilitiesHelper.isLibvirtSupportingFlagDeleteOnCommandVirshBlockcommit(Mockito.any())).thenReturn(true);

        storageProcessorSpy.mergeSnapshotIntoBaseFile(domainMock, "", "", "", volumeObjectToMock, connectMock);
    }

    @Test
    @PrepareForTest({Script.class, LibvirtUtilitiesHelper.class})
    public void validateMergeSnapshotIntoBaseFileMergeSuccessDoNothing() throws Exception {
        PowerMockito.mockStatic(Script.class, LibvirtUtilitiesHelper.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.anyString())).thenReturn(null);
        PowerMockito.when(LibvirtUtilitiesHelper.isLibvirtSupportingFlagDeleteOnCommandVirshBlockcommit(Mockito.any())).thenReturn(true);
        Mockito.doNothing().when(storageProcessorSpy).manuallyDeleteUnusedSnapshotFile(Mockito.anyBoolean(), Mockito.any());

        storageProcessorSpy.mergeSnapshotIntoBaseFile(domainMock, "", "", "", volumeObjectToMock, connectMock);
    }

    @Test (expected = CloudRuntimeException.class)
    @PrepareForTest(KVMStorageProcessor.class)
    public void validateManuallyDeleteUnusedSnapshotFileLibvirtDoesNotSupportsFlagDeleteExceptionOnFileDeletionThrowsException() throws IOException {
        Mockito.doReturn("").when(snapshotObjectToMock).getPath();
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.deleteIfExists(Mockito.any(Path.class))).thenThrow(IOException.class);

        storageProcessorSpy.manuallyDeleteUnusedSnapshotFile(false, "");
    }

    @Test
    public void validateIsAvailablePoolSizeDividedByDiskSizeLesserThanMinRate(){
        Assert.assertTrue(storageProcessorSpy.isAvailablePoolSizeDividedByDiskSizeLesserThanMinRate(10499l, 10000l));
        Assert.assertFalse(storageProcessorSpy.isAvailablePoolSizeDividedByDiskSizeLesserThanMinRate(10500l, 10000l));
        Assert.assertFalse(storageProcessorSpy.isAvailablePoolSizeDividedByDiskSizeLesserThanMinRate(10501l, 10000l));
    }

    @Test
    public void validateValidateCopyResultResultIsNullReturn() throws CloudRuntimeException, IOException{
        storageProcessorSpy.validateCopyResult(null, "");
    }

    @Test (expected = IOException.class)
    public void validateValidateCopyResultFailToDeleteThrowIOException() throws CloudRuntimeException, IOException{
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.deleteIfExists(Mockito.any())).thenThrow(new IOException(""));
        storageProcessorSpy.validateCopyResult("", "");
    }

    @Test (expected = CloudRuntimeException.class)
    @PrepareForTest(KVMStorageProcessor.class)
    public void validateValidateCopyResulResultNotNullThrowCloudRuntimeException() throws CloudRuntimeException, IOException{
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.deleteIfExists(Mockito.any())).thenReturn(true);
        storageProcessorSpy.validateCopyResult("", "");
    }

    @Test (expected = CloudRuntimeException.class)
    @PrepareForTest(KVMStorageProcessor.class)
    public void validateDeleteSnapshotFileErrorOnDeleteThrowsCloudRuntimeException() throws Exception {
        Mockito.doReturn("").when(snapshotObjectToMock).getPath();
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.deleteIfExists(Mockito.any(Path.class))).thenThrow(IOException.class);

        storageProcessorSpy.deleteSnapshotFile(snapshotObjectToMock);
    }

    @Test
    @PrepareForTest(KVMStorageProcessor.class)
    public void validateDeleteSnapshotFileSuccess () throws IOException {
        Mockito.doReturn("").when(snapshotObjectToMock).getPath();
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.deleteIfExists(Mockito.any(Path.class))).thenReturn(true);

        storageProcessorSpy.deleteSnapshotFile(snapshotObjectToMock);
    }
}
