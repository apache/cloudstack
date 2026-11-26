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

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.feign.client.AggregateFeignClient;
import org.apache.cloudstack.storage.feign.client.JobFeignClient;
import org.apache.cloudstack.storage.feign.client.NetworkFeignClient;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.client.SnapshotFeignClient;
import org.apache.cloudstack.storage.feign.client.SvmFeignClient;
import org.apache.cloudstack.storage.feign.client.VolumeFeignClient;
import org.apache.cloudstack.storage.feign.model.Aggregate;
import org.apache.cloudstack.storage.feign.model.IpInterface;
import org.apache.cloudstack.storage.feign.model.IscsiService;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

import feign.FeignException;
import feign.Request;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StorageStrategyTest {

    @Mock
    private AggregateFeignClient aggregateFeignClient;

    @Mock
    private VolumeFeignClient volumeFeignClient;

    @Mock
    private SvmFeignClient svmFeignClient;

    @Mock
    private JobFeignClient jobFeignClient;

    @Mock
    private NetworkFeignClient networkFeignClient;

    @Mock
    private SANFeignClient sanFeignClient;

    @Mock
    private SnapshotFeignClient snapshotFeignClient;

    private TestableStorageStrategy storageStrategy;

    // Concrete implementation for testing abstract class
    private static class TestableStorageStrategy extends StorageStrategy {
        public TestableStorageStrategy(OntapStorage ontapStorage,
                                       AggregateFeignClient aggregateFeignClient,
                                       VolumeFeignClient volumeFeignClient,
                                       SvmFeignClient svmFeignClient,
                                       JobFeignClient jobFeignClient,
                                       NetworkFeignClient networkFeignClient,
                                       SANFeignClient sanFeignClient,
                                       SnapshotFeignClient snapshotFeignClient) {
            super(ontapStorage);
            // Use reflection to replace the private Feign client fields with mocked ones
            injectMockedClient("aggregateFeignClient", aggregateFeignClient);
            injectMockedClient("volumeFeignClient", volumeFeignClient);
            injectMockedClient("svmFeignClient", svmFeignClient);
            injectMockedClient("jobFeignClient", jobFeignClient);
            injectMockedClient("networkFeignClient", networkFeignClient);
            injectMockedClient("sanFeignClient", sanFeignClient);
            injectMockedClient("snapshotFeignClient", snapshotFeignClient);
        }

        private void injectMockedClient(String fieldName, Object mockedClient) {
            try {
                Field field = StorageStrategy.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(this, mockedClient);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to inject mocked client: " + fieldName, e);
            }
        }

        @Override
        public CloudStackVolume createCloudStackVolume(CloudStackVolume cloudstackVolume) {
            return null;
        }

        @Override
        CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume) {
            return null;
        }

        @Override
        public void deleteCloudStackVolume(CloudStackVolume cloudstackVolume) {
        }

        @Override
        public void copyCloudStackVolume(CloudStackVolume cloudstackVolume) {

        }

        @Override
        public CloudStackVolume getCloudStackVolume(Map<String, String> cloudStackVolumeMap) {
            return null;
        }

        @Override
        public JobResponse revertSnapshotForCloudStackVolume(String snapshotName, String flexVolUuid, String snapshotUuid, String volumePath, String lunUuid, String flexVolName) {
            return null;
        }

        @Override
        public AccessGroup createAccessGroup(AccessGroup accessGroup) {
            return null;
        }

        @Override
        public void deleteAccessGroup(AccessGroup accessGroup) {
        }

        @Override
        public AccessGroup updateAccessGroup(AccessGroup accessGroup) {
            return null;
        }

        @Override
        public AccessGroup getAccessGroup(Map<String, String> values) {
            return null;
        }

        @Override
        public String enableLogicalAccess(Map<String, String> values) {
            return null;
        }

        @Override
        public void disableLogicalAccess(Map<String, String> values) {
        }

        @Override
        public String getLogicalAccess(Map<String, String> values) {
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        // Create OntapStorage using constructor (immutable object)
        OntapStorage ontapStorage = new OntapStorage("admin", "password", "192.168.1.100",
                "svm1", 5000000000L, ProtocolType.NFS3);

        // Note: In real implementation, StorageStrategy constructor creates Feign clients
        // For testing, we'll need to mock the FeignClientFactory behavior
        storageStrategy = new TestableStorageStrategy(ontapStorage,
                aggregateFeignClient, volumeFeignClient, svmFeignClient,
                jobFeignClient, networkFeignClient, sanFeignClient, snapshotFeignClient);
    }

    // ========== connect() Tests ==========

    @Test
    public void testConnect_positive() {
        // Setup
        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState(OntapStorageConstants.RUNNING);
        svm.setNfsEnabled(true);

        Aggregate aggregate = new Aggregate();
        aggregate.setName("aggr1");
        aggregate.setUuid("aggr-uuid-1");
        svm.setAggregates(List.of(aggregate));

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);
        Aggregate aggregateDetail = buildAggregate("aggr1", "aggr-uuid-1", 10000000000.0);
        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap())).thenReturn(aggregateDetail);

        // Execute
        boolean result = storageStrategy.connect();

        // Verify
        assertTrue(result, "connect() should return true on success");
        verify(svmFeignClient, times(1)).getSvmResponse(anyMap(), anyString());
    }

    @Test
    public void testConnect_operationsOnly_skipsAggregateValidation() {
        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState(OntapStorageConstants.RUNNING);
        svm.setNfsEnabled(true);

        Aggregate aggregate = new Aggregate();
        aggregate.setName("aggr1");
        aggregate.setUuid("aggr-uuid-1");
        svm.setAggregates(List.of(aggregate));

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        // Aggregate is ONLINE but has far less free space than the configured pool size (5GB).
        Aggregate aggregateDetail = buildAggregate("aggr1", "aggr-uuid-1", 1000000.0); // only 1MB free
        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap())).thenReturn(aggregateDetail);

        // Execute & Verify - connect(false) should succeed regardless of available space.
        boolean result = storageStrategy.connect(false);
        assertTrue(result, "connect() should succeed for an online aggregate even when its free space is below the pool capacity");
    }

    @Test
    public void testConnect_noOnlineAggregates() {
        // Setup - aggregate assigned to the SVM exists but is not ONLINE
        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState(OntapStorageConstants.RUNNING);
        svm.setNfsEnabled(true);

        Aggregate aggregate = new Aggregate();
        aggregate.setName("aggr1");
        aggregate.setUuid("aggr-uuid-1");
        svm.setAggregates(List.of(aggregate));

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        Aggregate aggregateDetail = new Aggregate();
        aggregateDetail.setName("aggr1");
        aggregateDetail.setUuid("aggr-uuid-1");
        aggregateDetail.setState(null); // not online
        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap())).thenReturn(aggregateDetail);

        // Execute & Verify
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("No suitable aggregates found"));
        boolean result = storageStrategy.connect(false);

        assertTrue(result);
        // connect(true) called getAggregateByUUID once; connect(false) must not add more calls
        verify(aggregateFeignClient, times(1)).getAggregateByUUID(anyString(), anyString(), anyMap());
    }

    @Test
    public void testConnect_svmNotFound() {
        // Setup
        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(new ArrayList<>());

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        // Execute & Verify
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("No SVM found"));
    }

    @Test
    public void testConnect_svmNotRunning() {
        // Setup
        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState("stopped");
        svm.setNfsEnabled(true);

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        // Execute & Verify
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("not in running state"));
    }

    @Test
    public void testConnect_nfsNotEnabled() {
        // Setup
        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState(OntapStorageConstants.RUNNING);
        svm.setNfsEnabled(false);

        Aggregate aggregate = new Aggregate();
        aggregate.setName("aggr1");
        aggregate.setUuid("aggr-uuid-1");
        svm.setAggregates(List.of(aggregate));

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        // Execute & Verify
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("NFS protocol is not enabled"));
    }

    @Test
    public void testConnect_iscsiNotEnabled() {
        // Setup - recreate with iSCSI protocol
        OntapStorage iscsiStorage = new OntapStorage("admin", "password", "192.168.1.100",
                "svm1", 5000000000L, ProtocolType.ISCSI);
        storageStrategy = new TestableStorageStrategy(iscsiStorage,
                aggregateFeignClient, volumeFeignClient, svmFeignClient,
                jobFeignClient, networkFeignClient, sanFeignClient, snapshotFeignClient);

        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState(OntapStorageConstants.RUNNING);
        svm.setIscsiEnabled(false);

        Aggregate aggregate = new Aggregate();
        aggregate.setName("aggr1");
        aggregate.setUuid("aggr-uuid-1");
        svm.setAggregates(List.of(aggregate));

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        // Execute & Verify
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("ISCSI protocol is not enabled"));
    }

    @Test
    public void testConnect_noAggregates() {
        // Setup
        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState(OntapStorageConstants.RUNNING);
        svm.setNfsEnabled(true);
        svm.setAggregates(new ArrayList<>());

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        // Execute & Verify
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("No aggregates"));
    }

    @Test
    public void testConnect_nullSvmResponse() {
        // Setup
        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(null);

        // Execute & Verify
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("No SVM found"));
    }

    @Test
    public void testConnect_invalidCredentials() {
        // Setup - ONTAP rejects the supplied username/password with HTTP 401 Unauthorized.
        Map<String, Collection<String>> emptyHeaders = Collections.emptyMap();
        Request dummyReq = Request.create(Request.HttpMethod.GET, "http://test", emptyHeaders, (byte[]) null, (Charset) null);
        when(svmFeignClient.getSvmResponse(anyMap(), anyString()))
                .thenThrow(new FeignException.Unauthorized("Unauthorized", dummyReq, null));

        // Execute & Verify - connect() must surface a clear "invalid credentials" error.
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> storageStrategy.connect());
        assertTrue(ex.getMessage().contains("Authentication failed: Invalid credentials"),
                "Expected an authentication failure message but got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Please verify the username and password"),
                "Expected the message to prompt verifying username/password but got: " + ex.getMessage());
    }

    // ========== createStorageVolume() Tests ==========

    @Test
    public void testCreateStorageVolume_positive() {
        // Setup - First connect to populate aggregates
        setupSuccessfulConnect();
        storageStrategy.connect();

        // Setup aggregate details
        Aggregate aggregateDetail = buildAggregate("aggr1", "aggr-uuid-1", 10000000000.0);
        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap()))
                .thenReturn(aggregateDetail);

        // Setup job response
        Job job = new Job();
        job.setUuid("job-uuid-1");
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJob(job);

        when(volumeFeignClient.createVolumeWithJob(anyString(), any(Volume.class)))
                .thenReturn(jobResponse);

        // Setup job polling
        Job completedJob = new Job();
        completedJob.setUuid("job-uuid-1");
        completedJob.setState(OntapStorageConstants.JOB_SUCCESS);
        when(jobFeignClient.getJobByUUID(anyString(), eq("job-uuid-1")))
                .thenReturn(completedJob);

        // Setup volume retrieval after creation
        Volume createdVolume = new Volume();
        createdVolume.setName("test-volume");
        createdVolume.setUuid("vol-uuid-1");
        OntapResponse<Volume> volumeResponse = new OntapResponse<>();
        volumeResponse.setRecords(List.of(createdVolume));

        when(volumeFeignClient.getAllVolumes(anyString(), anyMap()))
                .thenReturn(volumeResponse);
        when(volumeFeignClient.getVolume(anyString(), anyMap()))
                .thenReturn(volumeResponse);

        // Execute
        Volume result = storageStrategy.createStorageVolume("test-volume", 5000000000L);

        // Verify
        assertNotNull(result);
        assertEquals("test-volume", result.getName());
        assertEquals("vol-uuid-1", result.getUuid());
        verify(volumeFeignClient, times(1)).createVolumeWithJob(anyString(), any(Volume.class));
        verify(jobFeignClient, atLeastOnce()).getJobByUUID(anyString(), eq("job-uuid-1"));
    }

    @Test
    public void testCreateStorageVolume_invalidSize() {
        // Setup
        setupSuccessfulConnect();
        storageStrategy.connect();

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.createStorageVolume("test-volume", -1L));
        assertTrue(ex.getMessage().contains("Invalid volume size"));
    }

    @Test
    public void testCreateStorageVolume_nullSize() {
        // Setup
        setupSuccessfulConnect();
        storageStrategy.connect();

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.createStorageVolume("test-volume", null));
        assertTrue(ex.getMessage().contains("Invalid volume size"));
    }

    @Test
    public void testCreateStorageVolume_noAggregates() {
        // Execute & Verify - without calling connect first
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.createStorageVolume("test-volume", 5000000000L));
        assertTrue(ex.getMessage().contains("No aggregates available"));
    }

    @Test
    public void testCreateStorageVolume_aggregateNotOnline() {
        // Setup
        setupSuccessfulConnect();
        storageStrategy.connect();

        Aggregate aggregateDetail = new Aggregate();
        aggregateDetail.setName("aggr1");
        aggregateDetail.setUuid("aggr-uuid-1");
        aggregateDetail.setState(null); // null state to simulate offline

        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap()))
                .thenReturn(aggregateDetail);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.createStorageVolume("test-volume", 5000000000L));
        assertTrue(ex.getMessage().contains("No suitable aggregates found"));
    }

    @Test
    public void testCreateStorageVolume_insufficientSpace() {
        // Setup
        setupSuccessfulConnect();
        storageStrategy.connect();

        Aggregate aggregateDetail = buildAggregate("aggr1", "aggr-uuid-1", 1000000.0); // Only 1MB available

        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap()))
                .thenReturn(aggregateDetail);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.createStorageVolume("test-volume", 5000000000L)); // Request 5GB
        assertTrue(ex.getMessage().contains("No suitable aggregates found"));
    }

    @Test
    public void testCreateStorageVolume_jobFailed() {
        // Setup
        setupSuccessfulConnect();
        storageStrategy.connect();

        setupAggregateForVolumeCreation();

        Job job = new Job();
        job.setUuid("job-uuid-1");
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJob(job);

        when(volumeFeignClient.createVolumeWithJob(anyString(), any(Volume.class)))
                .thenReturn(jobResponse);

        // Setup failed job
        Job failedJob = new Job();
        failedJob.setUuid("job-uuid-1");
        failedJob.setState(OntapStorageConstants.JOB_FAILURE);
        failedJob.setMessage("Volume creation failed");
        when(jobFeignClient.getJobByUUID(anyString(), eq("job-uuid-1")))
                .thenReturn(failedJob);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.createStorageVolume("test-volume", 5000000000L));
        assertTrue(ex.getMessage().contains("failed") || ex.getMessage().contains("Job failed"));
    }

    @Test
    public void testCreateStorageVolume_volumeNotFoundAfterCreation() {
        // Setup
        setupSuccessfulConnect();
        storageStrategy.connect();
        setupAggregateForVolumeCreation();
        setupSuccessfulJobCreation();

        // Setup empty volume response
        OntapResponse<Volume> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        when(volumeFeignClient.getAllVolumes(anyString(), anyMap()))
                .thenReturn(emptyResponse);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.createStorageVolume("test-volume", 5000000000L));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("not found after creation"));
    }

    // ========== deleteStorageVolume() Tests ==========

    @Test
    public void testDeleteStorageVolume_positive() {
        // Setup
        Volume volume = new Volume();
        volume.setName("test-volume");
        volume.setUuid("vol-uuid-1");

        Job job = new Job();
        job.setUuid("job-uuid-1");
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJob(job);

        when(volumeFeignClient.deleteVolume(anyString(), eq("vol-uuid-1")))
                .thenReturn(jobResponse);

        Job completedJob = new Job();
        completedJob.setUuid("job-uuid-1");
        completedJob.setState(OntapStorageConstants.JOB_SUCCESS);
        when(jobFeignClient.getJobByUUID(anyString(), eq("job-uuid-1")))
                .thenReturn(completedJob);

        // Execute
        storageStrategy.deleteStorageVolume(volume);

        // Verify
        verify(volumeFeignClient, times(1)).deleteVolume(anyString(), eq("vol-uuid-1"));
        verify(jobFeignClient, atLeastOnce()).getJobByUUID(anyString(), eq("job-uuid-1"));
    }

    @Test
    public void testDeleteStorageVolume_jobFailed() {
        // Setup
        Volume volume = new Volume();
        volume.setName("test-volume");
        volume.setUuid("vol-uuid-1");

        Job job = new Job();
        job.setUuid("job-uuid-1");
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJob(job);

        when(volumeFeignClient.deleteVolume(anyString(), eq("vol-uuid-1")))
                .thenReturn(jobResponse);

        Job failedJob = new Job();
        failedJob.setUuid("job-uuid-1");
        failedJob.setState(OntapStorageConstants.JOB_FAILURE);
        failedJob.setMessage("Deletion failed");
        when(jobFeignClient.getJobByUUID(anyString(), eq("job-uuid-1")))
                .thenReturn(failedJob);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.deleteStorageVolume(volume));
        assertTrue(ex.getMessage().contains("Job failed"));
    }

    @Test
    public void testDeleteStorageVolume_feignException() {
        // Setup
        Volume volume = new Volume();
        volume.setName("test-volume");
        volume.setUuid("vol-uuid-1");

        Map<String, Collection<String>> emptyHeaders = Collections.emptyMap();
        Request dummyReq = Request.create(Request.HttpMethod.DELETE, "http://test", emptyHeaders, (byte[]) null, (Charset) null);
        when(volumeFeignClient.deleteVolume(anyString(), eq("vol-uuid-1")))
                .thenThrow(new FeignException.FeignClientException(500, "error", dummyReq, null));

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.deleteStorageVolume(volume));
        assertTrue(ex.getMessage().contains("Failed to delete volume"));
    }

    @Test
    public void testDeleteStorageVolume_notFound_404_returnsWithoutThrowing() {
        // Setup
        Volume volume = new Volume();
        volume.setName("test-volume");
        volume.setUuid("vol-uuid-1");

        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(404);
        when(volumeFeignClient.deleteVolume(anyString(), eq("vol-uuid-1")))
                .thenThrow(feignEx);

        // Execute - 404 means volume already gone on ONTAP, treated as no-op
        storageStrategy.deleteStorageVolume(volume);

        // Verify the delete was attempted
        verify(volumeFeignClient).deleteVolume(anyString(), eq("vol-uuid-1"));
    }

    // ========== getStoragePath() Tests ==========

    @Test
    public void testGetStoragePath_iscsi() {
        // Setup - recreate with iSCSI protocol
        OntapStorage iscsiStorage = new OntapStorage("admin", "password", "192.168.1.100",
                "svm1", null, ProtocolType.ISCSI);
        storageStrategy = new TestableStorageStrategy(iscsiStorage,
                aggregateFeignClient, volumeFeignClient, svmFeignClient,
                jobFeignClient, networkFeignClient, sanFeignClient, snapshotFeignClient);

        IscsiService.IscsiServiceTarget target = new IscsiService.IscsiServiceTarget();
        target.setName("iqn.1992-08.com.netapp:sn.123456:vs.1");

        IscsiService iscsiService = new IscsiService();
        iscsiService.setTarget(target);

        OntapResponse<IscsiService> iscsiResponse = new OntapResponse<>();
        iscsiResponse.setRecords(List.of(iscsiService));

        when(sanFeignClient.getIscsiServices(anyString(), anyMap()))
                .thenReturn(iscsiResponse);

        // Execute
        String result = storageStrategy.getStoragePath();

        // Verify
        assertNotNull(result);
        assertEquals("iqn.1992-08.com.netapp:sn.123456:vs.1", result);
        verify(sanFeignClient, times(1)).getIscsiServices(anyString(), anyMap());
    }

    @Test
    public void testGetStoragePath_iscsi_noService() {
        // Setup - recreate with iSCSI protocol
        OntapStorage iscsiStorage = new OntapStorage("admin", "password", "192.168.1.100",
                "svm1", null, ProtocolType.ISCSI);
        storageStrategy = new TestableStorageStrategy(iscsiStorage,
                aggregateFeignClient, volumeFeignClient, svmFeignClient,
                jobFeignClient, networkFeignClient, sanFeignClient, snapshotFeignClient);

        OntapResponse<IscsiService> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        when(sanFeignClient.getIscsiServices(anyString(), anyMap()))
                .thenReturn(emptyResponse);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.getStoragePath());
        assertTrue(ex.getMessage().contains("No iSCSI service found"));
    }

    @Test
    public void testGetStoragePath_iscsi_noTargetIqn() {
        // Setup - recreate with iSCSI protocol
        OntapStorage iscsiStorage = new OntapStorage("admin", "password", "192.168.1.100",
                "svm1", null, ProtocolType.ISCSI);
        storageStrategy = new TestableStorageStrategy(iscsiStorage,
                aggregateFeignClient, volumeFeignClient, svmFeignClient,
                jobFeignClient, networkFeignClient, sanFeignClient, snapshotFeignClient);

        IscsiService iscsiService = new IscsiService();
        iscsiService.setTarget(null);

        OntapResponse<IscsiService> iscsiResponse = new OntapResponse<>();
        iscsiResponse.setRecords(List.of(iscsiService));

        when(sanFeignClient.getIscsiServices(anyString(), anyMap()))
                .thenReturn(iscsiResponse);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.getStoragePath());
        assertTrue(ex.getMessage().contains("iSCSI target IQN not found"));
    }

    // ========== getNetworkInterface() Tests ==========

    @Test
    public void testGetNetworkInterface_nfs() {
        // Setup
        IpInterface.IpInfo ipInfo = new IpInterface.IpInfo();
        ipInfo.setAddress("192.168.1.50");

        IpInterface ipInterface = new IpInterface();
        ipInterface.setIp(ipInfo);
        ipInterface.setState(OntapStorageConstants.LIF_STATE_UP);
        ipInterface.setEnabled(true);

        OntapResponse<IpInterface> interfaceResponse = new OntapResponse<>();
        interfaceResponse.setRecords(List.of(ipInterface));

        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(interfaceResponse);

        // Execute
        Pair<String, String> result = storageStrategy.getNetworkInterface();

        // Verify
        assertNotNull(result);
        assertEquals("192.168.1.50", result.first());
        assertTrue(result.second() == null, "Expect no warning when a suitable LIF is found");
        verify(networkFeignClient, times(1)).getNetworkIpInterfaces(anyString(), anyMap());
    }

    @Test
    public void testGetNetworkInterface_iscsi() {
        // Setup - recreate with iSCSI protocol
        OntapStorage iscsiStorage = new OntapStorage("admin", "password", "192.168.1.100",
                "svm1", null, ProtocolType.ISCSI);
        storageStrategy = new TestableStorageStrategy(iscsiStorage,
                aggregateFeignClient, volumeFeignClient, svmFeignClient,
                jobFeignClient, networkFeignClient, sanFeignClient, snapshotFeignClient);

        IpInterface.IpInfo ipInfo = new IpInterface.IpInfo();
        ipInfo.setAddress("192.168.1.51");

        IpInterface ipInterface = new IpInterface();
        ipInterface.setIp(ipInfo);
        ipInterface.setState(OntapStorageConstants.LIF_STATE_UP);
        ipInterface.setEnabled(true);

        OntapResponse<IpInterface> interfaceResponse = new OntapResponse<>();
        interfaceResponse.setRecords(List.of(ipInterface));

        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(interfaceResponse);

        // Execute
        Pair<String, String> result = storageStrategy.getNetworkInterface();

        // Verify
        assertNotNull(result);
        assertEquals("192.168.1.51", result.first());
        assertTrue(result.second() == null, "Expect no warning when a suitable LIF is found");
    }

    @Test
    public void testGetNetworkInterface_nfs_lifDown() {
        // LIF exists but is operationally down — should fail
        IpInterface.IpInfo ipInfo = new IpInterface.IpInfo();
        ipInfo.setAddress("192.168.1.50");

        IpInterface ipInterface = new IpInterface();
        ipInterface.setIp(ipInfo);
        ipInterface.setState("down");
        ipInterface.setEnabled(true);

        OntapResponse<IpInterface> interfaceResponse = new OntapResponse<>();
        interfaceResponse.setRecords(List.of(ipInterface));

        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(interfaceResponse);

        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.getNetworkInterface());
        assertTrue(ex.getMessage().contains("operationally UP and enabled"));
    }

    @Test
    public void testGetNetworkInterface_nfs_lifDisabled() {
        // LIF exists but is administratively disabled — should fail
        IpInterface.IpInfo ipInfo = new IpInterface.IpInfo();
        ipInfo.setAddress("192.168.1.50");

        IpInterface ipInterface = new IpInterface();
        ipInterface.setIp(ipInfo);
        ipInterface.setState(OntapStorageConstants.LIF_STATE_UP);
        ipInterface.setEnabled(false);

        OntapResponse<IpInterface> interfaceResponse = new OntapResponse<>();
        interfaceResponse.setRecords(List.of(ipInterface));

        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(interfaceResponse);

        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.getNetworkInterface());
        assertTrue(ex.getMessage().contains("operationally UP and enabled"));
    }

    @Test
    public void testGetNetworkInterface_iscsi_lifDown() {
        // iSCSI LIF exists but is operationally down — should fail
        OntapStorage iscsiStorage = new OntapStorage("admin", "password", "192.168.1.100",
                "svm1", null, ProtocolType.ISCSI);
        storageStrategy = new TestableStorageStrategy(iscsiStorage,
                aggregateFeignClient, volumeFeignClient, svmFeignClient,
                jobFeignClient, networkFeignClient, sanFeignClient, snapshotFeignClient);

        IpInterface.IpInfo ipInfo = new IpInterface.IpInfo();
        ipInfo.setAddress("192.168.1.51");

        IpInterface ipInterface = new IpInterface();
        ipInterface.setIp(ipInfo);
        ipInterface.setState("down");
        ipInterface.setEnabled(true);

        OntapResponse<IpInterface> interfaceResponse = new OntapResponse<>();
        interfaceResponse.setRecords(List.of(ipInterface));

        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(interfaceResponse);

        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.getNetworkInterface());
        assertTrue(ex.getMessage().contains("operationally UP and enabled"));
    }

    @Test
    public void testGetNetworkInterface_noInterfaces() {
        // Setup
        OntapResponse<IpInterface> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(emptyResponse);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.getNetworkInterface());
        assertTrue(ex.getMessage().contains("No network interfaces found"));
    }

    @Test
    public void testGetNetworkInterface_feignException() {
        // Setup
        Map<String, Collection<String>> emptyHeaders = Collections.emptyMap();
        Request dummyReq = Request.create(Request.HttpMethod.GET, "http://test", emptyHeaders, (byte[]) null, (Charset) null);
        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenThrow(new FeignException.FeignClientException(500, "error", dummyReq, null));

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.getNetworkInterface());
        assertTrue(ex.getMessage().contains("Failed to retrieve network interfaces"));
    }

    // ========== getNetworkInterface() Node-Affinity Tests ==========

    /**
     * Tier 1: LIF homed on the same node as the chosen aggregate — selected without warning.
     */
    @Test
    public void testGetNetworkInterface_nfs_tier1_homeNodeMatch() {
        injectChosenAggregateNode(storageStrategy, "node-a");

        IpInterface lif = buildLif("10.0.0.1", OntapStorageConstants.LIF_STATE_UP, true, "node-a", "node-a");
        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(wrapLifs(List.of(lif)));

        Pair<String, String> result = storageStrategy.getNetworkInterface();

        assertEquals("10.0.0.1", result.first());
        assertTrue(result.second() == null, "Tier 1 should produce no warning");
    }

    /**
     * Tier 2: No home-node match, but another UP LIF is currently running on the target node (failover).
     * The result should carry a warning.
     */
    @Test
    public void testGetNetworkInterface_nfs_tier2_currentNodeMatch() {
        injectChosenAggregateNode(storageStrategy, "node-a");

        // home node = node-b, currently running on node-a after failover
        IpInterface lif = buildLif("10.0.0.2", OntapStorageConstants.LIF_STATE_UP, true, "node-b", "node-a");
        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(wrapLifs(List.of(lif)));

        Pair<String, String> result = storageStrategy.getNetworkInterface();

        assertEquals("10.0.0.2", result.first());
        assertTrue(result.second() != null, "Tier 2 should produce a warning");
        assertTrue(result.second().contains("node-a"));
    }

    /**
     * Tier 3 fallback: No LIF matches the target node in either home_node or current node.
     * First UP/enabled LIF used; result carries a warning directing the user to create a
     * LIF on the correct node.
     */
    @Test
    public void testGetNetworkInterface_nfs_tier3_crossNodeFallback() {
        injectChosenAggregateNode(storageStrategy, "node-a");

        // Both home_node and current node are node-b — no affinity to node-a
        IpInterface lif = buildLif("10.0.0.3", OntapStorageConstants.LIF_STATE_UP, true, "node-b", "node-b");
        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(wrapLifs(List.of(lif)));

        Pair<String, String> result = storageStrategy.getNetworkInterface();

        assertEquals("10.0.0.3", result.first());
        assertTrue(result.second() != null, "Tier 3 fallback should produce a warning");
        assertTrue(result.second().contains("node-a"),
                "Warning should mention the expected node");
        assertTrue(result.second().contains("10.0.0.3"),
                "Warning should mention the fallback LIF IP");
    }

    /**
     * When chosenAggregateNode is null (volume not yet created / no aggregate info),
     * any UP/enabled LIF is returned without warning.
     */
    @Test
    public void testGetNetworkInterface_nfs_noAggregateNode_noWarning() {
        // chosenAggregateNode is null by default — no node affinity context
        IpInterface lif = buildLif("10.0.0.4", OntapStorageConstants.LIF_STATE_UP, true, "node-a", "node-a");
        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(wrapLifs(List.of(lif)));

        Pair<String, String> result = storageStrategy.getNetworkInterface();

        assertEquals("10.0.0.4", result.first());
        // With no chosenAggregateNode, tier 1/2 selection is skipped — result falls through to tier 3
        // but since there's no "expected node" in the warning message (chosenAggregateNode is null),
        // the message text will still contain "null" — we simply verify no exception is thrown and IP is correct.
        // (Tier 3 warning is generated when chosenAggregateNode != null; here it is null so no warning)
        assertTrue(result.second() == null, "No warning when chosenAggregateNode is null");
    }

    /**
     * Tier-1 LIF is down; Tier-2 LIF matches the current node and should be selected with a warning.
     */
    @Test
    public void testGetNetworkInterface_nfs_tier1Down_tier2Used() {
        injectChosenAggregateNode(storageStrategy, "node-a");

        // Tier 1 candidate: home_node = node-a but operationally DOWN
        IpInterface lifDown = buildLif("10.0.0.5", "down", true, "node-a", "node-a");
        // Tier 2 candidate: home_node = node-b, currently on node-a
        IpInterface lifFailover = buildLif("10.0.0.6", OntapStorageConstants.LIF_STATE_UP, true, "node-b", "node-a");

        when(networkFeignClient.getNetworkIpInterfaces(anyString(), anyMap()))
                .thenReturn(wrapLifs(List.of(lifDown, lifFailover)));

        Pair<String, String> result = storageStrategy.getNetworkInterface();

        assertEquals("10.0.0.6", result.first());
        assertTrue(result.second() != null, "Should warn that the home-node LIF is not in use");
    }

    // ========== Helper Methods ==========

    private void setupSuccessfulConnect() {
        Svm svm = new Svm();
        svm.setName("svm1");
        svm.setState(OntapStorageConstants.RUNNING);
        svm.setNfsEnabled(true);

        Aggregate aggregate = new Aggregate();
        aggregate.setName("aggr1");
        aggregate.setUuid("aggr-uuid-1");
        svm.setAggregates(List.of(aggregate));

        OntapResponse<Svm> svmResponse = new OntapResponse<>();
        svmResponse.setRecords(List.of(svm));

        when(svmFeignClient.getSvmResponse(anyMap(), anyString())).thenReturn(svmResponse);

        Aggregate aggregateDetail = buildAggregate("aggr1", "aggr-uuid-1", 10000000000.0);
        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap())).thenReturn(aggregateDetail);
    }

    private void setupAggregateForVolumeCreation() {
        Aggregate aggregateDetail = buildAggregate("aggr1", "aggr-uuid-1", 10000000000.0);
        when(aggregateFeignClient.getAggregateByUUID(anyString(), eq("aggr-uuid-1"), anyMap()))
                .thenReturn(aggregateDetail);
    }

    private void setupSuccessfulJobCreation() {
        Job job = new Job();
        job.setUuid("job-uuid-1");
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJob(job);

        when(volumeFeignClient.createVolumeWithJob(anyString(), any(Volume.class)))
                .thenReturn(jobResponse);

        Job completedJob = new Job();
        completedJob.setUuid("job-uuid-1");
        completedJob.setState(OntapStorageConstants.JOB_SUCCESS);
        when(jobFeignClient.getJobByUUID(anyString(), eq("job-uuid-1")))
                .thenReturn(completedJob);

        Volume createdVolume = new Volume();
        createdVolume.setName("test-volume");
        createdVolume.setUuid("vol-uuid-1");
        OntapResponse<Volume> volumeResponse = new OntapResponse<>();
        volumeResponse.setRecords(List.of(createdVolume));

        when(volumeFeignClient.getAllVolumes(anyString(), anyMap()))
                .thenReturn(volumeResponse);
        when(volumeFeignClient.getVolume(anyString(), anyMap()))
                .thenReturn(volumeResponse);
    }

    /**
     * Injects a value into the private {@code chosenAggregateNode} field of StorageStrategy
     * so node-affinity tests can exercise all three selection tiers without having to drive
     * the full {@code createStorageVolume()} flow.
     */
    private static void injectChosenAggregateNode(StorageStrategy strategy, String nodeName) {
        try {
            Field field = StorageStrategy.class.getDeclaredField("chosenAggregateNode");
            field.setAccessible(true);
            field.set(strategy, nodeName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject chosenAggregateNode", e);
        }
    }

    /**
     * Builds an {@link IpInterface} with all node-affinity fields populated.
     *
     * @param ip          the LIF's IP address (IPv4 for NFS3 selection to work)
     * @param state       operational state (e.g. "up" or "down")
     * @param enabled     administrative state
     * @param homeNode    name of the node the LIF is homed to
     * @param currentNode name of the node the LIF is currently running on
     */
    private static IpInterface buildLif(String ip, String state, boolean enabled,
                                        String homeNode, String currentNode) {
        IpInterface.IpInfo ipInfo = new IpInterface.IpInfo();
        ipInfo.setAddress(ip);

        IpInterface.Node homeNodeObj = new IpInterface.Node();
        homeNodeObj.setName(homeNode);

        IpInterface.Node currentNodeObj = new IpInterface.Node();
        currentNodeObj.setName(currentNode);

        IpInterface.Location location = new IpInterface.Location();
        location.setHomeNode(homeNodeObj);
        location.setNode(currentNodeObj);

        IpInterface lif = new IpInterface();
        lif.setIp(ipInfo);
        lif.setState(state);
        lif.setEnabled(enabled);
        lif.setLocation(location);
        return lif;
    }

    private static OntapResponse<IpInterface> wrapLifs(List<IpInterface> lifs) {
        OntapResponse<IpInterface> response = new OntapResponse<>();
        response.setRecords(lifs);
        return response;
    }

    /**
     * Creates a real {@link Aggregate} with nested space information so tests can avoid
     * {@code mock(Aggregate.class)} which fails on JDK 26+ due to Byte Buddy limitations.
     */
    private static Aggregate buildAggregate(String name, String uuid, double availableBytes) {
        Aggregate.AggregateSpaceBlockStorage blockStorage = new Aggregate.AggregateSpaceBlockStorage();
        blockStorage.setAvailable(availableBytes);

        Aggregate.AggregateSpace space = new Aggregate.AggregateSpace();
        space.setBlockStorage(blockStorage);

        Aggregate agg = new Aggregate();
        agg.setName(name);
        agg.setUuid(uuid);
        agg.setState(Aggregate.StateEnum.ONLINE);
        agg.setSpace(space);
        return agg;
    }

    // ========== pollJobIfPresent / executeCliSfsrRestore Tests ==========

    @Test
    void testPollJobIfPresent_NoJob_DoesNotPoll() {
        storageStrategy.pollJobIfPresent(null, "test operation");
        storageStrategy.pollJobIfPresent(new JobResponse(), "test operation");
        verify(jobFeignClient, times(0)).getJobByUUID(anyString(), anyString());
    }

    @Test
    void testPollJobIfPresent_WithJob_PollsUntilSuccess() {
        Job job = new Job();
        job.setUuid("sfsr-job-1");
        JobResponse response = new JobResponse();
        response.setJob(job);

        Job completedJob = new Job();
        completedJob.setUuid("sfsr-job-1");
        completedJob.setState(OntapStorageConstants.JOB_SUCCESS);
        when(jobFeignClient.getJobByUUID(anyString(), eq("sfsr-job-1"))).thenReturn(completedJob);

        storageStrategy.executeCliSfsrRestore(response, "CLI SFSR restore");

        verify(jobFeignClient, atLeastOnce()).getJobByUUID(anyString(), eq("sfsr-job-1"));
    }

    @Test
    void testPollJobIfPresent_JobFailure_ThrowsCloudRuntimeException() {
        Job job = new Job();
        job.setUuid("sfsr-job-fail");
        JobResponse response = new JobResponse();
        response.setJob(job);

        Job failedJob = new Job();
        failedJob.setUuid("sfsr-job-fail");
        failedJob.setState(OntapStorageConstants.JOB_FAILURE);
        failedJob.setMessage("restore failed");
        when(jobFeignClient.getJobByUUID(anyString(), eq("sfsr-job-fail"))).thenReturn(failedJob);

        assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.executeCliSfsrRestore(response, "CLI SFSR restore"));
    }

    @Test
    void testDeleteFlexVolSnapshotForCloudStackVolume_PollsJobAndSucceeds() {
        Job job = new Job();
        job.setUuid("delete-job-1");
        JobResponse response = new JobResponse();
        response.setJob(job);
        when(snapshotFeignClient.deleteSnapshot(anyString(), eq("fv-uuid-1"), eq("snap-uuid-1")))
                .thenReturn(response);

        Job completedJob = new Job();
        completedJob.setUuid("delete-job-1");
        completedJob.setState(OntapStorageConstants.JOB_SUCCESS);
        when(jobFeignClient.getJobByUUID(anyString(), eq("delete-job-1"))).thenReturn(completedJob);

        storageStrategy.deleteFlexVolSnapshotForCloudStackVolume("fv-uuid-1", "snap-uuid-1", "snap-name-1");

        verify(snapshotFeignClient).deleteSnapshot(anyString(), eq("fv-uuid-1"), eq("snap-uuid-1"));
    }

    @Test
    void testDeleteFlexVolSnapshotForCloudStackVolume_AlreadyAbsentOnOntap() {
        Job job = new Job();
        job.setUuid("delete-job-missing");
        JobResponse response = new JobResponse();
        response.setJob(job);
        when(snapshotFeignClient.deleteSnapshot(anyString(), eq("fv-uuid-1"), eq("snap-uuid-1")))
                .thenReturn(response);

        Job failedJob = new Job();
        failedJob.setUuid("delete-job-missing");
        failedJob.setState(OntapStorageConstants.JOB_FAILURE);
        failedJob.setMessage("entry doesn't exist");
        when(jobFeignClient.getJobByUUID(anyString(), eq("delete-job-missing"))).thenReturn(failedJob);

        storageStrategy.deleteFlexVolSnapshotForCloudStackVolume("fv-uuid-1", "snap-uuid-1", "snap-name-1");

        verify(snapshotFeignClient).deleteSnapshot(anyString(), eq("fv-uuid-1"), eq("snap-uuid-1"));
    }

        @Test
        void testDeleteFlexVolSnapshotForCloudStackVolume_Feign404_TreatedAsSuccess() {
                FeignException notFoundException = mock(FeignException.class);
                when(notFoundException.status()).thenReturn(404);
                when(snapshotFeignClient.deleteSnapshot(anyString(), eq("fv-uuid-1"), eq("snap-uuid-1")))
                                .thenThrow(notFoundException);

                storageStrategy.deleteFlexVolSnapshotForCloudStackVolume("fv-uuid-1", "snap-uuid-1", "snap-name-1");

                verify(snapshotFeignClient).deleteSnapshot(anyString(), eq("fv-uuid-1"), eq("snap-uuid-1"));
                verify(jobFeignClient, never()).getJobByUUID(anyString(), anyString());
        }

    // ========== updateStorageVolume() Tests ==========

    @Test
    public void testUpdateStorageVolume_positive() {
        // Setup
        Volume volume = new Volume();
        volume.setUuid("vol-uuid-resize");
        volume.setName("flexvol-resize");
        volume.setSize(5368709120L); // 5 GB

        Job job = new Job();
        job.setUuid("resize-job-uuid");
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJob(job);

        when(volumeFeignClient.updateVolume(anyString(), eq("vol-uuid-resize"), any()))
                .thenReturn(jobResponse);

        Job completedJob = new Job();
        completedJob.setUuid("resize-job-uuid");
        completedJob.setState(OntapStorageConstants.JOB_SUCCESS);
        when(jobFeignClient.getJobByUUID(anyString(), eq("resize-job-uuid")))
                .thenReturn(completedJob);

        // Execute
        Volume result = storageStrategy.updateStorageVolume(volume);

        // Verify
        assertNotNull(result);
        assertEquals(5368709120L, result.getSize());
        verify(volumeFeignClient, times(1)).updateVolume(anyString(), eq("vol-uuid-resize"), any());
        verify(jobFeignClient, atLeastOnce()).getJobByUUID(anyString(), eq("resize-job-uuid"));
    }

    @Test
    public void testUpdateStorageVolume_jobFailed() {
        // Setup
        Volume volume = new Volume();
        volume.setUuid("vol-uuid-resize");
        volume.setName("flexvol-resize");
        volume.setSize(5368709120L);

        Job job = new Job();
        job.setUuid("resize-job-uuid");
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJob(job);

        when(volumeFeignClient.updateVolume(anyString(), eq("vol-uuid-resize"), any()))
                .thenReturn(jobResponse);

        Job failedJob = new Job();
        failedJob.setUuid("resize-job-uuid");
        failedJob.setState(OntapStorageConstants.JOB_FAILURE);
        failedJob.setMessage("Resize failed");
        when(jobFeignClient.getJobByUUID(anyString(), eq("resize-job-uuid")))
                .thenReturn(failedJob);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.updateStorageVolume(volume));
        assertTrue(ex.getMessage().contains("Job failed"));
    }

    @Test
    public void testUpdateStorageVolume_feignException() {
        // Setup
        Volume volume = new Volume();
        volume.setUuid("vol-uuid-fail");
        volume.setName("flexvol-fail");
        volume.setSize(3221225472L);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(volumeFeignClient.updateVolume(anyString(), eq("vol-uuid-fail"), any()))
                .thenThrow(feignException);

        // Execute & Verify
        Exception ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.updateStorageVolume(volume));
        assertTrue(ex.getMessage().contains("Failed to resize ONTAP FlexVolume"));
    }

    @Test
    public void testUpdateStorageVolume_notFound_404_throwsCloudRuntimeException() {
        // Setup
        Volume volume = new Volume();
        volume.setUuid("vol-uuid-notfound");
        volume.setName("flexvol-notfound");
        volume.setSize(1073741824L);

        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(404);
        when(volumeFeignClient.updateVolume(anyString(), eq("vol-uuid-notfound"), any()))
                .thenThrow(feignEx);

        // Execute & Verify — 404 means volume not found on ONTAP, should throw
        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> storageStrategy.updateStorageVolume(volume));
        assertTrue(ex.getMessage().contains("not found on ONTAP"));
    }
}
