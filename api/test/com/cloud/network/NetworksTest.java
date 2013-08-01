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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * @author dhoogland
 * 
 */
public class NetworksTest {

    @Before
    public void setUp() {
    }

    @Test
    public void emptyBroadcastDomainTypeTest() throws URISyntaxException {
        BroadcastDomainType type = BroadcastDomainType.getTypeOf("");
        Assert.assertEquals(
                "an empty uri should mean a broadcasttype of undecided",
                BroadcastDomainType.UnDecided, type);
    }

    @Test
    public void vlanBroadcastDomainTypeTest() throws URISyntaxException {
        String uri1 = "vlan://1";
        Long value2 = 2L;
        String uri2 = BroadcastDomainType.Vlan.toUri(value2).toString();
        BroadcastDomainType type1 = BroadcastDomainType.getTypeOf(uri1);
        String id1 = BroadcastDomainType.getValue(uri1);
        String id2 = BroadcastDomainType.getValue(uri2);
        Assert.assertEquals("uri1 should be of broadcasttype vlan",
                BroadcastDomainType.Vlan, type1);
        Assert.assertEquals("id1 should be \"1\"", "1", id1);
        Assert.assertEquals("id2 should be \"2\"", "2", id2);
    }

    @Test
    public void vlanIsolationTypeTest() throws URISyntaxException {
        String uri1 = "vlan://1";
        Long value2 = 2L;
        String uri2 = IsolationType.Vlan.toUri(value2).toString();
        Assert.assertEquals("id1 should be \"vlan://1\"", "vlan://1", uri1);
        Assert.assertEquals("id2 should be \"vlan://2\"", "vlan://2", uri2);
    }

    @Test
    public void otherTypesTest() throws URISyntaxException {
        String bogeyUri = "lswitch://0";
        String uri1 = "lswitch:1";
        String uri2 = "mido://2";
        BroadcastDomainType type = BroadcastDomainType.getTypeOf(bogeyUri);
        String id = BroadcastDomainType.getValue(bogeyUri);
        Assert.assertEquals("uri0 should be of broadcasttype vlan",
                BroadcastDomainType.Lswitch, type);
        Assert.assertEquals("id0 should be \"//0\"", "//0", id);
        type = BroadcastDomainType.getTypeOf(uri1);
        id = BroadcastDomainType.getValue(uri1);
        Assert.assertEquals("uri1 should be of broadcasttype vlan",
                BroadcastDomainType.Lswitch, type);
        Assert.assertEquals("id1 should be \"1\"", "1", id);
        type = BroadcastDomainType.getTypeOf(uri2);
        id = BroadcastDomainType.getValue(uri2);
        Assert.assertEquals("uri2 should be of broadcasttype vlan",
                BroadcastDomainType.Mido, type);
        Assert.assertEquals("id2 should be \"2\"", "2", id);
    }

    @Test
    public void invalidTypesTest() throws URISyntaxException {
        String uri1 = "https://1";
        String uri2 = "bla:0";
        BroadcastDomainType type = BroadcastDomainType.getTypeOf(uri1);
        try {
            /* URI result = */ BroadcastDomainType.fromString(uri1);
        } catch (CloudRuntimeException e) {
            Assert.assertEquals("unexpected parameter exception",
                    "string 'https://1' has an unknown BroadcastDomainType.",
                    e.getMessage());
        }
        try {
            /* URI result = */ BroadcastDomainType.fromString(uri2);
        } catch (CloudRuntimeException e) {
            Assert.assertEquals("unexpected parameter exception",
                    "string 'bla:0' has an unknown BroadcastDomainType.",
                    e.getMessage());
        }
    }
}
