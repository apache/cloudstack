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

import static com.cloud.upgrade.SystemVmTemplateRegistration.DEFAULT_SYSTEM_VM_GUEST_OS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.cpu.CPU;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class SystemVmTemplateRegistrationTest {

    @Mock
    ClusterDao clusterDao;

    @Mock
    VMTemplateDao vmTemplateDao;

    @Mock
    GuestOSDao guestOSDao;

    @Mock
    TemplateDataStoreDao templateDataStoreDao;

    @Mock
    ConfigurationDao configurationDao;

    @Mock
    DataCenterDao dataCenterDao;

    @Mock
    DataCenterDetailsDao dataCenterDetailsDao;

    @Mock
    VMTemplateZoneDao vmTemplateZoneDao;

    @Mock
    ImageStoreDao imageStoreDao;

    @Mock
    ImageStoreDetailsDao imageStoreDetailsDao;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Spy
    @InjectMocks
    SystemVmTemplateRegistration systemVmTemplateRegistration = new SystemVmTemplateRegistration();

    @Before
    public void setup() {
        SystemVmTemplateRegistration.METADATA_TEMPLATE_LIST.clear();
    }

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
            assertTrue(exception.getMessage().contains("Failed to parse system VM Template metadata file"));
        }
    }

    @Test
    public void test_parseMetadataFile_invalidContent() {
        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class, Mockito.CALLS_REAL_METHODS)) {
            setupMetadataFile(mockedStatic, "abc");
            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class,
                    SystemVmTemplateRegistration::parseMetadataFile);
            assertTrue(exception.getMessage().contains("Failed to parse system VM Template metadata file"));
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
        assertNull(SystemVmTemplateRegistration.getMetadataTemplateDetails(Hypervisor.HypervisorType.XenServer,
                CPU.CPUArch.getDefault()));
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                SystemVmTemplateRegistration.getMetadataTemplateDetails(Hypervisor.HypervisorType.KVM,
                        CPU.CPUArch.amd64);
        assertNotNull(templateDetails);
        assertEquals(CPU.CPUArch.amd64, templateDetails.getArch());
        assertEquals(Hypervisor.HypervisorType.KVM, templateDetails.getHypervisorType());
        templateDetails =
                SystemVmTemplateRegistration.getMetadataTemplateDetails(Hypervisor.HypervisorType.KVM,
                        CPU.CPUArch.arm64);
        assertNotNull(templateDetails);
        assertEquals(CPU.CPUArch.arm64, templateDetails.getArch());
        assertEquals(Hypervisor.HypervisorType.KVM, templateDetails.getHypervisorType());
        templateDetails =
                SystemVmTemplateRegistration.getMetadataTemplateDetails(Hypervisor.HypervisorType.VMware,
                        CPU.CPUArch.getDefault());
        assertNotNull(templateDetails);
        assertEquals(CPU.CPUArch.getDefault(), templateDetails.getArch());
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
                        "name", "file", "url", "checksum", CPU.CPUArch.amd64, "guestos");
        SystemVmTemplateRegistration.METADATA_TEMPLATE_LIST.add(details);
        doReturn(null).when(systemVmTemplateRegistration).getTemplateFile(details);
        try {
            systemVmTemplateRegistration.getValidatedTemplateDetailsForHypervisorAndArch(details.getHypervisorType(),
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
                        "name", "file", "url", "checksum", CPU.CPUArch.amd64, "guestos");
        File dummyFile = new File("dummy.txt");
        SystemVmTemplateRegistration.METADATA_TEMPLATE_LIST.add(details);
        doReturn(dummyFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        try (MockedStatic<DigestHelper> digestMock = Mockito.mockStatic(DigestHelper.class)) {
            digestMock.when(() -> DigestHelper.calculateChecksum(dummyFile)).thenReturn("differentChecksum");
            systemVmTemplateRegistration.getValidatedTemplateDetailsForHypervisorAndArch(details.getHypervisorType(),
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
                        "name", "file", "url", "checksum", CPU.CPUArch.amd64, "guestos");
        File dummyFile = new File("dummy.txt");
        SystemVmTemplateRegistration.METADATA_TEMPLATE_LIST.add(details);
        doReturn(dummyFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        try (MockedStatic<DigestHelper> digestMock = Mockito.mockStatic(DigestHelper.class)) {
            digestMock.when(() -> DigestHelper.calculateChecksum(dummyFile)).thenReturn("checksum");
            systemVmTemplateRegistration.getValidatedTemplateDetailsForHypervisorAndArch(details.getHypervisorType(),
                    details.getArch());
        }
    }

    @Test
    public void testValidateAndAddExistingTemplateToStore() {
        long zoneId = 1L;
        Hypervisor.HypervisorType hypervisor = Hypervisor.HypervisorType.KVM;
        VMTemplateVO templateVO = new VMTemplateVO();
        templateVO.setHypervisorType(hypervisor);
        templateVO.setArch(CPU.CPUArch.getDefault());
        TemplateDataStoreVO templateDataStoreVO = new TemplateDataStoreVO();
        Long storeId = 123L;
        String filePath = "/dummy/path";
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        doReturn(details).when(systemVmTemplateRegistration)
                .getValidatedTemplateDetailsForHypervisorAndArch(hypervisor, templateVO.getArch());
        doNothing().when(systemVmTemplateRegistration).addExistingTemplateToStore(templateVO, details,
                templateDataStoreVO, zoneId, storeId, filePath);
        systemVmTemplateRegistration.validateAndAddTemplateToStore(templateVO, templateDataStoreVO, zoneId, storeId,
                filePath);
        verify(systemVmTemplateRegistration)
                .getValidatedTemplateDetailsForHypervisorAndArch(hypervisor, templateVO.getArch());
        verify(systemVmTemplateRegistration).addExistingTemplateToStore(templateVO, details, templateDataStoreVO,
                zoneId, storeId, filePath);
    }

    @Test
    public void testValidateAndAddExistingTemplateToStoreForNonExistingEntries() {
        long zoneId = 1L;
        Hypervisor.HypervisorType hypervisor = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        String name = "TestTemplateNonExisting";
        long storeId = 123L;
        String filePath = "/dummy/path/nonexisting";
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        doReturn(details).when(systemVmTemplateRegistration)
                .getValidatedTemplateDetailsForHypervisorAndArch(hypervisor, arch);
        doNothing().when(systemVmTemplateRegistration).registerNewTemplate(name, details, zoneId, storeId, filePath);
        systemVmTemplateRegistration.validateAndRegisterNewTemplate(hypervisor, arch, name, zoneId, storeId, filePath);
        verify(systemVmTemplateRegistration).getValidatedTemplateDetailsForHypervisorAndArch(hypervisor, arch);
        verify(systemVmTemplateRegistration).registerNewTemplate(name, details, zoneId, storeId, filePath);
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
                        "", CPU.CPUArch.arm64, "guestos");
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
                        "", CPU.CPUArch.arm64, "guestos");
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
    public void testValidateTemplates_metadataTemplateSkip() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.VMware;
        CPU.CPUArch arch = CPU.CPUArch.arm64;
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        list.add(new Pair<>(hypervisorType, arch));
        systemVmTemplateRegistration.validateTemplates(list);
        verify(systemVmTemplateRegistration, never()).getValidatedTemplateDetailsForHypervisorAndArch(hypervisorType,
                arch);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testValidateTemplates_fileFailure() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        list.add(new Pair<>(hypervisorType, arch));
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(details.getHypervisorType()).thenReturn(hypervisorType);
        when(details.getArch()).thenReturn(arch);
        File mockFile = Mockito.mock(File.class);
        when(details.isFileChecksumDifferent(mockFile)).thenReturn(true);
        SystemVmTemplateRegistration.METADATA_TEMPLATE_LIST.add(details);
        doReturn(mockFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        systemVmTemplateRegistration.validateTemplates(list);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testValidateTemplates_downloadableFileNotFound() {
        CPU.CPUArch arch = SystemVmTemplateRegistration.DOWNLOADABLE_TEMPLATE_ARCH_TYPES.get(0);
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        list.add(new Pair<>(hypervisorType, arch));
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(details.getHypervisorType()).thenReturn(hypervisorType);
        when(details.getArch()).thenReturn(arch);
        SystemVmTemplateRegistration.METADATA_TEMPLATE_LIST.add(details);
        doReturn(null).when(systemVmTemplateRegistration).getTemplateFile(details);
        systemVmTemplateRegistration.validateTemplates(list);
    }

    @Test
    public void testValidateTemplates_success() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> list = new ArrayList<>();
        list.add(new Pair<>(hypervisorType, arch));
        SystemVmTemplateRegistration.MetadataTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(details.getHypervisorType()).thenReturn(hypervisorType);
        when(details.getArch()).thenReturn(arch);
        File mockFile = Mockito.mock(File.class);
        when(details.isFileChecksumDifferent(mockFile)).thenReturn(false);
        SystemVmTemplateRegistration.METADATA_TEMPLATE_LIST.add(details);
        doReturn(mockFile).when(systemVmTemplateRegistration).getTemplateFile(details);
        systemVmTemplateRegistration.validateTemplates(list);
    }

    @Test
    public void testAddExistingTemplatesForZoneToStore() {
        long zoneId = 1L;
        String filePath = "dummyFilePath";
        String nfsVersion = "nfs3";
        Pair<String, Long> storeUrlAndId = new Pair<>("nfs://dummy", 100L);
        String name = "existing";
        String url = "url";
        doReturn(storeUrlAndId).when(systemVmTemplateRegistration).getNfsStoreInZone(zoneId);
        doReturn(nfsVersion).when(systemVmTemplateRegistration).getNfsVersion(storeUrlAndId.second());
        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic = Mockito.mockStatic(
                SystemVmTemplateRegistration.class)) {
            List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList = new ArrayList<>();
            Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
            CPU.CPUArch arch = CPU.CPUArch.getDefault();
            hypervisorArchList.add(new Pair<>(hypervisorType, arch));
            doReturn(hypervisorArchList).when(clusterDao).listDistinctHypervisorsAndArchExcludingExternalType(zoneId);
            SystemVmTemplateRegistration.MetadataTemplateDetails details =
                    Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
            when(details.getArch()).thenReturn(CPU.CPUArch.getDefault());
            when(details.getName()).thenReturn(name);
            when(details.getUrl()).thenReturn(url);
            mockedStatic.when(() -> SystemVmTemplateRegistration.getMetadataTemplateDetails(Mockito.any(),
                    Mockito.any())).thenReturn(details);
            doNothing().when(systemVmTemplateRegistration).registerNewTemplate(name, details, zoneId,
                    storeUrlAndId.second(), filePath);
            systemVmTemplateRegistration.registerTemplatesForZone(zoneId, filePath);
            mockedStatic.verify(() -> SystemVmTemplateRegistration.mountStore(storeUrlAndId.first(), filePath,
                    nfsVersion));
            verify(systemVmTemplateRegistration).registerNewTemplate(name, details, zoneId,
                    storeUrlAndId.second(), filePath);
        }
    }

    @Test
    public void updateOrRegisterSystemVmTemplate_UpdatesRegisteredTemplate() {
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getName()).thenReturn("templateName");
        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getArch()).thenReturn(CPU.CPUArch.amd64);
        when(templateDetails.getUrl()).thenReturn("http://example.com/template");
        VMTemplateVO registeredTemplate = Mockito.mock(VMTemplateVO.class);
        when(registeredTemplate.getId()).thenReturn(1L);
        doReturn(registeredTemplate).when(systemVmTemplateRegistration).getRegisteredTemplate(
                "templateName", Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64, "http://example.com/template");
        doNothing().when(systemVmTemplateRegistration).updateRegisteredTemplateDetails(1L, templateDetails,
                null);

        boolean result = systemVmTemplateRegistration.updateOrRegisterSystemVmTemplate(templateDetails,
                new ArrayList<>());

        assertFalse(result);
        verify(systemVmTemplateRegistration).updateRegisteredTemplateDetails(1L, templateDetails, null);
    }

    @Test
    public void updateOrRegisterSystemVmTemplate_SkipsUnusedHypervisorArch() {
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getName()).thenReturn("templateName");
        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getArch()).thenReturn(CPU.CPUArch.amd64);
        when(templateDetails.getUrl()).thenReturn("http://example.com/template");
        doReturn(null).when(systemVmTemplateRegistration).getRegisteredTemplate(
                "templateName", Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64, "http://example.com/template");
        doReturn(null).when(vmTemplateDao).findLatestTemplateByTypeAndHypervisorAndArch(
                Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64, Storage.TemplateType.SYSTEM);

        boolean result = systemVmTemplateRegistration.updateOrRegisterSystemVmTemplate(templateDetails, new ArrayList<>());

        assertFalse(result);
        verify(systemVmTemplateRegistration, never()).registerTemplates(anyList());
    }

    @Test
    public void updateOrRegisterSystemVmTemplate_RegistersNewTemplate() {
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getName()).thenReturn("templateName");
        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getArch()).thenReturn(CPU.CPUArch.amd64);
        when(templateDetails.getUrl()).thenReturn("http://example.com/template");
        doReturn(null).when(systemVmTemplateRegistration).getRegisteredTemplate(
                "templateName", Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64, "http://example.com/template");
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsInUse = new ArrayList<>();
        hypervisorsInUse.add(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64));
        doNothing().when(systemVmTemplateRegistration).registerTemplates(hypervisorsInUse);

        boolean result = systemVmTemplateRegistration.updateOrRegisterSystemVmTemplate(templateDetails, hypervisorsInUse);

        assertTrue(result);
        verify(systemVmTemplateRegistration).registerTemplates(eq(hypervisorsInUse));
    }

    @Test
    public void updateOrRegisterSystemVmTemplate_ThrowsExceptionOnRegistrationFailure() {
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getName()).thenReturn("templateName");
        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getArch()).thenReturn(CPU.CPUArch.amd64);
        when(templateDetails.getUrl()).thenReturn("http://example.com/template");
        doReturn(null).when(systemVmTemplateRegistration).getRegisteredTemplate(
                "templateName", Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64, "http://example.com/template");
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsInUse = new ArrayList<>();
        hypervisorsInUse.add(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64));
        doThrow(new CloudRuntimeException("Registration failed")).when(systemVmTemplateRegistration).registerTemplates(hypervisorsInUse);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class,
                () -> systemVmTemplateRegistration.updateOrRegisterSystemVmTemplate(templateDetails, hypervisorsInUse));

        assertTrue(exception.getMessage().contains("Failed to register"));
    }

    @Test
    public void updateRegisteredTemplateDetails_UpdatesTemplateSuccessfully() {
        Long templateId = 1L;
        Long zoneId = 2L;
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        GuestOSVO guestOS = Mockito.mock(GuestOSVO.class);

        when(templateDetails.getGuestOs()).thenReturn("Debian");
        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getName()).thenReturn("templateName");
        when(vmTemplateDao.findById(templateId)).thenReturn(templateVO);
        when(guestOSDao.findOneByDisplayName("Debian")).thenReturn(guestOS);
        when(guestOS.getId()).thenReturn(10L);
        when(vmTemplateDao.update(templateVO.getId(), templateVO)).thenReturn(true);
        doNothing().when(systemVmTemplateRegistration).updateSystemVMEntries(templateId, Hypervisor.HypervisorType.KVM);
        doNothing().when(systemVmTemplateRegistration).updateConfigurationParams(Hypervisor.HypervisorType.KVM,
                "templateName", zoneId);

        systemVmTemplateRegistration.updateRegisteredTemplateDetails(templateId, templateDetails, zoneId);

        verify(templateVO).setTemplateType(Storage.TemplateType.SYSTEM);
        verify(templateVO).setGuestOSId(10);
        verify(vmTemplateDao).update(templateVO.getId(), templateVO);
        verify(systemVmTemplateRegistration).updateSystemVMEntries(templateId, Hypervisor.HypervisorType.KVM);
        verify(systemVmTemplateRegistration).updateConfigurationParams(Hypervisor.HypervisorType.KVM,
                "templateName", zoneId);
    }

    @Test
    public void updateRegisteredTemplateDetails_ThrowsExceptionWhenUpdateFails() {
        Long templateId = 1L;
        Long zoneId = 2L;
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);

        when(templateDetails.getGuestOs()).thenReturn("Debian");
        when(vmTemplateDao.findById(templateId)).thenReturn(templateVO);
        when(vmTemplateDao.update(templateVO.getId(), templateVO)).thenReturn(false);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class,
                () -> systemVmTemplateRegistration.updateRegisteredTemplateDetails(templateId, templateDetails, zoneId));

        assertTrue(exception.getMessage().contains("Exception while updating template with id"));
        verify(systemVmTemplateRegistration, never()).updateSystemVMEntries(anyLong(), any());
        verify(systemVmTemplateRegistration, never()).updateConfigurationParams(any(), any(), any());
    }

    @Test
    public void updateRegisteredTemplateDetails_SkipsGuestOSUpdateWhenNotFound() {
        Long templateId = 1L;
        Long zoneId = 2L;
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);

        when(templateDetails.getGuestOs()).thenReturn("NonExistentOS");
        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getName()).thenReturn("templateName");
        when(vmTemplateDao.findById(templateId)).thenReturn(templateVO);
        when(guestOSDao.findOneByDisplayName("NonExistentOS")).thenReturn(null);
        when(vmTemplateDao.update(templateVO.getId(), templateVO)).thenReturn(true);
        doNothing().when(systemVmTemplateRegistration).updateSystemVMEntries(templateId, Hypervisor.HypervisorType.KVM);
        doNothing().when(systemVmTemplateRegistration).updateConfigurationParams(Hypervisor.HypervisorType.KVM,
                "templateName", zoneId);

        systemVmTemplateRegistration.updateRegisteredTemplateDetails(templateId, templateDetails, zoneId);

        verify(templateVO, never()).setGuestOSId(anyInt());
        verify(vmTemplateDao).update(templateVO.getId(), templateVO);
        verify(systemVmTemplateRegistration).updateSystemVMEntries(templateId, Hypervisor.HypervisorType.KVM);
        verify(systemVmTemplateRegistration).updateConfigurationParams(Hypervisor.HypervisorType.KVM,
                "templateName", zoneId);
    }

    @Test
    public void registerTemplatesForZone_SuccessfullyRegistersNewTemplate() {
        long zoneId = 1L;
        String storeMountPath = "/mnt/nfs";
        Pair<String, Long> storeUrlAndId = new Pair<>("nfs://dummy", 100L);
        String nfsVersion = "nfs3";
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList = new ArrayList<>();
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        hypervisorArchList.add(new Pair<>(hypervisorType, arch));
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getHypervisorType()).thenReturn(hypervisorType);
        when(templateDetails.getArch()).thenReturn(arch);
        String name = "TestTemplate";
        String url = "http://example.com/template";
        when(templateDetails.getName()).thenReturn(name);
        when(templateDetails.getUrl()).thenReturn(url);
        doReturn(storeUrlAndId).when(systemVmTemplateRegistration).getNfsStoreInZone(zoneId);
        doReturn(nfsVersion).when(systemVmTemplateRegistration).getNfsVersion(storeUrlAndId.second());
        doReturn(null).when(systemVmTemplateRegistration).getRegisteredTemplate(
                name, hypervisorType, arch, url);
        doNothing().when(systemVmTemplateRegistration).registerNewTemplate(
                name, templateDetails, zoneId, storeUrlAndId.second(), storeMountPath);
        doReturn(hypervisorArchList).when(clusterDao).listDistinctHypervisorsAndArchExcludingExternalType(zoneId);
        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            mockedStatic.when(() -> SystemVmTemplateRegistration.getMetadataTemplateDetails(
                    hypervisorType, arch)).thenReturn(templateDetails);

            systemVmTemplateRegistration.registerTemplatesForZone(zoneId, storeMountPath);

            mockedStatic.verify(() -> SystemVmTemplateRegistration.mountStore(
                    eq(storeUrlAndId.first()), eq(storeMountPath), eq(nfsVersion)), times(1));
            verify(systemVmTemplateRegistration).registerNewTemplate(
                    templateDetails.getName(), templateDetails, zoneId, storeUrlAndId.second(), storeMountPath);
        }
    }

    @Test
    public void registerTemplatesForZone_SkipsWhenTemplateDetailsNotFound() {
        long zoneId = 1L;
        String storeMountPath = "/mnt/nfs";
        Pair<String, Long> storeUrlAndId = new Pair<>("nfs://dummy", 100L);
        String nfsVersion = "nfs3";
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList = new ArrayList<>();
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        hypervisorArchList.add(new Pair<>(hypervisorType, arch));
        doReturn(storeUrlAndId).when(systemVmTemplateRegistration).getNfsStoreInZone(zoneId);
        doReturn(nfsVersion).when(systemVmTemplateRegistration).getNfsVersion(storeUrlAndId.second());
        doReturn(hypervisorArchList).when(clusterDao).listDistinctHypervisorsAndArchExcludingExternalType(zoneId);

        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            mockedStatic.when(() -> SystemVmTemplateRegistration.getMetadataTemplateDetails(
                    hypervisorType, arch)).thenReturn(null);

            systemVmTemplateRegistration.registerTemplatesForZone(zoneId, storeMountPath);

            mockedStatic.verify(() -> SystemVmTemplateRegistration.mountStore(
                    eq(storeUrlAndId.first()), eq(storeMountPath), eq(nfsVersion)), times(1));
            verify(systemVmTemplateRegistration, never()).registerNewTemplate(any(), any(), anyLong(), anyLong(), any());
        }
    }

    @Test
    public void registerTemplatesForZone_AddsExistingTemplateToStore() {
        long zoneId = 1L;
        String storeMountPath = "/mnt/nfs";
        Pair<String, Long> storeUrlAndId = new Pair<>("nfs://dummy", 100L);
        String nfsVersion = "nfs3";
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList = new ArrayList<>();
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        hypervisorArchList.add(new Pair<>(hypervisorType, arch));
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getHypervisorType()).thenReturn(hypervisorType);
        when(templateDetails.getArch()).thenReturn(arch);
        String name = "TestTemplate";
        String url = "http://example.com/template";
        when(templateDetails.getName()).thenReturn(name);
        when(templateDetails.getUrl()).thenReturn(url);
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        long templateId = 100L;
        when(templateVO.getId()).thenReturn(templateId);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        String installPath = "/template/install/path";
        when(templateDataStoreVO.getInstallPath()).thenReturn(installPath);

        doReturn(storeUrlAndId).when(systemVmTemplateRegistration).getNfsStoreInZone(zoneId);
        doReturn(nfsVersion).when(systemVmTemplateRegistration).getNfsVersion(storeUrlAndId.second());
        doReturn(hypervisorArchList).when(clusterDao).listDistinctHypervisorsAndArchExcludingExternalType(zoneId);
        doReturn(templateVO).when(systemVmTemplateRegistration).getRegisteredTemplate(name, hypervisorType, arch, url);
        doReturn(templateDataStoreVO).when(templateDataStoreDao)
                .findByStoreTemplate(storeUrlAndId.second(), templateId);
        doReturn(false).when(systemVmTemplateRegistration).validateIfSeeded(
                templateDataStoreVO, storeUrlAndId.first(), installPath, nfsVersion);
        doNothing().when(systemVmTemplateRegistration).addExistingTemplateToStore(
                templateVO, templateDetails, templateDataStoreVO, zoneId, storeUrlAndId.second(), storeMountPath);
        doNothing().when(systemVmTemplateRegistration).updateRegisteredTemplateDetails(
                templateId, templateDetails, zoneId);

        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            mockedStatic.when(() -> SystemVmTemplateRegistration.getMetadataTemplateDetails(
                    hypervisorType, arch)).thenReturn(templateDetails);

            systemVmTemplateRegistration.registerTemplatesForZone(zoneId, storeMountPath);

            verify(systemVmTemplateRegistration).addExistingTemplateToStore(
                    templateVO, templateDetails, templateDataStoreVO, zoneId, storeUrlAndId.second(), storeMountPath);
            verify(systemVmTemplateRegistration).updateRegisteredTemplateDetails(templateId, templateDetails, zoneId);
        }
    }

    @Test
    public void performTemplateRegistrationOperations_CreatesNewTemplateWhenNotExists() {
        String name = "TestTemplate";
        String url = "http://example.com/template";
        String checksum = "abc123";
        Storage.ImageFormat format = Storage.ImageFormat.QCOW2;
        long guestOsId = 1L;
        Long storeId = 100L;
        Long templateId = null;
        String filePath = "/mnt/nfs";
        TemplateDataStoreVO templateDataStoreVO = null;
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);

        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getArch()).thenReturn(CPU.CPUArch.amd64);
        doReturn(new VMTemplateVO()).when(vmTemplateDao).persist(any());
        doNothing().when(systemVmTemplateRegistration).createCrossZonesTemplateZoneRefEntries(anyLong());
        doNothing().when(systemVmTemplateRegistration).createTemplateStoreRefEntry(any());
        doNothing().when(systemVmTemplateRegistration).setupTemplateOnStore(anyString(), any(), anyString());
        doNothing().when(systemVmTemplateRegistration).readTemplateProperties(anyString(), any());
        doNothing().when(systemVmTemplateRegistration).updateTemplateDetails(any());

        Long result = systemVmTemplateRegistration.performTemplateRegistrationOperations(name, templateDetails, url, checksum, format, guestOsId, storeId, templateId, filePath, templateDataStoreVO);

        assertNotNull(result);
        verify(vmTemplateDao).persist(any());
        verify(systemVmTemplateRegistration).createCrossZonesTemplateZoneRefEntries(anyLong());
        verify(systemVmTemplateRegistration).createTemplateStoreRefEntry(any());
        verify(systemVmTemplateRegistration).setupTemplateOnStore(anyString(), any(), anyString());
        verify(systemVmTemplateRegistration).updateTemplateDetails(any());
    }

    @Test
    public void performTemplateRegistrationOperations_UpdatesExistingTemplate() {
        String name = "TestTemplate";
        String url = "http://example.com/template";
        String checksum = "abc123";
        Storage.ImageFormat format = Storage.ImageFormat.QCOW2;
        long guestOsId = 1L;
        Long storeId = 100L;
        Long templateId = 1L;
        String filePath = "/mnt/nfs";
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);

        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getArch()).thenReturn(CPU.CPUArch.amd64);
        doNothing().when(systemVmTemplateRegistration).createCrossZonesTemplateZoneRefEntries(anyLong());
        doNothing().when(systemVmTemplateRegistration).setupTemplateOnStore(anyString(), any(), anyString());
        doNothing().when(systemVmTemplateRegistration).readTemplateProperties(anyString(), any());
        doNothing().when(systemVmTemplateRegistration).updateTemplateDetails(any());

        Long result = systemVmTemplateRegistration.performTemplateRegistrationOperations(name, templateDetails, url, checksum, format, guestOsId, storeId, templateId, filePath, templateDataStoreVO);

        assertNotNull(result);
        assertEquals(templateId, result);
        verify(vmTemplateDao, never()).persist(any());
        verify(systemVmTemplateRegistration).createCrossZonesTemplateZoneRefEntries(anyLong());
        verify(systemVmTemplateRegistration, never()).createTemplateStoreRefEntry(any());
        verify(systemVmTemplateRegistration).setupTemplateOnStore(anyString(), any(), anyString());
        verify(systemVmTemplateRegistration).updateTemplateDetails(any());
    }

    @Test
    public void performTemplateRegistrationOperations_ThrowsExceptionWhenTemplateCreationFails() {
        String name = "TestTemplate";
        String url = "http://example.com/template";
        String checksum = "abc123";
        Storage.ImageFormat format = Storage.ImageFormat.QCOW2;
        long guestOsId = 1L;
        Long storeId = 100L;
        Long templateId = null;
        String filePath = "/mnt/nfs";
        TemplateDataStoreVO templateDataStoreVO = null;
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);

        when(templateDetails.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateDetails.getArch()).thenReturn(CPU.CPUArch.amd64);
        doReturn(null).when(vmTemplateDao).persist(any());

        assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.performTemplateRegistrationOperations(name, templateDetails, url, checksum, format, guestOsId, storeId, templateId, filePath, templateDataStoreVO);
        });

        verify(vmTemplateDao).persist(any());
        verify(systemVmTemplateRegistration, never()).createCrossZonesTemplateZoneRefEntries(anyLong());
        verify(systemVmTemplateRegistration, never()).createTemplateStoreRefEntry(any());
        verify(systemVmTemplateRegistration, never()).setupTemplateOnStore(anyString(), any(), anyString());
        verify(systemVmTemplateRegistration, never()).updateTemplateDetails(any());
    }

    @Test
    public void setupTemplateOnStore_ThrowsExceptionWhenScriptNotFound() {
        String templateName = "templateName";
        String destTempFolder = "/tmp/folder";
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);

        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            scriptMock.when(() -> Script.findScript(SystemVmTemplateRegistration.STORAGE_SCRIPTS_DIR,
                    "setup-sysvm-tmplt")).thenReturn(null);

            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
                systemVmTemplateRegistration.setupTemplateOnStore(templateName, templateDetails, destTempFolder);
            });

            assertTrue(exception.getMessage().contains("Unable to find the setup-sysvm-tmplt script"));
        }
    }

    @Test
    public void updateTemplateEntriesOnFailure_RemovesTemplateAndDataStoreEntry() {
        long templateId = 1L;
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);

        when(vmTemplateDao.createForUpdate(templateId)).thenReturn(template);
        when(template.getId()).thenReturn(templateId);
        when(templateDataStoreDao.findByTemplate(templateId, DataStoreRole.Image)).thenReturn(templateDataStoreVO);

        systemVmTemplateRegistration.updateTemplateEntriesOnFailure(templateId);

        verify(vmTemplateDao).update(templateId, template);
        verify(vmTemplateDao).remove(templateId);
        verify(templateDataStoreDao).remove(templateDataStoreVO.getId());
    }

    @Test
    public void updateTemplateEntriesOnFailure_SkipsDataStoreRemovalWhenNotFound() {
        long templateId = 1L;
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);

        when(vmTemplateDao.createForUpdate(templateId)).thenReturn(template);
        when(template.getId()).thenReturn(templateId);
        when(templateDataStoreDao.findByTemplate(templateId, DataStoreRole.Image)).thenReturn(null);

        systemVmTemplateRegistration.updateTemplateEntriesOnFailure(templateId);

        verify(vmTemplateDao).update(templateId, template);
        verify(vmTemplateDao).remove(templateId);
        verify(templateDataStoreDao, never()).remove(anyLong());
    }

    @Test
    public void updateConfigurationParams_UpdatesConfigurationSuccessfully() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        String templateName = "templateName";
        long zoneId = 1L;
        String configName = SystemVmTemplateRegistration.ROUTER_TEMPLATE_CONFIGURATION_NAMES.get(hypervisorType);

        when(configurationDao.update(configName, templateName)).thenReturn(true);
        when(configurationDao.update(SystemVmTemplateRegistration.MINIMUM_SYSTEM_VM_VERSION_KEY,
                systemVmTemplateRegistration.getSystemVmTemplateVersion())).thenReturn(true);

        systemVmTemplateRegistration.updateConfigurationParams(hypervisorType, templateName, zoneId);

        verify(configurationDao).update(configName, templateName);
        verify(dataCenterDetailsDao).removeDetail(zoneId, configName);
        verify(configurationDao).update(SystemVmTemplateRegistration.MINIMUM_SYSTEM_VM_VERSION_KEY,
                systemVmTemplateRegistration.getSystemVmTemplateVersion());
        verify(dataCenterDetailsDao).removeDetail(zoneId, SystemVmTemplateRegistration.MINIMUM_SYSTEM_VM_VERSION_KEY);
    }

    @Test
    public void updateConfigurationParams_ThrowsExceptionWhenConfigUpdateFails() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        String templateName = "templateName";
        Long zoneId = 1L;
        String configName = SystemVmTemplateRegistration.ROUTER_TEMPLATE_CONFIGURATION_NAMES.get(hypervisorType);

        when(configurationDao.update(configName, templateName)).thenReturn(false);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.updateConfigurationParams(hypervisorType, templateName, zoneId);
        });

        assertTrue(exception.getMessage().contains("Failed to update configuration parameter"));
        verify(configurationDao).update(configName, templateName);
        verify(dataCenterDetailsDao, never()).removeDetail(anyLong(), anyString());
    }

    @Test
    public void updateConfigurationParams_SkipsZoneDetailsRemovalWhenZoneIdIsNull() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.VMware;
        String templateName = "templateName";
        Long zoneId = null;
        String configName = SystemVmTemplateRegistration.ROUTER_TEMPLATE_CONFIGURATION_NAMES.get(hypervisorType);

        when(configurationDao.update(configName, templateName)).thenReturn(true);
        when(configurationDao.update(SystemVmTemplateRegistration.MINIMUM_SYSTEM_VM_VERSION_KEY,
                systemVmTemplateRegistration.getSystemVmTemplateVersion())).thenReturn(true);

        systemVmTemplateRegistration.updateConfigurationParams(hypervisorType, templateName, zoneId);

        verify(configurationDao).update(configName, templateName);
        verify(dataCenterDetailsDao, never()).removeDetail(anyLong(), eq(configName));
        verify(configurationDao).update(SystemVmTemplateRegistration.MINIMUM_SYSTEM_VM_VERSION_KEY,
                systemVmTemplateRegistration.getSystemVmTemplateVersion());
        verify(dataCenterDetailsDao, never()).removeDetail(anyLong(),
                eq(SystemVmTemplateRegistration.MINIMUM_SYSTEM_VM_VERSION_KEY));
    }

    @Test
    public void createOrUpdateTemplateZoneEntry_CreatesNewEntryWhenNotExists() {
        long zoneId = 1L;
        long templateId = 100L;
        VMTemplateZoneVO newTemplateZoneVO = Mockito.mock(VMTemplateZoneVO.class);

        when(vmTemplateZoneDao.findByZoneTemplate(zoneId, templateId)).thenReturn(null);
        when(vmTemplateZoneDao.persist(any(VMTemplateZoneVO.class))).thenReturn(newTemplateZoneVO);

        VMTemplateZoneVO result = systemVmTemplateRegistration.createOrUpdateTemplateZoneEntry(zoneId, templateId);

        assertNotNull(result);
        verify(vmTemplateZoneDao).persist(any(VMTemplateZoneVO.class));
    }

    @Test
    public void createOrUpdateTemplateZoneEntry_UpdatesExistingEntry() {
        long zoneId = 1L;
        long templateId = 100L;
        VMTemplateZoneVO existingTemplateZoneVO = Mockito.mock(VMTemplateZoneVO.class);

        when(vmTemplateZoneDao.findByZoneTemplate(zoneId, templateId)).thenReturn(existingTemplateZoneVO);
        when(vmTemplateZoneDao.update(existingTemplateZoneVO.getId(), existingTemplateZoneVO)).thenReturn(true);

        VMTemplateZoneVO result = systemVmTemplateRegistration.createOrUpdateTemplateZoneEntry(zoneId, templateId);

        assertNotNull(result);
        verify(vmTemplateZoneDao).update(existingTemplateZoneVO.getId(), existingTemplateZoneVO);
    }

    @Test
    public void createOrUpdateTemplateZoneEntry_ReturnsNullWhenUpdateFails() {
        long zoneId = 1L;
        long templateId = 100L;
        VMTemplateZoneVO existingTemplateZoneVO = Mockito.mock(VMTemplateZoneVO.class);

        when(vmTemplateZoneDao.findByZoneTemplate(zoneId, templateId)).thenReturn(existingTemplateZoneVO);
        when(vmTemplateZoneDao.update(existingTemplateZoneVO.getId(), existingTemplateZoneVO)).thenReturn(false);

        VMTemplateZoneVO result = systemVmTemplateRegistration.createOrUpdateTemplateZoneEntry(zoneId, templateId);

        assertNull(result);
        verify(vmTemplateZoneDao).update(existingTemplateZoneVO.getId(), existingTemplateZoneVO);
    }

    @Test
    public void createCrossZonesTemplateZoneRefEntries_ThrowsExceptionWhenEntryCreationFails() {
        long templateId = 100L;
        DataCenterVO dataCenter = Mockito.mock(DataCenterVO.class);
        List<DataCenterVO> dataCenters = List.of(dataCenter);

        when(dataCenter.getId()).thenReturn(1L);
        when(dataCenterDao.listAll()).thenReturn(dataCenters);
        when(systemVmTemplateRegistration.createOrUpdateTemplateZoneEntry(1L, templateId)).thenReturn(null);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.createCrossZonesTemplateZoneRefEntries(templateId);
        });

        assertTrue(exception.getMessage().contains("Failed to create template-zone record"));
    }

    @Test
    public void createTemplateStoreRefEntry_CreatesEntrySuccessfully() {
        SystemVmTemplateRegistration.SystemVMTemplateDetails details = Mockito.mock(SystemVmTemplateRegistration.SystemVMTemplateDetails.class);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);

        when(details.getStoreId()).thenReturn(1L);
        when(details.getId()).thenReturn(100L);
        when(templateDataStoreDao.persist(any(TemplateDataStoreVO.class))).thenReturn(templateDataStoreVO);

        systemVmTemplateRegistration.createTemplateStoreRefEntry(details);

        verify(templateDataStoreDao).persist(any(TemplateDataStoreVO.class));
    }

    @Test
    public void createTemplateStoreRefEntry_ThrowsExceptionWhenCreationFails() {
        SystemVmTemplateRegistration.SystemVMTemplateDetails details = Mockito.mock(SystemVmTemplateRegistration.SystemVMTemplateDetails.class);

        when(details.getStoreId()).thenReturn(1L);
        when(details.getId()).thenReturn(100L);
        when(templateDataStoreDao.persist(any(TemplateDataStoreVO.class))).thenReturn(null);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.createTemplateStoreRefEntry(details);
        });

        assertTrue(exception.getMessage().contains("Failed to create template-store record"));
    }

    @Test
    public void updateTemplateDetails_UpdatesTemplateAndStoreSuccessfully() {
        SystemVmTemplateRegistration.SystemVMTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.SystemVMTemplateDetails.class);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);

        when(details.getId()).thenReturn(1L);
        when(details.getStoreId()).thenReturn(2L);
        when(details.getSize()).thenReturn(1024L);
        when(details.getPhysicalSize()).thenReturn(2048L);
        when(template.getId()).thenReturn(1L);
        when(vmTemplateDao.findById(1L)).thenReturn(template);
        when(templateDataStoreDao.findByStoreTemplate(2L, 1L)).thenReturn(templateDataStoreVO);
        when(templateDataStoreDao.update(anyLong(), any(TemplateDataStoreVO.class))).thenReturn(true);

        systemVmTemplateRegistration.updateTemplateDetails(details);

        verify(template).setSize(1024L);
        verify(template).setState(VirtualMachineTemplate.State.Active);
        verify(vmTemplateDao).update(template.getId(), template);
        verify(templateDataStoreVO).setSize(1024L);
        verify(templateDataStoreVO).setPhysicalSize(2048L);
        verify(templateDataStoreVO).setDownloadPercent(100);
        verify(templateDataStoreVO).setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        verify(templateDataStoreVO).setLastUpdated(details.getUpdated());
        verify(templateDataStoreVO).setState(ObjectInDataStoreStateMachine.State.Ready);
        verify(templateDataStoreDao).update(templateDataStoreVO.getId(), templateDataStoreVO);
    }

    @Test
    public void updateTemplateDetails_ThrowsExceptionWhenStoreUpdateFails() {
        SystemVmTemplateRegistration.SystemVMTemplateDetails details =
                Mockito.mock(SystemVmTemplateRegistration.SystemVMTemplateDetails.class);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);

        when(details.getId()).thenReturn(1L);
        when(details.getStoreId()).thenReturn(2L);
        when(details.getSize()).thenReturn(1024L);
        when(details.getPhysicalSize()).thenReturn(2048L);
        when(template.getId()).thenReturn(1L);
        when(vmTemplateDao.findById(1L)).thenReturn(template);
        when(templateDataStoreDao.findByStoreTemplate(2L, 1L)).thenReturn(templateDataStoreVO);
        when(templateDataStoreDao.update(anyLong(), any(TemplateDataStoreVO.class))).thenReturn(false);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.updateTemplateDetails(details);
        });

        assertTrue(exception.getMessage().contains("Failed to update template-store record"));
        verify(templateDataStoreDao).update(templateDataStoreVO.getId(), templateDataStoreVO);
    }

    @Test
    public void getNfsStoreInZone_ReturnsStoreDetailsWhenStoreExists() {
        long zoneId = 1L;
        ImageStoreVO storeVO = Mockito.mock(ImageStoreVO.class);

        when(imageStoreDao.findOneByZoneAndProtocol(zoneId, "nfs")).thenReturn(storeVO);
        when(storeVO.getUrl()).thenReturn("nfs://example.com/store");
        when(storeVO.getId()).thenReturn(100L);

        Pair<String, Long> result = systemVmTemplateRegistration.getNfsStoreInZone(zoneId);

        assertNotNull(result);
        assertEquals("nfs://example.com/store", result.first());
        assertEquals(100L, result.second().longValue());
    }

    @Test
    public void getNfsStoreInZone_ThrowsExceptionWhenStoreNotFound() {
        long zoneId = 1L;

        when(imageStoreDao.findOneByZoneAndProtocol(zoneId, "nfs")).thenReturn(null);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.getNfsStoreInZone(zoneId);
        });

        assertTrue(exception.getMessage().contains("Failed to fetch NFS store in zone"));
    }

    @Test
    public void readTemplateProperties_SetsTemplateSizesCorrectly() {
        String path = "/template/path";
        SystemVmTemplateRegistration.SystemVMTemplateDetails details = Mockito.mock(SystemVmTemplateRegistration.SystemVMTemplateDetails.class);
        Pair<Long, Long> templateSizes = new Pair<>(1024L, 2048L);

        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic = Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            mockedStatic.when(() -> SystemVmTemplateRegistration.readTemplatePropertiesSizes(path)).thenReturn(templateSizes);

            systemVmTemplateRegistration.readTemplateProperties(path, details);

            verify(details).setSize(1024L);
            verify(details).setPhysicalSize(2048L);
        }
    }

    @Test
    public void readTemplateProperties_ThrowsExceptionWhenPathIsInvalid() {
        String path = "/invalid/path";
        SystemVmTemplateRegistration.SystemVMTemplateDetails details = Mockito.mock(SystemVmTemplateRegistration.SystemVMTemplateDetails.class);

        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic = Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            mockedStatic.when(() -> SystemVmTemplateRegistration.readTemplatePropertiesSizes(path))
                    .thenThrow(new CloudRuntimeException("Invalid path"));

            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
                systemVmTemplateRegistration.readTemplateProperties(path, details);
            });

            assertTrue(exception.getMessage().contains("Invalid path"));
        }
    }

    @Test
    public void getEligibleZoneIds_ReturnsUniqueZoneIds() {
        ImageStoreVO store1 = Mockito.mock(ImageStoreVO.class);
        ImageStoreVO store2 = Mockito.mock(ImageStoreVO.class);
        ImageStoreVO store3 = Mockito.mock(ImageStoreVO.class);

        when(store1.getDataCenterId()).thenReturn(1L);
        when(store2.getDataCenterId()).thenReturn(2L);
        when(store3.getDataCenterId()).thenReturn(1L);
        when(imageStoreDao.findByProtocol("nfs")).thenReturn(List.of(store1, store2, store3));

        List<Long> result = systemVmTemplateRegistration.getEligibleZoneIds();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
    }

    @Test
    public void getEligibleZoneIds_ReturnsEmptyListWhenNoStoresExist() {
        when(imageStoreDao.findByProtocol("nfs")).thenReturn(new ArrayList<>());

        List<Long> result = systemVmTemplateRegistration.getEligibleZoneIds();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getNfsVersion_ReturnsStoreSpecificVersionWhenPresent() {
        long storeId = 1L;
        String configKey = "secstorage.nfs.version";
        String expectedVersion = "nfs4";
        Map<String, String> storeDetails = Map.of(configKey, expectedVersion);

        when(imageStoreDetailsDao.getDetails(storeId)).thenReturn(storeDetails);

        String result = systemVmTemplateRegistration.getNfsVersion(storeId);

        assertNotNull(result);
        assertEquals(expectedVersion, result);
    }

    @Test
    public void getNfsVersion_ReturnsGlobalVersionWhenStoreSpecificVersionNotPresent() {
        long storeId = 1L;
        String expectedVersion = "nfs3";
        ConfigurationVO globalConfig = Mockito.mock(ConfigurationVO.class);

        when(imageStoreDetailsDao.getDetails(storeId)).thenReturn(null);
        when(configurationDao.findByName(anyString())).thenReturn(globalConfig);
        when(globalConfig.getValue()).thenReturn(expectedVersion);

        String result = systemVmTemplateRegistration.getNfsVersion(storeId);

        assertNotNull(result);
        assertEquals(expectedVersion, result);
    }

    @Test
    public void getNfsVersion_ReturnsNullWhenNoVersionConfigured() {
        long storeId = 1L;

        when(imageStoreDetailsDao.getDetails(storeId)).thenReturn(null);
        when(configurationDao.findByName(anyString())).thenReturn(null);

        String result = systemVmTemplateRegistration.getNfsVersion(storeId);

        assertNull(result);
    }

    @Test
    public void validateIfSeeded_ReturnsTrueWhenTemplateIsSeeded() throws Exception {
        TemplateDataStoreVO templDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        String url = "nfs://example.com/store";
        Path local = Files.createTempDirectory("local");
        Files.createDirectory(local.resolve("template"));
        String path = "/template/path";
        String nfsVersion = "nfs3";
        Path secondary = Files.createTempDirectory("secondary");
        Files.createDirectory(secondary.resolve("template"));
        Files.createFile(secondary.resolve("template").resolve("template.properties"));
        String tempDir = secondary.toString();
        Pair<Long, Long> templateSizes = new Pair<>(1024L, 2048L);

        when(templDataStoreVO.getTemplateId()).thenReturn(1L);
        when(templDataStoreVO.getDataStoreId()).thenReturn(2L);
        doNothing().when(systemVmTemplateRegistration).updateSeededTemplateDetails(anyLong(), anyLong(), anyLong(), anyLong());
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
             MockedStatic<SystemVmTemplateRegistration> mockedStatic = Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            filesMock.when(() -> Files.createTempDirectory(SystemVmTemplateRegistration.TEMPORARY_SECONDARY_STORE)).thenReturn(Path.of(tempDir));
            mockedStatic.when(() -> SystemVmTemplateRegistration.readTemplatePropertiesSizes(anyString())).thenReturn(templateSizes);

            boolean result = systemVmTemplateRegistration.validateIfSeeded(templDataStoreVO, url, path, nfsVersion);

            assertTrue(result);
            verify(systemVmTemplateRegistration).updateSeededTemplateDetails(1L, 2L, 1024L, 2048L);
        }
    }

    @Test
    public void validateIfSeeded_ReturnsFalseWhenTemplateIsNotSeeded() throws Exception {
        TemplateDataStoreVO templDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        String url = "nfs://example.com/store";
        String path = "/template/path";
        String nfsVersion = "nfs3";
        String tempDir = "/tmp/secondary";

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.createTempDirectory(SystemVmTemplateRegistration.TEMPORARY_SECONDARY_STORE)).thenReturn(Path.of(tempDir));

            boolean result = systemVmTemplateRegistration.validateIfSeeded(templDataStoreVO, url, path, nfsVersion);

            assertFalse(result);
            verify(systemVmTemplateRegistration, never()).updateSeededTemplateDetails(anyLong(), anyLong(), anyLong(), anyLong());
        }
    }

    @Test
    public void validateIfSeeded_ThrowsExceptionWhenTempDirectoryCreationFails() {
        TemplateDataStoreVO templDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        String url = "nfs://example.com/store";
        String path = "/template/path";
        String nfsVersion = "nfs3";

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.createTempDirectory(SystemVmTemplateRegistration.TEMPORARY_SECONDARY_STORE)).thenThrow(IOException.class);

            assertThrows(CloudRuntimeException.class, () -> {
                systemVmTemplateRegistration.validateIfSeeded(templDataStoreVO, url, path, nfsVersion);
            });
        }
    }

    @Test
    public void validateIfSeeded_ThrowsExceptionWhenUnmountFails() throws Exception {
        TemplateDataStoreVO templDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        String url = "nfs://example.com/store";
        String path = "/template/path";
        String nfsVersion = "nfs3";
        String tempDir = "/tmp/secondary";

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
             MockedStatic<SystemVmTemplateRegistration> mockedStatic = Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            filesMock.when(() -> Files.createTempDirectory(SystemVmTemplateRegistration.TEMPORARY_SECONDARY_STORE)).thenReturn(Path.of(tempDir));
            mockedStatic.when(() -> SystemVmTemplateRegistration.unmountStore(anyString())).thenThrow(CloudRuntimeException.class);

            assertThrows(CloudRuntimeException.class, () -> {
                systemVmTemplateRegistration.validateIfSeeded(templDataStoreVO, url, path, nfsVersion);
            });
        }
    }

    @Test
    public void registerNewTemplate_RegistersTemplateSuccessfully() {
        String name = "templateName";
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getUrl()).thenReturn("a");
        when(templateDetails.getChecksum()).thenReturn("b");
        long zoneId = 1L;
        Long storeId = 2L;
        String filePath = "/tmp/store";
        Long templateId = 100L;
        Hypervisor.HypervisorType hypervisor = Hypervisor.HypervisorType.KVM;

        when(templateDetails.getHypervisorType()).thenReturn(hypervisor);
        doReturn(templateId).when(systemVmTemplateRegistration)
                .performTemplateRegistrationOperations(
                        name, templateDetails, "a", "b",
                        SystemVmTemplateRegistration.HYPERVISOR_IMAGE_FORMAT_MAP.get(hypervisor),
                        SystemVmTemplateRegistration.hypervisorGuestOsMap.get(hypervisor),
                        storeId, null, filePath, null);
        doNothing().when(systemVmTemplateRegistration).updateConfigurationParams(hypervisor, name, zoneId);
        doNothing().when(systemVmTemplateRegistration).updateSystemVMEntries(templateId, hypervisor);

        systemVmTemplateRegistration.registerNewTemplate(name, templateDetails, zoneId, storeId, filePath);

        verify(systemVmTemplateRegistration).performTemplateRegistrationOperations(
                name, templateDetails, templateDetails.getUrl(), templateDetails.getChecksum(),
                SystemVmTemplateRegistration.HYPERVISOR_IMAGE_FORMAT_MAP.get(hypervisor),
                SystemVmTemplateRegistration.hypervisorGuestOsMap.get(hypervisor),
                storeId, null, filePath, null);
        verify(systemVmTemplateRegistration).updateConfigurationParams(hypervisor, name, zoneId);
        verify(systemVmTemplateRegistration).updateSystemVMEntries(templateId, hypervisor);
    }

    @Test
    public void registerNewTemplate_ThrowsExceptionWhenRegistrationFails() {
        String name = "templateName";
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getUrl()).thenReturn("a");
        when(templateDetails.getChecksum()).thenReturn("b");
        long zoneId = 1L;
        Long storeId = 2L;
        String filePath = "/tmp/store";
        Hypervisor.HypervisorType hypervisor = Hypervisor.HypervisorType.KVM;

        when(templateDetails.getHypervisorType()).thenReturn(hypervisor);
        doThrow(new CloudRuntimeException("Registration failed")).when(systemVmTemplateRegistration)
                .performTemplateRegistrationOperations(
                        name, templateDetails, "a", "b",
                        SystemVmTemplateRegistration.HYPERVISOR_IMAGE_FORMAT_MAP.get(hypervisor),
                        SystemVmTemplateRegistration.hypervisorGuestOsMap.get(hypervisor),
                        storeId, null, filePath, null);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.registerNewTemplate(name, templateDetails, zoneId, storeId, filePath);
        });

        assertTrue(exception.getMessage().contains("Failed to register Template for"));
        verify(systemVmTemplateRegistration).performTemplateRegistrationOperations(
                name, templateDetails, templateDetails.getUrl(), templateDetails.getChecksum(),
                SystemVmTemplateRegistration.HYPERVISOR_IMAGE_FORMAT_MAP.get(hypervisor),
                SystemVmTemplateRegistration.hypervisorGuestOsMap.get(hypervisor),
                storeId, null, filePath, null);
        verify(systemVmTemplateRegistration, never()).updateConfigurationParams(any(), any(), anyLong());
        verify(systemVmTemplateRegistration, never()).updateSystemVMEntries(anyLong(), any());
    }

    @Test
    public void registerNewTemplate_CleansUpOnFailure() {
        String name = "templateName";
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        when(templateDetails.getUrl()).thenReturn("a");
        when(templateDetails.getChecksum()).thenReturn("b");
        long zoneId = 1L;
        Long storeId = 2L;
        String filePath = "/tmp/store";
        Long templateId = 100L;
        Hypervisor.HypervisorType hypervisor = Hypervisor.HypervisorType.KVM;

        when(templateDetails.getHypervisorType()).thenReturn(hypervisor);
        doReturn(templateId).when(systemVmTemplateRegistration)
                .performTemplateRegistrationOperations(
                        name, templateDetails, "a", "b",
                        SystemVmTemplateRegistration.HYPERVISOR_IMAGE_FORMAT_MAP.get(hypervisor),
                        SystemVmTemplateRegistration.hypervisorGuestOsMap.get(hypervisor),
                        storeId, null, filePath, null);
        doThrow(new CloudRuntimeException("Update failed")).when(systemVmTemplateRegistration)
                .updateConfigurationParams(hypervisor, name, zoneId);
        doNothing().when(systemVmTemplateRegistration).updateTemplateEntriesOnFailure(templateId);

        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
                systemVmTemplateRegistration.registerNewTemplate(name, templateDetails, zoneId, storeId, filePath);
            });

            assertTrue(exception.getMessage().contains("Failed to register Template for"));
            verify(systemVmTemplateRegistration).updateTemplateEntriesOnFailure(templateId);
            mockedStatic.verify(() -> SystemVmTemplateRegistration.cleanupStore(templateId, filePath));
        }
    }

    @Test
    public void addExistingTemplateToStore_AddsTemplateSuccessfully() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        when(templateVO.getId()).thenReturn(1L);
        when(templateVO.getName()).thenReturn("templateName");
        when(templateVO.getUrl()).thenReturn("http://example.com/template");
        when(templateVO.getChecksum()).thenReturn("abc123");
        when(templateVO.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(templateVO.getGuestOSId()).thenReturn(1L);
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        long zoneId = 1L;
        Long storeId = 2L;
        String filePath = "/tmp/store";

        doReturn(1L).when(systemVmTemplateRegistration)
                .performTemplateRegistrationOperations("templateName", templateDetails,
                        "http://example.com/template", "abc123", Storage.ImageFormat.QCOW2,
                        1L, storeId, 1L, filePath, templateDataStoreVO);

        systemVmTemplateRegistration.addExistingTemplateToStore(templateVO, templateDetails, templateDataStoreVO, zoneId, storeId, filePath);

        verify(systemVmTemplateRegistration).performTemplateRegistrationOperations(
                templateVO.getName(), templateDetails, templateVO.getUrl(), templateVO.getChecksum(),
                templateVO.getFormat(), templateVO.getGuestOSId(), storeId, templateVO.getId(), filePath, templateDataStoreVO);
    }

    @Test
    public void addExistingTemplateToStore_CleansUpOnFailure() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        when(templateVO.getId()).thenReturn(1L);
        when(templateVO.getName()).thenReturn("templateName");
        when(templateVO.getUrl()).thenReturn("http://example.com/template");
        when(templateVO.getChecksum()).thenReturn("abc123");
        when(templateVO.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(templateVO.getGuestOSId()).thenReturn(1L);
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails =
                Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);
        long zoneId = 1L;
        Long storeId = 2L;
        String filePath = "/tmp/store";

        doThrow(new CloudRuntimeException("Operation failed")).when(systemVmTemplateRegistration)
                .performTemplateRegistrationOperations("templateName", templateDetails,
                        "http://example.com/template", "abc123", Storage.ImageFormat.QCOW2,
                        1L, storeId, 1L, filePath, templateDataStoreVO);

        try (MockedStatic<SystemVmTemplateRegistration> mockedStatic =
                     Mockito.mockStatic(SystemVmTemplateRegistration.class)) {
            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
                systemVmTemplateRegistration.addExistingTemplateToStore(templateVO, templateDetails, templateDataStoreVO, zoneId, storeId, filePath);
            });

            assertTrue(exception.getMessage().contains("Failed to add"));
            mockedStatic.verify(() -> SystemVmTemplateRegistration.cleanupStore(templateVO.getId(), filePath));
        }
    }

    @Test
    public void updateSeededTemplateDetails_UpdatesTemplateAndStoreSuccessfully() {
        long templateId = 1L;
        long storeId = 2L;
        long size = 1024L;
        long physicalSize = 2048L;
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(template.getId()).thenReturn(templateId);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);

        when(vmTemplateDao.findById(templateId)).thenReturn(template);
        when(templateDataStoreDao.findByStoreTemplate(storeId, templateId)).thenReturn(templateDataStoreVO);
        when(templateDataStoreDao.update(anyLong(), any(TemplateDataStoreVO.class))).thenReturn(true);

        systemVmTemplateRegistration.updateSeededTemplateDetails(templateId, storeId, size, physicalSize);

        verify(template).setSize(size);
        verify(vmTemplateDao).update(template.getId(), template);
        verify(templateDataStoreVO).setSize(size);
        verify(templateDataStoreVO).setPhysicalSize(physicalSize);
        verify(templateDataStoreVO).setLastUpdated(any(Date.class));
        verify(templateDataStoreDao).update(templateDataStoreVO.getId(), templateDataStoreVO);
    }

    @Test
    public void updateSeededTemplateDetails_ThrowsExceptionWhenStoreUpdateFails() {
        long templateId = 1L;
        long storeId = 2L;
        long size = 1024L;
        long physicalSize = 2048L;
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(template.getId()).thenReturn(templateId);
        TemplateDataStoreVO templateDataStoreVO = Mockito.mock(TemplateDataStoreVO.class);

        when(vmTemplateDao.findById(templateId)).thenReturn(template);
        when(templateDataStoreDao.findByStoreTemplate(storeId, templateId)).thenReturn(templateDataStoreVO);
        when(templateDataStoreDao.update(anyLong(), any(TemplateDataStoreVO.class))).thenReturn(false);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.updateSeededTemplateDetails(templateId, storeId, size, physicalSize);
        });

        assertTrue(exception.getMessage().contains("Failed to update template-store record for seeded system VM Template"));
        verify(templateDataStoreDao).update(templateDataStoreVO.getId(), templateDataStoreVO);
    }

    @Test
    public void updateSystemVMEntries_UpdatesTemplateIdSuccessfully() {
        long templateId = 1L;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;

        systemVmTemplateRegistration.updateSystemVMEntries(templateId, hypervisorType);

        verify(vmInstanceDao).updateSystemVmTemplateId(templateId, hypervisorType);
    }

    @Test
    public void updateHypervisorGuestOsMap_UpdatesGuestOsMapSuccessfully() {
        GuestOSVO guestOS = Mockito.mock(GuestOSVO.class);
        when(guestOS.getId()).thenReturn(10L);
        when(guestOSDao.findOneByDisplayName(DEFAULT_SYSTEM_VM_GUEST_OS_NAME)).thenReturn(guestOS);

        systemVmTemplateRegistration.updateHypervisorGuestOsMap();

        verify(guestOSDao).findOneByDisplayName(DEFAULT_SYSTEM_VM_GUEST_OS_NAME);
        assertEquals(10, SystemVmTemplateRegistration.LINUX_12_ID.intValue());
        assertEquals(10, SystemVmTemplateRegistration.hypervisorGuestOsMap.get(Hypervisor.HypervisorType.KVM).intValue());
        assertEquals(10, SystemVmTemplateRegistration.hypervisorGuestOsMap.get(Hypervisor.HypervisorType.Hyperv).intValue());
        assertEquals(10, SystemVmTemplateRegistration.hypervisorGuestOsMap.get(Hypervisor.HypervisorType.LXC).intValue());
        assertEquals(10, SystemVmTemplateRegistration.hypervisorGuestOsMap.get(Hypervisor.HypervisorType.Ovm3).intValue());
    }

    @Test
    public void updateHypervisorGuestOsMap_SkipsUpdateWhenGuestOsNotFound() {
        Integer value = SystemVmTemplateRegistration.hypervisorGuestOsMap.get(Hypervisor.HypervisorType.KVM);
        when(guestOSDao.findOneByDisplayName(DEFAULT_SYSTEM_VM_GUEST_OS_NAME)).thenReturn(null);

        systemVmTemplateRegistration.updateHypervisorGuestOsMap();

        verify(guestOSDao).findOneByDisplayName(DEFAULT_SYSTEM_VM_GUEST_OS_NAME);
        assertEquals(value, SystemVmTemplateRegistration.hypervisorGuestOsMap.get(Hypervisor.HypervisorType.KVM));
    }

    @Test
    public void updateHypervisorGuestOsMap_LogsWarningOnException() {
        when(guestOSDao.findOneByDisplayName(DEFAULT_SYSTEM_VM_GUEST_OS_NAME)).thenThrow(new RuntimeException("Database error"));

        systemVmTemplateRegistration.updateHypervisorGuestOsMap();

        verify(guestOSDao).findOneByDisplayName(DEFAULT_SYSTEM_VM_GUEST_OS_NAME);
    }

    @Test
    public void updateTemplateUrlChecksumAndGuestOsId_UpdatesSuccessfully() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);
        GuestOSVO guestOS = Mockito.mock(GuestOSVO.class);

        when(templateDetails.getUrl()).thenReturn("http://example.com/template");
        when(templateDetails.getChecksum()).thenReturn("abc123");
        when(templateDetails.getGuestOs()).thenReturn("Debian");
        when(guestOSDao.findOneByDisplayName("Debian")).thenReturn(guestOS);
        when(guestOS.getId()).thenReturn(10L);
        when(templateVO.getId()).thenReturn(1L);
        when(vmTemplateDao.update(templateVO.getId(), templateVO)).thenReturn(true);

        systemVmTemplateRegistration.updateTemplateUrlChecksumAndGuestOsId(templateVO, templateDetails);

        verify(templateVO).setUrl("http://example.com/template");
        verify(templateVO).setChecksum("abc123");
        verify(templateVO).setGuestOSId(10L);
        verify(vmTemplateDao).update(templateVO.getId(), templateVO);
    }

    @Test
    public void updateTemplateUrlChecksumAndGuestOsId_SkipsGuestOsUpdateWhenNotFound() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);

        when(templateDetails.getUrl()).thenReturn("http://example.com/template");
        when(templateDetails.getChecksum()).thenReturn("abc123");
        when(templateDetails.getGuestOs()).thenReturn("NonExistentOS");
        when(guestOSDao.findOneByDisplayName("NonExistentOS")).thenReturn(null);
        when(templateVO.getId()).thenReturn(1L);
        when(vmTemplateDao.update(templateVO.getId(), templateVO)).thenReturn(true);

        systemVmTemplateRegistration.updateTemplateUrlChecksumAndGuestOsId(templateVO, templateDetails);

        verify(templateVO).setUrl("http://example.com/template");
        verify(templateVO).setChecksum("abc123");
        verify(templateVO, never()).setGuestOSId(anyLong());
        verify(vmTemplateDao).update(templateVO.getId(), templateVO);
    }

    @Test
    public void updateTemplateUrlChecksumAndGuestOsId_ThrowsExceptionWhenUpdateFails() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        SystemVmTemplateRegistration.MetadataTemplateDetails templateDetails = Mockito.mock(SystemVmTemplateRegistration.MetadataTemplateDetails.class);

        when(templateDetails.getUrl()).thenReturn("http://example.com/template");
        when(templateDetails.getChecksum()).thenReturn("abc123");
        when(templateDetails.getGuestOs()).thenReturn("Debian");
        when(templateVO.getId()).thenReturn(1L);
        when(vmTemplateDao.update(templateVO.getId(), templateVO)).thenReturn(false);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class, () -> {
            systemVmTemplateRegistration.updateTemplateUrlChecksumAndGuestOsId(templateVO, templateDetails);
        });

        assertTrue(exception.getMessage().contains("Exception while updating 'url' and 'checksum' for hypervisor type"));
        verify(vmTemplateDao).update(templateVO.getId(), templateVO);
    }
}
