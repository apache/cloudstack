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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class DnsServerDaoImplTest {

    DnsServerDaoImpl dao;
    DnsServerVO mockServer;

    @Before
    public void setUp() {
        dao = spy(new DnsServerDaoImpl());
        mockServer = new DnsServerVO("test-server", "http://pdns:8081", 8081, DnsProviderType.PowerDNS, null, "apikey", false, null, Collections.singletonList("ns1.example.com"), 1L, 10L);
    }

    @Test
    public void testFindByUrlAndAccount() {
        doReturn(mockServer).when(dao).findOneBy(any(SearchCriteria.class));

        DnsServer result = dao.findByUrlAndAccount("http://pdns:8081", 1L);
        assertNotNull(result);
        assertEquals("test-server", result.getName());
        assertEquals("http://pdns:8081", result.getUrl());
    }

    @Test
    public void testListDnsServerIdsByAccountId() {
        List<Long> expectedIds = Arrays.asList(100L);
        doReturn(expectedIds).when(dao).customSearch(any(SearchCriteria.class), any());

        List<Long> result = dao.listDnsServerIdsByAccountId(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).longValue());
    }

    @Test
    public void testListDnsServerIdsByAccountIdNullAccount() {
        List<Long> expectedIds = Arrays.asList(100L, 200L);
        doReturn(expectedIds).when(dao).customSearch(any(SearchCriteria.class), any());

        List<Long> result = dao.listDnsServerIdsByAccountId(null);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testSearchDnsServerWithAllParams() {
        List<DnsServerVO> expected = Collections.singletonList(mockServer);
        Pair<List<DnsServerVO>, Integer> expectedPair = new Pair<>(expected, 1);
        doReturn(expectedPair).when(dao).searchAndCount(any(SearchCriteria.class), any());

        Filter filter = new Filter(DnsServerVO.class, "id", true, 0L, 10L);
        Set<Long> domainIds = new HashSet<>(Arrays.asList(10L, 20L));
        Pair<List<DnsServerVO>, Integer> result = dao.searchDnsServer(100L, 1L, domainIds, DnsProviderType.PowerDNS, "test", filter);

        assertNotNull(result);
        assertEquals(1, result.first().size());
        assertEquals(1, result.second().intValue());
        assertEquals("test-server", result.first().get(0).getName());
    }

    @Test
    public void testSearchDnsServerWithNullParams() {
        List<DnsServerVO> expected = Collections.singletonList(mockServer);
        Pair<List<DnsServerVO>, Integer> expectedPair = new Pair<>(expected, 1);
        doReturn(expectedPair).when(dao).searchAndCount(any(SearchCriteria.class), any());

        Filter filter = new Filter(DnsServerVO.class, "id", true, 0L, 10L);
        Set<Long> domainIds = new HashSet<>();
        Pair<List<DnsServerVO>, Integer> result = dao.searchDnsServer(null, null, domainIds, null, null, filter);

        assertNotNull(result);
        assertEquals(1, result.first().size());
        assertEquals(1, result.second().intValue());
    }
}
