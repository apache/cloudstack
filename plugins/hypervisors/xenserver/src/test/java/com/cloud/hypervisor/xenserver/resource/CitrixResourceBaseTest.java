/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.hypervisor.xenserver.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.utils.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase.SRType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.StorageResourceType;
import com.cloud.template.TemplateManager;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Host.Record;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types.XenAPIException;

import static com.cloud.hypervisor.xenserver.resource.CitrixResourceBase.PLATFORM_CORES_PER_SOCKET_KEY;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Host.class, Script.class, SR.class})
public class CitrixResourceBaseTest {

    @Spy
    protected CitrixResourceBase citrixResourceBase = new CitrixResourceBase() {
        @Override
        protected String getPatchFilePath() {
            return null;
        }
    };

    @Mock
    private Connection connectionMock;
    @Mock
    private Host hostMock;
    @Mock
    private Record hostRecordMock;

    private String hostUuidMock = "hostUuidMock";

    private static final String platformString = "device-model:qemu-upstream-compat;vga:std;videoram:8;apic:true;viridian:false;timeoffset:0;pae:true;acpi:1;hpet:true;secureboot:false;nx:true";

    @Before
    public void beforeTest() throws XenAPIException, XmlRpcException {
        citrixResourceBase._host.setUuid(hostUuidMock);

        PowerMockito.mockStatic(Host.class);
        PowerMockito.when(Host.getByUuid(connectionMock, hostUuidMock)).thenReturn(hostMock);

        hostRecordMock.softwareVersion = new HashMap<>();
        Mockito.when(hostMock.getRecord(connectionMock)).thenReturn(hostRecordMock);

    }

    public void testGetPathFilesExeption() {
        String patch = citrixResourceBase.getPatchFilePath();

        PowerMockito.mockStatic(Script.class);
        Mockito.when(Script.findScript("", patch)).thenReturn(null);

        citrixResourceBase.getPatchFiles();

    }

    public void testGetPathFilesListReturned() {
        String patch = citrixResourceBase.getPatchFilePath();

        PowerMockito.mockStatic(Script.class);
        Mockito.when(Script.findScript("", patch)).thenReturn(patch);

        File expected = new File(patch);
        String pathExpected = expected.getAbsolutePath();

        List<File> files = citrixResourceBase.getPatchFiles();
        String receivedPath = files.get(0).getAbsolutePath();
        Assert.assertEquals(receivedPath, pathExpected);
    }

    @Test
    public void testGetGuestOsTypeNull() {
        String platformEmulator = null;

        String expected = "Other install media";
        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(expected, guestOsType);
    }

    @Test
    public void testGetGuestOsTypeEmpty() {
        String platformEmulator = "";

        String expected = "Other install media";
        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(expected, guestOsType);
    }

    @Test
    public void testGetGuestOsTypeBlank() {
        String platformEmulator = "    ";

        String expected = "Other install media";
        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(expected, guestOsType);
    }

    @Test
    public void testGetGuestOsTypeOther() {
        String platformEmulator = "My Own Linux Distribution Y.M (64-bit)";

        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(platformEmulator, guestOsType);
    }

    @Test
    public void actualIsoTemplateTestXcpHots() throws XenAPIException, XmlRpcException {
        hostRecordMock.softwareVersion.put("product_brand", "XCP");
        hostRecordMock.softwareVersion.put("product_version", "1.0");

        String returnedIsoTemplateName = citrixResourceBase.getActualIsoTemplate(connectionMock);

        Assert.assertEquals(TemplateManager.XS_TOOLS_ISO, returnedIsoTemplateName);
    }

    @Test
    public void actualIsoTemplateTestXenServerBefore70() throws XenAPIException, XmlRpcException {
        hostRecordMock.softwareVersion.put("product_brand", "XenServer");
        hostRecordMock.softwareVersion.put("product_version", "6.0");

        String returnedIsoTemplateName = citrixResourceBase.getActualIsoTemplate(connectionMock);

        Assert.assertEquals(TemplateManager.XS_TOOLS_ISO, returnedIsoTemplateName);
    }

