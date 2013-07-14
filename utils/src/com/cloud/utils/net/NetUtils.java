// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.googlecode.ipv6.IPv6Network;

import com.cloud.utils.IteratorUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;
import org.apache.commons.lang.SystemUtils;

public class NetUtils {
    protected final static Logger s_logger = Logger.getLogger(NetUtils.class);
    public final static String HTTP_PORT = "80";
    public final static int VPN_PORT = 500;
    public final static int VPN_NATT_PORT = 4500;
    public final static int VPN_L2TP_PORT = 1701;

    public final static String UDP_PROTO = "udp";
    public final static String TCP_PROTO = "tcp";
    public final static String ANY_PROTO = "any";
    public final static String ICMP_PROTO = "icmp";
    public final static String ALL_PROTO = "all";

    public final static String ALL_CIDRS = "0.0.0.0/0";
    public final static int PORT_RANGE_MIN = 0;
    public final static int PORT_RANGE_MAX = 65535;

    public final static int DEFAULT_AUTOSCALE_VM_DESTROY_TIME = 2 * 60; // Grace period before Vm is destroyed
    public final static int DEFAULT_AUTOSCALE_POLICY_INTERVAL_TIME = 30;
    public final static int DEFAULT_AUTOSCALE_POLICY_QUIET_TIME = 5 * 60;
    private final static Random _rand = new Random(System.currentTimeMillis());

    public static long createSequenceBasedMacAddress(long macAddress) {
        return macAddress | 0x060000000000l | (((long) _rand.nextInt(32768) << 25) & 0x00fffe000000l);
    }

    public static String getHostName() {
        try {
            InetAddress localAddr = InetAddress.getLocalHost();
            if (localAddr != null) {
                return localAddr.getHostName();
            }
        } catch (UnknownHostException e) {
            s_logger.warn("UnknownHostException when trying to get host name. ", e);
        }
        return "localhost";
    }

