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
package com.cloud.agent.api;

import com.cloud.network.IpAddress;

/**
 * 
 */
public class AssignIpToLogicalRouterCommand extends Command {
	private String logicalRouterUuid;
	private String gatewayServiceUuid;
	private String publicIpCidr;
	private long publicIpVlan;
	private boolean sourceNat;
	private String internalNetworkCidr;

	public AssignIpToLogicalRouterCommand(String logicalRouterUuid, String gatewayServiceUuid, String publicIpCidr, long publicIpVlan, boolean sourceNat, String internetNetworkCidr) {
		this.logicalRouterUuid = logicalRouterUuid;
		this.gatewayServiceUuid = gatewayServiceUuid;
		this.publicIpCidr = publicIpCidr;
		this.sourceNat = sourceNat;
		this.internalNetworkCidr = internetNetworkCidr;
		this.publicIpVlan = publicIpVlan;
	}
	
	public String getLogicalRouterUuid() {
		return logicalRouterUuid;
	}

	public void setLogicalRouterUuid(String logicalRouterUuid) {
		this.logicalRouterUuid = logicalRouterUuid;
	}

	public String getGatewayServiceUuid() {
		return gatewayServiceUuid;
	}

	public void setGatewayServiceUuid(String gatewayServiceUuid) {
		this.gatewayServiceUuid = gatewayServiceUuid;
	}

	public String getPublicIpCidr() {
		return publicIpCidr;
	}

	public void setPublicIpCidr(String publicIpCidr) {
		this.publicIpCidr = publicIpCidr;
	}

	public long getPublicIpVlan() {
		return publicIpVlan;
	}

	public void setPublicIpVlan(long publicIpVlan) {
		this.publicIpVlan = publicIpVlan;
	}

	public boolean isSourceNat() {
		return sourceNat;
	}

	public void setSourceNat(boolean sourceNat) {
		this.sourceNat = sourceNat;
	}

	public String getInternalNetworkCidr() {
		return internalNetworkCidr;
	}

	public void setInternalNetworkCidr(String internalNetworkCidr) {
		this.internalNetworkCidr = internalNetworkCidr;
	}

	/* (non-Javadoc)
	 * @see com.cloud.agent.api.Command#executeInSequence()
	 */
	@Override
	public boolean executeInSequence() {
		// TODO Auto-generated method stub
		return false;
	}

}
