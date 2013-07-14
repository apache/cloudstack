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
import java.util.TreeSet;

import javax.ejb.Local;
import javax.inject.Inject;

import com.cloud.configuration.ZoneConfig;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.SetupGuestNetworkAnswer;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.to.DhcpTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.vpc.NetworkACLItem;
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
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
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
@Local(value = {VpcVirtualNetworkApplianceManager.class, VpcVirtualNetworkApplianceService.class})
public class VpcVirtualNetworkApplianceManagerImpl extends VirtualNetworkApplianceManagerImpl implements VpcVirtualNetworkApplianceManager{
    private static final Logger s_logger = Logger.getLogger(VpcVirtualNetworkApplianceManagerImpl.class);

    String _name;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    PhysicalNetworkDao _pNtwkDao;
    @Inject
    NetworkService _ntwkService;
    @Inject
    NetworkACLManager _networkACLMgr;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    StaticRouteDao _staticRouteDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    IPAddressDao _ipAddrDao;
    @Inject
    Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Inject
    Site2SiteVpnConnectionDao _vpnConnectionDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    Site2SiteVpnManager _s2sVpnMgr;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    
    @Override
    public List<DomainRouterVO> deployVirtualRouterInVpc(Vpc vpc, DeployDestination dest, Account owner, 
            Map<Param, Object> params) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        if(s_logger.isTraceEnabled()) {
            s_logger.trace("deployVirtualRouterInVpc(" + vpc.getName() +", "+dest.getHost()+", "+owner.getAccountName()+", "+params.toString()+")");
        }

        List<DomainRouterVO> routers = findOrDeployVirtualRouterInVpc(vpc, dest, owner, params);
        
