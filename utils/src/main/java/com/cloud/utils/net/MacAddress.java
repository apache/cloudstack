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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Formatter;

/**
 * This class retrieves the (first) MAC address for the machine is it is loaded on and stores it statically for retrieval.
 * It can also be used for formatting MAC addresses.
 **/
public class MacAddress {
    private long _addr = 0;

    protected MacAddress() {
    }

    public MacAddress(long addr) {
        _addr = addr;
    }

    public long toLong() {
        return _addr;
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[6];
        bytes[0] = (byte)((_addr >> 40) & 0xff);
        bytes[1] = (byte)((_addr >> 32) & 0xff);
        bytes[2] = (byte)((_addr >> 24) & 0xff);
        bytes[3] = (byte)((_addr >> 16) & 0xff);
        bytes[4] = (byte)((_addr >> 8) & 0xff);
        bytes[5] = (byte)((_addr >> 0) & 0xff);
        return bytes;
    }

    public String toString(String separator) {
        StringBuilder buff = new StringBuilder();
        Formatter formatter = new Formatter(buff);
        formatter.format("%02x%s%02x%s%02x%s%02x%s%02x%s%02x", _addr >> 40 & 0xff, separator, _addr >> 32 & 0xff, separator, _addr >> 24 & 0xff, separator,
            _addr >> 16 & 0xff, separator, _addr >> 8 & 0xff, separator, _addr & 0xff);
        return buff.toString();
    }

    @Override
    public String toString() {
        return toString(":");
    }

    private static MacAddress s_address;
    static {
        String macAddress = null;

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                 NetworkInterface network = networkInterfaces.nextElement();
                 final byte [] mac = network.getHardwareAddress();
                 if (mac != null && !network.isVirtual() &&
                         !network.getName().startsWith("br-") &&
                         !network.getName().startsWith("veth") &&
                         !network.getName().startsWith("vnet")) {
                     StringBuilder macAddressBuilder = new StringBuilder();
                     for (byte b : mac) {
                         macAddressBuilder.append(String.format("%02X", b));
                     }
                     macAddress = macAddressBuilder.toString();
                 }
             }
        } catch (SocketException ignore) {
        }

        long macAddressInLong = 0;

        if (macAddress != null) {
            macAddressInLong = Long.parseLong(macAddress, 16);
        } else {
            try {
                byte[] local = InetAddress.getLocalHost().getAddress();
                macAddressInLong |= (local[0] << 24) & 0xFF000000L;
                macAddressInLong |= (local[1] << 16) & 0xFF0000;
                macAddressInLong |= (local[2] << 8) & 0xFF00;
                macAddressInLong |= local[3] & 0xFF;
            } catch (UnknownHostException ex) {
                macAddressInLong |= (long)(Math.random() * 0x7FFFFFFF);
            }
        }

        s_address = new MacAddress(macAddressInLong);
    }

    public static MacAddress getMacAddress() {
        return s_address;
    }
}
