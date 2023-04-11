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

package org.apache.cloudstack.storage.datastore.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.storage.datastore.api.Volume;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOGatewayClientImplTest {
    private final int port = 443;
    private final int timeout = 30;
    private final int maxConnections = 50;
    private final String username = "admin";
    private final String password = "P@ssword123";
    private final String sessionKey = "YWRtaW46MTYyMzM0OTc4NDk0MTo2MWQ2NGQzZWJhMTVmYTVkNDIwNjZmOWMwZDg0ZGZmOQ";
    private ScaleIOGatewayClient client = null;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .httpsPort(port)
            .needClientAuth(false)
            .basicAdminAuthenticator(username, password)
            .bindAddress("localhost"));

    @Before
    public void setUp() throws Exception {
        wireMockRule.stubFor(get("/api/login")
                .willReturn(ok()
                        .withHeader("content-type", "application/json;charset=UTF-8")
                        .withBody(sessionKey)));

        client = new ScaleIOGatewayClientImpl("https://localhost/api", username, password, false, timeout, maxConnections);

        wireMockRule.stubFor(post("/api/types/Volume/instances")
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json;charset=UTF-8")
                        .withStatus(200)
                        .withBody("{\"id\":\"c948d0b10000000a\"}")));

        wireMockRule.stubFor(get("/api/instances/Volume::c948d0b10000000a")
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json;charset=UTF-8")
                        .withStatus(200)
                        .withBody("{\"storagePoolId\":\"4daaa55e00000000\",\"dataLayout\":\"MediumGranularity\",\"vtreeId\":\"657e289500000009\","
                                + "\"sizeInKb\":8388608,\"snplIdOfAutoSnapshot\":null,\"volumeType\":\"ThinProvisioned\",\"consistencyGroupId\":null,"
                                + "\"ancestorVolumeId\":null,\"notGenuineSnapshot\":false,\"accessModeLimit\":\"ReadWrite\",\"secureSnapshotExpTime\":0,"
                                + "\"useRmcache\":false,\"managedBy\":\"ScaleIO\",\"lockedAutoSnapshot\":false,\"lockedAutoSnapshotMarkedForRemoval\":false,"
                                + "\"autoSnapshotGroupId\":null,\"compressionMethod\":\"Invalid\",\"pairIds\":null,\"timeStampIsAccurate\":false,\"mappedSdcInfo\":null,"
                                + "\"retentionLevels\":[],\"snplIdOfSourceVolume\":null,\"volumeReplicationState\":\"UnmarkedForReplication\",\"replicationJournalVolume\":false,"
                                + "\"replicationTimeStamp\":0,\"originalExpiryTime\":0,\"creationTime\":1623335880,\"name\":\"testvolume\",\"id\":\"c948d0b10000000a\"}")));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testClientAuthSuccess() {
        Assert.assertNotNull(client);
        wireMockRule.verify(getRequestedFor(urlEqualTo("/api/login"))
                .withBasicAuth(new BasicCredentials(username, password)));

        wireMockRule.stubFor(get("/api/types/StoragePool/instances")
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json;charset=UTF-8")
                        .withStatus(200)
                        .withBody("")));

        client.listStoragePools();

        wireMockRule.verify(getRequestedFor(urlEqualTo("/api/types/StoragePool/instances"))
                .withBasicAuth(new BasicCredentials(username, sessionKey)));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testClientAuthFailure() throws Exception {
        wireMockRule.stubFor(get("/api/login")
                .willReturn(unauthorized()
                        .withHeader("content-type", "application/json;charset=UTF-8")
                        .withBody("")));

        new ScaleIOGatewayClientImpl("https://localhost/api", username, password, false, timeout, maxConnections);
    }

    @Test(expected = ServerApiException.class)
    public void testRequestTimeout() {
        Assert.assertNotNull(client);
        wireMockRule.verify(getRequestedFor(urlEqualTo("/api/login"))
                .withBasicAuth(new BasicCredentials(username, password)));

        wireMockRule.stubFor(get("/api/types/StoragePool/instances")
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json;charset=UTF-8")
                        .withStatus(200)
                        .withFixedDelay(2 * timeout * 1000)
                        .withBody("")));

        client.listStoragePools();
    }

    @Test
    public void testCreateSingleVolume() {
        Assert.assertNotNull(client);
        wireMockRule.verify(getRequestedFor(urlEqualTo("/api/login"))
                .withBasicAuth(new BasicCredentials(username, password)));

        final String volumeName = "testvolume";
        final String scaleIOStoragePoolId = "4daaa55e00000000";
        final int sizeInGb = 8;
        Volume scaleIOVolume = client.createVolume(volumeName, scaleIOStoragePoolId, sizeInGb, Storage.ProvisioningType.THIN);

        wireMockRule.verify(postRequestedFor(urlEqualTo("/api/types/Volume/instances"))
                .withBasicAuth(new BasicCredentials(username, sessionKey))
                .withRequestBody(containing("\"name\":\"" + volumeName + "\""))
                .withHeader("content-type", equalTo("application/json")));
        wireMockRule.verify(getRequestedFor(urlEqualTo("/api/instances/Volume::c948d0b10000000a"))
                .withBasicAuth(new BasicCredentials(username, sessionKey)));

        Assert.assertNotNull(scaleIOVolume);
        Assert.assertEquals(scaleIOVolume.getId(), "c948d0b10000000a");
        Assert.assertEquals(scaleIOVolume.getName(), volumeName);
        Assert.assertEquals(scaleIOVolume.getStoragePoolId(), scaleIOStoragePoolId);
        Assert.assertEquals(scaleIOVolume.getSizeInKb(), Long.valueOf(sizeInGb * 1024 * 1024));
        Assert.assertEquals(scaleIOVolume.getVolumeType(), Volume.VolumeType.ThinProvisioned);
    }

    @Test
    public void testCreateMultipleVolumes() {
        Assert.assertNotNull(client);
        wireMockRule.verify(getRequestedFor(urlEqualTo("/api/login"))
                .withBasicAuth(new BasicCredentials(username, password)));

        final String volumeNamePrefix = "testvolume_";
        final String scaleIOStoragePoolId = "4daaa55e00000000";
        final int sizeInGb = 8;
        final int volumesCount = 1000;

        for (int i = 1; i <= volumesCount; i++) {
            String volumeName = volumeNamePrefix + i;
            Volume scaleIOVolume = client.createVolume(volumeName, scaleIOStoragePoolId, sizeInGb, Storage.ProvisioningType.THIN);

            Assert.assertNotNull(scaleIOVolume);
            Assert.assertEquals(scaleIOVolume.getId(), "c948d0b10000000a");
            Assert.assertEquals(scaleIOVolume.getStoragePoolId(), scaleIOStoragePoolId);
            Assert.assertEquals(scaleIOVolume.getSizeInKb(), Long.valueOf(sizeInGb * 1024 * 1024));
            Assert.assertEquals(scaleIOVolume.getVolumeType(), Volume.VolumeType.ThinProvisioned);
        }

        wireMockRule.verify(volumesCount, postRequestedFor(urlEqualTo("/api/types/Volume/instances"))
                .withBasicAuth(new BasicCredentials(username, sessionKey))
                .withRequestBody(containing("\"name\":\"" + volumeNamePrefix))
                .withHeader("content-type", equalTo("application/json")));
        wireMockRule.verify(volumesCount, getRequestedFor(urlEqualTo("/api/instances/Volume::c948d0b10000000a"))
                .withBasicAuth(new BasicCredentials(username, sessionKey)));
    }
}
