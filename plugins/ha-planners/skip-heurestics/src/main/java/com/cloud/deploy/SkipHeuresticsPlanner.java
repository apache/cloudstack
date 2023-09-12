// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.deploy;

import com.cloud.vm.VirtualMachineProfile;
import org.apache.log4j.Logger;


import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public class SkipHeuresticsPlanner extends FirstFitPlanner implements HAPlanner {
    private static final Logger s_logger = Logger.getLogger(SkipHeuresticsPlanner.class);


    /**
     * This method should remove the clusters crossing capacity threshold
     * to avoid further vm allocation on it.
     *
     * In case of HA, we shouldn't consider this threshold as we have reserved the capacity for such emergencies.
     */
    @Override
    protected void removeClustersCrossingThreshold(List<Long> clusterListForVmAllocation, ExcludeList avoid,
                                                   VirtualMachineProfile vmProfile, DeploymentPlan plan){
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deploying vm during HA process, so skipping disable threshold check");
        }
        return;
    }

    @Override
    public boolean canHandle(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        return true;

    }

}
