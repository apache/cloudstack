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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.Tag;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.junit.Test;

import org.apache.cloudstack.api.ApiConstants;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.vm.VirtualMachine;

public class UserVmJoinVOToVmConverterTest {

    @Test
    public void testToVm_MapsUpStateWithBasicFields() {
        final UserVmJoinVO src = mock(UserVmJoinVO.class);
        when(src.getId()).thenReturn(101L);
        when(src.getUuid()).thenReturn("vm-1");
        when(src.getName()).thenReturn("vm-1-name");
        when(src.getDisplayName()).thenReturn("vm-1-display");
        when(src.getInstanceName()).thenReturn("i-101");
        when(src.getState()).thenReturn(VirtualMachine.State.Running);
        when(src.getCreated()).thenReturn(new Date(1000L));
        when(src.getLastUpdated()).thenReturn(new Date(2000L));
        when(src.getTemplateUuid()).thenReturn("tmpl-1");
        when(src.getHostUuid()).thenReturn("host-1");
        when(src.getRamSize()).thenReturn(512);
        when(src.getArch()).thenReturn("x86_64");
        when(src.getCpu()).thenReturn(2);
        when(src.getGuestOsDisplayName()).thenReturn("Linux");
        when(src.getServiceOfferingUuid()).thenReturn("offering-1");
        when(src.getAccountUuid()).thenReturn("acct-1");
        when(src.getAffinityGroupUuid()).thenReturn("ag-1");
        when(src.getUserDataUuid()).thenReturn("ud-1");

        final Vm vm = UserVmJoinVOToVmConverter.toVm(src, null, null, null, null, null, false);

        assertEquals("vm-1", vm.getId());
        assertEquals("vm-1-name", vm.getName());
        assertEquals("vm-1-display", vm.getDescription());
        assertEquals("up", vm.getStatus());
        assertEquals(Long.valueOf(1000L), vm.getCreationTime());
        assertEquals(Long.valueOf(2000L), vm.getStartTime());
        assertNull(vm.getStopTime());
        assertEquals("536870912", vm.getMemory());
        assertEquals("x86_64", vm.getCpu().getArchitecture());
        assertEquals("host-1", vm.getHost().getId());
        assertNotNull(vm.getActions());
        assertEquals(3, vm.getActions().getItems().size());
    }

    @Test
    public void testToVm_UsesResolversAndMapsDownState() {
        final UserVmJoinVO src = mock(UserVmJoinVO.class);
        when(src.getId()).thenReturn(202L);
        when(src.getUuid()).thenReturn("vm-2");
        when(src.getName()).thenReturn("vm-2-name");
        when(src.getDisplayName()).thenReturn("vm-2-display");
        when(src.getInstanceName()).thenReturn("i-202");
        when(src.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(src.getCreated()).thenReturn(new Date(3000L));
        when(src.getLastUpdated()).thenReturn(new Date(4000L));
        when(src.getTemplateUuid()).thenReturn("tmpl-2");
        when(src.getHostUuid()).thenReturn(null);
        when(src.getHostId()).thenReturn(22L);
        when(src.getLastHostId()).thenReturn(null);
        when(src.getRamSize()).thenReturn(1024);
        when(src.getArch()).thenReturn("x86_64");
        when(src.getCpu()).thenReturn(4);
        when(src.getGuestOsDisplayName()).thenReturn("Linux");
        when(src.getServiceOfferingUuid()).thenReturn("offering-2");
        when(src.getAccountUuid()).thenReturn("acct-2");
        when(src.getAffinityGroupUuid()).thenReturn("ag-2");
        when(src.getUserDataUuid()).thenReturn("ud-2");

        final HostJoinVO hostVo = mock(HostJoinVO.class);
        when(hostVo.getUuid()).thenReturn("host-2");
        when(hostVo.getClusterUuid()).thenReturn("cluster-2");

        final Tag tag = new Tag();
        tag.setId("tag-1");

        final DiskAttachment disk = new DiskAttachment();
        disk.setId("da-1");

        final Nic nic = new Nic();
        nic.setId("nic-1");

        final Vm vm = UserVmJoinVOToVmConverter.toVm(
                src,
                id -> hostVo,
                id -> Map.of(ApiConstants.BootType.UEFI.toString(), "true"),
                id -> List.of(tag),
                id -> List.of(disk),
                ignored -> List.of(nic),
                false);

        assertEquals("down", vm.getStatus());
        assertEquals(Long.valueOf(4000L), vm.getStopTime());
        assertEquals("host-2", vm.getHost().getId());
        assertEquals("cluster-2", vm.getCluster().getId());
        assertEquals(1, vm.getTags().getItems().size());
        assertEquals(1, vm.getDiskAttachments().getItems().size());
        assertEquals(1, vm.getNics().getItems().size());
        assertEquals("acct-2", vm.getAccountId());
        assertEquals("ag-2", vm.getAffinityGroupId());
        assertEquals("ud-2", vm.getUserDataId());
    }
}
