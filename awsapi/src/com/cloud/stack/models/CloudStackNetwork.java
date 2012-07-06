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

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CloudStackNetwork {
	@SerializedName(ApiConstants.ID)
	private String id;
    @SerializedName(ApiConstants.ACCOUNT)
    private String account;
	@SerializedName(ApiConstants.BROADCAST_DOMAIN_TYPE)
	private String broadcastDomainType;
	@SerializedName(ApiConstants.BROADCAST_URI)
	private String broadcastURI;
	@SerializedName(ApiConstants.DISPLAY_TEXT)
	private String displaytext;
    @SerializedName(ApiConstants.DNS1)
    private String dns1;
    @SerializedName(ApiConstants.DNS2)
    private String dns2;
    @SerializedName(ApiConstants.DOMAIN)
    private String domain;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private String domainId;
    @SerializedName(ApiConstants.END_IP)
    private String endIp;
    @SerializedName(ApiConstants.GATEWAY)
    private String gateway;
    @SerializedName(ApiConstants.IS_DEFAULT)
    private Boolean isDefault;
    @SerializedName(ApiConstants.IS_SHARED)
    private Boolean isShared;
    @SerializedName(ApiConstants.IS_SYSTEM)
    private Boolean isSystem;
   	@SerializedName(ApiConstants.NAME)
	private String name;
   	@SerializedName(ApiConstants.NETMASK)
   	private String netmask;
    @SerializedName(ApiConstants.NETWORK_DOMAIN)
    private String networkDomain;
    @SerializedName(ApiConstants.NETWORK_OFFERING_AVAILABILITY)
    private String networkOfferingAvailability;
	@SerializedName(ApiConstants.NETWORK_OFFERING_DISPLAY_TEXT)
	private String networkOfferingDisplayText;
	@SerializedName(ApiConstants.NETWORK_OFFERING_ID)
	private String networkOfferingId;	
	@SerializedName(ApiConstants.NETWORK_OFFERING_NAME)
	private String networkOfferingName;	
    @SerializedName(ApiConstants.RELATED)
    private String related;
    @SerializedName(ApiConstants.SECURITY_GROUP_ENABLED)
    private Boolean securityGroupEnabled;
    @SerializedName(ApiConstants.START_IP)
    private String startIp;
    @SerializedName(ApiConstants.STATE)
    private String state;
    @SerializedName(ApiConstants.TAGS)
    private String tags;
   	@SerializedName(ApiConstants.TRAFFIC_TYPE)
	private String trafficType;
    @SerializedName(ApiConstants.TYPE)
    private String type;
    @SerializedName(ApiConstants.VLAN)
    private String vlan;
	@SerializedName(ApiConstants.ZONE_ID)
	private String zoneId;	
    @SerializedName(ApiConstants.SERVICE)
    private List<CloudStackNetworkService> services;

	/**
	 * 
	 */
	public CloudStackNetwork() {
		// TODO Auto-generated constructor stub
	}



	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}



	/**
	 * @return the broadcastDomainType
	 */
	public String getBroadcastDomainType() {
		return broadcastDomainType;
	}



	/**
	 * @return the broadcastURI
	 */
	public String getBroadcastURI() {
		return broadcastURI;
	}



	/**
	 * @return the displaytext
	 */
	public String getDisplaytext() {
		return displaytext;
	}



	/**
	 * @return the dns1
	 */
	public String getDns1() {
		return dns1;
	}



	/**
	 * @return the dns2
	 */
	public String getDns2() {
		return dns2;
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
	 * @return the endIp
	 */
	public String getEndIp() {
		return endIp;
	}



	/**
	 * @return the gateway
	 */
	public String getGateway() {
		return gateway;
	}



	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}



	/**
	 * @return the isDefault
	 */
	public Boolean getIsDefault() {
		return isDefault;
	}



	/**
	 * @return the isShared
	 */
	public Boolean getIsShared() {
		return isShared;
	}



	/**
	 * @return the isSystem
	 */
	public Boolean getIsSystem() {
		return isSystem;
	}



	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}



	/**
	 * @return the netmask
	 */
	public String getNetmask() {
		return netmask;
	}



	/**
	 * @return the networkDomain
	 */
	public String getNetworkDomain() {
		return networkDomain;
	}



	/**
	 * @return the networkOfferingAvailability
	 */
	public String getNetworkOfferingAvailability() {
		return networkOfferingAvailability;
	}



	/**
	 * @return the networkOfferingDisplayText
	 */
	public String getNetworkOfferingDisplayText() {
		return networkOfferingDisplayText;
	}



	/**
	 * @return the networkOfferingId
	 */
	public String getNetworkOfferingId() {
		return networkOfferingId;
	}



	/**
	 * @return the networkOfferingName
	 */
	public String getNetworkOfferingName() {
		return networkOfferingName;
	}



	/**
	 * @return the related
	 */
	public String getRelated() {
		return related;
	}



	/**
	 * @return the securityGroupEnabled
	 */
	public Boolean getSecurityGroupEnabled() {
		return securityGroupEnabled;
	}



	/**
	 * @return the services
	 */
	public List<CloudStackNetworkService> getServices() {
		return services;
	}



	/**
	 * @return the startIp
	 */
	public String getStartIp() {
		return startIp;
	}



	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}



	/**
	 * @return the tags
	 */
	public String getTags() {
		return tags;
	}



	/**
	 * @return the trafficType
	 */
	public String getTrafficType() {
		return trafficType;
	}



	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}



	/**
	 * @return the vlan
	 */
	public String getVlan() {
		return vlan;
	}



	/**
	 * @return the zoneId
	 */
	public String getZoneId() {
		return zoneId;
	}
}
