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

package org.apache.cloudstack.dns.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.dns.DnsZone;
import org.apache.cloudstack.dns.vo.DnsZoneVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class DnsZoneDaoImplTest {

    DnsZoneDaoImpl dao;
    DnsZoneVO mockZone;

    @Before
    public void setUp() {
        dao = spy(new DnsZoneDaoImpl());
        mockZone = new DnsZoneVO("example.com", DnsZone.ZoneType.Public, 1L, 10L, 100L, "test zone");
    }

    @Test
    public void testListByAccount() {
        List<DnsZoneVO> expected = Collections.singletonList(mockZone);
        doReturn(expected).when(dao).listBy(any(SearchCriteria.class));

        List<DnsZoneVO> result = dao.listByAccount(10L);
        assertEquals(1, result.size());
        assertEquals("example.com", result.get(0).getName());
    }

    @Test
    public void testFindByNameServerAndType() {
        doReturn(mockZone).when(dao).findOneBy(any(SearchCriteria.class));

        DnsZoneVO result = dao.findByNameServerAndType("example.com", 1L, DnsZone.ZoneType.Public);
        assertNotNull(result);
        assertEquals("example.com", result.getName());
    }

    @Test
    public void testFindDnsZonesByServerId() {
        List<DnsZoneVO> expected = Collections.singletonList(mockZone);
        doReturn(expected).when(dao).listBy(any(SearchCriteria.class));

        List<DnsZoneVO> result = dao.findDnsZonesByServerId(1L);
        assertEquals(1, result.size());
    }

    @Test
    public void testSearchZonesWithAllParams() {
        List<DnsZoneVO> expected = Collections.singletonList(mockZone);
        Pair<List<DnsZoneVO>, Integer> expectedPair = new Pair<>(expected, 1);
        doReturn(expectedPair).when(dao).searchAndCount(any(SearchCriteria.class), any());

        Filter filter = new Filter(DnsZoneVO.class, "id", true, 0L, 10L);
        List<Long> ownDnsServerIds = Arrays.asList(1L, 2L);
        Pair<List<DnsZoneVO>, Integer> result = dao.searchZones(1L, 10L, ownDnsServerIds, 1L, "example", filter);

        assertNotNull(result);
        assertEquals(1, result.first().size());
        assertEquals(1, result.second().intValue());
    }

    @Test
    public void testSearchZonesWithNullParams() {
        List<DnsZoneVO> expected = Collections.singletonList(mockZone);
        Pair<List<DnsZoneVO>, Integer> expectedPair = new Pair<>(expected, 1);
        doReturn(expectedPair).when(dao).searchAndCount(any(SearchCriteria.class), any());

        Filter filter = new Filter(DnsZoneVO.class, "id", true, 0L, 10L);
        List<Long> ownDnsServerIds = new ArrayList<>();
        Pair<List<DnsZoneVO>, Integer> result = dao.searchZones(null, null, ownDnsServerIds, null, null, filter);

        assertNotNull(result);
        assertEquals(1, result.first().size());
        assertEquals(1, result.second().intValue());
    }
}
