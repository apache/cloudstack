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

import static com.cloud.network.resource.NiciraNvpResource.NUM_RETRIES;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterCommand;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.network.nicira.NatRule;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpList;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  ConfigureStaticNatRulesOnLogicalRouterCommand.class)
public final class NiciraNvpConfigureStaticNatRulesCommandWrapper extends CommandWrapper<ConfigureStaticNatRulesOnLogicalRouterCommand, Answer, NiciraNvpResource> {

    private static final Logger s_logger = Logger.getLogger(NiciraNvpConfigureStaticNatRulesCommandWrapper.class);

    @Override
    public Answer execute(final ConfigureStaticNatRulesOnLogicalRouterCommand command, final NiciraNvpResource niciraNvpResource) {
        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        try {
            final NiciraNvpList<NatRule> existingRules = niciraNvpApi.findNatRulesByLogicalRouterUuid(command.getLogicalRouterUuid());
            // Rules of the game (also known as assumptions-that-will-make-stuff-break-later-on)
            // A SourceNat rule with a match other than a /32 cidr is assumed to be the "main" SourceNat rule
            // Any other SourceNat rule should have a corresponding DestinationNat rule

            for (final StaticNatRuleTO rule : command.getRules()) {

                final NatRule[] rulepair = niciraNvpResource.generateStaticNatRulePair(rule.getDstIp(), rule.getSrcIp());

                NatRule incoming = null;
                NatRule outgoing = null;

                for (final NatRule storedRule : existingRules.getResults()) {
                    if (storedRule.equalsIgnoreUuid(rulepair[1])) {
                        // The outgoing rule exists
                        outgoing = storedRule;
                        s_logger.debug("Found matching outgoing rule " + outgoing.getUuid());
                        if (incoming != null) {
                            break;
                        }
                    } else if (storedRule.equalsIgnoreUuid(rulepair[0])) {
                        // The incoming rule exists
                        incoming = storedRule;
                        s_logger.debug("Found matching incoming rule " + incoming.getUuid());
                        if (outgoing != null) {
                            break;
                        }
                    }
                }
                if (incoming != null && outgoing != null) {
                    if (rule.revoked()) {
                        s_logger.debug("Deleting incoming rule " + incoming.getUuid());
                        niciraNvpApi.deleteLogicalRouterNatRule(command.getLogicalRouterUuid(), incoming.getUuid());

                        s_logger.debug("Deleting outgoing rule " + outgoing.getUuid());
                        niciraNvpApi.deleteLogicalRouterNatRule(command.getLogicalRouterUuid(), outgoing.getUuid());
                    }
                } else {
                    if (rule.revoked()) {
                        s_logger.warn("Tried deleting a rule that does not exist, " + rule.getSrcIp() + " -> " + rule.getDstIp());
                        break;
                    }

                    rulepair[0] = niciraNvpApi.createLogicalRouterNatRule(command.getLogicalRouterUuid(), rulepair[0]);
                    s_logger.debug("Created " + niciraNvpResource.natRuleToString(rulepair[0]));

                    try {
                        rulepair[1] = niciraNvpApi.createLogicalRouterNatRule(command.getLogicalRouterUuid(), rulepair[1]);
                        s_logger.debug("Created " + niciraNvpResource.natRuleToString(rulepair[1]));
                    } catch (final NiciraNvpApiException ex) {
                        s_logger.debug("Failed to create SourceNatRule, rolling back DestinationNatRule");
                        niciraNvpApi.deleteLogicalRouterNatRule(command.getLogicalRouterUuid(), rulepair[0].getUuid());
                        throw ex; // Rethrow original exception
                    }

                }
            }
            return new ConfigureStaticNatRulesOnLogicalRouterAnswer(command, true, command.getRules().size() + " StaticNat rules applied");
        } catch (final NiciraNvpApiException e) {
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, ConfigureStaticNatRulesOnLogicalRouterAnswer.class, e);
        }
    }
}