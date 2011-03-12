package com.cloud.capacity;

import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine;

/**
 * Capacity Manager manages the different capacities
 * available within the Cloud Stack.
 *
 */
public interface CapacityManager extends Manager {
    public boolean releaseVmCapacity(VirtualMachine vm, boolean moveFromReserved, boolean moveToReservered, Long hostId);

    void allocateVmCapacity(VirtualMachine vm, boolean fromLastHost);
    
    boolean checkIfHostHasCapacity(long hostId, Integer cpu, long ram, boolean checkFromReservedCapacity);
}
