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

import org.junit.Test;

public class MacAddressTest {

    @Test
    public final void testMacAddress() throws Exception {
        MacAddress mac = new MacAddress();
        assertEquals(0L,mac.toLong());
    }

    @Test
    public final void testMacAddressLong() throws Exception {
        MacAddress mac = new MacAddress(1L);
        assertEquals(1L,mac.toLong());
    }

    @Test
    public final void testMacAddressToLong() throws Exception {
        // TODO this test should fail this address is beyond the acceptable range for macaddresses
        MacAddress mac = new MacAddress(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE,mac.toLong());
        System.out.println(mac.toString());
    }

    // TODO    public final void testToLong() throws Exception {
    // TODO    public final void testToByteArray() throws Exception {
    // TODO    public final void testToStringString() throws Exception {
    // TODO    public final void testToString() throws Exception {
    // TODO    public final void testGetMacAddress() throws Exception {
    // TODO    public final void testParse() throws Exception {
    // TODO    public final void testMain() throws Exception {
    // TODO    public final void testParseLong() throws Exception {
    // TODO    public final void testParseInt() throws Exception {
    // TODO    public final void testParseShort() throws Exception {
    // TODO    public final void testParseByte() throws Exception {

}
