//
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
//

package com.cloud.agent.resource.virtualnetwork.model;


public class IpAddress {
    private String publicIp;
    private boolean sourceNat;
    private boolean add;
    private boolean oneToOneNat;
    private boolean firstIP;
    private String gateway;
    private String netmask;
    private String vifMacAddress;
    private Integer nicDevId;
    private boolean newNic;

    public IpAddress() {
        // Empty constructor for (de)serialization
    }

    public IpAddress(String publicIp, boolean sourceNat, boolean add, boolean oneToOneNat, boolean firstIP, String gateway, String netmask, String vifMacAddress,
            Integer nicDevId, boolean newNic) {
        super();
        this.publicIp = publicIp;
        this.sourceNat = sourceNat;
        this.add = add;
        this.oneToOneNat = oneToOneNat;
        this.firstIP = firstIP;
        this.gateway = gateway;
        this.netmask = netmask;
        this.vifMacAddress = vifMacAddress;
        this.nicDevId = nicDevId;
        this.newNic = newNic;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public boolean isSourceNat() {
        return sourceNat;
    }

    public void setSourceNat(boolean sourceNat) {
        this.sourceNat = sourceNat;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public boolean isOneToOneNat() {
        return oneToOneNat;
    }

    public void setOneToOneNat(boolean oneToOneNat) {
        this.oneToOneNat = oneToOneNat;
    }

    public boolean isFirstIP() {
        return firstIP;
    }

    public void setFirstIP(boolean firstIP) {
        this.firstIP = firstIP;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getVifMacAddress() {
        return vifMacAddress;
    }

    public void setVifMacAddress(String vifMacAddress) {
        this.vifMacAddress = vifMacAddress;
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

}
