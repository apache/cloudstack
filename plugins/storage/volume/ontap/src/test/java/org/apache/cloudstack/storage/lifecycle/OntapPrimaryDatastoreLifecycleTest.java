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
package org.apache.cloudstack.storage.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.apache.cloudstack.storage.feign.model.Volume;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.dc.ClusterVO;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OntapPrimaryDatastoreLifecycleTest {
    @InjectMocks
    private OntapPrimaryDatastoreLifecycle ontapPrimaryDatastoreLifecycle;

    @Mock
    private ClusterDao _clusterDao;

    @Mock
    private StorageStrategy storageStrategy;

    @Mock
    private PrimaryDataStoreHelper _dataStoreHelper;

    @BeforeEach
    void setUp() {

        ClusterVO clusterVO = new ClusterVO(1L, 1L, "clusterName");
        clusterVO.setHypervisorType("KVM");
        when(_clusterDao.findById(1L)).thenReturn(clusterVO);

        when(storageStrategy.connect()).thenReturn(true);
        when(storageStrategy.getNetworkInterface()).thenReturn("testNetworkInterface");

        Volume volume = new Volume();
        volume.setUuid("test-volume-uuid");
        volume.setName("testVolume");
        when(storageStrategy.createStorageVolume(any(), any())).thenReturn(volume);

    }

    @Test
    public void testInitialize_positive() {

        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("username", "testUser");
        dsInfos.put("password", "testPassword");
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        try(MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
            storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
            ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
        }
    }

    @Test
    public void testInitialize_positiveWithIsDisaggregated() {

        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("username", "testUser");
        dsInfos.put("password", "testPassword");
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1;isDisaggregated=false");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        try(MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
            storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
            ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
        }
    }

    @Test
    public void testInitialize_null_Arg() {
        Exception ex = assertThrows(CloudRuntimeException.class,() ->
                ontapPrimaryDatastoreLifecycle.initialize(null));
        assertTrue(ex.getMessage().contains("Datastore info map is null, cannot create primary storage"));
    }

    @Test
    public void testInitialize_missingRequiredDetailKey() {
        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
            storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
            Exception ex = assertThrows(CloudRuntimeException.class, () -> ontapPrimaryDatastoreLifecycle.initialize(dsInfos));
            assertTrue(ex.getMessage().contains("missing detail"));
        }
    }

    @Test
    public void testInitialize_invalidCapacityBytes() {
        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",-1L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
            storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
            ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
        }
    }

    @Test
    public void testInitialize_unmanagedStorage() {
        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",false);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        Exception ex = assertThrows(CloudRuntimeException.class, () -> {
            try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
                storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
                ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
            }
        });
        assertTrue(ex.getMessage().contains("must be managed"));
    }

    @Test
    public void testInitialize_nullStoragePoolName() {
        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", null);
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        Exception ex = assertThrows(CloudRuntimeException.class, () -> {
            try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
                storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
                ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
            }
        });
        assertTrue(ex.getMessage().contains("Storage pool name is null or empty"));
    }

    @Test
    public void testInitialize_nullProviderName() {
        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", null);
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        Exception ex = assertThrows(CloudRuntimeException.class, () -> {
            try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
                storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
                ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
            }
        });
        assertTrue(ex.getMessage().contains("Provider name is null or empty"));
    }

    @Test
    public void testInitialize_nullPodAndClusterAndZone() {
        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1");
        dsInfos.put("zoneId",null);
        dsInfos.put("podId",null);
        dsInfos.put("clusterId", null);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        Exception ex = assertThrows(CloudRuntimeException.class, () -> {
            try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
                storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
                ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
            }
        });
        assertTrue(ex.getMessage().contains("Pod Id, Cluster Id and Zone Id are all null"));
    }

    @Test
    public void testInitialize_clusterNotKVM() {
        ClusterVO clusterVO = new ClusterVO(2L, 1L, "clusterName");
        clusterVO.setHypervisorType("XenServer");
        when(_clusterDao.findById(2L)).thenReturn(clusterVO);

        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 2L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        Exception ex = assertThrows(CloudRuntimeException.class, () -> {
            try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
                storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
                ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
            }
        });
        assertTrue(ex.getMessage().contains("ONTAP primary storage is supported only for KVM hypervisor"));
    }

    @Test
    public void testInitialize_unexpectedDetailKey() {
        Map<String, Object> dsInfos = new HashMap<>();
        dsInfos.put("url", "username=testUser;password=testPassword;svmName=testSVM;protocol=NFS3;managementLIF=192.168.1.1;unexpectedKey=unexpectedValue");
        dsInfos.put("zoneId",1L);
        dsInfos.put("podId",1L);
        dsInfos.put("clusterId", 1L);
        dsInfos.put("name", "testStoragePool");
        dsInfos.put("providerName", "testProvider");
        dsInfos.put("capacityBytes",200000L);
        dsInfos.put("managed",true);
        dsInfos.put("tags", "testTag");
        dsInfos.put("isTagARule", false);
        dsInfos.put("details", new HashMap<String, String>());

        Exception ex = assertThrows(CloudRuntimeException.class, () -> {
            try (MockedStatic<StorageProviderFactory> storageProviderFactory = Mockito.mockStatic(StorageProviderFactory.class)) {
                storageProviderFactory.when(() -> StorageProviderFactory.getStrategy(any())).thenReturn(storageStrategy);
                ontapPrimaryDatastoreLifecycle.initialize(dsInfos);
            }
        });
        assertTrue(ex.getMessage().contains("Unexpected ONTAP detail key in URL"));
    }

}
