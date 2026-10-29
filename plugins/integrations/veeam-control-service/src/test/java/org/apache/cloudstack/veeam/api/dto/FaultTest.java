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

public class FaultTest {
    @Test
    public void constructor_SetsReasonAndDetail() {
        Fault fault = new Fault("Not Found", "Entity was not found");
        assertEquals("Not Found", fault.getReason());
        assertEquals("Entity was not found", fault.getDetail());
    }

    @Test
    public void json_ContainsReasonAndDetail() throws Exception {
        Mapper mapper = new Mapper();
        Fault fault = new Fault("Unauthorized", "Invalid credentials");
        String json = mapper.toJson(fault);
        assertTrue(json.contains("\"reason\":\"Unauthorized\""));
        assertTrue(json.contains("\"detail\":\"Invalid credentials\""));
    }
}
