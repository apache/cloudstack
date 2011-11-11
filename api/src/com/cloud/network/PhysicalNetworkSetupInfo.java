/**
 *  Copyright (C) 2010 VMOps, Inc.  All rights reserved.
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
package com.cloud.network;


/**
 *  PhysicalNetworkNames provides the labels to identify per traffic type
 *  the physical networks available to the host .
 */
public class PhysicalNetworkSetupInfo {
    
    //physical network ID as seen by Mgmt server
    Long physicalNetworkId; 
    String privateNetworkName;
    String publicNetworkName;
    String guestNetworkName;
    String storageNetworkName;
    //this is used by VmWare to identify the vlan to use for management traffic
    String mgmtVlan;
    
    public PhysicalNetworkSetupInfo(){
    }

    public String getPrivateNetworkName() {
        return privateNetworkName;
    }

    public String getPublicNetworkName() {
        return publicNetworkName;
    }

    public String getGuestNetworkName() {
        return guestNetworkName;
    }

    public String getStorageNetworkName() {
        return storageNetworkName;
    }
    
    public void setPrivateNetworkName(String privateNetworkName) {
        this.privateNetworkName = privateNetworkName;
    }

    public void setPublicNetworkName(String publicNetworkName) {
        this.publicNetworkName = publicNetworkName;
    }

    public void setGuestNetworkName(String guestNetworkName) {
        this.guestNetworkName = guestNetworkName;
    }

    public void setStorageNetworkName(String storageNetworkName) {
        this.storageNetworkName = storageNetworkName;
    }    
    
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public String getMgmtVlan() {
        return mgmtVlan;
    }

    public void setMgmtVlan(String mgmtVlan) {
        this.mgmtVlan = mgmtVlan;
    }


}
