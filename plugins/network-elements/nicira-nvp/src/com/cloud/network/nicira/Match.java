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
	private String destination_ip_addresses;
	private Integer source_port;
	private Integer destination_port;
	private String ethertype = "IPv4";
	
	public Integer getProtocol() {
		return protocol;
	}
	
	public void setProtocol(Integer protocol) {
		this.protocol = protocol;
	}
	
	public Integer getSourcePort() {
		return source_port;
	}
	
	public void setSourcePort(Integer source_port) {
		this.source_port = source_port;
	}
		
	public Integer getDestinationPort() {
		return destination_port;
	}
	
	public void setDestinationPort(Integer destination_port) {
		this.destination_port = destination_port;
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

	public String getDestinationIpAddresses() {
		return destination_ip_addresses;
	}

	public void setDestinationIpAddresses(String destination_ip_addresses) {
		this.destination_ip_addresses = destination_ip_addresses;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((destination_ip_addresses == null) ? 0
						: destination_ip_addresses.hashCode());
		result = prime
				* result
				+ ((destination_port == null) ? 0 : destination_port
						.hashCode());
		result = prime * result
				+ ((ethertype == null) ? 0 : ethertype.hashCode());
		result = prime * result
				+ ((protocol == null) ? 0 : protocol.hashCode());
		result = prime
				* result
				+ ((source_ip_addresses == null) ? 0 : source_ip_addresses
						.hashCode());
		result = prime * result
				+ ((source_port == null) ? 0 : source_port.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Match other = (Match) obj;
		if (destination_ip_addresses == null) {
			if (other.destination_ip_addresses != null)
				return false;
		} else if (!destination_ip_addresses
				.equals(other.destination_ip_addresses))
			return false;
		if (destination_port == null) {
			if (other.destination_port != null)
				return false;
		} else if (!destination_port.equals(other.destination_port))
			return false;
		if (ethertype == null) {
			if (other.ethertype != null)
				return false;
		} else if (!ethertype.equals(other.ethertype))
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		} else if (!protocol.equals(other.protocol))
			return false;
		if (source_ip_addresses == null) {
			if (other.source_ip_addresses != null)
				return false;
		} else if (!source_ip_addresses.equals(other.source_ip_addresses))
			return false;
		if (source_port == null) {
			if (other.source_port != null)
				return false;
		} else if (!source_port.equals(other.source_port))
			return false;
		return true;
	}
	
	
}
