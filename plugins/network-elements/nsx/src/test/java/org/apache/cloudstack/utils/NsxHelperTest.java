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
package org.apache.cloudstack.utils;

import org.junit.Test;

import com.cloud.utils.Pair;

import static org.junit.Assert.assertEquals;

public class NsxHelperTest {

    @Test
    public void testVpnVtiAddressPairFirstSlot() {
        Pair<String, String> vtiAddresses = NsxHelper.getVpnVtiAddressPair(0L);
        assertEquals("169.254.64.1", vtiAddresses.first());
        assertEquals("169.254.64.2", vtiAddresses.second());
    }

    @Test
    public void testVpnVtiAddressPairIsDerivedFromConnectionId() {
        Pair<String, String> vtiAddresses = NsxHelper.getVpnVtiAddressPair(5L);
        assertEquals("169.254.64.21", vtiAddresses.first());
        assertEquals("169.254.64.22", vtiAddresses.second());
    }

    @Test
    public void testVpnVtiAddressPairLastSlotStaysWithinSubnet() {
        Pair<String, String> vtiAddresses = NsxHelper.getVpnVtiAddressPair(4095L);
        assertEquals("169.254.127.253", vtiAddresses.first());
        assertEquals("169.254.127.254", vtiAddresses.second());
    }

    @Test
    public void testVpnVtiAddressPairWrapsAfterSubnetIsExhausted() {
        assertEquals(NsxHelper.getVpnVtiAddressPair(1L), NsxHelper.getVpnVtiAddressPair(4097L));
    }

}
