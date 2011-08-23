/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.ManagedObjectReference;

public class NetworkMO extends BaseMO {
    public NetworkMO(VmwareContext context, ManagedObjectReference morCluster) {
        super(context, morCluster);
    }
    
    public NetworkMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }
    
    public void destroyNetwork() throws Exception {
        _context.getService().destroyNetwork(_mor); 
    }
    
    public ManagedObjectReference[] getVMsOnNetwork() throws Exception {
        ManagedObjectReference[] vms = (ManagedObjectReference[])_context.getServiceUtil().getDynamicProperty(_mor, "vm");
        return vms;
    }
}
