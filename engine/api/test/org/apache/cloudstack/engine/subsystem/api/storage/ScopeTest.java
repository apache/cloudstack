/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.subsystem.api.storage;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Test;

public class ScopeTest {

    @Test
    public void testZoneScope() {
        ZoneScope zoneScope = new ZoneScope(1L);
        ZoneScope zoneScope2 = new ZoneScope(1L);
        Assert.assertTrue(zoneScope.isSameScope(zoneScope2));

        ZoneScope zoneScope3 = new ZoneScope(2L);
        Assert.assertFalse(zoneScope.isSameScope(zoneScope3));
    }

    @Test
    public void testClusterScope() {
        ClusterScope clusterScope = new ClusterScope(1L, 1L, 1L);
        ClusterScope clusterScope2 = new ClusterScope(1L, 1L, 1L);

        Assert.assertTrue(clusterScope.isSameScope(clusterScope2));

        ClusterScope clusterScope3 = new ClusterScope(2L, 2L, 1L);
        Assert.assertFalse(clusterScope.isSameScope(clusterScope3));
    }

    @Test
    public void testHostScope() {
        HostScope hostScope = new HostScope(1L, 1L, 1L);
        HostScope hostScope2 = new HostScope(1L, 1L, 1L);
        HostScope hostScope3 = new HostScope(2L, 1L, 1L);

        Assert.assertTrue(hostScope.isSameScope(hostScope2));
        Assert.assertFalse(hostScope.isSameScope(hostScope3));
    }

}
