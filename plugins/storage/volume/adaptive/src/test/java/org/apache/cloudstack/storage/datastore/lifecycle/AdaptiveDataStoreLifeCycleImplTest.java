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
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class AdaptiveDataStoreLifeCycleImplTest {

    @Test
    public void portOrDefaultUsesSchemeDefaultWhenUrlHasNoPort() throws Exception {
        Assert.assertEquals(443, AdaptiveDataStoreLifeCycleImpl.portOrDefault(new URL("https://array/api?pod=p1")));
        Assert.assertEquals(80, AdaptiveDataStoreLifeCycleImpl.portOrDefault(new URL("http://array/api?pod=p1")));
    }

    @Test
    public void portOrDefaultKeepsExplicitPort() throws Exception {
        Assert.assertEquals(8443, AdaptiveDataStoreLifeCycleImpl.portOrDefault(new URL("https://array:8443/api?pod=p1")));
    }
}
