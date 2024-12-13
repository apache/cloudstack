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
package org.apache.cloudstack.backup.backroll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.cloudstack.backup.BackupService;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import org.apache.logging.log4j.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class BackrollClientTest {
    private BackrollClient client;

    @Mock
    private BackrollService mockBackrollService;


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9399);

    @Before
    public void setUp() throws Exception {
        mockBackrollService = mock(BackrollService.class);
        mockBackrollService.logger = Mockito.mock(Logger.class);
        client = new BackrollClient("http://localhost:5050/api/v1/", "backroll", "password", true, 300, 600, mockBackrollService);
    }

    @Test
    public void getAllBackupsfromVirtualMachine_test() throws Exception {
        String vmId = "TEST-vm_uuid";
        CloseableHttpResponse mockHttpResponse = mock(CloseableHttpResponse.class);
        CloseableHttpResponse mockHttpResponse2 = mock(CloseableHttpResponse.class);

        // Define mock behavior for first HTTP response
        String responseContent1 = "{\"Location\":\"/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782\"}";

        // Define mock behavior for second HTTP response
        String responseContent2 = "{ \"state\": \"SUCCESS\", \"info\": { \"archives\": [ { \"archive\": \"ROOT-00000\", \"barchive\": \"ROOT-00000\", \"id\": \"25d55ad283aa400af464c76d713c07ad7d163abdd3b8fbcdbdc46b827e5e0457\", \"name\": \"ROOT-00000\", \"start\": \"2024-11-08T18:24:48.000000\", \"time\": \"2024-11-08T18:24:48.000000\" } ], \"encryption\": { \"mode\": \"none\" }, \"repository\": { \"id\": \"36a11ebc0775a097c927735cc7015d19be7309be69fc15b896c5b1fd87fcbd79\", \"last_modified\": \"2024-11-29T09:53:09.000000\", \"location\": \"/mnt/backup/backup1\" } } }";

        // Mocking client responses
        doReturn(mockHttpResponse).when(mockBackrollService).get(Mockito.any(URI.class), Mockito.matches(".*/virtualmachines/.*"));
        doReturn(responseContent1).when(mockBackrollService).okBody(mockHttpResponse);
        doReturn(responseContent2).when(mockBackrollService).waitGet(Mockito.any(URI.class), Mockito.anyString());
        doReturn(mockHttpResponse2).when(mockBackrollService).get(Mockito.any(URI.class), Mockito.matches(".*/status/.*"));

        // Assuming getAllBackupsfromVirtualMachine belongs to a class named BackupService

        // Run the method under test
        List<BackrollVmBackup> backupsTestList = client.getAllBackupsfromVirtualMachine(vmId);

        // Check results
        assertEquals(1, backupsTestList.size());  // Should be 1 based on provided mock data

        // Optional: Verifications
        verify(mockHttpResponse).close();
    }

    // @Test
    // public void getAllBackupsfromVirtualMachine_test2() throws Exception {
    //     String vmId = "TEST-vm_uuid";
    //     CloseableHttpResponse mockHttpResponse = mock(CloseableHttpResponse.class);
    //     CloseableHttpResponse mockHttpResponse2 = mock(CloseableHttpResponse.class);
    //     StatusLine mockStatusLine = mock(StatusLine.class);
    //     HttpEntity mockEntity = mock(HttpEntity.class);
    //     HttpEntity mockEntity2 = mock(HttpEntity.class);
    //     //InputStream mockInputStream = new ByteArrayInputStream("{\"Location\":\"/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782\"}".getBytes());

    //     // Define mock behavior
    //     when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
    //     when(mockStatusLine.getStatusCode()).thenReturn(200);
    //     when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
    //     when(mockEntity.getContentLength()).thenReturn(400L);

    //     String responseContent = "{\"Location\":\"/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782\"}";
    //     InputStream stream = new ByteArrayInputStream(responseContent.getBytes());
    //     when(mockEntity.getContent()).thenReturn(stream);

    //     when(mockHttpResponse2.getStatusLine()).thenReturn(mockStatusLine);
    //     when(mockStatusLine.getStatusCode()).thenReturn(200);
    //     when(mockHttpResponse2.getEntity()).thenReturn(mockEntity2);
    //     when(mockEntity2.getContentLength()).thenReturn(400L);

    //     String responseContent2 = "{\r\n" + //
    //                     "    \"state\": \"SUCCESS\",\r\n" + //
    //                     "    \"info\": {\r\n" + //
    //                     "        \"archives\": [\r\n" + //
    //                     "            {\r\n" + //
    //                     "                \"archive\": \"ROOT-00000\",\r\n" + //
    //                     "                \"barchive\": \"ROOT-00000\",\r\n" + //
    //                     "                \"id\": \"25d55ad283aa400af464c76d713c07ad7d163abdd3b8fbcdbdc46b827e5e0457\",\r\n" + //
    //                     "                \"name\": \"ROOT-00000\",\r\n" + //
    //                     "                \"start\": \"2024-11-08T18:24:48.000000\",\r\n" + //
    //                     "                \"time\": \"2024-11-08T18:24:48.000000\"\r\n" + //
    //                     "            }\r\n" + //
    //                     "        ],\r\n" + //
    //                     "        \"encryption\": {\r\n" + //
    //                     "            \"mode\": \"none\"\r\n" + //
    //                     "        },\r\n" + //
    //                     "        \"repository\": {\r\n" + //
    //                     "            \"id\": \"36a11ebc0775a097c927735cc7015d19be7309be69fc15b896c5b1fd87fcbd79\",\r\n" + //
    //                     "            \"last_modified\": \"2024-11-29T09:53:09.000000\",\r\n" + //
    //                     "            \"location\": \"/mnt/backup/backup1\"\r\n" + //
    //                     "        }\r\n" + //
    //                     "    }\r\n" + //
    //                     "}";
    //     InputStream stream2 = new ByteArrayInputStream(responseContent2.getBytes());
    //     when(mockEntity2.getContent()).thenReturn(stream2);

    //     try {
    //         Mockito.doReturn(mockHttpResponse).when(mockClient).get(Mockito.matches(".*/virtualmachines/.*"));
    //         Mockito.doReturn(mockHttpResponse2).when(mockClient).get(Mockito.matches(".*/api/v1.*"));
    //         List<BackrollVmBackup> backupsTesList = mockClient.getAllBackupsfromVirtualMachine(vmId);
    //         Assert.assertTrue(backupsTesList.size() > 0);
    //         fail();
    //     } catch (Exception e) {
    //         Assert.assertEquals(CloudRuntimeException.class, e.getClass());
    //         Assert.assertEquals("Failed to get Repository Name from Job [name: TEST-BACKUP].", e.getMessage());
    //     }
    // }
}