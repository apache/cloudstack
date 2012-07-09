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

public class CloudStackNetworkOffering {
	@SerializedName(ApiConstants.ID)	
	private String id;
	@SerializedName(ApiConstants.AVAILABILITY)	
	private String availability;
	@SerializedName(ApiConstants.CREATED)	
	private String created;
	@SerializedName(ApiConstants.DISPLAY_TEXT)	
	private String displayText;
	@SerializedName(ApiConstants.GUEST_IP_TYPE)	
	private String guestIpType;
	@SerializedName(ApiConstants.IS_DEFAULT)	
	private Boolean isDefault;
	@SerializedName(ApiConstants.MAX_CONNECTIONS)	
	private Long maxconnections;
	@SerializedName(ApiConstants.NAME)	
	private String name;
	@SerializedName(ApiConstants.NETWORKRATE)	
	private Long networkRate;
	@SerializedName(ApiConstants.SPECIFY_VLAN)	
	private Boolean specifyVlan;
	@SerializedName(ApiConstants.TAGS)	
	private String tags;
	@SerializedName(ApiConstants.TRAFFIC_TYPE)	
	private String traffictype;
	
	/**
	 * 
	 */
	public CloudStackNetworkOffering() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the availability
	 */
	public String getAvailability() {
		return availability;
	}

	/**
	 * @return the created
	 */
	public String getCreated() {
		return created;
	}

	/**
	 * @return the displayText
	 */
	public String getDisplayText() {
		return displayText;
	}

	/**
	 * @return the guestIpType
	 */
	public String getGuestIpType() {
		return guestIpType;
	}

	/**
	 * @return the isDefault
	 */
	public Boolean getIsDefault() {
		return isDefault;
	}

	/**
	 * @return the maxconnections
	 */
	public Long getMaxconnections() {
		return maxconnections;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the networkRate
	 */
	public Long getNetworkRate() {
		return networkRate;
	}

	/**
	 * @return the specifyVlan
	 */
	public Boolean getSpecifyVlan() {
		return specifyVlan;
	}

	/**
	 * @return the tags
	 */
	public String getTags() {
		return tags;
	}

	/**
	 * @return the traffictype
	 */
	public String getTraffictype() {
		return traffictype;
	}

}
