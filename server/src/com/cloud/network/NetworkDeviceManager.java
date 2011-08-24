package com.cloud.network;

import java.util.List;

import com.cloud.api.commands.AddNetworkDeviceCmd;
import com.cloud.api.commands.DeleteNetworkDeviceCmd;
import com.cloud.api.commands.ListNetworkDeviceCmd;
import com.cloud.host.Host;
import com.cloud.server.api.response.NetworkDeviceResponse;
import com.cloud.utils.component.Manager;

public interface NetworkDeviceManager extends Manager {
	public static class NetworkDeviceType {
		private String _name;
		
		public static final NetworkDeviceType ExternalDhcp = new NetworkDeviceType("ExternalDhcp");
		public static final NetworkDeviceType PxeServer = new NetworkDeviceType("PxeServer");
		
		public NetworkDeviceType(String name) {
			_name = name;
		}
		
		public String getName() {
			return _name;
		}
	}
	
	public Host addNetworkDevice(AddNetworkDeviceCmd cmd);
	
	public NetworkDeviceResponse getApiResponse(Host device);
	
	public List<Host> listNetworkDevice(ListNetworkDeviceCmd cmd); 
	
	public boolean deleteNetworkDevice(DeleteNetworkDeviceCmd cmd);
}
