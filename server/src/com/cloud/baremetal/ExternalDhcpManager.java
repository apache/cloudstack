/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.baremetal;

import com.cloud.baremetal.ExternalDhcpEntryListener.DhcpEntryState;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface ExternalDhcpManager extends Manager {
	public static class DhcpServerType {
		private String _name;
		
		public static final DhcpServerType Dnsmasq = new DhcpServerType("Dnsmasq");
		public static final DhcpServerType Dhcpd = new DhcpServerType("Dhcpd");
		
		public DhcpServerType(String name) {
			_name = name;
		}
		
		public String getName() {
			return _name;
		}
		
	}
	
	
	DhcpServerResponse getApiResponse(Host dhcpServer);
	
	boolean addVirtualMachineIntoNetwork(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException;
	
	Host addDhcpServer(Long zoneId, Long podId, String type, String url, String username, String password);
}
