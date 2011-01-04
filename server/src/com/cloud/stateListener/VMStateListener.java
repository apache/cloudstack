package com.cloud.stateListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.fsm.StateDao;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value=StateListener.class)
public class VMStateListener extends AdapterBase implements StateListener<State, VirtualMachine.Event, VMInstanceVO> {
	private static final Logger s_logger = Logger.getLogger(VMStateListener.class);
	@Inject CapacityDao _capacityDao = null;
	@Inject ServiceOfferingDao _offeringsDao = null;
	@Inject HostDao _hostDao = null;
	@Inject VMInstanceDao _vmDao = null;
	@Inject ConfigurationDao _configDao = null;
	
	ScheduledExecutorService _executor;
	int _hostCapacityCheckerDelay;
	int _hostCapacityCheckerInterval;
	int _vmCapacityReleaseInterval;
	boolean _stopped = false;
	
	@DB
	@Override
	public boolean processStateTransitionEvent(State oldState,
			Event event, State newState, VMInstanceVO vm, boolean transitionStatus, Long id, StateDao<State, VirtualMachine.Event, VMInstanceVO> vmDao) {
		s_logger.debug("VM state transitted from :" + oldState + " to " + newState + " with event: " + event +
				"vm's original host id: " + vm.getHostId() + " new host id: " + id);
		if (!transitionStatus) {
			return false;
		}

		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			
			if (oldState == State.Starting) {
				if (event == Event.OperationSucceeded) {
					if (vm.getLastHostId() != null && vm.getLastHostId() != id) {
						/*need to release the reserved capacity on lasthost*/
						releaseResource(vm, true, false, vm.getLastHostId());
					}
					vm.setLastHostId(id);
				} else if (event == Event.OperationFailed) {
					releaseResource(vm, false, false, vm.getHostId());
				} else if (event == Event.OperationRetry) {
					releaseResource(vm, false, false, vm.getHostId());
				} else if (event == Event.AgentReportStopped) {
					releaseResource(vm, false, true, vm.getHostId());
				}
			} else if (oldState == State.Running) {
				if (event == Event.AgentReportStopped) {
					releaseResource(vm, false, true, vm.getHostId());
				}
			} else if (oldState == State.Migrating) {
				if (event == Event.AgentReportStopped) {
					/*Release capacity from original host*/
					releaseResource(vm, false, true, vm.getHostId());
				} else if (event == Event.MigrationFailedOnSource) {
					/*release capacity from dest host*/
					releaseResource(vm, false, false, id);
					id = vm.getHostId();
				} else if (event == Event.MigrationFailedOnDest) {
					/*release capacify from original host*/
					releaseResource(vm, false, false, vm.getHostId());
				} else if (event == Event.OperationSucceeded) {
					releaseResource(vm, false, false, vm.getHostId());
					/*set lasthost id to migration destination host id*/
					vm.setLastHostId(id);
				}
			} else if (oldState == State.Stopping) {
				if (event == Event.AgentReportStopped || event == Event.OperationSucceeded) {
					releaseResource(vm, false, true, vm.getHostId());
				}
			} else if (oldState == State.Stopped) {
				if (event == Event.DestroyRequested) {
					releaseResource(vm, true, false, vm.getLastHostId());
					vm.setLastHostId(null);
				}
			}

			transitionStatus = vmDao.updateState(oldState, event, newState, vm, id);
			if (transitionStatus) {
				txn.commit();				
			} else {
				s_logger.debug("Failed to transit vm's state");
				txn.rollback();
			}
		} catch (Exception e) {
			s_logger.debug("Failed to transit vm's state, due to " + e.getMessage());
			txn.rollback();
		}
		
