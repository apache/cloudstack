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
package com.cloud.bridge.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.apache.log4j.Logger;

public class NetHelper {
    protected final static Logger logger = Logger.getLogger(NetHelper.class);

    public static String getHostName() {
        try {
            InetAddress localAddr = InetAddress.getLocalHost();
            if (localAddr != null) {
                return localAddr.getHostName();
            }
        } catch (UnknownHostException e) {
            logger.warn("UnknownHostException when trying to get host name. ", e);
        }
        return "localhost";
    }

    public static InetAddress[] getAllLocalInetAddresses() {
        List<InetAddress> addrList = new ArrayList<InetAddress>();
        try {
            for (NetworkInterface ifc : IteratorHelper.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
                if (ifc.isUp() && !ifc.isVirtual()) {
                    for (InetAddress addr : IteratorHelper.enumerationAsIterable(ifc.getInetAddresses())) {
                        addrList.add(addr);
                    }
                }
            }
        } catch (SocketException e) {
            logger.warn("SocketException in getAllLocalInetAddresses().", e);
        }

        InetAddress[] addrs = new InetAddress[addrList.size()];
        if (addrList.size() > 0)
            System.arraycopy(addrList.toArray(), 0, addrs, 0, addrList.size());
        return addrs;
    }

    public static InetAddress getFirstNonLoopbackLocalInetAddress() {
        InetAddress[] addrs = getAllLocalInetAddresses();
        if (addrs != null) {
            for (InetAddress addr : addrs) {
                if (logger.isInfoEnabled())
                    logger.info("Check local InetAddress : " + addr.toString() + ", total count :" + addrs.length);

                if (!addr.isLoopbackAddress())
                    return addr;
            }
        }

        logger.warn("Unable to determine a non-loopback address, local inet address count :" + addrs.length);
        return null;
    }

    public static String getMacAddress(InetAddress address) {
        StringBuffer sb = new StringBuffer();
        Formatter formatter = new Formatter(sb);
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            byte[] mac = ni.getHardwareAddress();

            for (int i = 0; i < mac.length; i++) {
                formatter.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : "");
            }
        } catch (SocketException e) {
            logger.error("SocketException when trying to retrieve MAC address", e);
        }
        return sb.toString();
    }
}
