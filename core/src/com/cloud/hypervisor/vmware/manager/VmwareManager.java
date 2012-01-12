/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.manager;

import java.io.File;
import java.util.List;
import java.util.Map;

//import com.cloud.cluster.CheckPointManager;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.Pair;
import com.vmware.vim25.ManagedObjectReference;

public interface VmwareManager {
	public final String CONTEXT_STOCK_NAME = "vmwareMgr";
	
	// this limitation comes from the fact that we are using linked clone on shared VMFS storage,
	// we need to limit the size of vCenter cluster, http://en.wikipedia.org/wiki/VMware_VMFS
	public final int MAX_HOSTS_PER_CLUSTER = 8;

	String composeWorkerName();
	
    String getSystemVMIsoFileNameOnDatastore();
    String getSystemVMDefaultNicAdapterType();
    
	void prepareSecondaryStorageStore(String strStorageUrl);
	
	void setupResourceStartupParams(Map<String, Object> params);
	List<ManagedObjectReference> addHostToPodCluster(VmwareContext serviceContext, long dcId, Long podId, Long clusterId,
			String hostInventoryPath) throws Exception;

	String getManagementPortGroupByHost(HostMO hostMo) throws Exception; 
	String getServiceConsolePortGroupName();
	String getManagementPortGroupName();
	
	String getSecondaryStorageStoreUrl(long dcId);
	
	File getSystemVMKeyFile();
	
	VmwareStorageManager getStorageManager();
	long pushCleanupCheckpoint(String hostGuid, String vmName);
	void popCleanupCheckpoint(long checkpiont);
	void gcLeftOverVMs(VmwareContext context);
	
	Pair<Integer, Integer> getAddiionalVncPortRange();
	
	int getMaxHostsPerCluster();
	int getRouterExtraPublicNics();
	
	boolean beginExclusiveOperation(int timeOutSeconds);
	void endExclusiveOperation();
}
