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
package com.cloud.vpc;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.vpn.CreateVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnConnectionsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnCustomerGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpn.ResetVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.UpdateVpnCustomerGatewayCmd;
import org.springframework.stereotype.Component;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.network.vpn.Site2SiteVpnService;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.DomainRouterVO;

@Component
public class MockSite2SiteVpnManagerImpl extends ManagerBase implements Site2SiteVpnManager, Site2SiteVpnService {

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#createVpnGateway(org.apache.cloudstack.api.commands.CreateVpnGatewayCmd)
     */
    @Override
    public Site2SiteVpnGateway createVpnGateway(CreateVpnGatewayCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#createCustomerGateway(org.apache.cloudstack.api.commands.CreateVpnCustomerGatewayCmd)
     */
    @Override
    public Site2SiteCustomerGateway createCustomerGateway(CreateVpnCustomerGatewayCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#startVpnConnection(long)
     */
    @Override
    public Site2SiteVpnConnection startVpnConnection(long id) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#getVpnGateway(java.lang.Long)
     */
    @Override
    public Site2SiteVpnGateway getVpnGateway(Long vpnGatewayId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#getCustomerGateway(java.lang.Long)
     */
    @Override
    public Site2SiteCustomerGateway getCustomerGateway(Long customerGatewayId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#createVpnConnection(org.apache.cloudstack.api.commands.CreateVpnConnectionCmd)
     */
    @Override
    public Site2SiteVpnConnection createVpnConnection(CreateVpnConnectionCmd cmd) throws NetworkRuleConflictException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#deleteCustomerGateway(org.apache.cloudstack.api.commands.DeleteVpnCustomerGatewayCmd)
     */
    @Override
    public boolean deleteCustomerGateway(DeleteVpnCustomerGatewayCmd deleteVpnCustomerGatewayCmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#deleteVpnGateway(org.apache.cloudstack.api.commands.DeleteVpnGatewayCmd)
     */
    @Override
    public boolean deleteVpnGateway(DeleteVpnGatewayCmd deleteVpnGatewayCmd) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#deleteVpnConnection(org.apache.cloudstack.api.commands.DeleteVpnConnectionCmd)
     */
    @Override
    public boolean deleteVpnConnection(DeleteVpnConnectionCmd deleteVpnConnectionCmd) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#resetVpnConnection(org.apache.cloudstack.api.commands.ResetVpnConnectionCmd)
     */
    @Override
    public Site2SiteVpnConnection resetVpnConnection(ResetVpnConnectionCmd resetVpnConnectionCmd) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#searchForCustomerGateways(org.apache.cloudstack.api.commands.ListVpnCustomerGatewaysCmd)
     */
    @Override
    public Pair<List<? extends Site2SiteCustomerGateway>, Integer> searchForCustomerGateways(ListVpnCustomerGatewaysCmd listVpnCustomerGatewaysCmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#searchForVpnGateways(org.apache.cloudstack.api.commands.ListVpnGatewaysCmd)
     */
    @Override
    public Pair<List<? extends Site2SiteVpnGateway>, Integer> searchForVpnGateways(ListVpnGatewaysCmd listVpnGatewaysCmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#searchForVpnConnections(org.apache.cloudstack.api.commands.ListVpnConnectionsCmd)
     */
    @Override
    public Pair<List<? extends Site2SiteVpnConnection>, Integer> searchForVpnConnections(ListVpnConnectionsCmd listVpnConnectionsCmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnService#updateCustomerGateway(org.apache.cloudstack.api.commands.UpdateVpnCustomerGatewayCmd)
     */
    @Override
    public Site2SiteCustomerGateway updateCustomerGateway(UpdateVpnCustomerGatewayCmd updateVpnCustomerGatewayCmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnManager#cleanupVpnConnectionByVpc(long)
     */
    @Override
    public boolean cleanupVpnConnectionByVpc(long vpcId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnManager#cleanupVpnGatewayByVpc(long)
     */
    @Override
    public boolean cleanupVpnGatewayByVpc(long vpcId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnManager#markDisconnectVpnConnByVpc(long)
     */
    @Override
    public void markDisconnectVpnConnByVpc(long vpcId) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnManager#getConnectionsForRouter(com.cloud.vm.DomainRouterVO)
     */
    @Override
    public List<Site2SiteVpnConnectionVO> getConnectionsForRouter(DomainRouterVO router) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnManager#deleteCustomerGatewayByAccount(long)
     */
    @Override
    public boolean deleteCustomerGatewayByAccount(long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpn.Site2SiteVpnManager#reconnectDisconnectedVpnByVpc(java.lang.Long)
     */
    @Override
    public void reconnectDisconnectedVpnByVpc(Long vpcId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Site2SiteVpnConnection updateVpnConnection(long id, String customId, Boolean forDisplay) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Site2SiteVpnGateway updateVpnGateway(Long id, String customId, Boolean forDisplay) {
        // TODO Auto-generated method stub
        return null;
    }

}
