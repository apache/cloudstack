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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.storage.Storage.StoragePoolType;

public class AttachVolumeAnswerTest {
    AttachVolumeCommand avc = new AttachVolumeCommand(true, false, "vmname", StoragePoolType.Filesystem, "vPath", "vName", 1073741824L, 123456789L, "chainInfo");
    AttachVolumeAnswer ava1 = new AttachVolumeAnswer(avc);
    String results = "";
    AttachVolumeAnswer ava2 = new AttachVolumeAnswer(avc, results);
    Long deviceId = 10L;
    AttachVolumeAnswer ava3 = new AttachVolumeAnswer(avc, deviceId);

    @Test
    public void testGetDeviceId() {
        Long dId = ava1.getDeviceId();
        assertTrue(dId == null);

        dId = ava2.getDeviceId();
        assertTrue(dId == null);

        dId = ava3.getDeviceId();
        Long expected = 10L;
        assertEquals(expected, dId);
    }

    @Test
    public void testGetChainInfo() {
        ava1.setChainInfo("chainInfo");
        String chainInfo = ava1.getChainInfo();
        assertTrue(chainInfo.equals("chainInfo"));

        ava2.setChainInfo("chainInfo");
        chainInfo = ava2.getChainInfo();
        assertTrue(chainInfo.equals("chainInfo"));

        ava3.setChainInfo("chainInfo");
        chainInfo = ava3.getChainInfo();
        assertTrue(chainInfo.equals("chainInfo"));
    }
}
