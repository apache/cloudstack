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

package com.cloud.network.nicira;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NatRuleTest {

    @Test
    public void testNatRuleEncoding() {
        final Gson gson =
            new GsonBuilder().registerTypeAdapter(NatRule.class, new NatRuleAdapter())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        final DestinationNatRule rn1 = new DestinationNatRule();
        rn1.setToDestinationIpAddress("10.10.10.10");
        rn1.setToDestinationPort(80);
        final Match mr1 = new Match();
        mr1.setSourceIpAddresses("11.11.11.11/24");
        mr1.setEthertype("IPv4");
        mr1.setProtocol(6);
        rn1.setMatch(mr1);

        final String jsonString = gson.toJson(rn1);
        final NatRule dnr = gson.fromJson(jsonString, NatRule.class);

        assertTrue(dnr instanceof DestinationNatRule);
        assertTrue(rn1.equals(dnr));
    }

}
