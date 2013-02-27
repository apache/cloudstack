package org.apache.cloudstack.storage.allocator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class ZoneWideStoragePoolAllocator extends AbstractStoragePoolAllocator {
	private static final Logger s_logger = Logger.getLogger(ZoneWideStoragePoolAllocator.class);
	@Inject StoragePoolDao _storagePoolDao; 
	@Inject DataStoreManager dataStoreMgr; 
	
	@Override
	protected boolean filter(ExcludeList avoid, StoragePool pool, DiskProfile dskCh, 
			 DeploymentPlan plan) {
        Volume volume =  _volumeDao.findById(dskCh.getVolumeId());
        List<Volume> requestVolumes = new ArrayList<Volume>();
        requestVolumes.add(volume);
        return storageMgr.storagePoolHasEnoughSpace(requestVolumes, pool);
	}
	
	@Override
	protected List<StoragePool> select(DiskProfile dskCh,
			VirtualMachineProfile<? extends VirtualMachine> vmProfile,
			DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
		List<StoragePool> suitablePools = new ArrayList<StoragePool>();
		HypervisorType hypervisor = vmProfile.getHypervisorType();
		if (hypervisor != null) {
			if (hypervisor != HypervisorType.KVM) {
				s_logger.debug("Only kvm supports zone wide storage");
				return suitablePools;
			}
		}
		
		List<StoragePoolVO> storagePools = _storagePoolDao.findZoneWideStoragePoolsByTags(plan.getDataCenterId(), dskCh.getTags());
	
		for (StoragePoolVO storage : storagePools) {
			if (suitablePools.size() == returnUpTo) {
        		break;
        	}
			StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(storage.getId());
			if (filter(avoid, pol, dskCh, plan)) {
				suitablePools.add(pol);
			}
		}
		return suitablePools;
	}
}
