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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.junit.Test;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.org.Grouping;

public class DataCenterJoinVOToDataCenterConverterTest {

    @Test
    public void testToDataCenter_MapsIdentityStatusAndLinks() {
        final DataCenterJoinVO zone = mock(DataCenterJoinVO.class);
        when(zone.getUuid()).thenReturn("dc-1");
        when(zone.getName()).thenReturn("zone-a");
        when(zone.getDescription()).thenReturn("desc-a");
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        final DataCenter dc = DataCenterJoinVOToDataCenterConverter.toDataCenter(zone);

        assertEquals("dc-1", dc.getId());
        assertEquals("zone-a", dc.getName());
        assertEquals("desc-a", dc.getDescription());
        assertEquals("up", dc.getStatus());
        assertNotNull(dc.getVersion());
        assertNotNull(dc.getSupportedVersions());
        assertEquals(3, dc.getLink().size());
        assertEquals("cluster", dc.getLink().get(0).getRel());
    }

    @Test
    public void testToDataCenter_DisabledZoneMapsToDown() {
        final DataCenterJoinVO zone = mock(DataCenterJoinVO.class);
        when(zone.getUuid()).thenReturn("dc-2");
        when(zone.getName()).thenReturn("zone-b");
        when(zone.getDescription()).thenReturn("desc-b");
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);

        final DataCenter dc = DataCenterJoinVOToDataCenterConverter.toDataCenter(zone);

        assertEquals("down", dc.getStatus());
    }

    @Test
    public void testToDcList_ConvertsAllItems() {
        final DataCenterJoinVO first = mock(DataCenterJoinVO.class);
        when(first.getUuid()).thenReturn("dc-1");
        when(first.getName()).thenReturn("zone-a");
        when(first.getDescription()).thenReturn("desc-a");
        when(first.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        final DataCenterJoinVO second = mock(DataCenterJoinVO.class);
        when(second.getUuid()).thenReturn("dc-2");
        when(second.getName()).thenReturn("zone-b");
        when(second.getDescription()).thenReturn("desc-b");
        when(second.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);

        final List<DataCenter> result = DataCenterJoinVOToDataCenterConverter.toDCList(List.of(first, second));

        assertEquals(2, result.size());
        assertEquals("dc-1", result.get(0).getId());
        assertEquals("dc-2", result.get(1).getId());
    }
}
