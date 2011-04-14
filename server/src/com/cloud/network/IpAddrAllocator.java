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

package com.cloud.network;

import com.cloud.utils.component.Adapter;

public interface IpAddrAllocator extends Adapter {
	public  class IpAddr {
		public String ipaddr;
		public String netMask;
		public String gateway;
		public IpAddr(String ipaddr, String netMask, String gateway) {
			this.ipaddr = ipaddr;
			this.netMask = netMask;
			this.gateway = gateway;
		}
		public IpAddr() {
			this.ipaddr = null;
			this.netMask = null;
			this.gateway = null;
		}
	}
	public class networkInfo {
		public String _ipAddr;
		public String _netMask;
		public String _gateWay;
		public Long _vlanDbId;
		public String _vlanid;
		public networkInfo(String ip, String netMask, String gateway, Long vlanDbId, String vlanId) {
			_ipAddr = ip;
			_netMask = netMask;
			_gateWay = gateway;
			_vlanDbId = vlanDbId;
			_vlanid = vlanId;
		}
	}
	public IpAddr getPublicIpAddress(String macAddr, long dcId, long podId);
	public IpAddr getPrivateIpAddress(String macAddr, long dcId, long podId);
	public boolean releasePublicIpAddress(String ip, long dcId, long podId);
	public boolean releasePrivateIpAddress(String ip, long dcId, long podId);
	public boolean exteralIpAddressAllocatorEnabled();
}
