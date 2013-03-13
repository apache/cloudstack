package com.cloud.deploy;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.log4j.Logger;

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { DeploymentPlanningManager.class })
public class DeploymentPlanningManagerImpl extends ManagerBase implements DeploymentPlanningManager, Manager {

    private static final Logger s_logger = Logger.getLogger(DeploymentPlanningManagerImpl.class);
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    protected List<DeploymentPlanner> _planners;

    @Inject
    protected List<AffinityGroupProcessor> _affinityProcessors;

    @Override
    public DeployDestination planDeployment(VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoids) throws InsufficientServerCapacityException,
            AffinityConflictException {

        // call affinitygroup chain
        VirtualMachine vm = vmProfile.getVirtualMachine();
        long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            for (AffinityGroupProcessor processor : _affinityProcessors) {
                processor.process(vmProfile, plan, avoids);
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deploy avoids pods: " + avoids.getPodsToAvoid() + ", clusters: "
                    + avoids.getClustersToAvoid() + ", hosts: " + avoids.getHostsToAvoid());
        }

        // call planners
        DeployDestination dest = null;
        for (DeploymentPlanner planner : _planners) {
            if (planner.canHandle(vmProfile, plan, avoids)) {
                dest = planner.plan(vmProfile, plan, avoids);
            } else {
                continue;
            }
            if (dest != null) {
                avoids.addHost(dest.getHost().getId());
                break;
            }

        }
        return dest;
    }

}
