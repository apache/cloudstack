/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */
package com.cloud.configuration;

import java.util.ArrayList;
import java.util.List;

import com.cloud.baremetal.BareMetalPingServiceImpl;
import com.cloud.baremetal.BareMetalTemplateAdapter;
import com.cloud.baremetal.BareMetalVmManagerImpl;
import com.cloud.baremetal.ExternalDhcpManagerImpl;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.baremetal.PxeServerManagerImpl;
import com.cloud.baremetal.PxeServerService;
import com.cloud.ha.HighAvailabilityManagerExtImpl;
import com.cloud.hypervisor.vmware.VmwareManagerImpl;
import com.cloud.netapp.NetappManagerImpl;
import com.cloud.netapp.dao.LunDaoImpl;
import com.cloud.netapp.dao.PoolDaoImpl;
import com.cloud.netapp.dao.VolumeDaoImpl;
import com.cloud.network.ExternalNetworkManagerImpl;
import com.cloud.network.F5BigIpManagerImpl;
import com.cloud.network.JuniperSrxManagerImpl;
import com.cloud.network.NetworkDeviceManagerImpl;
import com.cloud.network.NetworkUsageManagerImpl;
import com.cloud.secstorage.CommandExecLogDaoImpl;
import com.cloud.secstorage.PremiumSecondaryStorageManagerImpl;
import com.cloud.template.TemplateAdapter;
import com.cloud.template.TemplateAdapter.TemplateAdapterType;
import com.cloud.upgrade.PremiumDatabaseUpgradeChecker;
import com.cloud.usage.dao.UsageDaoImpl;
import com.cloud.usage.dao.UsageIPAddressDaoImpl;
import com.cloud.usage.dao.UsageJobDaoImpl;
import com.cloud.utils.component.SystemIntegrityChecker;

public class PremiumComponentLibrary extends DefaultComponentLibrary {
    @Override
    protected void populateDaos() {
        addDao("UsageJobDao", UsageJobDaoImpl.class);
        addDao("UsageDao", UsageDaoImpl.class);
        addDao("UsageIpAddressDao", UsageIPAddressDaoImpl.class);
        addDao("CommandExecLogDao", CommandExecLogDaoImpl.class);
        addDao("NetappPool", PoolDaoImpl.class);
        addDao("NetappVolume", VolumeDaoImpl.class);
        addDao("NetappLun", LunDaoImpl.class);
    }

    @Override
    protected void populateManagers() {
    	// override FOSS SSVM manager
        addManager("secondary storage vm manager", PremiumSecondaryStorageManagerImpl.class);
	
        addManager("HA Manager", HighAvailabilityManagerExtImpl.class);
        addManager("VMWareManager", VmwareManagerImpl.class);
        addManager("ExternalNetworkManager", ExternalNetworkManagerImpl.class);
        addManager("JuniperSrxManager", JuniperSrxManagerImpl.class);
        addManager("F5BigIpManager", F5BigIpManagerImpl.class);
        addManager("BareMetalVmManager", BareMetalVmManagerImpl.class);
        addManager("ExternalDhcpManager", ExternalDhcpManagerImpl.class);
        addManager("PxeServerManager", PxeServerManagerImpl.class);
        addManager("NetworkDeviceManager", NetworkDeviceManagerImpl.class);
        addManager("NetworkUsageManager", NetworkUsageManagerImpl.class);
        addManager("NetappManager", NetappManagerImpl.class);
    }

    @Override
    protected void populateAdapters() {
    	super.populateAdapters();
    	addAdapter(PxeServerService.class, PxeServerType.PING.getName(), BareMetalPingServiceImpl.class);
    	addAdapter(TemplateAdapter.class, TemplateAdapterType.BareMetal.getName(), BareMetalTemplateAdapter.class);
    }
}
