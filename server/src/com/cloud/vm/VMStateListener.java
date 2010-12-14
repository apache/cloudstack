package com.cloud.vm;

import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.fsm.StateDao;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.VMInstanceDao;

public class VMStateListener implements StateListener<State, VirtualMachine.Event, VMInstanceVO>{
	private static final Logger s_logger = Logger.getLogger(VMStateListener.class);
	CapacityDao _capacityDao;
	ServiceOfferingDao _offeringDao;

	
	public VMStateListener(CapacityDao capacityDao, ServiceOfferingDao offering) {
		_capacityDao = capacityDao;
		_offeringDao = offering;
	}
	
	@Override
	public boolean processStateTransitionEvent(State oldState,
			Event event, State newState, VMInstanceVO vm, boolean transitionStatus, Long id, StateDao<State, VirtualMachine.Event, VMInstanceVO> vmDao) {
		s_logger.debug("VM state transitted from :" + oldState + " to " + newState + " with event: " + event +
				"vm's original host id: " + vm.getHostId() + " new host id: " + id);
		if (!transitionStatus) {
			return false;
		}

		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
			txn.start();
			
			if (oldState == State.Starting) {
				if (event == Event.OperationSucceeded) {
					if (vm.getLastHostId() != null && vm.getLastHostId() != id) {
						/*need to release the reserved capacity on lasthost*/
						releaseResource(vm, true, false, vm.getLastHostId());
					}
					vm.setLastHostId(id);
					
				} else if (event == Event.OperationRetry || event == Event.OperationFailed) {
					/*need to release resource from host, passed in from id, cause vm.gethostid is null*/
					releaseResource(vm, false, false, id);
					id = null;
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
		} finally {			
			txn.close();
		}
		
		return transitionStatus;
	}

	private void releaseResource(VMInstanceVO vm, boolean moveFromReserved, boolean moveToReservered, Long hostId) {
		ServiceOfferingVO svo = _offeringDao.findById(vm.getServiceOfferingId());
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
			if (usedCpu >= vmCPU)
				capacityCpu.setUsedCapacity(usedCpu - vmCPU);
			if (usedMem >= vmMem)
				capacityMemory.setUsedCapacity(usedMem - vmMem);

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
		ServiceOfferingVO svo = _offeringDao.findById(vm.getServiceOfferingId());
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
}
