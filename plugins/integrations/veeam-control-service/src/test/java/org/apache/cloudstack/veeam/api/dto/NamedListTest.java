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

package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class NamedListTest {

    @Test
    public void of_NullItems_UsesEmptyList() {
        NamedList<String> namedList = NamedList.of("item", null);

        assertNotNull(namedList.getItems());
        assertTrue(namedList.getItems().isEmpty());
        assertTrue(namedList.asMap().containsKey("item"));
    }

    @Test
    public void of_ValidName_StoresItemsInMap() {
        List<String> values = Arrays.asList("a", "b");
        NamedList<String> namedList = NamedList.of("values", values);

        assertEquals(values, namedList.getItems());
        assertEquals(values, namedList.asMap().get("values"));
    }

    @Test
    public void of_EmptyName_ThrowsIllegalArgumentException() {
        try {
            NamedList.of("", Collections.singletonList("x"));
            fail("Expected IllegalArgumentException for empty name");
        } catch (IllegalArgumentException e) {
            assertEquals("name must be non-empty", e.getMessage());
        }
    }

    @Test
    public void fromMap_InvalidMapShape_ThrowsIllegalArgumentException() {
        try {
            NamedList.fromMap(null);
            fail("Expected IllegalArgumentException for null map");
        } catch (IllegalArgumentException e) {
            assertEquals("Expected single-property object for NamedList", e.getMessage());
        }

        Map<String, List<String>> invalid = new HashMap<>();
        invalid.put("a", Collections.singletonList("x"));
        invalid.put("b", Collections.singletonList("y"));

        try {
            NamedList.fromMap(invalid);
            fail("Expected IllegalArgumentException for map with multiple keys");
        } catch (IllegalArgumentException e) {
            assertEquals("Expected single-property object for NamedList", e.getMessage());
        }
    }

    @Test
    public void fromMap_SingleEntry_ReturnsNamedList() {
        Map<String, List<String>> map = Collections.singletonMap("usage", Collections.singletonList("vm"));

        NamedList<String> namedList = NamedList.fromMap(map);

        assertEquals(Collections.singletonList("vm"), namedList.getItems());
        assertEquals(map, namedList.asMap());
    }
}

