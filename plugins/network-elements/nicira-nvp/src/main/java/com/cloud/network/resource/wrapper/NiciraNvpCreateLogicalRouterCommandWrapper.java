//
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
//

package com.cloud.network.resource.wrapper;

import static com.cloud.network.resource.NiciraNvpResource.NAME_MAX_LEN;
import static com.cloud.network.resource.NiciraNvpResource.NUM_RETRIES;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.agent.api.CreateLogicalRouterCommand;
import com.cloud.network.nicira.L3GatewayAttachment;
import com.cloud.network.nicira.LogicalRouter;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.Match;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.nicira.PatchAttachment;
import com.cloud.network.nicira.RouterNextHop;
import com.cloud.network.nicira.SingleDefaultRouteImplicitRoutingConfig;
import com.cloud.network.nicira.SourceNatRule;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CreateLogicalRouterCommand.class)
public final class NiciraNvpCreateLogicalRouterCommandWrapper extends CommandWrapper<CreateLogicalRouterCommand, Answer, NiciraNvpResource> {

    private static final Logger s_logger = Logger.getLogger(NiciraNvpCreateLogicalRouterCommandWrapper.class);

    @Override
    public Answer execute(final CreateLogicalRouterCommand command, final NiciraNvpResource niciraNvpResource) {
        final String routerName = command.getName();
        final String gatewayServiceUuid = command.getGatewayServiceUuid();
        final String logicalSwitchUuid = command.getLogicalSwitchUuid();

        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account", command.getOwnerName()));

        final String publicNetworkNextHopIp = command.getPublicNextHop();
        final String publicNetworkIpAddress = command.getPublicIpCidr();
        final String internalNetworkAddress = command.getInternalIpCidr();

        s_logger.debug("Creating a logical router with external ip " + publicNetworkIpAddress + " and internal ip " + internalNetworkAddress + "on gateway service " +
                gatewayServiceUuid);

        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        try {
            // Create the Router
            LogicalRouter lrc = new LogicalRouter();
            lrc.setDisplayName(niciraNvpResource.truncate(routerName, NAME_MAX_LEN));
            lrc.setTags(tags);
            lrc.setRoutingConfig(new SingleDefaultRouteImplicitRoutingConfig(new RouterNextHop(publicNetworkNextHopIp)));
            lrc = niciraNvpApi.createLogicalRouter(lrc);

            // store the switchport for rollback
            LogicalSwitchPort lsp = null;

            try {
                // Create the outside port for the router
                LogicalRouterPort lrpo = new LogicalRouterPort();
                lrpo.setAdminStatusEnabled(true);
                lrpo.setDisplayName(niciraNvpResource.truncate(routerName + "-outside-port", NAME_MAX_LEN));
                lrpo.setTags(tags);
                final List<String> outsideIpAddresses = new ArrayList<String>();
                outsideIpAddresses.add(publicNetworkIpAddress);
                lrpo.setIpAddresses(outsideIpAddresses);
                lrpo = niciraNvpApi.createLogicalRouterPort(lrc.getUuid(), lrpo);

                // Attach the outside port to the gateway service on the correct VLAN
                final L3GatewayAttachment attachment = new L3GatewayAttachment(gatewayServiceUuid);
                if (command.getVlanId() != 0) {
                    attachment.setVlanId(command.getVlanId());
                }
                niciraNvpApi.updateLogicalRouterPortAttachment(lrc.getUuid(), lrpo.getUuid(), attachment);

                // Create the inside port for the router
                LogicalRouterPort lrpi = new LogicalRouterPort();
                lrpi.setAdminStatusEnabled(true);
                lrpi.setDisplayName(niciraNvpResource.truncate(routerName + "-inside-port", NAME_MAX_LEN));
                lrpi.setTags(tags);
                final List<String> insideIpAddresses = new ArrayList<String>();
                insideIpAddresses.add(internalNetworkAddress);
                lrpi.setIpAddresses(insideIpAddresses);
                lrpi = niciraNvpApi.createLogicalRouterPort(lrc.getUuid(), lrpi);

                // Create the inside port on the lswitch
                lsp = new LogicalSwitchPort(niciraNvpResource.truncate(routerName + "-inside-port", NAME_MAX_LEN), tags, true);
                lsp = niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, lsp);

                // Attach the inside router port to the lswitch port with a PatchAttachment
                niciraNvpApi.updateLogicalRouterPortAttachment(lrc.getUuid(), lrpi.getUuid(), new PatchAttachment(lsp.getUuid()));

                // Attach the inside lswitch port to the router with a PatchAttachment
                niciraNvpApi.updateLogicalSwitchPortAttachment(logicalSwitchUuid, lsp.getUuid(), new PatchAttachment(lrpi.getUuid()));

                // Setup the source nat rule
                final SourceNatRule snr = new SourceNatRule();
                snr.setToSourceIpAddressMin(publicNetworkIpAddress.split("/")[0]);
                snr.setToSourceIpAddressMax(publicNetworkIpAddress.split("/")[0]);
                final Match match = new Match();
                match.setSourceIpAddresses(internalNetworkAddress);
                snr.setMatch(match);
                snr.setOrder(200);
                niciraNvpApi.createLogicalRouterNatRule(lrc.getUuid(), snr);
            } catch (final NiciraNvpApiException e) {
                // We need to destroy the router if we already created it
                // this will also take care of any router ports and rules
                try {
                    niciraNvpApi.deleteLogicalRouter(lrc.getUuid());
                    if (lsp != null) {
                        niciraNvpApi.deleteLogicalSwitchPort(logicalSwitchUuid, lsp.getUuid());
                    }
                } catch (final NiciraNvpApiException ex) {
                }

                throw e;
            }

            return new CreateLogicalRouterAnswer(command, true, "Logical Router created (uuid " + lrc.getUuid() + ")", lrc.getUuid());
        } catch (final NiciraNvpApiException e) {
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, CreateLogicalRouterAnswer.class, e);
        }
    }
}
