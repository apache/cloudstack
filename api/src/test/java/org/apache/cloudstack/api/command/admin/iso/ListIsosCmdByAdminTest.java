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

package org.apache.cloudstack.api.command.admin.iso;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

public class ListIsosCmdByAdminTest {

    @InjectMocks
    ListIsosCmdByAdmin cmd;
    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetDataStoreId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "dataStoreId", id);
        assertEquals(id, cmd.getDataStoreId());
    }

    @Test
    public void testGetStoreId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "zoneId", id);
        assertEquals(id, cmd.getZoneId());
    }

    @Test
    public void testGetShowRemoved() {
        Boolean showRemoved = true;
        ReflectionTestUtils.setField(cmd, "showRemoved", showRemoved);
        assertEquals(showRemoved, cmd.getShowRemoved());
    }

    @Test
    public void testGetIsoName() {
        String isoName = "test";
        ReflectionTestUtils.setField(cmd, "isoName", isoName);
        assertEquals(isoName, cmd.getIsoName());
    }

    @Test
    public void testGetIsoFilter() {
        String isoFilter = "test";
        ReflectionTestUtils.setField(cmd, "isoFilter", isoFilter);
        assertEquals(isoFilter, cmd.getIsoFilter());
    }

    @Test
    public void testGetShowUnique() {
        Boolean showUnique = true;
        ReflectionTestUtils.setField(cmd, "showUnique", showUnique);
        assertEquals(showUnique, cmd.getShowUnique());
    }

    @Test
    public void testGetShowIcon() {
        Boolean showIcon = true;
        ReflectionTestUtils.setField(cmd, "showIcon", showIcon);
        assertEquals(showIcon, cmd.getShowIcon());
    }

    @Test
    public void testGetBootable() {
        Boolean bootable = true;
        ReflectionTestUtils.setField(cmd, "bootable", bootable);
        assertEquals(bootable, cmd.isBootable());
    }

    @Test
    public void testGetHypervisor() {
        String hypervisor = "test";
        ReflectionTestUtils.setField(cmd, "hypervisor", hypervisor);
        assertEquals(hypervisor, cmd.getHypervisor());
    }

    @Test
    public void testGetId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void testGetPublic() {
        Boolean publicIso = true;
        ReflectionTestUtils.setField(cmd, "publicIso", publicIso);
        assertEquals(publicIso, cmd.isPublic());
    }

    @Test
    public void testGetReady() {
        Boolean ready = true;
        ReflectionTestUtils.setField(cmd, "ready", ready);
        assertEquals(ready, cmd.isReady());
    }
}
