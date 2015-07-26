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

package com.cloud.utils.net;

public class Ip4Address {
    String _addr;
    String _mac;
    static final String s_empty_mac = "00:00:00:00:00:00";

    public Ip4Address(String addr, String mac) {
        _addr = addr;
        _mac = mac;
    }

    public Ip4Address(long addr, long mac) {
        _addr = NetUtils.long2Ip(addr);
        _mac = NetUtils.long2Mac(mac);
    }

    public Ip4Address(String addr) {
        this(addr, s_empty_mac);
    }

    public Ip4Address(long addr) {
        this(NetUtils.long2Ip(addr), s_empty_mac);
    }

    public String ip4() {
        return _addr;
    }

    public String mac() {
        return _mac;
    }

    public long toLong() {
        return NetUtils.ip2Long(_addr);
    }

    @Override
    public boolean equals(Object that) {

        if (that instanceof Ip4Address) {
            Ip4Address ip4 = (Ip4Address)that;
            return _addr.equals(ip4._addr) && (_mac == ip4._mac || _mac.equals(ip4._mac));
        } else {
            return false;
        }
    }

    public boolean isSameAddressAs(Object other) {
        if (other instanceof String) { // Assume that is an ip4 address in String form
            return _addr.equals(other);
        } else {
            return equals(other);
        }
    }

    @Override
    public int hashCode(){
        return _mac.hashCode()*_addr.hashCode();
    }
}