    public static InetAddress getLocalInetAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            s_logger.warn("UnknownHostException in getLocalInetAddress().", e);
            return null;
        }
    }

    public static String resolveToIp(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return ipFromInetAddress(addr);
        } catch (UnknownHostException e) {
            s_logger.warn("Unable to resolve " + host + " to IP due to UnknownHostException");
            return null;
        }
    }

    public static InetAddress[] getAllLocalInetAddresses() {
        List<InetAddress> addrList = new ArrayList<InetAddress>();
        try {
            for (NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
                if (ifc.isUp() && !ifc.isVirtual()) {
                    for (InetAddress addr : IteratorUtil.enumerationAsIterable(ifc.getInetAddresses())) {
                        addrList.add(addr);
                    }
                }
            }
        } catch (SocketException e) {
            s_logger.warn("SocketException in getAllLocalInetAddresses().", e);
        }

        InetAddress[] addrs = new InetAddress[addrList.size()];
        if (addrList.size() > 0) {
            System.arraycopy(addrList.toArray(), 0, addrs, 0, addrList.size());
        }
        return addrs;
    }

    public static String[] getLocalCidrs() {
        String defaultHostIp = getDefaultHostIp();

        List<String> cidrList = new ArrayList<String>();
        try {
            for (NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
                if (ifc.isUp() && !ifc.isVirtual() && !ifc.isLoopback()) {
                    for (InterfaceAddress address : ifc.getInterfaceAddresses()) {
                        InetAddress addr = address.getAddress();
                        int prefixLength = address.getNetworkPrefixLength();
                        if (prefixLength < 32 && prefixLength > 0) {
                            String ip = ipFromInetAddress(addr);
                            if(ip.equalsIgnoreCase(defaultHostIp))
                                cidrList.add(ipAndNetMaskToCidr(ip, getCidrNetmask(prefixLength)));
                        }
                    }
                }
            }
        } catch (SocketException e) {
            s_logger.warn("UnknownHostException in getLocalCidrs().", e);
        }

        return cidrList.toArray(new String[0]);
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        if(os != null && os.startsWith("Windows"))
            return true;

        return false;
    }



    public static String getDefaultHostIp() {
        if(SystemUtils.IS_OS_WINDOWS) {
            Pattern pattern = Pattern.compile("\\s*0.0.0.0\\s*0.0.0.0\\s*(\\S*)\\s*(\\S*)\\s*");
            try {
                Process result = Runtime.getRuntime().exec("route print -4");
                BufferedReader output = new BufferedReader
                        (new InputStreamReader(result.getInputStream()));

                String line = output.readLine();
                while(line != null){
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(2);
                    }
                    line = output.readLine();
                }
            } catch( Exception e ) { 
            }    	
            return null;
        } else {
            NetworkInterface nic = null;
            String pubNic = getDefaultEthDevice();

            if (pubNic == null) {
                return null;
            }

            try {
                nic = NetworkInterface.getByName(pubNic);
            } catch (final SocketException e) {
                return null;
            }

            String[] info = NetUtils.getNetworkParams(nic);
            return info[0];
        }
    }

    public static String getDefaultEthDevice() {
        if (SystemUtils.IS_OS_MAC) {
            String defDev = Script.runSimpleBashScript("/sbin/route -n get default | grep interface | awk '{print $2}'");
            return defDev;
        }
        String defaultRoute = Script.runSimpleBashScript("/sbin/route | grep default");

        if (defaultRoute == null) {
            return null;
        }

        String[] defaultRouteList = defaultRoute.split("\\s+");

        if (defaultRouteList.length != 8) {
            return null;
        }

        return defaultRouteList[7];
    }



    public static InetAddress getFirstNonLoopbackLocalInetAddress() {
        InetAddress[] addrs = getAllLocalInetAddresses();
        if (addrs != null) {
            for (InetAddress addr : addrs) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Check local InetAddress : " + addr.toString() + ", total count :" + addrs.length);
                }

                if (!addr.isLoopbackAddress()) {
                    return addr;
                }
            }
        }

        s_logger.warn("Unable to determine a non-loopback address, local inet address count :" + addrs.length);
        return null;
    }

    public static InetAddress[] getInterfaceInetAddresses(String ifName) {
        List<InetAddress> addrList = new ArrayList<InetAddress>();
        try {
            for (NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
                if (ifc.isUp() && !ifc.isVirtual() && ifc.getName().equals(ifName)) {
                    for (InetAddress addr : IteratorUtil.enumerationAsIterable(ifc.getInetAddresses())) {
                        addrList.add(addr);
                    }
                }
            }
        } catch (SocketException e) {
            s_logger.warn("SocketException in getAllLocalInetAddresses().", e);
        }

        InetAddress[] addrs = new InetAddress[addrList.size()];
        if (addrList.size() > 0) {
            System.arraycopy(addrList.toArray(), 0, addrs, 0, addrList.size());
        }
        return addrs;
    }

    public static String getLocalIPString() {
        InetAddress addr = getLocalInetAddress();
        if (addr != null) {
            return ipFromInetAddress(addr);
        }

        return "127.0.0.1";
    }

    public static String ipFromInetAddress(InetAddress addr) {
        assert (addr != null);

        byte[] ipBytes = addr.getAddress();
        StringBuffer sb = new StringBuffer();
        sb.append(ipBytes[0] & 0xff).append(".");
        sb.append(ipBytes[1] & 0xff).append(".");
        sb.append(ipBytes[2] & 0xff).append(".");
        sb.append(ipBytes[3] & 0xff);

        return sb.toString();
    }

    public static boolean isLocalAddress(InetAddress addr) {
        InetAddress[] addrs = getAllLocalInetAddresses();

        if (addrs != null) {
            for (InetAddress self : addrs) {
                if (self.equals(addr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLocalAddress(String strAddress) {

        InetAddress addr;
        try {
            addr = InetAddress.getByName(strAddress);
            return isLocalAddress(addr);
        } catch (UnknownHostException e) {
        }
        return false;
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
            s_logger.error("SocketException when trying to retrieve MAC address", e);
        }
        return sb.toString();
    }

    public static long getMacAddressAsLong(InetAddress address) {
        long macAddressAsLong = 0;
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            byte[] mac = ni.getHardwareAddress();

            for (int i = 0; i < mac.length; i++) {
                macAddressAsLong |= ((long) (mac[i] & 0xff) << (mac.length - i - 1) * 8);
            }

        } catch (SocketException e) {
            s_logger.error("SocketException when trying to retrieve MAC address", e);
        }

        return macAddressAsLong;
    }

    public static boolean ipRangesOverlap(String startIp1, String endIp1, String startIp2, String endIp2) {
        long startIp1Long = ip2Long(startIp1);
        long endIp1Long = startIp1Long;
        if (endIp1 != null) {
            endIp1Long = ip2Long(endIp1);
        }
        long startIp2Long = ip2Long(startIp2);
        long endIp2Long = startIp2Long;
        if (endIp2 != null) {
            endIp2Long = ip2Long(endIp2);
        }

        if (startIp1Long == startIp2Long || startIp1Long == endIp2Long || endIp1Long == startIp2Long || endIp1Long == endIp2Long) {
            return true;
        } else if (startIp1Long > startIp2Long && startIp1Long < endIp2Long) {
            return true;
        } else if (endIp1Long > startIp2Long && endIp1Long < endIp2Long) {
            return true;
        } else if (startIp2Long > startIp1Long && startIp2Long < endIp1Long) {
            return true;
        } else if (endIp2Long > startIp1Long && endIp2Long < endIp1Long) {
            return true;
        } else {
            return false;
        }
    }

    public static long ip2Long(String ip) {
        String[] tokens = ip.split("[.]");
        assert (tokens.length == 4);
        long result = 0;
        for (int i = 0; i < tokens.length; i++) {
            try {
                result = (result << 8) | Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Incorrect number", e);
            }
        }

        return result;
    }

    public static String long2Ip(long ip) {
        StringBuilder result = new StringBuilder(15);
        result.append((ip >> 24 & 0xff)).append(".");
        result.append((ip >> 16 & 0xff)).append(".");
        result.append((ip >> 8 & 0xff)).append(".");
        result.append(ip & 0xff);

        return result.toString();
    }

    public static long mac2Long(String macAddress) {
        String[] tokens = macAddress.split(":");
        assert (tokens.length == 6);
        long result = 0;
        for (int i = 0; i < tokens.length; i++) {
            result = result << 8;
            result |= Integer.parseInt(tokens[i], 16);
        }
        return result;
    }

    public static String[] getNicParams(String nicName) {
        try {
            NetworkInterface nic = NetworkInterface.getByName(nicName);
            return getNetworkParams(nic);
        } catch (SocketException e) {
            return null;
        }
    }

    public static String[] getNetworkParams(NetworkInterface nic) {
        List<InterfaceAddress> addrs = nic.getInterfaceAddresses();
        if (addrs == null || addrs.size() == 0) {
            return null;
        }
        InterfaceAddress addr = null;
        for (InterfaceAddress iaddr : addrs) {
            InetAddress inet = iaddr.getAddress();
            if (!inet.isLinkLocalAddress() && !inet.isLoopbackAddress() && !inet.isMulticastAddress() && inet.getAddress().length == 4) {
                addr = iaddr;
                break;
            }
        }
        if (addr == null) {
            return null;
        }
        String[] result = new String[3];
        result[0] = addr.getAddress().getHostAddress();
        try {
            byte[] mac = nic.getHardwareAddress();
            result[1] = byte2Mac(mac);
        } catch (Exception e) {
        }

        result[2] = prefix2Netmask(addr.getNetworkPrefixLength());
        return result;
    }

    public static String prefix2Netmask(short prefix) {
        long addr = 0;
        for (int i = 0; i < prefix; i++) {
            addr = addr | (1 << (31 - i));
        }

        return long2Ip(addr);
    }

    public static String byte2Mac(byte[] m) {
        StringBuilder result = new StringBuilder(17);
        Formatter formatter = new Formatter(result);
        formatter.format("%02x:%02x:%02x:%02x:%02x:%02x", m[0], m[1], m[2], m[3], m[4], m[5]);
        return result.toString();
    }

    public static String long2Mac(long macAddress) {
        StringBuilder result = new StringBuilder(17);
        Formatter formatter = new Formatter(result);
        formatter.format("%02x:%02x:%02x:%02x:%02x:%02x", (macAddress >> 40) & 0xff, (macAddress >> 32) & 0xff, (macAddress >> 24) & 0xff, (macAddress >> 16) & 0xff, (macAddress >> 8) & 0xff,
                (macAddress & 0xff));

        return result.toString();
    }

    public static boolean isValidPrivateIp(String ipAddress, String guestIPAddress) {

        InetAddress privIp = parseIpAddress(ipAddress);
        if (privIp == null) {
            return false;
        }
        if (!privIp.isSiteLocalAddress()) {
            return false;
        }

        String firstGuestOctet = "10";
        if (guestIPAddress != null && !guestIPAddress.isEmpty()) {
            String[] guestIPList = guestIPAddress.split("\\.");
            firstGuestOctet = guestIPList[0];
        }

        String[] ipList = ipAddress.split("\\.");
        if (!ipList[0].equals(firstGuestOctet)) {
            return false;
        }

        return true;
    }

    public static boolean isSiteLocalAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        } else {
            InetAddress ip = parseIpAddress(ipAddress);
            return ip.isSiteLocalAddress();
        }
    }

    public static boolean validIpRange(String startIP, String endIP) {
        if (endIP == null || endIP.isEmpty()) {
            return true;
        }

        long startIPLong = NetUtils.ip2Long(startIP);
        long endIPLong = NetUtils.ip2Long(endIP);
        return (startIPLong <= endIPLong);
    }

    public static boolean isValidIp(final String ip) {
        final String[] ipAsList = ip.split("\\.");

        // The IP address must have four octets
        if (Array.getLength(ipAsList) != 4) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            // Each octet must be an integer
            final String octetString = ipAsList[i];
            int octet;
            try {
                octet = Integer.parseInt(octetString);
            } catch (final Exception e) {
                return false;
            }
            // Each octet must be between 0 and 255, inclusive
            if (octet < 0 || octet > 255) {
                return false;
            }

            // Each octetString must have between 1 and 3 characters
            if (octetString.length() < 1 || octetString.length() > 3) {
                return false;
            }
        }

        // IP is good, return true
        return true;
    }

    public static boolean isValidCIDR(final String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return false;
        }
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) {
            return false;
        }
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) {
            return false;
        }
        int cidrSizeNum = -1;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (Exception e) {
            return false;
        }

        if (cidrSizeNum < 0 || cidrSizeNum > 32) {
            return false;
        }

        return true;
    }

    public static boolean isValidNetmask(String netmask) {
        if (!isValidIp(netmask)) {
            return false;
        }

        long ip = ip2Long(netmask);
        int count = 0;
        boolean finished = false;
        for (int i = 31; i >= 0; i--) {
            if (((ip >> i) & 0x1) == 0) {
                finished = true;
            } else {
                if (finished) {
                    return false;
                }
                count += 1;
            }
        }

        if (count == 0) {
            return false;
        }

        return true;
    }

    private static InetAddress parseIpAddress(String address) {
        StringTokenizer st = new StringTokenizer(address, ".");
        byte[] bytes = new byte[4];

        if (st.countTokens() == 4) {
            try {
                for (int i = 0; i < 4; i++) {
                    bytes[i] = (byte) Integer.parseInt(st.nextToken());
                }
                return InetAddress.getByAddress(address, bytes);
            } catch (NumberFormatException nfe) {
                return null;
            } catch (UnknownHostException uhe) {
                return null;
            }
        }
        return null;
    }

    public static String getCidrFromGatewayAndNetmask(String gatewayStr, String netmaskStr) {
        long netmask = ip2Long(netmaskStr);
        long gateway = ip2Long(gatewayStr);
        long firstPart = gateway & netmask;
        long size = getCidrSize(netmaskStr);
        return long2Ip(firstPart) + "/" + size;
    }

    public static String[] getIpRangeFromCidr(String cidr, long size) {
        assert (size < 32) : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;
        String[] result = new String[2];
        long ip = ip2Long(cidr);
        long startNetMask = ip2Long(getCidrNetmask(size));
        long start = (ip & startNetMask) + 1;
        long end = start;

        end = end >> (32 - size);

        end++;
        end = (end << (32 - size)) - 2;

        result[0] = long2Ip(start);
        result[1] = long2Ip(end);

        return result;
    }

    public static Set<Long> getAllIpsFromCidr(String cidr, long size, Set<Long> usedIps) {
        assert (size < 32) : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;
        Set<Long> result = new TreeSet<Long>();
        long ip = ip2Long(cidr);
        long startNetMask = ip2Long(getCidrNetmask(size));
        long start = (ip & startNetMask) + 1;
        long end = start;

        end = end >> (32 - size);

        end++;
        end = (end << (32 - size)) - 2;
        int maxIps = 255; // get 255 ips as maximum
        while (start <= end && maxIps > 0) {
            if (!usedIps.contains(start)){
                result.add(start);
                maxIps--;
            }
            start++;
        }

        return result;
    }

    /**
     * Given a cidr, this method returns an ip address within the range but
     * is not in the avoid list.
     * 
     * @param startIp ip that the cidr starts with
     * @param size size of the cidr
     * @param avoid set of ips to avoid
     * @return ip that is within the cidr range but not in the avoid set.  -1 if unable to find one.
     */
    public static long getRandomIpFromCidr(String startIp, int size, SortedSet<Long> avoid) {
        return getRandomIpFromCidr(ip2Long(startIp), size, avoid);

    }

    /**
     * Given a cidr, this method returns an ip address within the range but
     * is not in the avoid list. 
     * Note: the gateway address has to be specified in the avoid list
     * 
     * @param cidr ip that the cidr starts with
     * @param size size of the cidr
     * @param avoid set of ips to avoid
     * @return ip that is within the cidr range but not in the avoid set.  -1 if unable to find one.
     */
    public static long getRandomIpFromCidr(long cidr, int size, SortedSet<Long> avoid) {
        assert (size < 32) : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;

        long startNetMask = ip2Long(getCidrNetmask(size));
        long startIp = (cidr & startNetMask) + 1; //exclude the first ip since it isnt valid, e.g., 192.168.10.0
        int range = 1 << (32 - size); //e.g., /24 = 2^8 = 256
        range = range -1; //exclude end of the range since that is the broadcast address, e.g., 192.168.10.255

        if (avoid.size() >= range) {
            return -1;
        }

        //Reduce the range by the size of the avoid set
        //e.g., cidr = 192.168.10.0, size = /24, avoid = 192.168.10.1, 192.168.10.20, 192.168.10.254
        // range = 2^8 - 1 - 3 = 252
        range = range - avoid.size();
        int next = _rand.nextInt(range); //note: nextInt excludes last value
        long ip = startIp + next;
        for (Long avoidable : avoid) {
            if (ip >= avoidable) {
                ip++;
            } else {
                break;
            }
        }

        return ip;
    }

    public static String getIpRangeStartIpFromCidr(String cidr, long size) {
        long ip = ip2Long(cidr);
        long startNetMask = ip2Long(getCidrNetmask(size));
        long start = (ip & startNetMask) + 1;
        return long2Ip(start);
    }

    public static String getIpRangeEndIpFromCidr(String cidr, long size) {
        long ip = ip2Long(cidr);
        long startNetMask = ip2Long(getCidrNetmask(size));
        long start = (ip & startNetMask) + 1;
        long end = start;
        end = end >> (32 - size);

        end++;
        end = (end << (32 - size)) - 2;
        return long2Ip(end);
    }

    public static boolean sameSubnet(final String ip1, final String ip2, final String netmask) {
        if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) {
            return true;
        }
        String subnet1 = NetUtils.getSubNet(ip1, netmask);
        String subnet2 = NetUtils.getSubNet(ip2, netmask);

        return (subnet1.equals(subnet2));
    }

    public static boolean sameSubnetCIDR(final String ip1, final String ip2, final long cidrSize) {
        if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) {
            return true;
        }
        String subnet1 = NetUtils.getCidrSubNet(ip1, cidrSize);
        String subnet2 = NetUtils.getCidrSubNet(ip2, cidrSize);

        return (subnet1.equals(subnet2));
    }

    public static String getSubNet(String ip, String netmask) {
        long ipAddr = ip2Long(ip);
        long subnet = ip2Long(netmask);
        long result = ipAddr & subnet;
        return long2Ip(result);
    }

    public static String getCidrSubNet(String ip, long cidrSize) {
        long numericNetmask = (0xffffffff >> (32 - cidrSize)) << (32 - cidrSize);
        String netmask = NetUtils.long2Ip(numericNetmask);
        return getSubNet(ip, netmask);
    }

    public static String ipAndNetMaskToCidr(String ip, String netmask) {
        long ipAddr = ip2Long(ip);
        long subnet = ip2Long(netmask);
        long result = ipAddr & subnet;
        int bits = (subnet == 0) ? 0 : 1;
        long subnet2 = subnet;
        while ((subnet2 = (subnet2 >> 1) & subnet) != 0) {
            bits++;
        }

        return long2Ip(result) + "/" + Integer.toString(bits);
    }

    public static String[] ipAndNetMaskToRange(String ip, String netmask) {
        long ipAddr = ip2Long(ip);
        long subnet = ip2Long(netmask);
        long start = (ipAddr & subnet) + 1;
        long end = start;
        int bits = (subnet == 0) ? 0 : 1;
        while ((subnet = (subnet >> 1) & subnet) != 0) {
            bits++;
        }
        end = end >> (32 - bits);

        end++;
        end = (end << (32 - bits)) - 2;

        return new String[] { long2Ip(start), long2Ip(end) };

    }

    public static Pair<String, Integer> getCidr(String cidr) {
        String[] tokens = cidr.split("/");
        return new Pair<String, Integer>(tokens[0], Integer.parseInt(tokens[1]));
    }

    public  static enum supersetOrSubset {
        isSuperset,
        isSubset,
        neitherSubetNorSuperset,
        sameSubnet,
        errorInCidrFormat
    }
    public static supersetOrSubset isNetowrkASubsetOrSupersetOfNetworkB (String cidrA, String cidrB) {
        Long[] cidrALong = cidrToLong(cidrA);
        Long[] cidrBLong = cidrToLong(cidrB);
        long shift =0;
        if (cidrALong == null || cidrBLong == null) {
            //implies error in the cidr format
            return supersetOrSubset.errorInCidrFormat;
        }
        if (cidrALong[1] >= cidrBLong[1]) {
            shift = 32 - cidrBLong[1];
        }
        else {
            shift = 32 - cidrALong[1];
        }
        long result = (cidrALong[0] >> shift) - (cidrBLong[0] >> shift);
        if (result == 0) {
            if (cidrALong[1] < cidrBLong[1]) {
                //this implies cidrA is super set of cidrB
                return supersetOrSubset.isSuperset;
            }
            else if (cidrALong[1] == cidrBLong[1]) {
             //this implies both the cidrs are equal
                return supersetOrSubset.sameSubnet;
            }
            // implies cidrA is subset of cidrB
            return supersetOrSubset.isSubset;
        }
        //this implies no overlap.
        return supersetOrSubset.neitherSubetNorSuperset;
    }

    public static boolean isNetworkAWithinNetworkB(String cidrA, String cidrB) {
        Long[] cidrALong = cidrToLong(cidrA);
        Long[] cidrBLong = cidrToLong(cidrB);
        if (cidrALong == null || cidrBLong == null) {
            return false;
        }
        long shift = 32 - cidrBLong[1];
        return ((cidrALong[0] >> shift) == (cidrBLong[0] >> shift));
    }

    public static Long[] cidrToLong(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return null;
        }
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) {
            return null;
        }
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) {
            return null;
        }
        int cidrSizeNum = -1;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (Exception e) {
            return null;
        }
        long numericNetmask = (0xffffffff >> (32 - cidrSizeNum)) << (32 - cidrSizeNum);
        long ipAddr = ip2Long(cidrAddress);
        Long[] cidrlong = { ipAddr & numericNetmask, (long) cidrSizeNum };
        return cidrlong;

    }

    public static String getCidrSubNet(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return null;
        }
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) {
            return null;
        }
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) {
            return null;
        }
        int cidrSizeNum = -1;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (Exception e) {
            return null;
        }
        long numericNetmask = (0xffffffff >> (32 - cidrSizeNum)) << (32 - cidrSizeNum);
        String netmask = NetUtils.long2Ip(numericNetmask);
        return getSubNet(cidrAddress, netmask);
    }

    public static String getCidrNetmask(long cidrSize) {
        long numericNetmask = (0xffffffff >> (32 - cidrSize)) << (32 - cidrSize);
        return long2Ip(numericNetmask);
    }

    public static String getCidrNetmask(String cidr) {
        String[] cidrPair = cidr.split("\\/");
        long guestCidrSize = Long.parseLong(cidrPair[1]);
        return getCidrNetmask(guestCidrSize);
    }

    public static String cidr2Netmask(String cidr) {
        String[] tokens = cidr.split("\\/");
        return getCidrNetmask(Integer.parseInt(tokens[1]));
    }

    public static long getCidrSize(String netmask) {
        long ip = ip2Long(netmask);
        int count = 0;
        for (int i = 0; i < 32; i++) {
            if (((ip >> i) & 0x1) == 0) {
                count++;
            } else {
                break;
            }
        }

        return 32 - count;
    }

    public static boolean isValidPort(String p) {
        try {
            int port = Integer.parseInt(p);
            return !(port > 65535 || port < 1);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidPort(int p) {
        return !(p > 65535 || p < 1);
    }

    public static boolean isValidLBPort(String p) {
        try {
            int port = Integer.parseInt(p);
            return !(port > 65535 || port < 1);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidProto(String p) {
        String proto = p.toLowerCase();
        return (proto.equals(TCP_PROTO) || proto.equals(UDP_PROTO) || proto.equals(ICMP_PROTO));
    }

    public static boolean isValidSecurityGroupProto(String p) {
        String proto = p.toLowerCase();
        return (proto.equals(TCP_PROTO) || proto.equals(UDP_PROTO) || proto.equals(ICMP_PROTO) || proto.equals(ALL_PROTO));
    }

    public static boolean isValidAlgorithm(String p) {
        String algo = p.toLowerCase();
        return (algo.equals("roundrobin") || algo.equals("leastconn") || algo.equals("source"));
    }

    public static boolean isValidAutoScaleAction(String p) {
        String action = p.toLowerCase();
        return (action.equals("scaleup") || action.equals("scaledown"));
    }

    public static String getLinkLocalNetMask() {
        return "255.255.0.0";
    }

    public static String getLinkLocalGateway() {
        return "169.254.0.1";
    }

    public static String getLinkLocalCIDR() {
        return "169.254.0.0/16";
    }

    public static String[] getLinkLocalIPRange(int size) {
        if (size > 16 || size <= 0) {
            return null;
        }
        /* reserve gateway */
        String[] range = getIpRangeFromCidr(getLinkLocalGateway(), 32 - size);

        if (range[0].equalsIgnoreCase(getLinkLocalGateway())) {
            /* remove the gateway */
            long ip = ip2Long(range[0]);
            ip += 1;
            range[0] = long2Ip(ip);
        }
        return range;
    }

    public static String getLinkLocalIpEnd() {
        String[] cidrPair = getLinkLocalCIDR().split("\\/");
        String cidr = cidrPair[0];

        return getIpRangeEndIpFromCidr(cidr, 32 - Long.parseLong(cidrPair[1]));
    }

    public static String portRangeToString(int portRange[]) {
        return Integer.toString(portRange[0]) + ":" + Integer.toString(portRange[1]);
    }


    public static boolean verifyDomainNameLabel(String hostName, boolean isHostName) {
        // must be between 1 and 63 characters long and may contain only the ASCII letters 'a' through 'z' (in a
        // case-insensitive manner),
        // the digits '0' through '9', and the hyphen ('-').
        // Can not start with a hyphen and digit, and must not end with a hyphen
        // If it's a host name, don't allow to start with digit

        if (hostName.length() > 63 || hostName.length() < 1) {
            s_logger.warn("Domain name label must be between 1 and 63 characters long");
            return false;
        } else if (!hostName.toLowerCase().matches("[a-z0-9-]*")) {
            s_logger.warn("Domain name label may contain only the ASCII letters 'a' through 'z' (in a case-insensitive manner)");
            return false;
        } else if (hostName.startsWith("-") || hostName.endsWith("-")) {
            s_logger.warn("Domain name label can not start  with a hyphen and digit, and must not end with a hyphen");
            return false;
        } else if (isHostName && hostName.matches("^[0-9-].*")) {
            s_logger.warn("Host name can't start with digit");
            return false;
        }

        return true;
    }

    public static boolean verifyDomainName(String domainName) {
        // don't allow domain name length to exceed 190 chars (190 + 63 (max host name length) = 253 = max domainName length
        if (domainName.length() < 1 || domainName.length() > 190) {
            s_logger.trace("Domain name must be between 1 and 190 characters long");
            return false;
        }

        if (domainName.startsWith(".") || domainName.endsWith(".")) {
            s_logger.trace("Domain name can't start or end with .");
            return false;
        }

        String[] domainNameLabels = domainName.split("\\.");

        for (int i = 0; i < domainNameLabels.length; i++) {
            if (!verifyDomainNameLabel(domainNameLabels[i], false)) {
                s_logger.warn("Domain name label " + domainNameLabels[i] + " is incorrect");
                return false;
            }
        }

        return true;
    }

    public static String getDhcpRange(String cidr) {
        String[] splitResult = cidr.split("\\/");
        long size = Long.valueOf(splitResult[1]);
        return NetUtils.getIpRangeStartIpFromCidr(splitResult[0], size);
    }

    // Check if 2 CIDRs have exactly same IP Range
    public static boolean isSameIpRange (String cidrA, String cidrB) {

        if(!NetUtils.isValidCIDR(cidrA)) {
            s_logger.info("Invalid value of cidr " + cidrA);
            return false;
        }
         if (!NetUtils.isValidCIDR(cidrB)) {
            s_logger.info("Invalid value of cidr " + cidrB);
            return false;
        }
        String[] cidrPairFirst = cidrA.split("\\/");
        String[] cidrPairSecond = cidrB.split("\\/");

        Long networkSizeFirst = Long.valueOf(cidrPairFirst[1]);
        Long networkSizeSecond = Long.valueOf(cidrPairSecond[1]);
        String ipRangeFirst [] = NetUtils.getIpRangeFromCidr(cidrPairFirst[0], networkSizeFirst);
        String ipRangeSecond [] = NetUtils.getIpRangeFromCidr(cidrPairFirst[0], networkSizeSecond);

        long startIpFirst = NetUtils.ip2Long(ipRangeFirst[0]);
        long endIpFirst = NetUtils.ip2Long(ipRangeFirst[1]);
        long startIpSecond = NetUtils.ip2Long(ipRangeSecond[0]);
        long endIpSecond = NetUtils.ip2Long(ipRangeSecond[1]);
        if(startIpFirst == startIpSecond && endIpFirst == endIpSecond) {
            return true;
        }
        return false;
    }
    public static boolean validateGuestCidr(String cidr) {
        // RFC 1918 - The Internet Assigned Numbers Authority (IANA) has reserved the
        // following three blocks of the IP address space for private internets:
        // 10.0.0.0 - 10.255.255.255 (10/8 prefix)
        // 172.16.0.0 - 172.31.255.255 (172.16/12 prefix)
        // 192.168.0.0 - 192.168.255.255 (192.168/16 prefix)

        String cidr1 = "10.0.0.0/8";
        String cidr2 = "172.16.0.0/12";
        String cidr3 = "192.168.0.0/16";

        if (!isValidCIDR(cidr)) {
            s_logger.warn("Cidr " + cidr + " is not valid");
            return false;
        }

        if (isNetworkAWithinNetworkB(cidr, cidr1) || isNetworkAWithinNetworkB(cidr, cidr2) || isNetworkAWithinNetworkB(cidr, cidr3)) {
            return true;
        } else {
            s_logger.warn("cidr " + cidr + " is not RFC 1918 compliant");
            return false;
        }
    }

    public static boolean verifyInstanceName(String instanceName) {
        //instance name for cloudstack vms shouldn't contain - and spaces
        if (instanceName.contains("-") || instanceName.contains(" ") || instanceName.contains("+")) {
            s_logger.warn("Instance name can not contain hyphen, spaces and \"+\" char");
            return false;
        } 

        return true;
    }

    public static boolean isNetworksOverlap(String cidrA, String cidrB) {
        Long[] cidrALong = cidrToLong(cidrA);
        Long[] cidrBLong = cidrToLong(cidrB);
        if (cidrALong == null || cidrBLong == null) {
            return false;
        }
        long shift = 32 - (cidrALong[1] > cidrBLong[1] ? cidrBLong[1] : cidrALong[1]);
        return ((cidrALong[0] >> shift) == (cidrBLong[0] >> shift));
    }

    public static boolean isValidS2SVpnPolicy(String policys) {
        if (policys == null || policys.isEmpty()) {
            return false;
        }
        for (String policy : policys.split(",")) {
            if (policy.isEmpty()) {
                return false;
            }
            String cipherHash = policy.split(";")[0];
            if (cipherHash.isEmpty()) {
                return false;
            }
            String[] list = cipherHash.split("-");
            if (list.length != 2) {
                return false;
            }
            String cipher = list[0];
            String hash = list[1];
            if (!cipher.matches("3des|aes128|aes192|aes256")) {
                return false;
            }
            if (!hash.matches("md5|sha1")) {
                return false;
            }
            String pfsGroup = null;
            if (!policy.equals(cipherHash)) {
                pfsGroup = policy.split(";")[1];
            }
            if (pfsGroup != null && !pfsGroup.matches("modp1024|modp1536")) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateGuestCidrList(String guestCidrList) {
        for (String guestCidr : guestCidrList.split(",")) {
            if (!validateGuestCidr(guestCidr)) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateIcmpType(long icmpType) {
        //Source - http://www.erg.abdn.ac.uk/~gorry/course/inet-pages/icmp-code.html
        if(!(icmpType >=0 && icmpType <=255)) {
            s_logger.warn("impcType is not within 0-255 range");
            return false;
        }
        return true;
    }

    public static boolean validateIcmpCode(long icmpCode) {

        //Source - http://www.erg.abdn.ac.uk/~gorry/course/inet-pages/icmp-code.html
        if(!(icmpCode >=0 && icmpCode <=15)) {
            s_logger.warn("Icmp code should be within 0-15 range");
            return false;
        }

        return true;
    }

	public static boolean isValidIpv6(String ip) {
		try {
			IPv6Address address = IPv6Address.fromString(ip);
		} catch (IllegalArgumentException ex) {
			return false;
		}
		return true;
	}

	public static boolean isValidIp6Cidr(String ip6Cidr) {
		try {
			IPv6Network network = IPv6Network.fromString(ip6Cidr);
		} catch (IllegalArgumentException ex) {
			return false;
		}
		return true;
	}

	public static int getIp6CidrSize(String ip6Cidr) {
		IPv6Network network = null;
		try {
			network = IPv6Network.fromString(ip6Cidr);
		} catch (IllegalArgumentException ex) {
			return 0;
		}
		return network.getNetmask().asPrefixLength();
	}

	// Can cover 127 bits
	public static String getIp6FromRange(String ip6Range) {
    	String[] ips = ip6Range.split("-");
    	String startIp = ips[0];
    	IPv6Address start = IPv6Address.fromString(startIp);
    	BigInteger gap = countIp6InRange(ip6Range);
    	BigInteger next = new BigInteger(gap.bitLength(), _rand);
    	while (next.compareTo(gap) >= 0) {
    		next = new BigInteger(gap.bitLength(), _rand);
    	}
    	BigInteger startInt = convertIPv6AddressToBigInteger(start);
    	BigInteger resultInt = startInt.add(next);
    	InetAddress resultAddr;
		try {
			resultAddr = InetAddress.getByAddress(resultInt.toByteArray());
		} catch (UnknownHostException e) {
			return null;
		}
    	IPv6Address ip = IPv6Address.fromInetAddress(resultAddr);
    	return ip.toString();
	}

	//RFC3315, section 9.4
	public static String getDuidLL(String macAddress) {
		String duid = "00:03:00:01:" + macAddress;
		return duid;
	}
	
	private static BigInteger convertIPv6AddressToBigInteger(IPv6Address addr) {
		InetAddress inetAddr;
		try {
			inetAddr = addr.toInetAddress();
		} catch (UnknownHostException e) {
			return null;
		}
		return new BigInteger(inetAddr.getAddress());
	}
	
	// Can cover 127 bits
	public static BigInteger countIp6InRange(String ip6Range) {
		if (ip6Range == null) {
			return null;
		}
    	String[] ips = ip6Range.split("-");
    	String startIp = ips[0];
    	String endIp = ips[0];
    	if (ips.length > 1) {
    		endIp = ips[1];
    	}
    	IPv6Address start, end;
    	try {
    		start = IPv6Address.fromString(startIp);
    		end = IPv6Address.fromString(endIp);
		} catch (IllegalArgumentException ex) {
			return null;
		}
    	BigInteger startInt = convertIPv6AddressToBigInteger(start);
    	BigInteger endInt = convertIPv6AddressToBigInteger(end);
    	if (startInt.compareTo(endInt) > 0) {
    		return null;
    	}
    	return endInt.subtract(startInt).add(BigInteger.ONE);
	}

	public static boolean isIp6InRange(String ip6, String ip6Range) {
		if (ip6Range == null) {
			return false;
		}
    	String[] ips = ip6Range.split("-");
    	String startIp = ips[0];
    	String endIp = null;
    	if (ips.length > 1) {
    		endIp = ips[1];
    	}
    	IPv6Address start = IPv6Address.fromString(startIp);
    	IPv6Address end = IPv6Address.fromString(endIp);
    	IPv6Address ip = IPv6Address.fromString(ip6);
    	if (start.compareTo(ip) <= 0 && end.compareTo(ip) >= 0) {
    		return true;
    	}
		return false;
	}
	
	public static boolean isIp6InNetwork(String ip6, String ip6Cidr) {
		IPv6Network network = null;
		try {
			network = IPv6Network.fromString(ip6Cidr);
		} catch (IllegalArgumentException ex) {
			return false;
		}
    	IPv6Address ip = IPv6Address.fromString(ip6);
		return network.contains(ip);
	}
	
	public static boolean isIp6RangeOverlap(String ipRange1, String ipRange2) {
		String[] ips = ipRange1.split("-");
    	String startIp1 = ips[0];
    	String endIp1 = null;
    	if (ips.length > 1) {
    		endIp1 = ips[1];
    	}
    	IPv6Address start1 = IPv6Address.fromString(startIp1);
    	IPv6Address end1 = IPv6Address.fromString(endIp1);
    	IPv6AddressRange range1 = IPv6AddressRange.fromFirstAndLast(start1, end1);
		ips = ipRange2.split("-");
    	String startIp2 = ips[0];
    	String endIp2 = null;
    	if (ips.length > 1) {
    		endIp2 = ips[1];
    	}
    	IPv6Address start2 = IPv6Address.fromString(startIp2);
    	IPv6Address end2 = IPv6Address.fromString(endIp2);
    	IPv6AddressRange range2 = IPv6AddressRange.fromFirstAndLast(start2, end2);
    	return range1.overlaps(range2);
	}

	public static String getNextIp6InRange(String currentIp, String ipRange) {
		String[] ips = ipRange.split("-");
    	String startIp = ips[0];
    	String endIp = null;
    	if (ips.length > 1) {
    		endIp = ips[1];
    	}
    	IPv6Address start = IPv6Address.fromString(startIp);
    	IPv6Address end = IPv6Address.fromString(endIp);
    	IPv6Address current = IPv6Address.fromString(currentIp); 
    	IPv6Address result = null;
    	if (current.equals(end)) {
    		result = start;
    	} else{
    		result = current.add(1);
    	}
    	String resultIp = null;
    	if (result != null) {
    		resultIp = result.toString();
    	}
		return resultIp;
	}

    public static boolean isValidVlan(String vlan) {
        try {
            int vnet = Integer.parseInt(vlan);
            if (vnet < 0 || vnet > 4096) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

	public static URI generateUriForPvlan(String primaryVlan, String isolatedPvlan) {
        return URI.create("pvlan://" + primaryVlan + "-i" + isolatedPvlan);
	}
	
	public static String getPrimaryPvlanFromUri(URI uri) {
		String[] vlans = uri.getHost().split("-");
		if (vlans.length < 1) {
			return null;
		}
		return vlans[0];
	}
	
	public static String getIsolatedPvlanFromUri(URI uri) {
		String[] vlans = uri.getHost().split("-");
		if (vlans.length < 2) {
			return null;
		}
		for (String vlan : vlans) {
			if (vlan.startsWith("i")) {
				return vlan.replace("i", " ").trim();
			}
		}
		return null;
	}

	public static String generateMacOnIncrease(String baseMac, long l) {
		long mac = mac2Long(baseMac);
		if (l > 0xFFFFl) {
			return null;
		}
		mac = mac + (l << 24);
		mac = mac & 0x06FFFFFFFFFFl;
		return long2Mac(mac);
	}
}
