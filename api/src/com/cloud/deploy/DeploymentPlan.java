/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.deploy;

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.vm.ReservationContext;

/**
 * Describes how a VM should be deployed.
 */
public interface DeploymentPlan {
    // TODO: This interface is not fully developed. It really
    // should be more complicated than this and allow a
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
