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

package org.apache.cloudstack.framework.extensions.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.extension.ExtensionResourceMap;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapVO;
import org.junit.Before;
import org.junit.Test;

public class ExtensionResourceMapDaoImplTest {

    private ExtensionResourceMapDaoImpl dao;

    @Before
    public void setUp() {
        dao = mock(ExtensionResourceMapDaoImpl.class);
    }

    @Test
    public void listByExtensionIdReturnsEmptyListWhenNoMatch() {
        when(dao.listByExtensionId(999L)).thenReturn(List.of());
        assertTrue(dao.listByExtensionId(999L).isEmpty());
    }

    @Test
    public void listByExtensionIdReturnsCorrectEntities() {
        ExtensionResourceMapVO entity1 = new ExtensionResourceMapVO();
        entity1.setExtensionId(1L);
        ExtensionResourceMapVO entity2 = new ExtensionResourceMapVO();
        entity2.setExtensionId(1L);
        List<ExtensionResourceMapVO> expected = List.of(entity1, entity2);
        when(dao.listByExtensionId(1L)).thenReturn(expected);
        assertEquals(expected, dao.listByExtensionId(1L));
    }

    @Test
    public void findByResourceIdAndTypeReturnsNullWhenNoMatch() {
        when(dao.findByResourceIdAndType(999L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(null);
        assertNull(dao.findByResourceIdAndType(999L, ExtensionResourceMap.ResourceType.Cluster));
    }

    @Test
    public void findByResourceIdAndTypeReturnsCorrectEntity() {
        ExtensionResourceMapVO expected = new ExtensionResourceMapVO();
        expected.setResourceId(123L);
        expected.setResourceType(ExtensionResourceMap.ResourceType.Cluster);
        when(dao.findByResourceIdAndType(123L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(expected);
        assertEquals(expected, dao.findByResourceIdAndType(123L, ExtensionResourceMap.ResourceType.Cluster));
    }

    @Test
    public void listResourceIdsByExtensionIdAndTypeReturnsEmptyListWhenNoMatch() {
        when(dao.listResourceIdsByExtensionIdAndType(999L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(List.of());
        assertTrue(dao.listResourceIdsByExtensionIdAndType(999L, ExtensionResourceMap.ResourceType.Cluster).isEmpty());
    }

    @Test
    public void listResourceIdsByExtensionIdAndTypeReturnsCorrectIds() {
        List<Long> expectedIds = List.of(1L, 2L, 3L);
        when(dao.listResourceIdsByExtensionIdAndType(1L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(expectedIds);
        assertEquals(expectedIds, dao.listResourceIdsByExtensionIdAndType(1L, ExtensionResourceMap.ResourceType.Cluster));
    }

    // -----------------------------------------------------------------------
    // Tests for new methods: listByResourceIdAndType, listResourceIdsByType
    // -----------------------------------------------------------------------

    @Test
    public void listByResourceIdAndTypeReturnsEmptyListWhenNoMatch() {
        when(dao.listByResourceIdAndType(999L, ExtensionResourceMap.ResourceType.PhysicalNetwork)).thenReturn(List.of());
        assertTrue(dao.listByResourceIdAndType(999L, ExtensionResourceMap.ResourceType.PhysicalNetwork).isEmpty());
    }

    @Test
    public void listByResourceIdAndTypeReturnsMatchingEntries() {
        ExtensionResourceMapVO map1 = new ExtensionResourceMapVO();
        map1.setResourceId(42L);
        map1.setResourceType(ExtensionResourceMap.ResourceType.PhysicalNetwork);
        ExtensionResourceMapVO map2 = new ExtensionResourceMapVO();
        map2.setResourceId(42L);
        map2.setResourceType(ExtensionResourceMap.ResourceType.PhysicalNetwork);
        List<ExtensionResourceMapVO> expected = List.of(map1, map2);
        when(dao.listByResourceIdAndType(42L, ExtensionResourceMap.ResourceType.PhysicalNetwork)).thenReturn(expected);
        List<ExtensionResourceMapVO> result = dao.listByResourceIdAndType(42L, ExtensionResourceMap.ResourceType.PhysicalNetwork);
        assertEquals(2, result.size());
        assertEquals(expected, result);
    }

    @Test
    public void listByResourceIdAndTypeDifferentiatesResourceTypes() {
        ExtensionResourceMapVO clusterMap = new ExtensionResourceMapVO();
        clusterMap.setResourceType(ExtensionResourceMap.ResourceType.Cluster);
        when(dao.listByResourceIdAndType(10L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(List.of(clusterMap));
        when(dao.listByResourceIdAndType(10L, ExtensionResourceMap.ResourceType.PhysicalNetwork)).thenReturn(List.of());

        assertEquals(1, dao.listByResourceIdAndType(10L, ExtensionResourceMap.ResourceType.Cluster).size());
        assertTrue(dao.listByResourceIdAndType(10L, ExtensionResourceMap.ResourceType.PhysicalNetwork).isEmpty());
    }

    @Test
    public void listResourceIdsByTypeReturnsEmptyListWhenNoMatch() {
        when(dao.listResourceIdsByType(ExtensionResourceMap.ResourceType.PhysicalNetwork)).thenReturn(List.of());
        assertTrue(dao.listResourceIdsByType(ExtensionResourceMap.ResourceType.PhysicalNetwork).isEmpty());
    }

    @Test
    public void listResourceIdsByTypeReturnsMatchingIds() {
        List<Long> expectedIds = List.of(5L, 10L, 15L);
        when(dao.listResourceIdsByType(ExtensionResourceMap.ResourceType.PhysicalNetwork)).thenReturn(expectedIds);
        List<Long> result = dao.listResourceIdsByType(ExtensionResourceMap.ResourceType.PhysicalNetwork);
        assertEquals(3, result.size());
        assertEquals(expectedIds, result);
    }
}
