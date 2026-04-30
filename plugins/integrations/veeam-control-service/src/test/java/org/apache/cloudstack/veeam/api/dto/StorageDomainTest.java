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

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class StorageDomainTest {
    @Test
    public void gettersSetters() {
        StorageDomain sd = new StorageDomain();
        sd.setName("data-domain");
        sd.setType("data");
        sd.setStatus("active");
        sd.setAvailable("107374182400");
        sd.setUsed("21474836480");
        sd.setStorageFormat("v5");
        assertEquals("data-domain", sd.getName());
        assertEquals("data", sd.getType());
        assertEquals("active", sd.getStatus());
        assertEquals("107374182400", sd.getAvailable());
        assertEquals("21474836480", sd.getUsed());
        assertEquals("v5", sd.getStorageFormat());
    }

    @Test
    public void json_ContainsNameAndType() throws Exception {
        Mapper mapper = new Mapper();
        StorageDomain sd = new StorageDomain();
        sd.setName("nfs-storage");
        sd.setType("data");
        String json = mapper.toJson(sd);
        assertTrue(json.contains("\"name\":\"nfs-storage\""));
        assertTrue(json.contains("\"type\":\"data\""));
    }
}
