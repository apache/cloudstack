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

package com.cloud.cluster.dao;

import com.cloud.cluster.ManagementServerHostVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for ManagementServerHostDaoImpl focusing on the new findByName method added in PR #641.
 *
 * Note: Full integration tests for findByName would require database setup. These unit tests
 * verify the basic contract of the method (null handling, empty string handling).
 */
@RunWith(MockitoJUnitRunner.class)
public class ManagementServerHostDaoImplTest {

    private final ManagementServerHostDaoImpl dao = new ManagementServerHostDaoImpl();

    // ========== TESTS FOR findByName METHOD (PR #641) ==========

    @Test
    public void testFindByName_ReturnsNullForNullHostname() {
        List<ManagementServerHostVO> result = dao.findAllByName(null);
        assertEquals(0, result.size());
    }

    @Test
    public void testFindByName_ReturnsNullForEmptyHostname() {
        List<ManagementServerHostVO> result = dao.findAllByName("");
        assertEquals(0, result.size());
    }

    @Test
    public void testFindByName_ReturnsNullForWhitespaceHostname() {
        List<ManagementServerHostVO> result = dao.findAllByName("   ");
        assertEquals(0, result.size());
    }

    @Test
    public void testFindByName_ReturnsNullForTabAndSpaceHostname() {
        List<ManagementServerHostVO> result = dao.findAllByName(" \t  ");
        assertEquals(0, result.size());
    }

    /**
     * Note: Tests for actual database lookups would require integration test setup.
     * The key functionality - looking up by hostname to handle Kubernetes pod restarts
     * with changing IPs - is tested through the behavior in ClusterManagerImpl which
     * calls this method when a hostname is found but MSID differs.
     *
     * See ClusterManagerImpl.java lines 1064-1079 for usage.
     */
}
