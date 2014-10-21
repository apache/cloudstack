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

public class CloudStackPortForwardingRule {
    @SerializedName(ApiConstants.ID)
    private String id;
    @SerializedName(ApiConstants.CIDR_LIST)
    private String cidrList;
    @SerializedName(ApiConstants.IP_ADDRESS)
    private String ipAddress;
    @SerializedName(ApiConstants.IP_ADDRESS_ID)
    private String ipAddressId;
    @SerializedName(ApiConstants.PRIVATE_END_PORT)
    private Long privateEndPort;
    @SerializedName(ApiConstants.PRIVATE_PORT)
    private Long privatePort;
    @SerializedName(ApiConstants.PROTOCOL)
    private String protocol;
    @SerializedName(ApiConstants.PUBLIC_END_PORT)
    private Long publicEndPort;
    @SerializedName(ApiConstants.PUBLIC_PORT)
    private Long publicPort;
    @SerializedName(ApiConstants.STATE)
    private String state;
    @SerializedName(ApiConstants.VIRTUAL_MACHINE_DISPLAY_NAME)
    private String virtualMachineDisplayName;
    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    private String virtualMachineId;
    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    private String virtualMachineName;

    /**
     *
     */
    public CloudStackPortForwardingRule() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the cidrList
     */
    public String getCidrList() {
        return cidrList;
    }

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return the ipAddressId
     */
    public String getIpAddressId() {
        return ipAddressId;
    }

    /**
     * @return the privateEndPort
     */
    public Long getPrivateEndPort() {
        return privateEndPort;
    }

    /**
     * @return the privatePort
     */
    public Long getPrivatePort() {
        return privatePort;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the publicEndPort
     */
    public Long getPublicEndPort() {
        return publicEndPort;
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
     * @return the virtualMachineDisplayName
     */
    public String getVirtualMachineDisplayName() {
        return virtualMachineDisplayName;
    }

    /**
     * @return the virtualMachineId
     */
    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    /**
     * @return the virtualMachineName
     */
    public String getVirtualMachineName() {
        return virtualMachineName;
    }

}
