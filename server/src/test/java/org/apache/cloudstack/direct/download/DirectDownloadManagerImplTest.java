//
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
//
package org.apache.cloudstack.direct.download;

import com.cloud.agent.AgentManager;
import com.cloud.host.dao.HostDao;
import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand.DownloadProtocol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DirectDownloadManagerImplTest {

    @Mock
    HostDao hostDao;
    @Mock
    AgentManager agentManager;

    @Spy
    @InjectMocks
    private DirectDownloadManagerImpl manager = new DirectDownloadManagerImpl();

    private static final String HTTP_HEADER_1 = "Content-Type";
    private static final String HTTP_VALUE_1 = "application/x-www-form-urlencoded";
    private static final String HTTP_HEADER_2 = "Accept-Encoding";
    private static final String HTTP_VALUE_2 = "gzip";

    @Before
    public void setUp() {
    }

    @Test
    public void testGetProtocolMetalink() {
        String url = "http://192.168.1.2/tmpl.metalink";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.METALINK, protocol);
    }

    @Test
    public void testGetProtocolHttp() {
        String url = "http://192.168.1.2/tmpl.qcow2";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.HTTP, protocol);
    }

    @Test
    public void testGetProtocolHttps() {
        String url = "https://192.168.1.2/tmpl.qcow2";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.HTTPS, protocol);
    }

    @Test
    public void testGetProtocolNfs() {
        String url = "nfs://192.168.1.2/tmpl.qcow2";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.NFS, protocol);
    }

    @Test
    public void testGetHeadersFromDetailsHttpHeaders() {
        Map<String, String> details = new HashMap<>();
        details.put("Message.ReservedCapacityFreed.Flag", "false");
        details.put(DirectDownloadManagerImpl.httpHeaderDetailKey + ":" + HTTP_HEADER_1, HTTP_VALUE_1);
        details.put(DirectDownloadManagerImpl.httpHeaderDetailKey + ":" + HTTP_HEADER_2, HTTP_VALUE_2);
        Map<String, String> headers = manager.getHeadersFromDetails(details);
        Assert.assertEquals(2, headers.keySet().size());
        Assert.assertTrue(headers.containsKey(HTTP_HEADER_1));
        Assert.assertTrue(headers.containsKey(HTTP_HEADER_2));
        Assert.assertEquals(HTTP_VALUE_1, headers.get(HTTP_HEADER_1));
        Assert.assertEquals(HTTP_VALUE_2, headers.get(HTTP_HEADER_2));
    }

    @Test
    public void testGetHeadersFromDetailsNonHttpHeaders() {
        Map<String, String> details = new HashMap<>();
        details.put("Message.ReservedCapacityFreed.Flag", "false");
        Map<String, String> headers = manager.getHeadersFromDetails(details);
        Assert.assertTrue(headers.isEmpty());
    }
}
