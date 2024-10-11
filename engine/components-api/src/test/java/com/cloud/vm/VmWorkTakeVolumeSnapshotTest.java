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
package com.cloud.vm;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class VmWorkTakeVolumeSnapshotTest {

    @Test
    public void testVmWorkTakeVolumeSnapshotZoneIds() {
        List<Long> zoneIds = List.of(10L, 20L);
        VmWorkTakeVolumeSnapshot work = new VmWorkTakeVolumeSnapshot(1L, 1L, 1L, "handler",
                1L, 1L, 1L, false, null, false, zoneIds);
        Assert.assertNotNull(work.getZoneIds());
        Assert.assertEquals(zoneIds.size(), work.getZoneIds().size());
        Assert.assertEquals(zoneIds.get(0), work.getZoneIds().get(0));
        Assert.assertEquals(zoneIds.get(1), work.getZoneIds().get(1));
    }
}
