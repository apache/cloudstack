// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class BackupTest {
    @Test
    public void gettersSetters() {
        Backup backup = new Backup();
        backup.setName("backup-vm1");
        backup.setDescription("Full backup");
        backup.setCreationDate(1714465200000L);
        backup.setPhase("succeeded");
        backup.setFromCheckpointId("cp-1");
        backup.setToCheckpointId("cp-2");
        assertEquals("backup-vm1", backup.getName());
        assertEquals("Full backup", backup.getDescription());
        assertEquals(Long.valueOf(1714465200000L), backup.getCreationDate());
        assertEquals("succeeded", backup.getPhase());
        assertEquals("cp-1", backup.getFromCheckpointId());
        assertEquals("cp-2", backup.getToCheckpointId());
    }

    @Test
    public void gettersSetters_VmAndHost() {
        Backup backup = new Backup();
        Vm vm = Vm.of("/api/vms/v1", "v1");
        Host host = Host.of("/api/hosts/h1", "h1");
        backup.setVm(vm);
        backup.setHost(host);
        assertEquals("v1", backup.getVm().getId());
        assertEquals("h1", backup.getHost().getId());
    }

    @Test
    public void disks_NamedList() {
        Backup backup = new Backup();
        Disk disk = new Disk();
        disk.setName("disk-1");
        NamedList<Disk> disks = NamedList.of("disk", Collections.singletonList(disk));
        backup.setDisks(disks);
        assertNotNull(backup.getDisks());
        assertEquals(1, backup.getDisks().getItems().size());
        assertEquals("disk-1", backup.getDisks().getItems().get(0).getName());
    }

    @Test
    public void json_ContainsNameAndPhase() throws Exception {
        Mapper mapper = new Mapper();
        Backup backup = new Backup();
        backup.setName("nightly");
        backup.setPhase("running");
        String json = mapper.toJson(backup);
        assertTrue(json.contains("\"name\":\"nightly\""));
        assertTrue(json.contains("\"phase\":\"running\""));
    }
}
