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
package com.cloud.network.vpn;

import java.util.List;

import com.cloud.api.commands.CreateVpnConnectionCmd;
import com.cloud.api.commands.CreateVpnCustomerGatewayCmd;
import com.cloud.api.commands.CreateVpnGatewayCmd;
import com.cloud.api.commands.DeleteVpnConnectionCmd;
import com.cloud.api.commands.DeleteVpnCustomerGatewayCmd;
import com.cloud.api.commands.DeleteVpnGatewayCmd;
import com.cloud.api.commands.ListVpnConnectionsCmd;
import com.cloud.api.commands.ListVpnCustomerGatewaysCmd;
import com.cloud.api.commands.ListVpnGatewaysCmd;
import com.cloud.api.commands.ResetVpnConnectionCmd;
import com.cloud.api.commands.UpdateVpnCustomerGatewayCmd;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.utils.Pair;

public interface Site2SiteVpnService {
    Site2SiteVpnGateway createVpnGateway(CreateVpnGatewayCmd cmd);
    Site2SiteCustomerGateway createCustomerGateway(CreateVpnCustomerGatewayCmd cmd);
    Site2SiteVpnConnection startVpnConnection(long id) throws ResourceUnavailableException;
    Site2SiteVpnGateway getVpnGateway(Long vpnGatewayId);
    Site2SiteVpnConnection createVpnConnection(CreateVpnConnectionCmd cmd) throws NetworkRuleConflictException;
    boolean deleteCustomerGateway(DeleteVpnCustomerGatewayCmd deleteVpnCustomerGatewayCmd);
    boolean deleteVpnGateway(DeleteVpnGatewayCmd deleteVpnGatewayCmd);
    boolean deleteVpnConnection(DeleteVpnConnectionCmd deleteVpnConnectionCmd) throws ResourceUnavailableException;
    Site2SiteVpnConnection resetVpnConnection(ResetVpnConnectionCmd resetVpnConnectionCmd) throws ResourceUnavailableException;
    Pair<List<? extends Site2SiteCustomerGateway>, Integer> searchForCustomerGateways(ListVpnCustomerGatewaysCmd listVpnCustomerGatewaysCmd);
    Pair<List<? extends Site2SiteVpnGateway>, Integer> searchForVpnGateways(ListVpnGatewaysCmd listVpnGatewaysCmd);
    Pair<List<? extends Site2SiteVpnConnection>, Integer> searchForVpnConnections(ListVpnConnectionsCmd listVpnConnectionsCmd);
    Site2SiteCustomerGateway updateCustomerGateway(UpdateVpnCustomerGatewayCmd updateVpnCustomerGatewayCmd);
}
