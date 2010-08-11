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
package com.cloud.agent.api.to;

import com.cloud.host.HostVO;

public class HostTO {
    private String guid;
    private NetworkTO privateNetwork;
    private NetworkTO publicNetwork;
    private NetworkTO storageNetwork1;
    private NetworkTO storageNetwork2;
    
    protected HostTO() {
    }
    
    public HostTO(HostVO vo) {
        guid = vo.getGuid();
        privateNetwork = new NetworkTO(vo.getPrivateIpAddress(), vo.getPrivateNetmask(), vo.getPrivateMacAddress());
        if (vo.getPublicIpAddress() != null) {
            publicNetwork = new NetworkTO(vo.getPublicIpAddress(), vo.getPublicNetmask(), vo.getPublicMacAddress());
        }
        if (vo.getStorageIpAddress() != null) {
            storageNetwork1 = new NetworkTO(vo.getStorageIpAddress(), vo.getStorageNetmask(), vo.getStorageMacAddress());
        }
        if (vo.getStorageIpAddressDeux() != null) {
            storageNetwork2 = new NetworkTO(vo.getStorageIpAddressDeux(), vo.getStorageNetmaskDeux(), vo.getStorageMacAddressDeux());
        }
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public NetworkTO getPrivateNetwork() {
        return privateNetwork;
    }

    public void setPrivateNetwork(NetworkTO privateNetwork) {
        this.privateNetwork = privateNetwork;
    }

    public NetworkTO getPublicNetwork() {
        return publicNetwork;
    }

    public void setPublicNetwork(NetworkTO publicNetwork) {
        this.publicNetwork = publicNetwork;
    }

    public NetworkTO getStorageNetwork1() {
        return storageNetwork1;
    }

    public void setStorageNetwork1(NetworkTO storageNetwork1) {
        this.storageNetwork1 = storageNetwork1;
    }

    public NetworkTO getStorageNetwork2() {
        return storageNetwork2;
    }

    public void setStorageNetwork2(NetworkTO storageNetwork2) {
        this.storageNetwork2 = storageNetwork2;
    }
}
