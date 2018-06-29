//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class BackupVOTest {

    private BackupVO vo = new BackupVO();

    private final List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);

    @Test
    public void testVolumeIdsNotEmptyList() {
        vo.setVolumeIds(ids);
        Assert.assertEquals("[1,2,3,4]", vo.getVolumes());
        Assert.assertEquals(ids, vo.getVolumeIds());
    }

    @Test
    public void testVolumeIdsEmptyList() {
        vo.setVolumeIds(new ArrayList<>());
        Assert.assertEquals(null, vo.getVolumes());
        Assert.assertEquals(new ArrayList<>(), vo.getVolumeIds());
    }

    @Test
    public void testVolumeIdsUnsetVolumeIds() {
        Assert.assertEquals(null, vo.getVolumes());
        Assert.assertEquals(new ArrayList<>(), vo.getVolumeIds());
    }

    @Test
    public void testDecodeVolumesStringNotEmptyList() {
        vo.setVolumes("[1,2,3,4]");
        Assert.assertEquals(ids, vo.getVolumeIds());
    }

    @Test
    public void testDecodeVolumesStringEmptyList() {
        vo.setVolumes(null);
        Assert.assertEquals(new ArrayList<>(), vo.getVolumeIds());
    }

    @Test
    public void testSetVolumeIdsMultipleTimes() {
        List<Long> list = Arrays.asList(1L, 2L);
        vo.setVolumeIds(list);
        Assert.assertEquals("[1,2]", vo.getVolumes());
        vo.setVolumeIds(new ArrayList<>());
        Assert.assertEquals(null, vo.getVolumes());
        vo.setVolumeIds(ids);
        Assert.assertEquals("[1,2,3,4]", vo.getVolumes());
    }
}