    @Test
    public void actualIsoTemplateTestXenServer70() throws XenAPIException, XmlRpcException {
        hostRecordMock.softwareVersion.put("product_brand", "XenServer");
        hostRecordMock.softwareVersion.put("product_version", "7.0");

        String returnedIsoTemplateName = citrixResourceBase.getActualIsoTemplate(connectionMock);

        Assert.assertEquals(CitrixResourceBase.XS_TOOLS_ISO_AFTER_70, returnedIsoTemplateName);
    }

    @Test
    public void actualIsoTemplateTestXenServer71() throws XenAPIException, XmlRpcException {
        hostRecordMock.softwareVersion.put("product_brand", "XenServer");
        hostRecordMock.softwareVersion.put("product_version", "7.1");

        String returnedIsoTemplateName = citrixResourceBase.getActualIsoTemplate(connectionMock);

        Assert.assertEquals(CitrixResourceBase.XS_TOOLS_ISO_AFTER_70, returnedIsoTemplateName);
    }

    @Test
    public void getAllLocalSrForTypeTest() throws Exception {
        String mockHostUuid = "hostUuid";
        citrixResourceBase._host.setUuid(mockHostUuid);

        Connection connectionMock = Mockito.mock(Connection.class);

        SR srExtShared = Mockito.mock(SR.class);
        SR srExtNonShared = Mockito.mock(SR.class);

        List<SR> expectedListOfSrs = new ArrayList<>();
        expectedListOfSrs.add(srExtNonShared);

        Set<PBD> pbds = new HashSet<>();
        PBD pbdMock = Mockito.mock(PBD.class);
        Host hostMock = Mockito.mock(Host.class);
        Mockito.when(hostMock.getUuid(connectionMock)).thenReturn(mockHostUuid);
        Mockito.when(hostMock.toWireString()).thenReturn(mockHostUuid);

        Mockito.when(pbdMock.getHost(connectionMock)).thenReturn(hostMock);
        pbds.add(pbdMock);

        SR.Record srExtSharedRecord = Mockito.mock(SR.Record.class);
        srExtSharedRecord.type = "EXT";
        srExtSharedRecord.shared = true;
        srExtSharedRecord.PBDs = pbds;

        SR.Record srExtNonSharedRecord = Mockito.mock(SR.Record.class);
        srExtNonSharedRecord.type = "EXT";
        srExtNonSharedRecord.shared = false;
        srExtNonSharedRecord.PBDs = pbds;

        Map<SR, SR.Record> mapOfSrsRecords = new HashMap<>();
        mapOfSrsRecords.put(srExtShared, srExtSharedRecord);
        mapOfSrsRecords.put(srExtNonShared, srExtNonSharedRecord);

        PowerMockito.mockStatic(SR.class);
        BDDMockito.given(SR.getAllRecords(connectionMock)).willReturn(mapOfSrsRecords);

        List<SR> allLocalSrForType = citrixResourceBase.getAllLocalSrForType(connectionMock, SRType.EXT);

        Assert.assertEquals(expectedListOfSrs.size(), allLocalSrForType.size());
        Assert.assertEquals(expectedListOfSrs.get(0), allLocalSrForType.get(0));
    }

    @Test
    public void getAllLocalSrForTypeNoSrsFoundTest() throws XenAPIException, XmlRpcException {
        Connection connectionMock = Mockito.mock(Connection.class);
        List<SR> allLocalSrForType = citrixResourceBase.getAllLocalSrForType(connectionMock, SRType.EXT);
        Assert.assertTrue(allLocalSrForType.isEmpty());
    }

    @Test
    public void getAllLocalSrsTest() throws XenAPIException, XmlRpcException {
        Connection connectionMock = Mockito.mock(Connection.class);
        SR sr1 = Mockito.mock(SR.class);
        List<SR> srsExt = new ArrayList<>();
        srsExt.add(sr1);

        SR sr2 = Mockito.mock(SR.class);
        List<SR> srsLvm = new ArrayList<>();
        srsLvm.add(sr2);

        Mockito.doReturn(srsExt).when(citrixResourceBase).getAllLocalSrForType(connectionMock, SRType.EXT);
        Mockito.doReturn(srsLvm).when(citrixResourceBase).getAllLocalSrForType(connectionMock, SRType.LVM);

        List<SR> allLocalSrs = citrixResourceBase.getAllLocalSrs(connectionMock);

        Assert.assertEquals(srsExt.size() + srsLvm.size(), allLocalSrs.size());
        Assert.assertEquals(srsExt.get(0), allLocalSrs.get(1));
        Assert.assertEquals(srsLvm.get(0), allLocalSrs.get(0));

        InOrder inOrder = Mockito.inOrder(citrixResourceBase);
        inOrder.verify(citrixResourceBase).getAllLocalSrForType(connectionMock, SRType.LVM);
        inOrder.verify(citrixResourceBase).getAllLocalSrForType(connectionMock, SRType.EXT);
    }

