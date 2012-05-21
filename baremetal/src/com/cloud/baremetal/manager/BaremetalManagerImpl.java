package com.cloud.baremetal.manager;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.Inject;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = {BaremetalManager.class})
public class BaremetalManagerImpl implements BaremetalManager,  StateListener<State, VirtualMachine.Event, VirtualMachine> {
	private static final Logger s_logger = Logger.getLogger(BaremetalManagerImpl.class);
	
	@Inject
	protected HostDao _hostDao;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		VirtualMachine.State.getStateMachine().registerListener(this);
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return "Baremetal Manager";
	}

	@Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
	    return false;
    }

	@Override
    public boolean postStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
		if (newState != State.Starting && newState != State.Error && newState != State.Expunging) {
			return true;
		}
		
		if (vo.getHypervisorType() != HypervisorType.BareMetal) {
		    return true;
		}
		
		HostVO host = _hostDao.findById(vo.getHostId());
		if (host == null) {
			s_logger.debug("Skip oldState " + oldState + " to " + "newState " + newState + " transimtion");
			return true;
		}
		_hostDao.loadDetails(host);
		
		if (newState == State.Starting) {
			host.setDetail("vmName", vo.getInstanceName());
			s_logger.debug("Add vmName " + host.getDetail("vmName") + " to host " + host.getId() + " details");
		} else {
			if (host.getDetail("vmName") != null && host.getDetail("vmName").equalsIgnoreCase(vo.getInstanceName())) {
				s_logger.debug("Remove vmName " + host.getDetail("vmName") + " from host " + host.getId() + " details");
				host.getDetails().remove("vmName");
			}
		}
		_hostDao.saveDetails(host);
		
		
		return true;
    }
}
