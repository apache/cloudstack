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

import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.storage.Storage.StoragePoolType;

public class AttachVolumeCommandTest {
    AttachVolumeCommand avc = new AttachVolumeCommand(true, false, "vmname", StoragePoolType.Filesystem, "vPath", "vName", 1073741824L, 123456789L, "chainInfo");

    @Test
    public void testExecuteInSequence() {
        boolean b = avc.executeInSequence();
        assertTrue(b);
    }

    @Test
    public void testGetAttach() {
        boolean b = avc.getAttach();
        assertTrue(b);
    }

    @Test
    public void testGetVmName() {
        String vmName = avc.getVmName();
        assertTrue(vmName.equals("vmname"));
    }

    @Test
    public void testGetPooltype() {
        StoragePoolType pt = avc.getPooltype();
        assertTrue(pt.equals(StoragePoolType.Filesystem));

        avc.setPooltype(StoragePoolType.NetworkFilesystem);
        pt = avc.getPooltype();
        assertTrue(pt.equals(StoragePoolType.NetworkFilesystem));

        avc.setPooltype(StoragePoolType.IscsiLUN);
        pt = avc.getPooltype();
        assertTrue(pt.equals(StoragePoolType.IscsiLUN));

        avc.setPooltype(StoragePoolType.Iscsi);
        pt = avc.getPooltype();
        assertTrue(pt.equals(StoragePoolType.Iscsi));
    }

    @Test
    public void testGetVolumePath() {
        String vPath = avc.getVolumePath();
        assertTrue(vPath.equals("vPath"));
    }

    @Test
    public void testGetVolumeName() {
        String vName = avc.getVolumeName();
        assertTrue(vName.equals("vName"));
    }

    @Test
    public void testGetDeviceId() {
        Long dId = avc.getDeviceId();
        Long expected = 123456789L;
        assertEquals(expected, dId);

        avc.setDeviceId(5L);
        dId = avc.getDeviceId();
        expected = 5L;
        assertEquals(expected, dId);

        avc.setDeviceId(0L);
        dId = avc.getDeviceId();
        expected = 0L;
        assertEquals(expected, dId);

        avc.setDeviceId(-5L);
        dId = avc.getDeviceId();
        expected = -5L;
        assertEquals(expected, dId);
    }

    @Test
    public void testGetPoolUuid() {
        avc.setPoolUuid("420fa39c-4ef1-a83c-fd93-46dc1ff515ae");
        String pUuid = avc.getPoolUuid();
        assertTrue(pUuid.equals("420fa39c-4ef1-a83c-fd93-46dc1ff515ae"));
    }

    @Test
    public void testGetWait() {
        String cInfo = avc.getChainInfo();
        assertTrue(cInfo.equals("chainInfo"));
    }
}
