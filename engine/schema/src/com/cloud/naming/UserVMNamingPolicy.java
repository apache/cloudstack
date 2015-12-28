package com.cloud.naming;

import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmVO;

public interface UserVMNamingPolicy extends ResourceNamingPolicy<UserVm, UserVmVO>{

    public String getHostName(Long resourceId, Long accountId, String uuid);

    public String getInstanceName(Long resourceId, Long accountId, String uuidName);

    public String getServiceVmName(Long resourceId, Long accountId);
}
