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

import com.cloud.host.Host;

public class HostTO {
    private String guid;
    private NetworkTO privateNetwork;
    private NetworkTO publicNetwork;
    private NetworkTO storageNetwork1;
    private NetworkTO storageNetwork2;

    protected HostTO() {
    }

    public HostTO(Host vo) {
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
