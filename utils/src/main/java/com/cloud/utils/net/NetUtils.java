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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.log4j.Logger;

import com.cloud.utils.IteratorUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;
import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.googlecode.ipv6.IPv6Network;

public class NetUtils {
    protected final static Logger s_logger = Logger.getLogger(NetUtils.class);

    private static final int MAX_CIDR = 32;
    private static final int RFC_3021_31_BIT_CIDR = 31;

    public final static String HTTP_PORT = "80";
    public final static String HTTPS_PORT = "443";
    public final static int VPN_PORT = 500;
    public final static int VPN_NATT_PORT = 4500;
    public final static int VPN_L2TP_PORT = 1701;
    public final static int HAPROXY_STATS_PORT = 8081;

    public final static String UDP_PROTO = "udp";
    public final static String TCP_PROTO = "tcp";
    public final static String ANY_PROTO = "any";
    public final static String ICMP_PROTO = "icmp";
    public final static String ALL_PROTO = "all";
    public final static String HTTP_PROTO = "http";
    public final static String SSL_PROTO = "ssl";

    public final static String ALL_CIDRS = "0.0.0.0/0";
    public final static int PORT_RANGE_MIN = 0;
    public final static int PORT_RANGE_MAX = 65535;

    public final static int DEFAULT_AUTOSCALE_VM_DESTROY_TIME = 2 * 60; // Grace period before Vm is destroyed
    public final static int DEFAULT_AUTOSCALE_POLICY_INTERVAL_TIME = 30;
    public final static int DEFAULT_AUTOSCALE_POLICY_QUIET_TIME = 5 * 60;
    private final static Random s_rand = new Random(System.currentTimeMillis());

    public static long createSequenceBasedMacAddress(final long macAddress) {
        return macAddress | 0x060000000000l | (long)s_rand.nextInt(32768) << 25 & 0x00fffe000000l;
    }

    public static String getHostName() {
        try {
            final InetAddress localAddr = InetAddress.getLocalHost();
            if (localAddr != null) {
                return localAddr.getHostName();
            }
        } catch (final UnknownHostException e) {
            s_logger.warn("UnknownHostException when trying to get host name. ", e);
        }
        return "localhost";
    }

