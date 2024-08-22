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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.veeam.api.RestoreSession;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.Logger;
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
    private static final SimpleDateFormat newDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9399);

    @Before
    public void setUp() throws Exception {
        wireMockRule.stubFor(post(urlMatching(".*/sessionMngr/.*"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("X-RestSvcSessionId", "some-session-auth-id")
                        .withBody("")));
        client = new VeeamClient("http://localhost:9399/api/", 12, adminUsername, adminPassword, true, 60, 600, 5, 120);
        mockClient = Mockito.mock(VeeamClient.class);
        mockClient.logger = Mockito.mock(Logger.class);
        Mockito.when(mockClient.getRepositoryNameFromJob(Mockito.anyString())).thenCallRealMethod();
        Mockito.when(mockClient.getVeeamServerVersion()).thenCallRealMethod();
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
        Pair<Boolean, String> response = new Pair<Boolean, String>(Boolean.TRUE, "\r\nName : test");
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
            Mockito.doCallRealMethod().when(mockClient).checkIfRestoreSessionFinished(Mockito.eq("RestoreTest"), Mockito.eq("any"));
            mockClient.checkIfRestoreSessionFinished("RestoreTest", "any");
            fail();
        } catch (Exception e) {
            Assert.assertEquals("Related job type: RestoreTest was not successful", e.getMessage());
        }
        Mockito.verify(mockClient, times(10)).get(Mockito.anyString());
    }

    @Test
    public void getRestoreVmErrorDescriptionTestFindErrorDescription() {
        Pair<Boolean, String> response = new Pair<>(true, "Example of error description found in Veeam.");
        Mockito.when(mockClient.getRestoreVmErrorDescription("uuid")).thenCallRealMethod();
        Mockito.when(mockClient.executePowerShellCommands(Mockito.any())).thenReturn(response);
        String result = mockClient.getRestoreVmErrorDescription("uuid");
        Assert.assertEquals("Example of error description found in Veeam.", result);
    }

    @Test
    public void getRestoreVmErrorDescriptionTestNotFindErrorDescription() {
        Pair<Boolean, String> response = new Pair<>(true, "Cannot find restore session with provided uid uuid");
        Mockito.when(mockClient.getRestoreVmErrorDescription("uuid")).thenCallRealMethod();
        Mockito.when(mockClient.executePowerShellCommands(Mockito.any())).thenReturn(response);
        String result = mockClient.getRestoreVmErrorDescription("uuid");
        Assert.assertEquals("Cannot find restore session with provided uid uuid", result);
    }

    @Test
    public void getRestoreVmErrorDescriptionTestWhenPowerShellOutputIsNull() {
        Mockito.when(mockClient.getRestoreVmErrorDescription("uuid")).thenCallRealMethod();
        Mockito.when(mockClient.executePowerShellCommands(Mockito.any())).thenReturn(null);
        String result = mockClient.getRestoreVmErrorDescription("uuid");
        Assert.assertEquals("Failed to get the description of the failed restore session [uuid]. Please contact an administrator.", result);
    }

    @Test
    public void getRestoreVmErrorDescriptionTestWhenPowerShellOutputIsFalse() {
        Pair<Boolean, String> response = new Pair<>(false, null);
        Mockito.when(mockClient.getRestoreVmErrorDescription("uuid")).thenCallRealMethod();
        Mockito.when(mockClient.executePowerShellCommands(Mockito.any())).thenReturn(response);
        String result = mockClient.getRestoreVmErrorDescription("uuid");
        Assert.assertEquals("Failed to get the description of the failed restore session [uuid]. Please contact an administrator.", result);
    }


    private void verifyBackupMetrics(Map<String, Backup.Metric> metrics) {
        Assert.assertEquals(2, metrics.size());

        Assert.assertTrue(metrics.containsKey("d1bd8abd-fc73-4b77-9047-7be98a2ecb72"));
        Assert.assertEquals(537776128L, (long) metrics.get("d1bd8abd-fc73-4b77-9047-7be98a2ecb72").getBackupSize());
        Assert.assertEquals(2147506644L, (long) metrics.get("d1bd8abd-fc73-4b77-9047-7be98a2ecb72").getDataSize());

        Assert.assertTrue(metrics.containsKey("0d752ca6-d628-4d85-a739-75275e4661e6"));
        Assert.assertEquals(1268682752L, (long) metrics.get("0d752ca6-d628-4d85-a739-75275e4661e6").getBackupSize());
        Assert.assertEquals(15624049921L, (long) metrics.get("0d752ca6-d628-4d85-a739-75275e4661e6").getDataSize());
    }

    @Test
    public void testProcessPowerShellResultForBackupMetrics() {
        String result = "i-2-3-VM-CSBKP-d1bd8abd-fc73-4b77-9047-7be98a2ecb72\r\n" +
                "537776128\r\n" +
                "2147506644\r\n" +
                "=====\r\n" +
                "i-13-22-VM-CSBKP-b3b3cb75-cfbf-4496-9c63-a08a93347276\r\n" +
                "=====\r\n" +
                "backup-job-based-on-sla\r\n" +
                "=====\r\n" +
                "i-12-20-VM-CSBKP-9f292f11-00ec-4915-84f0-e3895828640e\r\n" +
                "=====\r\n" +
                "i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\r\n" +
                "1268682752\r\n" +
                "15624049921\r\n" +
                "=====\r\n";

        Map<String, Backup.Metric> metrics = client.processPowerShellResultForBackupMetrics(result);

        verifyBackupMetrics(metrics);
    }

    @Test
    public void testProcessHttpResponseForBackupMetricsForV11() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<BackupFiles xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.veeam.com/ent/v1.0\">\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/d2110f5f-aa22-4e67-8084-5d8597f26d63?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-28T000059_745D.vbk\" UID=\"urn:veeam:BackupFile:d2110f5f-aa22-4e67-8084-5d8597f26d63\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/d2110f5f-aa22-4e67-8084-5d8597f26d63\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-28T000059_745D.vbk\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/d2110f5f-aa22-4e67-8084-5d8597f26d63/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/d2110f5f-aa22-4e67-8084-5d8597f26d63/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-28T000059_745D.vbk</FilePath>\n" +
                "    <BackupSize>579756032</BackupSize>\n" +
                "    <DataSize>7516219400</DataSize>\n" +
                "    <DeduplicationRatio>5.83</DeduplicationRatio>\n" +
                "    <CompressRatio>2.22</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-10-27T23:00:13.74Z</CreationTimeUtc>\n" +
                "    <FileType>vbk</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/7c54d13d-7b9c-465a-8ec8-7a276bde57dd?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-05T000022_7987.vib\" UID=\"urn:veeam:BackupFile:7c54d13d-7b9c-465a-8ec8-7a276bde57dd\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/7c54d13d-7b9c-465a-8ec8-7a276bde57dd\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-05T000022_7987.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/7c54d13d-7b9c-465a-8ec8-7a276bde57dd/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/7c54d13d-7b9c-465a-8ec8-7a276bde57dd/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-05T000022_7987.vib</FilePath>\n" +
                "    <BackupSize>12083200</BackupSize>\n" +
                "    <DataSize>69232800</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>6.67</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-11-05T00:00:22.827Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/4b1181fd-7b1e-4af1-a76b-8284a8953b99?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-01T000035_BEBF.vib\" UID=\"urn:veeam:BackupFile:4b1181fd-7b1e-4af1-a76b-8284a8953b99\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/4b1181fd-7b1e-4af1-a76b-8284a8953b99\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-01T000035_BEBF.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/4b1181fd-7b1e-4af1-a76b-8284a8953b99/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/4b1181fd-7b1e-4af1-a76b-8284a8953b99/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-01T000035_BEBF.vib</FilePath>\n" +
                "    <BackupSize>12398592</BackupSize>\n" +
                "    <DataSize>71329948</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>6.67</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-11-01T00:00:35.163Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/66b39f48-af76-4373-b333-996fc04da894?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-04T000109_2AC1.vbk\" UID=\"urn:veeam:BackupFile:66b39f48-af76-4373-b333-996fc04da894\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/66b39f48-af76-4373-b333-996fc04da894\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-04T000109_2AC1.vbk\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/66b39f48-af76-4373-b333-996fc04da894/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/66b39f48-af76-4373-b333-996fc04da894/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-04T000109_2AC1.vbk</FilePath>\n" +
                "    <BackupSize>581083136</BackupSize>\n" +
                "    <DataSize>7516219404</DataSize>\n" +
                "    <DeduplicationRatio>5.82</DeduplicationRatio>\n" +
                "    <CompressRatio>2.22</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-11-04T00:00:24.973Z</CreationTimeUtc>\n" +
                "    <FileType>vbk</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/8e9a854e-9bb8-4a34-815c-a6ab17a1e72f?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-29T000033_F468.vib\" UID=\"urn:veeam:BackupFile:8e9a854e-9bb8-4a34-815c-a6ab17a1e72f\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/8e9a854e-9bb8-4a34-815c-a6ab17a1e72f\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-29T000033_F468.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/8e9a854e-9bb8-4a34-815c-a6ab17a1e72f/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/8e9a854e-9bb8-4a34-815c-a6ab17a1e72f/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-29T000033_F468.vib</FilePath>\n" +
                "    <BackupSize>11870208</BackupSize>\n" +
                "    <DataSize>72378524</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>7.14</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-10-28T23:00:33.233Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/cf4536c0-d752-4ba5-ad7f-bbc17c7e107b?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-30T000022_0CE3.vib\" UID=\"urn:veeam:BackupFile:cf4536c0-d752-4ba5-ad7f-bbc17c7e107b\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/cf4536c0-d752-4ba5-ad7f-bbc17c7e107b\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-30T000022_0CE3.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/cf4536c0-d752-4ba5-ad7f-bbc17c7e107b/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/cf4536c0-d752-4ba5-ad7f-bbc17c7e107b/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-30T000022_0CE3.vib</FilePath>\n" +
                "    <BackupSize>14409728</BackupSize>\n" +
                "    <DataSize>76572828</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>6.25</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-10-30T00:00:22.7Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/2dd7f5b6-8a10-406d-9c4f-c0dfa987e85c?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-06T000018_055B.vib\" UID=\"urn:veeam:BackupFile:2dd7f5b6-8a10-406d-9c4f-c0dfa987e85c\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/2dd7f5b6-8a10-406d-9c4f-c0dfa987e85c\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-06T000018_055B.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/2dd7f5b6-8a10-406d-9c4f-c0dfa987e85c/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/2dd7f5b6-8a10-406d-9c4f-c0dfa987e85c/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-06T000018_055B.vib</FilePath>\n" +
                "    <BackupSize>17883136</BackupSize>\n" +
                "    <DataSize>80767136</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>5</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-11-06T00:00:18.253Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/3fd6da3a-47bf-45fa-a4c8-c436e3cd34a7?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-02T000029_65BE.vib\" UID=\"urn:veeam:BackupFile:3fd6da3a-47bf-45fa-a4c8-c436e3cd34a7\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/3fd6da3a-47bf-45fa-a4c8-c436e3cd34a7\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-02T000029_65BE.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/3fd6da3a-47bf-45fa-a4c8-c436e3cd34a7/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/3fd6da3a-47bf-45fa-a4c8-c436e3cd34a7/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-02T000029_65BE.vib</FilePath>\n" +
                "    <BackupSize>12521472</BackupSize>\n" +
                "    <DataSize>72378525</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>6.67</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-11-02T00:00:29.05Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/d93d7c7d-068a-4e8f-ba54-e08cea3cb9d2?format=Entity\" Type=\"BackupFile\" Name=\"i-2-3-VM-CSBKP-d1bd8abd-fc73-4b77-9047-7be98aD2023-10-25T145951_8062.vbk\" UID=\"urn:veeam:BackupFile:d93d7c7d-068a-4e8f-ba54-e08cea3cb9d2\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/a34cae53-2d9e-454b-8d3e-0aaa7b34c228\" Name=\"i-2-3-VM-CSBKP-d1bd8abd-fc73-4b77-9047-7be98a2ecb72\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/d93d7c7d-068a-4e8f-ba54-e08cea3cb9d2\" Name=\"i-2-3-VM-CSBKP-d1bd8abd-fc73-4b77-9047-7be98aD2023-10-25T145951_8062.vbk\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/d93d7c7d-068a-4e8f-ba54-e08cea3cb9d2/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/d93d7c7d-068a-4e8f-ba54-e08cea3cb9d2/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-3-VM-CSBKP-d1bd8abd-fc73-4b77-9047-7be98a2ecb72\\i-2-3-VM-CSBKP-d1bd8abd-fc73-4b77-9047-7be98aD2023-10-25T145951_8062.vbk</FilePath>\n" +
                "    <BackupSize>537776128</BackupSize>\n" +
                "    <DataSize>2147506644</DataSize>\n" +
                "    <DeduplicationRatio>1.68</DeduplicationRatio>\n" +
                "    <CompressRatio>2.38</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-10-25T13:59:51.76Z</CreationTimeUtc>\n" +
                "    <FileType>vbk</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/094564ff-02a1-46c7-b9e5-e249b8b9acf6?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-03T000024_7ACF.vib\" UID=\"urn:veeam:BackupFile:094564ff-02a1-46c7-b9e5-e249b8b9acf6\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/094564ff-02a1-46c7-b9e5-e249b8b9acf6\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-03T000024_7ACF.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/094564ff-02a1-46c7-b9e5-e249b8b9acf6/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/094564ff-02a1-46c7-b9e5-e249b8b9acf6/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-11-03T000024_7ACF.vib</FilePath>\n" +
                "    <BackupSize>14217216</BackupSize>\n" +
                "    <DataSize>76572832</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>6.25</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-11-03T00:00:24.803Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "  <BackupFile Href=\"https://10.0.3.141:9398/api/backupFiles/1f6f5c49-92ef-4757-b327-e63ae9f1fdea?format=Entity\" Type=\"BackupFile\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-31T000015_4624.vib\" UID=\"urn:veeam:BackupFile:1f6f5c49-92ef-4757-b327-e63ae9f1fdea\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backups/e7484f82-b01b-47cf-92ad-ac5e8379a4fe\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\" Type=\"BackupReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupServers/bb188236-7b8b-4763-b35a-5d6645d3e95b\" Name=\"10.0.3.141\" Type=\"BackupServerReference\" Rel=\"Up\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/1f6f5c49-92ef-4757-b327-e63ae9f1fdea\" Name=\"i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-31T000015_4624.vib\" Type=\"BackupFileReference\" Rel=\"Alternate\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/1f6f5c49-92ef-4757-b327-e63ae9f1fdea/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\"/>\n" +
                "      <Link Href=\"https://10.0.3.141:9398/api/backupFiles/1f6f5c49-92ef-4757-b327-e63ae9f1fdea/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\"/>\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275e4661e6\\i-2-5-VM-CSBKP-0d752ca6-d628-4d85-a739-75275eD2023-10-31T000015_4624.vib</FilePath>\n" +
                "    <BackupSize>12460032</BackupSize>\n" +
                "    <DataSize>72378524</DataSize>\n" +
                "    <DeduplicationRatio>1</DeduplicationRatio>\n" +
                "    <CompressRatio>6.67</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-10-31T00:00:15.853Z</CreationTimeUtc>\n" +
                "    <FileType>vib</FileType>\n" +
                "  </BackupFile>\n" +
                "</BackupFiles>\n";

        InputStream inputStream = new ByteArrayInputStream(xmlResponse.getBytes());
        Map<String, Backup.Metric> metrics = client.processHttpResponseForBackupMetrics(inputStream);

        verifyBackupMetrics(metrics);
    }

    @Test
    public void testGetBackupMetricsViaVeeamAPI() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<BackupFiles\n" +
                "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "  xmlns=\"http://www.veeam.com/ent/v1.0\">\n" +
                "  <BackupFile Href=\"https://10.0.3.142:9398/api/backupFiles/6bf10cad-9181-45d9-9cc5-dd669366a381?format=Entity\" Type=\"BackupFile\" Name=\"i-2-4-VM.vm-1036D2023-11-03T162535_89D6.vbk\" UID=\"urn:veeam:BackupFile:6bf10cad-9181-45d9-9cc5-dd669366a381\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/backups/957d3817-2480-4c06-85f9-103e625c20e5\" Name=\"i-2-4-VM-CSBKP-506760dc-ed77-40d6-a91d-e0914e7a1ad8 - i-2-4-VM\" Type=\"BackupReference\" Rel=\"Up\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/backupServers/18cc2a81-1ff0-42cd-8389-62f2bbcc6b7f\" Name=\"10.0.3.142\" Type=\"BackupServerReference\" Rel=\"Up\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/backupFiles/6bf10cad-9181-45d9-9cc5-dd669366a381\" Name=\"i-2-4-VM.vm-1036D2023-11-03T162535_89D6.vbk\" Type=\"BackupFileReference\" Rel=\"Alternate\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/backupFiles/6bf10cad-9181-45d9-9cc5-dd669366a381/restorePoints\" Type=\"RestorePointReferenceList\" Rel=\"Related\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/backupFiles/6bf10cad-9181-45d9-9cc5-dd669366a381/vmRestorePoints\" Type=\"VmRestorePointReferenceList\" Rel=\"Down\" />\n" +
                "    </Links>\n" +
                "    <FilePath>V:\\Backup\\i-2-4-VM-CSBKP-506760dc-ed77-40d6-a91d-e0914e7a1ad8\\i-2-4-VM.vm-1036D2023-11-03T162535_89D6.vbk</FilePath>\n" +
                "    <BackupSize>535875584</BackupSize>\n" +
                "    <DataSize>2147507235</DataSize>\n" +
                "    <DeduplicationRatio>1.68</DeduplicationRatio>\n" +
                "    <CompressRatio>2.38</CompressRatio>\n" +
                "    <CreationTimeUtc>2023-11-03T16:25:35.920773Z</CreationTimeUtc>\n" +
                "    <FileType>vbk</FileType>\n" +
                "  </BackupFile>\n" +
                "</BackupFiles>";

        wireMockRule.stubFor(get(urlMatching(".*/backupFiles\\?format=Entity"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/xml")
                        .withStatus(200)
                        .withBody(xmlResponse)));
        Map<String, Backup.Metric> metrics = client.getBackupMetricsViaVeeamAPI();

        Assert.assertEquals(1, metrics.size());
        Assert.assertTrue(metrics.containsKey("506760dc-ed77-40d6-a91d-e0914e7a1ad8"));
        Assert.assertEquals(535875584L, (long) metrics.get("506760dc-ed77-40d6-a91d-e0914e7a1ad8").getBackupSize());
        Assert.assertEquals(2147507235L, (long) metrics.get("506760dc-ed77-40d6-a91d-e0914e7a1ad8").getDataSize());
    }

    @Test
    public void testListVmRestorePointsViaVeeamAPI() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<VmRestorePoints\n" +
                "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "  xmlns=\"http://www.veeam.com/ent/v1.0\">\n" +
                "  <VmRestorePoint Href=\"https://10.0.3.142:9398/api/vmRestorePoints/f6d504cf-eafe-4cd2-8dfc-e9cfe2f1e977?format=Entity\" Type=\"VmRestorePoint\" Name=\"i-2-4-VM@2023-11-03 16:26:12.209913\" UID=\"urn:veeam:VmRestorePoint:f6d504cf-eafe-4cd2-8dfc-e9cfe2f1e977\" VmDisplayName=\"i-2-4-VM\">\n" +
                "    <Links>\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/vmRestorePoints/f6d504cf-eafe-4cd2-8dfc-e9cfe2f1e977?action=restore\" Rel=\"Restore\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/backupServers/18cc2a81-1ff0-42cd-8389-62f2bbcc6b7f\" Name=\"10.0.3.142\" Type=\"BackupServerReference\" Rel=\"Up\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/restorePoints/c030b23e-d7fa-45b6-a5a7-feb8525d2563\" Name=\"2023-11-03 16:25:35.920773\" Type=\"RestorePointReference\" Rel=\"Up\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/backupFiles/6bf10cad-9181-45d9-9cc5-dd669366a381\" Name=\"i-2-4-VM.vm-1036D2023-11-03T162535_89D6.vbk\" Type=\"BackupFileReference\" Rel=\"Up\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/vmRestorePoints/f6d504cf-eafe-4cd2-8dfc-e9cfe2f1e977\" Name=\"i-2-4-VM@2023-11-03 16:26:12.209913\" Type=\"VmRestorePointReference\" Rel=\"Alternate\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/vmRestorePoints/f6d504cf-eafe-4cd2-8dfc-e9cfe2f1e977/mounts\" Type=\"VmRestorePointMountList\" Rel=\"Down\" />\n" +
                "      <Link Href=\"https://10.0.3.142:9398/api/vmRestorePoints/f6d504cf-eafe-4cd2-8dfc-e9cfe2f1e977/mounts\" Type=\"VmRestorePointMount\" Rel=\"Create\" />\n" +
                "    </Links>\n" +
                "    <CreationTimeUTC>2023-11-03T16:26:12.209913Z</CreationTimeUTC>\n" +
                "    <VmName>i-2-4-VM</VmName>\n" +
                "    <Algorithm>Full</Algorithm>\n" +
                "    <PointType>Full</PointType>\n" +
                "    <HierarchyObjRef>urn:VMware:Vm:adb5423b-b578-4c26-8ab8-cde9c1faec55.vm-1036</HierarchyObjRef>\n" +
                "  </VmRestorePoint>\n" +
                "</VmRestorePoints>\n";
        String vmName = "i-2-4-VM";

        wireMockRule.stubFor(get(urlMatching(".*/vmRestorePoints\\?format=Entity"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/xml")
                        .withStatus(200)
                        .withBody(xmlResponse)));
        List<Backup.RestorePoint> vmRestorePointList = client.listVmRestorePointsViaVeeamAPI(vmName);

        Assert.assertEquals(1, vmRestorePointList.size());
        Assert.assertEquals("f6d504cf-eafe-4cd2-8dfc-e9cfe2f1e977", vmRestorePointList.get(0).getId());
        Assert.assertEquals("2023-11-03 16:26:12", newDateFormat.format(vmRestorePointList.get(0).getCreated()));
        Assert.assertEquals("Full", vmRestorePointList.get(0).getType());
    }

    @Test
    public void testGetVeeamServerVersionAllGood() {
        Pair<Boolean, String> response = new Pair<Boolean, String>(Boolean.TRUE, "12.0.0.1");
        Mockito.doReturn(response).when(mockClient).executePowerShellCommands(Mockito.anyList());
        Assert.assertEquals(12, (int) mockClient.getVeeamServerVersion());
    }

    @Test
    public void testGetVeeamServerVersionWithError() {
        Pair<Boolean, String> response = new Pair<Boolean, String>(Boolean.FALSE, "");
        Mockito.doReturn(response).when(mockClient).executePowerShellCommands(Mockito.anyList());
        Assert.assertEquals(0, (int) mockClient.getVeeamServerVersion());
    }

    @Test
    public void testGetVeeamServerVersionWithEmptyVersion() {
        Pair<Boolean, String> response = new Pair<Boolean, String>(Boolean.TRUE, "");
        Mockito.doReturn(response).when(mockClient).executePowerShellCommands(Mockito.anyList());
        Assert.assertEquals(0, (int) mockClient.getVeeamServerVersion());
    }
}
