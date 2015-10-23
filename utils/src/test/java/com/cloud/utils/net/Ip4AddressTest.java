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
package com.cloud.utils.net;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class Ip4AddressTest {

    @Test
    public void testEquals() throws Exception {
        new EqualsTester()
                .addEqualityGroup(new Ip4Address("0.0.0.1", "00:00:00:00:00:02"), new Ip4Address(1L, 2L))
                .addEqualityGroup(new Ip4Address("0.0.0.1", "00:00:00:00:00:00"), new Ip4Address(1L, 0L), new Ip4Address(1L, 0L), new Ip4Address(1L), new Ip4Address("0.0.0.1"))
                .testEquals();
    }

    @Test
    public void testIsSameAddressAs() {
        Assert.assertTrue("1 and one should be considdered the same address", new Ip4Address(1L, 5L).isSameAddressAs("0.0.0.1"));
        Assert.assertFalse("zero and 0L should be considdered the same address but a Long won't be accepted", new Ip4Address("0.0.0.0", "00:00:00:00:00:08").isSameAddressAs(0L));
    }

}