    public static InetAddress getLocalInetAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            s_logger.warn("UnknownHostException in getLocalInetAddress().", e);
            return null;
        }
    }

    public static String resolveToIp(final String host) {
        try {
            final InetAddress addr = InetAddress.getByName(host);
            return ipFromInetAddress(addr);
        } catch (final UnknownHostException e) {
            s_logger.warn("Unable to resolve " + host + " to IP due to UnknownHostException");
            return null;
        }
    }

    public static InetAddress[] getAllLocalInetAddresses() {
        final List<InetAddress> addrList = new ArrayList<InetAddress>();
        try {
            for (final NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
                if (ifc.isUp() && !ifc.isVirtual()) {
                    for (final InetAddress addr : IteratorUtil.enumerationAsIterable(ifc.getInetAddresses())) {
                        addrList.add(addr);
                    }
                }
            }
        } catch (final SocketException e) {
            s_logger.warn("SocketException in getAllLocalInetAddresses().", e);
        }

        final InetAddress[] addrs = new InetAddress[addrList.size()];
        if (addrList.size() > 0) {
            System.arraycopy(addrList.toArray(), 0, addrs, 0, addrList.size());
        }
        return addrs;
    }

    public static String[] getLocalCidrs() {
        final String defaultHostIp = getDefaultHostIp();

        final List<String> cidrList = new ArrayList<String>();
        try {
            for (final NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
                if (ifc.isUp() && !ifc.isVirtual() && !ifc.isLoopback()) {
                    for (final InterfaceAddress address : ifc.getInterfaceAddresses()) {
                        final InetAddress addr = address.getAddress();
                        final int prefixLength = address.getNetworkPrefixLength();
                        if (prefixLength < MAX_CIDR && prefixLength > 0) {
                            final String ip = ipFromInetAddress(addr);
                            if (ip.equalsIgnoreCase(defaultHostIp)) {
                                cidrList.add(ipAndNetMaskToCidr(ip, getCidrNetmask(prefixLength)));
                            }
                        }
                    }
                }
            }
        } catch (final SocketException e) {
            s_logger.warn("UnknownHostException in getLocalCidrs().", e);
        }

        return cidrList.toArray(new String[0]);
    }

    public static String getDefaultHostIp() {
        if (SystemUtils.IS_OS_WINDOWS) {
            final Pattern pattern = Pattern.compile("\\s*0.0.0.0\\s*0.0.0.0\\s*(\\S*)\\s*(\\S*)\\s*");
            try {
                final Process result = Runtime.getRuntime().exec("route print -4");
                final BufferedReader output = new BufferedReader(new InputStreamReader(result.getInputStream()));

                String line = output.readLine();
                while (line != null) {
                    final Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(2);
                    }
                    line = output.readLine();
                }
            } catch (final IOException e) {
                s_logger.debug("Caught IOException", e);
            }
            return null;
        } else {
            NetworkInterface nic = null;
            final String pubNic = getDefaultEthDevice();

            if (pubNic == null) {
                return null;
            }

            try {
                nic = NetworkInterface.getByName(pubNic);
            } catch (final SocketException e) {
                return null;
            }

            String[] info = null;
            try {
                info = NetUtils.getNetworkParams(nic);
            } catch (final NullPointerException ignored) {
                s_logger.debug("Caught NullPointerException when trying to getDefaultHostIp");
            }
            if (info != null) {
                return info[0];
            }
            return null;
        }
    }

    public static String getDefaultEthDevice() {
        if (SystemUtils.IS_OS_MAC) {
            final String defDev = Script.runSimpleBashScript("/sbin/route -n get default 2> /dev/null | grep interface | awk '{print $2}'");
            return defDev;
        }
        final String defaultRoute = Script.runSimpleBashScript("/sbin/route | grep default");

        if (defaultRoute == null) {
            return null;
        }

        final String[] defaultRouteList = defaultRoute.split("\\s+");

        if (defaultRouteList.length != 8) {
            return null;
        }

        return defaultRouteList[7];
    }

    public static InetAddress getFirstNonLoopbackLocalInetAddress() {
        final InetAddress[] addrs = getAllLocalInetAddresses();
        if (addrs != null) {
            for (final InetAddress addr : addrs) {
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

    public static InetAddress[] getInterfaceInetAddresses(final String ifName) {
        final List<InetAddress> addrList = new ArrayList<InetAddress>();
        try {
            for (final NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
                if (ifc.isUp() && !ifc.isVirtual() && ifc.getName().equals(ifName)) {
                    for (final InetAddress addr : IteratorUtil.enumerationAsIterable(ifc.getInetAddresses())) {
                        addrList.add(addr);
                    }
                }
            }
        } catch (final SocketException e) {
            s_logger.warn("SocketException in getAllLocalInetAddresses().", e);
        }

        final InetAddress[] addrs = new InetAddress[addrList.size()];
        if (addrList.size() > 0) {
            System.arraycopy(addrList.toArray(), 0, addrs, 0, addrList.size());
        }
        return addrs;
    }

    public static String getLocalIPString() {
        final InetAddress addr = getLocalInetAddress();
        if (addr != null) {
            return ipFromInetAddress(addr);
        }

        return "127.0.0.1";
    }

    public static String ipFromInetAddress(final InetAddress addr) {
        assert addr != null;

        final byte[] ipBytes = addr.getAddress();
        final StringBuffer sb = new StringBuffer();
        sb.append(ipBytes[0] & 0xff).append(".");
        sb.append(ipBytes[1] & 0xff).append(".");
        sb.append(ipBytes[2] & 0xff).append(".");
        sb.append(ipBytes[3] & 0xff);

        return sb.toString();
    }

    public static boolean isLocalAddress(final InetAddress addr) {
        final InetAddress[] addrs = getAllLocalInetAddresses();

        if (addrs != null) {
            for (final InetAddress self : addrs) {
                if (self.equals(addr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLocalAddress(final String strAddress) {

        InetAddress addr;
        try {
            addr = InetAddress.getByName(strAddress);
            return isLocalAddress(addr);
        } catch (final UnknownHostException e) {
        }
        return false;
    }

    public static String getMacAddress(final InetAddress address) {
        final StringBuffer sb = new StringBuffer();
        final Formatter formatter = new Formatter(sb);
        try {
            final NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            final byte[] mac = ni.getHardwareAddress();

            for (int i = 0; i < mac.length; i++) {
                formatter.format("%02X%s", mac[i], i < mac.length - 1 ? ":" : "");
            }
        } catch (final SocketException e) {
            s_logger.error("SocketException when trying to retrieve MAC address", e);
        } finally {
            formatter.close();
        }
        return sb.toString();
    }

    public static long getMacAddressAsLong(final InetAddress address) {
        long macAddressAsLong = 0;
        try {
            final NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            final byte[] mac = ni.getHardwareAddress();

            for (int i = 0; i < mac.length; i++) {
                macAddressAsLong |= (long)(mac[i] & 0xff) << (mac.length - i - 1) * 8;
            }

        } catch (final SocketException e) {
            s_logger.error("SocketException when trying to retrieve MAC address", e);
        }

        return macAddressAsLong;
    }

    /**
     * This method will fail in case we have a 31 Bit prefix network
     * See RFC 3021.
     *
     * In order to avoid calling this method, please check the <code>NetUtils.is31PrefixCidr(cidr)</code> first.
     */
    public static boolean ipRangesOverlap(final String startIp1, final String endIp1, final String startIp2, final String endIp2) {
        final long startIp1Long = ip2Long(startIp1);
        long endIp1Long = startIp1Long;
        if (endIp1 != null) {
            endIp1Long = ip2Long(endIp1);
        }
        final long startIp2Long = ip2Long(startIp2);
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

    public static long ip2Long(final String ip) {
        final String[] tokens = ip.split("[.]");
        assert tokens.length == 4;
        long result = 0;
        for (int i = 0; i < tokens.length; i++) {
            try {
                result = result << 8 | Integer.parseInt(tokens[i]);
            } catch (final NumberFormatException e) {
                throw new RuntimeException("Incorrect number", e);
            }
        }

        return result;
    }

    public static String long2Ip(final long ip) {
        final StringBuilder result = new StringBuilder(15);
        result.append(ip >> 24 & 0xff).append(".");
        result.append(ip >> 16 & 0xff).append(".");
        result.append(ip >> 8 & 0xff).append(".");
        result.append(ip & 0xff);

        return result.toString();
    }

    public static long mac2Long(final String macAddress) {
        final String[] tokens = macAddress.split(":");
        assert tokens.length == 6;
        long result = 0;
        for (int i = 0; i < tokens.length; i++) {
            result = result << 8;
            result |= Integer.parseInt(tokens[i], 16);
        }
        return result;
    }

    public static String[] getNicParams(final String nicName) {
        try {
            final NetworkInterface nic = NetworkInterface.getByName(nicName);
            return getNetworkParams(nic);
        } catch (final SocketException e) {
            return null;
        }
    }

    public static String[] getNetworkParams(final NetworkInterface nic) {
        final List<InterfaceAddress> addrs = nic.getInterfaceAddresses();
        if (addrs == null || addrs.size() == 0) {
            return null;
        }
        InterfaceAddress addr = null;
        for (final InterfaceAddress iaddr : addrs) {
            final InetAddress inet = iaddr.getAddress();
            if (!inet.isLinkLocalAddress() && !inet.isLoopbackAddress() && !inet.isMulticastAddress() && inet.getAddress().length == 4) {
                addr = iaddr;
                break;
            }
        }
        if (addr == null) {
            return null;
        }
        final String[] result = new String[3];
        result[0] = addr.getAddress().getHostAddress();
        try {
            final byte[] mac = nic.getHardwareAddress();
            result[1] = byte2Mac(mac);
        } catch (final SocketException e) {
            s_logger.debug("Caught exception when trying to get the mac address ", e);
        }

        result[2] = prefix2Netmask(addr.getNetworkPrefixLength());
        return result;
    }

    public static String prefix2Netmask(final short prefix) {
        long addr = 0;
        for (int i = 0; i < prefix; i++) {
            addr = addr | 1 << 31 - i;
        }

        return long2Ip(addr);
    }

    public static String byte2Mac(final byte[] m) {
        final StringBuilder result = new StringBuilder(17);
        final Formatter formatter = new Formatter(result);
        formatter.format("%02x:%02x:%02x:%02x:%02x:%02x", m[0], m[1], m[2], m[3], m[4], m[5]);
        formatter.close();
        return result.toString();
    }

    public static String long2Mac(final long macAddress) {
        final StringBuilder result = new StringBuilder(17);
        try (Formatter formatter = new Formatter(result)) {
            formatter.format("%02x:%02x:%02x:%02x:%02x:%02x",
                    macAddress >> 40 & 0xff, macAddress >> 32 & 0xff,
                    macAddress >> 24 & 0xff, macAddress >> 16 & 0xff,
                    macAddress >> 8 & 0xff, macAddress & 0xff);
        }
        return result.toString();
    }

    public static boolean isValidPrivateIp(final String ipAddress, final String guestIPAddress) {

        final InetAddress privIp = parseIpAddress(ipAddress);
        if (privIp == null) {
            return false;
        }
        if (!privIp.isSiteLocalAddress()) {
            return false;
        }

        String firstGuestOctet = "10";
        if (guestIPAddress != null && !guestIPAddress.isEmpty()) {
            final String[] guestIPList = guestIPAddress.split("\\.");
            firstGuestOctet = guestIPList[0];
        }

        final String[] ipList = ipAddress.split("\\.");
        if (!ipList[0].equals(firstGuestOctet)) {
            return false;
        }

        return true;
    }

    public static boolean isSiteLocalAddress(final String ipAddress) {
        if (ipAddress == null) {
            return false;
        } else {
            final InetAddress ip = parseIpAddress(ipAddress);
            if(ip != null) {
                return ip.isSiteLocalAddress();
            }
            return false;
        }
    }

    public static boolean validIpRange(final String startIP, final String endIP) {
        if (endIP == null || endIP.isEmpty()) {
            return true;
        }

        final long startIPLong = NetUtils.ip2Long(startIP);
        final long endIPLong = NetUtils.ip2Long(endIP);
        return startIPLong <= endIPLong;
    }

    public static boolean isValidIp(final String ip) {
        final InetAddressValidator validator = InetAddressValidator.getInstance();

        return validator.isValidInet4Address(ip);
    }

    public static boolean is31PrefixCidr(final String cidr) {
        final boolean isValidCird = isValidCIDR(cidr);
        if (isValidCird){
            final String[] cidrPair = cidr.split("\\/");
            final String cidrSize = cidrPair[1];

            final int cidrSizeNum = Integer.parseInt(cidrSize);
            if (cidrSizeNum == RFC_3021_31_BIT_CIDR) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidCIDR(final String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return false;
        }
        final String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) {
            return false;
        }
        final String cidrAddress = cidrPair[0];
        final String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) {
            return false;
        }
        int cidrSizeNum = -1;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (final Exception e) {
            return false;
        }

        if (cidrSizeNum < 0 || cidrSizeNum > MAX_CIDR) {
            return false;
        }

        return true;
    }

    public static boolean isValidNetmask(final String netmask) {
        if (!isValidIp(netmask)) {
            return false;
        }

        final long ip = ip2Long(netmask);
        int count = 0;
        boolean finished = false;
        for (int i = 31; i >= 0; i--) {
            if ((ip >> i & 0x1) == 0) {
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

    private static InetAddress parseIpAddress(final String address) {
        final StringTokenizer st = new StringTokenizer(address, ".");
        final byte[] bytes = new byte[4];

        if (st.countTokens() == 4) {
            try {
                for (int i = 0; i < 4; i++) {
                    bytes[i] = (byte)Integer.parseInt(st.nextToken());
                }
                return InetAddress.getByAddress(address, bytes);
            } catch (final NumberFormatException nfe) {
                return null;
            } catch (final UnknownHostException uhe) {
                return null;
            }
        }
        return null;
    }

    public static String getCidrFromGatewayAndNetmask(final String gatewayStr, final String netmaskStr) {
        final long netmask = ip2Long(netmaskStr);
        final long gateway = ip2Long(gatewayStr);
        final long firstPart = gateway & netmask;
        final long size = getCidrSize(netmaskStr);
        return long2Ip(firstPart) + "/" + size;
    }

    public static String[] getIpRangeFromCidr(final String cidr, final long size) {
        assert size < MAX_CIDR : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;
        final String[] result = new String[2];
        final long ip = ip2Long(cidr);
        final long startNetMask = ip2Long(getCidrNetmask(size));
        final long start = (ip & startNetMask) + 1;
        long end = start;

        end = end >> MAX_CIDR - size;

        end++;
        end = (end << MAX_CIDR - size) - 2;

        result[0] = long2Ip(start);
        result[1] = long2Ip(end);

        return result;
    }

    public static Set<Long> getAllIpsFromCidr(final String cidr, final long size, final Set<Long> usedIps) {
        assert size < MAX_CIDR : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;
        final Set<Long> result = new TreeSet<Long>();
        final long ip = ip2Long(cidr);
        final long startNetMask = ip2Long(getCidrNetmask(size));
        long start = (ip & startNetMask) + 1;
        long end = start;

        end = end >> MAX_CIDR - size;

        end++;
        end = (end << MAX_CIDR - size) - 2;
        int maxIps = 255; // get 255 ips as maximum
        while (start <= end && maxIps > 0) {
            if (!usedIps.contains(start)) {
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
    public static long getRandomIpFromCidr(final String startIp, final int size, final SortedSet<Long> avoid) {
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
    public static long getRandomIpFromCidr(final long cidr, final int size, final SortedSet<Long> avoid) {
        assert size < MAX_CIDR : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;

        final long startNetMask = ip2Long(getCidrNetmask(size));
        final long startIp = (cidr & startNetMask) + 1; //exclude the first ip since it isnt valid, e.g., 192.168.10.0
        int range = 1 << MAX_CIDR - size; //e.g., /24 = 2^8 = 256
        range = range - 1; //exclude end of the range since that is the broadcast address, e.g., 192.168.10.255

        if (avoid.size() >= range) {
            return -1;
        }

        //Reduce the range by the size of the avoid set
        //e.g., cidr = 192.168.10.0, size = /24, avoid = 192.168.10.1, 192.168.10.20, 192.168.10.254
        // range = 2^8 - 1 - 3 = 252
        range = range - avoid.size();
        final int next = s_rand.nextInt(range); //note: nextInt excludes last value
        long ip = startIp + next;
        for (final Long avoidable : avoid) {
            if (ip >= avoidable) {
                ip++;
            } else {
                break;
            }
        }

        return ip;
    }

    public static String getIpRangeStartIpFromCidr(final String cidr, final long size) {
        final long ip = ip2Long(cidr);
        final long startNetMask = ip2Long(getCidrNetmask(size));
        final long start = (ip & startNetMask) + 1;
        return long2Ip(start);
    }

    public static String getIpRangeEndIpFromCidr(final String cidr, final long size) {
        final long ip = ip2Long(cidr);
        final long startNetMask = ip2Long(getCidrNetmask(size));
        final long start = (ip & startNetMask) + 1;
        long end = start;
        end = end >> MAX_CIDR - size;

        end++;
        end = (end << MAX_CIDR - size) - 2;
        return long2Ip(end);
    }

    public static boolean sameSubnet(final String ip1, final String ip2, final String netmask) {
        if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) {
            return true;
        }
        final String subnet1 = NetUtils.getSubNet(ip1, netmask);
        final String subnet2 = NetUtils.getSubNet(ip2, netmask);

        return subnet1.equals(subnet2);
    }

    public static boolean sameSubnetCIDR(final String ip1, final String ip2, final long cidrSize) {
        if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) {
            return true;
        }
        final String subnet1 = NetUtils.getCidrSubNet(ip1, cidrSize);
        final String subnet2 = NetUtils.getCidrSubNet(ip2, cidrSize);

        return subnet1.equals(subnet2);
    }

    public static String getSubNet(final String ip, final String netmask) {
        final long ipAddr = ip2Long(ip);
        final long subnet = ip2Long(netmask);
        final long result = ipAddr & subnet;
        return long2Ip(result);
    }

    public static String getCidrSubNet(final String ip, final long cidrSize) {
        final long numericNetmask = 0xffffffff >> MAX_CIDR - cidrSize << MAX_CIDR - cidrSize;
        final String netmask = NetUtils.long2Ip(numericNetmask);
        return getSubNet(ip, netmask);
    }

    public static String ipAndNetMaskToCidr(final String ip, final String netmask) {
        if (!isValidIp(ip)) {
            return null;
        }

        if (!isValidNetmask(netmask)) {
            return null;
        }

        final long ipAddr = ip2Long(ip);
        final long subnet = ip2Long(netmask);
        final long result = ipAddr & subnet;
        int bits = subnet == 0 ? 0 : 1;
        long subnet2 = subnet;
        while ((subnet2 = subnet2 >> 1 & subnet) != 0) {
            bits++;
        }

        return long2Ip(result) + "/" + Integer.toString(bits);
    }

    public static String[] ipAndNetMaskToRange(final String ip, final String netmask) {
        final long ipAddr = ip2Long(ip);
        long subnet = ip2Long(netmask);
        final long start = (ipAddr & subnet) + 1;
        long end = start;
        int bits = subnet == 0 ? 0 : 1;
        while ((subnet = subnet >> 1 & subnet) != 0) {
            bits++;
        }
        end = end >> MAX_CIDR - bits;

        end++;
        end = (end << MAX_CIDR - bits) - 2;

        return new String[] {long2Ip(start), long2Ip(end)};

    }

    public static Pair<String, Integer> getCidr(final String cidr) {
        final String[] tokens = cidr.split("/");
        return new Pair<String, Integer>(tokens[0], Integer.parseInt(tokens[1]));
    }

    public static enum SupersetOrSubset {
        isSuperset, isSubset, neitherSubetNorSuperset, sameSubnet, errorInCidrFormat
    }

    public static SupersetOrSubset isNetowrkASubsetOrSupersetOfNetworkB(final String cidrA, final String cidrB) {
        final Long[] cidrALong = cidrToLong(cidrA);
        final Long[] cidrBLong = cidrToLong(cidrB);
        long shift = 0;
        if (cidrALong == null || cidrBLong == null) {
            //implies error in the cidr format
            return SupersetOrSubset.errorInCidrFormat;
        }
        if (cidrALong[1] >= cidrBLong[1]) {
            shift = MAX_CIDR - cidrBLong[1];
        } else {
            shift = MAX_CIDR - cidrALong[1];
        }
        final long result = (cidrALong[0] >> shift) - (cidrBLong[0] >> shift);
        if (result == 0) {
            if (cidrALong[1] < cidrBLong[1]) {
                //this implies cidrA is super set of cidrB
                return SupersetOrSubset.isSuperset;
            } else if (cidrALong[1].equals(cidrBLong[1])) {
                //this implies both the cidrs are equal
                return SupersetOrSubset.sameSubnet;
            }
            // implies cidrA is subset of cidrB
            return SupersetOrSubset.isSubset;
        }
        //this implies no overlap.
        return SupersetOrSubset.neitherSubetNorSuperset;
    }

    public static boolean isNetworkAWithinNetworkB(final String cidrA, final String cidrB) {
        final Long[] cidrALong = cidrToLong(cidrA);
        final Long[] cidrBLong = cidrToLong(cidrB);
        if (cidrALong == null || cidrBLong == null) {
            return false;
        }
        final long shift = MAX_CIDR - cidrBLong[1];
        return cidrALong[0] >> shift == cidrBLong[0] >> shift;
    }

    public static Long[] cidrToLong(final String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return null;
        }
        final String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) {
            return null;
        }
        final String cidrAddress = cidrPair[0];
        final String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) {
            return null;
        }
        int cidrSizeNum = -1;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (final Exception e) {
            return null;
        }
        final long numericNetmask = 0xffffffff >> MAX_CIDR - cidrSizeNum << MAX_CIDR - cidrSizeNum;
        final long ipAddr = ip2Long(cidrAddress);
        final Long[] cidrlong = {ipAddr & numericNetmask, (long)cidrSizeNum};
        return cidrlong;

    }

    public static String getCidrSubNet(final String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return null;
        }
        final String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) {
            return null;
        }
        final String cidrAddress = cidrPair[0];
        final String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) {
            return null;
        }
        int cidrSizeNum = -1;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (final Exception e) {
            return null;
        }
        final long numericNetmask = 0xffffffff >> MAX_CIDR - cidrSizeNum << MAX_CIDR - cidrSizeNum;
        final String netmask = NetUtils.long2Ip(numericNetmask);
        return getSubNet(cidrAddress, netmask);
    }

    public static String getCidrNetmask(final long cidrSize) {
        final long numericNetmask = 0xffffffff >> MAX_CIDR - cidrSize << MAX_CIDR - cidrSize;
        return long2Ip(numericNetmask);
    }

    public static String getCidrNetmask(final String cidr) {
        final String[] cidrPair = cidr.split("\\/");
        final long guestCidrSize = Long.parseLong(cidrPair[1]);
        return getCidrNetmask(guestCidrSize);
    }

    public static String cidr2Netmask(final String cidr) {
        final String[] tokens = cidr.split("\\/");
        return getCidrNetmask(Integer.parseInt(tokens[1]));
    }

    public static long getCidrSize(final String netmask) {
        final long ip = ip2Long(netmask);
        int count = 0;
        for (int i = 0; i < MAX_CIDR; i++) {
            if ((ip >> i & 0x1) == 0) {
                count++;
            } else {
                break;
            }
        }

        return MAX_CIDR - count;
    }

    public static boolean isValidPort(final String p) {
        try {
            final int port = Integer.parseInt(p);
            return !(port > 65535 || port < 1);
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidPort(final int p) {
        return !(p > 65535 || p < 1);
    }

    public static boolean isValidLBPort(final String p) {
        try {
            final int port = Integer.parseInt(p);
            return !(port > 65535 || port < 1);
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidProto(final String p) {
        final String proto = p.toLowerCase();
        return proto.equals(TCP_PROTO) || proto.equals(UDP_PROTO) || proto.equals(ICMP_PROTO);
    }

    public static boolean isValidSecurityGroupProto(final String p) {
        final String proto = p.toLowerCase();
        return proto.equals(TCP_PROTO) || proto.equals(UDP_PROTO) || proto.equals(ICMP_PROTO) || proto.equals(ALL_PROTO);
    }

    public static boolean isValidAlgorithm(final String p) {
        final String algo = p.toLowerCase();
        return algo.equals("roundrobin") || algo.equals("leastconn") || algo.equals("source");
    }

    public static boolean isValidAutoScaleAction(final String p) {
        final String action = p.toLowerCase();
        return action.equals("scaleup") || action.equals("scaledown");
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

    public static String[] getLinkLocalIPRange(final int size) {
        if (size > 16 || size <= 0) {
            return null;
        }
        /* reserve gateway */
        final String[] range = getIpRangeFromCidr(getLinkLocalGateway(), MAX_CIDR - size);

        if (range[0].equalsIgnoreCase(getLinkLocalGateway())) {
            /* remove the gateway */
            long ip = ip2Long(range[0]);
            ip += 1;
            range[0] = long2Ip(ip);
        }
        return range;
    }

    public static String getLinkLocalIpEnd() {
        final String[] cidrPair = getLinkLocalCIDR().split("\\/");
        final String cidr = cidrPair[0];

        return getIpRangeEndIpFromCidr(cidr, MAX_CIDR - Long.parseLong(cidrPair[1]));
    }

    public static String portRangeToString(final int portRange[]) {
        return Integer.toString(portRange[0]) + ":" + Integer.toString(portRange[1]);
    }

    public static boolean verifyDomainNameLabel(final String hostName, final boolean isHostName) {
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

    public static boolean verifyDomainName(final String domainName) {
        // don't allow domain name length to exceed 190 chars (190 + 63 (max host name length) = 253 = max domainName length
        if (domainName.length() < 1 || domainName.length() > 190) {
            s_logger.trace("Domain name must be between 1 and 190 characters long");
            return false;
        }

        if (domainName.startsWith(".") || domainName.endsWith(".")) {
            s_logger.trace("Domain name can't start or end with .");
            return false;
        }

        final String[] domainNameLabels = domainName.split("\\.");

        for (int i = 0; i < domainNameLabels.length; i++) {
            if (!verifyDomainNameLabel(domainNameLabels[i], false)) {
                s_logger.warn("Domain name label " + domainNameLabels[i] + " is incorrect");
                return false;
            }
        }

        return true;
    }

    public static String getDhcpRange(final String cidr) {
        final String[] splitResult = cidr.split("\\/");
        final long size = Long.parseLong(splitResult[1]);
        return NetUtils.getIpRangeStartIpFromCidr(splitResult[0], size);
    }

    // Check if 2 CIDRs have exactly same IP Range
    public static boolean isSameIpRange(final String cidrA, final String cidrB) {

        if (!NetUtils.isValidCIDR(cidrA)) {
            s_logger.info("Invalid value of cidr " + cidrA);
            return false;
        }
        if (!NetUtils.isValidCIDR(cidrB)) {
            s_logger.info("Invalid value of cidr " + cidrB);
            return false;
        }
        final String[] cidrPairFirst = cidrA.split("\\/");
        final String[] cidrPairSecond = cidrB.split("\\/");

        final Long networkSizeFirst = Long.valueOf(cidrPairFirst[1]);
        final Long networkSizeSecond = Long.valueOf(cidrPairSecond[1]);
        final String ipRangeFirst[] = NetUtils.getIpRangeFromCidr(cidrPairFirst[0], networkSizeFirst);
        final String ipRangeSecond[] = NetUtils.getIpRangeFromCidr(cidrPairFirst[0], networkSizeSecond);

        final long startIpFirst = NetUtils.ip2Long(ipRangeFirst[0]);
        final long endIpFirst = NetUtils.ip2Long(ipRangeFirst[1]);
        final long startIpSecond = NetUtils.ip2Long(ipRangeSecond[0]);
        final long endIpSecond = NetUtils.ip2Long(ipRangeSecond[1]);
        if (startIpFirst == startIpSecond && endIpFirst == endIpSecond) {
            return true;
        }
        return false;
    }

    public static boolean validateGuestCidr(final String cidr) {
        // RFC 1918 - The Internet Assigned Numbers Authority (IANA) has reserved the
        // following three blocks of the IP address space for private internets:
        // 10.0.0.0 - 10.255.255.255 (10/8 prefix)
        // 172.16.0.0 - 172.31.255.255 (172.16/12 prefix)
        // 192.168.0.0 - 192.168.255.255 (192.168/16 prefix)

        final String cidr1 = "10.0.0.0/8";
        final String cidr2 = "172.16.0.0/12";
        final String cidr3 = "192.168.0.0/16";

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

    public static boolean verifyInstanceName(final String instanceName) {
        //instance name for cloudstack vms shouldn't contain - and spaces
        if (instanceName.contains("-") || instanceName.contains(" ") || instanceName.contains("+")) {
            s_logger.warn("Instance name can not contain hyphen, spaces and \"+\" char");
            return false;
        }

        return true;
    }

    public static boolean isNetworksOverlap(final String cidrA, final String cidrB) {
        final Long[] cidrALong = cidrToLong(cidrA);
        final Long[] cidrBLong = cidrToLong(cidrB);
        if (cidrALong == null || cidrBLong == null) {
            return false;
        }
        final long shift = MAX_CIDR - (cidrALong[1] > cidrBLong[1] ? cidrBLong[1] : cidrALong[1]);
        return cidrALong[0] >> shift == cidrBLong[0] >> shift;
    }

    public static boolean isValidS2SVpnPolicy(final String policys) {
        if (policys == null || policys.isEmpty()) {
            return false;
        }
        for (final String policy : policys.split(",")) {
            if (policy.isEmpty()) {
                return false;
            }
            final String cipherHash = policy.split(";")[0];
            if (cipherHash.isEmpty()) {
                return false;
            }
            final String[] list = cipherHash.split("-");
            if (list.length != 2) {
                return false;
            }
            final String cipher = list[0];
            final String hash = list[1];
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

    public static boolean isValidCidrList(final String cidrList) {
        for (final String guestCidr : cidrList.split(",")) {
            if (!isValidCIDR(guestCidr)) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateGuestCidrList(final String guestCidrList) {
        for (final String guestCidr : guestCidrList.split(",")) {
            if (!validateGuestCidr(guestCidr)) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateIcmpType(final long icmpType) {
        //Source - http://www.erg.abdn.ac.uk/~gorry/course/inet-pages/icmp-code.html
        if (!(icmpType >= 0 && icmpType <= 255)) {
            s_logger.warn("impcType is not within 0-255 range");
            return false;
        }
        return true;
    }

    public static boolean validateIcmpCode(final long icmpCode) {

        //Source - http://www.erg.abdn.ac.uk/~gorry/course/inet-pages/icmp-code.html
        if (!(icmpCode >= 0 && icmpCode <= 15)) {
            s_logger.warn("Icmp code should be within 0-15 range");
            return false;
        }

        return true;
    }

    public static boolean isValidIpv6(final String ip) {
        try {
            IPv6Address.fromString(ip);
        } catch (final IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    public static boolean isValidIp6Cidr(final String ip6Cidr) {
        try {
            IPv6Network.fromString(ip6Cidr);
        } catch (final IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    public static int getIp6CidrSize(final String ip6Cidr) {
        IPv6Network network = null;
        try {
            network = IPv6Network.fromString(ip6Cidr);
        } catch (final IllegalArgumentException ex) {
            return 0;
        }
        return network.getNetmask().asPrefixLength();
    }

    // Can cover 127 bits
    public static String getIp6FromRange(final String ip6Range) {
        final String[] ips = ip6Range.split("-");
        final String startIp = ips[0];
        final IPv6Address start = IPv6Address.fromString(startIp);
        final BigInteger gap = countIp6InRange(ip6Range);
        BigInteger next = new BigInteger(gap.bitLength(), s_rand);
        while (next.compareTo(gap) >= 0) {
            next = new BigInteger(gap.bitLength(), s_rand);
        }
        InetAddress resultAddr = null;
        final BigInteger startInt = convertIPv6AddressToBigInteger(start);
        if (startInt != null) {
            final BigInteger resultInt = startInt.add(next);
            try {
                resultAddr = InetAddress.getByAddress(resultInt.toByteArray());
            } catch (final UnknownHostException e) {
                return null;
            }
        }
        if( resultAddr != null) {
            final IPv6Address ip = IPv6Address.fromInetAddress(resultAddr);
            return ip.toString();
        }
        return null;
    }

    //RFC3315, section 9.4
    public static String getDuidLL(final String macAddress) {
        final String duid = "00:03:00:01:" + macAddress;
        return duid;
    }

    private static BigInteger convertIPv6AddressToBigInteger(final IPv6Address addr) {
        InetAddress inetAddr;
        try {
            inetAddr = addr.toInetAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
        return new BigInteger(inetAddr.getAddress());
    }

    // Can cover 127 bits
    public static BigInteger countIp6InRange(final String ip6Range) {
        if (ip6Range == null) {
            return null;
        }
        final String[] ips = ip6Range.split("-");
        final String startIp = ips[0];
        String endIp = ips[0];
        if (ips.length > 1) {
            endIp = ips[1];
        }
        try {
            final BigInteger startInt = convertIPv6AddressToBigInteger(IPv6Address.fromString(startIp));
            final BigInteger endInt = convertIPv6AddressToBigInteger(IPv6Address.fromString(endIp));
            if (endInt != null && startInt != null && startInt.compareTo(endInt) <= 0) {
                return endInt.subtract(startInt).add(BigInteger.ONE);
            }
        } catch (final IllegalArgumentException ex) {
            s_logger.error("Failed to convert a string to an IPv6 address", ex);
        }
        return null;
    }

    public static boolean isIp6InRange(final String ip6, final String ip6Range) {
        if (ip6Range == null) {
            return false;
        }
        final String[] ips = ip6Range.split("-");
        final String startIp = ips[0];
        String endIp = null;
        if (ips.length > 1) {
            endIp = ips[1];
        }
        final IPv6Address start = IPv6Address.fromString(startIp);
        final IPv6Address end = IPv6Address.fromString(endIp);
        final IPv6Address ip = IPv6Address.fromString(ip6);
        if (start.compareTo(ip) <= 0 && end.compareTo(ip) >= 0) {
            return true;
        }
        return false;
    }

    public static boolean isIp6InNetwork(final String ip6, final String ip6Cidr) {
        IPv6Network network = null;
        try {
            network = IPv6Network.fromString(ip6Cidr);
        } catch (final IllegalArgumentException ex) {
            return false;
        }
        final IPv6Address ip = IPv6Address.fromString(ip6);
        return network.contains(ip);
    }

    public static boolean isIp6RangeOverlap(final String ipRange1, final String ipRange2) {
        String[] ips = ipRange1.split("-");
        final String startIp1 = ips[0];
        String endIp1 = null;
        if (ips.length > 1) {
            endIp1 = ips[1];
        }
        final IPv6Address start1 = IPv6Address.fromString(startIp1);
        final IPv6Address end1 = IPv6Address.fromString(endIp1);
        final IPv6AddressRange range1 = IPv6AddressRange.fromFirstAndLast(start1, end1);
        ips = ipRange2.split("-");
        final String startIp2 = ips[0];
        String endIp2 = null;
        if (ips.length > 1) {
            endIp2 = ips[1];
        }
        final IPv6Address start2 = IPv6Address.fromString(startIp2);
        final IPv6Address end2 = IPv6Address.fromString(endIp2);
        final IPv6AddressRange range2 = IPv6AddressRange.fromFirstAndLast(start2, end2);
        return range1.overlaps(range2);
    }

    public static String getNextIp6InRange(final String currentIp, final String ipRange) {
        final String[] ips = ipRange.split("-");
        final String startIp = ips[0];
        String endIp = null;
        if (ips.length > 1) {
            endIp = ips[1];
        }
        final IPv6Address start = IPv6Address.fromString(startIp);
        final IPv6Address end = IPv6Address.fromString(endIp);
        final IPv6Address current = IPv6Address.fromString(currentIp);
        IPv6Address result = null;
        if (current.equals(end)) {
            result = start;
        } else {
            result = current.add(1);
        }
        String resultIp = null;
        if (result != null) {
            resultIp = result.toString();
        }
        return resultIp;
    }

    public static String standardizeIp6Address(final String ip6Addr) {
        try {
            return IPv6Address.fromString(ip6Addr).toString();
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid IPv6 address: " + ex.getMessage());
        }
    }

    public static String standardizeIp6Cidr(final String ip6Cidr){
        try {
            return IPv6Network.fromString(ip6Cidr).toString();
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid IPv6 CIDR: " + ex.getMessage());
        }
    }

    static final String VLAN_PREFIX = "vlan://";
    static final int VLAN_PREFIX_LENGTH = VLAN_PREFIX.length();

    public static boolean isValidVlan(String vlan) {
        if (null == vlan || "".equals(vlan)) {
            return false;
        }
        if (vlan.startsWith(VLAN_PREFIX)) {
            vlan = vlan.substring(VLAN_PREFIX_LENGTH);
        }
        try {
            final int vnet = Integer.parseInt(vlan);
            if (vnet <= 0 || vnet >= 4095) { // the valid range is 1- 4094
                return false;
            }
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    static final String VLAN_UNTAGGED = "untagged";

    public static boolean isSameIsolationId(String one, String other) {
        // check nulls
        // check empty strings
        if ((one == null || one.isEmpty()) && (other == null || other.isEmpty())) {
            return true;
        }
        if (one == null || other == null) {
            return false;
        }
        // check 'untagged'
        if (one.contains(VLAN_UNTAGGED) && other.contains(VLAN_UNTAGGED)) {
            return true;
        }
        // if one is a number check the other as number and as 'vlan://' + number
        if (one.startsWith(VLAN_PREFIX)) {
            one = one.substring(VLAN_PREFIX_LENGTH);
        }
        if (other.startsWith(VLAN_PREFIX)) {
            other = other.substring(VLAN_PREFIX_LENGTH);
        }
        // check valid uris or numbers
        if (one.equalsIgnoreCase(other)) {
            return true;
        }

        return false;
    }

    // Attention maintainers: these pvlan functions should take into account code
    // in Networks.BroadcastDomainType, where URI construction is done for other
    // types of BroadcastDomainTypes
    public static URI generateUriForPvlan(final String primaryVlan, final String isolatedPvlan) {
        return URI.create("pvlan://" + primaryVlan + "-i" + isolatedPvlan);
    }

    public static String getPrimaryPvlanFromUri(final URI uri) {
        final String[] vlans = uri.getHost().split("-");
        if (vlans.length < 1) {
            return null;
        }
        return vlans[0];
    }

    public static String getIsolatedPvlanFromUri(final URI uri) {
        final String[] vlans = uri.getHost().split("-");
        if (vlans.length < 2) {
            return null;
        }
        for (final String vlan : vlans) {
            if (vlan.startsWith("i")) {
                return vlan.replace("i", " ").trim();
            }
        }
        return null;
    }

    public static String generateMacOnIncrease(final String baseMac, final long l) {
        long mac = mac2Long(baseMac);
        if (l > 0xFFFFl) {
            return null;
        }
        mac = mac + (l << 24);
        mac = mac & 0x06FFFFFFFFFFl;
        return long2Mac(mac);
    }

    public static boolean isIpWithtInCidrRange(final String ipAddress, final String cidr) {
        if (!isValidIp(ipAddress)) {
            return false;
        }
        if (!isValidCIDR(cidr)) {
            return false;
        }

        // check if the gatewayip is the part of the ip range being added.
        // RFC 3021 - 31-Bit Prefixes on IPv4 Point-to-Point Links
        //     GW              Netmask         Stat IP        End IP
        // 192.168.24.0 - 255.255.255.254 - 192.168.24.0 - 192.168.24.1
        // https://tools.ietf.org/html/rfc3021
        // Added by Wilder Rodrigues
        final SubnetUtils subnetUtils = new SubnetUtils(cidr);
        subnetUtils.setInclusiveHostCount(true);

        final boolean isInRange = subnetUtils.getInfo().isInRange(ipAddress);

        return isInRange;
    }

    public static Boolean IsIpEqualToNetworkOrBroadCastIp(final String requestedIp, final String cidr, final long size) {
        assert size < MAX_CIDR : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;

        final long ip = ip2Long(cidr);
        final long startNetMask = ip2Long(getCidrNetmask(size));

        final long start = ip & startNetMask;
        long end = start;

        end = end >> MAX_CIDR - size;

        end++;
        end = (end << MAX_CIDR - size) - 1;

        final long reqIp = ip2Long(requestedIp);
        if (reqIp == start || reqIp == end) {
            return true;
        }
        return false;
    }

}
