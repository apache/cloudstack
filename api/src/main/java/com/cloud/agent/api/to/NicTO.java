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

import com.cloud.offering.NetworkOffering;

import java.util.List;
import java.util.Map;

public class NicTO extends NetworkTO {
    int deviceId;
    Integer networkRateMbps;
    Integer networkRateMulticastMbps;
    boolean defaultNic;
    boolean pxeDisable;
    String nicUuid;
    List<String> nicSecIps;
    Map<NetworkOffering.Detail, String> details;
    boolean dpdkEnabled;
    Integer mtu;
    Long networkId;

    String networkSegmentName;

    public NicTO() {
        super();
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public Integer getNetworkRateMbps() {
        return networkRateMbps;
    }

    public void setNetworkRateMbps(Integer networkRateMbps) {
        this.networkRateMbps = networkRateMbps;
    }

    public Integer getNetworkRateMulticastMbps() {
        return networkRateMulticastMbps;
    }

    public boolean isDefaultNic() {
        return defaultNic;
    }

    public void setDefaultNic(boolean defaultNic) {
        this.defaultNic = defaultNic;
    }

    public void setPxeDisable(boolean pxeDisable) {
        this.pxeDisable = pxeDisable;
    }

    public boolean getPxeDisable() {
        return pxeDisable;
    }

    @Override
    public String getUuid() {
        return nicUuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.nicUuid = uuid;
    }

    @Override
    public String toString() {
        return new StringBuilder("[Nic:").append(type).append("-").append(ip).append("-").append(broadcastUri).append("]").toString();
    }

    public void setNicSecIps(List<String> secIps) {
        this.nicSecIps = secIps;
    }

    public List<String> getNicSecIps() {
        return nicSecIps;
    }

    public String getNetworkUuid() {
        return super.getUuid();
    }

    public void setNetworkUuid(String uuid) {
        super.setUuid(uuid);
    }

    public Map<NetworkOffering.Detail, String> getDetails() {
        return details;
    }

    public void setDetails(final Map<NetworkOffering.Detail, String> details) {
        this.details = details;
    }

    public boolean isDpdkEnabled() {
        return dpdkEnabled;
    }

    public void setDpdkEnabled(boolean dpdkEnabled) {
        this.dpdkEnabled = dpdkEnabled;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNetworkSegmentName() {
        return networkSegmentName;
    }

    public void setNetworkSegmentName(String networkSegmentName) {
        this.networkSegmentName = networkSegmentName;
    }
}
