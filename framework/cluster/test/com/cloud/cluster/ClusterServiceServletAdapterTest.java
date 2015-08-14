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
package com.cloud.cluster;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.utils.component.ComponentLifecycle;

@RunWith(MockitoJUnitRunner.class)
public class ClusterServiceServletAdapterTest {

    ClusterServiceServletAdapter clusterServiceServletAdapter;
    ClusterManagerImpl clusterManagerImpl;

    @Before
    public void setup() throws IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        clusterServiceServletAdapter = new ClusterServiceServletAdapter();
        clusterManagerImpl = new ClusterManagerImpl();
    }

    @Test
    public void testRunLevel() {
        int runLevel = clusterServiceServletAdapter.getRunLevel();
        assertTrue(runLevel == ComponentLifecycle.RUN_LEVEL_FRAMEWORK);
        assertTrue(runLevel == clusterManagerImpl.getRunLevel());
    }
}
