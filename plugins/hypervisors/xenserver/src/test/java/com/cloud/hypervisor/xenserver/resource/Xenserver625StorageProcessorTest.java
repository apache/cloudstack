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
package com.cloud.hypervisor.xenserver.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.PBD.Record;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types.InternalError;
import com.xensource.xenapi.Types.XenAPIException;

@RunWith(MockitoJUnitRunner.class)
public class Xenserver625StorageProcessorTest {

    @InjectMocks
    private Xenserver625StorageProcessor xenserver625StorageProcessor;

    @Mock
    private CitrixResourceBase citrixResourceBase;

    @Mock
    private Connection connectionMock;

    private String pathMock = "pathMock";

    @Before
    public void before() {
        xenserver625StorageProcessor = Mockito.spy(new Xenserver625StorageProcessor(citrixResourceBase));
        citrixResourceBase._host = Mockito.mock(XsHost.class);
        Mockito.when(citrixResourceBase.getHost()).thenReturn(citrixResourceBase._host);
    }

    @Test
    public void makeDirectoryTestCallHostPlugingReturningEmpty() {
        boolean makeDirectoryReturn = configureAndExecuteMakeDirectoryMethodForTest(pathMock, StringUtils.EMPTY);

        assertFalse(makeDirectoryReturn);
    }

    @Test
    public void makeDirectoryTestCallHostPlugingReturningNull() {
        boolean makeDirectoryReturn = configureAndExecuteMakeDirectoryMethodForTest(pathMock, null);

        assertFalse(makeDirectoryReturn);
    }

    @Test
    public void makeDirectoryTestCallHostPlugingReturningSomething() {
        boolean makeDirectoryReturn = configureAndExecuteMakeDirectoryMethodForTest(pathMock, "/fictitious/path/to/use/in/unit/test");

        assertTrue(makeDirectoryReturn);
    }

    private boolean configureAndExecuteMakeDirectoryMethodForTest(String path, String returnMakeDirectory) {
        Mockito.when(citrixResourceBase.callHostPlugin(connectionMock, "cloud-plugin-storage", "makeDirectory", "path", path)).thenReturn(returnMakeDirectory);
        return xenserver625StorageProcessor.makeDirectory(connectionMock, path);
    }

    @Test(expected = CloudRuntimeException.class)
    public void createFileSRTestNoSrRetrieveNoSrCreated() {
        Mockito.doReturn(null).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
        Mockito.doReturn(null).when(xenserver625StorageProcessor).createNewFileSr(connectionMock, pathMock);

        xenserver625StorageProcessor.createFileSR(connectionMock, pathMock);
    }

    @Test
    public void createFileSRTestSrAlreadyConfigured() {
        SR srMockRetrievedMethod = Mockito.mock(SR.class);

        Mockito.doReturn(srMockRetrievedMethod).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);

        SR methodCreateFileSrResult = xenserver625StorageProcessor.createFileSR(connectionMock, pathMock);

        InOrder inOrder = Mockito.inOrder(xenserver625StorageProcessor);
        inOrder.verify(xenserver625StorageProcessor, times(1)).retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
        inOrder.verify(xenserver625StorageProcessor, times(0)).createNewFileSr(connectionMock, pathMock);

