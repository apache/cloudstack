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

package com.cloud.stack.models;

import com.google.gson.annotations.SerializedName;

public class CloudStackLoadBalancerRule {
	@SerializedName(ApiConstants.ID)
	private String id;
	@SerializedName(ApiConstants.ACCOUNT)
	private String account;
	@SerializedName(ApiConstants.ALGORITHM)
	private String algorithm;  // source, roundrobin, leastconn
	@SerializedName(ApiConstants.DESCRIPTION)
	private String description;
	@SerializedName(ApiConstants.DOMAIN)
	private String domain;
	@SerializedName(ApiConstants.DOMAIN_ID)
	private String domainId;
	@SerializedName(ApiConstants.NAME)
	private String name;
	@SerializedName(ApiConstants.PRIVATE_PORT)
	private String privatePort;
	@SerializedName(ApiConstants.PUBLIC_IP)
	private String publicIp;
	@SerializedName(ApiConstants.PUBLIC_IP_ID)
	private String publicIpId;
	@SerializedName(ApiConstants.PUBLIC_PORT)
	private Long publicPort;
	@SerializedName(ApiConstants.STATE)
	private String state;
	@SerializedName(ApiConstants.ZONE_ID)
	private String zoneId;
	
	/**
	 * 
	 */
	public CloudStackLoadBalancerRule() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @return the algorithm
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @return the domainId
	 */
	public String getDomainId() {
		return domainId;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the privatePort
	 */
	public String getPrivatePort() {
		return privatePort;
	}

	/**
	 * @return the publicIp
	 */
	public String getPublicIp() {
		return publicIp;
	}

	/**
	 * @return the publicIpId
	 */
	public String getPublicIpId() {
		return publicIpId;
	}

	/**
	 * @return the publicPort
	 */
	public Long getPublicPort() {
		return publicPort;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @return the zoneId
	 */
	public String getZoneId() {
		return zoneId;
	}

}
