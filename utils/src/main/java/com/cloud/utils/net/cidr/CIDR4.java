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

package com.cloud.utils.net.cidr;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;

import com.cloud.utils.net.NetUtils;

/**
 */
public class CIDR4 implements CIDR {
    protected final static Logger s_logger = Logger.getLogger(CIDR4.class);

    protected InetAddress baseAddress;
    protected int cidrMask;
    private int addressInt;
    private final int addressEndInt;

    protected CIDR4(Inet4Address newaddr, int mask) {
        cidrMask = mask;
        addressInt = ipv4AddressToInt(newaddr);
        int newmask = ipv4PrefixLengthToMask(mask);
        addressInt &= newmask;
        try {
            baseAddress = intToIPv4Address(addressInt);
        } catch (UnknownHostException e) {
            // this should never happen
        }
        addressEndInt = addressInt + ipv4PrefixLengthToLength(cidrMask) - 1;
    }

    public int compareTo(CIDR arg) {
        if (arg instanceof CIDR6) {
            throw new NotImplementedException("Not implemented for CIDR6");
        }
        CIDR4 o = (CIDR4)arg;
        if (o.addressInt == addressInt && o.cidrMask == cidrMask) {
            return 0;
        }
        if (o.addressInt < addressInt) {
            return 1;
        }
        if (o.addressInt > addressInt) {
            return -1;
        }
        if (o.cidrMask < cidrMask) {
            // greater Mask means less IpAddresses so -1
            return -1;
        }
        return 1;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean contains(InetAddress inetAddress) {
        if (inetAddress == null) {
            throw new NullPointerException("inetAddress");
        }

        if (cidrMask == 0) {
            return true;
        }

        int search = ipv4AddressToInt(inetAddress);
        return search >= addressInt && search <= addressEndInt;
    }

    private static int ipv4PrefixLengthToLength(int prefixLength) {
        return 1 << 32 - prefixLength;
    }

    private static int ipv4PrefixLengthToMask(int prefixLength) {
        return ~((1 << 32 - prefixLength) - 1);
    }

    private static InetAddress intToIPv4Address(int addr) throws UnknownHostException {
        byte[] a = new byte[4];
        a[0] = (byte)(addr >> 24 & 0xFF);
        a[1] = (byte)(addr >> 16 & 0xFF);
        a[2] = (byte)(addr >> 8 & 0xFF);
        a[3] = (byte)(addr & 0xFF);
        return InetAddress.getByAddress(a);
    }

    private static int ipv4AddressToInt(InetAddress addr) {
        byte[] address;
        if (addr instanceof Inet6Address) {
            throw new NotImplementedException("Not implemented for ipv6 address");
        } else {
            address = addr.getAddress();
        }
        return ipv4AddressToInt(address);
    }

    private static int ipv4AddressToInt(byte[] address) {
        int net = 0;
        for (byte addres : address) {
            net <<= 8;
            net |= addres & 0xFF;
        }
        return net;
    }

    @Override
    public InetAddress getBaseAddress() {
        return baseAddress;
    }

    @Override
    public int getMask() {
        return cidrMask;
    }

    @Override
    public boolean contains(CIDR cidr) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean overlaps(CIDR cidr) {
        Long[] cidrALong = NetUtils.cidrToLong(this);
        Long[] cidrBLong = NetUtils.cidrToLong(cidr);
        final long shift = MAX_CIDR - (cidrALong[1] > cidrBLong[1] ? cidrBLong[1] : cidrALong[1]);
        return cidrALong[0] >> shift == cidrBLong[0] >> shift;
    }

    @Override
    public String toString() {
        return baseAddress.getHostAddress() + '/' + cidrMask;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CIDR)) {
            return false;
        }
        return compareTo((CIDR)o) == 0;
    }

    @Override
    public int hashCode() {
        return baseAddress.hashCode();
    }
}