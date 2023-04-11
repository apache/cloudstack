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

package org.apache.backup.networker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.cloudstack.backup.NetworkerBackupProvider.BACKUP_IDENTIFIER;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import com.cloud.vm.VMInstanceVO;
import com.github.tomakehurst.wiremock.client.VerificationException;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.backup.networker.NetworkerClient;
import org.apache.cloudstack.backup.networker.api.NetworkerBackup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class NetworkerClientTest {
    private final String adminUsername = "administrator";
    private final String adminPassword = "password";
    private final int port = 9399;
    private final String url =  "http://localhost:" + port + "/nwrestapi/v3";
    private NetworkerClient client;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    @Before
    public void setUp() throws Exception {
        wireMockRule.stubFor(get(urlMatching(".*")).withBasicAuth(adminUsername, adminPassword)
                        .willReturn(aResponse()
                        .withStatus(200)));
        client = new NetworkerClient(url, adminUsername, adminPassword, false, 60);
    }

    @Test
    public void testBasicAuthSuccess() {
        wireMockRule.stubFor(get(urlEqualTo("/nwrestapi/v3"))
                .willReturn(aResponse().withStatus(200)));
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3"))
                .withBasicAuth(new BasicCredentials(adminUsername, adminPassword)));
    }
    @Test(expected = VerificationException.class)
    public void testBasicAuthFailure() {
        wireMockRule.stubFor(get(urlEqualTo("/nwrestapi/v3"))
                .willReturn(aResponse().withStatus(200)));
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3"))
                .withBasicAuth(new BasicCredentials(adminUsername, "wrongPassword")));
    }
    @Test
    public void testListPolicies() {
        wireMockRule.stubFor(get(urlMatching(".*/protectionpolicies/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\n" +
                                "    \"count\": 3,\n" +
                                "    \"protectionPolicies\": [\n" +
                                "        {\n" +
                                "            \"comment\": \"-CSBKP-\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"http://localhost:9399/nwrestapi/v3/global/protectionpolicies/CSBRONZE\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"CSBRONZE\",\n" +
                                "            \"policyProtectionEnable\": true,\n" +
                                "            \"policyProtectionPeriod\": \"1 Days\",\n" +
                                "            \"resourceId\": {\n" +
                                "                \"id\": \"50.0.224.7.0.0.0.0.164.230.188.98.192.168.1.203\",\n" +
                                "                \"sequence\": 15\n" +
                                "            },\n" +
                                "            \"summaryNotification\": {\n" +
                                "                \"command\": \"nsrlog -f policy_notifications.log\",\n" +
                                "                \"executeOn\": \"Completion\"\n" +
                                "            },\n" +
                                "            \"workflows\": []\n" +
                                "        },\n" +
                                "        {\n" +
                                "            \"comment\": \"-CSBKP-\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"http://localhost:9399/nwrestapi/v3/global/protectionpolicies/CSGOLD\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"CSGOLD\",\n" +
                                "            \"policyProtectionEnable\": true,\n" +
                                "            \"policyProtectionPeriod\": \"1 Months\",\n" +
                                "            \"resourceId\": {\n" +
                                "                \"id\": \"52.0.224.7.0.0.0.0.164.230.188.98.192.168.1.203\",\n" +
                                "                \"sequence\": 37\n" +
                                "            },\n" +
                                "            \"summaryNotification\": {\n" +
                                "                \"command\": \"nsrlog -f policy_notifications.log\",\n" +
                                "                \"executeOn\": \"Completion\"\n" +
                                "            },\n" +
                                "            \"workflows\": []\n" +
                                "        },\n" +
                                "        {\n" +
                                "            \"comment\": \"-CSBKP-\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"http://localhost:9399/nwrestapi/v3/global/protectionpolicies/CSSILVER\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"CSSILVER\",\n" +
                                "            \"policyProtectionEnable\": true,\n" +
                                "            \"policyProtectionPeriod\": \"7 Days\",\n" +
                                "            \"resourceId\": {\n" +
                                "                \"id\": \"51.0.224.7.0.0.0.0.164.230.188.98.192.168.1.203\",\n" +
                                "                \"sequence\": 22\n" +
                                "            },\n" +
                                "            \"summaryNotification\": {\n" +
                                "                \"command\": \"nsrlog -f policy_notifications.log\",\n" +
                                "                \"executeOn\": \"Completion\"\n" +
                                "            },\n" +
                                "            \"workflows\": []\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}")));
        List<BackupOffering> policies = client.listPolicies();
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3/global/protectionpolicies/?q=comment:"+ BACKUP_IDENTIFIER)));
        Assert.assertEquals(3,policies.size());
        Assert.assertEquals("CSBRONZE",policies.get(0).getName());
        Assert.assertEquals("CSGOLD",policies.get(1).getName());
        Assert.assertEquals("CSSILVER",policies.get(2).getName());
    }
    @Test
    public void testListBackupForVM() {
        wireMockRule.stubFor(get(urlMatching(".*/backups/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\n" +
                                "    \"backups\": [\n" +
                                "        {\n" +
                                "            \"attributes\": [\n" +
                                "                {\n" +
                                "                    \"key\": \"*ACTUAL_HOST\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"cs-kvm-4\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*backup start time\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"1657607395\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*ss clone retention\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"          1657607395:          1657607395:-204083220\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"saveset features\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"CLIENT_SAVETIME\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"browseTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"clientHostname\": \"C1\",\n" +
                                "            \"clientId\": \"cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d\",\n" +
                                "            \"completionTime\": \"2022-07-12T09:29:57+03:00\",\n" +
                                "            \"creationTime\": \"2022-07-12T09:29:55+03:00\",\n" +
                                "            \"fileCount\": 5,\n" +
                                "            \"id\": \"6034732f-00000006-7acd14e3-62cd14e3-00871500-5a80015d\",\n" +
                                "            \"instances\": [\n" +
                                "                {\n" +
                                "                    \"clone\": false,\n" +
                                "                    \"id\": \"1657607395\",\n" +
                                "                    \"status\": \"Browsable\",\n" +
                                "                    \"volumeIds\": [\n" +
                                "                        \"2127369368\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"level\": \"Manual\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"http://localhost:9399/nwrestapi/v3/global/backups/6034732f-00000006-7acd14e3-62cd14e3-00871500-5a80015d\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"i-2-15-VM\",\n" +
                                "            \"retentionTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"saveTime\": \"2022-07-12T09:29:55+03:00\",\n" +
                                "            \"shortId\": \"2060260579\",\n" +
                                "            \"size\": {\n" +
                                "                \"unit\": \"Byte\",\n" +
                                "                \"value\": 658603164\n" +
                                "            },\n" +
                                "            \"type\": \"File\"\n" +
                                "        },\n" +
                                "        {\n" +
                                "            \"attributes\": [\n" +
                                "                {\n" +
                                "                    \"key\": \"*ACTUAL_HOST\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"cs-kvm-4\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*backup start time\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"1657592454\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*ss clone retention\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"          1657592455:          1657592455:-204068280\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"saveset features\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"CLIENT_SAVETIME\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"browseTime\": \"2152-02-29T13:11:10+02:00\",\n" +
                                "            \"clientHostname\": \"C1\",\n" +
                                "            \"clientId\": \"cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d\",\n" +
                                "            \"completionTime\": \"2022-07-12T05:20:59+03:00\",\n" +
                                "            \"creationTime\": \"2022-07-12T05:20:55+03:00\",\n" +
                                "            \"fileCount\": 5,\n" +
                                "            \"id\": \"98d29c5e-00000006-81ccda87-62ccda87-00801500-5a80015d\",\n" +
                                "            \"instances\": [\n" +
                                "                {\n" +
                                "                    \"clone\": false,\n" +
                                "                    \"id\": \"1657592455\",\n" +
                                "                    \"status\": \"Browsable\",\n" +
                                "                    \"volumeIds\": [\n" +
                                "                        \"12647424\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"level\": \"Manual\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"http://localhost:9399/nwrestapi/v3/global/backups/98d29c5e-00000006-81ccda87-62ccda87-00801500-5a80015d\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"i-2-15-VM\",\n" +
                                "            \"retentionTime\": \"2152-02-29T13:11:10+02:00\",\n" +
                                "            \"saveTime\": \"2022-07-12T05:20:54+03:00\",\n" +
                                "            \"shortId\": \"2177686151\",\n" +
                                "            \"size\": {\n" +
                                "                \"unit\": \"Byte\",\n" +
                                "                \"value\": 658632924\n" +
                                "            },\n" +
                                "            \"type\": \"File\"\n" +
                                "        },\n" +
                                "        {\n" +
                                "            \"attributes\": [\n" +
                                "                {\n" +
                                "                    \"key\": \"*ACTUAL_HOST\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"cs-kvm-4\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*backup start time\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"1657591323\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*ss clone retention\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"          1657591323:          1657591323:-204067148\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"saveset features\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"CLIENT_SAVETIME\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"browseTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"clientHostname\": \"C1\",\n" +
                                "            \"clientId\": \"cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d\",\n" +
                                "            \"completionTime\": \"2022-07-12T05:02:06+03:00\",\n" +
                                "            \"creationTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"fileCount\": 5,\n" +
                                "            \"id\": \"d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "            \"instances\": [\n" +
                                "                {\n" +
                                "                    \"clone\": false,\n" +
                                "                    \"id\": \"1657591323\",\n" +
                                "                    \"status\": \"Browsable\",\n" +
                                "                    \"volumeIds\": [\n" +
                                "                        \"12647424\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"level\": \"Manual\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"http://localhost:9399/nwrestapi/v3/global/backups/d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"i-2-15-VM\",\n" +
                                "            \"retentionTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"saveTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"shortId\": \"2228016667\",\n" +
                                "            \"size\": {\n" +
                                "                \"unit\": \"Byte\",\n" +
                                "                \"value\": 658580844\n" +
                                "            },\n" +
                                "            \"type\": \"File\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"count\": 3\n" +
                                "}")));
        VMInstanceVO backupedVM = new VMInstanceVO();
        backupedVM.setInstanceName("i-2-15-VM");
        List<String> backupsTaken = client.getBackupsForVm(backupedVM);
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3/global/backups/?q=name:"+backupedVM.getName())));
        Assert.assertEquals(3,backupsTaken.size());
        Assert.assertEquals("6034732f-00000006-7acd14e3-62cd14e3-00871500-5a80015d",backupsTaken.get(0));
        Assert.assertEquals("98d29c5e-00000006-81ccda87-62ccda87-00801500-5a80015d",backupsTaken.get(1));
        Assert.assertEquals("d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d",backupsTaken.get(2));
    }

    @Test(expected = java.lang.AssertionError.class)
    public void testRegisterBackupInvalid() {
        wireMockRule.stubFor(get(urlMatching(".*/backups/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\n" +
                                "    \"backups\": [\n" +
                                "        {\n" +
                                "            \"attributes\": [\n" +
                                "                {\n" +
                                "                    \"key\": \"*ACTUAL_HOST\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"cs-kvm-4\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*backup start time\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"1657591323\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*ss clone retention\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"          1657591323:          1657591323:-204067148\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"saveset features\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"CLIENT_SAVETIME\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"browseTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"clientHostname\": \"C1\",\n" +
                                "            \"clientId\": \"cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d\",\n" +
                                "            \"completionTime\": \"2022-07-12T05:02:06+03:00\",\n" +
                                "            \"creationTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"fileCount\": 5,\n" +
                                "            \"id\": \"d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "            \"instances\": [\n" +
                                "                {\n" +
                                "                    \"clone\": false,\n" +
                                "                    \"id\": \"1657591323\",\n" +
                                "                    \"status\": \"Browsable\",\n" +
                                "                    \"volumeIds\": [\n" +
                                "                        \"12647424\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"level\": \"Manual\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"https://192.168.1.203:9090/nwrestapi/v3/global/backups/d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"i-2-15-VM\",\n" +
                                "            \"retentionTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"saveTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"shortId\": \"2228016667\",\n" +
                                "            \"size\": {\n" +
                                "                \"unit\": \"Byte\",\n" +
                                "                \"value\": 658580844\n" +
                                "            },\n" +
                                "            \"type\": \"File\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"count\": 1\n" +
                                "}")));


        VMInstanceVO backupedVM = new VMInstanceVO();
        backupedVM.setInstanceName("some-random-vm");
        backupedVM.setUuid("some-random-uuid");
        backupedVM.setBackupOfferingId(0L);
        backupedVM.setDataCenterId(1);
        Date backupDate = new Date();
        BackupVO vmBackup = client.registerBackupForVm(backupedVM, backupDate, null);
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3/global/backups/?q=name:" + backupedVM.getName())));
        Assert.assertEquals("658580844", vmBackup.getSize().toString());
    }


        @Test
    public void testregisterBackupForVMwithSsid() {
        wireMockRule.stubFor(get(urlMatching(".*/backups/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\n" +
                                "    \"backups\": [\n" +
                                "        {\n" +
                                "            \"attributes\": [\n" +
                                "                {\n" +
                                "                    \"key\": \"*ACTUAL_HOST\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"cs-kvm-4\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*backup start time\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"1657591323\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*ss clone retention\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"          1657591323:          1657591323:-204067148\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"saveset features\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"CLIENT_SAVETIME\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"browseTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"clientHostname\": \"C1\",\n" +
                                "            \"clientId\": \"cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d\",\n" +
                                "            \"completionTime\": \"2022-07-12T05:02:06+03:00\",\n" +
                                "            \"creationTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"fileCount\": 5,\n" +
                                "            \"id\": \"d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "            \"instances\": [\n" +
                                "                {\n" +
                                "                    \"clone\": false,\n" +
                                "                    \"id\": \"1657591323\",\n" +
                                "                    \"status\": \"Browsable\",\n" +
                                "                    \"volumeIds\": [\n" +
                                "                        \"12647424\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"level\": \"Manual\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"https://192.168.1.203:9090/nwrestapi/v3/global/backups/d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"i-2-15-VM\",\n" +
                                "            \"retentionTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"saveTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"shortId\": \"2228016667\",\n" +
                                "            \"size\": {\n" +
                                "                \"unit\": \"Byte\",\n" +
                                "                \"value\": 658580844\n" +
                                "            },\n" +
                                "            \"type\": \"File\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"count\": 1\n" +
                                "}")));


        VMInstanceVO backupedVM = new VMInstanceVO();
        backupedVM.setInstanceName("i-2-15-VM");
        backupedVM.setUuid("some-random-uuid");
        backupedVM.setBackupOfferingId(0L);
        backupedVM.setDataCenterId(1);
        SimpleDateFormat formatterDateTime = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss");
        Long startTS=1657591323L;
        Instant instant = Instant.ofEpochSecond(startTS);
        Date backupDate = Date.from(instant);
        String saveTime = formatterDateTime.format(Date.from(instant));
        BackupVO vmBackup = client.registerBackupForVm(backupedVM,backupDate,startTS.toString());
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3/global/backups/?q=name:"+backupedVM.getName()+"+and+saveTime:'"+saveTime+"'")));
        Assert.assertEquals("658580844", vmBackup.getSize().toString());
        Assert.assertEquals("d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d",vmBackup.getExternalId());


    }

    @Test
    public void testregisterBackupForVMwithOutSsid() {
        wireMockRule.stubFor(get(urlMatching(".*/backups/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\n" +
                                "    \"backups\": [\n" +
                                "        {\n" +
                                "            \"attributes\": [\n" +
                                "                {\n" +
                                "                    \"key\": \"*ACTUAL_HOST\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"cs-kvm-4\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*backup start time\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"1657591323\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*ss clone retention\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"          1657591323:          1657591323:-204067148\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"saveset features\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"CLIENT_SAVETIME\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"browseTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"clientHostname\": \"C1\",\n" +
                                "            \"clientId\": \"cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d\",\n" +
                                "            \"completionTime\": \"2022-07-12T05:02:06+03:00\",\n" +
                                "            \"creationTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"fileCount\": 5,\n" +
                                "            \"id\": \"d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "            \"instances\": [\n" +
                                "                {\n" +
                                "                    \"clone\": false,\n" +
                                "                    \"id\": \"1657591323\",\n" +
                                "                    \"status\": \"Browsable\",\n" +
                                "                    \"volumeIds\": [\n" +
                                "                        \"12647424\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"level\": \"Manual\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"https://192.168.1.203:9090/nwrestapi/v3/global/backups/d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"i-2-15-VM\",\n" +
                                "            \"retentionTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"saveTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"shortId\": \"2228016667\",\n" +
                                "            \"size\": {\n" +
                                "                \"unit\": \"Byte\",\n" +
                                "                \"value\": 658580844\n" +
                                "            },\n" +
                                "            \"type\": \"File\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"count\": 1\n" +
                                "}")));


        VMInstanceVO backupedVM = new VMInstanceVO();
        backupedVM.setInstanceName("i-2-15-VM");
        backupedVM.setUuid("some-random-uuid");
        backupedVM.setBackupOfferingId(0L);
        backupedVM.setDataCenterId(1);
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
        Long startTS=1657591323L;
        Instant instant = Instant.ofEpochSecond(startTS);
        Date backupDate = Date.from(instant);
        String startDate = formatterDate.format(backupDate);
        String startTime = formatterTime.format(backupDate);
        String endDate = formatterDate.format(new Date());
        String endTime = formatterTime.format(new Date());
        final String searchRange = "['" + startDate + "T" + startTime + "'+TO+'" + endDate + "T" + endTime + "']";
        BackupVO vmBackup = client.registerBackupForVm(backupedVM,backupDate,null);
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3/global/backups/?q=name:"+backupedVM.getName()+"+and+saveTime:"+searchRange)));
        Assert.assertEquals("658580844", vmBackup.getSize().toString());
        Assert.assertEquals("d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d",vmBackup.getExternalId());
    }

    @Test
    public void testDeleteVMBackup() {
        wireMockRule.stubFor(delete(urlMatching(".*/backups/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(204)
                        .withBody("1")));

        String deleteBackupId = "d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d2";
        Boolean status = client.deleteBackupForVM(deleteBackupId);
        verify(deleteRequestedFor(urlEqualTo("/nwrestapi/v3/global/backups/" + deleteBackupId)));
        Assert.assertEquals(true, status);

    }

    @Test
    public void testNetworkerBackupInfo() {
        wireMockRule.stubFor(get(urlMatching(".*/backups/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\n" +
                                "    \"backups\": [\n" +
                                "        {\n" +
                                "            \"attributes\": [\n" +
                                "                {\n" +
                                "                    \"key\": \"*ACTUAL_HOST\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"cs-kvm-4\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*backup start time\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"1657591323\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"*ss clone retention\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"          1657591323:          1657591323:-204067148\"\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"key\": \"saveset features\",\n" +
                                "                    \"values\": [\n" +
                                "                        \"CLIENT_SAVETIME\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"browseTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"clientHostname\": \"C1\",\n" +
                                "            \"clientId\": \"cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d\",\n" +
                                "            \"completionTime\": \"2022-07-12T05:02:06+03:00\",\n" +
                                "            \"creationTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"fileCount\": 5,\n" +
                                "            \"id\": \"d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "            \"instances\": [\n" +
                                "                {\n" +
                                "                    \"clone\": false,\n" +
                                "                    \"id\": \"1657591323\",\n" +
                                "                    \"status\": \"Browsable\",\n" +
                                "                    \"volumeIds\": [\n" +
                                "                        \"12647424\"\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"level\": \"Manual\",\n" +
                                "            \"links\": [\n" +
                                "                {\n" +
                                "                    \"href\": \"https://192.168.1.203:9090/nwrestapi/v3/global/backups/d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d\",\n" +
                                "                    \"rel\": \"item\"\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"name\": \"i-2-15-VM\",\n" +
                                "            \"retentionTime\": \"2152-02-29T13:11:11+02:00\",\n" +
                                "            \"saveTime\": \"2022-07-12T05:02:03+03:00\",\n" +
                                "            \"shortId\": \"2228016667\",\n" +
                                "            \"size\": {\n" +
                                "                \"unit\": \"Byte\",\n" +
                                "                \"value\": 658580844\n" +
                                "            },\n" +
                                "            \"type\": \"File\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"count\": 1\n" +
                                "}")));


        String backupId="d371d629-00000006-84ccd61b-62ccd61b-007d1500-5a80015d";
        NetworkerBackup backup = client.getNetworkerBackupInfo(backupId);
        verify(getRequestedFor(urlEqualTo("/nwrestapi/v3/global/backups/?q=id:"+backupId)));
        Assert.assertEquals("658580844", backup.getSize().getValue().toString());
        Assert.assertEquals("cb2bf6eb-00000004-62c176f2-62c176f1-00021500-5a80015d", backup.getClientId());
        Assert.assertEquals("2022-07-12T05:02:03+03:00", backup.getSaveTime());
    }

}
