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

import java.util.List;

import javax.ejb.Local;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=DeploymentPlanner.class)
public class SimplePlanner extends PlannerBase implements DeploymentPlanner {
    @Inject DataCenterDao _dcDao;
    @Inject HostPodDao _podDao;
    @Inject HostDao _hostDao;
    @Inject ClusterDao _clusterDao;
    
    @Override
    public DeployDestination plan(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) {
        DataCenterVO dc = _dcDao.findById(plan.getDataCenterId());
        List<HostVO> hosts = _hostDao.listBy(Type.Routing, plan.getDataCenterId());
        
        if (hosts.size() == 0) {
            return null;
        }
        
        Host host = hosts.get(0);
        Pod pod = _podDao.findById(host.getPodId());
        
        Cluster cluster = null;
        if (host.getClusterId() != null) {
            cluster = _clusterDao.findById(host.getClusterId());
        }
        
        return new DeployDestination(dc, pod, cluster, host);
    }
    
    public boolean check(VirtualMachineProfile vm, DeploymentPlan plan, DeployDestination dest, ExcludeList avoid) {
        return true;
    }
    
    protected SimplePlanner() {
        super();
    }
}
