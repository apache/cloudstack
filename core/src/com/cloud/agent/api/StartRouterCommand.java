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
package com.cloud.agent.api;

import java.util.List;

import com.cloud.storage.VolumeVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.DomainRouter.Role;


public class StartRouterCommand extends AbstractStartCommand {

    DomainRouterVO router;
    int networkRateMbps;
    int networkRateMulticastMbps;

    protected StartRouterCommand() {
    	super();
    }
    
    @Override
    public boolean executeInSequence() {
    	//Temporary relaxing serialization
        return false;
    }
    
    public StartRouterCommand(DomainRouterVO router, int networkRateMbps, int networkRateMulticastMbps, 
            String routerName, String[] storageIps, List<VolumeVO> vols, boolean mirroredVols) {
        super(routerName, storageIps, vols, mirroredVols);
        this.router = router;
        this.networkRateMbps = networkRateMbps;
        this.networkRateMulticastMbps = networkRateMulticastMbps;
	}

	public DomainRouter getRouter() {
        return router;
    }
	
	public int getNetworkRateMbps() {
        return networkRateMbps;
    }

    public int getNetworkRateMulticastMbps() {
        return networkRateMulticastMbps;
    }

    public String getBootArgs() {
		String eth2Ip = router.getPublicIpAddress()==null?"0.0.0.0":router.getPublicIpAddress();
		String basic = " eth0ip=" + router.getGuestIpAddress() + " eth0mask=" + router.getGuestNetmask() + " eth1ip="
        + router.getPrivateIpAddress() + " eth1mask=" + router.getPrivateNetmask() + " gateway=" + router.getGateway()
		+ " dns1=" + router.getDns1() +  " name=" + router.getName();
		if (!router.getPublicMacAddress().equalsIgnoreCase("FE:FF:FF:FF:FF:FF")) {
		    basic = basic + " eth2ip=" + eth2Ip + " eth2mask=" + router.getPublicNetmask();
		}
		if (router.getDns2() != null) {
			basic = basic + " dns2=" + router.getDns2();
		}
		if (getDhcpRange() != null) {
			basic = basic + " dhcprange=" + getDhcpRange();
		}
		if (router.getRole() == Role.DHCP_FIREWALL_LB_PASSWD_USERDATA) {
			basic = basic + " type=router";
		} else if (router.getRole() == Role.DHCP_USERDATA) {
			basic = basic + " type=dhcpsrvr";
		}
		return basic;
	}

	public String getDhcpRange() {
		String [] range = router.getDhcpRange();
		String result = null;
		if (range[0] != null) {
			result = range[0];
		}
		return result;
	}


}
