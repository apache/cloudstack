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
 */
public interface DeploymentPlan {
    // TODO: This interface is not fully developed. It really
    // number of parameters to be specified.

    /**
     * @return data center the VM should deploy in.
     */
    public long getDataCenterId();

    /**
     * @return pod the Vm should deploy in; null if no preference.
     */
    public Long getPodId();

    /**
     * @return cluster the VM should deploy in; null if no preference.
     */
    public Long getClusterId();

    /**
     * @return host the VM should deploy in; null if no preference.
     */
    public Long getHostId();

    /**
     * @return pool the VM should be created in; null if no preference.
     */
    public Long getPoolId();

    /**
     * @param avoids
     *            Set the ExcludeList to avoid for deployment
     */
    public void setAvoids(ExcludeList avoids);

    /**
     * @return
     *         the ExcludeList to avoid for deployment
     */
    public ExcludeList getAvoids();

    Long getPhysicalNetworkId();

    ReservationContext getReservationContext();
}
