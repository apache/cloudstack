package net.juniper.contrail.management;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.UserVmVO;

public class ServiceVirtualMachine extends UserVmVO {
    public ServiceVirtualMachine(long id, String instanceName, String name, long templateId, long serviceOfferingId,
            HypervisorType hypervisorType, long guestOSId, long dataCenterId, long domainId, long accountId,
            boolean haEnabled) {
        super(id, instanceName, name, templateId, hypervisorType, guestOSId, false, false, domainId, accountId,
              serviceOfferingId, null, name, null);
    }
}
