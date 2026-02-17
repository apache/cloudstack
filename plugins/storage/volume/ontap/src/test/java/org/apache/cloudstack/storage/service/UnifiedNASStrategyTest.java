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

package org.apache.cloudstack.storage.service;

import com.cloud.agent.api.Answer;
import com.cloud.host.HostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.feign.client.JobFeignClient;
import org.apache.cloudstack.storage.feign.client.NASFeignClient;
import org.apache.cloudstack.storage.feign.client.VolumeFeignClient;
import org.apache.cloudstack.storage.feign.client.AggregateFeignClient;
import org.apache.cloudstack.storage.feign.client.SvmFeignClient;
import org.apache.cloudstack.storage.feign.client.NetworkFeignClient;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.model.ExportPolicy;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UnifiedNASStrategyTest {

    @Mock
    private NASFeignClient nasFeignClient;

    @Mock
    private VolumeFeignClient volumeFeignClient;

    @Mock
    private JobFeignClient jobFeignClient;

    @Mock
    private AggregateFeignClient aggregateFeignClient;

    @Mock
    private SvmFeignClient svmFeignClient;

    @Mock
    private NetworkFeignClient networkFeignClient;

    @Mock
    private SANFeignClient sanFeignClient;

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private EndPointSelector epSelector;

    @Mock
    private StoragePoolDetailsDao storagePoolDetailsDao;

    private TestableUnifiedNASStrategy strategy;

    private OntapStorage ontapStorage;

    @BeforeEach
    public void setUp() throws Exception {
        ontapStorage = new OntapStorage(
            "admin",
            "password",
            "192.168.1.100",
            "svm1",
            100L,
            ProtocolType.NFS3
        );
        strategy = new TestableUnifiedNASStrategy(ontapStorage, nasFeignClient, volumeFeignClient, jobFeignClient, aggregateFeignClient, svmFeignClient, networkFeignClient, sanFeignClient);
        injectField("volumeDao", volumeDao);
        injectField("epSelector", epSelector);
        injectField("storagePoolDetailsDao", storagePoolDetailsDao);
    }

    private void injectField(String fieldName, Object mockedField) throws Exception {
        Field field = UnifiedNASStrategy.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(strategy, mockedField);
    }

    private class TestableUnifiedNASStrategy extends UnifiedNASStrategy {
        public TestableUnifiedNASStrategy(OntapStorage ontapStorage,
                                          NASFeignClient nasFeignClient,
                                          VolumeFeignClient volumeFeignClient,
                                          JobFeignClient jobFeignClient,
                                          AggregateFeignClient aggregateFeignClient,
                                          SvmFeignClient svmFeignClient,
                                          NetworkFeignClient networkFeignClient,
                                          SANFeignClient sanFeignClient) {
            super(ontapStorage);
            // Inject mocked Feign clients immediately after super() call
            injectMockedClient("nasFeignClient", nasFeignClient);
            injectMockedClient("volumeFeignClient", volumeFeignClient);
            injectMockedClient("jobFeignClient", jobFeignClient);
            // Inject parent class (StorageStrategy) mocked clients
            injectParentMockedClient("aggregateFeignClient", aggregateFeignClient);
            injectParentMockedClient("volumeFeignClient", volumeFeignClient);
            injectParentMockedClient("svmFeignClient", svmFeignClient);
            injectParentMockedClient("jobFeignClient", jobFeignClient);
            injectParentMockedClient("networkFeignClient", networkFeignClient);
            injectParentMockedClient("sanFeignClient", sanFeignClient);
        }

        private void injectMockedClient(String fieldName, Object mockedClient) {
            try {
                Field field = UnifiedNASStrategy.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(this, mockedClient);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to inject mocked client: " + fieldName, e);
            }
        }

        private void injectParentMockedClient(String fieldName, Object mockedClient) {
            try {
                Field field = StorageStrategy.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(this, mockedClient);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to inject parent mocked client: " + fieldName, e);
            }
        }
    }

    // Test createCloudStackVolume - Success
    @Test
    public void testCreateCloudStackVolume_Success() throws Exception {
        // Setup CloudStackVolume
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeObject volumeObject = mock(VolumeObject.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        EndPoint endPoint = mock(EndPoint.class);
        Answer answer = new Answer(null, true, "Success");

        when(cloudStackVolume.getDatastoreId()).thenReturn("1");
        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeObject);
        when(volumeObject.getId()).thenReturn(100L);
        when(volumeObject.getUuid()).thenReturn("volume-uuid-123");
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeDao.update(anyLong(), any(VolumeVO.class))).thenReturn(true);
        when(epSelector.select(volumeObject)).thenReturn(endPoint);
        when(endPoint.sendMessage(any(CreateObjectCommand.class))).thenReturn(answer);

        // Execute
        CloudStackVolume result = strategy.createCloudStackVolume(cloudStackVolume);

        // Verify
        assertNotNull(result);
        verify(volumeDao).update(anyLong(), any(VolumeVO.class));
        verify(epSelector).select(volumeObject);
        verify(endPoint).sendMessage(any(CreateObjectCommand.class));
    }

    // Test createCloudStackVolume - Volume Not Found
    @Test
    public void testCreateCloudStackVolume_VolumeNotFound() {
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeObject volumeObject = mock(VolumeObject.class);

        when(cloudStackVolume.getDatastoreId()).thenReturn("1");
        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeObject);
        when(volumeObject.getId()).thenReturn(100L);
        when(volumeDao.findById(100L)).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.createCloudStackVolume(cloudStackVolume);
        });
    }

    // Test createCloudStackVolume - KVM Host Creation Failed
    @Test
    public void testCreateCloudStackVolume_KVMHostFailed() {
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeObject volumeObject = mock(VolumeObject.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        EndPoint endPoint = mock(EndPoint.class);
        Answer answer = new Answer(null, false, "Failed to create volume");

        when(cloudStackVolume.getDatastoreId()).thenReturn("1");
        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeObject);
        when(volumeObject.getId()).thenReturn(100L);
        when(volumeObject.getUuid()).thenReturn("volume-uuid-123");
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeDao.update(anyLong(), any(VolumeVO.class))).thenReturn(true);
        when(epSelector.select(volumeObject)).thenReturn(endPoint);
        when(endPoint.sendMessage(any(CreateObjectCommand.class))).thenReturn(answer);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.createCloudStackVolume(cloudStackVolume);
        });
    }

    // Test createCloudStackVolume - No Endpoint
    @Test
    public void testCreateCloudStackVolume_NoEndpoint() {
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeObject volumeObject = mock(VolumeObject.class);
        VolumeVO volumeVO = mock(VolumeVO.class);

        when(cloudStackVolume.getDatastoreId()).thenReturn("1");
        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeObject);
        when(volumeObject.getId()).thenReturn(100L);
        when(volumeObject.getUuid()).thenReturn("volume-uuid-123");
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeDao.update(anyLong(), any(VolumeVO.class))).thenReturn(true);
        when(epSelector.select(volumeObject)).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.createCloudStackVolume(cloudStackVolume);
        });
    }

    // Test createAccessGroup - Success
    @Test
    public void testCreateAccessGroup_Success() throws Exception {
        // Setup
        AccessGroup accessGroup = mock(AccessGroup.class);
        PrimaryDataStoreInfo primaryDataStoreInfo = mock(PrimaryDataStoreInfo.class);
        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.VOLUME_UUID, "vol-uuid-123");
        details.put(Constants.VOLUME_NAME, "vol1");

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageIpAddress()).thenReturn("10.0.0.1");
        hosts.add(host1);

        ExportPolicy createdPolicy = mock(ExportPolicy.class);
        when(createdPolicy.getId()).thenReturn(java.math.BigInteger.ONE);
        when(createdPolicy.getName()).thenReturn("export-policy-1");

        OntapResponse<ExportPolicy> policyResponse = new OntapResponse<>();
        List<ExportPolicy> policies = new ArrayList<>();
        policies.add(createdPolicy);
        policyResponse.setRecords(policies);

        JobResponse jobResponse = new JobResponse();
        Job job = new Job();
        job.setUuid("job-uuid-123");
        job.setState(Constants.JOB_SUCCESS);
        jobResponse.setJob(job);

        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getId()).thenReturn(1L);
        when(accessGroup.getHostsToConnect()).thenReturn(hosts);
        doNothing().when(nasFeignClient).createExportPolicy(anyString(), any(ExportPolicy.class));
        when(nasFeignClient.getExportPolicyResponse(anyString(), anyMap())).thenReturn(policyResponse);
        when(volumeFeignClient.updateVolumeRebalancing(anyString(), anyString(), any())).thenReturn(jobResponse);
        when(jobFeignClient.getJobByUUID(anyString(), anyString())).thenReturn(job);
        doNothing().when(storagePoolDetailsDao).addDetail(anyLong(), anyString(), anyString(), eq(true));

        // Execute
        AccessGroup result = strategy.createAccessGroup(accessGroup);

        // Verify
        assertNotNull(result);
        verify(nasFeignClient).createExportPolicy(anyString(), any(ExportPolicy.class));
        verify(nasFeignClient).getExportPolicyResponse(anyString(), anyMap());
        verify(volumeFeignClient).updateVolumeRebalancing(anyString(), eq("vol-uuid-123"), any());
        verify(storagePoolDetailsDao, times(2)).addDetail(anyLong(), anyString(), anyString(), eq(true));
    }

    // Test createAccessGroup - Failed to Create Policy
    @Test
    public void testCreateAccessGroup_FailedToCreatePolicy() {
        AccessGroup accessGroup = mock(AccessGroup.class);
        PrimaryDataStoreInfo primaryDataStoreInfo = mock(PrimaryDataStoreInfo.class);
        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.VOLUME_UUID, "vol-uuid-123");
        details.put(Constants.VOLUME_NAME, "vol1");

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageIpAddress()).thenReturn("10.0.0.1");
        hosts.add(host1);

        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(accessGroup.getHostsToConnect()).thenReturn(hosts);
        doThrow(new RuntimeException("Failed to create policy")).when(nasFeignClient)
            .createExportPolicy(anyString(), any(ExportPolicy.class));

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.createAccessGroup(accessGroup);
        });
    }

    // Test createAccessGroup - Failed to Verify Policy
    @Test
    public void testCreateAccessGroup_FailedToVerifyPolicy() {
        AccessGroup accessGroup = mock(AccessGroup.class);
        PrimaryDataStoreInfo primaryDataStoreInfo = mock(PrimaryDataStoreInfo.class);
        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.VOLUME_UUID, "vol-uuid-123");
        details.put(Constants.VOLUME_NAME, "vol1");

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageIpAddress()).thenReturn("10.0.0.1");
        hosts.add(host1);

        OntapResponse<ExportPolicy> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(accessGroup.getHostsToConnect()).thenReturn(hosts);
        doNothing().when(nasFeignClient).createExportPolicy(anyString(), any(ExportPolicy.class));
        when(nasFeignClient.getExportPolicyResponse(anyString(), anyMap())).thenReturn(emptyResponse);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.createAccessGroup(accessGroup);
        });
    }

    // Test createAccessGroup - Job Timeout
    // Note: This test is simplified to avoid 200 second wait time.
    // In reality, testing timeout would require mocking Thread.sleep() or refactoring the code.
    @Test
    public void testCreateAccessGroup_JobFailure() throws Exception {
        AccessGroup accessGroup = mock(AccessGroup.class);
        PrimaryDataStoreInfo primaryDataStoreInfo = mock(PrimaryDataStoreInfo.class);
        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.VOLUME_UUID, "vol-uuid-123");
        details.put(Constants.VOLUME_NAME, "vol1");

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageIpAddress()).thenReturn("10.0.0.1");
        hosts.add(host1);

        ExportPolicy createdPolicy = mock(ExportPolicy.class);
        when(createdPolicy.getId()).thenReturn(java.math.BigInteger.ONE);
        when(createdPolicy.getName()).thenReturn("export-policy-1");

        OntapResponse<ExportPolicy> policyResponse = new OntapResponse<>();
        List<ExportPolicy> policies = new ArrayList<>();
        policies.add(createdPolicy);
        policyResponse.setRecords(policies);

        JobResponse jobResponse = new JobResponse();
        Job job = new Job();
        job.setUuid("job-uuid-123");
        job.setState(Constants.JOB_FAILURE); // Set to FAILURE instead of timeout
        job.setMessage("Job failed");
        jobResponse.setJob(job);

        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getId()).thenReturn(1L);
        when(accessGroup.getHostsToConnect()).thenReturn(hosts);
        doNothing().when(nasFeignClient).createExportPolicy(anyString(), any(ExportPolicy.class));
        when(nasFeignClient.getExportPolicyResponse(anyString(), anyMap())).thenReturn(policyResponse);
        when(volumeFeignClient.updateVolumeRebalancing(anyString(), anyString(), any())).thenReturn(jobResponse);
        when(jobFeignClient.getJobByUUID(anyString(), anyString())).thenReturn(job);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.createAccessGroup(accessGroup);
        });
    }

    // Test createAccessGroup - Host with Private IP
    @Test
    public void testCreateAccessGroup_HostWithPrivateIP() throws Exception {
        AccessGroup accessGroup = mock(AccessGroup.class);
        PrimaryDataStoreInfo primaryDataStoreInfo = mock(PrimaryDataStoreInfo.class);
        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.VOLUME_UUID, "vol-uuid-123");
        details.put(Constants.VOLUME_NAME, "vol1");

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageIpAddress()).thenReturn(null);
        when(host1.getPrivateIpAddress()).thenReturn("192.168.1.10");
        hosts.add(host1);

        ExportPolicy createdPolicy = mock(ExportPolicy.class);
        when(createdPolicy.getId()).thenReturn(java.math.BigInteger.ONE);
        when(createdPolicy.getName()).thenReturn("export-policy-1");

        OntapResponse<ExportPolicy> policyResponse = new OntapResponse<>();
        List<ExportPolicy> policies = new ArrayList<>();
        policies.add(createdPolicy);
        policyResponse.setRecords(policies);

        JobResponse jobResponse = new JobResponse();
        Job job = new Job();
        job.setUuid("job-uuid-123");
        job.setState(Constants.JOB_SUCCESS);
        jobResponse.setJob(job);

        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getId()).thenReturn(1L);
        when(accessGroup.getHostsToConnect()).thenReturn(hosts);
        doNothing().when(nasFeignClient).createExportPolicy(anyString(), any(ExportPolicy.class));
        when(nasFeignClient.getExportPolicyResponse(anyString(), anyMap())).thenReturn(policyResponse);
        when(volumeFeignClient.updateVolumeRebalancing(anyString(), anyString(), any())).thenReturn(jobResponse);
        when(jobFeignClient.getJobByUUID(anyString(), anyString())).thenReturn(job);
        doNothing().when(storagePoolDetailsDao).addDetail(anyLong(), anyString(), anyString(), eq(true));

        // Execute
        AccessGroup result = strategy.createAccessGroup(accessGroup);

        // Verify
        assertNotNull(result);
        ArgumentCaptor<ExportPolicy> policyCaptor = ArgumentCaptor.forClass(ExportPolicy.class);
        verify(nasFeignClient).createExportPolicy(anyString(), policyCaptor.capture());
        ExportPolicy capturedPolicy = policyCaptor.getValue();
        assertEquals("192.168.1.10/32", capturedPolicy.getRules().get(0).getClients().get(0).getMatch());
    }

    // Test deleteAccessGroup - Success
    @Test
    public void testDeleteAccessGroup_Success() {
        AccessGroup accessGroup = mock(AccessGroup.class);
        PrimaryDataStoreInfo primaryDataStoreInfo = mock(PrimaryDataStoreInfo.class);
        Map<String, String> details = new HashMap<>();
        details.put(Constants.EXPORT_POLICY_NAME, "export-policy-1");
        details.put(Constants.EXPORT_POLICY_ID, "1");

        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getName()).thenReturn("storage-pool-1");
        doNothing().when(nasFeignClient).deleteExportPolicyById(anyString(), anyString());

        // Execute
        strategy.deleteAccessGroup(accessGroup);

        // Verify
        verify(nasFeignClient).deleteExportPolicyById(anyString(), eq("1"));
    }

    // Test deleteAccessGroup - Null AccessGroup
    @Test
    public void testDeleteAccessGroup_NullAccessGroup() {
        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteAccessGroup(null);
        });
    }

    // Test deleteAccessGroup - Null PrimaryDataStoreInfo
    @Test
    public void testDeleteAccessGroup_NullPrimaryDataStoreInfo() {
        AccessGroup accessGroup = mock(AccessGroup.class);
        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteAccessGroup(accessGroup);
        });
    }

    // Test deleteAccessGroup - Failed to Delete
    @Test
    public void testDeleteAccessGroup_Failed() {
        AccessGroup accessGroup = mock(AccessGroup.class);
        PrimaryDataStoreInfo primaryDataStoreInfo = mock(PrimaryDataStoreInfo.class);
        Map<String, String> details = new HashMap<>();
        details.put(Constants.EXPORT_POLICY_NAME, "export-policy-1");
        details.put(Constants.EXPORT_POLICY_ID, "1");

        when(accessGroup.getPrimaryDataStoreInfo()).thenReturn(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getName()).thenReturn("storage-pool-1");
        doThrow(new RuntimeException("Failed to delete")).when(nasFeignClient)
            .deleteExportPolicyById(anyString(), anyString());

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteAccessGroup(accessGroup);
        });
    }

    // Test deleteCloudStackVolume - Success
    @Test
    public void testDeleteCloudStackVolume_Success() throws Exception {
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        EndPoint endpoint = mock(EndPoint.class);
        Answer answer = mock(Answer.class);

        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeInfo);
        when(epSelector.select(volumeInfo)).thenReturn(endpoint);
        when(endpoint.sendMessage(any())).thenReturn(answer);
        when(answer.getResult()).thenReturn(true);

        // Execute - should not throw exception
        strategy.deleteCloudStackVolume(cloudStackVolume);

        // Verify endpoint was selected and message sent
        verify(epSelector).select(volumeInfo);
        verify(endpoint).sendMessage(any());
    }

    // Test deleteCloudStackVolume - Endpoint Not Found
    @Test
    public void testDeleteCloudStackVolume_EndpointNotFound() {
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeInfo volumeInfo = mock(VolumeInfo.class);

        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeInfo);
        when(epSelector.select(volumeInfo)).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteCloudStackVolume(cloudStackVolume);
        });
    }

    // Test deleteCloudStackVolume - Answer Result False
    @Test
    public void testDeleteCloudStackVolume_AnswerResultFalse() throws Exception {
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        EndPoint endpoint = mock(EndPoint.class);
        Answer answer = mock(Answer.class);

        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeInfo);
        when(epSelector.select(volumeInfo)).thenReturn(endpoint);
        when(endpoint.sendMessage(any())).thenReturn(answer);
        when(answer.getResult()).thenReturn(false);
        when(answer.getDetails()).thenReturn("Failed to delete volume file");

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteCloudStackVolume(cloudStackVolume);
        });
    }

    // Test deleteCloudStackVolume - Answer is Null
    @Test
    public void testDeleteCloudStackVolume_AnswerNull() throws Exception {
        CloudStackVolume cloudStackVolume = mock(CloudStackVolume.class);
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        EndPoint endpoint = mock(EndPoint.class);

        when(cloudStackVolume.getVolumeInfo()).thenReturn(volumeInfo);
        when(epSelector.select(volumeInfo)).thenReturn(endpoint);
        when(endpoint.sendMessage(any())).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteCloudStackVolume(cloudStackVolume);
        });
    }
}