        return startRouters(params, routers);
    }
    
    @DB
    protected List<DomainRouterVO> findOrDeployVirtualRouterInVpc(Vpc vpc, DeployDestination dest, Account owner,
            Map<Param, Object> params) throws ConcurrentOperationException, 
            InsufficientCapacityException, ResourceUnavailableException {

        s_logger.debug("Deploying Virtual Router in VPC "+ vpc);
        Vpc vpcLock = _vpcDao.acquireInLockTable(vpc.getId());
        if (vpcLock == null) {
            throw new ConcurrentOperationException("Unable to lock vpc " + vpc.getId());
        }
        
        //1) Get deployment plan and find out the list of routers     
        Pair<DeploymentPlan, List<DomainRouterVO>> planAndRouters = getDeploymentPlanAndRouters(vpc.getId(), dest);
        DeploymentPlan plan = planAndRouters.first();
        List<DomainRouterVO> routers = planAndRouters.second();
        try { 
            //2) Return routers if exist
            if (routers.size() >= 1) {
                return routers;
            }
            
            Long offeringId = _vpcOffDao.findById(vpc.getVpcOfferingId()).getServiceOfferingId();
            if (offeringId == null) {
                offeringId = _offering.getId();
            }
            //3) Deploy Virtual Router
            List<? extends PhysicalNetwork> pNtwks = _pNtwkDao.listByZone(vpc.getZoneId());
            
            VirtualRouterProvider vpcVrProvider = null;
           
            for (PhysicalNetwork pNtwk : pNtwks) {
                PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(pNtwk.getId(), 
                        VirtualRouterProviderType.VPCVirtualRouter.toString());
                if (provider == null) {
                    throw new CloudRuntimeException("Cannot find service provider " + 
                            VirtualRouterProviderType.VPCVirtualRouter.toString() + " in physical network " + pNtwk.getId());
                }
                vpcVrProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), 
                        VirtualRouterProviderType.VPCVirtualRouter);
                if (vpcVrProvider != null) {
                    break;
                }
            }
            
            PublicIp sourceNatIp = _vpcMgr.assignSourceNatIpAddressToVpc(owner, vpc);
            
            DomainRouterVO router = deployVpcRouter(owner, dest, plan, params, false, vpcVrProvider, offeringId,
                    vpc.getId(), sourceNatIp);
            routers.add(router);
            
        } finally {
            if (vpcLock != null) {
                _vpcDao.releaseFromLockTable(vpc.getId());
            }
        }
        return routers;
    }
    
    protected Pair<DeploymentPlan, List<DomainRouterVO>> getDeploymentPlanAndRouters(long vpcId, DeployDestination dest) {
        long dcId = dest.getDataCenter().getId();
        
        DeploymentPlan plan = new DataCenterDeployment(dcId);
        List<DomainRouterVO> routers = getVpcRouters(vpcId);
        
        return new Pair<DeploymentPlan, List<DomainRouterVO>>(plan, routers);
    }

    
    @Override
    public boolean addVpcRouterToGuestNetwork(VirtualRouter router, Network network, boolean isRedundant) 
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("addVpcRouterToGuestNetwork(" + router.getUuid() + ", " + network.getCidr() + ", " + isRedundant + ")");
        }
        if (network.getTrafficType() != TrafficType.Guest) {
            s_logger.warn("Network " + network + " is not of type " + TrafficType.Guest);
            return false;
        }
        
        //Add router to the Guest network
        boolean result = true;
        try {
            _routerDao.addRouterToGuestNetwork(router, network);

            NicProfile guestNic = _itMgr.addVmToNetwork(router, network, null);
            //setup guest network
            if (guestNic != null) {
                result = setupVpcGuestNetwork(network, router, true, guestNic);
            } else {
                s_logger.warn("Failed to add router " + router + " to guest network " + network);
                result = false;
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
    public boolean removeVpcRouterFromGuestNetwork(VirtualRouter router, Network network, boolean isRedundant) 
            throws ConcurrentOperationException, ResourceUnavailableException {
        if (network.getTrafficType() != TrafficType.Guest) {
            s_logger.warn("Network " + network + " is not of type " + TrafficType.Guest);
            return false;
        }
        
        //Check if router is a part of the Guest network
        if (!_networkModel.isVmPartOfNetwork(router.getId(), network.getId())) {
            s_logger.debug("Router " + router + " is not a part of the Guest network " + network);
            return true;
        }
        
        boolean result = setupVpcGuestNetwork(network, router, false, _networkModel.getNicProfile(router, network.getId(), null));
        if (!result) {
            s_logger.warn("Failed to destroy guest network config " + network + " on router " + router);
            return false;
        }
        
        result = result && _itMgr.removeVmFromNetwork(router, network, null);
        
        if (result) {
                _routerDao.removeRouterFromGuestNetwork(router.getId(), network.getId());
            }
        return result;
    }
    
    
    protected DomainRouterVO deployVpcRouter(Account owner, DeployDestination dest, DeploymentPlan plan, Map<Param, Object> params,
            boolean isRedundant, VirtualRouterProvider vrProvider, long svcOffId,
            Long vpcId, PublicIp sourceNatIp) throws ConcurrentOperationException, 
            InsufficientAddressCapacityException, InsufficientServerCapacityException, InsufficientCapacityException, 
            StorageUnavailableException, ResourceUnavailableException {
        if(s_logger.isTraceEnabled()) {
            s_logger.trace("deployVpcRouter(" + owner.getAccountName() + ", " + dest.getHost() + ", " + plan.toString() + ", " + params.toString()
                    + ", " + isRedundant + ", " + vrProvider.getUuid() + ", " + svcOffId + ", " + vpcId + ", " + sourceNatIp + ")");
        }
        List<Pair<NetworkVO, NicProfile>> networks = createVpcRouterNetworks(owner, isRedundant, plan, new Pair<Boolean, PublicIp>(true, sourceNatIp),
                vpcId);
        DomainRouterVO router = 
                super.deployRouter(owner, dest, plan, params, isRedundant, vrProvider, svcOffId, vpcId, networks, true, 
                        _vpcMgr.getSupportedVpcHypervisors());
        
        return router;
    }
    
    protected boolean setupVpcGuestNetwork(Network network, VirtualRouter router, boolean add, NicProfile guestNic) 
            throws ConcurrentOperationException, ResourceUnavailableException{

        boolean result = true;  
        if (router.getState() == State.Running) {
            SetupGuestNetworkCommand setupCmd = createSetupGuestNetworkCommand(router, add, guestNic);   

            Commands cmds = new Commands(OnError.Stop);
            cmds.addCommand("setupguestnetwork", setupCmd);
            sendCommandsToRouter(router, cmds);
            
            SetupGuestNetworkAnswer setupAnswer = cmds.getAnswer(SetupGuestNetworkAnswer.class);
            String setup = add ? "set" : "destroy";
            if (!(setupAnswer != null && setupAnswer.getResult())) {
                s_logger.warn("Unable to " + setup + " guest network on router " + router);
                result = false;
            }
            return result;
        } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
            s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + 
                    ", so not sending setup guest network command to the backend");
            return true;
        } else {
            s_logger.warn("Unable to setup guest network on virtual router " + router + " is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to setup guest network on the backend," +
                    " virtual router " + router + " is not in the right state", DataCenter.class, router.getDataCenterId());
        }
    }

    protected SetupGuestNetworkCommand createSetupGuestNetworkCommand(VirtualRouter router, boolean add, NicProfile guestNic) {
        Network network = _networkModel.getNetwork(guestNic.getNetworkId());
        
        String defaultDns1 = null;
        String defaultDns2 = null;
        
        boolean dnsProvided = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dns, Provider.VPCVirtualRouter);
        boolean dhcpProvided = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, 
                Provider.VPCVirtualRouter);
        
        boolean setupDns = dnsProvided || dhcpProvided;
        
        if (setupDns) {
            defaultDns1 = guestNic.getDns1();
            defaultDns2 = guestNic.getDns2();
        }
        
        Nic nic = _nicDao.findByNtwkIdAndInstanceId(network.getId(), router.getId());
        String networkDomain = network.getNetworkDomain();
        String dhcpRange = getGuestDhcpRange(guestNic, network, _configMgr.getZone(network.getDataCenterId()));
        
        NicProfile nicProfile = _networkModel.getNicProfile(router, nic.getNetworkId(), null);

        SetupGuestNetworkCommand setupCmd = new SetupGuestNetworkCommand(dhcpRange, networkDomain, false, null, 
                defaultDns1, defaultDns2, add, _itMgr.toNicTO(nicProfile, router.getHypervisorType()));
        
        String brd = NetUtils.long2Ip(NetUtils.ip2Long(guestNic.getIp4Address()) | ~NetUtils.ip2Long(guestNic.getNetmask()));
        setupCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        setupCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, getRouterIpInNetwork(network.getId(), router.getId()));
        
        setupCmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, network.getGateway());
        setupCmd.setAccessDetail(NetworkElementCommand.GUEST_BRIDGE, brd);
        setupCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        
        if (network.getBroadcastDomainType() == BroadcastDomainType.Vlan) {
                long guestVlanTag = Long.parseLong(network.getBroadcastUri().getHost());
                setupCmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, String.valueOf(guestVlanTag));
        }
        
        return setupCmd;
    }
    
    private void createVpcAssociatePublicIPCommands(final VirtualRouter router, final List<? extends PublicIpAddress> ips,
            Commands cmds, Map<String, String> vlanMacAddress) {
        
        Pair<IpAddressTO, Long> sourceNatIpAdd = null;
        Boolean addSourceNat = null;
        // Ensure that in multiple vlans case we first send all ip addresses of vlan1, then all ip addresses of vlan2, etc..
        Map<String, ArrayList<PublicIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PublicIpAddress>>();
        for (final PublicIpAddress ipAddress : ips) {
            String vlanTag = ipAddress.getVlanTag();
            ArrayList<PublicIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PublicIpAddress>();
            }
            //VR doesn't support release for sourceNat IP address; so reset the state
            if (ipAddress.isSourceNat() && ipAddress.getState() == IpAddress.State.Releasing) {
                ipAddress.setState(IpAddress.State.Allocated);
            }
            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }

        for (Map.Entry<String, ArrayList<PublicIpAddress>> vlanAndIp : vlanIpMap.entrySet()) {
            List<PublicIpAddress> ipAddrList = vlanAndIp.getValue();

            // Get network rate - required for IpAssoc
            Integer networkRate = _networkModel.getNetworkRate(ipAddrList.get(0).getNetworkId(), router.getId());
            Network network = _networkModel.getNetwork(ipAddrList.get(0).getNetworkId());

            IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
            int i = 0;

            for (final PublicIpAddress ipAddr : ipAddrList) {
                boolean add = (ipAddr.getState() == IpAddress.State.Releasing ? false : true);
                
                String macAddress = vlanMacAddress.get(ipAddr.getVlanTag());
                
                IpAddressTO ip = new IpAddressTO(ipAddr.getAccountId(), ipAddr.getAddress().addr(), add, false, 
                        ipAddr.isSourceNat(), ipAddr.getVlanTag(), ipAddr.getGateway(), ipAddr.getNetmask(), macAddress,
                        networkRate, ipAddr.isOneToOneNat());

                ip.setTrafficType(network.getTrafficType());
                ip.setNetworkName(_networkModel.getNetworkTag(router.getHypervisorType(), network));
                ipsToSend[i++] = ip;
                if (ipAddr.isSourceNat()) {
                    sourceNatIpAdd = new Pair<IpAddressTO, Long>(ip, ipAddr.getNetworkId());
                    addSourceNat = add;
                }
            }
            IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, getRouterIpInNetwork(ipAddrList.get(0).getNetworkId(), router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand("IPAssocVpcCommand", cmd);
        }
        
        //set source nat ip
        if (sourceNatIpAdd != null) {
            IpAddressTO sourceNatIp = sourceNatIpAdd.first();
            SetSourceNatCommand cmd = new SetSourceNatCommand(sourceNatIp, addSourceNat);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
            cmds.addCommand("SetSourceNatCommand", cmd);
        }
    }

    protected NicTO getNicTO(final VirtualRouter router, Long networkId, String broadcastUri) {
        NicProfile nicProfile = _networkModel.getNicProfile(router, networkId, broadcastUri);
        
        return _itMgr.toNicTO(nicProfile, router.getHypervisorType());
    }
    
    @Override
    public boolean associatePublicIP(Network network, final List<? extends PublicIpAddress> ipAddress,
            List<? extends VirtualRouter> routers)
            throws ResourceUnavailableException {
        if (ipAddress == null || ipAddress.isEmpty()) {
            s_logger.debug("No ip association rules to be applied for network " + network.getId());
            return true;
        }
        
        //only one router is supported in VPC now
        VirtualRouter router = routers.get(0);
        
        if (router.getVpcId() == null) {
            return super.associatePublicIP(network, ipAddress, routers);
        }
        
        Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> nicsToChange = getNicsToChangeOnRouter(ipAddress, router);
        Map<String, PublicIpAddress> nicsToPlug = nicsToChange.first();
        Map<String, PublicIpAddress> nicsToUnplug = nicsToChange.second();
        
        //1) Unplug the nics
        for (String vlanTag : nicsToUnplug.keySet()) {
            Network publicNtwk = null;
            try {
                publicNtwk = _networkModel.getNetwork(nicsToUnplug.get(vlanTag).getNetworkId());
                URI broadcastUri = BroadcastDomainType.Vlan.toUri(vlanTag);
                _itMgr.removeVmFromNetwork(router, publicNtwk, broadcastUri);
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to remove router " + router + " from vlan " + vlanTag + 
                        " in public network " + publicNtwk + " due to ", e);
                return false;
            }
        }

        Commands netUsagecmds = new Commands(OnError.Continue);
        VpcVO vpc = _vpcDao.findById(router.getVpcId());
         
        //2) Plug the nics
        for (String vlanTag : nicsToPlug.keySet()) {
            PublicIpAddress ip = nicsToPlug.get(vlanTag);
            //have to plug the nic(s)
            NicProfile defaultNic = new NicProfile();
            if (ip.isSourceNat()) {
                defaultNic.setDefaultNic(true);
            }
            defaultNic.setIp4Address(ip.getAddress().addr());
            defaultNic.setGateway(ip.getGateway());
            defaultNic.setNetmask(ip.getNetmask());
            defaultNic.setMacAddress(ip.getMacAddress());
            defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
            defaultNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(ip.getVlanTag()));
            defaultNic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            
            NicProfile publicNic = null;
            Network publicNtwk = null;
            try {
                publicNtwk = _networkModel.getNetwork(ip.getNetworkId());
                publicNic = _itMgr.addVmToNetwork(router, publicNtwk, defaultNic);
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to add router " + router + " to vlan " + vlanTag + 
                        " in public network " + publicNtwk + " due to ", e);
            } catch (InsufficientCapacityException e) {
                s_logger.warn("Failed to add router " + router + " to vlan " + vlanTag + 
                        " in public network " + publicNtwk + " due to ", e);
            } finally {
                if (publicNic == null) {
                    s_logger.warn("Failed to add router " + router + " to vlan " + vlanTag + 
                            " in public network " + publicNtwk);
                    return false;
                }
            }
            //Create network usage commands. Send commands to router after IPAssoc
            NetworkUsageCommand netUsageCmd = new NetworkUsageCommand(router.getPrivateIpAddress(), router.getInstanceName(), true, defaultNic.getIp4Address(), vpc.getCidr());
            netUsagecmds.addCommand(netUsageCmd);
            UserStatisticsVO stats = _userStatsDao.findBy(router.getAccountId(), router.getDataCenterId(), 
            publicNtwk.getId(), publicNic.getIp4Address(), router.getId(), router.getType().toString());
            if (stats == null) {
                stats = new UserStatisticsVO(router.getAccountId(), router.getDataCenterId(), publicNic.getIp4Address(), router.getId(),
                        router.getType().toString(), publicNtwk.getId());
                _userStatsDao.persist(stats);
            }
        }
        
        //3) apply the ips
        boolean result = applyRules(network, routers, "vpc ip association", false, null, false, new RuleApplier() {
            @Override
            public boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException {
                Commands cmds = new Commands(OnError.Continue);
                Map<String, String> vlanMacAddress = new HashMap<String, String>();
                List<PublicIpAddress> ipsToSend = new ArrayList<PublicIpAddress>();
                for (PublicIpAddress ipAddr : ipAddress) {
                    String broadcastURI = BroadcastDomainType.Vlan.toUri(ipAddr.getVlanTag()).toString();
                    Nic nic = _nicDao.findByNetworkIdInstanceIdAndBroadcastUri(ipAddr.getNetworkId(), 
                            router.getId(), broadcastURI);
                    
                    String macAddress = null;
                    if (nic == null) {
                        if (ipAddr.getState() != IpAddress.State.Releasing) {
                            throw new CloudRuntimeException("Unable to find the nic in network " + ipAddr.getNetworkId() + 
                                    "  to apply the ip address " + ipAddr  + " for");
                        }
                        s_logger.debug("Not sending release for ip address " + ipAddr + 
                                " as its nic is already gone from VPC router " + router);
                    } else {
                        macAddress = nic.getMacAddress();
                        vlanMacAddress.put(ipAddr.getVlanTag(), macAddress);
                        ipsToSend.add(ipAddr);
                    }   
                }
                if (!ipsToSend.isEmpty()) {
                    createVpcAssociatePublicIPCommands(router, ipsToSend, cmds, vlanMacAddress);
                    return sendCommandsToRouter(router, cmds);
                }else {
                    return true;
                }
            }
        });
        if(result && netUsagecmds.size() > 0){
            //After successful ipassoc, send commands to router
            sendCommandsToRouter(router, netUsagecmds);
        }
        return result;
    }
    
    
    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, 
            ReservationContext context) {
        
        if (profile.getVirtualMachine().getVpcId() != null) {
            String defaultDns1 = null;
            String defaultDns2 = null;
            //remove public and guest nics as we will plug them later
            Iterator<NicProfile> it = profile.getNics().iterator();
            while (it.hasNext()) {
                NicProfile nic = it.next();
                if (nic.getTrafficType() == TrafficType.Public || nic.getTrafficType() == TrafficType.Guest) {
                    //save dns information
                    if(nic.getTrafficType() == TrafficType.Public) {
                        defaultDns1 = nic.getDns1();
                        defaultDns2 = nic.getDns2();
                    }
                    s_logger.debug("Removing nic " + nic + " of type " + nic.getTrafficType() + " from the nics passed on vm start. " +
                            "The nic will be plugged later");
                    it.remove();
                }
            }
            
            //add vpc cidr/dns/networkdomain to the boot load args
            StringBuilder buf = profile.getBootArgsBuilder();
            Vpc vpc = _vpcMgr.getVpc(profile.getVirtualMachine().getVpcId());
            buf.append(" vpccidr=" + vpc.getCidr() + " domain=" + vpc.getNetworkDomain());
            
            buf.append(" dns1=").append(defaultDns1);
            if (defaultDns2 != null) {
                buf.append(" dns2=").append(defaultDns2);
            }
        }

        return super.finalizeVirtualMachineProfile(profile, dest, context);
    }
    
    @Override
    public boolean applyNetworkACLs(Network network, final List<? extends NetworkACLItem> rules, List<? extends VirtualRouter> routers, final boolean isPrivateGateway)
            throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No network ACLs to be applied for network " + network.getId());
            return true;
        }
        return applyRules(network, routers, "network acls", false, null, false, new RuleApplier() {
            @Override
            public boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException {
                return sendNetworkACLs(router, rules, network.getId(), isPrivateGateway);
            }
        });
    }

    
    protected boolean sendNetworkACLs(VirtualRouter router, List<? extends NetworkACLItem> rules, long guestNetworkId, boolean isPrivateGateway)
            throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createNetworkACLsCommands(rules, router, cmds, guestNetworkId, isPrivateGateway);
        return sendCommandsToRouter(router, cmds);
    }
    
    private void createNetworkACLsCommands(List<? extends NetworkACLItem> rules, VirtualRouter router, Commands cmds,
                                           long guestNetworkId, boolean privateGateway) {
        List<NetworkACLTO> rulesTO = null;
        String guestVlan = null;
        Network guestNtwk = _networkDao.findById(guestNetworkId);
        URI uri = guestNtwk.getBroadcastUri();
        if (uri != null) {
            guestVlan = guestNtwk.getBroadcastUri().getHost();
        }
        
        if (rules != null) {
            rulesTO = new ArrayList<NetworkACLTO>();
            
            for (NetworkACLItem rule : rules) {
//                if (rule.getSourceCidrList() == null && (rule.getPurpose() == Purpose.Firewall || rule.getPurpose() == Purpose.NetworkACL)) {
//                    _firewallDao.loadSourceCidrs((FirewallRuleVO)rule);
//                }
                NetworkACLTO ruleTO = new NetworkACLTO(rule, guestVlan, rule.getTrafficType());
                rulesTO.add(ruleTO);
            }
        }
        
        SetNetworkACLCommand cmd = new SetNetworkACLCommand(rulesTO, getNicTO(router, guestNetworkId, null));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, guestVlan);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (privateGateway) {
            cmd.setAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY, String.valueOf(VpcGateway.Type.Private));
        }

        cmds.addCommand(cmd);
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile) {
        DomainRouterVO router = profile.getVirtualMachine();
        if(s_logger.isTraceEnabled()) {
            s_logger.trace("finalizeCommandsOnStart(" + cmds + ", " + profile.getHostName() + ")");
        }

        boolean isVpc = (router.getVpcId() != null);
        if (!isVpc) {
            return super.finalizeCommandsOnStart(cmds, profile);
        }

        //1) FORM SSH CHECK COMMAND
        NicProfile controlNic = getControlNic(profile);
        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the router " + router);
            return false;
        }

        finalizeSshAndVersionAndNetworkUsageOnStart(cmds, profile, router, controlNic);

        //2) FORM PLUG NIC COMMANDS
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
                String vlanTag = routerNic.getBroadcastUri().getHost();
                vlanMacAddress.put(vlanTag, routerNic.getMacAddress());
            }
        }
        
        List<Command> usageCmds = new ArrayList<Command>();
        
        //3) PREPARE PLUG NIC COMMANDS
        try {
            //add VPC router to public networks
            List<PublicIp> sourceNat = new ArrayList<PublicIp>(1);
            for (Pair<Nic, Network> nicNtwk : publicNics) {
                Nic publicNic = nicNtwk.first();
                Network publicNtwk = nicNtwk.second();
                IPAddressVO userIp = _ipAddressDao.findByIpAndSourceNetworkId(publicNtwk.getId(), 
                        publicNic.getIp4Address());
               
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
                PlugNicCommand plugNicCmd = new PlugNicCommand(getNicTO(router, publicNic.getNetworkId(), publicNic.getBroadcastUri().toString()), router.getInstanceName(), router.getType());
                cmds.addCommand(plugNicCmd); 
                VpcVO vpc = _vpcDao.findById(router.getVpcId());
                NetworkUsageCommand netUsageCmd = new NetworkUsageCommand(router.getPrivateIpAddress(), router.getInstanceName(), true, publicNic.getIp4Address(), vpc.getCidr());
                usageCmds.add(netUsageCmd);
                UserStatisticsVO stats = _userStatsDao.findBy(router.getAccountId(), router.getDataCenterId(), 
                publicNtwk.getId(), publicNic.getIp4Address(), router.getId(), router.getType().toString());
                if (stats == null) {
                    stats = new UserStatisticsVO(router.getAccountId(), router.getDataCenterId(), publicNic.getIp4Address(), router.getId(),
                            router.getType().toString(), publicNtwk.getId());
                    _userStatsDao.persist(stats);
                }
            }
            
            // create ip assoc for source nat
            if (!sourceNat.isEmpty()) {
                createVpcAssociatePublicIPCommands(router, sourceNat, cmds, vlanMacAddress);
            }
            
            //add VPC router to guest networks
            for (Pair<Nic, Network> nicNtwk : guestNics) {
                Nic guestNic = nicNtwk.first();
                //plug guest nic 
                PlugNicCommand plugNicCmd = new PlugNicCommand(getNicTO(router, guestNic.getNetworkId(), null), router.getInstanceName(), router.getType());
                cmds.addCommand(plugNicCmd);
                
                if (!_networkModel.isPrivateGateway(guestNic)) {
                    //set guest network
                    VirtualMachine vm = _vmDao.findById(router.getId());
                    NicProfile nicProfile = _networkModel.getNicProfile(vm, guestNic.getNetworkId(), null);
                    SetupGuestNetworkCommand setupCmd = createSetupGuestNetworkCommand(router, true, nicProfile);
                    cmds.addCommand(setupCmd);
                } else {

                    //set private network
                    PrivateIpVO ipVO = _privateIpDao.findByIpAndSourceNetworkId(guestNic.getNetworkId(), guestNic.getIp4Address());
                    Network network = _networkDao.findById(guestNic.getNetworkId());
                    String vlanTag = network.getBroadcastUri().getHost();
                    String netmask = NetUtils.getCidrNetmask(network.getCidr());
                    PrivateIpAddress ip = new PrivateIpAddress(ipVO, vlanTag, network.getGateway(), netmask, guestNic.getMacAddress());
                    
                    List<PrivateIpAddress> privateIps = new ArrayList<PrivateIpAddress>(1);
                    privateIps.add(ip);
                    createVpcAssociatePrivateIPCommands(router, privateIps, cmds, true);

                    Long privateGwAclId = _vpcGatewayDao.getNetworkAclIdForPrivateIp(ipVO.getVpcId(), ipVO.getNetworkId(), ipVO.getIpAddress());

                    if (privateGwAclId != null) {
                        //set network acl on private gateway
                        List<NetworkACLItemVO> networkACLs = _networkACLItemDao.listByACL(privateGwAclId);
                        s_logger.debug("Found " + networkACLs.size() + " network ACLs to apply as a part of VPC VR " + router
                                + " start for private gateway ip = " + ipVO.getIpAddress());

                        createNetworkACLsCommands(networkACLs, router, cmds, ipVO.getNetworkId(), true);
                    }
                }
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to add router " + router + " to network due to exception ", ex);
            return false;
        }
        
        //4) RE-APPLY ALL STATIC ROUTE RULES
        List<? extends StaticRoute> routes = _staticRouteDao.listByVpcId(router.getVpcId());
        List<StaticRouteProfile> staticRouteProfiles = new ArrayList<StaticRouteProfile>(routes.size());
        Map<Long, VpcGateway> gatewayMap = new HashMap<Long, VpcGateway>();
        for (StaticRoute route : routes) {
            VpcGateway gateway = gatewayMap.get(route.getVpcGatewayId());
            if (gateway == null) {
                gateway = _vpcMgr.getVpcGateway(route.getVpcGatewayId());
                gatewayMap.put(gateway.getId(), gateway);
            }
            staticRouteProfiles.add(new StaticRouteProfile(route, gateway)); 
        }
        
        s_logger.debug("Found " + staticRouteProfiles.size() + " static routes to apply as a part of vpc route " 
                + router + " start");
        if (!staticRouteProfiles.isEmpty()) {   
            createStaticRouteCommands(staticRouteProfiles, router, cmds);
        }
        
        //5) REPROGRAM GUEST NETWORK
        boolean reprogramGuestNtwks = true;
        if (profile.getParameter(Param.ReProgramGuestNetworks) != null 
                && (Boolean) profile.getParameter(Param.ReProgramGuestNetworks) == false) {
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
            if (reprogramGuestNtwks) {
                finalizeIpAssocForNetwork(cmds, router, provider, guestNic.getNetworkId(), vlanMacAddress);
                finalizeNetworkRulesForNetwork(cmds, router, provider, guestNic.getNetworkId());
            }

            finalizeUserDataAndDhcpOnStart(cmds, router, provider, guestNic.getNetworkId());
        }

        //Add network usage commands
        cmds.addCommands(usageCmds);
        configDnsMasq(router, cmds);

        return true;
    }

    protected void configDnsMasq(VirtualRouter router, Commands cmds) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("configDnsMasq(" + router.getHostName() + ", " + cmds + ")");
        }
        VpcVO vpc = _vpcDao.findById(router.getVpcId());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        List<DhcpTO> ipList = new ArrayList<DhcpTO>();

        String cidr = vpc.getCidr();
        String[] cidrPair = cidr.split("\\/");
        String cidrAddress = cidrPair[0];
        long cidrSize = Long.parseLong(cidrPair[1]);
        String startIpOfSubnet = NetUtils.getIpRangeStartIpFromCidr(cidrAddress, cidrSize);
        DhcpTO DhcpTO = new DhcpTO(router.getPrivateIpAddress(), router.getPublicIpAddress(), NetUtils.getCidrNetmask(cidrSize), startIpOfSubnet);
        ipList.add(DhcpTO);

        NicVO nic = _nicDao.findByIp4AddressAndVmId(_routerDao.findById(router.getId()).getPrivateIpAddress(), router.getId());
        DataCenterVO dcvo = _dcDao.findById(router.getDataCenterId());
        boolean dnsProvided = _networkModel.isProviderSupportServiceInNetwork(nic.getNetworkId(), Service.Dns, Provider.VirtualRouter);
        String domain_suffix = dcvo.getDetail(ZoneConfig.DnsSearchOrder.getName());
        DnsMasqConfigCommand dnsMasqConfigCmd = new DnsMasqConfigCommand(vpc.getNetworkDomain(),ipList, dcVo.getDns1(), dcVo.getDns2(), dcVo.getInternalDns1(), dcVo.getInternalDns2());
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getPublicIpAddress());
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        dnsMasqConfigCmd.setDomainSuffix(domain_suffix);
        dnsMasqConfigCmd.setIfDnsProvided(dnsProvided);

        cmds.addCommand("dnsMasqConfig" ,dnsMasqConfigCmd);
    }


    @Override
    protected void finalizeNetworkRulesForNetwork(Commands cmds, DomainRouterVO router, Provider provider, Long guestNetworkId) {
        if(s_logger.isTraceEnabled()) {
            s_logger.trace("finalizing network config for "+ router.getHostName());
        }
        super.finalizeNetworkRulesForNetwork(cmds, router, provider, guestNetworkId);
        if (router.getVpcId() != null) {
            if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.NetworkACL, Provider.VPCVirtualRouter)) {
                List<NetworkACLItemVO> networkACLs = _networkACLMgr.listNetworkACLItems(guestNetworkId);
                if ((networkACLs != null) && !networkACLs.isEmpty()) {
                    s_logger.debug("Found " + networkACLs.size() + " network ACLs to apply as a part of VPC VR " + router
                            + " start for guest network id=" + guestNetworkId);
                    createNetworkACLsCommands(networkACLs, router, cmds, guestNetworkId, false);
                }
            }
            
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("setup the vpc domain on router " + router.getHostName());
            }
        }
    }

    @Override
    public boolean setupPrivateGateway(PrivateGateway gateway, VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException {
        boolean result = true;
        try {
            Network network = _networkModel.getNetwork(gateway.getNetworkId());
            NicProfile requested = createPrivateNicProfileForGateway(gateway);
            
            NicProfile guestNic = _itMgr.addVmToNetwork(router, network, requested);
            
            //setup source nat
            if (guestNic != null) {
                result = setupVpcPrivateNetwork(router, true, guestNic);
            } else {
                s_logger.warn("Failed to setup gateway " + gateway + " on router " + router + " with the source nat");
                result = false;
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to create private gateway " + gateway + " on router " + router + " due to ", ex);
            result = false;
        } finally {
            if (!result) {
                s_logger.debug("Removing gateway " + gateway + " from router " + router + " as a part of cleanup");
                if (destroyPrivateGateway(gateway, router)) {
                    s_logger.debug("Removed the gateway " + gateway + " from router " + router + " as a part of cleanup");
                } else {
                    s_logger.warn("Failed to remove the gateway " + gateway + " from router " + router + " as a part of cleanup");
                }
            }
        }
        return result;
    }

    /**
     * @param router
     * @param add
     * @param privateNic
     * @return
     * @throws ResourceUnavailableException
     */
    protected boolean setupVpcPrivateNetwork(VirtualRouter router, boolean add, NicProfile privateNic) 
            throws ResourceUnavailableException {
        if(s_logger.isTraceEnabled()) {
            s_logger.trace("deployVpcRouter(" + router.getHostName() + ", " + add + ", " + privateNic.getMacAddress() + ")");
        }

        if (router.getState() == State.Running) {
            PrivateIpVO ipVO = _privateIpDao.findByIpAndSourceNetworkId(privateNic.getNetworkId(), privateNic.getIp4Address());
            Network network = _networkDao.findById(privateNic.getNetworkId());
            String vlanTag = network.getBroadcastUri().getHost();
            String netmask = NetUtils.getCidrNetmask(network.getCidr());
            PrivateIpAddress ip = new PrivateIpAddress(ipVO, vlanTag, network.getGateway(), netmask, privateNic.getMacAddress());
            
            List<PrivateIpAddress> privateIps = new ArrayList<PrivateIpAddress>(1);
            privateIps.add(ip);
            Commands cmds = new Commands(OnError.Stop);
            createVpcAssociatePrivateIPCommands(router, privateIps, cmds, add);

            if (sendCommandsToRouter(router, cmds)) {
                s_logger.debug("Successfully applied ip association for ip " + ip + " in vpc network " + network);
                return true;
            } else {
                s_logger.warn("Failed to associate ip address " + ip + " in vpc network " + network);
                return false;
            }
        } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
            s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + 
                    ", so not sending setup private network command to the backend");
        } else {
            s_logger.warn("Unable to setup private gateway, virtual router " + router + " is not in the right state " + router.getState());
            
            throw new ResourceUnavailableException("Unable to setup Private gateway on the backend," +
                    " virtual router " + router + " is not in the right state", DataCenter.class, router.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean destroyPrivateGateway(PrivateGateway gateway, VirtualRouter router) 
            throws ConcurrentOperationException, ResourceUnavailableException {
        
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

        //revoke network acl on the private gateway.
        if (!_networkACLMgr.revokeACLItemsForPrivateGw(gateway)) {
            s_logger.debug("Failed to delete network acl items on " + gateway +" from router " +  router);
            return false;
        }

        s_logger.debug("Removing router " + router + " from private network " + privateNetwork + " as a part of delete private gateway");
        result = result && _itMgr.removeVmFromNetwork(router, privateNetwork, null);
        s_logger.debug("Private gateawy " + gateway + " is removed from router " + router);
        return result;
    }
    
    @Override
    protected void finalizeIpAssocForNetwork(Commands cmds, VirtualRouter router, Provider provider, 
            Long guestNetworkId, Map<String, String> vlanMacAddress) {
        
        if (router.getVpcId() == null) {
            super.finalizeIpAssocForNetwork(cmds, router, provider, guestNetworkId, vlanMacAddress);
            return;
        }
        
        ArrayList<? extends PublicIpAddress> publicIps = getPublicIpsToApply(router, provider, guestNetworkId, IpAddress.State.Releasing);
        
        if (publicIps != null && !publicIps.isEmpty()) {
            s_logger.debug("Found " + publicIps.size() + " ip(s) to apply as a part of domR " + router + " start.");
            // Re-apply public ip addresses - should come before PF/LB/VPN
            createVpcAssociatePublicIPCommands(router, publicIps, cmds, vlanMacAddress);
        }
    }

    @Override
    public boolean applyStaticRoutes(List<StaticRouteProfile> staticRoutes, List<DomainRouterVO> routers) throws ResourceUnavailableException {
        if (staticRoutes == null || staticRoutes.isEmpty()) {
            s_logger.debug("No static routes to apply");
            return true;
        }
        
        boolean result = true;
        for (VirtualRouter router : routers) {
            if (router.getState() == State.Running) {
                result = result && sendStaticRoutes(staticRoutes, routers.get(0));     
            } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
                s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + 
                        ", so not sending StaticRoute command to the backend");
            } else {
                s_logger.warn("Unable to apply StaticRoute, virtual router is not in the right state " + router.getState());
                
                throw new ResourceUnavailableException("Unable to apply StaticRoute on the backend," +
                    " virtual router is not in the right state", DataCenter.class, router.getDataCenterId());
            }
        }
        return result;
    }
    
    protected boolean sendStaticRoutes(List<StaticRouteProfile> staticRoutes, DomainRouterVO router) 
            throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createStaticRouteCommands(staticRoutes, router, cmds);
        return sendCommandsToRouter(router, cmds);
    }

    /**
     * @param staticRoutes
     * @param router
     * @param cmds
     */
    private void createStaticRouteCommands(List<StaticRouteProfile> staticRoutes, DomainRouterVO router, Commands cmds) {
        SetStaticRouteCommand cmd = new SetStaticRouteCommand(staticRoutes);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    @Override
    public boolean startSite2SiteVpn(Site2SiteVpnConnection conn, VirtualRouter router) throws ResourceUnavailableException {
        if (router.getState() != State.Running) {
            s_logger.warn("Unable to apply site-to-site VPN configuration, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to apply site 2 site VPN configuration," +
                    " virtual router is not in the right state", DataCenter.class, router.getDataCenterId());
        }

        return applySite2SiteVpn(true, router, conn);
    }

    @Override
    public boolean stopSite2SiteVpn(Site2SiteVpnConnection conn, VirtualRouter router) throws ResourceUnavailableException {
        if (router.getState() != State.Running) {
            s_logger.warn("Unable to apply site-to-site VPN configuration, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to apply site 2 site VPN configuration," +
                    " virtual router is not in the right state", DataCenter.class, router.getDataCenterId());
        }

        return applySite2SiteVpn(false, router, conn);
    }

    protected boolean applySite2SiteVpn(boolean isCreate, VirtualRouter router, Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createSite2SiteVpnCfgCommands(conn, isCreate, router, cmds);
        return sendCommandsToRouter(router, cmds);
    }

    private void createSite2SiteVpnCfgCommands(Site2SiteVpnConnection conn, boolean isCreate, VirtualRouter router, Commands cmds) {
        Site2SiteCustomerGatewayVO gw = _s2sCustomerGatewayDao.findById(conn.getCustomerGatewayId());
        Site2SiteVpnGatewayVO vpnGw = _s2sVpnGatewayDao.findById(conn.getVpnGatewayId());
        IpAddress ip = _ipAddressDao.findById(vpnGw.getAddrId());
        Vpc vpc = _vpcDao.findById(ip.getVpcId());
        String localPublicIp = ip.getAddress().toString();
        String localGuestCidr = vpc.getCidr();
        String localPublicGateway = _vlanDao.findById(ip.getVlanId()).getVlanGateway();
        String peerGatewayIp = gw.getGatewayIp();
        String peerGuestCidrList = gw.getGuestCidrList();
        String ipsecPsk = gw.getIpsecPsk();
        String ikePolicy = gw.getIkePolicy();
        String espPolicy = gw.getEspPolicy();
        Long ikeLifetime = gw.getIkeLifetime();
        Long espLifetime = gw.getEspLifetime();
        Boolean dpd = gw.getDpd();

        Site2SiteVpnCfgCommand cmd = new Site2SiteVpnCfgCommand(isCreate, localPublicIp, localPublicGateway, localGuestCidr,
                peerGatewayIp, peerGuestCidrList, ikePolicy, espPolicy, ipsecPsk, ikeLifetime, espLifetime, dpd);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand("applyS2SVpn", cmd);
    }
    
    private void createVpcAssociatePrivateIPCommands(final VirtualRouter router, final List<PrivateIpAddress> ips,
            Commands cmds, boolean add) {
        
        // Ensure that in multiple vlans case we first send all ip addresses of vlan1, then all ip addresses of vlan2, etc..
        Map<String, ArrayList<PrivateIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PrivateIpAddress>>();
        for (final PrivateIpAddress ipAddress : ips) {
            String vlanTag = ipAddress.getVlanTag();
            ArrayList<PrivateIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PrivateIpAddress>();
            }
            
            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }

        for (Map.Entry<String, ArrayList<PrivateIpAddress>> vlanAndIp : vlanIpMap.entrySet()) {
            List<PrivateIpAddress> ipAddrList = vlanAndIp.getValue();
            IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
            int i = 0;

            for (final PrivateIpAddress ipAddr : ipAddrList) {
                Network network = _networkModel.getNetwork(ipAddr.getNetworkId());
                IpAddressTO ip = new IpAddressTO(Account.ACCOUNT_ID_SYSTEM, ipAddr.getIpAddress(), add, false,
                        ipAddr.getSourceNat(), ipAddr.getVlanTag(), ipAddr.getGateway(), ipAddr.getNetmask(), ipAddr.getMacAddress(),
                        null, false);

                ip.setTrafficType(network.getTrafficType());
                ip.setNetworkName(_networkModel.getNetworkTag(router.getHypervisorType(), network));
                ipsToSend[i++] = ip;
                
            }
            IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, getRouterIpInNetwork(ipAddrList.get(0).getNetworkId(), router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand("IPAssocVpcCommand", cmd);
        }
    }
    
    
    protected List<Pair<NetworkVO, NicProfile>> createVpcRouterNetworks(Account owner, boolean isRedundant, 
            DeploymentPlan plan, Pair<Boolean, PublicIp> sourceNatIp, long vpcId) throws ConcurrentOperationException,
            InsufficientAddressCapacityException {

        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(4);
        
        TreeSet<String> publicVlans = new TreeSet<String>();
        publicVlans.add(sourceNatIp.second().getVlanTag());
        
        //1) allocate nic for control and source nat public ip
        networks = super.createRouterNetworks(owner, isRedundant, plan, null, sourceNatIp);

        //2) allocate nic for private gateways if needed
        List<PrivateGateway> privateGateways = _vpcMgr.getVpcPrivateGateways(vpcId);
        if (privateGateways != null && !privateGateways.isEmpty()) {
            for (PrivateGateway privateGateway : privateGateways) {
                NicProfile privateNic = createPrivateNicProfileForGateway(privateGateway);
                Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());
                networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) privateNetwork, privateNic));
            }
        }
        
        //3) allocate nic for guest gateway if needed
        List<? extends Network> guestNetworks = _vpcMgr.getVpcNetworks(vpcId);
        for (Network guestNetwork : guestNetworks) {
            if (guestNetwork.getState() == Network.State.Implemented) {
                NicProfile guestNic = createGuestNicProfileForVpcRouter(guestNetwork);
                networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) guestNetwork, guestNic));
            }
        }
        
        //4) allocate nic for additional public network(s)
        List<IPAddressVO> ips = _ipAddressDao.listByAssociatedVpc(vpcId, false);
        for (IPAddressVO ip : ips) {
            PublicIp publicIp = PublicIp.createFromAddrAndVlan(ip, _vlanDao.findById(ip.getVlanId()));
            if ((ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) 
                    && _vpcMgr.isIpAllocatedToVpc(ip)&& !publicVlans.contains(publicIp.getVlanTag())) {
                s_logger.debug("Allocating nic for router in vlan " + publicIp.getVlanTag());
                NicProfile publicNic = new NicProfile();
                publicNic.setDefaultNic(false);
                publicNic.setIp4Address(publicIp.getAddress().addr());
                publicNic.setGateway(publicIp.getGateway());
                publicNic.setNetmask(publicIp.getNetmask());
                publicNic.setMacAddress(publicIp.getMacAddress());
                publicNic.setBroadcastType(BroadcastDomainType.Vlan);
                publicNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(publicIp.getVlanTag()));
                publicNic.setIsolationUri(IsolationType.Vlan.toUri(publicIp.getVlanTag()));
                NetworkOffering publicOffering = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
                List<NetworkVO> publicNetworks = _networkMgr.setupNetwork(_systemAcct, publicOffering, plan, null, null, false);
                networks.add(new Pair<NetworkVO, NicProfile>(publicNetworks.get(0), publicNic));
                publicVlans.add(publicIp.getVlanTag());
            }
        }
        
        return networks;
    }

    @DB
    protected NicProfile createPrivateNicProfileForGateway(VpcGateway privateGateway) {
        Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());
        PrivateIpVO ipVO = _privateIpDao.allocateIpAddress(privateNetwork.getDataCenterId(), privateNetwork.getId(), privateGateway.getIp4Address());
        Nic privateNic = _nicDao.findByIp4AddressAndNetworkId(ipVO.getIpAddress(), privateNetwork.getId());
        
        NicProfile privateNicProfile = new NicProfile();
        
        if (privateNic != null) {
            VirtualMachine vm = _vmDao.findById(privateNic.getId());
            privateNicProfile = new NicProfile(privateNic, privateNetwork, privateNic.getBroadcastUri(), privateNic.getIsolationUri(), 
                    _networkModel.getNetworkRate(privateNetwork.getId(), vm.getId()), 
                    _networkModel.isSecurityGroupSupportedInNetwork(privateNetwork), 
                    _networkModel.getNetworkTag(vm.getHypervisorType(), privateNetwork));
        } else {
            String vlanTag = privateNetwork.getBroadcastUri().getHost();
            String netmask = NetUtils.getCidrNetmask(privateNetwork.getCidr());
            PrivateIpAddress ip = new PrivateIpAddress(ipVO, vlanTag, privateNetwork.getGateway(), netmask,
                    NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ipVO.getMacAddress())));
            
            privateNicProfile.setIp4Address(ip.getIpAddress());
            privateNicProfile.setGateway(ip.getGateway());
            privateNicProfile.setNetmask(ip.getNetmask());
            privateNicProfile.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            privateNicProfile.setBroadcastUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            privateNicProfile.setBroadcastType(BroadcastDomainType.Vlan);
            privateNicProfile.setFormat(AddressFormat.Ip4);
            privateNicProfile.setReservationId(String.valueOf(ip.getVlanTag()));
            privateNicProfile.setMacAddress(ip.getMacAddress());
        }
       
        return privateNicProfile;
    }
   
    protected NicProfile createGuestNicProfileForVpcRouter(Network guestNetwork) {
        NicProfile guestNic = new NicProfile();
        guestNic.setIp4Address(guestNetwork.getGateway());
        guestNic.setBroadcastUri(guestNetwork.getBroadcastUri());
        guestNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
        guestNic.setIsolationUri(guestNetwork.getBroadcastUri());
        guestNic.setMode(guestNetwork.getMode());
        String gatewayCidr = guestNetwork.getCidr();
        guestNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));
        
        return guestNic;
    }
    
    protected Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> getNicsToChangeOnRouter 
    (final List<? extends PublicIpAddress> publicIps, VirtualRouter router) {
        //1) check which nics need to be plugged/unplugged and plug/unplug them
        
        Map<String, PublicIpAddress> nicsToPlug = new HashMap<String, PublicIpAddress>();
        Map<String, PublicIpAddress> nicsToUnplug = new HashMap<String, PublicIpAddress>();

        
        //find out nics to unplug
        for (PublicIpAddress ip : publicIps) {
            long publicNtwkId = ip.getNetworkId();
            
            //if ip is not associated to any network, and there are no firewall rules, release it on the backend
            if (!_vpcMgr.isIpAllocatedToVpc(ip)) {
                ip.setState(IpAddress.State.Releasing);
            }
                         
            if (ip.getState() == IpAddress.State.Releasing) {
                Nic nic = _nicDao.findByIp4AddressAndNetworkIdAndInstanceId(publicNtwkId, router.getId(), ip.getAddress().addr());
                if (nic != null) {
                    nicsToUnplug.put(ip.getVlanTag(), ip);
                    s_logger.debug("Need to unplug the nic for ip=" + ip + "; vlan=" + ip.getVlanTag() + 
                            " in public network id =" + publicNtwkId);
                }
            }
        }
        
        //find out nics to plug
        for (PublicIpAddress ip : publicIps) {
            URI broadcastUri = BroadcastDomainType.Vlan.toUri(ip.getVlanTag());
            long publicNtwkId = ip.getNetworkId();
            
            //if ip is not associated to any network, and there are no firewall rules, release it on the backend
            if (!_vpcMgr.isIpAllocatedToVpc(ip)) {
                ip.setState(IpAddress.State.Releasing);
            }
                         
            if (ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) {
                //nic has to be plugged only when there are no nics for this vlan tag exist on VR
                Nic nic = _nicDao.findByNetworkIdInstanceIdAndBroadcastUri(publicNtwkId, router.getId(), 
                        broadcastUri.toString());
                
                if (nic == null && nicsToPlug.get(ip.getVlanTag()) == null) {
                    nicsToPlug.put(ip.getVlanTag(), ip);
                    s_logger.debug("Need to plug the nic for ip=" + ip + "; vlan=" + ip.getVlanTag() + 
                            " in public network id =" + publicNtwkId);
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
        
        Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> nicsToChange = 
                new Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>>(nicsToPlug, nicsToUnplug);
        return nicsToChange;
    }
    
    @Override
    public void finalizeStop(VirtualMachineProfile<DomainRouterVO> profile, StopAnswer answer) {
        super.finalizeStop(profile, answer);
        //Mark VPN connections as Disconnected
        DomainRouterVO router = profile.getVirtualMachine();
        Long vpcId = router.getVpcId();
        if (vpcId != null) {
            _s2sVpnMgr.markDisconnectVpnConnByVpc(vpcId);
        }
    }
    
    @Override
    public List<DomainRouterVO> getVpcRouters(long vpcId) {
        return _routerDao.listByVpcId(vpcId);
    }

}
