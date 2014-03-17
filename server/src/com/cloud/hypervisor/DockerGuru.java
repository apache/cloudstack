package com.cloud.hypervisor;

import javax.ejb.Local;
import javax.inject.Inject;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = HypervisorGuru.class)
public class DockerGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject
    GuestOSDao _guestOsDao;

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.Docker;
    }
    
    protected DockerGuru() {
        super();
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);

        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());

        return to;
    }
    
    @Override
    public boolean trackVmHostChange() {
        return false;
    }
}
