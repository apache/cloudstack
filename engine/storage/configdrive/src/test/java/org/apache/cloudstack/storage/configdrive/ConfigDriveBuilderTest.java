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
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
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
    public void buildConfigDriveTestNoVmData() {
        ConfigDriveBuilder.buildConfigDrive(null, "teste", "C:", null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void buildConfigDriveTestIoException() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(nullable(File.class))).thenThrow(CloudRuntimeException.class);
            Mockito.when(ConfigDriveBuilder.buildConfigDrive(new ArrayList<>(), "teste", "C:", null)).thenCallRealMethod();
            ConfigDriveBuilder.buildConfigDrive(new ArrayList<>(), "teste", "C:", null);
        }
    }

    @Test
    public void buildConfigDriveTest() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(Mockito.any(File.class))).then(invocationOnMock -> null);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVmMetadata(Mockito.anyListOf(String[].class), Mockito.anyString(), Mockito.any(File.class), anyMap())).then(invocationOnMock -> null);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.linkUserData((Mockito.anyString()))).then(invocationOnMock -> null);

            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> "mockIsoDataBase64");
            //force execution of real method
            Mockito.when(ConfigDriveBuilder.buildConfigDrive(new ArrayList<>(), "teste", "C:", null)).thenCallRealMethod();

            String returnedIsoData = ConfigDriveBuilder.buildConfigDrive(new ArrayList<>(), "teste", "C:", null);

            Assert.assertEquals("mockIsoDataBase64", returnedIsoData);

            configDriveBuilderMocked.verify(() -> {
                ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(Mockito.any(File.class));
                ConfigDriveBuilder.writeVmMetadata(Mockito.anyListOf(String[].class), Mockito.anyString(), Mockito.any(File.class), anyMap());
                ConfigDriveBuilder.linkUserData(Mockito.anyString());
                ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
            });
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void writeVendorAndNetworkEmptyJsonFileTestCannotCreateOpenStackFolder() {
        File folderFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(folderFileMock).mkdirs();

        ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(folderFileMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void writeVendorAndNetworkEmptyJsonFileTest() {
        File folderFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(folderFileMock).mkdirs();

        ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(folderFileMock);
    }

    @Test
    public void writeVendorAndNetworkEmptyJsonFileTestCreatingFolder() {
        try (MockedStatic<ConfigDriveBuilder> configDriveBuilderMocked = Mockito.mockStatic(ConfigDriveBuilder.class)) {

            File folderFileMock = Mockito.mock(File.class);
            Mockito.doReturn(false).when(folderFileMock).exists();
            Mockito.doReturn(true).when(folderFileMock).mkdirs();

            //force execution of real method
            configDriveBuilderMocked.when(() -> ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(folderFileMock)).thenCallRealMethod();

            ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(folderFileMock);

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
            Mockito.when(ConfigDriveBuilder.createJsonObjectWithVmData(Mockito.anyListOf(String[].class), Mockito.anyString(), Mockito.anyMap())).thenReturn(new JsonObject());

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

            Mockito.when(ConfigDriveBuilder.createJsonObjectWithVmData(Mockito.anyListOf(String[].class), Mockito.anyString(), Mockito.nullable(Map.class))).thenCallRealMethod();

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
}
