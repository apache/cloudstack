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

package org.apache.cloudstack.storage.configdrive;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.network.Network;
import com.cloud.vm.NicProfile;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;

@RunWith(MockitoJUnitRunner.class)
public class ConfigDriveBuilderTest {

    private static Map<Long, List<Network.Service>> supportedServices;

    @BeforeClass
    public static void beforeClass() throws Exception {
        supportedServices = Map.of(1L, List.of(Network.Service.UserData, Network.Service.Dhcp, Network.Service.Dns));
    }

    @Test
    public void writeFileTest() {
        try (MockedStatic<FileUtils> fileUtilsMocked = Mockito.mockStatic(FileUtils.class)) {

            ConfigDriveBuilder.writeFile(new File("folder"), "subfolder", "content");

            fileUtilsMocked.verify(() -> FileUtils.write(Mockito.any(File.class), Mockito.anyString(), Mockito.any(Charset.class), Mockito.eq(false)));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void writeFileTestwriteFileTestIOExceptionWhileWritingFile() {
        try (MockedStatic<FileUtils> fileUtilsMocked = Mockito.mockStatic(FileUtils.class)) {
            fileUtilsMocked.when(() -> FileUtils.write(Mockito.any(File.class), Mockito.any(CharSequence.class), Mockito.any(Charset.class), Mockito.anyBoolean())).thenThrow(IOException.class);
            ConfigDriveBuilder.writeFile(new File("folder"), "subfolder", "content");
        }
    }

    @Test
    public void fileToBase64StringTest() throws Exception {
        try (MockedStatic<FileUtils> ignored = Mockito.mockStatic(FileUtils.class)) {

            String fileContent = "content";
            Mockito.when(FileUtils.readFileToByteArray(Mockito.any(File.class))).thenReturn(fileContent.getBytes());

            String returnedContentInBase64 = ConfigDriveBuilder.fileToBase64String(new File("file"));

            Assert.assertEquals("Y29udGVudA==", returnedContentInBase64);
        }
    }

    @Test(expected = IOException.class)
    public void fileToBase64StringTestIOException() throws Exception {
        try (MockedStatic<FileUtils> ignored = Mockito.mockStatic(FileUtils.class)) {

            Mockito.when(FileUtils.readFileToByteArray(Mockito.any(File.class))).thenThrow(IOException.class);

            ConfigDriveBuilder.fileToBase64String(new File("file"));
        }
    }

    @Test
    public void base64StringToFileTest() throws Exception {
        String encodedIsoData = "Y29udGVudA==";

        String parentFolder = "parentFolder";
        String fileName = "fileName";

        File parentFolderFile = new File(parentFolder);
        parentFolderFile.mkdir();

        ConfigDriveBuilder.base64StringToFile(encodedIsoData, parentFolder, fileName);

        File file = new File(parentFolderFile, fileName);
        String contentOfFile = new String(FileUtils.readFileToByteArray(file), StandardCharsets.US_ASCII);

        Assert.assertEquals("content", contentOfFile);

        file.delete();
        parentFolderFile.delete();
    }

    @Test(expected = CloudRuntimeException.class)
    public void buildConfigDriveTestNoVmDataAndNic() {
        ConfigDriveBuilder.buildConfigDrive(null, null, "teste", "C:", null, null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void buildConfigDriveTestIoException() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVendorEmptyJsonFile(nullable(File.class))).thenThrow(CloudRuntimeException.class);
            Mockito.when(ConfigDriveBuilder.buildConfigDrive(null, new ArrayList<>(), "teste", "C:", null, supportedServices)).thenCallRealMethod();
            ConfigDriveBuilder.buildConfigDrive(null, new ArrayList<>(), "teste", "C:", null, supportedServices);
        }
    }

    @Test
    public void buildConfigDriveTest() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVendorEmptyJsonFile(Mockito.any(File.class))).then(invocationOnMock -> null);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVmMetadata(Mockito.anyList(), Mockito.anyString(), Mockito.any(File.class), anyMap())).then(invocationOnMock -> null);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.linkUserData((Mockito.anyString()))).then(invocationOnMock -> null);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> "mockIsoDataBase64");

            NicProfile mockedNicProfile = Mockito.mock(NicProfile.class);
            Mockito.when(mockedNicProfile.getId()).thenReturn(1L);

            //force execution of real method
            Mockito.when(ConfigDriveBuilder.buildConfigDrive(List.of(mockedNicProfile), new ArrayList<>(), "teste", "C:", null, supportedServices)).thenCallRealMethod();

            String returnedIsoData = ConfigDriveBuilder.buildConfigDrive(List.of(mockedNicProfile), new ArrayList<>(), "teste", "C:", null, supportedServices);

            Assert.assertEquals("mockIsoDataBase64", returnedIsoData);

            configDriveBuilderMocked.verify(() -> {
                ConfigDriveBuilder.writeVendorEmptyJsonFile(Mockito.any(File.class));
                ConfigDriveBuilder.writeVmMetadata(Mockito.anyList(), Mockito.anyString(), Mockito.any(File.class), anyMap());
                ConfigDriveBuilder.linkUserData(Mockito.anyString());
                ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
            });
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void writeVendorEmptyJsonFileTestCannotCreateOpenStackFolder() {
        File folderFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(folderFileMock).mkdirs();

        ConfigDriveBuilder.writeVendorEmptyJsonFile(folderFileMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void writeVendorEmptyJsonFileTest() {
        File folderFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(folderFileMock).mkdirs();

        ConfigDriveBuilder.writeVendorEmptyJsonFile(folderFileMock);
    }

    @Test
    public void writeVendorEmptyJsonFileTestCreatingFolder() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            File folderFileMock = Mockito.mock(File.class);
            Mockito.doReturn(false).when(folderFileMock).exists();
            Mockito.doReturn(true).when(folderFileMock).mkdirs();

            //force execution of real method
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVendorEmptyJsonFile(folderFileMock)).thenCallRealMethod();

            ConfigDriveBuilder.writeVendorEmptyJsonFile(folderFileMock);

            Mockito.verify(folderFileMock).exists();
            Mockito.verify(folderFileMock).mkdirs();

            configDriveBuilderMocked.verify(() -> {
                ConfigDriveBuilder.writeFile(Mockito.any(File.class), Mockito.eq("vendor_data.json"), Mockito.eq("{}"));
                ConfigDriveBuilder.writeFile(Mockito.any(File.class), Mockito.eq("network_data.json"), Mockito.eq("{}"));
            });
        }
    }

    @Test
    public void writeVmMetadataTest() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {
            Mockito.when(ConfigDriveBuilder.createJsonObjectWithVmData(Mockito.anyList(), Mockito.anyString(), Mockito.anyMap())).thenReturn(new JsonObject());

            List<String[]> vmData = new ArrayList<>();
            vmData.add(new String[]{"dataType", "fileName", "content"});
            vmData.add(new String[]{"dataType2", "fileName2", "content2"});

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVmMetadata(vmData, "metadataFile", new File("folder"), new HashMap<>())).thenCallRealMethod();

            ConfigDriveBuilder.writeVmMetadata(vmData, "metadataFile", new File("folder"), new HashMap<>());

            configDriveBuilderMocked.verify(() -> {
                ConfigDriveBuilder.createJsonObjectWithVmData(vmData, "metadataFile", new HashMap<>());
                ConfigDriveBuilder.writeFile(Mockito.any(File.class), Mockito.eq("meta_data.json"), Mockito.eq("{}"));
            });
        }
    }

    @Test
    public void linkUserDataTestUserDataFilePathDoesNotExist() {
        File fileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(fileMock).exists();

        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class);
             MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class)
        ) {
            Mockito.when(ConfigDriveBuilder.getFile(anyString())).thenReturn(fileMock);
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.linkUserData(anyString())).thenCallRealMethod();
            ConfigDriveBuilder.linkUserData("test");
            scriptMock.constructed().forEach(s -> Mockito.verify(s, times(0)).execute());
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void linkUserDataTestUserDataFilePathExistAndExecutionPresentedSomeError() {
        File fileMock = Mockito.mock(File.class);
        Mockito.doReturn(true).when(fileMock).exists();

        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class);
             MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
                 Mockito.doReturn("message").when(mock).execute();
             })
        ) {
            Mockito.when(ConfigDriveBuilder.getFile(anyString())).thenReturn(fileMock);
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.linkUserData(anyString())).thenCallRealMethod();
            ConfigDriveBuilder.linkUserData("test");
        }
    }

    @Test
    public void linkUserDataTest() {
        String tempDirName = "test";

        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class);
             MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class, (mock, context) -> {
                 Mockito.doReturn(StringUtils.EMPTY).when(mock).execute();
             })
        ) {
            File fileMock = Mockito.mock(File.class);
            Mockito.doReturn(true).when(fileMock).exists();
            Mockito.when(ConfigDriveBuilder.getFile(anyString())).thenReturn(fileMock);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.linkUserData(anyString())).thenCallRealMethod();

            ConfigDriveBuilder.linkUserData(tempDirName);
            Script mockedScript = scriptMock.constructed().get(0);
            Mockito.verify(mockedScript).add(tempDirName + ConfigDrive.cloudStackConfigDriveName + "userdata/user_data.txt");
            Mockito.verify(mockedScript).add(tempDirName + ConfigDrive.openStackConfigDriveName + "user_data");
            Mockito.verify(mockedScript).execute();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void generateAndRetrieveIsoAsBase64IsoTestGenIsoFailure() throws Exception {

        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class);
        MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
            Mockito.doReturn("scriptMessage").when(mock).execute();
        })) {
            configDriveBuilderMocked.when(ConfigDriveBuilder::getProgramToGenerateIso).thenReturn("/usr/bin/genisoimage");

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(anyString(), anyString(), anyString())).thenCallRealMethod();
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.getFile(anyString())).thenCallRealMethod();
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.getFile(anyString(), anyString())).thenCallRealMethod();

            ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso("isoFileName", "driveLabel", "tempDirName");
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void generateAndRetrieveIsoAsBase64IsoTestIsoTooBig() throws Exception {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class);
             MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
                 Mockito.doReturn(StringUtils.EMPTY).when(mock).execute();
             })) {
            File fileMock = Mockito.mock(File.class);
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.getFile(anyString())).thenReturn(fileMock);
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.getFile(anyString(), anyString())).thenReturn(fileMock);

            Mockito.when(fileMock.getAbsolutePath()).thenReturn("");
            Mockito.when(fileMock.length()).thenReturn(64L * 1024L * 1024L + 1L);
            Mockito.when(ConfigDriveBuilder.getProgramToGenerateIso()).thenReturn("/usr/bin/genisoimage");

            Mockito.when(ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(nullable(String.class), nullable(String.class), nullable(String.class))).thenCallRealMethod();

            ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso("isoFileName", "driveLabel", "tempDirName");
        }
    }

    @Test
    public void generateAndRetrieveIsoAsBase64IsoTest() throws Exception {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class);
             MockedConstruction<Script> scriptMockedConstruction = Mockito.mockConstruction(Script.class, (mock, context) -> {
                 Mockito.doReturn(StringUtils.EMPTY).when(mock).execute();
             })) {

            File fileMock = Mockito.mock(File.class);
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.getFile(anyString(), anyString())).thenReturn(fileMock);
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.getFile(anyString())).thenReturn(fileMock);

            Mockito.when(fileMock.getAbsolutePath()).thenReturn("absolutePath");
            Mockito.doReturn(64L * 1024L * 1024L).when(fileMock).length();

            Mockito.when(ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(nullable(String.class), nullable(String.class), nullable(String.class))).thenCallRealMethod();

            Mockito.when(ConfigDriveBuilder.getProgramToGenerateIso()).thenReturn("/usr/bin/genisoimage");

            ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso("isoFileName", "driveLabel", "tempDirName");

            Script scriptMock = scriptMockedConstruction.constructed().get(0);
            InOrder inOrder = Mockito.inOrder(scriptMock);
            inOrder.verify(scriptMock).add("-o", "absolutePath");
            inOrder.verify(scriptMock).add("-ldots");
            inOrder.verify(scriptMock).add("-allow-lowercase");
            inOrder.verify(scriptMock).add("-allow-multidot");
            inOrder.verify(scriptMock).add("-cache-inodes");
            inOrder.verify(scriptMock).add("-l");
            inOrder.verify(scriptMock).add("-quiet");
            inOrder.verify(scriptMock).add("-J");
            inOrder.verify(scriptMock).add("-r");
            inOrder.verify(scriptMock).add("-V", "driveLabel");
            inOrder.verify(scriptMock).add("tempDirName");
            inOrder.verify(scriptMock).execute();


            configDriveBuilderMocked.verify(() -> ConfigDriveBuilder.fileToBase64String(nullable(File.class)));
        }
    }

    @Test
    public void createJsonObjectWithVmDataTesT() {

        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            Mockito.when(ConfigDriveBuilder.createJsonObjectWithVmData(Mockito.anyList(), Mockito.anyString(), Mockito.nullable(Map.class))).thenCallRealMethod();

            List<String[]> vmData = new ArrayList<>();
            vmData.add(new String[]{"dataType", "fileName", "content"});
            vmData.add(new String[]{"dataType2", "fileName2", "content2"});

            ConfigDriveBuilder.createJsonObjectWithVmData(vmData, "tempDirName", new HashMap<>());

            configDriveBuilderMocked.verify(() -> {
                ConfigDriveBuilder.createFileInTempDirAnAppendOpenStackMetadataToJsonObject(Mockito.eq("tempDirName"), Mockito.any(JsonObject.class), Mockito.eq("dataType"), Mockito.eq("fileName"),
                        Mockito.eq("content"), Mockito.anyMap());
                ConfigDriveBuilder.createFileInTempDirAnAppendOpenStackMetadataToJsonObject(Mockito.eq("tempDirName"), Mockito.any(JsonObject.class), Mockito.eq("dataType2"), Mockito.eq("fileName2"),
                        Mockito.eq("content2"), Mockito.anyMap());
            });
        }
    }

    @Test
    public void buildCustomUserdataParamsMetadataTestNullContent() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {
            JsonObject metadata = new JsonObject();
            String dataType = "dataType1";
            String fileName = "testFileName";
            String content = null;
            Map<String, String> customUserdataParams = new HashMap<>();
            customUserdataParams.put(fileName, content);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.buildCustomUserdataParamsMetaData(metadata, dataType, fileName, content, customUserdataParams)).thenCallRealMethod();

            ConfigDriveBuilder.buildCustomUserdataParamsMetaData(metadata, dataType, fileName, content, customUserdataParams);

            Assert.assertNull(metadata.getAsJsonPrimitive(fileName));
        }
    }

    @Test
    public void buildCustomUserdataParamsMetadataTestWithContent() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            JsonObject metadata = new JsonObject();
            String dataType = "metadata";
            String fileName = "testFileName";
            String content = "testContent";
            Map<String, String> customUserdataParams = new HashMap<>();
            customUserdataParams.put(fileName, content);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.buildCustomUserdataParamsMetaData(metadata, dataType, fileName, content, customUserdataParams)).thenCallRealMethod();

            ConfigDriveBuilder.buildCustomUserdataParamsMetaData(metadata, dataType, fileName, content, customUserdataParams);

            Assert.assertEquals(content, metadata.getAsJsonPrimitive(fileName).getAsString());
        }
    }

    @Test
    public void getProgramToGenerateIsoTestGenIsoExistsAndIsExecutable() throws Exception {
        try (MockedStatic<ConfigDriveBuilder> ignored = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            File genIsoFileMock = Mockito.mock(File.class);
            Mockito.doReturn(true).when(genIsoFileMock).exists();
            Mockito.doReturn(true).when(genIsoFileMock).canExecute();

            Mockito.when(ConfigDriveBuilder.getFile("/usr/bin/genisoimage")).thenReturn(genIsoFileMock);
            Mockito.when(ConfigDriveBuilder.getProgramToGenerateIso()).thenCallRealMethod();

            ConfigDriveBuilder.getProgramToGenerateIso();

            Mockito.verify(genIsoFileMock, Mockito.times(2)).exists();
            Mockito.verify(genIsoFileMock).canExecute();
            Mockito.verify(genIsoFileMock).getCanonicalPath();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void getProgramToGenerateIsoTestGenIsoExistsbutNotExecutable() throws Exception {
        try (MockedStatic<ConfigDriveBuilder> ignored = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            File genIsoFileMock = Mockito.mock(File.class);
            Mockito.doReturn(true).when(genIsoFileMock).exists();
            Mockito.doReturn(false).when(genIsoFileMock).canExecute();

            Mockito.when(ConfigDriveBuilder.getFile("/usr/bin/genisoimage")).thenReturn(genIsoFileMock);

            Mockito.when(ConfigDriveBuilder.getProgramToGenerateIso()).thenCallRealMethod();

            ConfigDriveBuilder.getProgramToGenerateIso();
        }
    }

    @Test
    public void getProgramToGenerateIsoTestNotGenIsoMkIsoInLinux() throws Exception {
        try (MockedStatic<ConfigDriveBuilder> ignored = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            File genIsoFileMock = Mockito.mock(File.class);
            Mockito.doReturn(false).when(genIsoFileMock).exists();

            File mkIsoProgramInLinuxFileMock = Mockito.mock(File.class);
            Mockito.doReturn(true).when(mkIsoProgramInLinuxFileMock).exists();
            Mockito.doReturn(true).when(mkIsoProgramInLinuxFileMock).canExecute();

            Mockito.when(ConfigDriveBuilder.getFile("/usr/bin/genisoimage")).thenReturn(genIsoFileMock);
            Mockito.when(ConfigDriveBuilder.getFile("/usr/bin/mkisofs")).thenReturn(mkIsoProgramInLinuxFileMock);

            Mockito.when(ConfigDriveBuilder.getProgramToGenerateIso()).thenCallRealMethod();

            ConfigDriveBuilder.getProgramToGenerateIso();

            Mockito.verify(genIsoFileMock, Mockito.times(1)).exists();
            Mockito.verify(genIsoFileMock, Mockito.times(0)).canExecute();
            Mockito.verify(genIsoFileMock, Mockito.times(0)).getCanonicalPath();

            Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(2)).exists();
            Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(1)).canExecute();
            Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(1)).getCanonicalPath();
        }
    }

    @Test
    public void getProgramToGenerateIsoTestMkIsoMac() throws Exception {
        try (MockedStatic<ConfigDriveBuilder> ignored = Mockito.mockStatic(ConfigDriveBuilder.class)) {


            File genIsoFileMock = Mockito.mock(File.class);
            Mockito.doReturn(false).when(genIsoFileMock).exists();

            File mkIsoProgramInLinuxFileMock = Mockito.mock(File.class);
            Mockito.doReturn(false).when(mkIsoProgramInLinuxFileMock).exists();

            File mkIsoProgramInMacOsFileMock = Mockito.mock(File.class);
            Mockito.doReturn(true).when(mkIsoProgramInMacOsFileMock).exists();
            Mockito.doReturn(true).when(mkIsoProgramInMacOsFileMock).canExecute();

            Mockito.when(ConfigDriveBuilder.getFile("/usr/bin/genisoimage")).thenReturn(genIsoFileMock);
            Mockito.when(ConfigDriveBuilder.getFile("/usr/bin/mkisofs")).thenReturn(mkIsoProgramInLinuxFileMock);
            Mockito.when(ConfigDriveBuilder.getFile("/usr/local/bin/mkisofs")).thenReturn(mkIsoProgramInMacOsFileMock);

            Mockito.when(ConfigDriveBuilder.getProgramToGenerateIso()).thenCallRealMethod();


            ConfigDriveBuilder.getProgramToGenerateIso();

            Mockito.verify(genIsoFileMock, Mockito.times(1)).exists();
            Mockito.verify(genIsoFileMock, Mockito.times(0)).canExecute();
            Mockito.verify(genIsoFileMock, Mockito.times(0)).getCanonicalPath();

            Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(1)).exists();
            Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(0)).canExecute();
            Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(0)).getCanonicalPath();

            Mockito.verify(mkIsoProgramInMacOsFileMock, Mockito.times(1)).exists();
            Mockito.verify(mkIsoProgramInMacOsFileMock, Mockito.times(1)).canExecute();
            Mockito.verify(mkIsoProgramInMacOsFileMock, Mockito.times(1)).getCanonicalPath();
        }
    }

    @Test
    public void testWriteNetworkData() throws Exception {
        // Setup
        NicProfile nicp = mock(NicProfile.class);
        Mockito.when(nicp.getId()).thenReturn(1L);

        Mockito.when(nicp.getMacAddress()).thenReturn("00:00:00:00:00:00");
        Mockito.when(nicp.getMtu()).thenReturn(2000);

        Mockito.when(nicp.getIPv4Address()).thenReturn("172.31.0.10");
        Mockito.when(nicp.getDeviceId()).thenReturn(1);
        Mockito.when(nicp.getIPv4Netmask()).thenReturn("255.255.255.0");
        Mockito.when(nicp.getUuid()).thenReturn("NETWORK UUID");
        Mockito.when(nicp.getIPv4Gateway()).thenReturn("172.31.0.1");


        Mockito.when(nicp.getIPv6Address()).thenReturn("2001:db8:0:1234:0:567:8:1");
        Mockito.when(nicp.getIPv6Cidr()).thenReturn("2001:db8:0:1234:0:567:8:1/64");
        Mockito.when(nicp.getIPv6Gateway()).thenReturn("2001:db8:0:1234:0:567:8::1");

        Mockito.when(nicp.getIPv4Dns1()).thenReturn("8.8.8.8");
        Mockito.when(nicp.getIPv4Dns2()).thenReturn("1.1.1.1");
        Mockito.when(nicp.getIPv6Dns1()).thenReturn("2001:4860:4860::8888");
        Mockito.when(nicp.getIPv6Dns2()).thenReturn("2001:4860:4860::8844");


        List<Network.Service> services1 = Arrays.asList(Network.Service.Dhcp, Network.Service.Dns);

        Map<Long, List<Network.Service>> supportedServices = new HashMap<>();
        supportedServices.put(1L, services1);

        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        File openStackFolder = folder.newFolder("openStack");

        // Expected JSON structure
        String expectedJson = "{" +
                "  \"links\": [" +
                "    {" +
                "      \"ethernet_mac_address\": \"00:00:00:00:00:00\"," +
                "      \"id\": \"eth1\"," +
                "      \"mtu\": 2000," +
                "      \"type\": \"phy\"" +
                "    }" +
                "  ]," +
                "  \"networks\": [" +
                "    {" +
                "      \"id\": \"eth1\"," +
                "      \"ip_address\": \"172.31.0.10\"," +
                "      \"link\": \"eth1\"," +
                "      \"netmask\": \"255.255.255.0\"," +
                "      \"network_id\": \"NETWORK UUID\"," +
                "      \"type\": \"ipv4\"," +
                "      \"routes\": [" +
                "        {" +
                "          \"gateway\": \"172.31.0.1\"," +
                "          \"netmask\": \"0.0.0.0\"," +
                "          \"network\": \"0.0.0.0\"" +
                "        }" +
                "      ]" +
                "    }," +
                "    {" +
                "      \"id\": \"eth1\"," +
                "      \"ip_address\": \"2001:db8:0:1234:0:567:8:1\"," +
                "      \"link\": \"eth1\"," +
                "      \"netmask\": \"64\"," +
                "      \"network_id\": \"NETWORK UUID\"," +
                "      \"type\": \"ipv6\"," +
                "      \"routes\": [" +
                "        {" +
                "          \"gateway\": \"2001:db8:0:1234:0:567:8::1\"," +
                "          \"netmask\": \"0\"," +
                "          \"network\": \"::\"" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]," +
                "  \"services\": [" +
                "    {" +
                "      \"address\": \"8.8.8.8\"," +
                "      \"type\": \"dns\"" +
                "    }," +
                "    {" +
                "      \"address\": \"1.1.1.1\"," +
                "      \"type\": \"dns\"" +
                "    }," +
                "    {" +
                "      \"address\": \"2001:4860:4860::8888\"," +
                "      \"type\": \"dns\"" +
                "    }," +
                "    {" +
                "      \"address\": \"2001:4860:4860::8844\"," +
                "      \"type\": \"dns\"" +
                "    }" +
                "  ]" +
                "}";

        // Action
        ConfigDriveBuilder.writeNetworkData(Arrays.asList(nicp), supportedServices, openStackFolder);

        // Verify
        File networkDataFile = new File(openStackFolder, "network_data.json");
        String content = FileUtils.readFileToString(networkDataFile, StandardCharsets.UTF_8);
        JsonObject actualJson = new JsonParser().parse(content).getAsJsonObject();
        JsonObject expectedJsonObject = new JsonParser().parse(expectedJson).getAsJsonObject();

        Assert.assertEquals(expectedJsonObject, actualJson);
        folder.delete();
    }

    @Test
    public void testWriteNetworkDataEmptyJson() throws Exception {
        // Setup
        NicProfile nicp = mock(NicProfile.class);
        List<Network.Service> services1 = Collections.emptyList();

        Map<Long, List<Network.Service>> supportedServices = new HashMap<>();
        supportedServices.put(1L, services1);

        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        File openStackFolder = folder.newFolder("openStack");

        // Expected JSON structure
        String expectedJson = "{}";

        // Action
        ConfigDriveBuilder.writeNetworkData(Arrays.asList(nicp), supportedServices, openStackFolder);

        // Verify
        File networkDataFile = new File(openStackFolder, "network_data.json");
        String content = FileUtils.readFileToString(networkDataFile, StandardCharsets.UTF_8);
        JsonObject actualJson = new JsonParser().parse(content).getAsJsonObject();
        JsonObject expectedJsonObject = new JsonParser().parse(expectedJson).getAsJsonObject();

        Assert.assertEquals(expectedJsonObject, actualJson);
        folder.delete();
    }
}
