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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.veeam.api.dto.Nic;
import org.junit.Test;

import com.cloud.network.dao.NetworkVO;

public class NicVOToNicConverterTest {

    @Test
    public void testToNic_MapsVmHrefIpAndVnicProfile() {
        final com.cloud.vm.NicVO vo = mock(com.cloud.vm.NicVO.class);
        when(vo.getUuid()).thenReturn("nic-1");
        when(vo.getReserver()).thenReturn("eth0");
        when(vo.getMacAddress()).thenReturn("02:00:00:00:00:01");
        when(vo.getIPv4Address()).thenReturn("10.1.1.10");
        when(vo.getIPv4Gateway()).thenReturn("10.1.1.1");
        when(vo.getNetworkId()).thenReturn(10L);

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getUuid()).thenReturn("net-1");

        final Nic nic = NicVOToNicConverter.toNic(vo, "vm-1", id -> network);

        assertEquals("nic-1", nic.getId());
        assertEquals("virtio", nic.getInterfaceType());
        assertEquals("vm-1", nic.getVm().getId());
        assertTrue(nic.getHref().contains("/api/vms/vm-1/nics/nic-1"));
        assertEquals("net-1", nic.getVnicProfile().getId());
        assertNotNull(nic.getReportedDevices());
        assertEquals("v4", nic.getReportedDevices().getItems().get(0).getIps().getItems().get(0).getVersion());
    }

    @Test(expected = NullPointerException.class)
    public void testToNic_BlankVmUuidCurrentBehaviorThrowsNpe() {
        final com.cloud.vm.NicVO vo = mock(com.cloud.vm.NicVO.class);
        when(vo.getUuid()).thenReturn("nic-2");
        when(vo.getReserver()).thenReturn("eth1");
        when(vo.getMacAddress()).thenReturn("02:00:00:00:00:02");

        NicVOToNicConverter.toNic(vo, "", null);
    }
}
