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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.junit.Before;
import org.junit.Test;

public class ExtensionDaoImplTest {

    private ExtensionDaoImpl dao;

    @Before
    public void setUp() {
        dao = mock(ExtensionDaoImpl.class);
    }

    @Test
    public void findByNameReturnsNullWhenNoMatch() {
        when(dao.findByName("nonexistent")).thenReturn(null);
        assertNull(dao.findByName("nonexistent"));
    }

    @Test
    public void findByNameReturnsCorrectEntity() {
        ExtensionVO expected = new ExtensionVO();
        expected.setName("extensionName");
        when(dao.findByName("extensionName")).thenReturn(expected);
        assertEquals(expected, dao.findByName("extensionName"));
    }
}