    @Test
    public void createStoragePoolInfoTest() throws XenAPIException, XmlRpcException {
        Connection connectionMock = Mockito.mock(Connection.class);
        Host hostMock = Mockito.mock(Host.class);
        SR srMock = Mockito.mock(SR.class);

        String hostAddress = "hostAddress";
        Mockito.when(hostMock.getAddress(connectionMock)).thenReturn(hostAddress);

        String hostUuid = "hostUuid";
        citrixResourceBase._host.setUuid(hostUuid);

        PowerMockito.mockStatic(Host.class);
        PowerMockito.when(Host.getByUuid(connectionMock, hostUuid)).thenReturn(hostMock);

        String srType = "ext";
        String srUuid = "srUuid";
        long srPhysicalSize = 100l;
        long physicalUtilization = 10l;

        Mockito.when(srMock.getPhysicalSize(connectionMock)).thenReturn(srPhysicalSize);
        Mockito.when(srMock.getUuid(connectionMock)).thenReturn(srUuid);
        Mockito.when(srMock.getPhysicalUtilisation(connectionMock)).thenReturn(physicalUtilization);
        Mockito.when(srMock.getType(connectionMock)).thenReturn(srType);

        StoragePoolInfo storagePoolInfo = citrixResourceBase.createStoragePoolInfo(connectionMock, srMock);

        Assert.assertEquals(srUuid, storagePoolInfo.getUuid());
        Assert.assertEquals(hostAddress, storagePoolInfo.getHost());
        Assert.assertEquals(srType.toUpperCase(), storagePoolInfo.getHostPath());
        Assert.assertEquals(srType.toUpperCase(), storagePoolInfo.getLocalPath());
        Assert.assertEquals(StoragePoolType.EXT, storagePoolInfo.getPoolType());
        Assert.assertEquals(srPhysicalSize, storagePoolInfo.getCapacityBytes());
        Assert.assertEquals(srPhysicalSize - physicalUtilization, storagePoolInfo.getAvailableBytes());
    }

    @Test
    public void configureStorageNameAndDescriptionTest() throws XenAPIException, XmlRpcException {
        String nameFormat = "Cloud Stack Local (%s) Storage Pool for %s";

        String hostUuid = "hostUuid";
        citrixResourceBase._host.setUuid(hostUuid);

        Connection connectionMock = Mockito.mock(Connection.class);
        SR srMock = Mockito.mock(SR.class);

        String srUuid = "srUuid";
        String srType = "ext";
        String expectedNameDescription = String.format(nameFormat, srType, hostUuid);

        Mockito.when(srMock.getUuid(connectionMock)).thenReturn(srUuid);
        Mockito.when(srMock.getType(connectionMock)).thenReturn(srType);

        Mockito.doNothing().when(srMock).setNameLabel(connectionMock, srUuid);
        Mockito.doNothing().when(srMock).setNameDescription(connectionMock, expectedNameDescription);

        citrixResourceBase.configureStorageNameAndDescription(connectionMock, srMock);

        Mockito.verify(srMock).setNameLabel(connectionMock, srUuid);
        Mockito.verify(srMock).setNameDescription(connectionMock, expectedNameDescription);
    }

