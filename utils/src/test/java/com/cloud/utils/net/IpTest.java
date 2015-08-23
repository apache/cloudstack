//
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
//

package com.cloud.utils.net;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class IpTest {

    @Test
    public void testUltimate() {
        Ip max = new Ip(2L * Integer.MAX_VALUE +1 );
        assertEquals("Maximal address not created", "255.255.255.255", max.addr());
    }
    @Test
    public void testTurningOfTheCentury() {
        Ip eve = new Ip(Integer.MAX_VALUE);
        assertEquals("Minimal address not created", "127.255.255.255", eve.addr());
        Ip dawn = new Ip(Integer.MAX_VALUE + 1L);
        assertEquals("Minimal address not created", "128.0.0.0", dawn.addr());
    }
    @Test
    public void testStart() {
        Ip min = new Ip(0);
        assertEquals("Minimal address not created", "0.0.0.0", min.addr());
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(new Ip("0.0.0.1"), new Ip(1L))
                .addEqualityGroup(new Ip("0.0.0.0"), new Ip(0L))
                .testEquals();
    }

    @Test
    public void testIsSameAddressAs() {
        Assert.assertTrue("1 and one should be considdered the same address", new Ip(1L).isSameAddressAs("0.0.0.1"));
        Assert.assertTrue("zero and 0L should be considdered the same address", new Ip("0.0.0.0").isSameAddressAs(0L));
    }

}
