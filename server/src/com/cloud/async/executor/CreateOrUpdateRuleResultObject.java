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

import com.cloud.serializer.Param;

public class CreateOrUpdateRuleResultObject {
	
	@Param(name="id")
	private long ruleId;
	
	@Param(name="publicip")
	private String publicIp;
	
	@Param(name="publicport")
	private int publicPort;
	
	@Param(name="privateip")
	private String privateIp;
	
	@Param(name="privateport")
	private int privatePort;
	
	@Param(name="isenabled")
	private boolean isEnabled;
	
	@Param(name="protocol")
	private String protocol;
	
	@Param(name="algorithm")
	private String algorithm;
	
	@Param(name="portforwardingserviceid")
	private Long portForwardingServiceId;

	public Long getPortForwardingServiceId() {
		return portForwardingServiceId;
	}

	public void setPortForwardingServiceId(Long portForwardingServiceId) {
		this.portForwardingServiceId = portForwardingServiceId;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public long getRuleId() {
		return ruleId;
	}

	public void setRuleId(long ruleId) {
		this.ruleId = ruleId;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public int getPublicPort() {
		return publicPort;
	}

	public void setPublicPort(int publicPort) {
		this.publicPort = publicPort;
	}

	public String getPrivateIp() {
		return privateIp;
	}

	public void setPrivateIp(String privateIp) {
		this.privateIp = privateIp;
	}

	public int getPrivatePort() {
		return privatePort;
	}

	public void setPrivatePort(int privatePort) {
		this.privatePort = privatePort;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
}
