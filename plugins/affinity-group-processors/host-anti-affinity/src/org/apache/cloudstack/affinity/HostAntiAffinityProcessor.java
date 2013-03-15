package org.apache.cloudstack.affinity;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.log4j.Logger;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = AffinityGroupProcessor.class)
public class HostAntiAffinityProcessor extends AdapterBase implements AffinityGroupProcessor {

    private static final Logger s_logger = Logger.getLogger(HostAntiAffinityProcessor.class);
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Override
    public void process(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan,
            ExcludeList avoid)
            throws AffinityConflictException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        AffinityGroupVMMapVO vmGroupMapping = _affinityGroupVMMapDao.findByVmIdType(vm.getId(), getType());

        if (vmGroupMapping != null) {
            AffinityGroupVO group = _affinityGroupDao.findById(vmGroupMapping.getAffinityGroupId());

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Processing affinity group " + group.getName() + " for VM Id: " + vm.getId());
            }

            List<Long> groupVMIds = _affinityGroupVMMapDao.listVmIdsByAffinityGroup(group.getId());

            for (Long groupVMId : groupVMIds) {
                VMInstanceVO groupVM = _vmInstanceDao.findById(groupVMId);
                if (groupVM != null && !groupVM.isRemoved() && groupVM.getHostId() != null) {
                    avoid.addHost(groupVM.getHostId());
                }
            }
        }

    }

    @Override
    public String getType() {
        return "HostAntiAffinity";
    }

}
