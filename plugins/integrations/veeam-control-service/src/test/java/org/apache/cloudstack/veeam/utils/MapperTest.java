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

package org.apache.cloudstack.veeam.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MapperTest {

    @Test
    public void testToJson_UsesSnakeCaseAndOmitsNulls() throws Exception {
        final Mapper mapper = new Mapper();
        final String json = mapper.toJson(new Sample("John", "Doe", null));

        assertTrue(json.contains("\"first_name\":\"John\""));
        assertTrue(json.contains("\"last_name\":\"Doe\""));
        assertFalse(json.contains("optional_field"));
    }

    @Test
    public void testToXml_UsesSnakeCaseAndOmitsNulls() throws Exception {
        final Mapper mapper = new Mapper();
        final String xml = mapper.toXml(new Sample("John", "Doe", null));

        assertTrue(xml.contains("<first_name>John</first_name>"));
        assertTrue(xml.contains("<last_name>Doe</last_name>"));
        assertFalse(xml.contains("optional_field"));
    }

    @Test
    public void testJsonMapper_IgnoresUnknownProperties() throws Exception {
        final Mapper mapper = new Mapper();
        final Sample sample = mapper.jsonMapper().readValue("{\"first_name\":\"Alice\",\"unknown\":\"x\"}", Sample.class);

        assertNotNull(sample);
        assertEquals("Alice", sample.firstName);
    }

    static class Sample {
        public String firstName;
        public String lastName;
        public String optionalField;

        Sample() {
        }

        Sample(final String firstName, final String lastName, final String optionalField) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.optionalField = optionalField;
        }
    }
}
