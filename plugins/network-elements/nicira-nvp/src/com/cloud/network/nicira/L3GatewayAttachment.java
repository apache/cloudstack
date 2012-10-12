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

/**
 * 
 */
public class L3GatewayAttachment extends Attachment {
	private String l3_gateway_service_uuid;
	private String type = "L3GatewayAttachment";
	private Long vlan_id;
	
	public L3GatewayAttachment(String l3_gateway_service_uuid) {
		this.l3_gateway_service_uuid = l3_gateway_service_uuid;
	}
	
	public L3GatewayAttachment(String l3_gateway_service_uuid, long vlan_id) {
		this.l3_gateway_service_uuid = l3_gateway_service_uuid;
		this.vlan_id = vlan_id;
	}
	
	public String getL3GatewayServiceUuid() {
		return l3_gateway_service_uuid;
	}
	
	public void setL3GatewayServiceUuid(String l3_gateway_service_uuid) {
		this.l3_gateway_service_uuid = l3_gateway_service_uuid;
	}
	
	public long getVlanId() {
		return vlan_id;
	}
	
	public void setVlanId(long vlan_id) {
		this.vlan_id = vlan_id;
	}

}
