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
package org.apache.cloudstack.api.agent.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;

public class BackupSnapshotCommandTest {
    public StoragePool pool = new StoragePool() {
        @Override
        public long getId() {
            return 1L;
        };

        @Override
        public String getName() {
            return "name";
        };

        @Override
        public String getUuid() {
            return "bed9f83e-cac3-11e1-ac8a-0050568b007e";
        };

        @Override
        public StoragePoolType getPoolType() {
            return StoragePoolType.Filesystem;
        };

        @Override
        public Date getCreated() {
            Date date = null;
            try {
                date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
                .parse("01/01/1970 12:12:12");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return date;
        }

        @Override
        public Date getUpdateTime() {
            return new Date();
        };

        @Override
        public long getDataCenterId() {
            return 0L;
        };

        @Override
        public long getCapacityBytes() {
            return 0L;
        };

        @Override
        public long getAvailableBytes() {
            return 0L;
        };

        @Override
        public Long getClusterId() {
            return 0L;
        };

        @Override
        public String getHostAddress() {
            return "hostAddress";
        };

        @Override
        public String getPath() {
            return "path";
        };

        @Override
        public String getUserInfo() {
            return "userInfo";
        };

        @Override
        public boolean isShared() {
            return false;
        };

        @Override
        public boolean isLocal() {
            return false;
        };

        @Override
        public StoragePoolStatus getStatus() {
            return StoragePoolStatus.Up;
        };

        @Override
        public int getPort() {
            return 25;
        };

        @Override
        public Long getPodId() {
            return 0L;
        }

        @Override
        public Long getStorageProviderId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isInMaintenance() {
            // TODO Auto-generated method stub
            return false;
        };
    };

    BackupSnapshotCommand bsc = new BackupSnapshotCommand(
            "http://secondary.Storage.Url",
            101L, 102L, 103L, 104L, 105L, "vPath", pool,
            "420fa39c-4ef1-a83c-fd93-46dc1ff515ae", "sName",
            "9012793e-0657-11e2-bebc-0050568b0057",
            "7167e0b2-f5b0-11e1-8414-0050568b0057", false, "vmName", 5);

    BackupSnapshotCommand bsc1 = new BackupSnapshotCommand(
            "http://secondary.Storage.Url",
            101L, 102L, 103L, 104L, 105L,"vPath", pool,
            "420fa39c-4ef1-a83c-fd93-46dc1ff515ae", "sName",
            "9012793e-0657-11e2-bebc-0050568b0057",
            "7167e0b2-f5b0-11e1-8414-0050568b0057", false, "vmName", 5);

    @Test
    public void testGetPrimaryStoragePoolNameLabel() {
        String label = bsc.getPrimaryStoragePoolNameLabel();
        assertTrue(label.equals("bed9f83e-cac3-11e1-ac8a-0050568b007e"));
    }

    @Test
    public void testGetSecondaryStorageUrl() {
        String url = bsc.getSecondaryStorageUrl();
        assertTrue(url.equals("http://secondary.Storage.Url"));
    }

    @Test
    public void testGetDataCenterId() {
        Long dcId = bsc.getDataCenterId();
        Long expected = 101L;
        assertEquals(expected, dcId);
    }

    @Test
    public void testGetAccountId() {
        Long aId = bsc.getAccountId();
        Long expected = 102L;
        assertEquals(expected, aId);
    }

    @Test
    public void testGetVolumeId() {
        Long vId = bsc.getVolumeId();
        Long expected = 103L;
        assertEquals(expected, vId);
    }

    @Test
    public void testGetSnapshotId() {
        Long ssId = bsc.getSnapshotId();
        Long expected = 104L;
        assertEquals(expected, ssId);
    }

    @Test
    public void testGetPool() {
        StorageFilerTO pool = bsc.getPool();

        Long id = pool.getId();
        Long expectedL = 1L;
        assertEquals(expectedL, id);

        String uuid = pool.getUuid();
        assertTrue(uuid.equals("bed9f83e-cac3-11e1-ac8a-0050568b007e"));

        String host = pool.getHost();
        assertTrue(host.equals("hostAddress"));

        String path = pool.getPath();
        assertTrue(path.equals("path"));

        String userInfo = pool.getUserInfo();
        assertTrue(userInfo.equals("userInfo"));

        Integer port = pool.getPort();
        Integer expectedI = 25;
        assertEquals(expectedI, port);

        StoragePoolType type = pool.getType();
        assertEquals(StoragePoolType.Filesystem, type);

        String str = pool.toString();
        assertTrue(str.equals("Pool[" + id.toString() + "|" + host + ":"
                + port.toString() + "|" + path + "]"));
    }

    @Test
    public void testGetCreated() {
        try {
            Date date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
            .parse("01/01/1970 12:12:12");
            Date d = pool.getCreated();
            assertTrue(d.compareTo(date) == 0);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetSwift() {
        SwiftTO s1 = new SwiftTO();
        bsc.setSwift(s1);
        SwiftTO s2 = bsc.getSwift();
        assertEquals(s1, s2);
    }

    @Test
    public void testGetSnapshotName() {
        String ssName = bsc.getSnapshotName();
        assertTrue(ssName.equals("sName"));
    }

    @Test
    public void testGetSnapshotUuid() {
        String uuid = bsc.getSnapshotUuid();
        assertTrue(uuid.equals("420fa39c-4ef1-a83c-fd93-46dc1ff515ae"));
    }

    @Test
    public void testGetPrevSnapshotUuid() {
        String uuid = bsc.getPrevSnapshotUuid();
        assertTrue(uuid.equals("9012793e-0657-11e2-bebc-0050568b0057"));
    }

    @Test
    public void testGetPrevBackupUuid() {
        String uuid = bsc.getPrevBackupUuid();
        assertTrue(uuid.equals("7167e0b2-f5b0-11e1-8414-0050568b0057"));
    }

    @Test
    public void testGetVolumePath() {
        String path = bsc.getVolumePath();
        assertTrue(path.equals("vPath"));

        bsc.setVolumePath("vPath1");
        path = bsc.getVolumePath();
        assertTrue(path.equals("vPath1"));

        bsc1.setVolumePath("vPath2");
        path = bsc1.getVolumePath();
        assertTrue(path.equals("vPath2"));
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = bsc.executeInSequence();
        assertFalse(b);

        b = bsc1.executeInSequence();
        assertFalse(b);
    }

}
