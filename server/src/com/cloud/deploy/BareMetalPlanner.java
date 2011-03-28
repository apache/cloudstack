package com.cloud.deploy;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;

@Local(value=DeploymentPlanner.class)
public class BareMetalPlanner extends FirstFitPlanner implements DeploymentPlanner {
	private static final Logger s_logger = Logger.getLogger(BareMetalPlanner.class);
	
	@Override
	public DeployDestination plan(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
		VirtualMachine vm = vmProfile.getVirtualMachine();
		ServiceOffering offering = vmProfile.getServiceOffering();
		String hostTag = null;
		
		if (offering.getTags() != null) {
			String[] tags = offering.getTags().split(",");
			if (tags.length > 0) {
				hostTag = tags[0];
			}
		}
		
		if (hostTag != null) {
			List<HostVO> hosts = _hostDao.listBy(Host.Type.Routing, vm.getDataCenterId());
			HostVO target = null;
			for (HostVO h : hosts) {
				_hostDao.loadDetails(h);
				if (h.getDetail("hostTag") != null && h.getDetail("hostTag").equalsIgnoreCase(hostTag)) {
					target = h;
					break;
				}
			}
			
			if (target == null) {
				s_logger.warn("Cannot find host with tag " + hostTag);
				return null;
			}
			
			int cpu = target.getCpus();
			int speed = target.getSpeed().intValue();
			Long ramSize = target.getTotalMemory() / (1024L*1024L);
			ServiceOfferingVO newOffering = new ServiceOfferingVO(offering.getName(), cpu, ramSize.intValue(), speed, offering.getRateMbps(),
					offering.getMulticastRateMbps(), false, offering.getDisplayText(), offering.getUseLocalStorage(), false, offering.getTags(), false);
			((VirtualMachineProfileImpl)vmProfile).setServiceOffering(newOffering);
		}
		
		DeployDestination dest = super.plan(vmProfile, plan, avoid);
		
		if (hostTag == null && dest != null) {
			Host h = dest.getHost();
			if (h.getCpus() != offering.getCpu() || h.getTotalMemory() != offering.getRamSize() || h.getSpeed() != offering.getSpeed()) {
				throw new CloudRuntimeException(String.format("Bare Metal only allows one VM one host, " +
						"the offering capcacity doesn't equal to host capacity(offering: cpu number:%$1s, cpu speed:%$2s," +
						"ram size:%3$s; host: cpu number:%$4s, cpu speed:%$5s, ram size:%$6s)", offering.getCpu(), offering.getSpeed(),
						offering.getRamSize(), h.getCpus(), h.getSpeed(), h.getTotalMemory()));
			}
		}
		
		return dest;
	}

	@Override
	public boolean canHandle(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, ExcludeList avoid) {
		return vm.getHypervisorType() == HypervisorType.BareMetal;
	}
}
