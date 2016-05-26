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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.utils.net.ipaddress.IpAddressFactory;
import org.apache.cloudstack.utils.net.netmask.NetmaskFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.net.util.SubnetUtils;

import com.cloud.utils.net.NetUtils;

public class CIDRFactory {

    public static CIDR createCIDR(InetAddress baseAddress, int cidrMask) throws CIDRException {
        if (cidrMask < 0) {
            throw new CIDRException("Invalid mask length used: " + cidrMask);
        }
        if (baseAddress instanceof Inet4Address) {
            if (cidrMask > 32) {
                throw new CIDRException("Invalid mask length used: " + cidrMask);
            }
            return new CIDR4((Inet4Address)baseAddress, cidrMask);
        }
        // IPv6.
        if (cidrMask > 128) {
            throw new CIDRException("Invalid mask length used: " + cidrMask);
        }
        return new CIDR6((Inet6Address)baseAddress, cidrMask);
    }

    public static CIDR createCIDR(String cidr) throws CIDRException {
        int p = cidr.indexOf('/');
        if (p < 0) {
            throw new CIDRException("Invalid CIDR notation used: " + cidr);
        }
        String addrString = cidr.substring(0, p);
        String maskString = cidr.substring(p + 1);
        InetAddress addr = IpAddressFactory.createFromHostName(addrString);
        int mask;
        if (maskString.indexOf('.') < 0) {
            mask = Integer.decode(maskString);
        } else {
            mask = NetmaskFactory.getNetMask(maskString);
            if (addr instanceof Inet6Address) {
                mask += 96;
            }
        }
        if (mask < 0) {
            throw new CIDRException("Invalid mask length used: " + maskString);
        }
        return createCIDR(addr, mask);
    }

    public static boolean isValidCIDR(String cidrStr) {
        if (cidrStr == null || cidrStr.isEmpty()) {
            return false;
        }
        final String[] cidrPair = cidrStr.split("\\/");
        if (cidrPair.length != 2) {
            return false;
        }
        final String cidrAddress = cidrPair[0];
        final String cidrSize = cidrPair[1];

        int cidrSizeNum = -1;
        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (final Exception e) {
            return false;
        }

        if (IpAddressFactory.isValidIpv4(cidrAddress)) {
            if (cidrSizeNum < 0 || cidrSizeNum > CIDR.MAX_CIDR4) {
                return false;
            } else {
                return true;
            }
        } else if (IpAddressFactory.isValidIpv6(cidrAddress)) {
            if (cidrSizeNum < 0 || cidrSizeNum > CIDR.MAX_CIDR6) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }

    }

    public static CIDR[] getCIDRList(String superCIDRList) throws CIDRException {
        String[] cidr_list = superCIDRList.split(",");
        for (String cidr : cidr_list) {
            if (!CIDRFactory.isValidCIDR(cidr)) {
                throw new CIDRException("The super CIDR is not a valid cidr " + cidr);
            }
        }
        CIDR[] v_cidr = convertToCIDR(cidr_list);
        if (!cidrListConsistency(v_cidr)) {
            throw new CIDRException("The cidr list is not consistent " + Arrays.toString(cidr_list));
        }
        return v_cidr;
    }

    public static List<CIDR> getAllSubnets(final CIDR[] superCidrList, final String netmask) throws CIDRException {
        if (!NetUtils.isValidNetmask(netmask)) {
            throw new IllegalStateException("Invalid netmask");
        }

        List<String> addresses = new ArrayList<String>();
        if (!ArrayUtils.isEmpty(superCidrList)) {
            for (CIDR superCidr : superCidrList) {
                SubnetUtils utils = new SubnetUtils(superCidr.toString());
                addresses.addAll(Arrays.asList(utils.getInfo().getAllAddresses()));
            }
        }

        List<CIDR> subnets = new ArrayList<CIDR>();
        String cidr = "";
        for (String tip : addresses) {
            String new_cidr = NetUtils.getCidrFromGatewayAndNetmask(tip, netmask);
            if (!cidr.endsWith(new_cidr)) {
                subnets.add(CIDRFactory.createCIDR(new_cidr));
                cidr = new_cidr;
            }
        }
        return subnets;
    }

    public static CIDR findUnusedSubnet(CIDR[] usedSubnets, CIDR[] cidrList, String netmask) throws CIDRException {
        //find cidr that does not overlap with the used cidrs
        List<CIDR> allSubnets = CIDRFactory.getAllSubnets(cidrList, netmask);
        //check for overlap with usedSubnets
        CIDR unused_cidr = null;
        for (CIDR subnet : allSubnets) {
            if (subnet.overlaps(usedSubnets)) {
                continue;
            } else {
                unused_cidr = subnet;
                break;
            }
        }
        return unused_cidr;
    }

    public static boolean isCidrOverlap(final String cidr, final List<String> usedCidrs) {
        for (String usedCidr : usedCidrs) {
            boolean result = NetUtils.isNetworksOverlap(cidr, usedCidr);
            if (result) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCidrOverlap(final CIDR cidr, final List<CIDR> usedCidrs) {
        for (CIDR usedCidr : usedCidrs) {
            if (cidr.overlaps(usedCidr)) {
                return true;
            }
        }
        return false;
    }

    public static boolean cidrListConsistency(final CIDR[] cidrList) throws CIDRException, IllegalStateException {
        if (ArrayUtils.isEmpty(cidrList)) {
            throw new IllegalStateException("Null or empty cidrList");
        }
        boolean isGuestCidr = false;
        if (cidrList[0].isGuestCidr()) {
            isGuestCidr = true;
        }
        // check if there is any overlap of cidrs
        for (int i = 0; i < cidrList.length; i++) {
            for (int j = i + 1; j < cidrList.length; j++) {
                if (cidrList[i].overlaps(cidrList[j])) {
                    throw new IllegalStateException("Invalid cidr in the list the overlapping cidrs are " + cidrList[i] + " and " + cidrList[j]);
                }
            }
        }

        if (isGuestCidr) {
            for (CIDR cidr : cidrList) {
                if (!cidr.isGuestCidr()) {
                    return false;
                }
            }
            return true;
        } else { //inverted check
            for (CIDR cidr : cidrList) {
                if (cidr.isGuestCidr()) {
                    return false;
                }
            }
            return true;
        }
    }

    public static InetAddress addressStringToInet(String addr) throws UnknownHostException {
        return InetAddress.getByName(addr);
    }

    public static CIDR[] convertToCIDR(String[] cidrs) throws CIDRException {
        List<CIDR> v_cidr = new ArrayList<CIDR>();
        for (String cidr : cidrs) {
            v_cidr.add(CIDRFactory.createCIDR(cidr));
        }
        return (CIDR[])v_cidr.toArray(new CIDR[v_cidr.size()]);
    }

}
