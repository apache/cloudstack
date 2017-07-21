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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by dahn on 18/07/17.
 */
public class IsolationMethodTest {
    @Test
    public void equalsTest() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla");
        assertEquals(PhysicalNetwork.IsolationMethod.UNKNOWN_PROVIDER, method.provider);
        assertEquals(new PhysicalNetwork.IsolationMethod("bla", PhysicalNetwork.IsolationMethod.UNKNOWN_PROVIDER), method);
    }

    @Test
    public void toStringTest() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla", "blob");
        assertEquals("bla", method.toString());

    }

    @Test
    public void remove() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla", "blob");

        PhysicalNetwork.IsolationMethod.remove("bla","blob");
        assertNull(PhysicalNetwork.IsolationMethod.getIsolationMethod("bla"));

        method = new PhysicalNetwork.IsolationMethod("blob", "bla");

        PhysicalNetwork.IsolationMethod.remove(method);
        assertNull(PhysicalNetwork.IsolationMethod.getIsolationMethod("bla"));
    }
}