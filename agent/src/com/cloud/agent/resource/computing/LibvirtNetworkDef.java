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
package com.cloud.agent.resource.computing;

import java.util.ArrayList;
import java.util.List;

public class LibvirtNetworkDef {
	enum netType {
		BRIDGE, NAT, LOCAL
	}

	private final String _networkName;
	private final String _uuid;
	private netType _networkType;
	private String _brName;
	private boolean _stp;
	private int _delay;
	private String _fwDev;
	private final String _domainName;
	private String _brIPAddr;
	private String _brNetMask;
	private final List<IPRange> ipranges = new ArrayList<IPRange>();
	private final List<dhcpMapping> dhcpMaps = new ArrayList<dhcpMapping>();

	public static class dhcpMapping {
		String _mac;
		String _name;
		String _ip;

		public dhcpMapping(String mac, String name, String ip) {
			_mac = mac;
			_name = name;
			_ip = ip;
		}
	}

	public static class IPRange {
		String _start;
		String _end;

		public IPRange(String start, String end) {
			_start = start;
			_end = end;
		}
	}

	public LibvirtNetworkDef(String netName, String uuid, String domName) {
		_networkName = netName;
		_uuid = uuid;
		_domainName = domName;
	}

	public void defNATNetwork(String brName, boolean stp, int delay,
			String fwNic, String ipAddr, String netMask) {
		_networkType = netType.NAT;
		_brName = brName;
		_stp = stp;
		_delay = delay;
		_fwDev = fwNic;
		_brIPAddr = ipAddr;
		_brNetMask = netMask;
	}

	public void defBrNetwork(String brName, boolean stp, int delay,
			String fwNic, String ipAddr, String netMask) {
		_networkType = netType.BRIDGE;
		_brName = brName;
		_stp = stp;
		_delay = delay;
		_fwDev = fwNic;
		_brIPAddr = ipAddr;
		_brNetMask = netMask;
	}

	public void defLocalNetwork(String brName, boolean stp, int delay,
			String ipAddr, String netMask) {
		_networkType = netType.LOCAL;
		_brName = brName;
		_stp = stp;
		_delay = delay;
		_brIPAddr = ipAddr;
		_brNetMask = netMask;
	}

	public void adddhcpIPRange(String start, String end) {
		IPRange ipr = new IPRange(start, end);
		ipranges.add(ipr);
	}

	public void adddhcpMapping(String mac, String host, String ip) {
		dhcpMapping map = new dhcpMapping(mac, host, ip);
		dhcpMaps.add(map);
	}

	@Override
	public String toString() {
		StringBuilder netBuilder = new StringBuilder();
		netBuilder.append("<network>\n");
		netBuilder.append("<name>" + _networkName + "</name>\n");
		if (_uuid != null)
			netBuilder.append("<uuid>" + _uuid + "</uuid>\n");
		if (_brName != null) {
			netBuilder.append("<bridge name='" + _brName + "'");
			if (_stp) {
				netBuilder.append(" stp='on'");
			} else {
				netBuilder.append(" stp='off'");
			}
			if (_delay != -1) {
				netBuilder.append(" delay='" + _delay + "'");
			}
			netBuilder.append("/>\n");
		}
		if (_domainName != null) {
			netBuilder.append("<domain name='" + _domainName + "'/>\n");
		}
		if (_networkType == netType.BRIDGE) {
			netBuilder.append("<forward mode='route'");
			if (_fwDev != null) {
				netBuilder.append(" dev='" + _fwDev + "'");
			}
			netBuilder.append("/>\n");
		} else if (_networkType == netType.NAT) {
			netBuilder.append("<forward mode='nat'");
			if (_fwDev != null) {
				netBuilder.append(" dev='" + _fwDev + "'");
			}
			netBuilder.append("/>\n");
		}
		if (_brIPAddr != null || _brNetMask != null || !ipranges.isEmpty()
				|| !dhcpMaps.isEmpty()) {
			netBuilder.append("<ip");
			if (_brIPAddr != null)
				netBuilder.append(" address='" + _brIPAddr + "'");
			if (_brNetMask != null) {
				netBuilder.append(" netmask='" + _brNetMask + "'");
			}
			netBuilder.append(">\n");

			if (!ipranges.isEmpty() || !dhcpMaps.isEmpty()) {
				netBuilder.append("<dhcp>\n");
				for (IPRange ip : ipranges) {
					netBuilder.append("<range start='" + ip._start + "'"
							+ " end='" + ip._end + "'/>\n");
				}
				for (dhcpMapping map : dhcpMaps) {
					netBuilder.append("<host mac='" + map._mac + "' name='"
							+ map._name + "' ip='" + map._ip + "'/>\n");
				}
				netBuilder.append("</dhcp>\n");
			}
			netBuilder.append("</ip>\n");
		}
		netBuilder.append("</network>\n");
		return netBuilder.toString();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LibvirtNetworkDef net = new LibvirtNetworkDef("cloudPrivate", null,
				"cloud.com");
		net.defNATNetwork("cloudbr0", false, 0, null, "192.168.168.1",
				"255.255.255.0");
		net.adddhcpIPRange("192.168.168.100", "192.168.168.220");
		net.adddhcpIPRange("192.168.168.10", "192.168.168.50");
		net.adddhcpMapping("branch0.cloud.com", "00:16:3e:77:e2:ed",
				"192.168.168.100");
		net.adddhcpMapping("branch1.cloud.com", "00:16:3e:77:e2:ef",
				"192.168.168.101");
		net.adddhcpMapping("branch2.cloud.com", "00:16:3e:77:e2:f0",
				"192.168.168.102");
		System.out.println(net.toString());

	}

}
