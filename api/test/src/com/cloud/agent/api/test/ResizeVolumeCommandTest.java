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
package src.com.cloud.agent.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;


public class ResizeVolumeCommandTest {

    public StoragePool dummypool = new StoragePool() {
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

    Long newSize = 4194304L;
    Long currentSize = 1048576L;

    ResizeVolumeCommand rv = new ResizeVolumeCommand("dummydiskpath", 
            new StorageFilerTO(dummypool), currentSize, newSize, false, 
            "vmName");

    @Test
    public void testExecuteInSequence() {
        boolean b = rv.executeInSequence();
        assertFalse(b);
    }

    @Test
    public void testGetPath() {
        String path = rv.getPath();
        assertTrue(path.equals("dummydiskpath"));
    }

    @Test
    public void testGetPoolUuid() {
        String poolUuid = rv.getPoolUuid();
        assertTrue(poolUuid.equals("bed9f83e-cac3-11e1-ac8a-0050568b007e"));
    }

    @Test
    public void testGetPool() {
        StorageFilerTO pool = rv.getPool();

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
    public void testGetNewSize() {
        long newSize = rv.getNewSize();
        assertTrue(newSize == 4194304L);
    }

    @Test
    public void testGetCurrentSize() {
        long currentSize = rv.getCurrentSize();
        assertTrue(currentSize == 1048576L);
    }

    @Test
    public void testGetShrinkOk() {
        assertFalse(rv.getShrinkOk());
    }

    @Test
    public void testGetInstanceName() {
        String vmName = rv.getInstanceName();
        assertTrue(vmName.equals("vmName"));
    }

}
