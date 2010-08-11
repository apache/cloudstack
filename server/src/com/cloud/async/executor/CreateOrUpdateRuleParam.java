/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.async.executor;

public class CreateOrUpdateRuleParam {
	private boolean isForwarding;
	private long userId;
	private Long accountId;
	private String address;
	private String port;
	private String privateIpAddress;
	private String privatePort;
	private String protocol;
	private String algorithm;
	private Long domainId;
	private Long securityGroupId;

	public CreateOrUpdateRuleParam() {
	}
	
	public CreateOrUpdateRuleParam(boolean isForwarding, long userId, Long accountId, String address, 
		String port, String privateIpAddress, String privatePort, String protocol, String algorithm, Long domainId, Long securityGroupId) {
		this.isForwarding = isForwarding;
		this.userId = userId;
		this.accountId = accountId;
		this.address = address;
		this.port = port;
		this.privateIpAddress = privateIpAddress;
		this.privatePort = privatePort;
		this.protocol = protocol;
		this.algorithm = algorithm;
		this.domainId = domainId;
		this.securityGroupId = securityGroupId;
	}
	
	public boolean isForwarding() {
		return isForwarding;
	}
	
	public void setForwarding(boolean isForwarding) {
		this.isForwarding = isForwarding;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
	}
	
	public Long getAccountId() {
		return accountId;
	}
	
	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getPort() {
		return port;
	}
	
	public void setPort(String port) {
		this.port = port;
	}
	
	public String getPrivateIpAddress() {
		return privateIpAddress;
	}
	
	public void setPrivateIpAddress(String privateIpAddress) {
		this.privateIpAddress = privateIpAddress;
	}
	
	public String getPrivatePort() {
		return privatePort;
	}
	
	public void setPrivatePort(String privatePort) {
		this.privatePort = privatePort;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public String getAlgorithm() {
		return algorithm;
	}
	
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	
	public Long getDomainId() {
		return domainId;
	}

	public void setDomainId(Long domainId) {
		this.domainId = domainId;
	}

	public Long getSecurityGroupId() {
		return securityGroupId;
	}

	public void setSecurityGroupId(Long securityGroupId) {
		this.securityGroupId = securityGroupId;
	}
	
}
