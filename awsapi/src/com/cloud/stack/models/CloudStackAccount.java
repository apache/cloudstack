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

public class CloudStackAccount {
	@SerializedName(ApiConstants.ID)
	private String id;
	@SerializedName(ApiConstants.NAME)
	private String name;
	@SerializedName(ApiConstants.ACCOUNT_TYPE)
	private Long accountType;
	@SerializedName(ApiConstants.DOMAIN_ID)
	private String domainId;
	@SerializedName(ApiConstants.DOMAIN)
	private String domain;
	@SerializedName(ApiConstants.RECEIVED_BYTES)
	private Long receivedBytes;
	@SerializedName(ApiConstants.SENT_BYTES)
	private Long sentBytes;
	@SerializedName(ApiConstants.VM_LIMIT)
	private String vmLimit;
	@SerializedName(ApiConstants.VM_TOTAL)
	private Long vmTotal;
	@SerializedName(ApiConstants.VM_AVAILABLE)
	private String vmAvailable;
	@SerializedName(ApiConstants.IP_LIMIT)
	private String ipLimit;
	@SerializedName(ApiConstants.IP_TOTAL)
	private Long ipTotal;
	@SerializedName(ApiConstants.IP_AVAILABLE)
	private String ipAvailable;
	@SerializedName(ApiConstants.VOLUME_LIMIT)
	private String volumeLimit;
	@SerializedName(ApiConstants.VOLUME_TOTAL)
	private Long volumeTotal;
	@SerializedName(ApiConstants.VOLUME_AVAILABLE)
	private String volumeAvailable;
	@SerializedName(ApiConstants.SNAPSHOT_LIMIT)
	private String snapShotLimit;
	@SerializedName(ApiConstants.SNAPSHOT_TOTAL)
	private Long snapShotTotal;
	@SerializedName(ApiConstants.SNAPSHOT_AVAILABLE)
	private String snapShotAvailable;
	@SerializedName(ApiConstants.TEMPLATE_LIMIT)
	private String templateLimit;
	@SerializedName(ApiConstants.TEMPLATE_TOTAL)
	private String templateTotal;
	@SerializedName(ApiConstants.TEMPLATE_AVAILABLE)
	private String templateAvailable;
	@SerializedName(ApiConstants.VM_STOPPED)
	private Long vmStopped;
	@SerializedName(ApiConstants.VM_RUNNING)
	private Long vmRunning;
	@SerializedName(ApiConstants.ENABLED)
	private String enabled;
	@SerializedName(ApiConstants.USER)
	private CloudStackUser[] user;
	@SerializedName(ApiConstants.DEFAULT_ZONE_ID)     
	private String defaultZoneId;
	
	/**
	 * 
	 */
	public CloudStackAccount() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
    }
	
    public void setId(String id) {
        this.id = id;
    }

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
    }
	
    public void setName(String name) {
        this.name = name;
    }

	/**
	 * @return the accountType
	 */
	public Long getAccountType() {
		return accountType;
	}

	/**
	 * @return the domainId
	 */
	public String getDomainId() {
		return domainId;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @return the receivedBytes
	 */
	public Long getReceivedBytes() {
		return receivedBytes;
	}

	/**
	 * @return the sentBytes
	 */
	public Long getSentBytes() {
		return sentBytes;
	}

	/**
	 * @return the vmLimit
	 */
	public String getVmLimit() {
		return vmLimit;
	}

	/**
	 * @return the vmTotal
	 */
	public Long getVmTotal() {
		return vmTotal;
	}

	/**
	 * @return the vmAvailable
	 */
	public String getVmAvailable() {
		return vmAvailable;
	}

	/**
	 * @return the ipLimit
	 */
	public String getIpLimit() {
		return ipLimit;
	}

	/**
	 * @return the ipTotal
	 */
	public Long getIpTotal() {
		return ipTotal;
	}

	/**
	 * @return the ipAvailable
	 */
	public String getIpAvailable() {
		return ipAvailable;
	}

	/**
	 * @return the volumeLimit
	 */
	public String getVolumeLimit() {
		return volumeLimit;
	}

	/**
	 * @return the volumeTotal
	 */
	public Long getVolumeTotal() {
		return volumeTotal;
	}

	/**
	 * @return the volumeAvailable
	 */
	public String getVolumeAvailable() {
		return volumeAvailable;
	}

	/**
	 * @return the snapShotLimit
	 */
	public String getSnapShotLimit() {
		return snapShotLimit;
	}

	/**
	 * @return the snapShotTotal
	 */
	public Long getSnapShotTotal() {
		return snapShotTotal;
	}

	/**
	 * @return the snapShotAvailable
	 */
	public String getSnapShotAvailable() {
		return snapShotAvailable;
	}

	/**
	 * @return the templateLimit
	 */
	public String getTemplateLimit() {
		return templateLimit;
	}

	/**
	 * @return the templateTotal
	 */
	public String getTemplateTotal() {
		return templateTotal;
	}

	/**
	 * @return the templateAvailable
	 */
	public String getTemplateAvailable() {
		return templateAvailable;
	}

	/**
	 * @return the vmStopped
	 */
	public Long getVmStopped() {
		return vmStopped;
	}

	/**
	 * @return the vmRunning
	 */
	public Long getVmRunning() {
		return vmRunning;
	}

	/**
	 * @return the enabled
	 */
	public String getEnabled() {
		return enabled;
	}

	/**
	 * @return the user
	 */
	public CloudStackUser[] getUser() {
		return user;
	}
	
    /**        
     * @return the defaultZoneId
     */
    public String getDefaultZoneId() {
        return defaultZoneId;
    }
	
    public void setdefaultZoneId(String defaultZoneId) {
        this.defaultZoneId = defaultZoneId;
    }
}
