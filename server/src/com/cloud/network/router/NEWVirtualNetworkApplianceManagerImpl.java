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
package com.cloud.network.router;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.router.UpgradeRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterTemplateCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.maint.Version;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachineProfile.Param;


/**
 * NetworkManager manages the network for the different end users.
 *
 */
@Local(value = { NEWVirtualNetworkApplianceManager.class, VirtualNetworkApplianceService.class })
public class NEWVirtualNetworkApplianceManagerImpl implements NEWVirtualNetworkApplianceManager {

    private static final Logger s_logger = Logger.getLogger(NEWVirtualNetworkApplianceManagerImpl.class);

    static final ConfigKey<Boolean> routerVersionCheckEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "router.version.check", "true",
            "If true, router minimum required version is checked before sending command", false);

    @Inject
    private AgentManager _agentMgr;

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setName(final String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(final Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(final int level) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params)
            throws ConfigurationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public VirtualRouter startRouter(final long routerId, final boolean reprogramNetwork)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter rebootRouter(final long routerId, final boolean reprogramNetwork)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter upgradeRouter(final UpgradeRouterCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter stopRouter(final long routerId, final boolean forced)
            throws ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter startRouter(final long id)
            throws ResourceUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter destroyRouter(final long routerId, final Account caller,
            final Long callerUserId) throws ResourceUnavailableException,
            ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter findRouter(final long routerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> upgradeRouterTemplate(final UpgradeRouterTemplateCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DomainRouterVO> deployVirtualRouterInGuestNetwork(
            final Network guestNetwork, final DeployDestination dest, final Account owner,
            final Map<Param, Object> params, final boolean isRedundant)
                    throws InsufficientCapacityException, ResourceUnavailableException,
                    ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean startRemoteAccessVpn(final Network network, final RemoteAccessVpn vpn,
            final List<? extends VirtualRouter> routers)
                    throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteRemoteAccessVpn(final Network network, final RemoteAccessVpn vpn,
            final List<? extends VirtualRouter> routers)
                    throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<VirtualRouter> getRoutersForNetwork(final long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouter stop(final VirtualRouter router, final boolean forced,
            final User callingUser, final Account callingAccount)
                    throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDnsBasicZoneUpdate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeDhcpSupportForSubnet(final Network network,
            final List<DomainRouterVO> routers) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean prepareAggregatedExecution(final Network network,
            final List<DomainRouterVO> routers) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean completeAggregatedExecution(final Network network,
            final List<DomainRouterVO> routers) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean cleanupAggregatedExecution(final Network network,
            final List<DomainRouterVO> routers) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean sendCommandsToRouter(final VirtualRouter router, final Commands cmds) throws AgentUnavailableException {
        if(!checkRouterVersion(router)){
            s_logger.debug("Router requires upgrade. Unable to send command to router:" + router.getId() + ", router template version : " + router.getTemplateVersion()
                    + ", minimal required version : " + MinVRVersion);
            throw new CloudRuntimeException("Unable to send command. Upgrade in progress. Please contact administrator.");
        }
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmds);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", router.getHostId(), e);
        }

        if (answers == null) {
            return false;
        }

        if (answers.length != cmds.size()) {
            return false;
        }

        // FIXME: Have to return state for individual command in the future
        boolean result = true;
        if (answers.length > 0) {
            for (final Answer answer : answers) {
                if (!answer.getResult()) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    // Checks if the router is at the required version
    // Compares MS version and router version
    protected boolean checkRouterVersion(final VirtualRouter router) {
        if(!routerVersionCheckEnabled.value()){
            //Router version check is disabled.
            return true;
        }
        if(router.getTemplateVersion() == null){
            return false;
        }
        final String trimmedVersion = Version.trimRouterVersion(router.getTemplateVersion());
        return (Version.compare(trimmedVersion, MinVRVersion) >= 0);
    }
}