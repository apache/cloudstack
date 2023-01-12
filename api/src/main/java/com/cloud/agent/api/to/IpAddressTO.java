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

import java.util.Map;

public class IpAddressTO {

    private long accountId;
    private String publicIp;
    private boolean sourceNat;
    private boolean add;
    private boolean oneToOneNat;
    private boolean firstIP;
    private String broadcastUri;
    private String vlanGateway;
    private String vlanNetmask;
    private String vifMacAddress;
    private Integer networkRate;
    private TrafficType trafficType;
    private String networkName;
    private Integer nicDevId;
    private boolean newNic;
    private boolean isPrivateGateway;
    private NicTO nicTO;

    private Integer mtu;
    Map<String, String> details;

    public IpAddressTO(long accountId, String ipAddress, boolean add, boolean firstIP, boolean sourceNat, String broadcastUri, String vlanGateway, String vlanNetmask,
                       String vifMacAddress, Integer networkRate, boolean isOneToOneNat) {
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
    }

    public IpAddressTO(String ipAddress, Integer mtu, String vlanNetmask ) {
        this.publicIp = ipAddress;
        this.mtu = mtu;
        this.vlanNetmask = vlanNetmask;
    }

    protected IpAddressTO() {
    }

    public long getAccountId() {
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

    public boolean isAdd() {
        return add;
    }

    public boolean isOneToOneNat() {
        return this.oneToOneNat;
    }

    public boolean isFirstIP() {
        return firstIP;
    }

    public void setSourceNat(boolean sourceNat) {
        this.sourceNat = sourceNat;
    }

    public boolean isSourceNat() {
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

    public boolean isNewNic() {
        return newNic;
    }

    public void setNewNic(boolean newNic) {
        this.newNic = newNic;
    }

    public boolean isPrivateGateway() {
        return isPrivateGateway;
    }

    public void setPrivateGateway(boolean isPrivateGateway) {
        this.isPrivateGateway = isPrivateGateway;
    }

    public NicTO getNicTO() {
        return nicTO;
    }

    public void setNicTO(NicTO nicTO) {
        this.nicTO = nicTO;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }


    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
