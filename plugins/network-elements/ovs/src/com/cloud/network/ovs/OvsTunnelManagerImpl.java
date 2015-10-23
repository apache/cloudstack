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
package com.cloud.network.ovs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.persistence.EntityExistsException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.OvsCreateTunnelAnswer;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.agent.api.OvsDestroyTunnelCommand;
import com.cloud.agent.api.OvsFetchInterfaceAnswer;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.agent.api.OvsSetupBridgeCommand;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.ovs.dao.OvsTunnel;
import com.cloud.network.ovs.dao.OvsTunnelInterfaceDao;
import com.cloud.network.ovs.dao.OvsTunnelInterfaceVO;
import com.cloud.network.ovs.dao.OvsTunnelNetworkDao;
import com.cloud.network.ovs.dao.OvsTunnelNetworkVO;
import com.cloud.network.ovs.dao.VpcDistributedRouterSeqNoDao;
import com.cloud.network.ovs.dao.VpcDistributedRouterSeqNoVO;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.Nic;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
@Local(value = {OvsTunnelManager.class})
public class OvsTunnelManagerImpl extends ManagerBase implements OvsTunnelManager, StateListener<VirtualMachine.State, VirtualMachine.Event, VirtualMachine> {
    public static final Logger s_logger = Logger.getLogger(OvsTunnelManagerImpl.class.getName());

    // boolean _isEnabled;
    ScheduledExecutorService _executorPool;
    ScheduledExecutorService _cleanupExecutor;

    @Inject
    ConfigurationDao _configDao;
    @Inject
    NicDao _nicDao;
    @Inject
    HostDao _hostDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _physNetTTDao;

