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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;
import org.apache.cloudstack.network.contrail.model.NetworkPolicyModel;

import org.springframework.stereotype.Component;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.vm.ReservationContext;

@Component
public class ContrailVpcElementImpl extends ContrailElementImpl implements NetworkACLServiceProvider, VpcProvider {

    @Inject
    NetworkACLDao _networkACLDao;

    // NetworkElement API
    @Override
    public Provider getProvider() {
        return Provider.JuniperContrailVpcRouter;
    }

    @Override
    public boolean implementVpc(Vpc vpc, DeployDestination dest,
            ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        logger.debug("NetworkElement implementVpc");
        return true;
    }

    @Override
    public boolean shutdownVpc(Vpc vpc, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        logger.debug("NetworkElement shutdownVpc");
        return true;
    }

    @Override
    public boolean createPrivateGateway(PrivateGateway gateway)
            throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        logger.debug("NetworkElement createPrivateGateway");
        return false;
    }

    @Override
    public boolean deletePrivateGateway(PrivateGateway privateGateway)
            throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        logger.debug("NetworkElement deletePrivateGateway");
        return false;
    }

    @Override
    public boolean applyStaticRoutes(Vpc vpc, List<StaticRouteProfile> routes)
            throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        logger.debug("NetworkElement applyStaticRoutes");
        return true;
    }

    @Override
    public boolean applyNetworkACLs(Network net,
            List<? extends NetworkACLItem> rules)
                    throws ResourceUnavailableException {
        logger.debug("NetworkElement applyNetworkACLs");
        if (rules == null || rules.isEmpty()) {
            logger.debug("no rules to apply");
            return true;
        }

        Long aclId = rules.get(0).getAclId();
        NetworkACLVO acl = _networkACLDao.findById(aclId);
        NetworkPolicyModel policyModel = _manager.getDatabase().lookupNetworkPolicy(acl.getUuid());
        if (policyModel == null) {
            /*
             * For the first time, when a CS ACL applied to a network, create a network-policy in VNC
             * and when there are no networks associated to CS ACL, delete it from VNC.
             */
            policyModel = new NetworkPolicyModel(acl.getUuid(), acl.getName());
            net.juniper.contrail.api.types.Project project;
            try {
                project = _manager.getVncProject(net.getDomainId(), net.getAccountId());
                if (project == null) {
                    project = _manager.getDefaultVncProject();
                }
            } catch (IOException ex) {
                logger.warn("read project", ex);
                return false;
            }
            policyModel.setProject(project);
        }

        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(net.getUuid(),
                _manager.getCanonicalName(net), net.getTrafficType());
        NetworkPolicyModel oldPolicyModel = null;
        /* this method is called when network is destroyed too, hence vn model might have been deleted already */
        if (vnModel != null) {
            oldPolicyModel = vnModel.getNetworkPolicyModel();
            vnModel.addToNetworkPolicy(policyModel);
        }

        try {
            policyModel.build(_manager.getModelController(), rules);
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            return false;
        }

        try {
            if (!policyModel.verify(_manager.getModelController())) {
                policyModel.update(_manager.getModelController());
            }
            _manager.getDatabase().getNetworkPolicys().add(policyModel);
        } catch (Exception ex) {
            logger.error("network-policy update: ", ex);
            ex.printStackTrace();
            return false;
        }

        if (!policyModel.hasPolicyRules()) {
            try {
                policyModel.delete(_manager.getModelController());
                _manager.getDatabase().getNetworkPolicys().remove(policyModel);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        /*
         * if no other VNs are associated with the old policy,
         * we could delete it from the Contrail VNC
         */
        if (policyModel != oldPolicyModel && oldPolicyModel != null && !oldPolicyModel.hasDescendents()) {
            try {
                oldPolicyModel.delete(_manager.getModelController());
                _manager.getDatabase().getNetworkPolicys().remove(oldPolicyModel);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean applyACLItemsToPrivateGw(PrivateGateway privateGateway,
            List<? extends NetworkACLItem> rules)
                    throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        logger.debug("NetworkElement applyACLItemsToPrivateGw");
        return true;
    }

}
