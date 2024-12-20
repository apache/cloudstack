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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.model.response.BackrollTaskRequestResponse;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.backroll.model.response.archive.BackrollBackupsFromVMResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.backup.BackrollBackupMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.backup.BackupMetricsInfo;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.BackrollVmMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.CacheStats;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.InfosCache;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.MetricsInfos;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.VirtualMachineBackupsResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackrollBackupPolicyResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackupPoliciesResponse;
import org.apache.cloudstack.backup.backroll.utils.BackrollApiException;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClientProvider;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class BackrollClientTest {
    private BackrollClient client;

    @Mock
    private BackrollHttpClientProvider backrollHttpClientProviderMock;


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9399);

    @Before
    public void setUp() throws Exception {
        backrollHttpClientProviderMock = mock(BackrollHttpClientProvider.class);
        client = new BackrollClient(backrollHttpClientProviderMock);
        client.logger = Mockito.mock(Logger.class);
    }

    @Test
    public void getAllBackupsfromVirtualMachine_test() throws Exception {
        String vmId = "TEST-vm_uuid";
        String virtualMachineResponseString = "{ \"state\": \"SUCCESS\", \"info\": { \"archives\": [ { \"archive\": \"ROOT-00000\", \"barchive\": \"ROOT-00000\", \"id\": \"25d55ad283aa400af464c76d713c07ad7d163abdd3b8fbcdbdc46b827e5e0457\", \"name\": \"ROOT-00000\", \"start\": \"2024-11-08T18:24:48.000000\", \"time\": \"2024-11-08T18:24:48.000000\" } ], \"encryption\": { \"mode\": \"none\" }, \"repository\": { \"id\": \"36a11ebc0775a097c927735cc7015d19be7309be69fc15b896c5b1fd87fcbd79\", \"last_modified\": \"2024-11-29T09:53:09.000000\", \"location\": \"/mnt/backup/backup1\" } } }";
        BackrollTaskRequestResponse backrollTaskReqResponseMock = new BackrollTaskRequestResponse();
        backrollTaskReqResponseMock.location = "/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782";
        VirtualMachineBackupsResponse virtualMachineBackupsResponseMock = new ObjectMapper().readValue(virtualMachineResponseString, VirtualMachineBackupsResponse.class);

        // Mocking client responses
        doReturn(backrollTaskReqResponseMock).when(backrollHttpClientProviderMock).get(Mockito.matches(".*/virtualmachines/.*"), Mockito.any());
        doReturn(virtualMachineBackupsResponseMock).when(backrollHttpClientProviderMock).waitGet(Mockito.anyString(), Mockito.any());
        // Run the method under test
        List<BackrollVmBackup> backupsTestList = client.getAllBackupsfromVirtualMachine(vmId);

        // Check results
        assertEquals(1, backupsTestList.size());  // Should be 1 based on provided mock data
    }

    @Test
     public void getBackupMetrics_Test() throws IOException, BackrollApiException {
        BackrollTaskRequestResponse backrollTaskReqResponseMock = new BackrollTaskRequestResponse();
        backrollTaskReqResponseMock.location = "/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782";
        BackrollBackupMetricsResponse mockResponse = new BackrollBackupMetricsResponse();
        mockResponse.info = new BackupMetricsInfo();
        mockResponse.info.originalSize = "1000";
        mockResponse.info.deduplicatedSize = "800";

        doReturn(backrollTaskReqResponseMock).when(backrollHttpClientProviderMock).get(Mockito.matches(".*/virtualmachines/.*"), Mockito.any());
        doReturn(mockResponse).when(backrollHttpClientProviderMock).waitGet(Mockito.anyString(), Mockito.any());

        BackrollBackupMetrics metrics = client.getBackupMetrics("dummyVMId", "dummyBackupId");

        assertEquals(1000L, metrics.getSize());
        assertEquals(800L, metrics.getDeduplicated());
    }

    @Test
    public void getVirtualMachineMetrics_Test() throws IOException, BackrollApiException {
        BackrollTaskRequestResponse backrollTaskReqResponseMock = new BackrollTaskRequestResponse();
        backrollTaskReqResponseMock.location = "/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782";

        BackrollVmMetricsResponse mockResponse = new BackrollVmMetricsResponse();
        mockResponse.state = TaskState.SUCCESS;
        mockResponse.infos = new MetricsInfos();
        mockResponse.infos.cache = new InfosCache();
        mockResponse.infos.cache.stats = new CacheStats();
        mockResponse.infos.cache.stats.totalSize = "10000";

        doReturn(backrollTaskReqResponseMock).when(backrollHttpClientProviderMock).get(Mockito.matches(".*/virtualmachines/.*"), Mockito.any());
        doReturn(mockResponse).when(backrollHttpClientProviderMock).waitGet(Mockito.anyString(), Mockito.any());

        Metric metrics = client.getVirtualMachineMetrics("dummyVMId");

        assertEquals(10000L, (long)metrics.getBackupSize());
        assertEquals(10000L, (long)metrics.getDataSize());
    }
    @Test
    public void deleteBackup_Test() throws IOException, BackrollApiException{
        BackrollTaskRequestResponse backrollTaskReqResponseMock = new BackrollTaskRequestResponse();
        backrollTaskReqResponseMock.location = "/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782";

        BackrollBackupsFromVMResponse mockResponse = new BackrollBackupsFromVMResponse();
        mockResponse.state = TaskState.SUCCESS;

        doReturn(backrollTaskReqResponseMock).when(backrollHttpClientProviderMock).delete(Mockito.matches(".*/virtualmachines/.*"), Mockito.any());
        doReturn(mockResponse).when(backrollHttpClientProviderMock).waitGet(Mockito.anyString(), Mockito.any());

        Boolean isBackupDeleted = client.deleteBackup("dummyVMId", "dummyBackUpName");

        assertTrue(isBackupDeleted);
    }
    @Test
    public void checkBackupTaskStatusSuccess_Test() throws IOException, BackrollApiException {
        String backupResponse = "{\"state\":\"SUCCESS\",\"info\":\"test\"}";
        doReturn(backupResponse).when(backrollHttpClientProviderMock).getWithoutParseResponse(Mockito.matches(".*status/.*"));

        BackrollTaskStatus status = client.checkBackupTaskStatus("dummytaskid");

        assertEquals(TaskState.SUCCESS, status.getState());
        assertEquals("test", status.getInfo());
    }

    @Test
    public void checkBackupTaskStatus_Test() throws IOException, BackrollApiException {
        String backupResponse = "{\"state\":\"PENDING\",\"current\":0,\"total\":1,\"status\":\"Pending...\"}";
        doReturn(backupResponse).when(backrollHttpClientProviderMock).getWithoutParseResponse(Mockito.matches(".*/status/.*"));

        BackrollTaskStatus status = client.checkBackupTaskStatus("dummytaskid");

        assertEquals(TaskState.PENDING, status.getState());
    }
    @Test
    public void restoreVMFromBackup_Test() throws IOException, BackrollApiException  {
        BackrollTaskRequestResponse backrollTaskReqResponseMock = new BackrollTaskRequestResponse();
        backrollTaskReqResponseMock.location = "/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782";
        String resultMock = "SUCCESS WOW YOUHOU";
        doReturn(backrollTaskReqResponseMock).when(backrollHttpClientProviderMock).post(Mockito.matches(".*/tasks/restore/.*"), Mockito.any(JSONObject.class),Mockito.any());
        doReturn(resultMock).when(backrollHttpClientProviderMock).waitGetWithoutParseResponse(Mockito.anyString());

        Boolean isRestoreOk = client.restoreVMFromBackup("dummyVMId", "dummyBackUpName");

        assertTrue(isRestoreOk);
    }

    @Test
    public void startBackupJob_Test() throws IOException, BackrollApiException {

        BackrollTaskRequestResponse backrollTaskReqResponseMock = new BackrollTaskRequestResponse();
        backrollTaskReqResponseMock.location = "/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782";
        doReturn(backrollTaskReqResponseMock).when(backrollHttpClientProviderMock).post(Mockito.matches(".*/tasks/singlebackup/.*"), Mockito.nullable(JSONObject.class),Mockito.any());

        String response = client.startBackupJob("dummyJobId");

        assertEquals("/status/f32092e4-3e8a-461b-8733-ed93e23fa782", response);
    }

    @Test
    public void getBackupOfferingUrl_Test() throws IOException, BackrollApiException  {
        BackrollTaskRequestResponse backrollTaskReqResponseMock = new BackrollTaskRequestResponse();
        backrollTaskReqResponseMock.location = "/api/v1/status/f32092e4-3e8a-461b-8733-ed93e23fa782";
        doReturn(backrollTaskReqResponseMock).when(backrollHttpClientProviderMock).get(Mockito.matches(".*/backup_policies.*"), Mockito.any());

        String response = client.getBackupOfferingUrl();

        assertEquals("/status/f32092e4-3e8a-461b-8733-ed93e23fa782", response);
    }

    @Test
    public void getBackupOfferings_Test() throws BackrollApiException, IOException {

        BackrollBackupPolicyResponse policy1 = new BackrollBackupPolicyResponse();
        policy1.name = "User-Policy-1";
        policy1.retentionDay = 6;
        policy1.schedule = "0 0 * * 1";
        policy1.retentionMonth = 0;
        policy1.storage = "f32092e4-3e8a-461b-8733-ed93e23fa782";
        policy1.enabled = false;
        policy1.description = "User's policy 1 description";
        policy1.id = "f32092e4-3e8a-461b-8733-ed93e23fa782";
        policy1.retentionWeek = 0;
        policy1.retentionYear = 0;
        policy1.externalHook = null;

        BackrollBackupPolicyResponse policy2 = new BackrollBackupPolicyResponse();
        policy1.name = "User-Policy-2";
        policy1.retentionDay = 6;
        policy1.schedule = "0 0 * * 1";
        policy1.retentionMonth = 0;
        policy1.storage = "f32092e4-3e8a-461b-8733-ed93e23fa782";
        policy1.enabled = false;
        policy1.description = "User's policy 2 description";
        policy1.id = "f32092e4-3e8a-461b-8733-ed93e23fa782";
        policy1.retentionWeek = 0;
        policy1.retentionYear = 0;
        policy1.externalHook = null;
        BackupPoliciesResponse backupPoliciesResponseMock = new BackupPoliciesResponse();
        backupPoliciesResponseMock.backupPolicies = Arrays.asList(policy1, policy2);

        doReturn(backupPoliciesResponseMock).when(backrollHttpClientProviderMock).waitGet(Mockito.matches("/status/f32092e4-3e8a-461b-8733-ed93e23fa782"), Mockito.any());

        List<BackupOffering> response = client.getBackupOfferings("/status/f32092e4-3e8a-461b-8733-ed93e23fa782");

        assertEquals(response.size(), 2);

    }
}