    @Inject
    DomainRouterDao _routerDao;
    @Inject
    OvsTunnelNetworkDao _tunnelNetworkDao;
    @Inject
    OvsTunnelInterfaceDao _tunnelInterfaceDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    OvsNetworkTopologyGuru _ovsNetworkToplogyGuru;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    MessageBus _messageBus;
    @Inject
    NetworkACLDao _networkACLDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    VpcDistributedRouterSeqNoDao _vpcDrSeqNoDao;

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        _executorPool = Executors.newScheduledThreadPool(10, new NamedThreadFactory("OVS"));
        _cleanupExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("OVS-Cleanup"));

        // register for network ACL updated for a VPC.
        _messageBus.subscribe("Network_ACL_Replaced", new NetworkAclEventsSubscriber());

        // register for VM state transition updates
        VirtualMachine.State.getStateMachine().registerListener(this);

        return true;
    }

    @DB
    protected OvsTunnelInterfaceVO createInterfaceRecord(String ip,
            String netmask, String mac, long hostId, String label) {
        OvsTunnelInterfaceVO ti = null;
        try {
            ti = new OvsTunnelInterfaceVO(ip, netmask, mac, hostId, label);
            // TODO: Is locking really necessary here?
            OvsTunnelInterfaceVO lock = _tunnelInterfaceDao
                    .acquireInLockTable(Long.valueOf(1));
            if (lock == null) {
                s_logger.warn("Cannot lock table ovs_tunnel_account");
                return null;
            }
            _tunnelInterfaceDao.persist(ti);
            _tunnelInterfaceDao.releaseFromLockTable(lock.getId());
        } catch (EntityExistsException e) {
            s_logger.debug("A record for the interface for network " + label
                    + " on host id " + hostId + " already exists");
        }
        return ti;
    }

    private String handleFetchInterfaceAnswer(Answer[] answers, Long hostId) {
        OvsFetchInterfaceAnswer ans = (OvsFetchInterfaceAnswer)answers[0];
        if (ans.getResult()) {
            if (ans.getIp() != null && !("".equals(ans.getIp()))) {
                OvsTunnelInterfaceVO ti = createInterfaceRecord(ans.getIp(),
                        ans.getNetmask(), ans.getMac(), hostId, ans.getLabel());
                return ti.getIp();
            }
        }
        // Fetch interface failed!
        s_logger.warn("Unable to fetch the IP address for the GRE tunnel endpoint"
                + ans.getDetails());
        return null;
    }

    @DB
    protected OvsTunnelNetworkVO createTunnelRecord(long from, long to, long networkId, int key) {
        OvsTunnelNetworkVO ta = null;
        try {
            ta = new OvsTunnelNetworkVO(from, to, key, networkId);
            OvsTunnelNetworkVO lock = _tunnelNetworkDao.acquireInLockTable(Long.valueOf(1));
            if (lock == null) {
                s_logger.warn("Cannot lock table ovs_tunnel_account");
                return null;
            }
            _tunnelNetworkDao.persist(ta);
            _tunnelNetworkDao.releaseFromLockTable(lock.getId());
        } catch (EntityExistsException e) {
            s_logger.debug("A record for the tunnel from " + from + " to " + to + " already exists");
        }
        return ta;
    }

    private void handleCreateTunnelAnswer(Answer[] answers) {
        OvsCreateTunnelAnswer r = (OvsCreateTunnelAnswer)answers[0];
        String s =
                String.format("(hostIP:%1$s, remoteIP:%2$s, bridge:%3$s," + "greKey:%4$s, portName:%5$s)",
                        r.getFromIp(), r.getToIp(), r.getBridge(), r.getKey(), r.getInPortName());
        Long from = r.getFrom();
        Long to = r.getTo();
        long networkId = r.getNetworkId();
        OvsTunnelNetworkVO tunnel = _tunnelNetworkDao.getByFromToNetwork(from, to, networkId);
        if (tunnel == null) {
            throw new CloudRuntimeException(
                    String.format("Unable find tunnelNetwork record" +
                            "(from=%1$s,to=%2$s, account=%3$s",
                            from, to, networkId));
        }
        if (!r.getResult()) {
            tunnel.setState(OvsTunnel.State.Failed.name());
            s_logger.warn("Create GRE tunnel from " + from + " to " + to + " failed due to " + r.getDetails()
                    + s);
        } else {
            tunnel.setState(OvsTunnel.State.Established.name());
            tunnel.setPortName(r.getInPortName());
            s_logger.info("Create GRE tunnel from " + from + " to " + to + " succeeded." + r.getDetails() + s);
        }
        _tunnelNetworkDao.update(tunnel.getId(), tunnel);
    }

    private String getGreEndpointIP(Host host, Network nw)
            throws AgentUnavailableException, OperationTimedoutException {
        String endpointIp = null;
        // Fetch fefault name for network label from configuration
        String physNetLabel = _configDao.getValue(Config.OvsTunnelNetworkDefaultLabel.key());
        Long physNetId = nw.getPhysicalNetworkId();
        PhysicalNetworkTrafficType physNetTT =
                _physNetTTDao.findBy(physNetId, TrafficType.Guest);
        HypervisorType hvType = host.getHypervisorType();

        String label = null;
        switch (hvType) {
        case XenServer:
            label = physNetTT.getXenNetworkLabel();
            if ((label != null) && (!label.equals(""))) {
                physNetLabel = label;
            }
            break;
        case KVM:
            label = physNetTT.getKvmNetworkLabel();
            if ((label != null) && (!label.equals(""))) {
                physNetLabel = label;
            }
            break;
        default:
            throw new CloudRuntimeException("Hypervisor " +
                    hvType.toString() +
                    " unsupported by OVS Tunnel Manager");
        }

        // Try to fetch GRE endpoint IP address for cloud db
        // If not found, then find it on the hypervisor
        OvsTunnelInterfaceVO tunnelIface =
                _tunnelInterfaceDao.getByHostAndLabel(host.getId(),
                        physNetLabel);
        if (tunnelIface == null) {
            //Now find and fetch configuration for physical interface
            //for network with label on target host
            Commands fetchIfaceCmds =
                    new Commands(new OvsFetchInterfaceCommand(physNetLabel));
            s_logger.debug("Ask host " + host.getId() +
                    " to retrieve interface for phy net with label:" +
                    physNetLabel);
            Answer[] fetchIfaceAnswers = _agentMgr.send(host.getId(), fetchIfaceCmds);
            //And finally save it for future use
            endpointIp = handleFetchInterfaceAnswer(fetchIfaceAnswers, host.getId());
        } else {
            endpointIp = tunnelIface.getIp();
        }
        return endpointIp;
    }

    private int getGreKey(Network network) {
        int key = 0;
        try {
            //The GRE key is actually in the host part of the URI
            String keyStr = network.getBroadcastUri().getAuthority();
            if (keyStr.contains(".")) {
                String[] parts = keyStr.split("\\.");
                key = Integer.parseInt(parts[1]);
            } else {
                key = Integer.parseInt(keyStr);
            }

            return key;
        } catch (NumberFormatException e) {
            s_logger.debug("Well well, how did '" + key
                    + "' end up in the broadcast URI for the network?");
            throw new CloudRuntimeException(String.format(
                    "Invalid GRE key parsed from"
                            + "network broadcast URI (%s)", network
                            .getBroadcastUri().toString()));
        }
    }

    @DB
    protected void checkAndCreateTunnel(Network nw, Host host) {

        s_logger.debug("Creating tunnels with OVS tunnel manager");

        long hostId = host.getId();
        int key = getGreKey(nw);
        String bridgeName = generateBridgeName(nw, key);
        List<Long> toHostIds = new ArrayList<Long>();
        List<Long> fromHostIds = new ArrayList<Long>();
        List<Long> networkSpannedHosts = _ovsNetworkToplogyGuru.getNetworkSpanedHosts(nw.getId());
        for (Long rh : networkSpannedHosts) {
            if (rh == hostId) {
                continue;
            }
            OvsTunnelNetworkVO ta = _tunnelNetworkDao.getByFromToNetwork(hostId, rh.longValue(), nw.getId());
            // Try and create the tunnel even if a previous attempt failed
            if (ta == null || ta.getState().equals(OvsTunnel.State.Failed.name())) {
                s_logger.debug("Attempting to create tunnel from:" + hostId + " to:" + rh.longValue());
                if (ta == null) {
                    createTunnelRecord(hostId, rh.longValue(), nw.getId(), key);
                }
                if (!toHostIds.contains(rh)) {
                    toHostIds.add(rh);
                }
            }

            ta = _tunnelNetworkDao.getByFromToNetwork(rh.longValue(),
                    hostId, nw.getId());
            // Try and create the tunnel even if a previous attempt failed
            if (ta == null || ta.getState().equals(OvsTunnel.State.Failed.name())) {
                s_logger.debug("Attempting to create tunnel from:" +
                        rh.longValue() + " to:" + hostId);
                if (ta == null) {
                    createTunnelRecord(rh.longValue(), hostId,
                            nw.getId(), key);
                }
                if (!fromHostIds.contains(rh)) {
                    fromHostIds.add(rh);
                }
            }
        }
        //TODO: Should we propagate the exception here?
        try {
            String myIp = getGreEndpointIP(host, nw);
            if (myIp == null)
                throw new GreTunnelException("Unable to retrieve the source " + "endpoint for the GRE tunnel." + "Failure is on host:" + host.getId());
            boolean noHost = true;
            for (Long i : toHostIds) {
                HostVO rHost = _hostDao.findById(i);
                String otherIp = getGreEndpointIP(rHost, nw);
                if (otherIp == null)
                    throw new GreTunnelException(
                            "Unable to retrieve the remote "
                                    + "endpoint for the GRE tunnel."
                                    + "Failure is on host:" + rHost.getId());
                Commands cmds = new Commands(
                        new OvsCreateTunnelCommand(otherIp, key,
                                Long.valueOf(hostId), i, nw.getId(), myIp, bridgeName, nw.getUuid()));
                s_logger.debug("Attempting to create tunnel from:" + hostId + " to:" + i + " for the network " + nw.getId());
                s_logger.debug("Ask host " + hostId
                        + " to create gre tunnel to " + i);
                Answer[] answers = _agentMgr.send(hostId, cmds);
                handleCreateTunnelAnswer(answers);
                noHost = false;
            }

            for (Long i : fromHostIds) {
                HostVO rHost = _hostDao.findById(i);
                String otherIp = getGreEndpointIP(rHost, nw);
                Commands cmds = new Commands(new OvsCreateTunnelCommand(myIp,
                        key, i, Long.valueOf(hostId), nw.getId(), otherIp, bridgeName, nw.getUuid()));
                s_logger.debug("Ask host " + i + " to create gre tunnel to "
                        + hostId);
                Answer[] answers = _agentMgr.send(i, cmds);
                handleCreateTunnelAnswer(answers);
                noHost = false;
            }

            // If no tunnels have been configured, perform the bridge setup
            // anyway. This will ensure VIF rules will be triggered
            if (noHost) {
                Commands cmds = new Commands(new OvsSetupBridgeCommand(bridgeName, hostId, nw.getId()));
                s_logger.debug("Ask host " + hostId + " to configure bridge for network:" + nw.getId());
                Answer[] answers = _agentMgr.send(hostId, cmds);
                handleSetupBridgeAnswer(answers);
            }
        } catch (GreTunnelException | OperationTimedoutException | AgentUnavailableException e) {
            // I really thing we should do a better handling of these exceptions
            s_logger.warn("Ovs Tunnel network created tunnel failed", e);
        }
    }

    @Override
    public boolean isOvsTunnelEnabled() {
        return true;
    }

    boolean isVpcEnabledForDistributedRouter(long vpcId) {
        VpcVO vpc = _vpcDao.findById(vpcId);
        return vpc.usesDistributedRouter();
    }

    @Override
    public void checkAndPrepareHostForTunnelNetwork(Network nw, Host host) {
        if (nw.getVpcId() != null && isVpcEnabledForDistributedRouter(nw.getVpcId())) {
            // check and setup host to be in full tunnel mesh with each of the network in the VPC
            checkAndCreateVpcTunnelNetworks(host, nw.getVpcId());
        } else {
            // check and setup host to be in full tunnel mesh with the network
            checkAndCreateTunnel(nw, host);
        }
    }

    @DB
    private void handleDestroyTunnelAnswer(Answer ans, long from, long to, long networkId) {
        if (ans.getResult()) {
            OvsTunnelNetworkVO lock = _tunnelNetworkDao.acquireInLockTable(Long.valueOf(1));
            if (lock == null) {
                s_logger.warn(String.format("failed to lock" +
                        "ovs_tunnel_account, remove record of " +
                        "tunnel(from=%1$s, to=%2$s account=%3$s) failed",
                        from, to, networkId));
                return;
            }

            _tunnelNetworkDao.removeByFromToNetwork(from, to, networkId);
            _tunnelNetworkDao.releaseFromLockTable(lock.getId());

            s_logger.debug(String.format("Destroy tunnel(account:%1$s," +
                    "from:%2$s, to:%3$s) successful",
                    networkId, from, to));
        } else {
            s_logger.debug(String.format("Destroy tunnel(account:%1$s," + "from:%2$s, to:%3$s) failed", networkId, from, to));
        }
    }

    @DB
    private void handleDestroyBridgeAnswer(Answer ans, long hostId, long networkId) {

        if (ans.getResult()) {
            OvsTunnelNetworkVO lock = _tunnelNetworkDao.acquireInLockTable(Long.valueOf(1));
            if (lock == null) {
                s_logger.warn("failed to lock ovs_tunnel_network," + "remove record");
                return;
            }

            _tunnelNetworkDao.removeByFromNetwork(hostId, networkId);
            _tunnelNetworkDao.releaseFromLockTable(lock.getId());

            s_logger.debug(String.format("Destroy bridge for" +
                    "network %1$s successful", networkId));
        } else {
            s_logger.debug(String.format("Destroy bridge for" +
                    "network %1$s failed", networkId));
        }
    }

    private void handleSetupBridgeAnswer(Answer[] answers) {
        //TODO: Add some error management here?
        s_logger.debug("Placeholder for something more meanginful to come");
    }

    @Override
    public void checkAndRemoveHostFromTunnelNetwork(Network nw, Host host) {

        if (nw.getVpcId() != null && isVpcEnabledForDistributedRouter(nw.getVpcId())) {
            List<Long> vmIds = _ovsNetworkToplogyGuru.getActiveVmsInVpcOnHost(nw.getVpcId(), host.getId());

            if (vmIds != null && !vmIds.isEmpty()) {
                return;
            }

            // there are not active VM's on this host belonging to any of the tiers in the VPC, so remove
            // the host from the tunnel mesh network and destroy the bridge
            List<? extends Network> vpcNetworks =  _vpcMgr.getVpcNetworks(nw.getVpcId());
            try {
                for (Network network: vpcNetworks) {
                    int key = getGreKey(nw);
                    String bridgeName = generateBridgeName(nw, key);
                    /* Then ask hosts have peer tunnel with me to destroy them */
                    List<OvsTunnelNetworkVO> peers = _tunnelNetworkDao.listByToNetwork(host.getId(),nw.getId());
                    for (OvsTunnelNetworkVO p : peers) {
                        // If the tunnel was not successfully created don't bother to remove it
                        if (p.getState().equals(OvsTunnel.State.Established.name())) {
                            Command cmd= new OvsDestroyTunnelCommand(p.getNetworkId(), bridgeName,
                                    p.getPortName());
                            s_logger.debug("Destroying tunnel to " + host.getId() +
                                    " from " + p.getFrom());
                            Answer ans = _agentMgr.send(p.getFrom(), cmd);
                            handleDestroyTunnelAnswer(ans, p.getFrom(), p.getTo(), p.getNetworkId());
                        }
                    }
                }

                Command cmd = new OvsDestroyBridgeCommand(nw.getId(), generateBridgeNameForVpc(nw.getVpcId()),
                        host.getId());
                s_logger.debug("Destroying bridge for network " + nw.getId() + " on host:" + host.getId());
                Answer ans = _agentMgr.send(host.getId(), cmd);
                handleDestroyBridgeAnswer(ans, host.getId(), nw.getId());
            } catch (Exception e) {
                s_logger.info("[ignored]"
                        + "exception while removing host from networks: " + e.getLocalizedMessage());
            }
        } else {
            List<Long> vmIds = _ovsNetworkToplogyGuru.getActiveVmsInNetworkOnHost(nw.getId(), host.getId(), true);
            if (vmIds != null && !vmIds.isEmpty()) {
                return;
            }
            try {
                /* Now we are last one on host, destroy the bridge with all
                * the tunnels for this network  */
                int key = getGreKey(nw);
                String bridgeName = generateBridgeName(nw, key);
                Command cmd = new OvsDestroyBridgeCommand(nw.getId(), bridgeName, host.getId());
                s_logger.debug("Destroying bridge for network " + nw.getId() + " on host:" + host.getId());
                Answer ans = _agentMgr.send(host.getId(), cmd);
                handleDestroyBridgeAnswer(ans, host.getId(), nw.getId());

                /* Then ask hosts have peer tunnel with me to destroy them */
                List<OvsTunnelNetworkVO> peers =
                        _tunnelNetworkDao.listByToNetwork(host.getId(),
                                nw.getId());
                for (OvsTunnelNetworkVO p : peers) {
                    // If the tunnel was not successfully created don't bother to remove it
                    if (p.getState().equals(OvsTunnel.State.Established.name())) {
                        cmd = new OvsDestroyTunnelCommand(p.getNetworkId(), bridgeName,
                                p.getPortName());
                        s_logger.debug("Destroying tunnel to " + host.getId() +
                                " from " + p.getFrom());
                        ans = _agentMgr.send(p.getFrom(), cmd);
                        handleDestroyTunnelAnswer(ans, p.getFrom(),
                                p.getTo(), p.getNetworkId());
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Destroy tunnel failed", e);
            }
        }
    }

    private String generateBridgeName(Network nw, int key) {
        if (nw.getVpcId() != null && isVpcEnabledForDistributedRouter(nw.getVpcId())) {
            return "OVS-DR-VPC-Bridge" + nw.getVpcId();
        } else {
            return "OVSTunnel"+key;
        }
    }
    private String generateBridgeNameForVpc(long vpcId) {
        return "OVS-DR-VPC-Bridge" + vpcId;
    }

    @DB
    protected void checkAndCreateVpcTunnelNetworks(Host host, long vpcId) {

        long hostId = host.getId();
        String bridgeName=generateBridgeNameForVpc(vpcId);

        List<Long> vmIds = _ovsNetworkToplogyGuru.getActiveVmsInVpcOnHost(vpcId, hostId);

        if (vmIds == null || vmIds.isEmpty()) {

            // since this is the first VM from the VPC being launched on the host, first setup the bridge
            try {
                Commands cmds = new Commands(new OvsSetupBridgeCommand(bridgeName, hostId, null));
                s_logger.debug("Ask host " + hostId + " to create bridge for vpc " + vpcId + " and configure the "
                        + " bridge for distributed routing.");
                Answer[] answers = _agentMgr.send(hostId, cmds);
                handleSetupBridgeAnswer(answers);
            } catch (OperationTimedoutException | AgentUnavailableException e) {
                s_logger.warn("Ovs Tunnel network created tunnel failed", e);
            }

            // now that bridge is setup, populate network acl's before the VM gets created
            OvsVpcRoutingPolicyConfigCommand cmd = prepareVpcRoutingPolicyUpdate(vpcId);
            cmd.setSequenceNumber(getNextRoutingPolicyUpdateSequenceNumber(vpcId));

            if (!sendVpcRoutingPolicyChangeUpdate(cmd, hostId, bridgeName)) {
                s_logger.debug("Failed to send VPC routing policy change update to host : " + hostId +
                        ". But moving on with sending the updates to the rest of the hosts.");
            }
        }

        List<? extends Network> vpcNetworks =  _vpcMgr.getVpcNetworks(vpcId);
        List<Long> vpcSpannedHostIds = _ovsNetworkToplogyGuru.getVpcSpannedHosts(vpcId);
        for (Network vpcNetwork: vpcNetworks) {
            if (vpcNetwork.getState() != Network.State.Implemented &&
                    vpcNetwork.getState() != Network.State.Implementing && vpcNetwork.getState() != Network.State.Setup)
                continue;

            int key = getGreKey(vpcNetwork);
            List<Long> toHostIds = new ArrayList<Long>();
            List<Long> fromHostIds = new ArrayList<Long>();
            OvsTunnelNetworkVO tunnelRecord = null;

            for (Long rh : vpcSpannedHostIds) {
                if (rh == hostId) {
                    continue;
                }
                tunnelRecord = _tunnelNetworkDao.getByFromToNetwork(hostId, rh.longValue(), vpcNetwork.getId());
                // Try and create the tunnel if does not exit or previous attempt failed
                if (tunnelRecord == null || tunnelRecord.getState().equals(OvsTunnel.State.Failed.name())) {
                    s_logger.debug("Attempting to create tunnel from:" + hostId + " to:" + rh.longValue());
                    if (tunnelRecord == null) {
                        createTunnelRecord(hostId, rh.longValue(), vpcNetwork.getId(), key);
                    }
                    if (!toHostIds.contains(rh)) {
                        toHostIds.add(rh);
                    }
                }
                tunnelRecord = _tunnelNetworkDao.getByFromToNetwork(rh.longValue(), hostId, vpcNetwork.getId());
                // Try and create the tunnel if does not exit or previous attempt failed
                if (tunnelRecord == null || tunnelRecord.getState().equals(OvsTunnel.State.Failed.name())) {
                    s_logger.debug("Attempting to create tunnel from:" + rh.longValue() + " to:" + hostId);
                    if (tunnelRecord == null) {
                        createTunnelRecord(rh.longValue(), hostId, vpcNetwork.getId(), key);
                    }
                    if (!fromHostIds.contains(rh)) {
                        fromHostIds.add(rh);
                    }
                }
            }

            try {
                String myIp = getGreEndpointIP(host, vpcNetwork);
                if (myIp == null)
                    throw new GreTunnelException("Unable to retrieve the source " + "endpoint for the GRE tunnel."
                            + "Failure is on host:" + host.getId());
                boolean noHost = true;

                for (Long i : toHostIds) {
                    HostVO rHost = _hostDao.findById(i);
                    String otherIp = getGreEndpointIP(rHost, vpcNetwork);
                    if (otherIp == null)
                        throw new GreTunnelException(
                                "Unable to retrieve the remote endpoint for the GRE tunnel."
                                        + "Failure is on host:" + rHost.getId());
                    Commands cmds = new Commands( new OvsCreateTunnelCommand(otherIp, key, Long.valueOf(hostId),
                                     i, vpcNetwork.getId(), myIp, bridgeName, vpcNetwork.getUuid()));
                    s_logger.debug("Attempting to create tunnel from:" + hostId + " to:" + i + " for the network "
                            + vpcNetwork.getId());
                    s_logger.debug("Ask host " + hostId
                            + " to create gre tunnel to " + i);
                    Answer[] answers = _agentMgr.send(hostId, cmds);
                    handleCreateTunnelAnswer(answers);
                }

                for (Long i : fromHostIds) {
                    HostVO rHost = _hostDao.findById(i);
                    String otherIp = getGreEndpointIP(rHost, vpcNetwork);
                    Commands cmds = new Commands(new OvsCreateTunnelCommand(myIp,
                            key, i, Long.valueOf(hostId), vpcNetwork.getId(), otherIp, bridgeName,
                            vpcNetwork.getUuid()));
                    s_logger.debug("Ask host " + i + " to create gre tunnel to "
                            + hostId);
                    Answer[] answers = _agentMgr.send(i, cmds);
                    handleCreateTunnelAnswer(answers);
                }
            } catch (GreTunnelException | OperationTimedoutException | AgentUnavailableException e) {
                // I really thing we should do a better handling of these exceptions
                s_logger.warn("Ovs Tunnel network created tunnel failed", e);
            }
        }
    }

    @Override
    public boolean preStateTransitionEvent(VirtualMachine.State oldState,
                                           VirtualMachine.Event event, VirtualMachine.State newState,
                                           VirtualMachine vo, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition, VirtualMachine vm, boolean status, Object opaque) {
      if (!status) {
        return false;
      }

      VirtualMachine.State oldState = transition.getCurrentState();
      VirtualMachine.State newState = transition.getToState();
      VirtualMachine.Event event = transition.getEvent();
      if (VirtualMachine.State.isVmStarted(oldState, event, newState)) {
        handleVmStateChange((VMInstanceVO)vm);
      } else if (VirtualMachine.State.isVmStopped(oldState, event, newState)) {
        handleVmStateChange((VMInstanceVO)vm);
      } else if (VirtualMachine.State.isVmMigrated(oldState, event, newState)) {
        handleVmStateChange((VMInstanceVO)vm);
      }

      return true;
    }

  private void handleVmStateChange(VMInstanceVO vm) {

        // get the VPC's impacted with the VM start
        List<Long> vpcIds = _ovsNetworkToplogyGuru.getVpcIdsVmIsPartOf(vm.getId());
        if (vpcIds == null || vpcIds.isEmpty()) {
            return;
        }

        for (Long vpcId: vpcIds) {
            VpcVO vpc = _vpcDao.findById(vpcId);
            // nothing to do if the VPC is not setup for distributed routing
            if (vpc == null || !vpc.usesDistributedRouter()) {
                return;
            }

            // get the list of hosts on which VPC spans (i.e hosts that need to be aware of VPC topology change update)
            List<Long> vpcSpannedHostIds = _ovsNetworkToplogyGuru.getVpcSpannedHosts(vpcId);
            String bridgeName=generateBridgeNameForVpc(vpcId);

            OvsVpcPhysicalTopologyConfigCommand topologyConfigCommand = prepareVpcTopologyUpdate(vpcId);
            topologyConfigCommand.setSequenceNumber(getNextTopologyUpdateSequenceNumber(vpcId));

            // send topology change update to VPC spanned hosts
            for (Long id: vpcSpannedHostIds) {
                if (!sendVpcTopologyChangeUpdate(topologyConfigCommand, id, bridgeName)) {
                    s_logger.debug("Failed to send VPC topology change update to host : " + id + ". Moving on " +
                            "with rest of the host update.");
                }
            }
        }
    }

    public boolean sendVpcTopologyChangeUpdate(OvsVpcPhysicalTopologyConfigCommand updateCmd, long hostId, String bridgeName) {
        try {
            s_logger.debug("Sending VPC topology change update to the host " + hostId);
            updateCmd.setHostId(hostId);
            updateCmd.setBridgeName(bridgeName);
            Answer ans = _agentMgr.send(hostId, updateCmd);
            if (ans.getResult()) {
                s_logger.debug("Successfully updated the host " + hostId + " with latest VPC topology." );
                return true;
            }  else {
                s_logger.debug("Failed to update the host " + hostId + " with latest VPC topology." );
                return false;
            }
        } catch (Exception e) {
            s_logger.debug("Failed to updated the host " + hostId + " with latest VPC topology.", e );
            return false;
        }
    }

    OvsVpcPhysicalTopologyConfigCommand prepareVpcTopologyUpdate(long vpcId) {
        VpcVO vpc = _vpcDao.findById(vpcId);
        assert (vpc != null): "invalid vpc id";

        List<? extends Network> vpcNetworks =  _vpcMgr.getVpcNetworks(vpcId);
        List<Long> hostIds = _ovsNetworkToplogyGuru.getVpcSpannedHosts(vpcId);
        List<Long> vmIds = _ovsNetworkToplogyGuru.getAllActiveVmsInVpc(vpcId);

        List<OvsVpcPhysicalTopologyConfigCommand.Host> hosts = new ArrayList<>();
        List<OvsVpcPhysicalTopologyConfigCommand.Tier> tiers = new ArrayList<>();
        List<OvsVpcPhysicalTopologyConfigCommand.Vm> vms = new ArrayList<>();

        for (Long hostId : hostIds) {
            HostVO hostDetails = _hostDao.findById(hostId);
            String remoteIp = null;
            for (Network network: vpcNetworks) {
                try {
                    remoteIp = getGreEndpointIP(hostDetails, network);
                } catch (Exception e) {
                    s_logger.info("[ignored]"
                            + "error getting GRE endpoint: " + e.getLocalizedMessage());
                }
            }
            OvsVpcPhysicalTopologyConfigCommand.Host host = new OvsVpcPhysicalTopologyConfigCommand.Host(hostId, remoteIp);
            hosts.add(host);
        }

        for (Network network: vpcNetworks) {
            String key = network.getBroadcastUri().getAuthority();
            long gre_key;
            if (key.contains(".")) {
                String[] parts = key.split("\\.");
                gre_key = Long.parseLong(parts[1]);
            } else {
                try {
                    gre_key = Long.parseLong(BroadcastDomainType.getValue(key));
                } catch (Exception e) {
                    return null;
                }
            }
            NicVO nic = _nicDao.findByIp4AddressAndNetworkId(network.getGateway(), network.getId());
            OvsVpcPhysicalTopologyConfigCommand.Tier tier = new OvsVpcPhysicalTopologyConfigCommand.Tier(gre_key,
                    network.getUuid(), network.getGateway(), nic.getMacAddress(), network.getCidr());
            tiers.add(tier);
        }

        for (long vmId: vmIds) {
            VirtualMachine vmInstance = _vmInstanceDao.findById(vmId);
            List<OvsVpcPhysicalTopologyConfigCommand.Nic>  vmNics = new ArrayList<OvsVpcPhysicalTopologyConfigCommand.Nic>();
            for (Nic vmNic :_nicDao.listByVmId(vmId)) {
                Network network = _networkDao.findById(vmNic.getNetworkId());
                if (network.getTrafficType() == TrafficType.Guest) {
                    OvsVpcPhysicalTopologyConfigCommand.Nic nic =  new OvsVpcPhysicalTopologyConfigCommand.Nic(
                            vmNic.getIPv4Address(), vmNic.getMacAddress(), network.getUuid());
                    vmNics.add(nic);
                }
            }
            OvsVpcPhysicalTopologyConfigCommand.Vm vm = new OvsVpcPhysicalTopologyConfigCommand.Vm(
                    vmInstance.getHostId(), vmNics.toArray(new OvsVpcPhysicalTopologyConfigCommand.Nic[vmNics.size()]));
            vms.add(vm);
        }

        return new OvsVpcPhysicalTopologyConfigCommand(
                hosts.toArray(new OvsVpcPhysicalTopologyConfigCommand.Host[hosts.size()]),
                tiers.toArray(new OvsVpcPhysicalTopologyConfigCommand.Tier[tiers.size()]),
                vms.toArray(new OvsVpcPhysicalTopologyConfigCommand.Vm[vms.size()]),
                vpc.getCidr());
    }

    // Subscriber to ACL replace events. On acl replace event, if the vpc for the tier is enabled for
    // distributed routing send the ACL update to all the hosts on which VPC spans
    public class NetworkAclEventsSubscriber implements MessageSubscriber {
        @Override
        public void onPublishMessage(String senderAddress, String subject, Object args) {
            try {
                NetworkVO network = (NetworkVO) args;
                String bridgeName=generateBridgeNameForVpc(network.getVpcId());
                if (network.getVpcId() != null && isVpcEnabledForDistributedRouter(network.getVpcId())) {
                    long vpcId = network.getVpcId();
                    OvsVpcRoutingPolicyConfigCommand cmd = prepareVpcRoutingPolicyUpdate(vpcId);
                    cmd.setSequenceNumber(getNextRoutingPolicyUpdateSequenceNumber(vpcId));

                    // get the list of hosts on which VPC spans (i.e hosts that need to be aware of VPC
                    // network ACL update)
                    List<Long> vpcSpannedHostIds = _ovsNetworkToplogyGuru.getVpcSpannedHosts(vpcId);
                    for (Long id: vpcSpannedHostIds) {
                        if (!sendVpcRoutingPolicyChangeUpdate(cmd, id, bridgeName)) {
                            s_logger.debug("Failed to send VPC routing policy change update to host : " + id +
                                    ". But moving on with sending the updates to the rest of the hosts.");
                        }
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Failed to send VPC routing policy change updates all hosts in vpc", e);
            }
        }
    }

    private OvsVpcRoutingPolicyConfigCommand prepareVpcRoutingPolicyUpdate(long vpcId) {

        List<OvsVpcRoutingPolicyConfigCommand.Acl> acls = new ArrayList<>();
        List<OvsVpcRoutingPolicyConfigCommand.Tier> tiers = new ArrayList<>();

        VpcVO vpc = _vpcDao.findById(vpcId);
        List<? extends Network> vpcNetworks =  _vpcMgr.getVpcNetworks(vpcId);
        assert (vpc != null && (vpcNetworks != null && !vpcNetworks.isEmpty())): "invalid vpc id";

        for (Network network : vpcNetworks) {
            Long networkAclId = network.getNetworkACLId();
            if (networkAclId == null)
                continue;
            NetworkACLVO networkAcl = _networkACLDao.findById(networkAclId);

            List<OvsVpcRoutingPolicyConfigCommand.AclItem> aclItems = new ArrayList<>();
            List<NetworkACLItemVO> aclItemVos = _networkACLItemDao.listByACL(networkAclId);
            for (NetworkACLItemVO aclItem : aclItemVos) {
                String[] sourceCidrs = aclItem.getSourceCidrList().toArray(new String[aclItem.getSourceCidrList().size()]);

                aclItems.add(new OvsVpcRoutingPolicyConfigCommand.AclItem(
                        aclItem.getNumber(), aclItem.getUuid(), aclItem.getAction().name(),
                        aclItem.getTrafficType().name(),
                        ((aclItem.getSourcePortStart() != null) ?aclItem.getSourcePortStart().toString() :null),
                        ((aclItem.getSourcePortEnd() != null) ?aclItem.getSourcePortEnd().toString() :null),
                        aclItem.getProtocol(),
                        sourceCidrs));
            }

            OvsVpcRoutingPolicyConfigCommand.Acl acl = new OvsVpcRoutingPolicyConfigCommand.Acl(networkAcl.getUuid(),
                    aclItems.toArray(new OvsVpcRoutingPolicyConfigCommand.AclItem[aclItems.size()]));
            acls.add(acl);

            OvsVpcRoutingPolicyConfigCommand.Tier tier = new OvsVpcRoutingPolicyConfigCommand.Tier(network.getUuid(),
                    network.getCidr(), networkAcl.getUuid());
            tiers.add(tier);
        }

        OvsVpcRoutingPolicyConfigCommand cmd = new OvsVpcRoutingPolicyConfigCommand(vpc.getUuid(), vpc.getCidr(),
                acls.toArray(new OvsVpcRoutingPolicyConfigCommand.Acl[acls.size()]),
                tiers.toArray(new OvsVpcRoutingPolicyConfigCommand.Tier[tiers.size()]));
        return cmd;
    }

    private boolean sendVpcRoutingPolicyChangeUpdate(OvsVpcRoutingPolicyConfigCommand updateCmd, long hostId, String bridgeName) {
        try {
            s_logger.debug("Sending VPC routing policies change update to the host " + hostId);
            updateCmd.setHostId(hostId);
            updateCmd.setBridgeName(bridgeName);
            Answer ans = _agentMgr.send(hostId, updateCmd);
            if (ans.getResult()) {
                s_logger.debug("Successfully updated the host " + hostId + " with latest VPC routing policies." );
                return true;
            }  else {
                s_logger.debug("Failed to update the host " + hostId + " with latest routing policies." );
                return false;
            }
        } catch (Exception e) {
            s_logger.debug("Failed to updated the host " + hostId + " with latest routing policies due to" , e );
            return false;
        }
    }

    private long getNextTopologyUpdateSequenceNumber(final long vpcId) {

        try {
            return  Transaction.execute(new TransactionCallback<Long>() {
                @Override
                public Long doInTransaction(TransactionStatus status) {
                    VpcDistributedRouterSeqNoVO seqVo = _vpcDrSeqNoDao.findByVpcId(vpcId);
                    if (seqVo == null) {
                        seqVo = new VpcDistributedRouterSeqNoVO(vpcId);
                        _vpcDrSeqNoDao.persist(seqVo);
                    }
                    seqVo = _vpcDrSeqNoDao.lockRow(seqVo.getId(), true);
                    seqVo.incrTopologyUpdateSequenceNo();
                    _vpcDrSeqNoDao.update(seqVo.getId(), seqVo);
                    return seqVo.getTopologyUpdateSequenceNo();
                }
            });
        } finally {

        }
    }

    private long getNextRoutingPolicyUpdateSequenceNumber(final long vpcId) {

        try {
            return  Transaction.execute(new TransactionCallback<Long>() {
                @Override
                public Long doInTransaction(TransactionStatus status) {
                    VpcDistributedRouterSeqNoVO seqVo = _vpcDrSeqNoDao.findByVpcId(vpcId);
                    if (seqVo == null) {
                        seqVo = new VpcDistributedRouterSeqNoVO(vpcId);
                        _vpcDrSeqNoDao.persist(seqVo);
                    }
                    seqVo = _vpcDrSeqNoDao.lockRow(seqVo.getId(), true);
                    seqVo.incrPolicyUpdateSequenceNo();
                    _vpcDrSeqNoDao.update(seqVo.getId(), seqVo);
                    return seqVo.getPolicyUpdateSequenceNo();
                }
            });
        } finally {

        }
    }
}
