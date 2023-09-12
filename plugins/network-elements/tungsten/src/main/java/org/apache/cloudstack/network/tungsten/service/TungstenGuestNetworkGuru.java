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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.network.tungsten.agent.api.ClearTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNatIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TungstenGuestNetworkGuru extends GuestNetworkGuru implements NetworkMigrationResponder {

    private static final Logger s_logger = Logger.getLogger(TungstenGuestNetworkGuru.class);

    @Inject
    NetworkDao networkDao;
    @Inject
    IpAddressManager ipAddressManager;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    TungstenService tungstenService;
    @Inject
    FirewallRulesDao firewallRulesDao;
    @Inject
    TungstenFabricUtils tungstenFabricUtils;
    @Inject
    HostDao hostDao;
    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    VlanDao vlanDao;
    @Inject
    AccountDao accountDao;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    TungstenProviderDao tungstenProviderDao;
    @Inject
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;

    private static final Networks.TrafficType[] TrafficTypes = {Networks.TrafficType.Guest};

    public TungstenGuestNetworkGuru() {
        super();
        _isolationMethods = new PhysicalNetwork.IsolationMethod[] {new PhysicalNetwork.IsolationMethod("TF")};
    }

    @Override
    public boolean isMyTrafficType(Networks.TrafficType type) {
        for (Networks.TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType,
        PhysicalNetwork physicalNetwork) {
        return networkType == DataCenter.NetworkType.Advanced && isMyTrafficType(offering.getTrafficType())
            && isMyIsolationMethod(physicalNetwork) && networkOfferingServiceMapDao.isProviderForNetworkOffering(
            offering.getId(), Network.Provider.Tungsten);
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {

        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            s_logger.debug("Refusing to design this network");
            return null;
        }

        NetworkVO network = (NetworkVO) super.design(offering, plan, userSpecified, owner);

        if (network == null) {
            return null;
        }

        if (userSpecified != null) {
            if ((userSpecified.getIp6Cidr() == null && userSpecified.getIp6Gateway() != null) || (
                userSpecified.getIp6Cidr() != null && userSpecified.getIp6Gateway() == null)) {
                throw new InvalidParameterValueException("cidrv6 and gatewayv6 must be specified together.");
            }

            if (userSpecified.getIp6Cidr() != null) {
                network.setIp6Cidr(userSpecified.getIp6Cidr());
                network.setIp6Gateway(userSpecified.getIp6Gateway());
            }
        }

        network.setBroadcastDomainType(Networks.BroadcastDomainType.TUNGSTEN);
        network.setState(Network.State.Allocated);
        return network;
    }

    @Override
    @DB
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile vm) {
        super.deallocate(config, nic, vm);
        String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(config);

        // delete instance ip and vmi
        String nicName = TungstenUtils.getVmiName(config.getTrafficType().toString(), vm.getType().toString(),
            vm.getInstanceName(), nic.getId());
        TungstenCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(tungstenProjectFqn, nicName);
        tungstenFabricUtils.sendTungstenCommand(deleteVmiCmd, config.getDataCenterId());

        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        if (nics.size() == 1) {
            try {
                // delete tungsten vm
                DeleteTungstenVmCommand cmd = new DeleteTungstenVmCommand(vm.getUuid());
                tungstenFabricUtils.sendTungstenCommand(cmd, config.getDataCenterId());
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException("Failing to expunge the vm from Tungsten-Fabric with the uuid " + vm.getUuid());
            }
        }
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest,
        ReservationContext context) {

        if (network.getState() != Network.State.Implementing) {
            throw new IllegalArgumentException("Why are we implementing " + network);
        }

        // get zone id
        long zoneId = network.getDataCenterId();

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(),
            network.getBroadcastDomainType(), network.getNetworkOfferingId(), Network.State.Implemented,
            network.getDataCenterId(), network.getPhysicalNetworkId(), offering.isRedundantRouter());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        implemented.setBroadcastUri(Networks.BroadcastDomainType.TUNGSTEN.toUri("tf"));

        // setup tungsten network
        try {
            if (offering.getGuestType() == Network.GuestType.Shared) {
                List<VlanVO> vlanVOList = vlanDao.listVlansByNetworkId(network.getId());
                if (!vlanVOList.isEmpty()) {
                    tungstenService.createSharedNetwork(network, vlanVOList.get(0));
                } else {
                    throw new CloudRuntimeException("can not create Tungsten-Fabric shared network");
                }
            } else {
                String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);
                Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
                    Networks.TrafficType.Public);

                // create tungsten network
                createTungstenNetwork(network, tungstenProjectFqn);

                if (!tungstenService.allocateDnsIpAddress(network, null, TungstenUtils.getSubnetName(network.getId()))) {
                    throw new CloudRuntimeException("can not allocate Tungsten-Fabric Dns Ip Address");
                }

                // create logical router with public network
                TungstenAnswer createLogicalRouterAnswer = createTungstenLogicalRouter(network, tungstenProjectFqn);

                // after logical router created, tungsten system will create service instance
                // need to wait for service instance created before get its public ip address
                // need to find a way to set public ip address to instance service
                TungstenCommand getTungstenNatIpCommand = new GetTungstenNatIpCommand(tungstenProjectFqn,
                    createLogicalRouterAnswer.getApiObjectBase().getUuid());
                TungstenAnswer getTungstenNatIpAnswer = tungstenFabricUtils.sendTungstenCommand(getTungstenNatIpCommand,
                    network.getDataCenterId());
                if (getTungstenNatIpAnswer.getResult()) {
                    String natIp = getTungstenNatIpAnswer.getDetails();
                    Account networkAccount = accountDao.findById(network.getAccountId());
                    ipAddressManager.assignSourceNatPublicIpAddress(zoneId, null, networkAccount,
                        Vlan.VlanType.VirtualNetwork, network.getId(), natIp, false, false);

                    // add default tungsten guest network policy
                    tungstenService.addTungstenDefaultNetworkPolicy(zoneId, tungstenProjectFqn,
                        TungstenUtils.getVirtualNetworkPolicyName(network.getId()), network.getUuid(),
                        getDefautlGuestNetworkPolicyRule(network), 100, 0);

                    IPAddressVO ipAddressVO = ipAddressDao.findByIpAndDcId(network.getDataCenterId(), natIp);
                    Pair<String, Integer> pair = NetUtils.getCidr(network.getCidr());

                    List<TungstenRule> tungstenSourceNatRuleList = new ArrayList<>();
                    tungstenSourceNatRuleList.add(
                        new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.ONE_WAY_DIRECTION, TungstenUtils.ANY,
                            null, TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, null, natIp, TungstenUtils.MAX_CIDR, -1,
                            -1));
                    tungstenSourceNatRuleList.add(
                        new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION, TungstenUtils.ANY,
                            TungstenUtils.ANY, pair.first(), pair.second(), -1, -1, TungstenUtils.ANY,
                            TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1));

                    tungstenService.addTungstenDefaultNetworkPolicy(zoneId, null,
                        TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), publicNetwork.getUuid(),
                        tungstenSourceNatRuleList, 0, 0);

                    // create gateway vmi, update logical router
                    createTungstenVmiGateway(network, tungstenProjectFqn);
                }
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException("unable to create Tungsten-Fabric network " + network.getUuid());
        }
        return implemented;
    }

    @Override
    public void reserve(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final DeployDestination dest, final ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        super.reserve(nic, network, vm, dest, context);
        createTungstenVM(nic, network, vm, dest);
        if (nic.getName() == null) {
            nic.setName(TungstenUtils.DEFAULT_VHOST_INTERFACE);
        }
    }

    @Override
    public boolean release(final NicProfile nic, final VirtualMachineProfile vm, final String reservationId) {
        // delete vrouter port
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vm.getId());
        Long hostId = vmInstanceVO.getHostId() != null ? vmInstanceVO.getHostId() : vmInstanceVO.getLastHostId();
        if (hostId != null) {
            TungstenAnswer tungstenAnswer = deleteVrouterPort(hostId, nic.getUuid());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }

        return super.release(nic, vm, reservationId);
    }

    @Override
    public void shutdown(final NetworkProfile profile, final NetworkOffering offering) {
        if (offering.getGuestType() == Network.GuestType.Isolated) {
            String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(profile);

            Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(profile.getDataCenterId(),
                Networks.TrafficType.Public);

            // clear floating ip
            List<IPAddressVO> staticNatIpList = ipAddressDao.listByAssociatedNetwork(profile.getId(), false);
            for (IPAddressVO ipAddressVO : staticNatIpList) {
                DeleteTungstenFloatingIpCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(
                    publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(profile.getDataCenterId()),
                    TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
                tungstenFabricUtils.sendTungstenCommand(deleteTungstenFloatingIpCommand, profile.getDataCenterId());
            }

            // delete rule network policy
            List<FirewallRuleVO> firewallRuleVOList = firewallRulesDao.listByNetworkAndPurpose(profile.getId(),
                FirewallRule.Purpose.Firewall);
            for (FirewallRuleVO firewallRuleVO : firewallRuleVOList) {
                String networkUuid =
                    firewallRuleVO.getTrafficType() == FirewallRule.TrafficType.Egress ? profile.getUuid() :
                        publicNetwork.getUuid();
                DeleteTungstenNetworkPolicyCommand deleteRuleNetworkPolicyCommand =
                    new DeleteTungstenNetworkPolicyCommand(
                    TungstenUtils.getRuleNetworkPolicyName(firewallRuleVO.getId()), tungstenProjectFqn, networkUuid);
                tungstenFabricUtils.sendTungstenCommand(deleteRuleNetworkPolicyCommand, profile.getDataCenterId());
            }

            // clear source nat network policy
            List<IPAddressVO> sourceNatIpList = ipAddressDao.listByAssociatedNetwork(profile.getId(), true);
            for (IPAddressVO ipAddressVO : sourceNatIpList) {
                DeleteTungstenNetworkPolicyCommand deletePublicNetworkPolicyCommand =
                    new DeleteTungstenNetworkPolicyCommand(
                    TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), null, publicNetwork.getUuid());
                tungstenFabricUtils.sendTungstenCommand(deletePublicNetworkPolicyCommand, profile.getDataCenterId());
            }
        }
        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        try {
            String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);

            // delete default network policy
            DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand =
                new DeleteTungstenNetworkPolicyCommand(
                TungstenUtils.getVirtualNetworkPolicyName(network.getId()), tungstenProjectFqn, network.getUuid());
            tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkPolicyCommand, network.getDataCenterId());

            // clear network gateway
            ClearTungstenNetworkGatewayCommand clearTungstenNetworkGatewayCommand =
                new ClearTungstenNetworkGatewayCommand(
                tungstenProjectFqn, TungstenUtils.getLogicalRouterName(network.getId()), network.getId());
            tungstenFabricUtils.sendTungstenCommand(clearTungstenNetworkGatewayCommand, network.getDataCenterId());

            if (network.getGuestType() == Network.GuestType.Shared) {
                tungstenService.deallocateDnsIpAddress(network, null, TungstenUtils.getIPV4SubnetName(network.getId()));
            } else {
                tungstenService.deallocateDnsIpAddress(network, null, TungstenUtils.getSubnetName(network.getId()));
            }

            if (network.getGuestType() == Network.GuestType.Shared) {
                NetworkDetailVO networkDetailVO = networkDetailsDao.findDetail(network.getId(), "vrf");
                TungstenProvider tungstenProvider = tungstenProviderDao.findByZoneId(network.getDataCenterId());
                if (tungstenProvider != null && networkDetailVO != null) {
                    Host host = hostDao.findByPublicIp(tungstenProvider.getGateway());
                    if (host != null) {
                        Command setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("delete",
                            TungstenUtils.getSgVgwName(network.getId()), network.getCidr(), NetUtils.ALL_IP4_CIDRS,
                            networkDetailVO.getValue());
                        agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
                    }

                    networkDetailsDao.expunge(networkDetailVO.getId());
                }
            }

            // delete network
            DeleteTungstenNetworkCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(
                network.getUuid());
            tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, network.getDataCenterId());
        } catch (Exception e) {
            return false;
        }
        return super.trash(network, offering);
    }

    @Override
    public boolean prepareMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final DeployDestination dest, final ReservationContext context) {
        if (vm.getType() == VirtualMachine.Type.User) {
            TungstenAnswer answer = createTungstenVM(nic, network, vm, dest);
            return answer.getResult();
        } else {
            return true;
        }
    }

    @Override
    public void rollbackMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final ReservationContext src, final ReservationContext dst) {
        if (vm.getType() == VirtualMachine.Type.User) {
            Long hostId = vm.getVirtualMachine().getHostId();
            if (hostId != null) {
                deleteVrouterPort(hostId, nic.getUuid());
            }
        }
    }

    @Override
    public void commitMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final ReservationContext src, final ReservationContext dst) {
        if (vm.getType() == VirtualMachine.Type.User) {
            Long hostId = vm.getVirtualMachine().getLastHostId();
            if (hostId != null) {
                deleteVrouterPort(hostId, nic.getUuid());
            }
        }
    }

    private void createTungstenNetwork(Network network, String tungstenProjectFqn) {
        Pair<String, Integer> pair = NetUtils.getCidr(network.getCidr());
        TungstenCommand createTungstenGuestNetworkCommand = new CreateTungstenNetworkCommand(network.getUuid(),
            TungstenUtils.getGuestNetworkName(network.getName(), network.getUuid()), network.getName(), tungstenProjectFqn, false, false,
            pair.first(), pair.second(), null, network.getMode().equals(Networks.Mode.Dhcp), null, null,
            null, false, false, TungstenUtils.getSubnetName(network.getId()));

        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(createTungstenGuestNetworkCommand, network.getDataCenterId());

        if (!tungstenAnswer.getResult()) {
            throw new CloudRuntimeException("can not create Tungsten-Fabric network");
        }
    }

    private void createTungstenVmiGateway(Network network, String tungstenProjectFqn) {
        SetTungstenNetworkGatewayCommand setTungstenNetworkGatewayCommand = new SetTungstenNetworkGatewayCommand(
            tungstenProjectFqn, TungstenUtils.getLogicalRouterName(network.getId()), network.getId(), network.getUuid(),
            network.getGateway());
        tungstenFabricUtils.sendTungstenCommand(setTungstenNetworkGatewayCommand, network.getDataCenterId());
    }

    private List<TungstenRule> getDefautlGuestNetworkPolicyRule(Network network) {
        Pair<String, Integer> pair = NetUtils.getCidr(network.getCidr());
        List<TungstenRule> tungstenRuleList = new ArrayList<>();
        tungstenRuleList.add(
            new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION, TungstenUtils.ANY,
                TungstenUtils.ANY, pair.first(), pair.second(), -1, -1, TungstenUtils.ANY, pair.first(), pair.second(),
                -1, -1));
        tungstenRuleList.add(
            new TungstenRule(TungstenUtils.DENY_ACTION, TungstenUtils.ONE_WAY_DIRECTION, TungstenUtils.ANY,
                TungstenUtils.ANY, pair.first(), pair.second(), -1, -1, TungstenUtils.ANY, TungstenUtils.ALL_IP4_PREFIX,
                0, -1, -1));
        return tungstenRuleList;
    }

    private TungstenAnswer createTungstenLogicalRouter(Network network, String tungstenProjectFqn) {
        Network managementNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
                Networks.TrafficType.Public);
        TungstenCommand createTungstenLogicalRouterCommand = new CreateTungstenLogicalRouterCommand(
            TungstenUtils.getLogicalRouterName(network.getId()), tungstenProjectFqn, managementNetwork.getUuid());
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(createTungstenLogicalRouterCommand, network.getDataCenterId());
        if (!tungstenAnswer.getResult()) {
            throw new CloudRuntimeException("can not create Tungsten-Fabric logical router");
        }
        return tungstenAnswer;
    }

    private TungstenAnswer createTungstenVM(NicProfile nic, Network network, VirtualMachineProfile vm,
        DeployDestination dest) {
        String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);

        // create tungsten vm ( vmi - ii - port )
        Host host = dest.getHost();
        if (host == null) {
            VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vm.getId());
            host = hostDao.findById(vmInstanceVO.getHostId());
        }

        String ipV4Address = nic.getIPv4Address() != null ? nic.getIPv4Address() : nic.getIPv6Address();
        String ipV6Address = nic.getIPv6Address() != null ? nic.getIPv6Address() : null;
        CreateTungstenVirtualMachineCommand cmd = new CreateTungstenVirtualMachineCommand(tungstenProjectFqn,
            network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(), ipV4Address, ipV6Address,
            nic.getMacAddress(), vm.getType().toString(), network.getTrafficType().toString(),
            host.getPublicIpAddress(), network.getGateway(), nic.isDefaultNic());
        return tungstenFabricUtils.sendTungstenCommand(cmd, network.getDataCenterId());
    }

    private TungstenAnswer deleteVrouterPort(long hostId, String nicUuid) {
        HostVO host = hostDao.findById(hostId);
        TungstenCommand deleteTungstenVRouterPortCommand = new DeleteTungstenVRouterPortCommand(
            host.getPublicIpAddress(), nicUuid);
        return tungstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, host.getDataCenterId());
    }
}
