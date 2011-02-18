package com.cloud.hypervisor;

import com.cloud.agent.api.Command;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.Manager;

public interface HypervisorGuruManager extends Manager {
	HypervisorGuru getGuru(HypervisorType hypervisorType);
    long getGuruProcessedCommandTargetHost(long hostId, Command cmd);
}

