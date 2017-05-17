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

package org.apache.cloudstack.storage.datastore.driver;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.inject.Inject;

import static org.mockito.Mockito.when;

public class DateraPrimaryDataStoreDriverTest {

    @Inject
    DateraPrimaryDataStoreDriver dateraDriver = new DateraPrimaryDataStoreDriver();

    @Mock private ClusterDao _clusterDao;
    @Mock private ClusterDetailsDao _clusterDetailsDao;
    @Mock private HostDao _hostDao;
    @Mock private SnapshotDao _snapshotDao;
    @Mock private SnapshotDetailsDao _snapshotDetailsDao;
    @Mock private PrimaryDataStoreDao _storagePoolDao;
    @Mock private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Mock private VolumeDao _volumeDao;
    @Mock private VMTemplatePoolDao tmpltPoolDao;
    @Mock private PrimaryDataStoreDao storagePoolDao;
    @Mock private VolumeDetailsDao volumeDetailsDao;
    @Mock private SnapshotDetailsDao snapshotDetailsDao;
    @Mock private VolumeDataFactory volumeDataFactory;

    // Mock variables
    @Mock private Host mockHost;


    //Constants
    private static final String STORAGE_URL = "TODO";

    @Before
    public void setUp() throws Exception {

        //setup Mock objects
        when(mockHost.getStorageUrl()).thenReturn(STORAGE_URL);

    }

    @Test
    public void testGrantAccess() throws Exception {

    }

    @Test
    public void testRevokeAccess() throws Exception {

    }

    @Test
    public void testGetUsedBytes() throws Exception {

    }

    @Test
    public void testGetUsedIops() throws Exception {

    }

    @Test
    public void testGetVolumeSizeIncludingHypervisorSnapshotReserve() throws Exception {

    }

    @Test
    public void testCreateAsync() throws Exception {

    }

    @Test
    public void testDeleteAsync() throws Exception {

    }

    @Test
    public void testCopyAsync() throws Exception {

    }

    @Test
    public void testCanCopy() throws Exception {

    }

    @Test
    public void testTakeSnapshot() throws Exception {

    }

    @Test
    public void testRevertSnapshot() throws Exception {

    }

    @Test
    public void testResize() throws Exception {

    }
}