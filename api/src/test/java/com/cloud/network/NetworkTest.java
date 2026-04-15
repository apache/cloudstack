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

package com.cloud.network;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class NetworkTest {

    @Test
    public void testProviderContains() {
        List<Network.Provider> providers = new ArrayList<>();
        providers.add(Network.Provider.VirtualRouter);

        // direct instance present
        assertTrue("List should contain VirtualRouter provider", providers.contains(Network.Provider.VirtualRouter));

        // resolved provider by name (registered provider)
        Network.Provider resolved = Network.Provider.getProvider("VirtualRouter");
        assertNotNull("Resolved provider should not be null", resolved);
        assertTrue("List should contain resolved VirtualRouter provider", providers.contains(resolved));

        // transient provider with same name should be considered equal (equals by name)
        Network.Provider transientProvider = Network.Provider.createTransientProvider("NetworkExtension");
        assertFalse("List should not contain the transient provider", providers.contains(transientProvider));

        providers.add(transientProvider);
        assertTrue("List should contain the transient provider", providers.contains(transientProvider));

        // another transient provider with same name should be considered equal
        Network.Provider transientProviderNew = Network.Provider.createTransientProvider("NetworkExtension");
        assertTrue("List should contain the new transient provider with same name", providers.contains(transientProviderNew));
    }
}
