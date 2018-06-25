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

import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.reflections.ReflectionUtils;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class})
public class ConfigDriveBuilderTest {

    @Test
    public void writeFileTest() throws Exception {
        PowerMockito.mockStatic(FileUtils.class);

        ConfigDriveBuilder.writeFile(new File("folder"), "subfolder", "content");

        PowerMockito.verifyStatic();
        FileUtils.write(Mockito.any(File.class), Mockito.anyString(), Mockito.any(Charset.class), Mockito.eq(false));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = CloudRuntimeException.class)
    public void writeFileTestwriteFileTestIOExceptionWhileWritingFile() throws Exception {
        PowerMockito.mockStatic(FileUtils.class);

        //Does not look good, I know... but this is the price of static methods.
        Method method = ReflectionUtils.getMethods(FileUtils.class, ReflectionUtils.withParameters(File.class, CharSequence.class, Charset.class, Boolean.TYPE)).iterator().next();
        PowerMockito.when(FileUtils.class, method).withArguments(Mockito.any(File.class), Mockito.anyString(), Mockito.any(Charset.class), Mockito.anyBoolean()).thenThrow(IOException.class);

        ConfigDriveBuilder.writeFile(new File("folder"), "subfolder", "content");
    }

    @Test
    public void fileToBase64StringTest() throws Exception {
        PowerMockito.mockStatic(FileUtils.class);

        String fileContent = "content";
        Method method = getFileUtilsReadfileToByteArrayMethod();
        PowerMockito.when(FileUtils.class, method).withArguments(Mockito.any(File.class)).thenReturn(fileContent.getBytes());

        String returnedContentInBase64 = ConfigDriveBuilder.fileToBase64String(new File("file"));

        Assert.assertEquals("Y29udGVudA==", returnedContentInBase64);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IOException.class)
    public void fileToBase64StringTestIOException() throws Exception {
        PowerMockito.mockStatic(FileUtils.class);

        Method method = getFileUtilsReadfileToByteArrayMethod();
        PowerMockito.when(FileUtils.class, method).withArguments(Mockito.any(File.class)).thenThrow(IOException.class);

        ConfigDriveBuilder.fileToBase64String(new File("file"));
    }

    @SuppressWarnings("unchecked")
    private Method getFileUtilsReadfileToByteArrayMethod() {
        return ReflectionUtils.getMethods(FileUtils.class, ReflectionUtils.withName("readFileToByteArray")).iterator().next();
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
        ConfigDriveBuilder.buildConfigDrive(null, "teste", "C:");
    }

    @SuppressWarnings("unchecked")
    @PrepareForTest({ConfigDriveBuilder.class})
    @Test(expected = CloudRuntimeException.class)
    public void buildConfigDriveTestIoException() throws Exception {
        PowerMockito.mockStatic(ConfigDriveBuilder.class);

        Method method = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("writeVendorAndNetworkEmptyJsonFile")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, method).withArguments(Mockito.any(File.class)).thenThrow(IOException.class);

        //This is odd, but it was necessary to allow us to check if we catch the IOexception and re-throw as a CloudRuntimeException
        //We are mocking the class being tested; therefore, we needed to force the execution of the real method we want to test.
        PowerMockito.when(ConfigDriveBuilder.class, new ArrayList<>(), "teste", "C:").thenCallRealMethod();

