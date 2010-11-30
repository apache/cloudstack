package com.cloud.vm;

import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.VMInstanceDao;

public class VMStateListener implements StateListener<State, VirtualMachine.Event, VMInstanceVO>{
	CapacityDao _capacityDao;
	ServiceOfferingDao _offeringDao;
	VMInstanceDao _vmDao;
	
	public VMStateListener(CapacityDao capacityDao, ServiceOfferingDao offering, VMInstanceDao vmDao) {
		_capacityDao = capacityDao;
		_offeringDao = offering;
		this._vmDao = vmDao;
	}
	
	@Override
	@DB
	public boolean processStateTransitionEvent(State oldState,
			Event event, State newState, VMInstanceVO vm, boolean transitionStatus) {
		if (oldState == State.Starting) {
			if (event == Event.OperationRetry || event == Event.OperationFailed) {
				releaseResource(vm, false, false);
			}
		}
		
		if (!transitionStatus) {
			return true;
		}
		
		if (oldState == State.Starting) {
			if (event == Event.OperationSucceeded) {
				vm.setLastHostId(vm.getHostId());
				_vmDao.update(vm.getId(), vm);
			}
		} else if (oldState == State.Running) {
			if (event == Event.AgentReportStopped) {
				releaseResource(vm, false, true);
			}
		} else if (oldState == State.Migrating) {
			if (event == Event.AgentReportStopped) {
				releaseResource(vm, false, true);
			}
		} else if (oldState == State.Stopping) {
			if (event == Event.AgentReportStopped || event == Event.OperationSucceeded) {
				releaseResource(vm, false, true);
			}
		} else if (oldState == State.Stopped) {
			if (event == Event.DestroyRequested) {
				releaseResource(vm, true, false);
				
				vm.setLastHostId(null);
				_vmDao.update(vm.getId(), vm);
			}
		}
		return true;
	}
	
	@DB
	private void releaseResource(VMInstanceVO vm, boolean moveFromReserved, boolean moveToReservered) {
        CapacityVO capacityCpu = _capacityDao.findByHostIdType(vm.getHostId(), CapacityVO.CAPACITY_TYPE_CPU);
        CapacityVO capacityMemory = _capacityDao.findByHostIdType(vm.getHostId(), CapacityVO.CAPACITY_TYPE_MEMORY);
        
        ServiceOfferingVO offeringVO = _offeringDao.findById(vm.getServiceOfferingId());
        int cpu = offeringVO.getCpu();
        int speed = offeringVO.getSpeed();
        int vmCPU = cpu * speed;
        int vmMem = offeringVO.getRamSize();    
        
		Transaction txn = Transaction.currentTxn();
        txn.start();
        
        try {
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

        	_capacityDao.update(capacityCpu.getId(), capacityCpu);
        	_capacityDao.update(capacityMemory.getId(), capacityMemory);
        } finally {
        	txn.commit();
        }
	}
}
