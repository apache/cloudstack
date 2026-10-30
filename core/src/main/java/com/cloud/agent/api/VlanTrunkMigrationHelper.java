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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;

public class VlanTrunkMigrationHelper {

    private static final String NATIVE_VLAN_ON_FILTERING_HOST_REASON =
            "Instance has a NIC on VLAN 1, the native VLAN of the guest bridge, but this host has VLAN filtering enabled";

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
        return nicDao.listByVmId(vmId).stream().anyMatch(nic -> nicNeedsManualVlanMembership(nic, destVlanFilteringEnabled));
    }

    // A multi-VLAN trunk nic needs a VLAN-filtering host; a VLAN 1 nic needs a legacy host, as the shared bridge's PVID is VLAN 1. Null when the host fits.
    public static String getVlanFilteringUnsuitableReason(long vmId, long hostId, NicDao nicDao, NetworkDao networkDao, HostDetailsDao hostDetailsDao) {
        List<NicVO> nics = nicDao.listByVmId(vmId);
        boolean hasTrunkNic = nics.stream().anyMatch(NicVO::getMultiNetwork);
        boolean hasNativeVlanNic = nics.stream().anyMatch(nic -> !nic.getMultiNetwork() && isNativeVlan(getEffectiveBroadcastUri(nic, networkDao)));
        if (!hasTrunkNic && !hasNativeVlanNic) {
            return null;
        }
        boolean vlanFilteringEnabled = isVlanFilteringEnabled(hostId, hostDetailsDao);
        if (hasTrunkNic && !vlanFilteringEnabled) {
            return "Instance has a multi-VLAN trunk NIC, but this host does not have VLAN filtering enabled";
        }
        if (hasNativeVlanNic && vlanFilteringEnabled) {
            return NATIVE_VLAN_ON_FILTERING_HOST_REASON;
        }
        return null;
    }

    // For a network about to be plugged into a running Instance on this host.
    public static String getNativeVlanUnsuitableReason(Network network, long hostId, HostDetailsDao hostDetailsDao) {
        return isNativeVlan(network.getBroadcastUri()) && isVlanFilteringEnabled(hostId, hostDetailsDao) ? NATIVE_VLAN_ON_FILTERING_HOST_REASON : null;
    }

    private static boolean isVlanFilteringEnabled(long hostId, HostDetailsDao hostDetailsDao) {
        DetailVO vlanFilteringDetail = hostDetailsDao.findDetail(hostId, Host.HOST_VLAN_FILTERING_ENABLED);
        return vlanFilteringDetail != null && Boolean.parseBoolean(vlanFilteringDetail.getValue());
    }

    // a stopped Instance's nics carry no broadcast URI (released on stop), so fall back to the network's
    private static URI getEffectiveBroadcastUri(NicVO nic, NetworkDao networkDao) {
        if (nic.getBroadcastUri() != null) {
            return nic.getBroadcastUri();
        }
        NetworkVO network = networkDao.findById(nic.getNetworkId());
        return network == null ? null : network.getBroadcastUri();
    }

    private static boolean isNativeVlan(URI broadcastUri) {
        return broadcastUri != null && Networks.BroadcastDomainType.getSchemeValue(broadcastUri) == Networks.BroadcastDomainType.Vlan
                && "1".equals(Networks.BroadcastDomainType.getValue(broadcastUri));
    }

    public static boolean nicNeedsManualVlanMembership(NicVO nic, boolean vlanFilteringEnabled) {
        return nic.getBroadcastUri() != null && Networks.BroadcastDomainType.getSchemeValue(nic.getBroadcastUri()) == Networks.BroadcastDomainType.Vlan
                && (nic.getMultiNetwork() || vlanFilteringEnabled);
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
