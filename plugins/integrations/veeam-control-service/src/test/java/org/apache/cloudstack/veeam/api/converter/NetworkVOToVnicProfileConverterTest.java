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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.VnicProfile;
import org.junit.Test;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkVO;

public class NetworkVOToVnicProfileConverterTest {

    @Test
    public void testToVnicProfile_MapsNetworkAndDataCenterRefs() {
        final NetworkVO vo = mock(NetworkVO.class);
        when(vo.getUuid()).thenReturn("net-3");
        when(vo.getName()).thenReturn("profile-net");
        when(vo.getDisplayText()).thenReturn("Profile network");
        when(vo.getDataCenterId()).thenReturn(30L);

        final DataCenterJoinVO dc = mock(DataCenterJoinVO.class);
        when(dc.getUuid()).thenReturn("dc-3");

        final VnicProfile profile = NetworkVOToVnicProfileConverter.toVnicProfile(vo, id -> dc);

        assertEquals("net-3", profile.getId());
        assertEquals("profile-net", profile.getName());
        assertEquals("Profile network", profile.getDescription());
        assertEquals("net-3", profile.getNetwork().getId());
        assertEquals("dc-3", profile.getDataCenter().getId());
        assertTrue(profile.getHref().contains("/api/vnicprofiles/net-3"));
        assertTrue(profile.getNetwork().getHref().contains("/api/networks/net-3"));
    }

    @Test
    public void testToVnicProfile_UsesFallbackNameAndOmitsBlankDataCenterUuid() {
        final NetworkVO vo = mock(NetworkVO.class);
        when(vo.getUuid()).thenReturn("net-4");
        when(vo.getName()).thenReturn(null);
        when(vo.getTrafficType()).thenReturn(Networks.TrafficType.Management);
        when(vo.getDisplayText()).thenReturn("Mgmt network");
        when(vo.getDataCenterId()).thenReturn(40L);

        final DataCenterJoinVO dc = mock(DataCenterJoinVO.class);
        when(dc.getUuid()).thenReturn("");

        final VnicProfile profile = NetworkVOToVnicProfileConverter.toVnicProfile(vo, id -> dc);

        assertEquals("Management-net-4", profile.getName());
        assertEquals("Mgmt network", profile.getDescription());
        assertEquals("net-4", profile.getNetwork().getId());
        assertNull(profile.getDataCenter());
    }

    @Test
    public void testToVnicProfileList_ConvertsAllItemsInOrder() {
        final NetworkVO first = mock(NetworkVO.class);
        when(first.getUuid()).thenReturn("net-1");
        when(first.getName()).thenReturn("profile-a");
        when(first.getDisplayText()).thenReturn("Profile A");

        final NetworkVO second = mock(NetworkVO.class);
        when(second.getUuid()).thenReturn("net-2");
        when(second.getName()).thenReturn(null);
        when(second.getTrafficType()).thenReturn(Networks.TrafficType.Control);
        when(second.getDisplayText()).thenReturn("Profile B");

        final List<VnicProfile> result = NetworkVOToVnicProfileConverter.toVnicProfileList(List.of(first, second), null);

        assertEquals(2, result.size());
        assertEquals("net-1", result.get(0).getId());
        assertEquals("profile-a", result.get(0).getName());
        assertEquals("net-2", result.get(1).getId());
        assertEquals("Control-net-2", result.get(1).getName());
    }
}
