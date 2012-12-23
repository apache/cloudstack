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
package com.cloud.network.nicira;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.gson.Gson;

public class NatRuleTest {
	
	Gson gson = new Gson();
	
	@Test
	public void testNatRuleEncoding() {
		NatRule rn1 = new NatRule();
		rn1.setToDestinationIpAddressMax("10.10.10.10");
		rn1.setToDestinationIpAddressMin("10.10.10.10");
		rn1.setToDestinationPort(80);
		Match mr1 = new Match();
		mr1.setSourceIpAddresses("11.11.11.11/24");
		mr1.setEthertype("IPv4");
		mr1.setProtocol(6);
		rn1.setMatch(mr1);
		
		
		String jsonString = gson.toJson(rn1);
		NatRule dnr = gson.fromJson(jsonString, NatRule.class);
		
		assertTrue(rn1.equals(dnr));
	}
}
