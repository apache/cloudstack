package com.cloud.network.ovs;

import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.NicVO;
import com.cloud.vm.Nic;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @Inject
    VpcManager _vpcMgr;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    NicDao _nicDao;
    @Inject
    NetworkDao _networkDao;

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

    /**
     * get the list of hypervisor hosts on which VM's belonging to a VPC currently spans
     */
    @Override
    public List<Long> getVpcSpannedHosts(long vpcId) {
        List<? extends Network> vpcNetworks =  _vpcMgr.getVpcNetworks(vpcId);
        List<Long> vpcHostIds = new ArrayList<>();
        for (Network vpcNetwork : vpcNetworks) {
            List<Long> networkHostIds = new ArrayList<Long>();
            networkHostIds = getNetworkSpanedHosts(vpcNetwork.getId());
            if (networkHostIds != null && !networkHostIds.isEmpty()) {
                for (Long hostId : networkHostIds) {
                    if (!vpcHostIds.contains(hostId)) {
                        vpcHostIds.add(hostId);
                    }
                }
            }
        }
        return vpcHostIds;
    }

    @Override
    public List<Long> getVpcOnHost(long hostId) {
        List<Long> vpcIds = new ArrayList<>();
        List<VMInstanceVO> vmInstances = _vmInstanceDao.listByHostId(hostId);
        for (VMInstanceVO instance : vmInstances) {
            List<NicVO> nics = _nicDao.listByVmId(instance.getId());
            for (Nic nic: nics) {
                Network network = _networkDao.findById(nic.getNetworkId());
                if (network.getTrafficType() == Networks.TrafficType.Guest && network.getVpcId() != null) {
                    if (!vpcIds.contains(network.getVpcId())) {
                        vpcIds.add(network.getVpcId());
                    }
                }
            }
        }
        return vpcIds;
    }

    @Override
    public List<Long> getAllActiveVmsInNetwork(long networkId) {
        List <Long> vmIds = new ArrayList<>();
        List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(networkId,
                VirtualMachine.State.Running, VirtualMachine.State.Starting, VirtualMachine.State.Stopping, VirtualMachine.State.Unknown,
                VirtualMachine.State.Migrating);
        // Find routers for the network
        List<DomainRouterVO> routers = _routerDao.findByNetwork(networkId);
        List<VMInstanceVO> ins = new ArrayList<VMInstanceVO>();

        if (vms != null) {
            for (UserVmVO vm : vms) {
                vmIds.add(vm.getId());
            }
        }
        if (routers.size() != 0) {
            for (DomainRouterVO router: routers) {
                vmIds.add(router.getId());
            }
        }
        return  vmIds;
    }

    @Override
    public List<Long> getAllActiveVmsInVpc(long vpcId) {

        Set<Long> vmIdsSet = new HashSet<>();
        List<? extends Network> vpcNetworks =  _vpcMgr.getVpcNetworks(vpcId);
        for (Network network : vpcNetworks) {
            List<Long> networkVmIds = getAllActiveVmsInNetwork(network.getId());
            if (networkVmIds  != null && !networkVmIds.isEmpty()) {
                vmIdsSet.addAll(networkVmIds);
            }
        }
        List<Long> vmIds = new ArrayList<>();
        vmIds.addAll(vmIdsSet);
        return vmIds;
    }

    @Override
    public List<Long> getActiveVmsInVpcOnHost(long vpcId, long hostId) {
        Set<Long> vmIdsSet = new HashSet<>();
        List<? extends Network> vpcNetworks =  _vpcMgr.getVpcNetworks(vpcId);
        for (Network network : vpcNetworks) {
            List<Long> networkVmIds = getActiveVmsInNetworkOnHost(network.getId(), hostId);
            if (networkVmIds  != null && !networkVmIds.isEmpty()) {
                vmIdsSet.addAll(networkVmIds);
            }
        }
        List<Long> vmIds = new ArrayList<>();
        vmIds.addAll(vmIdsSet);
        return vmIds;
    }

    @Override
    public List<Long> getActiveVmsInNetworkOnHost(long networkId, long hostId) {
        List <Long> vmIds = new ArrayList<>();
        List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(networkId,
                VirtualMachine.State.Running, VirtualMachine.State.Starting, VirtualMachine.State.Stopping, VirtualMachine.State.Unknown,
                VirtualMachine.State.Migrating);
        // Find routers for the network
        List<DomainRouterVO> routers = _routerDao.findByNetwork(networkId);
        List<VMInstanceVO> ins = new ArrayList<VMInstanceVO>();

        if (vms != null) {
            for (UserVmVO vm : vms) {
                if (vm.getHostId() == hostId)
                    vmIds.add(vm.getId());
            }
        }
        if (routers.size() != 0) {
            for (DomainRouterVO router: routers) {
                if (router.getHostId() == hostId)
                    vmIds.add(router.getId());
            }
        }
        return  vmIds;
    }
}
