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

import java.io.File;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.utils.IteratorUtil;
import com.cloud.utils.NumbersUtil;

public class NetUtils {
    protected final static Logger s_logger = Logger.getLogger(NetUtils.class);

	public static String getHostName() {
        try {
            InetAddress localAddr = InetAddress.getLocalHost();
            if(localAddr != null) {
        		return localAddr.getHostName();
            }
        } catch(UnknownHostException e) {
        	s_logger.warn("UnknownHostException when trying to get host name. ", e);
        }
        return "localhost";
	}
	
    public static InetAddress getLocalInetAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch(UnknownHostException e) {
        	s_logger.warn("UnknownHostException in getLocalInetAddress().", e);
            return null;
        }
    }
    
    public static InetAddress[] getAllLocalInetAddresses() {
    	List<InetAddress> addrList = new ArrayList<InetAddress>();
    	try {
	    	for(NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
	    		if(ifc.isUp() && !ifc.isVirtual()) {
		    		for(InetAddress addr : IteratorUtil.enumerationAsIterable(ifc.getInetAddresses())) {
		    			addrList.add(addr);
		    		}
	    		}
	    	}
    	} catch (SocketException e) {
        	s_logger.warn("SocketException in getAllLocalInetAddresses().", e);
    	}
    	
    	InetAddress[] addrs = new InetAddress[addrList.size()];
    	if(addrList.size() > 0)
    		System.arraycopy(addrList.toArray(), 0, addrs, 0, addrList.size());
    	return addrs;
    }
    
    public static InetAddress getFirstNonLoopbackLocalInetAddress() {
    	InetAddress[] addrs = getAllLocalInetAddresses();
    	if(addrs != null) {
    		for(InetAddress addr : addrs) {
    			if(s_logger.isInfoEnabled())
    				s_logger.info("Check local InetAddress : " + addr.toString() + ", total count :" + addrs.length);
    			
    			if(!addr.isLoopbackAddress())
    				return addr;
    		}
    	}
    	
		s_logger.warn("Unable to determine a non-loopback address, local inet address count :" + addrs.length);
    	return null;
    }
    
    public static InetAddress[] getInterfaceInetAddresses(String ifName) {
    	List<InetAddress> addrList = new ArrayList<InetAddress>();
    	try {
	    	for(NetworkInterface ifc : IteratorUtil.enumerationAsIterable(NetworkInterface.getNetworkInterfaces())) {
	    		if(ifc.isUp() && !ifc.isVirtual() && ifc.getName().equals(ifName)) {
		    		for(InetAddress addr : IteratorUtil.enumerationAsIterable(ifc.getInetAddresses())) {
		    			addrList.add(addr);
		    		}
	    		}
	    	}
    	} catch (SocketException e) {
        	s_logger.warn("SocketException in getAllLocalInetAddresses().", e);
    	}
    	
    	InetAddress[] addrs = new InetAddress[addrList.size()];
    	if(addrList.size() > 0)
    		System.arraycopy(addrList.toArray(), 0, addrs, 0, addrList.size());
    	return addrs;
    }
    
    public static String getLocalIPString() {
        InetAddress addr = getLocalInetAddress();
        if(addr != null) {
            byte[] ipBytes = addr.getAddress();
            StringBuffer sb = new StringBuffer();
            sb.append(ipBytes[0] & 0xff).append(".");
            sb.append(ipBytes[1] & 0xff).append(".");
            sb.append(ipBytes[2] & 0xff).append(".");
            sb.append(ipBytes[3] & 0xff);

            return sb.toString();
        }

        return new String("127.0.0.1");
    }

    public static boolean isLocalAddress(InetAddress addr) {
		InetAddress[] addrs = getAllLocalInetAddresses();
		
		if(addrs != null) {
			for(InetAddress self : addrs) {
				if(self.equals(addr))
					return true;
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
	
			for (int i = 0; i < mac.length; i++)
				macAddressAsLong |= ((long)(mac[i] & 0xff) << (mac.length - i - 1)*8);
			 
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
        assert(tokens.length == 4);
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
        assert(tokens.length == 6);
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
            if (!inet.isLinkLocalAddress() &&
                !inet.isLoopbackAddress() &&
                !inet.isMulticastAddress() &&
                 inet.getAddress().length == 4) {
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
        formatter.format("%02x:%02x:%02x:%02x:%02x:%02x",
                         (macAddress >> 40) & 0xff,
                         (macAddress >> 32) & 0xff,
                         (macAddress >> 24) & 0xff,
                         (macAddress >> 16) & 0xff,
                         (macAddress >> 8) & 0xff,
                         (macAddress & 0xff));
        
        return result.toString();
    }
    
    public static boolean isValidPrivateIp(String ipAddress, String guestIPAddress) {
    	
    	InetAddress privIp = parseIpAddress(ipAddress);
    	if (privIp == null)
    		return false;
    	if (!privIp.isSiteLocalAddress()) {
    		return false;
    	}
    	
    	String firstGuestOctet = "10";
    	if (guestIPAddress != null && !guestIPAddress.isEmpty()) {
    		String[] guestIPList = guestIPAddress.split("\\.");
    		firstGuestOctet = guestIPList[0];
    	}
    	
    	String[] ipList = ipAddress.split("\\.");
    	if (!ipList[0].equals(firstGuestOctet)) return false;
    	
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
    	if (endIP == null || endIP.isEmpty()) return true;
    	
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong =  NetUtils.ip2Long(endIP);
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
	    	} catch(final Exception e) {
	    		return false;
	   		}
	    	// Each octet must be between 0 and 255, inclusive
	    	if (octet < 0 || octet > 255) return false;

	    	// Each octetString must have between 1 and 3 characters
	    	if (octetString.length() < 1 || octetString.length() > 3) return false;
	   		
	   	}
	    
	   	// IP is good, return true
		return true;
	}
	
	public static boolean isValidCIDR(final String cidr) {
		if (cidr == null || cidr.isEmpty()) return false;
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) return false;
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) return false;
        int cidrSizeNum = -1;
        
        try {
        	cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (Exception e) {
        	return false;
        }
        
        if (cidrSizeNum < 0 || cidrSizeNum > 32) return false;
        
        return true;
	}
	
	public static boolean isValidNetmask(String netmask) {
		if (!isValidIp(netmask))
			return false;
		
		long ip = ip2Long(netmask);
		int count = 0;
    	boolean finished = false;
    	for (int i = 31; i >= 0; i--) {
    		if (((ip >> i) & 0x1) == 0) {
    			finished = true;
    		} else {
    			if (finished)
    				return false;
    			count += 1;
    		}
    	}
    	
    	if (count == 0)
    		return false;
		
		return true;
	}
    
    private static InetAddress parseIpAddress(String address) {
    	StringTokenizer st = new StringTokenizer(address, ".");
		byte[] bytes = new byte[4];

		if (st.countTokens() == 4) {
			try {
				for (int i = 0; i < 4; i++){
					bytes[i] = (byte)Integer.parseInt(st.nextToken());
				}
				return InetAddress.getByAddress(address, bytes);
			} catch (NumberFormatException nfe) {
				return null;
			} catch (UnknownHostException uhe){
				return null;
			}
		}
		return null;
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
    
    public static Set<Long> getAllIpsFromCidr(String cidr, long size) {
        assert (size < 32) : "You do know this is not for ipv6 right?  Keep it smaller than 32 but you have " + size;
        Set<Long> result = new TreeSet<Long>();
        long ip = ip2Long(cidr);
        long startNetMask = ip2Long(getCidrNetmask(size));
        long start = (ip & startNetMask) + 2;
        long end = start;
        
        end = end >> (32 - size);
        
        end++;
        end = (end << (32 - size)) - 2;
        while (start <= end) {
        	result.add(start);
        	start++;
        }
        
        
        return result;
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
    	if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) return true;
    	String subnet1 = NetUtils.getSubNet(ip1, netmask);
    	String subnet2 = NetUtils.getSubNet(ip2, netmask);
    	
    	return (subnet1.equals(subnet2));
    }
    
    public static boolean sameSubnetCIDR(final String ip1, final String ip2, final long cidrSize) {
    	if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) return true;
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
    	int bits = (subnet == 0)?0:1;
    	long subnet2 = subnet;
    	while ((subnet2 = (subnet2 >> 1) & subnet) != 0 )
    		bits++;

    	return long2Ip(result) + "/" + Integer.toString(bits);
    }
    
    public static boolean isNetworkAWithinNetworkB(String cidrA, String cidrB) {
    	Long cidrALong = cidrToLong(cidrA);
    	Long cidrBLong = cidrToLong(cidrB);
    	if (cidrALong == null || cidrBLong == null) {
    		return false;
    	}
    	return (cidrALong.longValue() & cidrBLong.longValue()) == cidrBLong.longValue();
    }
    
    public static Long cidrToLong(String cidr) {
    	if (cidr == null || cidr.isEmpty()) return null;
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) return null;
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) return null;
        int cidrSizeNum = -1;
        
        try {
        	cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (Exception e) {
        	return null;
        }
    	long numericNetmask = (0xffffffff >> (32 - cidrSizeNum)) << (32 - cidrSizeNum);
    	long ipAddr = ip2Long(cidrAddress);
    	return ipAddr & numericNetmask;
    }
    
    public static String getCidrSubNet(String cidr) {
    	if (cidr == null || cidr.isEmpty()) return null;
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) return null;
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!isValidIp(cidrAddress)) return null;
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
    	return (proto.equals("tcp") || proto.equals("udp") || proto.equals("icmp"));
    }
    
    public static boolean isValidNetworkGroupProto(String p) {
    	String proto = p.toLowerCase();
    	return (proto.equals("tcp") || proto.equals("udp") || proto.equals("icmp") || proto.equals("all"));
    }
    
    public static boolean isValidAlgorithm(String p) {
    	String algo = p.toLowerCase();
    	return (algo.equals("roundrobin") || algo.equals("leastconn") || algo.equals("source"));
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
    	/*reserve gateway*/
    	return getIpRangeFromCidr(getLinkLocalGateway(), 32 - size);
    }
    
    public static String getLinkLocalIpEnd() {
    	String[] cidrPair = getLinkLocalCIDR().split("\\/");
		String cidr =  cidrPair[0];
		
    	return getIpRangeEndIpFromCidr(cidr, 32 - Long.parseLong(cidrPair[1]));
    }

    // test only
	private static void configLog4j() {
		URL configUrl = System.class.getResource("/conf/log4j-cloud.xml");
		if(configUrl != null) {
			System.out.println("Configure log4j using log4j-cloud.xml");

			try {
				File file = new File(configUrl.toURI());
				
				System.out.println("Log4j configuration from : " + file.getAbsolutePath());
				DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
			} catch (URISyntaxException e) {
				System.out.println("Unable to convert log4j configuration Url to URI");
			}
			// DOMConfigurator.configure(configUrl);
		} else {
			System.out.println("Configure log4j with default properties");
		}
	}
    
    public static void main(String[] args) {
    	configLog4j();
    	if (args.length == 0) {
    		System.out.println("Must specify at least one parameter");
    	}
        if (args[0].equals("m2l")) {
            System.out.println(mac2Long(args[1]));
        } else if (args[0].equals("l2m")) {
            System.out.println(long2Mac(NumbersUtil.parseLong(args[1], 0)));
        } else if (args[0].equals("i2l")) {
            System.out.println(ip2Long(args[1]));
        } else if (args[0].equals("nic")) {
            if (args.length < 2) {
                System.out.println("Needs the nic information");
                System.exit(1);
            }
            String[] result = getNicParams(args[1]);
            if (result == null) {
                System.out.println("Unable to get information for " + args[1]);
            } else {
                if (result[0] != null) {
                    System.out.println("Ip Address: " + result[0]);
                }
                if (result[1] != null) {
                    System.out.println("Mac Address: " + result[1]);
                }
                if (result[2] != null) {
                    System.out.println("Netmask: " + result[2]);
                }
            }
        } else if (args[0].equals("range")){
            if (args.length < 4) {
                String[] result = getIpRangeFromCidr(args[1], Long.parseLong(args[2]));
                System.out.println("Range is " + result[0] + "-" + result[1]);
            } else {
                System.err.println("Needs 3 parameters: " + args.length);
            }
        } else if (args[0].equals("ip2")) {
        	Set<Long> result = getAllIpsFromCidr("10.1.1.192", 24);
        	System.out.println("Number of ips: " + result.size());
        	
        } else if (args[0].equals("within")) {
        	String cidrA = args[1];
        	String cidrB = args[2];
        	System.out.println(NetUtils.isNetworkAWithinNetworkB(cidrA, cidrB));
        	
        }  else if (args[0].equals("tocidr")) {
        	String ip = args[1];
        	String mask = args[2];
        	System.out.println(NetUtils.ipAndNetMaskToCidr(ip, mask));
        	
        } else {
            System.out.println(long2Ip(NumbersUtil.parseLong(args[1], 0)));
        }
    }
    	
}

