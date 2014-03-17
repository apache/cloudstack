package com.cloud.hypervisor.kvm.discoverer;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.Discoverer;

@Local(value = Discoverer.class)
public class DockerServerDiscoverer extends LibvirtServerDiscoverer {
	private static final Logger s_logger = Logger.getLogger(DockerServerDiscoverer.class);

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.Docker;
    }
}
