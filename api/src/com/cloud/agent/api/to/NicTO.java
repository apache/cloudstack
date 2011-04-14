/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

/**
 * 
 */
package com.cloud.agent.api.to;

public class NicTO extends NetworkTO {
    int deviceId;
    Integer networkRateMbps;
    Integer networkRateMulticastMbps;
    boolean defaultNic;


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
    
    @Override
    public String toString() {
        return new StringBuilder("[Nic:").append(type).append("-").append(ip).append("-").append(broadcastUri).append("]").toString();
    }
}
