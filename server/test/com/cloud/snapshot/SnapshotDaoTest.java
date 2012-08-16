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
package com.cloud.snapshot;

import java.util.List;

import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.storage.snapshot.Snapshot;
import com.cloud.utils.component.ComponentLocator;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SnapshotDaoTest extends TestCase {
	
    public void testListBy() {
        SnapshotDaoImpl dao = ComponentLocator.inject(SnapshotDaoImpl.class);
        
        List<SnapshotVO> snapshots = dao.listByInstanceId(3, Snapshot.Status.BackedUp);
        for(SnapshotVO snapshot : snapshots) {
            Assert.assertTrue(snapshot.getStatus() == Snapshot.Status.BackedUp);
        }
    }
}
