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
	private Boolean source_ip_addresses_not;
	private String destination_ip_addresses;
	private Boolean destination_ip_addresses_not;
	private Integer source_port_min;
	private Integer source_port_max;
	private Boolean source_port_not;
	private Integer destination_port_min;
	private Integer destination_port_max;
	private Boolean destination_port_not;
	private String ethertype = "IPv4";
	
	public Integer getProtocol() {
		return protocol;
	}
	
	public void setProtocol(Integer protocol) {
		this.protocol = protocol;
	}
	
	public Integer getSourcePortMin() {
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
	
	public Boolean isSourcePortNot() {
		return source_port_not;
	}

	public void setSourcePortNot(Boolean source_port_not) {
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
	
	public Boolean isDestinationPortNot() {
		return destination_port_not;
	}

	public void setDestinationPortNot(Boolean destination_port_not) {
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

	public Boolean isDestinationIpAddressesNot() {
		return destination_ip_addresses_not;
	}

	public void setDestinationIpAddressesNot(Boolean destination_ip_addresses_not) {
		this.destination_ip_addresses_not = destination_ip_addresses_not;
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
				+ ((destination_ip_addresses_not == null) ? 0
						: destination_ip_addresses_not.hashCode());
		result = prime
				* result
				+ ((destination_port_max == null) ? 0 : destination_port_max
						.hashCode());
		result = prime
				* result
				+ ((destination_port_min == null) ? 0 : destination_port_min
						.hashCode());
		result = prime
				* result
				+ ((destination_port_not == null) ? 0 : destination_port_not
						.hashCode());
		result = prime * result
				+ ((ethertype == null) ? 0 : ethertype.hashCode());
		result = prime * result
				+ ((protocol == null) ? 0 : protocol.hashCode());
		result = prime
				* result
				+ ((source_ip_addresses == null) ? 0 : source_ip_addresses
						.hashCode());
		result = prime
				* result
				+ ((source_ip_addresses_not == null) ? 0
						: source_ip_addresses_not.hashCode());
		result = prime * result
				+ ((source_port_max == null) ? 0 : source_port_max.hashCode());
		result = prime * result
				+ ((source_port_min == null) ? 0 : source_port_min.hashCode());
		result = prime * result
				+ ((source_port_not == null) ? 0 : source_port_not.hashCode());
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
		if (destination_ip_addresses_not == null) {
			if (other.destination_ip_addresses_not != null)
				return false;
		} else if (!destination_ip_addresses_not
				.equals(other.destination_ip_addresses_not))
			return false;
		if (destination_port_max == null) {
			if (other.destination_port_max != null)
				return false;
		} else if (!destination_port_max.equals(other.destination_port_max))
			return false;
		if (destination_port_min == null) {
			if (other.destination_port_min != null)
				return false;
		} else if (!destination_port_min.equals(other.destination_port_min))
			return false;
		if (destination_port_not == null) {
			if (other.destination_port_not != null)
				return false;
		} else if (!destination_port_not.equals(other.destination_port_not))
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
		if (source_ip_addresses_not == null) {
			if (other.source_ip_addresses_not != null)
				return false;
		} else if (!source_ip_addresses_not
				.equals(other.source_ip_addresses_not))
			return false;
		if (source_port_max == null) {
			if (other.source_port_max != null)
				return false;
		} else if (!source_port_max.equals(other.source_port_max))
			return false;
		if (source_port_min == null) {
			if (other.source_port_min != null)
				return false;
		} else if (!source_port_min.equals(other.source_port_min))
			return false;
		if (source_port_not == null) {
			if (other.source_port_not != null)
				return false;
		} else if (!source_port_not.equals(other.source_port_not))
			return false;
		return true;
	}
	
	
}
