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
import java.util.Collections;
import java.util.Formatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.List;

/**
 * This class retrieves the (first) MAC address for the machine is it is loaded on and stores it statically for retrieval.
 * It can also be used for formatting MAC addresses.
 **/
public class MacAddress {
    protected static Logger LOGGER = LogManager.getLogger(MacAddress.class);

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

    private static MacAddress macAddress;

    static {
        String macString = null;
        try {
            final List<NetworkInterface> nics = Collections.list(NetworkInterface.getNetworkInterfaces());
            Collections.reverse(nics);
            for (final NetworkInterface nic : nics) {
                final byte[] mac = nic.getHardwareAddress();
                if (mac != null &&
                        !nic.isVirtual() &&
                        !nic.isLoopback() &&
                        !nic.getName().startsWith("br") &&
                        !nic.getName().startsWith("veth") &&
                        !nic.getName().startsWith("vnet")) {
                    StringBuilder macAddressBuilder = new StringBuilder();
                    for (byte b : mac) {
                        macAddressBuilder.append(String.format("%02X", b));
                    }
                    macString = macAddressBuilder.toString();
                    break;
                }
            }
        } catch (SocketException ignore) {
        }

        long macAddressLong = 0;

        if (macString != null) {
            macAddressLong = Long.parseLong(macString, 16);
        } else {
            try {
                byte[] local = InetAddress.getLocalHost().getAddress();
                macAddressLong |= (local[0] << 24) & 0xFF000000L;
                macAddressLong |= (local[1] << 16) & 0xFF0000;
                macAddressLong |= (local[2] << 8) & 0xFF00;
                macAddressLong |= local[3] & 0xFF;
            } catch (UnknownHostException ex) {
                macAddressLong |= (long)(Math.random() * 0x7FFFFFFF);
            }
        }

        MacAddress.macAddress = new MacAddress(macAddressLong);
    }

    public static MacAddress getMacAddress() {
        return macAddress;
    }
}
