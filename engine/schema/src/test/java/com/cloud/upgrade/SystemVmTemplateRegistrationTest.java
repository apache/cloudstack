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

package com.cloud.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.cpu.CPU;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class SystemVmTemplateRegistrationTest {

    @Mock
    ClusterDao clusterDao;

    @Mock
    VMTemplateDao vmTemplateDao;

    @Spy
    @InjectMocks
    SystemVmTemplateRegistration systemVmTemplateRegistration = new SystemVmTemplateRegistration();

    private void setupMetadataFile(MockedStatic<SystemVmTemplateRegistration> mockedStatic, String content) {
        try {
            String location = "metadata.ini";
            if (StringUtils.isNotBlank(content)) {
                File tempFile = File.createTempFile("metadata", ".ini");
                location = tempFile.getAbsolutePath();
                Files.write(Paths.get(location), content.getBytes());
                tempFile.deleteOnExit();
            }
            mockedStatic.when(SystemVmTemplateRegistration::getMetadataFilePath).thenReturn(location);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_parseMetadataFile_noFile() {
        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class, Mockito.CALLS_REAL_METHODS)) {
            setupMetadataFile(mockedStatic, null);
            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class,
                    SystemVmTemplateRegistration::parseMetadataFile);
            assertTrue(exception.getMessage().contains("Failed to parse systemVM template metadata file"));
        }
    }

    @Test
    public void test_parseMetadataFile_invalidContent() {
        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class, Mockito.CALLS_REAL_METHODS)) {
            setupMetadataFile(mockedStatic, "abc");
            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class,
                    SystemVmTemplateRegistration::parseMetadataFile);
            assertTrue(exception.getMessage().contains("Failed to parse systemVM template metadata file"));
        }
    }

    @Test
    public void test_parseMetadataFile_success() {
        String metadataFileContent = "[default]\n" +
                "version = x.y.z.0\n" +
                "\n" +
                "[kvm-x86_64]\n" +
                "templatename = systemvm-kvm-x.y.z\n" +
                "checksum = abc1\n" +
                "downloadurl = https://download.cloudstack.org/systemvm/x.y/systemvmtemplate-x.y.z-kvm.qcow2.bz2\n" +
                "filename = systemvmtemplate-x.y.z-kvm.qcow2.bz2\n" +
                "\n" +
                "[kvm-aarch64]\n" +
                "templatename = systemvm-kvm-x.y.z\n" +
                "checksum = abc2\n" +
                "downloadurl = https://download.cloudstack.org/systemvm/x.y/systemvmtemplate-x.y.z-kvm.qcow2.bz2\n" +
                "filename = systemvmtemplate-x.y.z-kvm.qcow2.bz2\n" +
                "\n" +
                "[vmware]\n" +
                "templatename = systemvm-vmware-x.y.z\n" +
                "checksum = abc3\n" +
                "downloadurl = https://download.cloudstack.org/systemvm/x.y/systemvmtemplate-x.y.z-vmware.ova\n" +
                "filename = systemvmtemplate-x.y.z-vmware.ova\n";
        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class, Mockito.CALLS_REAL_METHODS)) {
            setupMetadataFile(mockedStatic, metadataFileContent);
            String version = SystemVmTemplateRegistration.parseMetadataFile();
            assertEquals("x.y.z.0", version);
        }
        assertNull(SystemVmTemplateRegistration.NewTemplateMap.get("xenserver"));
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                SystemVmTemplateRegistration.NewTemplateMap.get("kvm-x86_64");
        assertNotNull(templateDetails);
        assertEquals(CPU.CPUArch.amd64, templateDetails.getArch());
        assertEquals(Hypervisor.HypervisorType.KVM, templateDetails.getHypervisorType());
        templateDetails =
                SystemVmTemplateRegistration.NewTemplateMap.get("kvm-aarch64");
        assertNotNull(templateDetails);
        assertEquals(CPU.CPUArch.arm64, templateDetails.getArch());
        assertEquals(Hypervisor.HypervisorType.KVM, templateDetails.getHypervisorType());
        templateDetails =
                SystemVmTemplateRegistration.NewTemplateMap.get("vmware");
        assertNotNull(templateDetails);
        assertNull(templateDetails.getArch());
        assertEquals(Hypervisor.HypervisorType.VMware, templateDetails.getHypervisorType());
    }

    @Test
    public void testMountStore_nullStoreUrl() throws Exception {
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            SystemVmTemplateRegistration.mountStore(null, "/mnt/nfs", "nfs3");
            scriptMock.verifyNoInteractions();
        }
    }

    @Test
    public void testMountStore_validStoreUrl() throws Exception {
        String storeUrl = "nfs://192.168.1.100/export";
        String path = "/mnt/nfs";
        String nfsVersion = "nfs3";
        String expectedMountCommand = "expectedMountCommand";
        try (MockedStatic<UriUtils> uriUtilsMock = Mockito.mockStatic(UriUtils.class);
             MockedStatic<SystemVmTemplateRegistration> sysVmMock =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            uriUtilsMock.when(() -> UriUtils.encodeURIComponent(storeUrl)).thenReturn(storeUrl);
            sysVmMock.when(() -> SystemVmTemplateRegistration.getMountCommand(
                    eq(nfsVersion),
                    eq("192.168.1.100:/export"),
                    eq(path)
            )).thenReturn(expectedMountCommand);
            SystemVmTemplateRegistration.mountStore(storeUrl, path, nfsVersion);
            scriptMock.verify(() -> Script.runSimpleBashScript(expectedMountCommand), times(1));
        }
    }

    @Test
    public void testValidateTemplateFile_fileNotFound() {
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                new SystemVmTemplateRegistration.MetadataTemplateDetails(Hypervisor.HypervisorType.KVM,
                        "name", "file", "url", "checksum", CPU.CPUArch.amd64);
        SystemVmTemplateRegistration.NewTemplateMap.put(SystemVmTemplateRegistration.getHypervisorArchKey(
                details.getHypervisorType(), details.getArch()), details);
        doReturn(null).when(systemVmTemplateRegistration).getTemplateFile(details);
        try {
            systemVmTemplateRegistration.validateTemplateFileForHypervisorAndArch(details.getHypervisorType(),
                    details.getArch());
            fail("Expected CloudRuntimeException due to missing template file");
        } catch (CloudRuntimeException e) {
            assertEquals("Failed to find local template file", e.getMessage());
        }
    }

    @Test
    public void testValidateTemplateFile_checksumMismatch() {
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                new SystemVmTemplateRegistration.MetadataTemplateDetails(Hypervisor.HypervisorType.KVM,
                        "name", "file", "url", "checksum", CPU.CPUArch.amd64);
        File dummyFile = new File("dummy.txt");
        SystemVmTemplateRegistration.NewTemplateMap.put(SystemVmTemplateRegistration.getHypervisorArchKey(
                details.getHypervisorType(), details.getArch()), details);
        doReturn(dummyFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        doReturn(true).when(systemVmTemplateRegistration).isTemplateFileChecksumDifferent(details, dummyFile);
        try {
            systemVmTemplateRegistration.validateTemplateFileForHypervisorAndArch(details.getHypervisorType(),
                    details.getArch());
            fail("Expected CloudRuntimeException due to checksum failure");
        } catch (CloudRuntimeException e) {
            assertEquals("Checksum failed for local template file", e.getMessage());
        }
    }

    @Test
    public void testValidateTemplateFile_success() {
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                new SystemVmTemplateRegistration.MetadataTemplateDetails(Hypervisor.HypervisorType.KVM,
                        "name", "file", "url", "checksum", CPU.CPUArch.amd64);
        File dummyFile = new File("dummy.txt");
        SystemVmTemplateRegistration.NewTemplateMap.put(SystemVmTemplateRegistration.getHypervisorArchKey(
                details.getHypervisorType(), details.getArch()), details);
        doReturn(dummyFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        doReturn(false).when(systemVmTemplateRegistration).isTemplateFileChecksumDifferent(details, dummyFile);
        systemVmTemplateRegistration.validateTemplateFileForHypervisorAndArch(details.getHypervisorType(),
                details.getArch());
    }

    @Test
    public void testValidateAndRegisterTemplate() {
        Hypervisor.HypervisorType hypervisor = Hypervisor.HypervisorType.KVM;
        String name = "TestTemplate";
        Long storeId = 123L;
        VMTemplateVO templateVO = new VMTemplateVO();
        templateVO.setArch(CPU.CPUArch.x86);
        TemplateDataStoreVO templateDataStoreVO = new TemplateDataStoreVO();
        String filePath = "/dummy/path";
        doNothing().when(systemVmTemplateRegistration).validateTemplateFileForHypervisorAndArch(hypervisor, templateVO.getArch());
        doNothing().when(systemVmTemplateRegistration).registerTemplate(hypervisor, name, storeId, templateVO, templateDataStoreVO, filePath);
        systemVmTemplateRegistration.validateAndRegisterTemplate(hypervisor, name, storeId, templateVO, templateDataStoreVO, filePath);
        verify(systemVmTemplateRegistration).validateTemplateFileForHypervisorAndArch(eq(hypervisor), eq(templateVO.getArch()));
        verify(systemVmTemplateRegistration).registerTemplate(eq(hypervisor), eq(name), eq(storeId), eq(templateVO), eq(templateDataStoreVO), eq(filePath));
    }

    @Test
    public void testValidateAndRegisterTemplateForNonExistingEntries() {
        Hypervisor.HypervisorType hypervisor = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        String name = "TestTemplateNonExisting";
        Pair<String, Long> storeUrlAndId = new Pair<>("nfs://dummy", 456L);
        String filePath = "/dummy/path/nonexisting";
        doNothing().when(systemVmTemplateRegistration).validateTemplateFileForHypervisorAndArch(hypervisor, arch);
        doNothing().when(systemVmTemplateRegistration).registerTemplateForNonExistingEntries(hypervisor, arch, name, storeUrlAndId, filePath);
        systemVmTemplateRegistration.validateAndRegisterTemplateForNonExistingEntries(hypervisor, arch, name, storeUrlAndId, filePath);
        verify(systemVmTemplateRegistration).validateTemplateFileForHypervisorAndArch(eq(hypervisor), eq(arch));
        verify(systemVmTemplateRegistration).registerTemplateForNonExistingEntries(eq(hypervisor), eq(arch), eq(name), eq(storeUrlAndId), eq(filePath));
    }

    @Test
    public void testGetTemplateFile_fileExists() throws Exception {
        File tempFile = File.createTempFile("template", ".qcow2");
        tempFile.deleteOnExit();
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(details.getDefaultFilePath()).thenReturn(tempFile.getAbsolutePath());
        File result = systemVmTemplateRegistration.getTemplateFile(details);
        assertNotNull(result);
        assertEquals(tempFile.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    public void testGetTemplateFile_fileDoesNotExist_downloadFails() {
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                new SystemVmTemplateRegistration.MetadataTemplateDetails(Hypervisor.HypervisorType.KVM,
                        "name", "nonexistent.qcow2", "http://example.com/file.qcow2",
                        "", CPU.CPUArch.arm64);
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
             MockedStatic<HttpUtils> httpMock = Mockito.mockStatic(HttpUtils.class)) {
            filesMock.when(() -> Files.isWritable(any(Path.class))).thenReturn(true);
            httpMock.when(() -> HttpUtils.downloadFileWithProgress(eq(details.getUrl()), anyString(), any()))
                    .thenReturn(false);
            File result = systemVmTemplateRegistration.getTemplateFile(details);
            assertNull(result);
        }
    }

    @Test
    public void testGetTemplateFile_fileDoesNotExist_downloadSucceeds() {
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                new SystemVmTemplateRegistration.MetadataTemplateDetails(Hypervisor.HypervisorType.KVM,
                        "name", "file.qcow2", "http://example.com/file.qcow2",
                        "", CPU.CPUArch.arm64);
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
             MockedStatic<HttpUtils> httpMock = Mockito.mockStatic(HttpUtils.class)) {
            filesMock.when(() -> Files.isWritable(any(Path.class))).thenReturn(false);
            File expectedFile = new File(systemVmTemplateRegistration.getTempDownloadDir(), details.getFilename());
            httpMock.when(() -> HttpUtils.downloadFileWithProgress(eq(details.getUrl()), eq(expectedFile.getAbsolutePath()), any()))
                    .thenReturn(true);
            File result = systemVmTemplateRegistration.getTemplateFile(details);
            assertNotNull(result);
            assertEquals(expectedFile.getAbsolutePath(), result.getAbsolutePath());
            assertEquals(expectedFile.getAbsolutePath(), details.getDownloadedFilePath());
        }
    }

    @Test
    public void testIsTemplateFileChecksumDifferent_noMismatch() {
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(details.getChecksum()).thenReturn("dummyChecksum");
        File file = new File("dummy.txt");
        try (MockedStatic<DigestHelper> digestMock = Mockito.mockStatic(DigestHelper.class)) {
            digestMock.when(() -> DigestHelper.calculateChecksum(file)).thenReturn("dummyChecksum");
            boolean result = systemVmTemplateRegistration.isTemplateFileChecksumDifferent(details, file);
            assertFalse(result);
        }
    }

    @Test
    public void testIsTemplateFileChecksumDifferent_mismatch() {
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(details.getChecksum()).thenReturn("expectedChecksum");
        File file = new File("dummy.txt");
        try (MockedStatic<DigestHelper> digestMock = Mockito.mockStatic(DigestHelper.class)) {
            digestMock.when(() -> DigestHelper.calculateChecksum(file)).thenReturn("actualChecksum");
            boolean result = systemVmTemplateRegistration.isTemplateFileChecksumDifferent(details, file);
            assertTrue(result);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testValidateTemplates_metadataTemplateFailure() {
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        list.add(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64));
        systemVmTemplateRegistration.validateTemplates(list);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testValidateTemplates_fileFailure() {
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        list.add(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64));

        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        SystemVmTemplateRegistration.NewTemplateMap.put(SystemVmTemplateRegistration.getHypervisorArchKey(
                Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64), details);
        File mockFile = Mockito.mock(File.class);
        doReturn(mockFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        doReturn(true).when(systemVmTemplateRegistration).isTemplateFileChecksumDifferent(details, mockFile);
        systemVmTemplateRegistration.validateTemplates(list);
    }

    public void testValidateTemplates_downloadableFileNotFound() {
        CPU.CPUArch arch = SystemVmTemplateRegistration.DOWNLOADABLE_TEMPLATE_ARCH_TYPES.get(0);
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        list.add(new Pair<>(Hypervisor.HypervisorType.KVM, arch));
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        SystemVmTemplateRegistration.NewTemplateMap.put(SystemVmTemplateRegistration.getHypervisorArchKey(
                Hypervisor.HypervisorType.KVM, arch), details);
        doReturn(null).when(systemVmTemplateRegistration).getTemplateFile(details);
        systemVmTemplateRegistration.validateTemplates(list);
    }

    @Test
    public void testValidateTemplates_success() {
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        list.add(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64));

        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        SystemVmTemplateRegistration.NewTemplateMap.put(SystemVmTemplateRegistration.getHypervisorArchKey(
                Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64), details);
        File mockFile = Mockito.mock(File.class);
        doReturn(mockFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        doReturn(false).when(systemVmTemplateRegistration).isTemplateFileChecksumDifferent(details, mockFile);
        systemVmTemplateRegistration.validateTemplates(list);
    }

    @Test
    public void testRegisterTemplatesForZone() {
        long zoneId = 1L;
        String filePath = "dummyFilePath";
        String nfsVersion = "nfs3";
        Pair<String, Long> storeUrlAndId = new Pair<>("nfs://dummy", 100L);
        doReturn(storeUrlAndId).when(systemVmTemplateRegistration).getNfsStoreInZone(zoneId);
        doReturn(nfsVersion).when(systemVmTemplateRegistration).getNfsVersion(storeUrlAndId.second());
        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic = Mockito.mockStatic(
                SystemVmTemplateRegistration.class)) {
            List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList = new ArrayList<>();
            Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
            CPU.CPUArch arch = CPU.CPUArch.getDefault();
            hypervisorArchList.add(new Pair<>(hypervisorType, arch));
            doReturn(hypervisorArchList).when(clusterDao).listDistinctHypervisorsArchAcrossClusters(zoneId);
            SystemVmTemplateRegistration.MetadataTemplateDetails details =
                    Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
            String name = "existing";
            Mockito.when(details.getArch()).thenReturn(CPU.CPUArch.getDefault());
            Mockito.when(details.getName()).thenReturn(name);
            mockedStatic.when(() -> SystemVmTemplateRegistration.getMetadataTemplateDetails(Mockito.any(),
                    Mockito.any())).thenReturn(details);
            when(systemVmTemplateRegistration.getRegisteredTemplate(name, arch))
                    .thenReturn(null);
            doNothing().when(systemVmTemplateRegistration).registerTemplateForNonExistingEntries(
                    hypervisorType, arch,
                    name, storeUrlAndId, filePath);
            systemVmTemplateRegistration.registerTemplatesForZone(zoneId, filePath);
            mockedStatic.verify(() -> SystemVmTemplateRegistration.mountStore(storeUrlAndId.first(), filePath,
                    nfsVersion));
            verify(systemVmTemplateRegistration).registerTemplateForNonExistingEntries(hypervisorType,
                    arch, name, storeUrlAndId, filePath);
        }
    }
}
