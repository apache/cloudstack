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

package org.apache.cloudstack.backup.veeam;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.List;

import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.veeam.api.RestoreSession;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class VeeamClientTest {

    private String adminUsername = "administrator";
    private String adminPassword = "password";
    private VeeamClient client;
    private VeeamClient mockClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9399);

    @Before
    public void setUp() throws Exception {
        wireMockRule.stubFor(post(urlMatching(".*/sessionMngr/.*"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("X-RestSvcSessionId", "some-session-auth-id")
                        .withBody("")));
        client = new VeeamClient("http://localhost:9399/api/", adminUsername, adminPassword, true, 60, 600);
        mockClient = Mockito.mock(VeeamClient.class);
        Mockito.when(mockClient.getRepositoryNameFromJob(Mockito.anyString())).thenCallRealMethod();
    }

    @Test
    public void testBasicAuth() {
        verify(postRequestedFor(urlMatching(".*/sessionMngr/.*"))
                .withBasicAuth(new BasicCredentials(adminUsername, adminPassword)));
    }

    @Test
    public void testVeeamJobs() {
        wireMockRule.stubFor(get(urlMatching(".*/jobs"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/xml")
                        .withStatus(200)
                        .withBody("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<EntityReferences xmlns=\"http://www.veeam.com/ent/v1.0\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                "    <Ref UID=\"urn:veeam:Job:8acac50d-3711-4c99-bf7b-76fe9c7e39c3\" Name=\"ZONE1-GOLD\" Href=\"http://10.1.1.10:9399/api/jobs/8acac50d-3711-4c99-bf7b-76fe9c7e39c3\" Type=\"JobReference\">\n" +
                                "        <Links>\n" +
                                "            <Link Href=\"http://10.1.1.10:9399/api/backupServers/1efaeae4-d23c-46cd-84a1-8798f68bdb78\" Name=\"10.1.1.10\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                                "            <Link Href=\"http://10.1.1.10:9399/api/jobs/8acac50d-3711-4c99-bf7b-76fe9c7e39c3?format=Entity\" Name=\"ZONE1-GOLD\" Type=\"Job\" Rel=\"Alternate\"/>\n" +
                                "            <Link Href=\"http://10.1.1.10:9399/api/jobs/8acac50d-3711-4c99-bf7b-76fe9c7e39c3/backupSessions\" Type=\"BackupJobSessionReferenceList\" Rel=\"Down\"/>\n" +
                                "        </Links>\n" +
                                "    </Ref>\n" +
                                "</EntityReferences>")));
        List<BackupOffering> policies = client.listJobs();
        verify(getRequestedFor(urlMatching(".*/jobs")));
        Assert.assertEquals(policies.size(), 1);
        Assert.assertEquals(policies.get(0).getName(), "ZONE1-GOLD");
    }

    @Test
    public void getRepositoryNameFromJobTestExceptionCmdWithoutResult() throws Exception {
        String backupName = "TEST-BACKUP";
        try {
            Mockito.doReturn(null).when(mockClient).executePowerShellCommands(Mockito.anyList());
            mockClient.getRepositoryNameFromJob(backupName);
            fail();
        } catch (Exception e) {
            Assert.assertEquals(CloudRuntimeException.class, e.getClass());
            Assert.assertEquals("Failed to get Repository Name from Job [name: TEST-BACKUP].", e.getMessage());
        }
    }

    @Test
    public void getRepositoryNameFromJobTestExceptionCmdWithFalseResult() {
        String backupName = "TEST-BACKUP2";
        Pair<Boolean, String> response = new Pair<Boolean, String>(Boolean.FALSE, "");
        Mockito.doReturn(response).when(mockClient).executePowerShellCommands(Mockito.anyList());
        try {
            mockClient.getRepositoryNameFromJob(backupName);
            fail();
        } catch (Exception e) {
            Assert.assertEquals(CloudRuntimeException.class, e.getClass());
            Assert.assertEquals("Failed to get Repository Name from Job [name: TEST-BACKUP2].", e.getMessage());
        }
    }

    @Test
    public void getRepositoryNameFromJobTestExceptionWhenResultIsInWrongFormat() {
        String backupName = "TEST-BACKUP3";
        Pair<Boolean, String> response = new Pair<Boolean, String>(Boolean.TRUE, "\nName:\n\nName-test");
        Mockito.doReturn(response).when(mockClient).executePowerShellCommands(Mockito.anyList());
        try {
            mockClient.getRepositoryNameFromJob(backupName);
            fail();
        } catch (Exception e) {
            Assert.assertEquals(CloudRuntimeException.class, e.getClass());
            Assert.assertEquals("Can't find any repository name for Job [name: TEST-BACKUP3].", e.getMessage());
        }
    }

    @Test
    public void getRepositoryNameFromJobTestSuccess() throws Exception {
        String backupName = "TEST-BACKUP3";
        Pair<Boolean, String> response = new Pair<Boolean, String>(Boolean.TRUE, "\n\nName : test");
        Mockito.doReturn(response).when(mockClient).executePowerShellCommands(Mockito.anyList());
        String repositoryNameFromJob = mockClient.getRepositoryNameFromJob(backupName);
        Assert.assertEquals("test", repositoryNameFromJob);
    }

    @Test
    public void checkIfRestoreSessionFinishedTestTimeoutException() throws IOException {
        try {
            ReflectionTestUtils.setField(mockClient, "restoreTimeout", 10);
            RestoreSession restoreSession = Mockito.mock(RestoreSession.class);
            HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
            Mockito.when(mockClient.get(Mockito.anyString())).thenReturn(httpResponse);
            Mockito.when(mockClient.parseRestoreSessionResponse(httpResponse)).thenReturn(restoreSession);
            Mockito.when(restoreSession.getResult()).thenReturn("No Success");
            Mockito.when(mockClient.checkIfRestoreSessionFinished(Mockito.eq("RestoreTest"), Mockito.eq("any"))).thenCallRealMethod();
            mockClient.checkIfRestoreSessionFinished("RestoreTest", "any");
            fail();
        } catch (Exception e) {
            Assert.assertEquals("Related job type: RestoreTest was not successful", e.getMessage());
        }
        Mockito.verify(mockClient, times(10)).get(Mockito.anyString());
    }
}
