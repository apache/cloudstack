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

package org.apache.cloudstack.utils.net.cidr;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;

import com.cloud.utils.net.NetUtils;

public class CIDR4 implements CIDR {
    private static final long serialVersionUID = 5948678078525984569L;

    protected final static Logger s_logger = Logger.getLogger(CIDR4.class);

    protected final InetAddress baseAddress;
    protected int cidrMask;
    private int addressInt;
    private final int addressEndInt;

    //IP address space for private Internet
    final static CIDR CIDR_10_0_0_0m8;
    final static CIDR CIDR_172_16_0_0m12;
    final static CIDR CIDR_192_168_0_0m16;

    static {
        try {
            CIDR_10_0_0_0m8 = CIDRFactory.createCIDR("10.0.0.0/8");
            CIDR_172_16_0_0m12 = CIDRFactory.createCIDR("172.16.0.0/12");
            CIDR_192_168_0_0m16 = CIDRFactory.createCIDR("192.168.0.0/16");
        } catch (CIDRException e) {
            // TODO Auto-generated catch block
            throw new CIDRError("Should never be thrown ", e);
        }
    }

    protected CIDR4(Inet4Address newaddr, int mask) {
        cidrMask = mask;
        addressInt = ipv4AddressToInt(newaddr);
        int newmask = ipv4PrefixLengthToMask(mask);
        addressInt &= newmask;
        try {
            baseAddress = intToIPv4Address(addressInt);
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
        addressEndInt = addressInt + ipv4PrefixLengthToLength(cidrMask) - 1;
    }

    public int compareTo(CIDR arg) {
        if (!(arg instanceof CIDR4)) {
            throw new NotImplementedException("Not implemented for " + arg.getClass());
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
            throw new Error("inetAddress cannot be null");
        }
        if (!(inetAddress instanceof Inet4Address)) {
            throw new Error("inetAddress should be of Inet4Address");
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
        Long[] cidrALong = this.toLong();
        Long[] cidrBLong = cidr.toLong();

        long shift = MAX_CIDR4 - cidrBLong[1];
        return cidrALong[0] >> shift == cidrBLong[0] >> shift;
    }

    @Override
    public boolean overlaps(CIDR cidr) {
        Long[] cidrALong = this.toLong();
        Long[] cidrBLong = cidr.toLong();

        final long shift = MAX_CIDR4 - (cidrALong[1] > cidrBLong[1] ? cidrBLong[1] : cidrALong[1]);
        return cidrALong[0] >> shift == cidrBLong[0] >> shift;
    }

    @Override
    public boolean overlaps(CIDR[] ospfSuperCIDRList) {
        if (!ArrayUtils.isEmpty(ospfSuperCIDRList)) {
            for (CIDR superCidr : ospfSuperCIDRList) {
                if (this.overlaps(superCidr)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return baseAddress.getHostAddress() + '/' + cidrMask;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseAddress == null) ? 0 : baseAddress.hashCode());
        result = prime * result + cidrMask;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CIDR4 other = (CIDR4)obj;
        if (baseAddress == null) {
            if (other.baseAddress != null)
                return false;
        } else if (!baseAddress.equals(other.baseAddress))
            return false;
        if (cidrMask != other.cidrMask)
            return false;
        return true;
    }

    @Override
    public boolean isGuestCidr() throws CIDRException {
        if (CIDR_10_0_0_0m8.contains(this) || CIDR_172_16_0_0m12.contains(this) || CIDR_192_168_0_0m16.contains(this)) {
            return true;
        } else {
            s_logger.warn("cidr " + this + " is not RFC 1918 compliant");
            return false;
        }
    }

    @Override
    public Long[] toLong() {
        return NetUtils.cidrToLong(this.toString());
    }

}