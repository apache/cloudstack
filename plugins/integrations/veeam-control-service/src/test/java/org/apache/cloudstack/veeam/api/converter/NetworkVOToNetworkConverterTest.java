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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.Network;
import org.junit.Test;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkVO;

public class NetworkVOToNetworkConverterTest {

    @Test
    public void testToNetwork_MapsFieldsAndDataCenterRef() {
        final NetworkVO vo = mock(NetworkVO.class);
        when(vo.getUuid()).thenReturn("net-1");
        when(vo.getName()).thenReturn("guest-net");
        when(vo.getDisplayText()).thenReturn("Guest network");
        when(vo.getPrivateMtu()).thenReturn(1450);
        when(vo.getDataCenterId()).thenReturn(10L);

        final DataCenterJoinVO dc = mock(DataCenterJoinVO.class);
        when(dc.getUuid()).thenReturn("dc-1");

        final Network dto = NetworkVOToNetworkConverter.toNetwork(vo, id -> dc);

        assertEquals("net-1", dto.getId());
        assertEquals("guest-net", dto.getName());
        assertEquals("Guest network", dto.getDescription());
        assertEquals("", dto.getComment());
        assertEquals("1450", dto.getMtu());
        assertEquals("false", dto.getPortIsolation());
        assertEquals("false", dto.getStp());
        assertEquals("guest-net", dto.getVdsmName());
        assertNotNull(dto.getUsages());
        assertEquals(1, dto.getUsages().getItems().size());
        assertEquals("vm", dto.getUsages().getItems().get(0));
        assertNotNull(dto.getLink());
        assertTrue(dto.getLink().isEmpty());
        assertEquals("dc-1", dto.getDataCenter().getId());
        assertTrue(dto.getHref().contains("/api/networks/net-1"));
    }

    @Test
    public void testToNetwork_UsesFallbackNameAndSkipsBlankDataCenterUuid() {
        final NetworkVO vo = mock(NetworkVO.class);
        when(vo.getUuid()).thenReturn("net-2");
        when(vo.getName()).thenReturn(null);
        when(vo.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(vo.getDisplayText()).thenReturn("Fallback network");
        when(vo.getPrivateMtu()).thenReturn(null);
        when(vo.getDataCenterId()).thenReturn(20L);

        final DataCenterJoinVO dc = mock(DataCenterJoinVO.class);
        when(dc.getUuid()).thenReturn("");

        final Network dto = NetworkVOToNetworkConverter.toNetwork(vo, id -> dc);

        assertEquals("Guest-net-2", dto.getName());
        assertEquals("0", dto.getMtu());
        assertEquals("Guest-net-2", dto.getVdsmName());
        assertNull(dto.getDataCenter());
    }

    @Test
    public void testToNetworkList_ConvertsAllItemsInOrder() {
        final NetworkVO first = mock(NetworkVO.class);
        when(first.getUuid()).thenReturn("net-1");
        when(first.getName()).thenReturn("first-net");
        when(first.getDisplayText()).thenReturn("First network");
        when(first.getPrivateMtu()).thenReturn(1400);

        final NetworkVO second = mock(NetworkVO.class);
        when(second.getUuid()).thenReturn("net-2");
        when(second.getName()).thenReturn(null);
        when(second.getTrafficType()).thenReturn(Networks.TrafficType.Control);
        when(second.getDisplayText()).thenReturn("Second network");
        when(second.getPrivateMtu()).thenReturn(null);

        final List<Network> result = NetworkVOToNetworkConverter.toNetworkList(List.of(first, second), null);

        assertEquals(2, result.size());
        assertEquals("net-1", result.get(0).getId());
        assertEquals("first-net", result.get(0).getName());
        assertEquals("net-2", result.get(1).getId());
        assertEquals("Control-net-2", result.get(1).getName());
    }
}
