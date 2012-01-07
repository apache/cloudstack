/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.manager;

import com.cloud.agent.api.Command;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareContext;

public interface VmwareHostService {
	VmwareContext getServiceContext(Command cmd);
	void invalidateServiceContext(VmwareContext context);
	VmwareHypervisorHost getHyperHost(VmwareContext context, Command cmd);

	String getWorkerName(VmwareContext context, Command cmd, int workerSequence);
}