    @Test
    public void createStartUpStorageCommandTest() throws XenAPIException, XmlRpcException {
        String hostUuid = "hostUUid";
        citrixResourceBase._host.setUuid(hostUuid);
        citrixResourceBase._dcId = 1;

        Connection connectionMock = Mockito.mock(Connection.class);
        SR srMock = Mockito.mock(SR.class);

        StoragePoolInfo storagePoolInfoMock = Mockito.mock(StoragePoolInfo.class);

        Mockito.doNothing().when(citrixResourceBase).configureStorageNameAndDescription(connectionMock, srMock);
        Mockito.doReturn(storagePoolInfoMock).when(citrixResourceBase).createStoragePoolInfo(connectionMock, srMock);

        StartupStorageCommand startUpStorageCommand = citrixResourceBase.createStartUpStorageCommand(connectionMock, srMock);

        Assert.assertEquals(hostUuid, startUpStorageCommand.getGuid());
        Assert.assertEquals(storagePoolInfoMock, startUpStorageCommand.getPoolInfo());
        Assert.assertEquals(citrixResourceBase._dcId + "", startUpStorageCommand.getDataCenter());
        Assert.assertEquals(StorageResourceType.STORAGE_POOL, startUpStorageCommand.getResourceType());
    }

    @Test
    public void initializeLocalSrTest() throws XenAPIException, XmlRpcException {
        Connection connectionMock = Mockito.mock(Connection.class);

        List<SR> srsMocks = new ArrayList<>();
        SR srMock1 = Mockito.mock(SR.class);
        SR srMock2 = Mockito.mock(SR.class);

        Mockito.when(srMock1.getPhysicalSize(connectionMock)).thenReturn(0l);
        Mockito.when(srMock2.getPhysicalSize(connectionMock)).thenReturn(100l);
        srsMocks.add(srMock1);
        srsMocks.add(srMock2);

        Mockito.doReturn(srsMocks).when(citrixResourceBase).getAllLocalSrs(connectionMock);

        StartupStorageCommand startupStorageCommandMock = Mockito.mock(StartupStorageCommand.class);
        Mockito.doReturn(startupStorageCommandMock).when(citrixResourceBase).createStartUpStorageCommand(Mockito.eq(connectionMock), Mockito.any(SR.class));

        List<StartupStorageCommand> startUpCommandsForLocalStorage = citrixResourceBase.initializeLocalSrs(connectionMock);

        Mockito.verify(citrixResourceBase, Mockito.times(0)).createStartUpStorageCommand(connectionMock, srMock1);
        Mockito.verify(citrixResourceBase, Mockito.times(1)).createStartUpStorageCommand(connectionMock, srMock2);

        Assert.assertEquals(1, startUpCommandsForLocalStorage.size());
    }

    @Test
    public void syncPlatformAndCoresPerSocketSettingsEmptyCoresPerSocket() {
        String coresPerSocket = null;
        Map<String, String> platform = Mockito.mock(Map.class);
        citrixResourceBase.syncPlatformAndCoresPerSocketSettings(coresPerSocket, platform);
        Mockito.verify(platform, Mockito.never()).put(Mockito.any(), Mockito.any());
        Mockito.verify(platform, Mockito.never()).remove(Mockito.any());
    }

    @Test
    public void syncPlatformAndCoresPerSocketSettingsEmptyCoresPerSocketOnPlatform() {
        String coresPerSocket = "2";
        Map<String, String> platform = StringUtils.stringToMap(platformString);
        citrixResourceBase.syncPlatformAndCoresPerSocketSettings(coresPerSocket, platform);
        Assert.assertTrue(platform.containsKey(PLATFORM_CORES_PER_SOCKET_KEY));
        Assert.assertEquals(coresPerSocket, platform.get(PLATFORM_CORES_PER_SOCKET_KEY));
    }

    @Test
    public void syncPlatformAndCoresPerSocketSettingsUpdateCoresPerSocketOnPlatform() {
        String coresPerSocket = "2";
        String platformStr = platformString + "," + PLATFORM_CORES_PER_SOCKET_KEY + ":3";
        Map<String, String> platform = StringUtils.stringToMap(platformStr);
        citrixResourceBase.syncPlatformAndCoresPerSocketSettings(coresPerSocket, platform);
        Assert.assertTrue(platform.containsKey(PLATFORM_CORES_PER_SOCKET_KEY));
        Assert.assertEquals(coresPerSocket, platform.get(PLATFORM_CORES_PER_SOCKET_KEY));
    }
}
