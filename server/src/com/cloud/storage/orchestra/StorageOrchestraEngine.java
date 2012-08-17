package com.cloud.storage.orchestra;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.capacity.CapacityVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.pool.StoragePool;
import com.cloud.storage.volume.Volume;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;

public interface StorageOrchestraEngine {
	void prepareForMigration(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest);
    void prepare(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, boolean recreate) throws StorageUnavailableException, 
    InsufficientStorageCapacityException, ConcurrentOperationException;

    void allocateVolume(Long vmId, Pair<? extends DiskOfferingVO, Long> rootDiskOffering, 
    		List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
    		Long templateId, Account owner);
	VolumeVO createVolume(CreateVolumeCmd cmd);
	VolumeVO allocVolume(CreateVolumeCmd cmd)
			throws ResourceAllocationException;
}