        Assert.assertEquals(srMockRetrievedMethod, methodCreateFileSrResult);
    }

    @Test
    public void createFileSRTestSrNotConfiguredAlreadyCreatingSr() {
        SR srMockCreateMethod = Mockito.mock(SR.class);

        Mockito.doReturn(null).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
        Mockito.doReturn(srMockCreateMethod).when(xenserver625StorageProcessor).createNewFileSr(connectionMock, pathMock);

        SR methodCreateFileSrResult = xenserver625StorageProcessor.createFileSR(connectionMock, pathMock);

        InOrder inOrder = Mockito.inOrder(xenserver625StorageProcessor);
        inOrder.verify(xenserver625StorageProcessor, times(1)).retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
        inOrder.verify(xenserver625StorageProcessor, times(1)).createNewFileSr(connectionMock, pathMock);

        Assert.assertEquals(srMockCreateMethod, methodCreateFileSrResult);
    }

    @Test(expected = CloudRuntimeException.class)
    public void retrieveAlreadyConfiguredSrWithoutExceptionThrowingXenAPIException() throws XenAPIException, XmlRpcException {
        Mockito.doThrow(XenAPIException.class).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSr(connectionMock, pathMock);

        xenserver625StorageProcessor.retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void retrieveAlreadyConfiguredSrWithoutExceptionThrowingXmlRpcException() throws XenAPIException, XmlRpcException {
        Mockito.doThrow(XmlRpcException.class).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSr(connectionMock, pathMock);

        xenserver625StorageProcessor.retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
    }

    @Test(expected = RuntimeException.class)
    public void retrieveAlreadyConfiguredSrWithoutExceptionThrowingOtherException() throws XenAPIException, XmlRpcException {
        Mockito.doThrow(RuntimeException.class).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSr(connectionMock, pathMock);

        xenserver625StorageProcessor.retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
    }

    @Test
    public void retrieveAlreadyConfiguredSrWithoutExceptionMethodWorking() throws XenAPIException, XmlRpcException {
        SR srMock = Mockito.mock(SR.class);
        Mockito.doReturn(srMock).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSr(connectionMock, pathMock);

        SR sr = xenserver625StorageProcessor.retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);

        Mockito.verify(xenserver625StorageProcessor).retrieveAlreadyConfiguredSr(connectionMock, pathMock);
        Assert.assertEquals(srMock, sr);
    }

    @Test
    public void retrieveAlreadyConfiguredSrTestNoSrFound() throws XenAPIException, XmlRpcException {
        try (MockedStatic<SR> srMocked = Mockito.mockStatic(SR.class)) {
            Mockito.when(SR.getByNameLabel(connectionMock, pathMock)).thenReturn(null);
            SR sr = xenserver625StorageProcessor.retrieveAlreadyConfiguredSr(connectionMock, pathMock);

            srMocked.verify(() -> SR.getByNameLabel(connectionMock, pathMock), times(1));
            Assert.assertNull(sr);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void retrieveAlreadyConfiguredSrTestMultipleSrsFound() throws XenAPIException, XmlRpcException {
        HashSet<SR> srs = new HashSet<>();
        srs.add(Mockito.mock(SR.class));
        srs.add(Mockito.mock(SR.class));

        try (MockedStatic<SR> ignored = Mockito.mockStatic(SR.class)) {
            Mockito.when(SR.getByNameLabel(connectionMock, pathMock)).thenReturn(srs);

            xenserver625StorageProcessor.retrieveAlreadyConfiguredSr(connectionMock, pathMock);
        }
    }

    @Test
    public void retrieveAlreadyConfiguredSrTestSrFailsSanityCheckWithXenAPIException() throws XenAPIException, XmlRpcException {
        configureAndExecuteMethodRetrieveAlreadyConfiguredSrTestSrFailsSanityCheckForException(XenAPIException.class);
    }

    @Test
    public void retrieveAlreadyConfiguredSrTestSrFailsSanityCheckWithXmlRpcException() throws XenAPIException, XmlRpcException {
        configureAndExecuteMethodRetrieveAlreadyConfiguredSrTestSrFailsSanityCheckForException(XmlRpcException.class);
    }

    @Test(expected = RuntimeException.class)
    public void retrieveAlreadyConfiguredSrTestSrFailsSanityCheckWithRuntimeException() throws XenAPIException, XmlRpcException {
        configureAndExecuteMethodRetrieveAlreadyConfiguredSrTestSrFailsSanityCheckForException(RuntimeException.class);
    }

    private void configureAndExecuteMethodRetrieveAlreadyConfiguredSrTestSrFailsSanityCheckForException(Class<? extends Throwable> exceptionClass) throws XenAPIException, XmlRpcException {
        SR srMock = Mockito.mock(SR.class);
        Mockito.doThrow(exceptionClass).when(srMock).scan(connectionMock);

        HashSet<SR> srs = new HashSet<>();
        srs.add(srMock);

        try (MockedStatic<SR> ignored = Mockito.mockStatic(SR.class)) {
            Mockito.when(SR.getByNameLabel(connectionMock, pathMock)).thenReturn(srs);
            Mockito.doNothing().when(xenserver625StorageProcessor).forgetSr(connectionMock, srMock);

            SR sr = xenserver625StorageProcessor.retrieveAlreadyConfiguredSr(connectionMock, pathMock);

            Assert.assertNull(sr);
            Mockito.verify(xenserver625StorageProcessor).forgetSr(connectionMock, srMock);
        }
    }

    @Test
    public void methodRetrieveAlreadyConfiguredSrTestSrScanSucceeds() throws XenAPIException, XmlRpcException {
        SR srMock = Mockito.mock(SR.class);
        Mockito.doNothing().when(srMock).scan(connectionMock);

        HashSet<SR> srs = new HashSet<>();
        srs.add(srMock);

        try (MockedStatic<SR> ignored = Mockito.mockStatic(SR.class)) {
            Mockito.when(SR.getByNameLabel(connectionMock, pathMock)).thenReturn(srs);

            SR sr = xenserver625StorageProcessor.retrieveAlreadyConfiguredSr(connectionMock, pathMock);

            Assert.assertEquals(srMock, sr);
            Mockito.verify(xenserver625StorageProcessor, times(0)).forgetSr(connectionMock, srMock);
        }
    }

    @Test
    public void forgetSrTest() throws XenAPIException, XmlRpcException {
        PBD pbdMock = Mockito.mock(PBD.class);
        Set<PBD> pbds = new HashSet<>();
        pbds.add(pbdMock);

        SR srMock = Mockito.mock(SR.class);
        Mockito.when(srMock.getPBDs(connectionMock)).thenReturn(pbds);

        Mockito.doNothing().when(xenserver625StorageProcessor).unplugPbd(connectionMock, pbdMock);

        xenserver625StorageProcessor.forgetSr(connectionMock, srMock);
        Mockito.verify(srMock).getPBDs(connectionMock);
        Mockito.verify(xenserver625StorageProcessor, times(1)).unplugPbd(connectionMock, pbdMock);
        Mockito.verify(srMock).forget(connectionMock);
    }

    @Test
    public void unplugPbdTest() throws XenAPIException, XmlRpcException {
        PBD pbdMock = Mockito.mock(PBD.class);

        xenserver625StorageProcessor.unplugPbd(connectionMock, pbdMock);

        Mockito.verify(pbdMock).getUuid(connectionMock);
        Mockito.verify(pbdMock).unplug(connectionMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void unplugPbdTestThrowXenAPIException() throws XenAPIException, XmlRpcException {
        prepareAndExecuteUnplugMethodForException(XenAPIException.class);
    }

    @Test(expected = CloudRuntimeException.class)
    public void unplugPbdTestThrowXmlRpcException() throws XenAPIException, XmlRpcException {
        prepareAndExecuteUnplugMethodForException(XmlRpcException.class);
    }

    @Test(expected = RuntimeException.class)
    public void unplugPbdTestThrowRuntimeException() throws XenAPIException, XmlRpcException {
        prepareAndExecuteUnplugMethodForException(RuntimeException.class);
    }

    private void prepareAndExecuteUnplugMethodForException(Class<? extends Throwable> exceptionClass) throws XenAPIException, XmlRpcException {
        PBD pbdMock = Mockito.mock(PBD.class);
        Mockito.doThrow(exceptionClass).when(pbdMock).unplug(connectionMock);
        xenserver625StorageProcessor.unplugPbd(connectionMock, pbdMock);
    }

    @Test
    public void createNewFileSrTestThrowingXenAPIException() throws XenAPIException, XmlRpcException {
        prepareAndExecuteTestcreateNewFileSrTestThrowingException(XenAPIException.class);
    }

    @Test
    public void createNewFileSrTestThrowingXmlRpcException() throws XenAPIException, XmlRpcException {
        prepareAndExecuteTestcreateNewFileSrTestThrowingException(XmlRpcException.class);
    }

    @Test(expected = RuntimeException.class)
    public void createNewFileSrTestThrowingRuntimeException() throws XenAPIException, XmlRpcException {
        prepareAndExecuteTestcreateNewFileSrTestThrowingException(RuntimeException.class);
    }

    private void prepareAndExecuteTestcreateNewFileSrTestThrowingException(Class<? extends Throwable> exceptionClass) throws XenAPIException, XmlRpcException {
        String uuid = "hostUuid";
        Mockito.when(citrixResourceBase._host.getUuid()).thenReturn(uuid);

        String srUuid = UUID.nameUUIDFromBytes(pathMock.getBytes()).toString();

        Host hostMock = Mockito.mock(Host.class);

        try (MockedStatic<Host> ignored = Mockito.mockStatic(
                Host.class); MockedStatic<SR> ignored1 = Mockito.mockStatic(SR.class)) {
            Mockito.when(Host.getByUuid(connectionMock, uuid)).thenReturn(hostMock);
            Mockito.when(SR.introduce(Mockito.eq(connectionMock), Mockito.eq(srUuid), Mockito.eq(pathMock),
                    Mockito.eq(pathMock), Mockito.eq("file"), Mockito.eq("file"), Mockito.eq(false),
                    Mockito.anyMap())).thenThrow(Mockito.mock(exceptionClass));


            SR sr = xenserver625StorageProcessor.createNewFileSr(connectionMock, pathMock);

            assertNull(sr);
            Mockito.verify(xenserver625StorageProcessor).removeSrAndPbdIfPossible(Mockito.eq(connectionMock),
                    nullable(SR.class), nullable(PBD.class));
        }
    }

    @Test
    public void createNewFileSrTestThrowingDbUniqueException() throws XenAPIException, XmlRpcException {
        String uuid = "hostUuid";
        Mockito.when(citrixResourceBase._host.getUuid()).thenReturn(uuid);

        SR srMock = Mockito.mock(SR.class);
        Mockito.doReturn(srMock).when(xenserver625StorageProcessor).retrieveAlreadyConfiguredSrWithoutException(connectionMock, pathMock);
        String srUuid = UUID.nameUUIDFromBytes(pathMock.getBytes()).toString();

        Host hostMock = Mockito.mock(Host.class);

        try (MockedStatic<Host> ignored = Mockito.mockStatic(Host.class);MockedStatic<SR> ignored1 =
                Mockito.mockStatic(SR.class) ) {
            Mockito.when(Host.getByUuid(connectionMock, uuid)).thenReturn(hostMock);

            InternalError dbUniquenessException = new InternalError(
                    "message: Db_exn.Uniqueness_constraint_violation(\"SR\", \"uuid\", \"fd3edbcf-f142-83d1-3fcb-029ca2446b68\")");

            Mockito.when(SR.introduce(Mockito.eq(connectionMock), Mockito.eq(srUuid), Mockito.eq(pathMock),
                    Mockito.eq(pathMock), Mockito.eq("file"), Mockito.eq("file"), Mockito.eq(false),
                    Mockito.anyMap())).thenThrow(dbUniquenessException);

            SR sr = xenserver625StorageProcessor.createNewFileSr(connectionMock, pathMock);

            Assert.assertEquals(srMock, sr);
            Mockito.verify(xenserver625StorageProcessor, times(0)).removeSrAndPbdIfPossible(Mockito.eq(connectionMock),
                    Mockito.any(SR.class), Mockito.any(PBD.class));
            Mockito.verify(xenserver625StorageProcessor).retrieveAlreadyConfiguredSrWithoutException(connectionMock,
                    pathMock);
        }
    }

    @Test
    public void createNewFileSrTest() throws XenAPIException, XmlRpcException {
        String uuid = "hostUuid";
        Mockito.when(citrixResourceBase._host.getUuid()).thenReturn(uuid);

        SR srMock = Mockito.mock(SR.class);
        String srUuid = UUID.nameUUIDFromBytes(pathMock.getBytes()).toString();

        Host hostMock = Mockito.mock(Host.class);

        try (MockedStatic<Host> ignored = Mockito.mockStatic(Host.class);MockedStatic<SR> ignored1 =
                Mockito.mockStatic(SR.class);MockedStatic<PBD> pbdMockedStatic = Mockito.mockStatic(PBD.class)) {
            Mockito.when(Host.getByUuid(connectionMock, uuid)).thenReturn(hostMock);


            Mockito.when(SR.introduce(Mockito.eq(connectionMock), Mockito.eq(srUuid), Mockito.eq(pathMock),
                    Mockito.eq(pathMock), Mockito.eq("file"), Mockito.eq("file"), Mockito.eq(false),
                    Mockito.anyMap())).thenReturn(srMock);

            PBD pbdMock = Mockito.mock(PBD.class);
            Mockito.when(PBD.create(Mockito.eq(connectionMock), Mockito.any(Record.class))).thenReturn(pbdMock);

            SR sr = xenserver625StorageProcessor.createNewFileSr(connectionMock, pathMock);

            Assert.assertEquals(srMock, sr);
            Mockito.verify(xenserver625StorageProcessor, times(0)).removeSrAndPbdIfPossible(Mockito.eq(connectionMock),
                    Mockito.any(SR.class), Mockito.any(PBD.class));
            Mockito.verify(xenserver625StorageProcessor, times(0)).retrieveAlreadyConfiguredSrWithoutException(
                    connectionMock, pathMock);

            Mockito.verify(srMock).scan(connectionMock);
            Mockito.verify(pbdMock).plug(connectionMock);

            SR.introduce(Mockito.eq(connectionMock), Mockito.eq(srUuid), Mockito.eq(pathMock), Mockito.eq(pathMock),
                    Mockito.eq("file"), Mockito.eq("file"), Mockito.eq(false),
                    Mockito.anyMap());

            pbdMockedStatic.verify(() -> PBD.create(Mockito.eq(connectionMock), Mockito.any(Record.class)));
        }
    }

    @Test
    public void removeSrAndPbdIfPossibleBothPbdAndSrNotNull() {
        SR srMock = Mockito.mock(SR.class);
        Mockito.doNothing().when(xenserver625StorageProcessor).forgetSr(connectionMock, srMock);

        PBD pbdMock = Mockito.mock(PBD.class);
        Mockito.doNothing().when(xenserver625StorageProcessor).unplugPbd(connectionMock, pbdMock);

        xenserver625StorageProcessor.removeSrAndPbdIfPossible(connectionMock, srMock, pbdMock);

        Mockito.verify(xenserver625StorageProcessor).forgetSr(connectionMock, srMock);
        Mockito.verify(xenserver625StorageProcessor).unplugPbd(connectionMock, pbdMock);

    }

    @Test
    public void removeSrAndPbdIfPossiblePbdNullAndSrNotNull() {
        SR srMock = Mockito.mock(SR.class);
        Mockito.doNothing().when(xenserver625StorageProcessor).forgetSr(connectionMock, srMock);

        xenserver625StorageProcessor.removeSrAndPbdIfPossible(connectionMock, srMock, null);

        Mockito.verify(xenserver625StorageProcessor).forgetSr(connectionMock, srMock);
        Mockito.verify(xenserver625StorageProcessor, times(0)).unplugPbd(Mockito.eq(connectionMock), Mockito.any(PBD.class));

    }

    @Test
    public void removeSrAndPbdIfPossiblePbdNotNullButSrNull() {
        PBD pbdMock = Mockito.mock(PBD.class);
        Mockito.doNothing().when(xenserver625StorageProcessor).unplugPbd(connectionMock, pbdMock);

        xenserver625StorageProcessor.removeSrAndPbdIfPossible(connectionMock, null, pbdMock);

        Mockito.verify(xenserver625StorageProcessor, times(0)).forgetSr(Mockito.eq(connectionMock), Mockito.any(SR.class));
        Mockito.verify(xenserver625StorageProcessor).unplugPbd(connectionMock, pbdMock);

    }

    @Test
    public void removeSrAndPbdIfPossibleBothPbdAndSrNull() {
        xenserver625StorageProcessor.removeSrAndPbdIfPossible(connectionMock, null, null);

        Mockito.verify(xenserver625StorageProcessor, times(0)).forgetSr(Mockito.eq(connectionMock), Mockito.any(SR.class));
        Mockito.verify(xenserver625StorageProcessor, times(0)).unplugPbd(Mockito.eq(connectionMock), Mockito.any(PBD.class));

    }
}
