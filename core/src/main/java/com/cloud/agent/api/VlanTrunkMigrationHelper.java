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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

    private static final int POST_MIGRATION_MAX_ATTEMPTS = 3;
    private static final long POST_MIGRATION_RETRY_DELAY_MS = 2000L;

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

    // A multi-VLAN trunk nic needs a VLAN-filtering host; a Guest VLAN 1 nic needs a legacy host, as the shared bridge's PVID is VLAN 1.
    // The shared bridge is only used when the zone setting is on, so a VLAN 1 nic is placeable anywhere without it. Null when the host fits.
    public static String getVlanFilteringUnsuitableReason(long vmId, long hostId, NicDao nicDao, NetworkDao networkDao, HostDetailsDao hostDetailsDao,
            boolean sharedVlanBridgeEnabled) {
        List<NicVO> nics = nicDao.listByVmId(vmId);
        boolean hasTrunkNic = hasTrunkNic(nics);
        boolean hasNativeVlanNic = sharedVlanBridgeEnabled && hasNativeVlanGuestNic(nics, networkDao);
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
    public static String getNativeVlanUnsuitableReason(Network network, long hostId, HostDetailsDao hostDetailsDao, boolean sharedVlanBridgeEnabled) {
        return sharedVlanBridgeEnabled && isNativeVlanGuestNetwork(network) && isVlanFilteringEnabled(hostId, hostDetailsDao) ? NATIVE_VLAN_ON_FILTERING_HOST_REASON : null;
    }

    public static boolean hasTrunkNic(Collection<NicVO> nics) {
        return nics.stream().anyMatch(NicVO::getMultiNetwork);
    }

    // Only Guest nics ride the shared bridge; Public, Management and Storage nics use their own bridges.
    public static boolean hasNativeVlanGuestNic(Collection<NicVO> nics, NetworkDao networkDao) {
        return nics.stream().anyMatch(nic -> isNativeVlanGuestNic(nic, networkDao));
    }

    public static boolean isNativeVlanGuestNetwork(Network network) {
        return network.getTrafficType() == Networks.TrafficType.Guest && isNativeVlan(network.getBroadcastUri());
    }

    private static boolean isVlanFilteringEnabled(long hostId, HostDetailsDao hostDetailsDao) {
        DetailVO vlanFilteringDetail = hostDetailsDao.findDetail(hostId, Host.HOST_VLAN_FILTERING_ENABLED);
        return vlanFilteringDetail != null && Boolean.parseBoolean(vlanFilteringDetail.getValue());
    }

    // a stopped Instance's nics carry no broadcast URI (released on stop), so fall back to the network's
    private static boolean isNativeVlanGuestNic(NicVO nic, NetworkDao networkDao) {
        if (nic.getMultiNetwork()) {
            return false;
        }
        URI nicBroadcastUri = nic.getBroadcastUri();
        if (nicBroadcastUri != null && !isNativeVlan(nicBroadcastUri)) {
            return false;
        }
        NetworkVO network = networkDao.findById(nic.getNetworkId());
        if (network == null || network.getTrafficType() != Networks.TrafficType.Guest) {
            return false;
        }
        return isNativeVlan(nicBroadcastUri != null ? nicBroadcastUri : network.getBroadcastUri());
    }

    private static boolean isNativeVlan(URI broadcastUri) {
        return broadcastUri != null && Networks.BroadcastDomainType.getSchemeValue(broadcastUri) == Networks.BroadcastDomainType.Vlan
                && "1".equals(Networks.BroadcastDomainType.getValue(broadcastUri));
    }

    // A plain VLAN nic only rides the shared bridge, and so needs manual membership, when the zone opt-in is on; a trunk nic always does.
    public static boolean nicNeedsManualVlanMembership(NicVO nic, boolean vlanFilteringEnabled, boolean sharedVlanBridgeEnabled) {
        return nicNeedsManualVlanMembership(nic, vlanFilteringEnabled && (nic.getMultiNetwork() || sharedVlanBridgeEnabled));
    }

    public static boolean nicNeedsManualVlanMembership(NicVO nic, boolean vlanFilteringEnabled) {
        return nic.getBroadcastUri() != null && Networks.BroadcastDomainType.getSchemeValue(nic.getBroadcastUri()) == Networks.BroadcastDomainType.Vlan
                && (nic.getMultiNetwork() || vlanFilteringEnabled);
    }

    // returns null when nothing was needed or the tasks completed, else the last failure so the caller can alert
    public static String sendPostMigrationVlanTrunkMembershipIfNeeded(VirtualMachineTO vmTO, long dstHostId,
            NicDao nicDao, HostDetailsDao hostDetailsDao, CommandSender commandSender, Logger logger) {
        return sendPostMigrationVlanTrunkMembershipIfNeeded(vmTO, dstHostId, nicDao, hostDetailsDao, commandSender, logger, POST_MIGRATION_RETRY_DELAY_MS);
    }

    static String sendPostMigrationVlanTrunkMembershipIfNeeded(VirtualMachineTO vmTO, long dstHostId,
            NicDao nicDao, HostDetailsDao hostDetailsDao, CommandSender commandSender, Logger logger, long retryDelayMs) {
        if (!vmNeedsPostMigrationVlanTrunkMembership(vmTO.getId(), dstHostId, nicDao, hostDetailsDao)) {
            return null;
        }
        String failure = sendPostMigrationCommandWithRetry(vmTO, dstHostId, commandSender, logger, retryDelayMs);
        if (failure != null) {
            logger.error("Post-migration VLAN trunk membership tasks did not complete for VM [{}] on destination host [{}]: {}. "
                    + "Migration completed but its tap may still be on the bridge's default VLAN; correct the VLAN membership or restart the VM.",
                    vmTO, dstHostId, failure);
        }
        return failure;
    }

    // returns null once an attempt succeeds, else the last failure after all attempts
    public static String sendPostMigrationCommandWithRetry(VirtualMachineTO vmTO, long dstHostId, CommandSender commandSender, Logger logger) {
        return sendPostMigrationCommandWithRetry(vmTO, dstHostId, commandSender, logger, POST_MIGRATION_RETRY_DELAY_MS);
    }

    static String sendPostMigrationCommandWithRetry(VirtualMachineTO vmTO, long dstHostId, CommandSender commandSender, Logger logger, long retryDelayMs) {
        String failure = null;
        for (int attempt = 1; attempt <= POST_MIGRATION_MAX_ATTEMPTS; attempt++) {
            failure = sendPostMigrationCommand(vmTO, dstHostId, commandSender);
            if (failure == null) {
                return null;
            }
            logger.warn("Post-migration attempt {}/{} failed for VM [{}] on destination host [{}]: {}",
                    attempt, POST_MIGRATION_MAX_ATTEMPTS, vmTO, dstHostId, failure);
            if (attempt < POST_MIGRATION_MAX_ATTEMPTS && !pauseBeforePostMigrationRetry(retryDelayMs)) {
                break;
            }
        }
        return failure;
    }

    // returns null on success, else the reason
    private static String sendPostMigrationCommand(VirtualMachineTO vmTO, long dstHostId, CommandSender commandSender) {
        try {
            Answer postMigrationAnswer = commandSender.send(dstHostId, new PostMigrationCommand(vmTO, vmTO.getName()));
            if (postMigrationAnswer != null && postMigrationAnswer.getResult()) {
                return null;
            }
            return postMigrationAnswer != null ? StringUtils.defaultIfBlank(postMigrationAnswer.getDetails(), "no details returned") : "null answer returned";
        } catch (Exception e) {
            return StringUtils.defaultIfBlank(e.getMessage(), e.toString());
        }
    }

    private static boolean pauseBeforePostMigrationRetry(long retryDelayMs) {
        try {
            Thread.sleep(retryDelayMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
