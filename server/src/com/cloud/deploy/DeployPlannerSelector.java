package com.cloud.deploy;

import com.cloud.utils.component.Adapter;
import com.cloud.vm.UserVmVO;

public interface DeployPlannerSelector extends Adapter {
    String selectPlanner(UserVmVO vm);
}
