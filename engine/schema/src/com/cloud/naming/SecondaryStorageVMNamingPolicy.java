package com.cloud.naming;

import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;

public interface SecondaryStorageVMNamingPolicy extends ResourceNamingPolicy<SecondaryStorageVm, SecondaryStorageVmVO> {

    public String getSsvmName(Long id);

}
