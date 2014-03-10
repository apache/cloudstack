package com.cloud.network.ovs;

import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Local;
import javax.inject.Inject;
import org.springframework.stereotype.Component;

@Component
@Local(value = {OvsNetworkTopologyGuru.class})
public class OvsNetworkTopologyGuruImpl extends ManagerBase implements OvsNetworkTopologyGuru {

    @Inject
    UserVmDao _userVmDao;
    @Inject
    DomainRouterDao _routerDao;

    /**
     * get the list of hypervisor hosts on which VM's belonging to a network currently spans
     */
    public  List<Long> getNetworkSpanedHosts(long networkId) {
        List<Long> hostIds = new ArrayList<Long>();
        // Find active VMs with a NIC on the target network
        List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(networkId,
                VirtualMachine.State.Running, VirtualMachine.State.Starting, VirtualMachine.State.Stopping, VirtualMachine.State.Unknown,
                VirtualMachine.State.Migrating);
        // Find routers for the network
        List<DomainRouterVO> routers = _routerDao.findByNetwork(networkId);
        List<VMInstanceVO> ins = new ArrayList<VMInstanceVO>();
        if (vms != null) {
            ins.addAll(vms);
        }
        if (routers.size() != 0) {
            ins.addAll(routers);
        }
        for (VMInstanceVO v : ins) {
            Long rh = v.getHostId();
            if (rh == null) {
                continue;
            }
            if (!hostIds.contains(rh)) {
                hostIds.add(rh);
            }
        }
        return  hostIds;
    }

    @Override
    public List<Long> getVpcSpannedHosts(long vpId) {
        return null;
    }

    @Override
    public List<Long> getVpcOnHost(long hostId) {
        return null;
    }

    @Override
    public List<Long> getAllActiveVmsInVpc(long vpcId) {
        return null;
    }

    @Override
    public List<Long> getActiveVmsInVpcOnHost(long vpcId, long hostId) {
        return null;
    }
}
