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
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

public class PathUtilTest {

    @Test
    public void testExtractIdAndSubPath_ReturnsNullForBlankOrInvalidPath() {
        assertNull(PathUtil.extractIdAndSubPath(null, "/api/datacenters"));
        assertNull(PathUtil.extractIdAndSubPath("   ", "/api/datacenters"));
        assertNull(PathUtil.extractIdAndSubPath("api/datacenters/123", "/api/datacenters"));
        assertNull(PathUtil.extractIdAndSubPath("/api/datacenters", "/api/datacenters"));
    }

    @Test
    public void testExtractIdAndSubPath_RemovesBaseRouteAndReturnsSegments() {
        final List<String> parts = PathUtil.extractIdAndSubPath("/api/datacenters/123/sub/path", "/api/datacenters");
        assertEquals(List.of("123", "sub", "path"), parts);
    }

    @Test
    public void testExtractIdAndSubPath_HandlesTrailingSlashBaseAndRepeatedSlashes() {
        final List<String> parts = PathUtil.extractIdAndSubPath("/api/datacenters//123///child/", "/api/datacenters/");
        assertEquals(List.of("123", "child"), parts);
    }

    @Test
    public void testExtractIdAndSubPath_WithoutBaseRouteUsesPathDirectly() {
        final List<String> parts = PathUtil.extractIdAndSubPath("/id-1/sub", "");
        assertEquals(List.of("id-1", "sub"), parts);
    }
}
