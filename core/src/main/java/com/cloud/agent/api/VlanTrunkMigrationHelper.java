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
package com.cloud.agent.api;

import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Networks;
import com.cloud.vm.dao.NicDao;

public class VlanTrunkMigrationHelper {

    // matches AgentManager.send's signature so callers can pass agentManager::send without core depending on AgentManager
    @FunctionalInterface
    public interface CommandSender {
        Answer send(Long hostId, Command cmd) throws AgentUnavailableException, OperationTimedoutException;
    }

    private VlanTrunkMigrationHelper() {
    }

    public static void populateVlanTrunkMigrationDetails(MigrateCommand migrateCommand, PrepareForMigrationAnswer prepareForMigrationAnswer,
            HostDetailsDao hostDetailsDao, long destHostId) {
        Map<String, String> nicBridgeMapping = prepareForMigrationAnswer.getNicBridgeMapping();
        if (MapUtils.isNotEmpty(nicBridgeMapping)) {
            migrateCommand.setNicBridgeMapping(nicBridgeMapping);
        }

        DetailVO destVlanFilteringDetail = hostDetailsDao.findDetail(destHostId, Host.HOST_VLAN_FILTERING_ENABLED);
        migrateCommand.setDestVlanFilteringEnabled(destVlanFilteringDetail == null ? null : Boolean.parseBoolean(destVlanFilteringDetail.getValue()));
        DetailVO destVlanTrunkXmlDetail = hostDetailsDao.findDetail(destHostId, Host.HOST_VLAN_TRUNK_XML_SUPPORTED);
        migrateCommand.setDestVlanTrunkXmlSupported(destVlanTrunkXmlDetail == null ? null : Boolean.parseBoolean(destVlanTrunkXmlDetail.getValue()));
    }

    public static boolean vmNeedsPostMigrationVlanTrunkMembership(long vmId, long dstHostId, NicDao nicDao, HostDetailsDao hostDetailsDao) {
        DetailVO trunkXmlDetail = hostDetailsDao.findDetail(dstHostId, Host.HOST_VLAN_TRUNK_XML_SUPPORTED);
        if (trunkXmlDetail != null && Boolean.parseBoolean(trunkXmlDetail.getValue())) {
            return false;
        }
        DetailVO vlanFilteringDetail = hostDetailsDao.findDetail(dstHostId, Host.HOST_VLAN_FILTERING_ENABLED);
        boolean destVlanFilteringEnabled = vlanFilteringDetail != null && Boolean.parseBoolean(vlanFilteringDetail.getValue());
        return nicDao.listByVmId(vmId).stream()
                .anyMatch(nic -> nic.getBroadcastUri() != null && Networks.BroadcastDomainType.getSchemeValue(nic.getBroadcastUri()) == Networks.BroadcastDomainType.Vlan
                        && (nic.getMultiNetwork() || destVlanFilteringEnabled));
    }

    public static void sendPostMigrationVlanTrunkMembershipIfNeeded(VirtualMachineTO vmTO, long dstHostId,
            NicDao nicDao, HostDetailsDao hostDetailsDao, CommandSender commandSender, Logger logger) {
        if (!vmNeedsPostMigrationVlanTrunkMembership(vmTO.getId(), dstHostId, nicDao, hostDetailsDao)) {
            return;
        }
        final String warningMsg = "Post-migration VLAN trunk membership tasks did not complete for VM [{}] "
                + "on destination host [{}]: {}. Migration completed but VLAN membership may need manual correction.";
        try {
            PostMigrationCommand postMigrationCommand = new PostMigrationCommand(vmTO, vmTO.getName());
            Answer postMigrationAnswer = commandSender.send(dstHostId, postMigrationCommand);
            if (postMigrationAnswer == null || !postMigrationAnswer.getResult()) {
                String details = postMigrationAnswer != null ? postMigrationAnswer.getDetails() : "null answer returned";
                logger.warn(warningMsg, vmTO, dstHostId, details);
            }
        } catch (Exception e) {
            logger.warn(warningMsg, vmTO, dstHostId, e.getMessage(), e);
        }
    }
}
