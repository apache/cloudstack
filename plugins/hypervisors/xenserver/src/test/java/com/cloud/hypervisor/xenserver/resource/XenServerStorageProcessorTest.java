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

import static org.mockito.Mockito.when;

import java.util.List;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Connection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.to.SwiftTO;

public class XenServerStorageProcessorTest {
    @Test
    public void testOrderOfSwiftUplodScriptParamsWithoutStoragePolicy() {
        CitrixResourceBase resource = Mockito.mock(CitrixResourceBase.class);
        XenServerStorageProcessor mock = new XenServerStorageProcessor(resource);

        SwiftTO swift = Mockito.mock(SwiftTO.class);
        when(swift.getStoragePolicy()).thenReturn(null);

        String container = "sample-container-name";
        String ldir = "sample-ldir";
        String lfilename = "sample-lfilename";
        Boolean isISCSI = true;

        List<String> params = mock.getSwiftParams(swift, container, ldir, lfilename, isISCSI);

        // make sure the params not null and has correct number of items in it
        Assert.assertNotNull("params is null", params);
        Assert.assertTrue("Expected param list size is 18 but it was" + params.size(), params.size() == 18);

        // check the order of params
        Assert.assertEquals("unexpected param.", "op", params.get(0));
        Assert.assertEquals("unexpected param.", "upload", params.get(1));
        Assert.assertEquals("unexpected param.", "url", params.get(2));
        Assert.assertEquals("unexpected param.", swift.getUrl(), params.get(3));
        Assert.assertEquals("unexpected param.", "account", params.get(4));
        Assert.assertEquals("unexpected param.", swift.getAccount(), params.get(5));
        Assert.assertEquals("unexpected param.", "username", params.get(6));
        Assert.assertEquals("unexpected param.", swift.getUserName(), params.get(7));
        Assert.assertEquals("unexpected param.", "key", params.get(8));
        Assert.assertEquals("unexpected param.", swift.getKey(), params.get(9));
        Assert.assertEquals("unexpected param.", "container", params.get(10));
        Assert.assertEquals("unexpected param.", container, params.get(11));
        Assert.assertEquals("unexpected param.", "ldir", params.get(12));
        Assert.assertEquals("unexpected param.", ldir, params.get(13));
        Assert.assertEquals("unexpected param.", "lfilename", params.get(14));
        Assert.assertEquals("unexpected param.", lfilename, params.get(15));
        Assert.assertEquals("unexpected param.", "isISCSI", params.get(16));
        Assert.assertEquals("unexpected param.", isISCSI.toString(), params.get(17));
    }

    @Test
    public void testOrderOfSwiftUplodScriptParamsWithStoragePolicy() {
        CitrixResourceBase resource = Mockito.mock(CitrixResourceBase.class);
        XenServerStorageProcessor mock = new XenServerStorageProcessor(resource);

        SwiftTO swift = Mockito.mock(SwiftTO.class);
        when(swift.getStoragePolicy()).thenReturn("sample-storagepolicy");

        String container = "sample-container-name";
        String ldir = "sample-ldir";
        String lfilename = "sample-lfilename";
        Boolean isISCSI = true;

        List<String> params = mock.getSwiftParams(swift, container, ldir, lfilename, isISCSI);

        // make sure the params not null and has correct number of items in it
        Assert.assertNotNull("params is null", params);
        Assert.assertTrue("Expected param list size is 20 but it was" + params.size(), params.size() == 20);

        // check the order of params
        Assert.assertEquals("unexpected param.", "op", params.get(0));
        Assert.assertEquals("unexpected param.", "upload", params.get(1));
        Assert.assertEquals("unexpected param.", "url", params.get(2));
        Assert.assertEquals("unexpected param.", swift.getUrl(), params.get(3));
        Assert.assertEquals("unexpected param.", "account", params.get(4));
        Assert.assertEquals("unexpected param.", swift.getAccount(), params.get(5));
        Assert.assertEquals("unexpected param.", "username", params.get(6));
        Assert.assertEquals("unexpected param.", swift.getUserName(), params.get(7));
        Assert.assertEquals("unexpected param.", "key", params.get(8));
        Assert.assertEquals("unexpected param.", swift.getKey(), params.get(9));
        Assert.assertEquals("unexpected param.", "container", params.get(10));
        Assert.assertEquals("unexpected param.", container, params.get(11));
        Assert.assertEquals("unexpected param.", "ldir", params.get(12));
        Assert.assertEquals("unexpected param.", ldir, params.get(13));
        Assert.assertEquals("unexpected param.", "lfilename", params.get(14));
        Assert.assertEquals("unexpected param.", lfilename, params.get(15));
        Assert.assertEquals("unexpected param.", "isISCSI", params.get(16));
        Assert.assertEquals("unexpected param.", isISCSI.toString(), params.get(17));
        Assert.assertEquals("unexpected param.", "storagepolicy", params.get(18));
        Assert.assertEquals("unexpected param.", "sample-storagepolicy", params.get(19));
    }

    @Test
    public void testDestroySnapshotOnPrimaryWaitsForTheCancelledCopyToLetGoOfIt() throws Exception {
        CitrixResourceBase resource = Mockito.mock(CitrixResourceBase.class);
        XenServerStorageProcessor processor = Mockito.spy(new XenServerStorageProcessor(resource));
        Connection conn = Mockito.mock(Connection.class);
        VDI snapshot = Mockito.mock(VDI.class);
        Mockito.doReturn(snapshot).when(processor).getVDIbyUuid(conn, "snap-uuid");
        // the cancelled VDI.copy still holds the snapshot on the first attempt
        Mockito.doThrow(Mockito.mock(Types.OtherOperationInProgress.class)).doNothing().when(snapshot).destroy(conn);
        when(snapshot.getCurrentOperations(conn)).thenReturn(Collections.singletonMap("OpaqueRef:copy", Types.VdiOperations.COPY));

        Assert.assertTrue(processor.destroySnapshotOnPrimaryStorage(conn, "snap-uuid"));
        Mockito.verify(snapshot, Mockito.times(2)).destroy(conn);
    }

    @Test
    public void testDestroySnapshotOnPrimaryReportsAMissingSnapshot() {
        CitrixResourceBase resource = Mockito.mock(CitrixResourceBase.class);
        XenServerStorageProcessor processor = Mockito.spy(new XenServerStorageProcessor(resource));
        Connection conn = Mockito.mock(Connection.class);
        Mockito.doReturn(null).when(processor).getVDIbyUuid(conn, "gone");

        Assert.assertFalse(processor.destroySnapshotOnPrimaryStorage(conn, "gone"));
    }
}
