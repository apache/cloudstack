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
package com.cloud.utils.net;

public class Ip4Address {
    String _addr;
    String _mac;

    public Ip4Address(String addr, String mac) {
        _addr = addr;
        _mac = mac;
    }

    public Ip4Address(long addr, long mac) {
        _addr = NetUtils.long2Ip(addr);
        _mac = NetUtils.long2Mac(mac);
    }

    public Ip4Address(String addr) {
        this(addr, null);
    }

    public Ip4Address(long addr) {
        this(NetUtils.long2Ip(addr), null);
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
        if (that instanceof String) { // Assume that is an ip4 address in String form 
            return _addr.equals(that);
        } else if (that instanceof Ip4Address) {
            Ip4Address ip4 = (Ip4Address)that;
            return this._addr.equals(ip4._addr) && (this._mac == ip4._mac || this._mac.equals(ip4._mac));
        } else {
            return false;
        }
    }
}