        ConfigDriveBuilder.buildConfigDrive(new ArrayList<>(), "teste", "C:");
    }

    @Test
    @SuppressWarnings("unchecked")
    @PrepareForTest({ConfigDriveBuilder.class})
    public void buildConfigDriveTest() throws Exception {
        PowerMockito.mockStatic(ConfigDriveBuilder.class);

        Method writeVendorAndNetworkEmptyJsonFileMethod = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("writeVendorAndNetworkEmptyJsonFile")).iterator().next();
        PowerMockito.doNothing().when(ConfigDriveBuilder.class, writeVendorAndNetworkEmptyJsonFileMethod).withArguments(Mockito.any(File.class));

        Method writeVmMetadataMethod = getWriteVmMetadataMethod();
        PowerMockito.doNothing().when(ConfigDriveBuilder.class, writeVmMetadataMethod).withArguments(Mockito.anyListOf(String[].class), Mockito.anyString(), Mockito.any(File.class));

        Method linkUserDataMethod = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("linkUserData")).iterator().next();
        PowerMockito.doNothing().when(ConfigDriveBuilder.class, linkUserDataMethod).withArguments(Mockito.anyString());

        Method generateAndRetrieveIsoAsBase64IsoMethod = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("generateAndRetrieveIsoAsBase64Iso")).iterator().next();
        PowerMockito.doReturn("mockIsoDataBase64").when(ConfigDriveBuilder.class, generateAndRetrieveIsoAsBase64IsoMethod).withArguments(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //force execution of real method
        PowerMockito.when(ConfigDriveBuilder.class, new ArrayList<>(), "teste", "C:").thenCallRealMethod();

        String returnedIsoData = ConfigDriveBuilder.buildConfigDrive(new ArrayList<>(), "teste", "C:");

        Assert.assertEquals("mockIsoDataBase64", returnedIsoData);

        PowerMockito.verifyStatic();
        ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(Mockito.any(File.class));
        ConfigDriveBuilder.writeVmMetadata(Mockito.anyListOf(String[].class), Mockito.anyString(), Mockito.any(File.class));
        ConfigDriveBuilder.linkUserData(Mockito.anyString());
        ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @SuppressWarnings("unchecked")
    private Method getWriteVmMetadataMethod() {
        return ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("writeVmMetadata")).iterator().next();
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
    @PrepareForTest({ConfigDriveBuilder.class})
    public void writeVendorAndNetworkEmptyJsonFileTestCreatingFolder() throws Exception {
        PowerMockito.mockStatic(ConfigDriveBuilder.class);

        File folderFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(folderFileMock).exists();
        Mockito.doReturn(true).when(folderFileMock).mkdirs();

        //force execution of real method
        Method writeVendorAndNetworkEmptyJsonFileMethod = getWriteVendorAndNetworkEmptyJsonFileMethod();
        PowerMockito.when(ConfigDriveBuilder.class, writeVendorAndNetworkEmptyJsonFileMethod).withArguments(folderFileMock).thenCallRealMethod();

        ConfigDriveBuilder.writeVendorAndNetworkEmptyJsonFile(folderFileMock);

        Mockito.verify(folderFileMock).exists();
        Mockito.verify(folderFileMock).mkdirs();

        PowerMockito.verifyStatic();
        ConfigDriveBuilder.writeFile(Mockito.any(File.class), Mockito.eq("vendor_data.json"), Mockito.eq("{}"));
        ConfigDriveBuilder.writeFile(Mockito.any(File.class), Mockito.eq("network_data.json"), Mockito.eq("{}"));
    }

    @SuppressWarnings("unchecked")
    private Method getWriteVendorAndNetworkEmptyJsonFileMethod() {
        return ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("writeVendorAndNetworkEmptyJsonFile")).iterator().next();
    }

    @Test
    @SuppressWarnings("unchecked")
    @PrepareForTest({ConfigDriveBuilder.class})
    public void writeVmMetadataTest() throws Exception {
        PowerMockito.mockStatic(ConfigDriveBuilder.class);

        Method method = getWriteVmMetadataMethod();
        PowerMockito.when(ConfigDriveBuilder.class, method).withArguments(new ArrayList<>(), "metadataFile", new File("folder")).thenCallRealMethod();

        Method createJsonObjectWithVmDataMethod = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("createJsonObjectWithVmData")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, createJsonObjectWithVmDataMethod).withArguments(Mockito.anyListOf(String[].class), Mockito.any(File.class)).thenReturn(new JsonObject());

        ConfigDriveBuilder.writeVmMetadata(new ArrayList<>(), "metadataFile", new File("folder"));

        PowerMockito.verifyStatic();
        ConfigDriveBuilder.createJsonObjectWithVmData(Mockito.anyListOf(String[].class), Mockito.anyString());
        ConfigDriveBuilder.writeFile(Mockito.any(File.class), Mockito.eq("meta_data.json"), Mockito.eq("{}"));
    }

    @Test
    @PrepareForTest({File.class, Script.class, ConfigDriveBuilder.class})
    public void linkUserDataTestUserDataFilePathDoesNotExist() throws Exception {
        File fileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(fileMock).exists();

        PowerMockito.mockStatic(File.class, Script.class);
        PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(fileMock);

        Script scriptMock = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(scriptMock);

        ConfigDriveBuilder.linkUserData("test");

        Mockito.verify(scriptMock, times(0)).execute();
    }

    @Test(expected = CloudRuntimeException.class)
    @PrepareForTest({File.class, Script.class, ConfigDriveBuilder.class})
    public void linkUserDataTestUserDataFilePathExistAndExecutionPresentedSomeError() throws Exception {
        File fileMock = Mockito.mock(File.class);
        Mockito.doReturn(true).when(fileMock).exists();

        PowerMockito.mockStatic(File.class, Script.class);
        PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(fileMock);

        Script scriptMock = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(scriptMock);

        Mockito.doReturn("message").when(scriptMock).execute();
        ConfigDriveBuilder.linkUserData("test");
    }

    @Test
    @PrepareForTest({File.class, Script.class, ConfigDriveBuilder.class})
    public void linkUserDataTest() throws Exception {
        File fileMock = Mockito.mock(File.class);
        Mockito.doReturn(true).when(fileMock).exists();

        PowerMockito.mockStatic(File.class, Script.class);
        PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(fileMock);

        Script scriptMock = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(scriptMock);

        Mockito.doReturn(StringUtils.EMPTY).when(scriptMock).execute();
        String tempDirName = "test";
        ConfigDriveBuilder.linkUserData(tempDirName);

        Mockito.verify(scriptMock).add(tempDirName + ConfigDrive.cloudStackConfigDriveName + "userdata/user_data.txt");
        Mockito.verify(scriptMock).add(tempDirName + ConfigDrive.openStackConfigDriveName + "user_data");
        Mockito.verify(scriptMock).execute();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = CloudRuntimeException.class)
    @PrepareForTest({Script.class, ConfigDriveBuilder.class})
    public void generateAndRetrieveIsoAsBase64IsoTestGenIsoFailure() throws Exception {
        PowerMockito.mockStatic(Script.class, ConfigDriveBuilder.class);

        Script scriptMock = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(scriptMock);

        Mockito.doReturn("scriptMessage").when(scriptMock).execute();

        Method method = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("generateAndRetrieveIsoAsBase64Iso")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, method).withArguments(Mockito.any(File.class), Mockito.any(File.class), Mockito.any(File.class)).thenCallRealMethod();

        Method getProgramToGenerateIsoMethod = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("getProgramToGenerateIso")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, getProgramToGenerateIsoMethod).withNoArguments().thenReturn("/usr/bin/genisoimage");

        ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso("isoFileName", "driveLabel", "tempDirName");
    }

    @SuppressWarnings("unchecked")
    @Test(expected = CloudRuntimeException.class)
    @PrepareForTest({File.class, Script.class, ConfigDriveBuilder.class})
    public void generateAndRetrieveIsoAsBase64IsoTestIsoTooBig() throws Exception {
        PowerMockito.mockStatic(File.class, Script.class, ConfigDriveBuilder.class);

        File fileMock = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(fileMock);

        Script scriptMock = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(scriptMock);

        Mockito.doReturn(StringUtils.EMPTY).when(scriptMock).execute();
        Mockito.doReturn(64L * 1024L * 1024L + 1l).when(fileMock).length();

        Method method = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("generateAndRetrieveIsoAsBase64Iso")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, method).withArguments(Mockito.any(File.class), Mockito.any(File.class), Mockito.any(File.class)).thenCallRealMethod();

        Method getProgramToGenerateIsoMethod = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("getProgramToGenerateIso")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, getProgramToGenerateIsoMethod).withNoArguments().thenReturn("/usr/bin/genisoimage");

        ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso("isoFileName", "driveLabel", "tempDirName");
    }

    @Test
    @SuppressWarnings("unchecked")
    @PrepareForTest({File.class, Script.class, ConfigDriveBuilder.class})
    public void generateAndRetrieveIsoAsBase64IsoTest() throws Exception {
        PowerMockito.mockStatic(File.class, Script.class, ConfigDriveBuilder.class);

        File fileMock = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withArguments("tempDirName", "isoFileName").thenReturn(fileMock);

        Script scriptMock = Mockito.mock(Script.class);
        PowerMockito.whenNew(Script.class).withAnyArguments().thenReturn(scriptMock);

        Mockito.when(fileMock.getAbsolutePath()).thenReturn("absolutePath");
        Mockito.doReturn(StringUtils.EMPTY).when(scriptMock).execute();
        Mockito.doReturn(64L * 1024L * 1024L).when(fileMock).length();

        Method method = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("generateAndRetrieveIsoAsBase64Iso")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, method).withArguments(Mockito.any(File.class), Mockito.any(File.class), Mockito.any(File.class)).thenCallRealMethod();

        Method getProgramToGenerateIsoMethod = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("getProgramToGenerateIso")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, getProgramToGenerateIsoMethod).withNoArguments().thenReturn("/usr/bin/genisoimage");

        ConfigDriveBuilder.generateAndRetrieveIsoAsBase64Iso("isoFileName", "driveLabel", "tempDirName");

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

        PowerMockito.verifyStatic();
        ConfigDriveBuilder.fileToBase64String(Mockito.any(File.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    @PrepareForTest({ConfigDriveBuilder.class})
    public void createJsonObjectWithVmDataTesT() throws Exception {
        PowerMockito.mockStatic(ConfigDriveBuilder.class);

        Method method = ReflectionUtils.getMethods(ConfigDriveBuilder.class, ReflectionUtils.withName("createJsonObjectWithVmData")).iterator().next();
        PowerMockito.when(ConfigDriveBuilder.class, method).withArguments(Mockito.anyListOf(String[].class), Mockito.anyString()).thenCallRealMethod();

        List<String[]> vmData = new ArrayList<>();
        vmData.add(new String[] {"dataType", "fileName", "content"});
        vmData.add(new String[] {"dataType2", "fileName2", "content2"});

        ConfigDriveBuilder.createJsonObjectWithVmData(vmData, "tempDirName");

        PowerMockito.verifyStatic(Mockito.times(1));
        ConfigDriveBuilder.createFileInTempDirAnAppendOpenStackMetadataToJsonObject(Mockito.eq("tempDirName"), Mockito.any(JsonObject.class), Mockito.eq("dataType"), Mockito.eq("fileName"),
                Mockito.eq("content"));
        ConfigDriveBuilder.createFileInTempDirAnAppendOpenStackMetadataToJsonObject(Mockito.eq("tempDirName"), Mockito.any(JsonObject.class), Mockito.eq("dataType2"), Mockito.eq("fileName2"),
                Mockito.eq("content2"));
    }

    @Test
    @PrepareForTest({File.class, ConfigDriveBuilder.class})
    public void getProgramToGenerateIsoTestGenIsoExistsAndIsExecutable() throws Exception {
        PowerMockito.mockStatic(File.class);

        File genIsoFileMock = Mockito.mock(File.class);
        Mockito.doReturn(true).when(genIsoFileMock).exists();
        Mockito.doReturn(true).when(genIsoFileMock).canExecute();

        PowerMockito.whenNew(File.class).withArguments("/usr/bin/genisoimage").thenReturn(genIsoFileMock);

        ConfigDriveBuilder.getProgramToGenerateIso();

        Mockito.verify(genIsoFileMock, Mockito.times(2)).exists();
        Mockito.verify(genIsoFileMock).canExecute();
        Mockito.verify(genIsoFileMock).getCanonicalPath();
    }

    @Test(expected = CloudRuntimeException.class)
    @PrepareForTest({File.class, ConfigDriveBuilder.class})
    public void getProgramToGenerateIsoTestGenIsoExistsbutNotExecutable() throws Exception {
        PowerMockito.mockStatic(File.class);

        File genIsoFileMock = Mockito.mock(File.class);
        Mockito.doReturn(true).when(genIsoFileMock).exists();
        Mockito.doReturn(false).when(genIsoFileMock).canExecute();

        PowerMockito.whenNew(File.class).withArguments("/usr/bin/genisoimage").thenReturn(genIsoFileMock);

        ConfigDriveBuilder.getProgramToGenerateIso();
    }

    @Test
    @PrepareForTest({File.class, ConfigDriveBuilder.class})
    public void getProgramToGenerateIsoTestNotGenIsoMkIsoInLinux() throws Exception {
        PowerMockito.mockStatic(File.class);

        File genIsoFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(genIsoFileMock).exists();

        File mkIsoProgramInLinuxFileMock = Mockito.mock(File.class);
        Mockito.doReturn(true).when(mkIsoProgramInLinuxFileMock).exists();
        Mockito.doReturn(true).when(mkIsoProgramInLinuxFileMock).canExecute();

        PowerMockito.whenNew(File.class).withArguments("/usr/bin/genisoimage").thenReturn(genIsoFileMock);
        PowerMockito.whenNew(File.class).withArguments("/usr/bin/mkisofs").thenReturn(mkIsoProgramInLinuxFileMock);

        ConfigDriveBuilder.getProgramToGenerateIso();

        Mockito.verify(genIsoFileMock, Mockito.times(1)).exists();
        Mockito.verify(genIsoFileMock, Mockito.times(0)).canExecute();
        Mockito.verify(genIsoFileMock, Mockito.times(0)).getCanonicalPath();

        Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(2)).exists();
        Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(1)).canExecute();
        Mockito.verify(mkIsoProgramInLinuxFileMock, Mockito.times(1)).getCanonicalPath();
    }

    @Test
    @PrepareForTest({File.class, ConfigDriveBuilder.class})
    public void getProgramToGenerateIsoTestMkIsoMac() throws Exception {
        PowerMockito.mockStatic(File.class);

        File genIsoFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(genIsoFileMock).exists();

        File mkIsoProgramInLinuxFileMock = Mockito.mock(File.class);
        Mockito.doReturn(false).when(mkIsoProgramInLinuxFileMock).exists();

        File mkIsoProgramInMacOsFileMock = Mockito.mock(File.class);
        Mockito.doReturn(true).when(mkIsoProgramInMacOsFileMock).exists();
        Mockito.doReturn(true).when(mkIsoProgramInMacOsFileMock).canExecute();

        PowerMockito.whenNew(File.class).withArguments("/usr/bin/genisoimage").thenReturn(genIsoFileMock);
        PowerMockito.whenNew(File.class).withArguments("/usr/bin/mkisofs").thenReturn(mkIsoProgramInLinuxFileMock);
        PowerMockito.whenNew(File.class).withArguments("/usr/local/bin/mkisofs").thenReturn(mkIsoProgramInMacOsFileMock);

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