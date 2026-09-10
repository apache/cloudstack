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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class DiskTest {
    @Test
    public void gettersSetters() {
        Disk disk = new Disk();
        disk.setName("root-disk");
        disk.setAlias("vm-alias");
        disk.setFormat("cow");
        disk.setProvisionedSize("10737418240");
        disk.setActualSize("2147483648");
        disk.setStatus("ok");
        disk.setImageId("img-uuid-1");
        disk.setBootable("true");
        assertEquals("root-disk", disk.getName());
        assertEquals("vm-alias", disk.getAlias());
        assertEquals("cow", disk.getFormat());
        assertEquals("10737418240", disk.getProvisionedSize());
        assertEquals("2147483648", disk.getActualSize());
        assertEquals("ok", disk.getStatus());
        assertEquals("img-uuid-1", disk.getImageId());
        assertEquals("true", disk.getBootable());
    }

    @Test
    public void json_ContainsFormatAndName() throws Exception {
        Mapper mapper = new Mapper();
        Disk disk = new Disk();
        disk.setName("data-disk");
        disk.setFormat("raw");
        String json = mapper.toJson(disk);
        assertTrue(json.contains("\"name\":\"data-disk\""));
        assertTrue(json.contains("\"format\":\"raw\""));
    }

    @Test
    public void json_OmitsNullOptionals() throws Exception {
        Mapper mapper = new Mapper();
        Disk disk = new Disk();
        disk.setName("minimal");
        String json = mapper.toJson(disk);
        assertFalse(json.contains("\"alias\""));
        assertFalse(json.contains("\"bootable\""));
    }
}
