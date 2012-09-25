package com.cloud.network.nicira;

public class DestinationNatRule extends NatRule {
	private Match match;
	private String to_destination_ip_address_min;
	private String to_destination_ip_address_max;
	private Integer to_destination_port;
	private String uuid;
	private String type = "DestinationNatRule";
	
	public Match getMatch() {
		return match;
	}
	
	public void setMatch(Match match) {
		this.match = match;
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
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
}
