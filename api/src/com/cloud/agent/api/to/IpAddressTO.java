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
package com.cloud.agent.api.to;

import com.cloud.network.Networks.TrafficType;

public class IpAddressTO {

    private Long accountId;
    private String publicIp;
    private Boolean sourceNat;
    private Boolean add;
    private Boolean oneToOneNat;
    private Boolean firstIP;
    private String broadcastUri;
    private String vlanGateway;
    private String vlanNetmask;
    private String vifMacAddress;
    private Integer networkRate;
    private TrafficType trafficType;
    private String networkName;
    private Integer nicDevId;
    private Boolean newNic;

    public IpAddressTO(Long accountId, String ipAddress, Boolean add, Boolean firstIP, Boolean sourceNat, String broadcastUri, String vlanGateway, String vlanNetmask,
            String vifMacAddress, Integer networkRate, Boolean isOneToOneNat) {
        this.accountId = accountId;
        this.publicIp = ipAddress;
        this.add = add;
        this.firstIP = firstIP;
        this.sourceNat = sourceNat;
        this.broadcastUri = broadcastUri;
        this.vlanGateway = vlanGateway;
        this.vlanNetmask = vlanNetmask;
        this.vifMacAddress = vifMacAddress;
        this.networkRate = networkRate;
        this.oneToOneNat = isOneToOneNat;
        setNicDevId(0);
        setNewNic(false);
    }

    protected IpAddressTO() {
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public TrafficType getTrafficType() {
        return trafficType;
    }

    public void setNetworkName(String name) {
        this.networkName = name;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }

    public Boolean isAdd() {
        return add;
    }

    public Boolean isOneToOneNat() {
        return this.oneToOneNat;
    }

    public Boolean isFirstIP() {
        return firstIP;
    }

    public void setSourceNat(Boolean sourceNat) {
        this.sourceNat = sourceNat;
    }

    public Boolean isSourceNat() {
        return sourceNat;
    }

    public String getBroadcastUri() {
        return broadcastUri;
    }

    public String getVlanGateway() {
        return vlanGateway;
    }

    public String getVlanNetmask() {
        return vlanNetmask;
    }

    public String getVifMacAddress() {
        return vifMacAddress;
    }

    public Integer getNetworkRate() {
        return networkRate;
    }

    public Integer getNicDevId() {
        return nicDevId;
    }

    public void setNicDevId(Integer nicDevId) {
        this.nicDevId = nicDevId;
    }

    public Boolean isNewNic() {
        return newNic;
    }

    public void setNewNic(Boolean newNic) {
        this.newNic = newNic;
    }
}
