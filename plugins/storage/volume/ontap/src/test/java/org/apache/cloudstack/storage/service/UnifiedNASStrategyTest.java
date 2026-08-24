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
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.host.HostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.client.JobFeignClient;
import org.apache.cloudstack.storage.feign.client.NASFeignClient;
import org.apache.cloudstack.storage.feign.client.VolumeFeignClient;
import org.apache.cloudstack.storage.feign.client.AggregateFeignClient;
import org.apache.cloudstack.storage.feign.client.SvmFeignClient;
import org.apache.cloudstack.storage.feign.client.NetworkFeignClient;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.model.ExportPolicy;
import org.apache.cloudstack.storage.feign.model.ExportRule;
import org.apache.cloudstack.storage.feign.model.FileCloneRequest;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;

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

    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;

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
        injectField("primaryDataStoreDao", primaryDataStoreDao);
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
            // All Feign clients are in StorageStrategy parent class
            injectParentMockedClient("nasFeignClient", nasFeignClient);
            injectParentMockedClient("volumeFeignClient", volumeFeignClient);
            injectParentMockedClient("jobFeignClient", jobFeignClient);
            injectParentMockedClient("aggregateFeignClient", aggregateFeignClient);
            injectParentMockedClient("svmFeignClient", svmFeignClient);
            injectParentMockedClient("networkFeignClient", networkFeignClient);
            injectParentMockedClient("sanFeignClient", sanFeignClient);
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
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.SVM_NAME, "svm1");
        details.put(OntapStorageConstants.VOLUME_UUID, "vol-uuid-123");
        details.put(OntapStorageConstants.VOLUME_NAME, "vol1");

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
        job.setState(OntapStorageConstants.JOB_SUCCESS);
        jobResponse.setJob(job);

        // Removed primaryDataStoreInfo mock - using storage pool ID directly
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
        when(accessGroup.getStoragePoolId()).thenReturn(1L);
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
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.SVM_NAME, "svm1");
        details.put(OntapStorageConstants.VOLUME_UUID, "vol-uuid-123");
        details.put(OntapStorageConstants.VOLUME_NAME, "vol1");

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageIpAddress()).thenReturn("10.0.0.1");
        hosts.add(host1);

        // Removed primaryDataStoreInfo mock - using storage pool ID directly
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
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
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.SVM_NAME, "svm1");
        details.put(OntapStorageConstants.VOLUME_UUID, "vol-uuid-123");
        details.put(OntapStorageConstants.VOLUME_NAME, "vol1");

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageIpAddress()).thenReturn("10.0.0.1");
        hosts.add(host1);

        OntapResponse<ExportPolicy> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        // Removed primaryDataStoreInfo mock - using storage pool ID directly
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
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
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.SVM_NAME, "svm1");
        details.put(OntapStorageConstants.VOLUME_UUID, "vol-uuid-123");
        details.put(OntapStorageConstants.VOLUME_NAME, "vol1");

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
        job.setState(OntapStorageConstants.JOB_FAILURE); // Set to FAILURE instead of timeout
        job.setMessage("Job failed");
        jobResponse.setJob(job);

        // Removed primaryDataStoreInfo mock - using storage pool ID directly
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
        when(accessGroup.getStoragePoolId()).thenReturn(1L);
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
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.SVM_NAME, "svm1");
        details.put(OntapStorageConstants.VOLUME_UUID, "vol-uuid-123");
        details.put(OntapStorageConstants.VOLUME_NAME, "vol1");

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
        job.setState(OntapStorageConstants.JOB_SUCCESS);
        jobResponse.setJob(job);

        // Removed primaryDataStoreInfo mock - using storage pool ID directly
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
        when(accessGroup.getStoragePoolId()).thenReturn(1L);
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
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.EXPORT_POLICY_NAME, "export-policy-1");
        details.put(OntapStorageConstants.EXPORT_POLICY_ID, "1");

        when(accessGroup.getStoragePoolId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
        // Removed primaryDataStoreInfo.getName() - not used
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
        when(accessGroup.getStoragePoolId()).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteAccessGroup(accessGroup);
        });
    }

    // Test deleteAccessGroup - Failed to Delete
    @Test
    public void testDeleteAccessGroup_Failed() {
        AccessGroup accessGroup = mock(AccessGroup.class);
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.EXPORT_POLICY_NAME, "export-policy-1");
        details.put(OntapStorageConstants.EXPORT_POLICY_ID, "1");

        when(accessGroup.getStoragePoolId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
        doThrow(new RuntimeException("Failed to delete")).when(nasFeignClient)
            .deleteExportPolicyById(anyString(), anyString());

        assertThrows(CloudRuntimeException.class, () -> {
            strategy.deleteAccessGroup(accessGroup);
        });
    }

    // Test deleteAccessGroup - Export policy not found should be treated as no-op
    @Test
    public void testDeleteAccessGroup_NotFound404_NoThrow() {
        AccessGroup accessGroup = mock(AccessGroup.class);
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.EXPORT_POLICY_NAME, "export-policy-1");
        details.put(OntapStorageConstants.EXPORT_POLICY_ID, "1");

        when(accessGroup.getStoragePoolId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);
        doThrow(feignException).when(nasFeignClient).deleteExportPolicyById(anyString(), eq("1"));

        strategy.deleteAccessGroup(accessGroup);

        verify(nasFeignClient).deleteExportPolicyById(anyString(), eq("1"));
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

    // -------------------------------------------------------------------------
    // updateAccessGroup tests
    // -------------------------------------------------------------------------

    private Map<String, String> detailsWithExportPolicyId() {
        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.EXPORT_POLICY_ID, "policy-42");
        return details;
    }

    private ExportPolicy existingPolicyWithClients(String... matchIps) {
        ExportRule rule = new ExportRule();
        List<ExportRule.ExportClient> clients = new ArrayList<>();
        for (String ip : matchIps) {
            ExportRule.ExportClient client = new ExportRule.ExportClient();
            client.setMatch(ip);
            clients.add(client);
        }
        rule.setClients(clients);
        ExportPolicy policy = new ExportPolicy();
        policy.setName("test-policy");
        policy.setRules(new ArrayList<>(List.of(rule)));
        return policy;
    }

    // updateAccessGroup - null accessGroup
    @Test
    public void testUpdateAccessGroup_NullAccessGroup() {
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(null));
    }

    // updateAccessGroup - null storagePoolId
    @Test
    public void testUpdateAccessGroup_NullStoragePoolId() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setHostsToConnect(List.of(mock(HostVO.class)));
        // storagePoolId is null by default
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - null hostsToConnect
    @Test
    public void testUpdateAccessGroup_NullHostsToConnect() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        // hostsToConnect is null by default
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - empty hostsToConnect
    @Test
    public void testUpdateAccessGroup_EmptyHostsToConnect() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(new ArrayList<>());
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - storagePoolDetailsDao returns null
    @Test
    public void testUpdateAccessGroup_NoStoragePoolDetails() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(mock(HostVO.class)));
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(null);
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - details missing EXPORT_POLICY_ID key
    @Test
    public void testUpdateAccessGroup_MissingExportPolicyId() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(mock(HostVO.class)));
        Map<String, String> details = new HashMap<>();
        details.put("someOtherKey", "someValue");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - getExportPolicyById returns null
    @Test
    public void testUpdateAccessGroup_ExportPolicyNotFound() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(mock(HostVO.class)));
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(null);
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - existing policy has null rules
    @Test
    public void testUpdateAccessGroup_NullRules() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(mock(HostVO.class)));
        ExportPolicy policy = new ExportPolicy();
        policy.setName("test-policy");
        policy.setRules(null);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(policy);
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - existing policy has empty rules list
    @Test
    public void testUpdateAccessGroup_EmptyRules() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(mock(HostVO.class)));
        ExportPolicy policy = new ExportPolicy();
        policy.setName("test-policy");
        policy.setRules(new ArrayList<>());
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(policy);
        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }

    // updateAccessGroup - all hosts have no IP: returns early without ONTAP patch
    @Test
    public void testUpdateAccessGroup_AllHostsHaveNoIp_ReturnsEarly() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn(null);
        when(host.getPrivateIpAddress()).thenReturn(null);

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));

        ExportPolicy existingPolicy = existingPolicyWithClients("10.0.0.1/32");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        AccessGroup result = strategy.updateAccessGroup(accessGroup);

        assertNotNull(result);
        assertSame(existingPolicy, result.getPolicy());
        verify(nasFeignClient, never()).updateExportPolicy(anyString(), anyString(), any());
    }

    // updateAccessGroup - ADD: new host IP added to policy
    @Test
    public void testUpdateAccessGroup_Add_NewHost_Success() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn("10.0.0.2");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));
        // default action is ADD

        ExportPolicy existingPolicy = existingPolicyWithClients("10.0.0.1/32");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        AccessGroup result = strategy.updateAccessGroup(accessGroup);

        assertNotNull(result);
        assertSame(existingPolicy, result.getPolicy());
        // Existing client + new client = 2
        assertEquals(2, existingPolicy.getRules().get(0).getClients().size());
        verify(nasFeignClient).updateExportPolicy(anyString(), eq("policy-42"), any(ExportPolicy.class));
    }

    // updateAccessGroup - ADD: host uses private IP when storage IP is absent
    @Test
    public void testUpdateAccessGroup_Add_UsesPrivateIpWhenStorageIpAbsent() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn(null);
        when(host.getPrivateIpAddress()).thenReturn("192.168.1.50");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));

        ExportPolicy existingPolicy = existingPolicyWithClients();
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        AccessGroup result = strategy.updateAccessGroup(accessGroup);

        assertNotNull(result);
        List<ExportRule.ExportClient> clients = existingPolicy.getRules().get(0).getClients();
        assertEquals(1, clients.size());
        assertEquals("192.168.1.50/32", clients.get(0).getMatch());
        verify(nasFeignClient).updateExportPolicy(anyString(), eq("policy-42"), any(ExportPolicy.class));
    }

    // updateAccessGroup - ADD: host IP already present in policy (no-op)
    @Test
    public void testUpdateAccessGroup_Add_DuplicateHost_NoUpdate() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn("10.0.0.1");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));

        ExportPolicy existingPolicy = existingPolicyWithClients("10.0.0.1/32");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        AccessGroup result = strategy.updateAccessGroup(accessGroup);

        assertNotNull(result);
        assertSame(existingPolicy, result.getPolicy());
        // Client count must remain 1 (no duplicate inserted)
        assertEquals(1, existingPolicy.getRules().get(0).getClients().size());
        verify(nasFeignClient, never()).updateExportPolicy(anyString(), anyString(), any());
    }

    // updateAccessGroup - ADD: existing rule has null clients list
    @Test
    public void testUpdateAccessGroup_Add_NullClientsInRule() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn("10.0.0.5");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));

        ExportRule rule = new ExportRule();
        rule.setClients(null); // null clients list
        ExportPolicy policy = new ExportPolicy();
        policy.setName("test-policy");
        policy.setRules(new ArrayList<>(List.of(rule)));

        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(policy);

        AccessGroup result = strategy.updateAccessGroup(accessGroup);

        assertNotNull(result);
        assertEquals(1, rule.getClients().size());
        assertEquals("10.0.0.5/32", rule.getClients().get(0).getMatch());
        verify(nasFeignClient).updateExportPolicy(anyString(), eq("policy-42"), any(ExportPolicy.class));
    }

    // updateAccessGroup - REMOVE: matching host IP removed from policy
    @Test
    public void testUpdateAccessGroup_Remove_MatchingHost_Success() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn("10.0.0.1");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));
        accessGroup.setHostRuleAction(AccessGroup.HostRuleAction.REMOVE);

        ExportPolicy existingPolicy = existingPolicyWithClients("10.0.0.1/32", "10.0.0.2/32");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        AccessGroup result = strategy.updateAccessGroup(accessGroup);

        assertNotNull(result);
        assertSame(existingPolicy, result.getPolicy());
        // Only 10.0.0.2/32 should remain
        List<ExportRule.ExportClient> clients = existingPolicy.getRules().get(0).getClients();
        assertEquals(1, clients.size());
        assertEquals("10.0.0.2/32", clients.get(0).getMatch());
        verify(nasFeignClient).updateExportPolicy(anyString(), eq("policy-42"), any(ExportPolicy.class));
    }

    // updateAccessGroup - REMOVE: IP not in policy (no-op)
    @Test
    public void testUpdateAccessGroup_Remove_IpNotPresent_NoUpdate() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn("10.0.0.99");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));
        accessGroup.setHostRuleAction(AccessGroup.HostRuleAction.REMOVE);

        ExportPolicy existingPolicy = existingPolicyWithClients("10.0.0.1/32");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        AccessGroup result = strategy.updateAccessGroup(accessGroup);

        assertNotNull(result);
        assertSame(existingPolicy, result.getPolicy());
        assertEquals(1, existingPolicy.getRules().get(0).getClients().size());
        verify(nasFeignClient, never()).updateExportPolicy(anyString(), anyString(), any());
    }

    // updateAccessGroup - FeignException from ONTAP wrapped in CloudRuntimeException
    @Test
    public void testUpdateAccessGroup_FeignExceptionWrapped() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn("10.0.0.1");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));

        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42")))
                .thenThrow(new RuntimeException("ONTAP unreachable"));

        assertThrows(CloudRuntimeException.class, () -> strategy.updateAccessGroup(accessGroup));
    }
    // updateAccessGroup - whitespace in storage IP is trimmed before building match
    @Test
    public void testUpdateAccessGroup_TrimsWhitespaceFromStorageIp() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn("  10.0.0.2  ");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));

        ExportPolicy existingPolicy = existingPolicyWithClients();
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        strategy.updateAccessGroup(accessGroup);

        List<ExportRule.ExportClient> clients = existingPolicy.getRules().get(0).getClients();
        assertEquals(1, clients.size());
        assertEquals("10.0.0.2/32", clients.get(0).getMatch());
    }

    // updateAccessGroup - whitespace in private IP is trimmed when storage IP absent
    @Test
    public void testUpdateAccessGroup_TrimsWhitespaceFromPrivateIp() {
        HostVO host = mock(HostVO.class);
        when(host.getStorageIpAddress()).thenReturn(null);
        when(host.getPrivateIpAddress()).thenReturn("  192.168.1.10  ");

        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setStoragePoolId(1L);
        accessGroup.setHostsToConnect(List.of(host));

        ExportPolicy existingPolicy = existingPolicyWithClients();
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(detailsWithExportPolicyId());
        when(nasFeignClient.getExportPolicyById(anyString(), eq("policy-42"))).thenReturn(existingPolicy);

        strategy.updateAccessGroup(accessGroup);

        List<ExportRule.ExportClient> clients = existingPolicy.getRules().get(0).getClients();
        assertEquals(1, clients.size());
        assertEquals("192.168.1.10/32", clients.get(0).getMatch());
    }

    @Test
    public void testCloneCloudStackVolume_Success() {
        VolumeObject volumeObject = mock(VolumeObject.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeObject.getId()).thenReturn(100L);
        when(volumeObject.getUuid()).thenReturn("volume-uuid");
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeDao.update(anyLong(), any(VolumeVO.class))).thenReturn(true);

        Map<String, String> details = new HashMap<>();
        details.put(OntapStorageConstants.SVM_NAME, "svm1");
        details.put(OntapStorageConstants.VOLUME_NAME, "flexvol1");
        details.put(OntapStorageConstants.VOLUME_UUID, "flexvol-uuid-1");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);

        FileInfo source = new FileInfo();
        source.setPath("template-uuid");
        CloudStackVolume request = new CloudStackVolume();
        request.setDatastoreId("1");
        request.setVolumeInfo(volumeObject);
        request.setFile(source);
        request.setDestinationPath("volume-uuid");

        when(nasFeignClient.cloneFile(anyString(), any(FileCloneRequest.class))).thenReturn(new JobResponse());

        CloudStackVolume result = strategy.cloneCloudStackVolume(request);

        assertNotNull(result);
        assertEquals("volume-uuid", result.getFile().getPath());
        ArgumentCaptor<FileCloneRequest> captor = ArgumentCaptor.forClass(FileCloneRequest.class);
        verify(nasFeignClient).cloneFile(anyString(), captor.capture());
        assertEquals("template-uuid", captor.getValue().getSourcePath());
        assertEquals("volume-uuid", captor.getValue().getDestinationPath());
        assertEquals("flexvol1", captor.getValue().getVolume().getName());
        assertEquals("flexvol-uuid-1", captor.getValue().getVolume().getUuid());
    }

    @Test
    public void testCloneCloudStackVolume_InvalidRequest_ThrowsException() {
        assertThrows(CloudRuntimeException.class, () -> strategy.cloneCloudStackVolume(null));
    }

    @Test
    public void testResizeCloudStackVolume_SendsResizeCommand() {
        VolumeObject volumeObject = mock(VolumeObject.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        EndPoint endPoint = mock(EndPoint.class);

        when(volumeObject.getId()).thenReturn(100L);
        when(volumeObject.getUuid()).thenReturn("volume-uuid");
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getPath()).thenReturn("volume-uuid");
        when(volumeVO.getSize()).thenReturn(5368709120L);
        when(volumeVO.getPoolId()).thenReturn(1L);
        when(primaryDataStoreDao.findById(1L)).thenReturn(storagePool);
        when(epSelector.select(volumeObject)).thenReturn(endPoint);
        when(endPoint.sendMessage(any(ResizeVolumeCommand.class))).thenReturn(new Answer(null, true, "Success"));

        CloudStackVolume request = new CloudStackVolume();
        request.setVolumeInfo(volumeObject);

        strategy.resizeCloudStackVolume(request, 21474836480L);

        verify(endPoint).sendMessage(any(ResizeVolumeCommand.class));
    }

    @Test
    public void testDeleteFileByPath_Treats404AsSuccess() {
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);
        doThrow(feignException).when(nasFeignClient).deleteFile(anyString(), eq("flexvol-uuid"), eq("template-uuid"));

        strategy.deleteFileByPath("flexvol-uuid", "template-uuid");

        verify(nasFeignClient).deleteFile(anyString(), eq("flexvol-uuid"), eq("template-uuid"));
    }
}
