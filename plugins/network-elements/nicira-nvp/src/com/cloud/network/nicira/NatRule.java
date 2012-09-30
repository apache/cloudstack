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
public abstract class NatRule {
	protected Match match;
	protected String to_source_ip_address_min;
	protected String to_source_ip_address_max;
	protected Integer to_source_port_min;
	protected Integer to_source_port_max;
	protected String uuid;
	protected String type;
	protected String to_destination_ip_address_min;
	protected String to_destination_ip_address_max;
	protected Integer to_destination_port;
	
	public Match getMatch() {
		return match;
	}
	
	public void setMatch(Match match) {
		this.match = match;
	}
	
	public String getToSourceIpAddressMin() {
		return to_source_ip_address_min;
	}
	
	public void setToSourceIpAddressMin(String to_source_ip_address_min) {
		this.to_source_ip_address_min = to_source_ip_address_min;
	}
	
	public String getToSourceIpAddressMax() {
		return to_source_ip_address_max;
	}
	
	public void setToSourceIpAddressMax(String to_source_ip_address_max) {
		this.to_source_ip_address_max = to_source_ip_address_max;
	}
	
	public Integer getToSourcePortMin() {
		return to_source_port_min;
	}
	
	public void setToSourcePortMin(Integer to_source_port_min) {
		this.to_source_port_min = to_source_port_min;
	}
	
	public Integer getToSourcePortMax() {
		return to_source_port_max;
	}
	
	public void setToSourcePortMax(Integer to_source_port_max) {
		this.to_source_port_max = to_source_port_max;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getToDestinationIpAddressMin() {
		return to_destination_ip_address_min;
	}
	
	public void setToDestinationIpAddressMin(
			String to_destination_ip_address_min) {
		this.to_destination_ip_address_min = to_destination_ip_address_min;
	}
	
	public String getToDestinationIpAddressMax() {
		return to_destination_ip_address_max;
	}
	
	public void setToDestinationIpAddressMax(
			String to_destination_ip_address_max) {
		this.to_destination_ip_address_max = to_destination_ip_address_max;
	}
	
	public Integer getToDestinationPort() {
		return to_destination_port;
	}
	
	public void setToDestinationPort(Integer to_destination_port) {
		this.to_destination_port = to_destination_port;
	}
	
	public String getType() {
		return type;
	}
}
