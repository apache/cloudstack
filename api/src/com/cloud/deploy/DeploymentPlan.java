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

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.vm.ReservationContext;

/**
 * DeploymentPlan gives explicit direction on where to deploy a virtual machine.
 * 
 */
public interface DeploymentPlan {
    /**
     * @return data center the VM should deploy in.
     */
    long getDataCenterId();

    /**
     * @return pod the Vm should deploy in; null if no preference.
     */
    Long getPodId();

    /**
     * @return cluster the VM should deploy in; null if no preference.
     */
    Long getClusterId();

    /**
     * @return host the VM should deploy in; null if no preference.
     */
    Long getHostId();

    /**
     * @return pool the VM should be created in; null if no preference.
     */
    Long getPoolId();

    /**
     * @param avoids Set the ExcludeList to avoid for deployment
     */
    void setAvoids(ExcludeList avoids);

    /**
     * @return the ExcludeList to avoid for deployment
     */
    ExcludeList getAvoids();

    Long getPhysicalNetworkId();

    ReservationContext getReservationContext();
}