		return transitionStatus;
	}

	private void releaseResource(VMInstanceVO vm, boolean moveFromReserved, boolean moveToReservered, Long hostId) {
		ServiceOfferingVO svo = _offeringsDao.findById(vm.getServiceOfferingId());
		CapacityVO capacityCpu = _capacityDao.findByHostIdType(hostId, CapacityVO.CAPACITY_TYPE_CPU);
		CapacityVO capacityMemory = _capacityDao.findByHostIdType(hostId, CapacityVO.CAPACITY_TYPE_MEMORY);
		
		if (capacityCpu == null || capacityMemory == null || svo == null) {
			return;
		}
		
		int vmCPU = svo.getCpu() * svo.getSpeed();
		long vmMem = svo.getRamSize() * 1024L * 1024L;

		capacityCpu = _capacityDao.lockRow(capacityCpu.getId(), true);
		capacityMemory = _capacityDao.lockRow(capacityMemory.getId(), true);

		long usedCpu = capacityCpu.getUsedCapacity();
		long usedMem = capacityMemory.getUsedCapacity();
		long reservedCpu = capacityCpu.getReservedCapacity();
		long reservedMem = capacityMemory.getReservedCapacity();
		long totalCpu = capacityCpu.getTotalCapacity();
		long totalMem = capacityMemory.getTotalCapacity();

		if (!moveFromReserved) {
			/*move resource from used*/
			if (usedCpu >= vmCPU) {
                capacityCpu.setUsedCapacity(usedCpu - vmCPU);
            }
			if (usedMem >= vmMem) {
                capacityMemory.setUsedCapacity(usedMem - vmMem);
            }

			if (moveToReservered) {
				if (reservedCpu + vmCPU <= totalCpu) {
					capacityCpu.setReservedCapacity(reservedCpu + vmCPU); 
				}
				if (reservedMem + vmMem <= totalMem) {
					capacityMemory.setReservedCapacity(reservedMem + vmMem); 
				}
			}
		} else {
			if (reservedCpu >= vmCPU) {
				capacityCpu.setReservedCapacity(reservedCpu - vmCPU); 
			}
			if (reservedMem >= vmMem) {
				capacityMemory.setReservedCapacity(reservedMem - vmMem);
			}
		}
		
		s_logger.debug("release cpu from host: " + hostId + ", old used: " + usedCpu + ",reserved: " + reservedCpu + ", total: " + totalCpu +
				"; new used: " + capacityCpu.getUsedCapacity() + ",reserved:" + capacityCpu.getReservedCapacity() + ",total: " + capacityCpu.getTotalCapacity() +
				"; movedfromreserved: " + moveFromReserved + ",moveToReservered" + moveToReservered);
		
		s_logger.debug("release mem from host: " + hostId + ", old used: " + usedMem + ",reserved: " + reservedMem + ", total: " + totalMem +
				"; new used: " + capacityMemory.getUsedCapacity() + ",reserved:" + capacityMemory.getReservedCapacity() + ",total: " + capacityMemory.getTotalCapacity() +
				"; movedfromreserved: " + moveFromReserved + ",moveToReservered" + moveToReservered);

 		_capacityDao.update(capacityCpu.getId(), capacityCpu);
		_capacityDao.update(capacityMemory.getId(), capacityMemory);

	}
	
	/*Add capacity to destination host, for migration*/
	private void addResource(VMInstanceVO vm, Long destHostId) {
		ServiceOfferingVO svo = _offeringsDao.findById(vm.getServiceOfferingId());
		CapacityVO capacityCpu = _capacityDao.findByHostIdType(destHostId, CapacityVO.CAPACITY_TYPE_CPU);
		CapacityVO capacityMemory = _capacityDao.findByHostIdType(destHostId, CapacityVO.CAPACITY_TYPE_MEMORY);
		int vmCPU = svo.getCpu() * svo.getSpeed();
		long vmMem = svo.getRamSize() * 1024L * 1024L;

		capacityCpu = _capacityDao.lockRow(capacityCpu.getId(), true);
		capacityMemory = _capacityDao.lockRow(capacityMemory.getId(), true);

		long usedCpu = capacityCpu.getUsedCapacity();
		long usedMem = capacityMemory.getUsedCapacity();
		long reservedCpu = capacityCpu.getReservedCapacity();
		long reservedMem = capacityMemory.getReservedCapacity();
		long totalCpu = capacityCpu.getTotalCapacity();
		long totalMem = capacityMemory.getTotalCapacity();

		if (usedCpu + reservedCpu + vmCPU <= totalCpu) {
			capacityCpu.setUsedCapacity(usedCpu + vmCPU);
		} else {
			s_logger.debug("What's the heck? :u:" + usedCpu + ",r:" + reservedCpu + ",vm:" + vmCPU + " > " + totalCpu);
		}
		
		if (usedMem + reservedMem + vmMem <= totalMem) {
			capacityMemory.setUsedCapacity(usedMem + vmMem);
		} else {
			s_logger.debug("What's the heck? :u:" + usedMem + ",r:" + reservedMem + ",vm:" + vmMem + " > " + totalMem);
		}

		_capacityDao.update(capacityCpu.getId(), capacityCpu);
		_capacityDao.update(capacityMemory.getId(), capacityMemory);

	}
	
	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		super.configure(name, params);
		_hostCapacityCheckerDelay = NumbersUtil.parseInt(_configDao.getValue(Config.HostCapacityCheckerWait.key()), 3600);
		_hostCapacityCheckerInterval = NumbersUtil.parseInt(_configDao.getValue(Config.HostCapacityCheckerInterval.key()), 3600);
		_vmCapacityReleaseInterval = NumbersUtil.parseInt(_configDao.getValue(Config.VmHostCapacityReleaseInterval.key()), 86400);
		_executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("HostCapacity-Checker"));
        return true;
    }
	
	@Override
	public boolean start() {
		 _executor.schedule(new HostCapacityCollector(), _hostCapacityCheckerDelay,  TimeUnit.SECONDS);
		 return true;
    }

    @Override
    public boolean stop() {
    	_executor.shutdownNow();
    	_stopped = true;
        return true;
    }
    
    public class HostCapacityCollector implements Runnable {

		@Override
		public void run() {
			while (!_stopped) {
				try {
					Thread.sleep(_hostCapacityCheckerInterval * 1000);
				} catch (InterruptedException e1) {

				}
				// get all hosts..
				SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();
				sc.addAnd("status", SearchCriteria.Op.EQ, Status.Up.toString());
				List<HostVO> hosts = _hostDao.search(sc, null);

				// prep the service offerings
				List<ServiceOfferingVO> offerings = _offeringsDao.listAllIncludingRemoved();
				Map<Long, ServiceOfferingVO> offeringsMap = new HashMap<Long, ServiceOfferingVO>();
				for (ServiceOfferingVO offering : offerings) {
					offeringsMap.put(offering.getId(), offering);
				}

				for (HostVO host : hosts) {
					if (host.getType() != Host.Type.Routing) {
						continue;
					}

					long usedCpu = 0;
					long usedMemory = 0;
					long reservedMemory = 0;
					long reservedCpu = 0;

					List<VMInstanceVO> vms = _vmDao.listUpByHostId(host.getId());
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Found " + vms.size() + " VMs on host " + host.getId());
					}

					for (VMInstanceVO vm : vms) {
						ServiceOffering so = offeringsMap.get(vm.getServiceOfferingId());
						usedMemory += so.getRamSize() * 1024L * 1024L;
						usedCpu += so.getCpu() * so.getSpeed();
					}

					List<VMInstanceVO> vmsByLastHostId = _vmDao.listByLastHostId(host.getId());
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Found " + vmsByLastHostId.size() + " VM, not running on host " + host.getId());
					}
					for (VMInstanceVO vm : vmsByLastHostId) {
						long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - vm.getUpdateTime().getTime())/1000;
						if (secondsSinceLastUpdate < _vmCapacityReleaseInterval) {
							ServiceOffering so = offeringsMap.get(vm.getServiceOfferingId());
							reservedMemory += so.getRamSize() * 1024L * 1024L; 
							reservedCpu += so.getCpu() * so.getSpeed();
						}
					}

					CapacityVO cpuCap = _capacityDao.findByHostIdType(host.getId(), CapacityVO.CAPACITY_TYPE_CPU);
					CapacityVO memCap = _capacityDao.findByHostIdType(host.getId(), CapacityVO.CAPACITY_TYPE_MEMORY);

					if (cpuCap.getUsedCapacity() == usedCpu && cpuCap.getReservedCapacity() == reservedCpu) {
						s_logger.debug("Cool, no need to calibrate cpu capacity, host:" + host.getId() + " usedCpu: " + cpuCap.getUsedCapacity() + " reservedCpu: " + cpuCap.getReservedCapacity());
					} else if (cpuCap.getReservedCapacity() != reservedCpu) {
						s_logger.debug("Calibrate reserved cpu for host: " + host.getId() + " old reservedCpu:" + cpuCap.getReservedCapacity() + " new reservedCpu:" + reservedCpu);
						cpuCap.setReservedCapacity(reservedCpu);
					} else if (cpuCap.getUsedCapacity() != usedCpu) {
						s_logger.debug("Calibrate used cpu for host: " + host.getId() + " old usedCpu:" + cpuCap.getUsedCapacity() + " new usedCpu:" + usedCpu);
						cpuCap.setUsedCapacity(usedCpu);
					}

					if (memCap.getUsedCapacity() == usedMemory && memCap.getReservedCapacity() == reservedMemory) {
						s_logger.debug("Cool, no need to calibrate memory capacity, host:" + host.getId() + " usedMem: " + memCap.getUsedCapacity() + " reservedMem: " + memCap.getReservedCapacity());
					} else if (memCap.getReservedCapacity() != reservedMemory) {
						s_logger.debug("Calibrate reserved memory for host: " + host.getId() + " old reservedMem:" + memCap.getReservedCapacity() + " new reservedMem:" + reservedMemory);
						memCap.setReservedCapacity(reservedMemory);
					} else if (memCap.getUsedCapacity() != usedMemory) {
						/*Didn't calibrate for used memory, because VMs can be in state(starting/migrating) that I don't know on which host they are allocated*/
						s_logger.debug("Calibrate used memory for host: " + host.getId() + " old usedMem: " + memCap.getUsedCapacity() + " new usedMem: " + usedMemory);
						memCap.setUsedCapacity(usedMemory);
					}

					try {
						_capacityDao.update(cpuCap.getId(), cpuCap);
						_capacityDao.update(memCap.getId(), memCap);
					} catch (Exception e) {

					}
				} 

			} 
		}
    }
}
