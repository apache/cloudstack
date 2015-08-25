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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class NatRuleAdapterTest {
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(NatRule.class, new NatRuleAdapter())
        .create();

    @Test(expected = JsonParseException.class)
    public void testNatRuleAdapterNoType() {
        gson.fromJson("{}", NatRule.class);
    }

    @Test(expected = JsonParseException.class)
    public void testNatRuleAdapterWrongType() {
        gson.fromJson("{type : \"WrongType\"}", NatRule.class);
    }

    @Test()
    public void testNatRuleAdapterWithSourceNatRule() {
        final SourceNatRule sourceNatRule = (SourceNatRule) gson.fromJson("{type : \"SourceNatRule\"}", NatRule.class);

        assertThat(sourceNatRule, instanceOf(SourceNatRule.class));
    }

    @Test()
    public void testNatRuleAdapterWithDestinationNatRule() {
        final DestinationNatRule destinationNatRule = (DestinationNatRule) gson.fromJson("{type : \"DestinationNatRule\"}", NatRule.class);

        assertThat(destinationNatRule, instanceOf(DestinationNatRule.class));
    }

}
