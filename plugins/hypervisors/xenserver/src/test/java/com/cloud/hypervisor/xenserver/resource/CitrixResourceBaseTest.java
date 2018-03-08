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
import java.util.HashMap;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Host.Record;
import com.xensource.xenapi.Types.XenAPIException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Host.class, Script.class})
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

        Assert.assertEquals("xs-tools.iso", returnedIsoTemplateName);
    }

    @Test
    public void actualIsoTemplateTestXenServerBefore70() throws XenAPIException, XmlRpcException {
        hostRecordMock.softwareVersion.put("product_brand", "XenServer");
        hostRecordMock.softwareVersion.put("product_version", "6.0");

        String returnedIsoTemplateName = citrixResourceBase.getActualIsoTemplate(connectionMock);

        Assert.assertEquals("xs-tools.iso", returnedIsoTemplateName);
    }

    @Test
    public void actualIsoTemplateTestXenServer70() throws XenAPIException, XmlRpcException {
        hostRecordMock.softwareVersion.put("product_brand", "XenServer");
        hostRecordMock.softwareVersion.put("product_version", "7.0");

        String returnedIsoTemplateName = citrixResourceBase.getActualIsoTemplate(connectionMock);

        Assert.assertEquals("guest-tools.iso", returnedIsoTemplateName);
    }

    @Test
    public void actualIsoTemplateTestXenServer71() throws XenAPIException, XmlRpcException {
        hostRecordMock.softwareVersion.put("product_brand", "XenServer");
        hostRecordMock.softwareVersion.put("product_version", "7.1");

        String returnedIsoTemplateName = citrixResourceBase.getActualIsoTemplate(connectionMock);

        Assert.assertEquals("guest-tools.iso", returnedIsoTemplateName);
    }
}
