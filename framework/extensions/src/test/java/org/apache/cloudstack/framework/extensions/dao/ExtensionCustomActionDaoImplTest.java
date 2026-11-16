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

import org.apache.cloudstack.framework.extensions.vo.ExtensionCustomActionVO;
import org.junit.Before;
import org.junit.Test;

public class ExtensionCustomActionDaoImplTest {

    private ExtensionCustomActionDaoImpl dao;

    @Before
    public void setUp() {
        dao = mock(ExtensionCustomActionDaoImpl.class);
    }

    @Test
    public void findByNameAndExtensionIdReturnsNullWhenNoMatch() {
        when(dao.findByNameAndExtensionId(1L, "nonexistent")).thenReturn(null);
        assertNull(dao.findByNameAndExtensionId(1L, "nonexistent"));
    }

    @Test
    public void findByNameAndExtensionIdReturnsCorrectEntity() {
        ExtensionCustomActionVO expected = new ExtensionCustomActionVO();
        expected.setName("actionName");
        expected.setExtensionId(1L);
        when(dao.findByNameAndExtensionId(1L, "actionName")).thenReturn(expected);
        assertEquals(expected, dao.findByNameAndExtensionId(1L, "actionName"));
    }

    @Test
    public void listIdsByExtensionIdReturnsEmptyListWhenNoMatch() {
        when(dao.listIdsByExtensionId(999L)).thenReturn(List.of());
        assertTrue(dao.listIdsByExtensionId(999L).isEmpty());
    }

    @Test
    public void listIdsByExtensionIdReturnsCorrectIds() {
        List<Long> expectedIds = List.of(1L, 2L, 3L);
        when(dao.listIdsByExtensionId(1L)).thenReturn(expectedIds);
        assertEquals(expectedIds, dao.listIdsByExtensionId(1L));
    }
}
