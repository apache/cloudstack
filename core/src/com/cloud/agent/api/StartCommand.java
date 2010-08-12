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
import java.util.Map;

import com.cloud.service.ServiceOffering;
import com.cloud.storage.VolumeVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.UserVm;
import com.cloud.vm.UserVmVO;

public class StartCommand extends AbstractStartCommand {
    long id;
    String guestIpAddress;
    String gateway;
    int ramSize;
    String imagePath;
    String guestNetworkId;
    String guestMacAddress;
    String vncPassword;
    String externalVlan;
    String externalMacAddress;
    int utilization;
    int cpuWeight;
    int cpu;
    int networkRateMbps;
    int networkRateMulticastMbps;
    String hostName;
    String arch;
    String isoPath;
    boolean bootFromISO;
    String guestOSDescription;
    
    public StartCommand(UserVm vm, String vmName, ServiceOffering offering, int networkRate, int multicastRate, DomainRouter router, String storageHost, String imagePath, String guestNetworkId, int utilization, int cpuWeight, List<VolumeVO> vols, int bits, String isoPath, boolean bootFromISO, String guestOSDescription) {
    	super(vmName, storageHost, vols);
        initialize(vm, offering, networkRate, multicastRate, router, imagePath, guestNetworkId, utilization, cpuWeight, bits, isoPath, bootFromISO, guestOSDescription);
    }

	private void initialize(UserVm vm,
			ServiceOffering offering, int networkRate, int multicastRate, DomainRouter router, String imagePath,
			String guestNetworkId, int utilization, int cpuWeight, int bits, String isoPath, boolean bootFromISO, String guestOSDescription) {
		id = vm.getId();
        guestIpAddress = vm.getGuestIpAddress();
        if (router != null)
        	gateway = router.getPrivateIpAddress();
        ramSize = offering.getRamSize();
        cpu = offering.getCpu();
        this.utilization= utilization;
        this.cpuWeight = cpuWeight;
        this.imagePath = imagePath;
        this.guestNetworkId = guestNetworkId;
        guestMacAddress = vm.getGuestMacAddress();
        vncPassword = vm.getVncPassword();
        hostName = vm.getName();
//        networkRateMbps = offering.getRateMbps();
//        networkRateMulticastMbps = offering.getMulticastRateMbps();
        networkRateMbps = networkRate;
        networkRateMulticastMbps = multicastRate;
        if (bits == 32) {
        	arch = "i686";
        } else {
        	arch = "x86_64";
        }
        this.isoPath = isoPath;
        this.bootFromISO = bootFromISO;
        this.guestOSDescription = guestOSDescription;
	}
	
	public String getArch() {
		return arch;
	}

	public String getHostName() {
		return hostName;
	}
    
    protected StartCommand() {
    	super();
    }
    
    public StartCommand(UserVmVO vm, String vmName, ServiceOffering offering, int networkRate, int multicastRate,
			DomainRouter router, String[] storageIps, String imagePath,
			String guestNetworkId, int utilization, int cpuWeight, List<VolumeVO> vols,
			boolean mirroredVols, int bits, String isoPath, boolean bootFromISO, String guestOSDescription) {
		super(vmName, storageIps, vols, mirroredVols);
        initialize(vm, offering, networkRate, multicastRate, router, imagePath, guestNetworkId, utilization, cpuWeight, bits, isoPath, bootFromISO, guestOSDescription);

	}

	@Override
    public boolean executeInSequence() {
        return true;
    }

    public int getCpu() {
        return cpu;
    }
    
    public int getUtilization() {
        return utilization;
    }

    public int getCpuWeight() {
		return cpuWeight;
	}
    
    public long getId() {
        return id;
    }

    public String getGuestIpAddress() {
        return guestIpAddress;
    }

    public String getGuestMacAddress() {
        return guestMacAddress;
    }

    public String getVncPassword() {
        return vncPassword;
    }

    public String getGateway() {
        return gateway;
    }
    
    public String getGuestNetworkId() {
        return guestNetworkId;
    }
    
    
    public int getRamSize() {
        return ramSize;
    }
    
    public String getImagePath() {
        return imagePath;
    }

	public int getNetworkRateMbps() {
		return networkRateMbps;
	}

	public int getNetworkRateMulticastMbps() {
		return networkRateMulticastMbps;
	}
	
	public String getISOPath() {
		return isoPath;
	}
	
	public boolean getBootFromISO() {
		return bootFromISO;
	}

	public void setExternalVlan(String vlanId) {
		this.externalVlan = vlanId;
		
	}

	public String getExternalVlan() {
		return externalVlan;
	}

	public String getExternalMacAddress() {
		return externalMacAddress;
	}

	public void setExternalMacAddress(String externalMacAddress) {
		this.externalMacAddress = externalMacAddress;
	}
	
	public String getGuestOSDescription() {
		return this.guestOSDescription;
	}
	
	public void setGuestOSDescription(String guestOSDescription) {
		this.guestOSDescription = guestOSDescription;
	}
    
}
