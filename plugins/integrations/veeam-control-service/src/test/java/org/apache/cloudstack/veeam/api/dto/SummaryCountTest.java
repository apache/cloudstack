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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class SummaryCountTest {
    @Test
    public void constructor_IntValues_ConvertsToStrings() {
        SummaryCount count = new SummaryCount(3, 10);
        assertEquals("3", count.getActive());
        assertEquals("10", count.getTotal());
    }

    @Test
    public void defaultConstructor_NullValues() {
        SummaryCount count = new SummaryCount();
        assertNull(count.getActive());
        assertNull(count.getTotal());
    }

    @Test
    public void json_OmitsNullFields() throws Exception {
        Mapper mapper = new Mapper();
        SummaryCount count = new SummaryCount();
        String json = mapper.toJson(count);
        assertFalse(json.contains("active"));
        assertFalse(json.contains("total"));
    }

    @Test
    public void json_IncludesPopulatedFields() throws Exception {
        Mapper mapper = new Mapper();
        SummaryCount count = new SummaryCount(5, 20);
        String json = mapper.toJson(count);
        assertEquals("{\"active\":\"5\",\"total\":\"20\"}", json);
    }
}
