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
public class Match {
	private Integer protocol;
	private String source_ip_addresses;
	private boolean source_ip_addresses_not;
	private String destination_ip_addresses;
	private boolean destination_ip_addresses_not;
	private Integer source_port_min;
	private Integer source_port_max;
	private boolean source_port_not;
	private Integer destination_port_min;
	private Integer destination_port_max;
	private boolean destination_port_not;
	private String ethertype = "IPv4";
	
	public Integer getProtocol() {
		return protocol;
	}
	
	public void setProtocol(Integer protocol) {
		this.protocol = protocol;
	}
	
	public Integer getSource_port_min() {
		return source_port_min;
	}
	
	public void setSourcePortMin(Integer source_port_min) {
		this.source_port_min = source_port_min;
	}
	
	public Integer getSourcePortMax() {
		return source_port_max;
	}
	
	public void setSourcePortMax(Integer source_port_max) {
		this.source_port_max = source_port_max;
	}
	
	public boolean isSourcePortNot() {
		return source_port_not;
	}

	public void setSourcePortNot(boolean source_port_not) {
		this.source_port_not = source_port_not;
	}

	public Integer getDestinationPortMin() {
		return destination_port_min;
	}
	
	public void setDestinationPortMin(Integer destination_port_min) {
		this.destination_port_min = destination_port_min;
	}
	
	public Integer getDestinationPortMax() {
		return destination_port_max;
	}
	
	public void setDestinationPortMax(Integer destination_port_max) {
		this.destination_port_max = destination_port_max;
	}
	
	public boolean isDestinationPortNot() {
		return destination_port_not;
	}

	public void setDestinationPortNot(boolean destination_port_not) {
		this.destination_port_not = destination_port_not;
	}

	public String getEthertype() {
		return ethertype;
	}
	
	public void setEthertype(String ethertype) {
		this.ethertype = ethertype;
	}

	public String getSourceIpAddresses() {
		return source_ip_addresses;
	}

	public void setSourceIpAddresses(String source_ip_addresses) {
		this.source_ip_addresses = source_ip_addresses;
	}

	public boolean isSourceIpAddressesNot() {
		return source_ip_addresses_not;
	}

	public void setSourceIpAddresses_not(boolean source_ip_addresses_not) {
		this.source_ip_addresses_not = source_ip_addresses_not;
	}

	public String getDestinationIpAddresses() {
		return destination_ip_addresses;
	}

	public void setDestinationIpAddresses(String destination_ip_addresses) {
		this.destination_ip_addresses = destination_ip_addresses;
	}

	public boolean isDestinationIpAddressesNot() {
		return destination_ip_addresses_not;
	}

	public void setDestinationIpAddressesNot(boolean destination_ip_addresses_not) {
		this.destination_ip_addresses_not = destination_ip_addresses_not;
	}
	
	
}
