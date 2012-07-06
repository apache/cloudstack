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

public class CloudStackNic {
	
    @SerializedName(ApiConstants.ID)
    private String id;

    @SerializedName(ApiConstants.BROADCAST_URI)
    private String broadcastUri;
    
    @SerializedName(ApiConstants.GATEWAY)
    private String gateway;
    
    @SerializedName(ApiConstants.IP_ADDRESS)
    private String ipaddress;
    
    @SerializedName(ApiConstants.IS_DEFAULT)
    private Boolean isDefault;

    @SerializedName(ApiConstants.ISOLATION_URI)
    private String isolationUri;
    
    @SerializedName(ApiConstants.MAC_ADDRESS)
    private String macAddress;
    
    @SerializedName(ApiConstants.NETMASK)
    private String netmask;
    
    @SerializedName(ApiConstants.NETWORK_ID)
    private String networkid;
    
    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    private String trafficType;
    
    @SerializedName(ApiConstants.TYPE) 
    private String type;
    
    public CloudStackNic() {
    }

    public String getId() {
        return id;
    }

	public String getNetworkid() {
		return networkid;
	}

	public String getNetmask() {
		return netmask;
	}

	public String getGateway() {
		return gateway;
	}

	public String getIpaddress() {
		return ipaddress;
	}

	public String getIsolationUri() {
		return isolationUri;
	}

	public String getBroadcastUri() {
		return broadcastUri;
	}

	public String getTrafficType() {
		return trafficType;
	}

	public String getType() {
		return type;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	/**
	 * @return the macAddress
	 */
	public String getMacAddress() {
		return macAddress;
	}
}
