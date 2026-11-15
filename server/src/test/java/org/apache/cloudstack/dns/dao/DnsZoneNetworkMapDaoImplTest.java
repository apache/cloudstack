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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class DnsZoneNetworkMapDaoImplTest {

    private DnsZoneNetworkMapDaoImpl dao;
    private DnsZoneNetworkMapVO mockMapping;

    @Before
    public void setUp() {
        dao = spy(new DnsZoneNetworkMapDaoImpl());
        mockMapping = new DnsZoneNetworkMapVO(10L, 20L, "dev");
    }

    @Test
    public void testFindByNetworkId() {
        doReturn(mockMapping).when(dao).findOneBy(any(SearchCriteria.class));

        DnsZoneNetworkMapVO result = dao.findByNetworkId(20L);
        assertNotNull(result);
        assertEquals(10L, result.getDnsZoneId());
        assertEquals(20L, result.getNetworkId());
        assertEquals("dev", result.getSubDomain());
    }

    @Test
    public void testFindByNetworkIdNotFound() {
        doReturn(null).when(dao).findOneBy(any(SearchCriteria.class));

        DnsZoneNetworkMapVO result = dao.findByNetworkId(999L);
        assertNull(result);
    }

    @Test
    public void testFindByZoneId() {
        doReturn(mockMapping).when(dao).findOneBy(any(SearchCriteria.class));

        DnsZoneNetworkMapVO result = dao.findByZoneId(10L);
        assertNotNull(result);
        assertEquals(10L, result.getDnsZoneId());
        assertEquals(20L, result.getNetworkId());
    }

    @Test
    public void testFindByZoneIdNotFound() {
        doReturn(null).when(dao).findOneBy(any(SearchCriteria.class));

        DnsZoneNetworkMapVO result = dao.findByZoneId(999L);
        assertNull(result);
    }

    @Test
    public void testRemoveNetworkMappingByZoneId() {
        doReturn(1).when(dao).remove(any(SearchCriteria.class));

        dao.removeNetworkMappingByZoneId(10L);
        verify(dao).remove(any(SearchCriteria.class));
    }
}
