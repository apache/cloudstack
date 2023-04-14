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

package com.cloud.hypervisor.vmware.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VmwareContextTest {

    @Test
    public void testUploadResourceContentCharsetException() throws Exception {
        VmwareClient client = Mockito.mock(VmwareClient.class);
        String address = "10.1.1.1";
        VmwareContext vmwareContext = Mockito.spy(new VmwareContext(client, address));
        HttpURLConnection conn = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(Mockito.mock(OutputStream.class)).when(conn).getOutputStream();
        Mockito.doReturn(Mockito.mock(InputStream.class)).when(conn).getInputStream();
        Mockito.doReturn(conn).when(vmwareContext).getHTTPConnection("http://example.com", "PUT");
        //This method should not throw any exception. Ref: CLOUDSTACK-8669
        vmwareContext.uploadResourceContent("http://example.com", "content".getBytes());
    }

}
