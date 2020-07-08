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

import java.util.List;

import org.apache.cloudstack.backup.BackupOffering;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class VeeamClientTest {

    private String adminUsername = "administrator";
    private String adminPassword = "password";
    private VeeamClient client;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9399);

    @Before
    public void setUp() throws Exception {
        wireMockRule.stubFor(post(urlMatching(".*/sessionMngr/.*"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("X-RestSvcSessionId", "some-session-auth-id")
                        .withBody("")));
        client = new VeeamClient("http://localhost:9399/api/", adminUsername, adminPassword, true, 60);
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
                        .withHeader("Content-Type", "application/xml")
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
}