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
