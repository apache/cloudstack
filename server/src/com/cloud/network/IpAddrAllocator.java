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
package com.cloud.network;

import com.cloud.utils.component.Adapter;

public interface IpAddrAllocator extends Adapter {
    public class IpAddr {
        public String ipaddr;
        public String netMask;
        public String gateway;

        public IpAddr(String ipaddr, String netMask, String gateway) {
            this.ipaddr = ipaddr;
            this.netMask = netMask;
            this.gateway = gateway;
        }

        public IpAddr() {
            this.ipaddr = null;
            this.netMask = null;
            this.gateway = null;
        }
    }

    public class NetworkInfo {
        public String _ipAddr;
        public String _netMask;
        public String _gateWay;
        public Long _vlanDbId;
        public String _vlanid;

        public NetworkInfo(String ip, String netMask, String gateway, Long vlanDbId, String vlanId) {
            _ipAddr = ip;
            _netMask = netMask;
            _gateWay = gateway;
            _vlanDbId = vlanDbId;
            _vlanid = vlanId;
        }
    }

    public IpAddr getPublicIpAddress(String macAddr, long dcId, long podId);

    public IpAddr getPrivateIpAddress(String macAddr, long dcId, long podId);

    public boolean releasePublicIpAddress(String ip, long dcId, long podId);

    public boolean releasePrivateIpAddress(String ip, long dcId, long podId);

    public boolean externalIpAddressAllocatorEnabled();
}
