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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.Command.OnError;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.AggregationControlCommand;
import com.cloud.agent.api.routing.AggregationControlCommand.Action;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.RemoteAccessVpnVO;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.VMInstanceDao;

@Component
@Local(value = { VpcVirtualNetworkApplianceManager.class, VpcVirtualNetworkApplianceService.class })
public class VpcVirtualNetworkApplianceManagerImpl extends VirtualNetworkApplianceManagerImpl implements VpcVirtualNetworkApplianceManager {
    private static final Logger s_logger = Logger.getLogger(VpcVirtualNetworkApplianceManagerImpl.class);

    @Inject
    private VpcDao _vpcDao;
    @Inject
    private NetworkACLManager _networkACLMgr;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private StaticRouteDao _staticRouteDao;
    @Inject
    private VpcManager _vpcMgr;
    @Inject
    private PrivateIpDao _privateIpDao;
    @Inject
    private Site2SiteVpnManager _s2sVpnMgr;
    @Inject
    private VpcGatewayDao _vpcGatewayDao;
    @Inject
    private NetworkACLItemDao _networkACLItemDao;
    @Inject
    private EntityManager _entityMgr;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _itMgr.registerGuru(VirtualMachine.Type.DomainRouter, this);
        return super.configure(name, params);
    }

    @Override
    public boolean addVpcRouterToGuestNetwork(final VirtualRouter router, final Network network, final boolean isRedundant, final Map<VirtualMachineProfile.Param, Object> params)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        if (network.getTrafficType() != TrafficType.Guest) {
            s_logger.warn("Network " + network + " is not of type " + TrafficType.Guest);
            return false;
        }

        // Add router to the Guest network
        boolean result = true;
        try {

            // 1) add nic to the router
            _routerDao.addRouterToGuestNetwork(router, network);

            NicProfile guestNic = _itMgr.addVmToNetwork(router, network, null);
            // 2) setup guest network
            if (guestNic != null) {
                result = setupVpcGuestNetwork(network, router, true, guestNic);
            } else {
                s_logger.warn("Failed to add router " + router + " to guest network " + network);
                result = false;
            }
            // 3) apply networking rules
            if (result && params.get(Param.ReProgramGuestNetworks) != null && (Boolean) params.get(Param.ReProgramGuestNetworks) == true) {
                sendNetworkRulesToRouter(router.getId(), network.getId());
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to add router " + router + " to network " + network + " due to ", ex);
            result = false;
        } finally {
            if (!result) {
                s_logger.debug("Removing the router " + router + " from network " + network + " as a part of cleanup");
                if (removeVpcRouterFromGuestNetwork(router, network, isRedundant)) {
                    s_logger.debug("Removed the router " + router + " from network " + network + " as a part of cleanup");
                } else {
                    s_logger.warn("Failed to remove the router " + router + " from network " + network + " as a part of cleanup");
                }
            } else {
                s_logger.debug("Succesfully added router " + router + " to guest network " + network);
            }
        }

        return result;
    }

    @Override
    public boolean removeVpcRouterFromGuestNetwork(final VirtualRouter router, final Network network, final boolean isRedundant) throws ConcurrentOperationException,
            ResourceUnavailableException {
        if (network.getTrafficType() != TrafficType.Guest) {
            s_logger.warn("Network " + network + " is not of type " + TrafficType.Guest);
            return false;
        }

        boolean result = true;
        try {
            // Check if router is a part of the Guest network
            if (!_networkModel.isVmPartOfNetwork(router.getId(), network.getId())) {
                s_logger.debug("Router " + router + " is not a part of the Guest network " + network);
                return result;
            }

            result = setupVpcGuestNetwork(network, router, false, _networkModel.getNicProfile(router, network.getId(), null));
            if (!result) {
                s_logger.warn("Failed to destroy guest network config " + network + " on router " + router);
                return false;
            }

            result = result && _itMgr.removeVmFromNetwork(router, network, null);
        } finally {
            if (result) {
                _routerDao.removeRouterFromGuestNetwork(router.getId(), network.getId());
            }
        }

        return result;
    }

    protected boolean setupVpcGuestNetwork(final Network network, final VirtualRouter router, final boolean add, final NicProfile guestNic) throws ConcurrentOperationException,
            ResourceUnavailableException {

        boolean result = true;
        if (router.getState() == State.Running) {
            SetupGuestNetworkCommand setupCmd = _commandSetupHelper.createSetupGuestNetworkCommand(router, add, guestNic);

            Commands cmds = new Commands(Command.OnError.Stop);
            cmds.addCommand("setupguestnetwork", setupCmd);
            _nwHelper.sendCommandsToRouter(router, cmds);

            Answer setupAnswer = cmds.getAnswer("setupguestnetwork");
            String setup = add ? "set" : "destroy";
            if (!(setupAnswer != null && setupAnswer.getResult())) {
                s_logger.warn("Unable to " + setup + " guest network on router " + router);
                result = false;
            }
            return result;
        } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
            s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + ", so not sending setup guest network command to the backend");
            return true;
        } else {
            s_logger.warn("Unable to setup guest network on virtual router " + router + " is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to setup guest network on the backend," + " virtual router " + router + " is not in the right state", DataCenter.class,
                    router.getDataCenterId());
        }
    }

    @Override
    public boolean finalizeVirtualMachineProfile(final VirtualMachineProfile profile, final DeployDestination dest, final ReservationContext context) {
        DomainRouterVO vr = _routerDao.findById(profile.getId());

        if (vr.getVpcId() != null) {
            String defaultDns1 = null;
            String defaultDns2 = null;
            // remove public and guest nics as we will plug them later
            Iterator<NicProfile> it = profile.getNics().iterator();
            while (it.hasNext()) {
                NicProfile nic = it.next();
                if (nic.getTrafficType() == TrafficType.Public || nic.getTrafficType() == TrafficType.Guest) {
                    // save dns information
                    if (nic.getTrafficType() == TrafficType.Public) {
                        defaultDns1 = nic.getDns1();
                        defaultDns2 = nic.getDns2();
                    }
                    s_logger.debug("Removing nic " + nic + " of type " + nic.getTrafficType() + " from the nics passed on vm start. " + "The nic will be plugged later");
                    it.remove();
                }
            }

            // add vpc cidr/dns/networkdomain to the boot load args
            StringBuilder buf = profile.getBootArgsBuilder();
            Vpc vpc = _entityMgr.findById(Vpc.class, vr.getVpcId());
            buf.append(" vpccidr=" + vpc.getCidr() + " domain=" + vpc.getNetworkDomain());

            buf.append(" dns1=").append(defaultDns1);
            if (defaultDns2 != null) {
                buf.append(" dns2=").append(defaultDns2);
            }
        }

        return super.finalizeVirtualMachineProfile(profile, dest, context);
    }

    @Override
    public boolean finalizeCommandsOnStart(final Commands cmds, final VirtualMachineProfile profile) {
        DomainRouterVO router = _routerDao.findById(profile.getId());

        boolean isVpc = router.getVpcId() != null;
        if (!isVpc) {
            return super.finalizeCommandsOnStart(cmds, profile);
        }

        // 1) FORM SSH CHECK COMMAND
        NicProfile controlNic = getControlNic(profile);
        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the router " + router);
            return false;
        }

        finalizeSshAndVersionAndNetworkUsageOnStart(cmds, profile, router, controlNic);

        // 2) FORM PLUG NIC COMMANDS
        List<Pair<Nic, Network>> guestNics = new ArrayList<Pair<Nic, Network>>();
        List<Pair<Nic, Network>> publicNics = new ArrayList<Pair<Nic, Network>>();
        Map<String, String> vlanMacAddress = new HashMap<String, String>();

        List<? extends Nic> routerNics = _nicDao.listByVmId(profile.getId());
        for (Nic routerNic : routerNics) {
            Network network = _networkModel.getNetwork(routerNic.getNetworkId());
            if (network.getTrafficType() == TrafficType.Guest) {
                Pair<Nic, Network> guestNic = new Pair<Nic, Network>(routerNic, network);
                guestNics.add(guestNic);
            } else if (network.getTrafficType() == TrafficType.Public) {
                Pair<Nic, Network> publicNic = new Pair<Nic, Network>(routerNic, network);
                publicNics.add(publicNic);
                String vlanTag = BroadcastDomainType.getValue(routerNic.getBroadcastUri());
                vlanMacAddress.put(vlanTag, routerNic.getMacAddress());
            }
        }

        List<Command> usageCmds = new ArrayList<Command>();

        // 3) PREPARE PLUG NIC COMMANDS
        try {
            // add VPC router to public networks
            List<PublicIp> sourceNat = new ArrayList<PublicIp>(1);
            for (Pair<Nic, Network> nicNtwk : publicNics) {
                Nic publicNic = nicNtwk.first();
                Network publicNtwk = nicNtwk.second();
                IPAddressVO userIp = _ipAddressDao.findByIpAndSourceNetworkId(publicNtwk.getId(), publicNic.getIp4Address());

                if (userIp.isSourceNat()) {
                    PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                    sourceNat.add(publicIp);

                    if (router.getPublicIpAddress() == null) {
                        DomainRouterVO routerVO = _routerDao.findById(router.getId());
                        routerVO.setPublicIpAddress(publicNic.getIp4Address());
                        routerVO.setPublicNetmask(publicNic.getNetmask());
                        routerVO.setPublicMacAddress(publicNic.getMacAddress());
                        _routerDao.update(routerVO.getId(), routerVO);
                    }
                }
                PlugNicCommand plugNicCmd = new PlugNicCommand(_nwHelper.getNicTO(router, publicNic.getNetworkId(), publicNic.getBroadcastUri().toString()),
                        router.getInstanceName(), router.getType());
                cmds.addCommand(plugNicCmd);
                VpcVO vpc = _vpcDao.findById(router.getVpcId());
                NetworkUsageCommand netUsageCmd = new NetworkUsageCommand(router.getPrivateIpAddress(), router.getInstanceName(), true, publicNic.getIp4Address(), vpc.getCidr());
                usageCmds.add(netUsageCmd);
                UserStatisticsVO stats = _userStatsDao.findBy(router.getAccountId(), router.getDataCenterId(), publicNtwk.getId(), publicNic.getIp4Address(), router.getId(),
                        router.getType().toString());
                if (stats == null) {
                    stats = new UserStatisticsVO(router.getAccountId(), router.getDataCenterId(), publicNic.getIp4Address(), router.getId(), router.getType().toString(),
                            publicNtwk.getId());
                    _userStatsDao.persist(stats);
                }
            }

            // create ip assoc for source nat
            if (!sourceNat.isEmpty()) {
                _commandSetupHelper.createVpcAssociatePublicIPCommands(router, sourceNat, cmds, vlanMacAddress);
            }

            // add VPC router to guest networks
            for (Pair<Nic, Network> nicNtwk : guestNics) {
                Nic guestNic = nicNtwk.first();
                // plug guest nic
                PlugNicCommand plugNicCmd = new PlugNicCommand(_nwHelper.getNicTO(router, guestNic.getNetworkId(), null), router.getInstanceName(), router.getType());
                cmds.addCommand(plugNicCmd);
                if (!_networkModel.isPrivateGateway(guestNic.getNetworkId())) {
                    // set guest network
                    VirtualMachine vm = _vmDao.findById(router.getId());
                    NicProfile nicProfile = _networkModel.getNicProfile(vm, guestNic.getNetworkId(), null);
                    SetupGuestNetworkCommand setupCmd = _commandSetupHelper.createSetupGuestNetworkCommand(router, true, nicProfile);
                    cmds.addCommand(setupCmd);
                } else {

                    // set private network
                    PrivateIpVO ipVO = _privateIpDao.findByIpAndSourceNetworkId(guestNic.getNetworkId(), guestNic.getIp4Address());
                    Network network = _networkDao.findById(guestNic.getNetworkId());
                    BroadcastDomainType.getValue(network.getBroadcastUri());
                    String netmask = NetUtils.getCidrNetmask(network.getCidr());
                    PrivateIpAddress ip = new PrivateIpAddress(ipVO, network.getBroadcastUri().toString(), network.getGateway(), netmask, guestNic.getMacAddress());

                    List<PrivateIpAddress> privateIps = new ArrayList<PrivateIpAddress>(1);
                    privateIps.add(ip);
                    _commandSetupHelper.createVpcAssociatePrivateIPCommands(router, privateIps, cmds, true);

                    Long privateGwAclId = _vpcGatewayDao.getNetworkAclIdForPrivateIp(ipVO.getVpcId(), ipVO.getNetworkId(), ipVO.getIpAddress());

                    if (privateGwAclId != null) {
                        // set network acl on private gateway
                        List<NetworkACLItemVO> networkACLs = _networkACLItemDao.listByACL(privateGwAclId);
                        s_logger.debug("Found " + networkACLs.size() + " network ACLs to apply as a part of VPC VR " + router + " start for private gateway ip = "
                                + ipVO.getIpAddress());

                        _commandSetupHelper.createNetworkACLsCommands(networkACLs, router, cmds, ipVO.getNetworkId(), true);
                    }
                }
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to add router " + router + " to network due to exception ", ex);
            return false;
        }

        // 4) RE-APPLY ALL STATIC ROUTE RULES
        List<? extends StaticRoute> routes = _staticRouteDao.listByVpcId(router.getVpcId());
        List<StaticRouteProfile> staticRouteProfiles = new ArrayList<StaticRouteProfile>(routes.size());
        Map<Long, VpcGateway> gatewayMap = new HashMap<Long, VpcGateway>();
        for (StaticRoute route : routes) {
            VpcGateway gateway = gatewayMap.get(route.getVpcGatewayId());
            if (gateway == null) {
                gateway = _entityMgr.findById(VpcGateway.class, route.getVpcGatewayId());
                gatewayMap.put(gateway.getId(), gateway);
            }
            staticRouteProfiles.add(new StaticRouteProfile(route, gateway));
        }

        s_logger.debug("Found " + staticRouteProfiles.size() + " static routes to apply as a part of vpc route " + router + " start");
        if (!staticRouteProfiles.isEmpty()) {
            _commandSetupHelper.createStaticRouteCommands(staticRouteProfiles, router, cmds);
        }

        // 5) RE-APPLY ALL REMOTE ACCESS VPNs
        RemoteAccessVpnVO vpn = _vpnDao.findByAccountAndVpc(router.getAccountId(), router.getVpcId());
        if (vpn != null) {
            _commandSetupHelper.createApplyVpnCommands(true, vpn, router, cmds);
        }

        // 6) REPROGRAM GUEST NETWORK
        boolean reprogramGuestNtwks = true;
        if (profile.getParameter(Param.ReProgramGuestNetworks) != null && (Boolean) profile.getParameter(Param.ReProgramGuestNetworks) == false) {
            reprogramGuestNtwks = false;
        }

        VirtualRouterProvider vrProvider = _vrProviderDao.findById(router.getElementId());
        if (vrProvider == null) {
            throw new CloudRuntimeException("Cannot find related virtual router provider of router: " + router.getHostName());
        }
        Provider provider = Network.Provider.getProvider(vrProvider.getType().toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find related provider of virtual router provider: " + vrProvider.getType().toString());
        }

        for (Pair<Nic, Network> nicNtwk : guestNics) {
            Nic guestNic = nicNtwk.first();
            AggregationControlCommand startCmd = new AggregationControlCommand(Action.Start, router.getInstanceName(), controlNic.getIp4Address(), getRouterIpInNetwork(
                    guestNic.getNetworkId(), router.getId()));
            cmds.addCommand(startCmd);
            if (reprogramGuestNtwks) {
                finalizeIpAssocForNetwork(cmds, router, provider, guestNic.getNetworkId(), vlanMacAddress);
                finalizeNetworkRulesForNetwork(cmds, router, provider, guestNic.getNetworkId());
            }

            finalizeUserDataAndDhcpOnStart(cmds, router, provider, guestNic.getNetworkId());
            AggregationControlCommand finishCmd = new AggregationControlCommand(Action.Finish, router.getInstanceName(), controlNic.getIp4Address(), getRouterIpInNetwork(
                    guestNic.getNetworkId(), router.getId()));
            cmds.addCommand(finishCmd);
        }

        // Add network usage commands
        cmds.addCommands(usageCmds);

        return true;
    }

    @Override
    protected void finalizeNetworkRulesForNetwork(final Commands cmds, final DomainRouterVO router, final Provider provider, final Long guestNetworkId) {

        super.finalizeNetworkRulesForNetwork(cmds, router, provider, guestNetworkId);

        if (router.getVpcId() != null) {
            if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.NetworkACL, Provider.VPCVirtualRouter)) {
                List<NetworkACLItemVO> networkACLs = _networkACLMgr.listNetworkACLItems(guestNetworkId);
                if (networkACLs != null && !networkACLs.isEmpty()) {
                    s_logger.debug("Found " + networkACLs.size() + " network ACLs to apply as a part of VPC VR " + router + " start for guest network id=" + guestNetworkId);
                    _commandSetupHelper.createNetworkACLsCommands(networkACLs, router, cmds, guestNetworkId, false);
                }
            }
        }
    }

    protected boolean sendNetworkRulesToRouter(final long routerId, final long networkId) throws ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findById(routerId);
        Commands cmds = new Commands(OnError.Continue);

        VirtualRouterProvider vrProvider = _vrProviderDao.findById(router.getElementId());
        if (vrProvider == null) {
            throw new CloudRuntimeException("Cannot find related virtual router provider of router: " + router.getHostName());
        }
        Provider provider = Network.Provider.getProvider(vrProvider.getType().toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find related provider of virtual router provider: " + vrProvider.getType().toString());
        }

        finalizeNetworkRulesForNetwork(cmds, router, provider, networkId);
        return _nwHelper.sendCommandsToRouter(router, cmds);
    }

    /**
     * @param router
     * @param add
     * @param privateNic
     * @return
     * @throws ResourceUnavailableException
     */
    protected boolean setupVpcPrivateNetwork(final VirtualRouter router, final boolean add, final NicProfile privateNic) throws ResourceUnavailableException {

        if (router.getState() == State.Running) {
            PrivateIpVO ipVO = _privateIpDao.findByIpAndSourceNetworkId(privateNic.getNetworkId(), privateNic.getIp4Address());
            Network network = _networkDao.findById(privateNic.getNetworkId());
            String netmask = NetUtils.getCidrNetmask(network.getCidr());
            PrivateIpAddress ip = new PrivateIpAddress(ipVO, network.getBroadcastUri().toString(), network.getGateway(), netmask, privateNic.getMacAddress());

            List<PrivateIpAddress> privateIps = new ArrayList<PrivateIpAddress>(1);
            privateIps.add(ip);
            Commands cmds = new Commands(Command.OnError.Stop);
            _commandSetupHelper.createVpcAssociatePrivateIPCommands(router, privateIps, cmds, add);

            try {
                if (_nwHelper.sendCommandsToRouter(router, cmds)) {
                    s_logger.debug("Successfully applied ip association for ip " + ip + " in vpc network " + network);
                    return true;
                } else {
                    s_logger.warn("Failed to associate ip address " + ip + " in vpc network " + network);
                    return false;
                }
            } catch (Exception ex) {
                s_logger.warn("Failed to send  " + (add ? "add " : "delete ") + " private network " + network + " commands to rotuer ");
                return false;
            }
        } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
            s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + ", so not sending setup private network command to the backend");
        } else {
            s_logger.warn("Unable to setup private gateway, virtual router " + router + " is not in the right state " + router.getState());

            throw new ResourceUnavailableException("Unable to setup Private gateway on the backend," + " virtual router " + router + " is not in the right state",
                    DataCenter.class, router.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean destroyPrivateGateway(final PrivateGateway gateway, final VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException {

        if (!_networkModel.isVmPartOfNetwork(router.getId(), gateway.getNetworkId())) {
            s_logger.debug("Router doesn't have nic for gateway " + gateway + " so no need to removed it");
            return true;
        }

        Network privateNetwork = _networkModel.getNetwork(gateway.getNetworkId());

        s_logger.debug("Releasing private ip for gateway " + gateway + " from " + router);
        boolean result = setupVpcPrivateNetwork(router, false, _networkModel.getNicProfile(router, privateNetwork.getId(), null));
        if (!result) {
            s_logger.warn("Failed to release private ip for gateway " + gateway + " on router " + router);
            return false;
        }

        // revoke network acl on the private gateway.
        if (!_networkACLMgr.revokeACLItemsForPrivateGw(gateway)) {
            s_logger.debug("Failed to delete network acl items on " + gateway + " from router " + router);
            return false;
        }

        s_logger.debug("Removing router " + router + " from private network " + privateNetwork + " as a part of delete private gateway");
        result = result && _itMgr.removeVmFromNetwork(router, privateNetwork, null);
        s_logger.debug("Private gateawy " + gateway + " is removed from router " + router);
        return result;
    }

    @Override
    protected void finalizeIpAssocForNetwork(final Commands cmds, final VirtualRouter router, final Provider provider, final Long guestNetworkId,
            final Map<String, String> vlanMacAddress) {

        if (router.getVpcId() == null) {
            super.finalizeIpAssocForNetwork(cmds, router, provider, guestNetworkId, vlanMacAddress);
            return;
        }

        ArrayList<? extends PublicIpAddress> publicIps = getPublicIpsToApply(router, provider, guestNetworkId, IpAddress.State.Releasing);

        if (publicIps != null && !publicIps.isEmpty()) {
            s_logger.debug("Found " + publicIps.size() + " ip(s) to apply as a part of domR " + router + " start.");
            // Re-apply public ip addresses - should come before PF/LB/VPN
            _commandSetupHelper.createVpcAssociatePublicIPCommands(router, publicIps, cmds, vlanMacAddress);
        }
    }

    @Override
    public boolean startSite2SiteVpn(final Site2SiteVpnConnection conn, final VirtualRouter router) throws ResourceUnavailableException {
        if (router.getState() != State.Running) {
            s_logger.warn("Unable to apply site-to-site VPN configuration, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to apply site 2 site VPN configuration," + " virtual router is not in the right state", DataCenter.class,
                    router.getDataCenterId());
        }

        return applySite2SiteVpn(true, router, conn);
    }

    @Override
    public boolean stopSite2SiteVpn(final Site2SiteVpnConnection conn, final VirtualRouter router) throws ResourceUnavailableException {
        if (router.getState() != State.Running) {
            s_logger.warn("Unable to apply site-to-site VPN configuration, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to apply site 2 site VPN configuration," + " virtual router is not in the right state", DataCenter.class,
                    router.getDataCenterId());
        }

        return applySite2SiteVpn(false, router, conn);
    }

    protected boolean applySite2SiteVpn(final boolean isCreate, final VirtualRouter router, final Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        Commands cmds = new Commands(Command.OnError.Continue);
        _commandSetupHelper.createSite2SiteVpnCfgCommands(conn, isCreate, router, cmds);
        return _nwHelper.sendCommandsToRouter(router, cmds);
    }

    protected Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> getNicsToChangeOnRouter(final List<? extends PublicIpAddress> publicIps, final VirtualRouter router) {
        // 1) check which nics need to be plugged/unplugged and plug/unplug them

        Map<String, PublicIpAddress> nicsToPlug = new HashMap<String, PublicIpAddress>();
        Map<String, PublicIpAddress> nicsToUnplug = new HashMap<String, PublicIpAddress>();

        // find out nics to unplug
        for (PublicIpAddress ip : publicIps) {
            long publicNtwkId = ip.getNetworkId();

            // if ip is not associated to any network, and there are no firewall
            // rules, release it on the backend
            if (!_vpcMgr.isIpAllocatedToVpc(ip)) {
                ip.setState(IpAddress.State.Releasing);
            }

            if (ip.getState() == IpAddress.State.Releasing) {
                Nic nic = _nicDao.findByIp4AddressAndNetworkIdAndInstanceId(publicNtwkId, router.getId(), ip.getAddress().addr());
                if (nic != null) {
                    nicsToUnplug.put(ip.getVlanTag(), ip);
                    s_logger.debug("Need to unplug the nic for ip=" + ip + "; vlan=" + ip.getVlanTag() + " in public network id =" + publicNtwkId);
                }
            }
        }

        // find out nics to plug
        for (PublicIpAddress ip : publicIps) {
            URI broadcastUri = BroadcastDomainType.Vlan.toUri(ip.getVlanTag());
            long publicNtwkId = ip.getNetworkId();

            // if ip is not associated to any network, and there are no firewall
            // rules, release it on the backend
            if (!_vpcMgr.isIpAllocatedToVpc(ip)) {
                ip.setState(IpAddress.State.Releasing);
            }

            if (ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) {
                // nic has to be plugged only when there are no nics for this
                // vlan tag exist on VR
                Nic nic = _nicDao.findByNetworkIdInstanceIdAndBroadcastUri(publicNtwkId, router.getId(), broadcastUri.toString());

                if (nic == null && nicsToPlug.get(ip.getVlanTag()) == null) {
                    nicsToPlug.put(ip.getVlanTag(), ip);
                    s_logger.debug("Need to plug the nic for ip=" + ip + "; vlan=" + ip.getVlanTag() + " in public network id =" + publicNtwkId);
                } else {
                    PublicIpAddress nicToUnplug = nicsToUnplug.get(ip.getVlanTag());
                    if (nicToUnplug != null) {
                        NicVO nicVO = _nicDao.findByIp4AddressAndNetworkIdAndInstanceId(publicNtwkId, router.getId(), nicToUnplug.getAddress().addr());
                        nicVO.setIp4Address(ip.getAddress().addr());
                        _nicDao.update(nicVO.getId(), nicVO);
                        s_logger.debug("Updated the nic " + nicVO + " with the new ip address " + ip.getAddress().addr());
                        nicsToUnplug.remove(ip.getVlanTag());
                    }
                }
            }
        }

        Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> nicsToChange = new Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>>(nicsToPlug,
                nicsToUnplug);
        return nicsToChange;
    }

    @Override
    public void finalizeStop(final VirtualMachineProfile profile, final Answer answer) {
        super.finalizeStop(profile, answer);
        // Mark VPN connections as Disconnected
        DomainRouterVO router = _routerDao.findById(profile.getId());
        Long vpcId = router.getVpcId();
        if (vpcId != null) {
            _s2sVpnMgr.markDisconnectVpnConnByVpc(vpcId);
        }
    }

    @Override
    public List<DomainRouterVO> getVpcRouters(final long vpcId) {
        return _routerDao.listByVpcId(vpcId);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean startRemoteAccessVpn(final RemoteAccessVpn vpn, final VirtualRouter router) throws ResourceUnavailableException {
        if (router.getState() != State.Running) {
            s_logger.warn("Unable to apply remote access VPN configuration, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to apply remote access VPN configuration," + " virtual router is not in the right state", DataCenter.class,
                    router.getDataCenterId());
        }

        Commands cmds = new Commands(Command.OnError.Stop);
        _commandSetupHelper.createApplyVpnCommands(true, vpn, router, cmds);

        try {
            _agentMgr.send(router.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.debug("Failed to start remote access VPN: ", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", router.getHostId(), e);
        }
        Answer answer = cmds.getAnswer("users");
        if (!answer.getResult()) {
            s_logger.error("Unable to start vpn: unable add users to vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: "
                    + router.getInstanceName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn: Unable to add users to vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId()
                    + " on domR: " + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }
        answer = cmds.getAnswer("startVpn");
        if (!answer.getResult()) {
            s_logger.error("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName() + " due to "
                    + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: "
                    + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }

        return true;
    }

    @Override
    public boolean stopRemoteAccessVpn(final RemoteAccessVpn vpn, final VirtualRouter router) throws ResourceUnavailableException {
        boolean result = true;

        if (router.getState() == State.Running) {
            Commands cmds = new Commands(Command.OnError.Continue);
            _commandSetupHelper.createApplyVpnCommands(false, vpn, router, cmds);
            result = result && _nwHelper.sendCommandsToRouter(router, cmds);
        } else if (router.getState() == State.Stopped) {
            s_logger.debug("Router " + router + " is in Stopped state, not sending deleteRemoteAccessVpn command to it");
        } else {
            s_logger.warn("Failed to delete remote access VPN: domR " + router + " is not in right state " + router.getState());
            throw new ResourceUnavailableException("Failed to delete remote access VPN: domR is not in right state " + router.getState(), DataCenter.class,
                    router.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(final StateMachine2.Transition<State, VirtualMachine.Event> transition, final VirtualMachine vo, final boolean status, final Object opaque) {
        // Without this VirtualNetworkApplianceManagerImpl.postStateTransitionEvent() gets called twice as part of listeners -
        // once from VpcVirtualNetworkApplianceManagerImpl and once from VirtualNetworkApplianceManagerImpl itself
        return true;
    }
}
