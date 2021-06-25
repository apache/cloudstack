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

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
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
import org.apache.cloudstack.network.tungsten.agent.api.ReleaseTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@Component
public class TungstenGuestNetworkGuru extends GuestNetworkGuru {

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
    AccountDao accountDao;
    @Inject
    IpAddressManager ipAddrMgr;

    private static final Networks.TrafficType[] TrafficTypes = {Networks.TrafficType.Guest};

    public TungstenGuestNetworkGuru() {
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
            && isMyIsolationMethod(physicalNetwork) && _ntwkOfferingSrvcDao.isProviderForNetworkOffering(
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

        if (offering.getGuestType() == Network.GuestType.Isolated) {

            NetworkVO network = (NetworkVO) super.design(offering, plan, userSpecified, owner);

            if (network == null) {
                return null;
            }

            network.setBroadcastDomainType(Networks.BroadcastDomainType.Tungsten);
            return network;
        }

        if (offering.getGuestType() == Network.GuestType.Shared) {
            Network.State state = Network.State.Allocated;
            if (dc.getNetworkType() == DataCenter.NetworkType.Basic) {
                state = Network.State.Setup;
            }

            NetworkVO network = new NetworkVO(offering.getTrafficType(), Networks.Mode.Dhcp,
                Networks.BroadcastDomainType.Tungsten, offering.getId(), state, plan.getDataCenterId(),
                plan.getPhysicalNetworkId(), offering.isRedundantRouter());

            if (userSpecified != null) {
                if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) || (
                    userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                    throw new InvalidParameterValueException("cidr and gateway must be specified together.");
                }

                if ((userSpecified.getIp6Cidr() == null && userSpecified.getIp6Gateway() != null) || (
                    userSpecified.getIp6Cidr() != null && userSpecified.getIp6Gateway() == null)) {
                    throw new InvalidParameterValueException("cidrv6 and gatewayv6 must be specified together.");
                }

                if (userSpecified.getCidr() != null) {
                    network.setCidr(userSpecified.getCidr());
                    network.setGateway(userSpecified.getGateway());
                }

                if (userSpecified.getIp6Cidr() != null) {
                    network.setIp6Cidr(userSpecified.getIp6Cidr());
                    network.setIp6Gateway(userSpecified.getIp6Gateway());
                }

                if (userSpecified.getBroadcastUri() != null) {
                    network.setBroadcastUri(userSpecified.getBroadcastUri());
                    network.setState(Network.State.Setup);
                }

                if (userSpecified.getBroadcastDomainType() != null) {
                    network.setBroadcastDomainType(userSpecified.getBroadcastDomainType());
                }

                if (userSpecified.getPvlanType() != null) {
                    network.setPvlanType(userSpecified.getPvlanType());
                }
            }

            boolean isSecurityGroupEnabled = _networkModel.areServicesSupportedByNetworkOffering(offering.getId(),
                Network.Service.SecurityGroup);
            if (isSecurityGroupEnabled) {
                network.setName("SecurityGroupEnabledNetwork");
                network.setDisplayText("SecurityGroupEnabledNetwork");
            }

            return network;
        }
        return null;
    }

    @Override
    public NicProfile allocate(Network config, NicProfile nic, VirtualMachineProfile vm)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        if (config.getGuestType() == Network.GuestType.Shared) {

            if (vm.getType() == VirtualMachine.Type.User) {

                String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(config);

                // create tungsten network
                createTungstenNetwork(config, tungstenProjectFqn);

                // create logical router with management network
                TungstenAnswer createLogicalRouterAnswer = createTungstenLogicalRouter(config, tungstenProjectFqn,
                    Networks.TrafficType.Management);
                if (!createLogicalRouterAnswer.getResult()) {
                    throw new CloudRuntimeException("can not create Tungsten-Fabric logical router");
                }

                // add default tungsten network policy
                tungstenService.addTungstenDefaultNetworkPolicy(config.getDataCenterId(), tungstenProjectFqn,
                    TungstenUtils.getVirtualNetworkPolicyName(config.getId()), config.getUuid(),
                    getDefautlGuestNetworkPolicyRule(config), 100, 0);

                // create tungsten vmi gateway
                createTungstenVmiGateway(config, tungstenProjectFqn);
            } else {
                DataCenter dc = _dcDao.findById(config.getDataCenterId());

                if (nic == null) {
                    nic = new NicProfile(Nic.ReservationStrategy.Create, null, null, null, null);
                } else if (nic.getIPv4Address() == null && nic.getIPv6Address() == null) {
                    nic.setReservationStrategy(Nic.ReservationStrategy.Start);
                } else {
                    nic.setReservationStrategy(Nic.ReservationStrategy.Create);
                }

                allocateDirectIp(nic, config, vm, dc, nic.getRequestedIPv4(), nic.getRequestedIPv6());
                nic.setReservationStrategy(Nic.ReservationStrategy.Create);

                if (nic.getMacAddress() == null) {
                    nic.setMacAddress(_networkModel.getNextAvailableMacAddressInNetwork(config.getId()));
                    if (nic.getMacAddress() == null) {
                        throw new InsufficientAddressCapacityException("Unable to allocate more mac addresses",
                            Network.class, config.getId());
                    }
                }

                return nic;
            }
        }
        return super.allocate(config, nic, vm);
    }

    @Override
    @DB
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile vm) {
        super.deallocate(config, nic, vm);

        try {
            // delete tungsten vm
            DeleteTungstenVmCommand cmd = new DeleteTungstenVmCommand(vm.getUuid());
            tungstenFabricUtils.sendTungstenCommand(cmd, config.getDataCenterId());
        } catch (IllegalArgumentException e) {
            throw new CloudRuntimeException("Failing to expunge the vm from Tungsten-Fabric with the uuid " + vm.getUuid());
        }
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest,
        ReservationContext context) throws InsufficientVirtualNetworkCapacityException {

        assert (network.getState() == Network.State.Implementing) : "Why are we implementing " + network;

        // get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();
        Long zoneId = network.getDataCenterId();

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(),
            network.getBroadcastDomainType(), network.getNetworkOfferingId(), Network.State.Allocated,
            network.getDataCenterId(), physicalNetworkId, offering.isRedundantRouter());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        implemented.setBroadcastUri(Networks.BroadcastDomainType.Tungsten.toUri("tf"));

        // setup tungsten network
        try {
            String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);
            Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
                Networks.TrafficType.Public);

            // create tungsten network
            createTungstenNetwork(network, tungstenProjectFqn);

            // create logical router with public network
            TungstenAnswer createLogicalRouterAnswer = createTungstenLogicalRouter(network, tungstenProjectFqn,
                Networks.TrafficType.Public);
            if (!createLogicalRouterAnswer.getResult()) {
                throw new CloudRuntimeException("can not create Tungsten-Fabric logical router");
            }

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
                        null, TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, null, natIp, TungstenUtils.MAX_CIDR, -1, -1));
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

        String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);

        // create tungsten vm ( vmi - ii - port )
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vm.getId());
        HostVO host = hostDao.findById(vmInstanceVO.getHostId());
        CreateTungstenVirtualMachineCommand cmd = new CreateTungstenVirtualMachineCommand(tungstenProjectFqn,
            network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(), nic.getIPv4Address(),
            nic.getMacAddress(), TungstenUtils.getUserVm(), TungstenUtils.getGuestType(), host.getPublicIpAddress());
        tungstenFabricUtils.sendTungstenCommand(cmd, network.getDataCenterId());

        if (nic.getName() == null) {
            nic.setName(TungstenUtils.getDefaultVhostInterface());
        }
    }

    @Override
    public boolean release(final NicProfile nic, final VirtualMachineProfile vm, final String reservationId) {
        Network network = networkDao.findById(nic.getNetworkId());
        String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);

        // release floating ip
        if (network.getGuestType().equals(Network.GuestType.Isolated)) {
            Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
                Networks.TrafficType.Public);
            IPAddressVO ipAddressVO = ipAddressDao.findByAssociatedVmId(vm.getId());
            if (ipAddressVO != null) {
                TungstenCommand releaseTungstenFloatingIpCommand = new ReleaseTungstenFloatingIpCommand(
                    publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                    TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
                TungstenAnswer releaseFloatingIpAnswer = tungstenFabricUtils.sendTungstenCommand(
                    releaseTungstenFloatingIpCommand, network.getDataCenterId());
                if (!releaseFloatingIpAnswer.getResult()) {
                    return false;
                }
            }
        }

        // delete vrouter port
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vm.getId());
        long hostId = vmInstanceVO.getHostId() != null ? vmInstanceVO.getHostId() : vmInstanceVO.getLastHostId();
        HostVO host = hostDao.findById(hostId);
        TungstenCommand deleteTungstenVRouterPortCommand = new DeleteTungstenVRouterPortCommand(
            host.getPublicIpAddress(), nic.getUuid());
        TungstenAnswer deleteTungstenVrouterPortAnswer = tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenVRouterPortCommand, network.getDataCenterId());
        if (!deleteTungstenVrouterPortAnswer.getResult()) {
            return false;
        }

        // delete instance ip and vmi
        String nicName = TungstenUtils.getVmiName(TungstenUtils.getGuestType(), TungstenUtils.getUserVm(),
            vm.getInstanceName(), nic.getId());
        TungstenCommand cmd = new DeleteTungstenVmInterfaceCommand(tungstenProjectFqn, nicName);
        TungstenAnswer deleteTungstenVmInterfaceAnswer = tungstenFabricUtils.sendTungstenCommand(cmd,
            network.getDataCenterId());
        if (!deleteTungstenVmInterfaceAnswer.getResult()) {
            return false;
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

            // delete network
            DeleteTungstenNetworkCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(
                network.getUuid());
            tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, network.getDataCenterId());
        } catch (Exception e) {
            return false;
        }
        return super.trash(network, offering);
    }

    private void createTungstenNetwork(Network network, String tungstenProjectFqn) {
        Pair<String, Integer> pair = NetUtils.getCidr(network.getCidr());
        TungstenCommand createTungstenGuestNetworkCommand = new CreateTungstenNetworkCommand(network.getUuid(),
            TungstenUtils.getGuestNetworkName(network.getName()), network.getName(), tungstenProjectFqn, false, false,
            pair.first(), pair.second(), network.getGateway(), network.getMode().equals(Networks.Mode.Dhcp), null, null,
            null, false, false, TungstenUtils.getSubnetName(network.getId()));
        tungstenFabricUtils.sendTungstenCommand(createTungstenGuestNetworkCommand, network.getDataCenterId());
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

    private TungstenAnswer createTungstenLogicalRouter(Network network, String tungstenProjectFqn,
        Networks.TrafficType trafficType) {
        Network managementNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            trafficType);
        TungstenCommand createTungstenLogicalRouterCommand = new CreateTungstenLogicalRouterCommand(
            TungstenUtils.getLogicalRouterName(network.getId()), tungstenProjectFqn, managementNetwork.getUuid());
        TungstenAnswer createLogicalRouterAnswer = tungstenFabricUtils.sendTungstenCommand(
            createTungstenLogicalRouterCommand, network.getDataCenterId());
        return createLogicalRouterAnswer;
    }

    protected void allocateDirectIp(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final DataCenter dc, final String requestedIp4Addr, final String requestedIp6Addr)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {

        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<InsufficientCapacityException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status)
                    throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
                    if (_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                        ipAddrMgr.allocateNicValues(nic, dc, vm, network, requestedIp4Addr, requestedIp6Addr);
                    } else {
                        ipAddrMgr.allocateDirectIp(nic, dc, vm, network, requestedIp4Addr, requestedIp6Addr);
                        //save the placeholder nic if the vm is the Virtual router
                        if (vm.getType() == VirtualMachine.Type.DomainRouter) {
                            Nic placeholderNic = _networkModel.getPlaceholderNicForRouter(network, null);
                            if (placeholderNic == null) {
                                s_logger.debug("Saving placeholder nic with ip4 address " + nic.getIPv4Address()
                                    + " and ipv6 address " + nic.getIPv6Address() + " for the network " + network);
                                _networkMgr.savePlaceholderNic(network, nic.getIPv4Address(), nic.getIPv6Address(),
                                    VirtualMachine.Type.DomainRouter);
                            }
                        }
                    }
                }
            });
        } catch (InsufficientCapacityException e) {
            ExceptionUtil.rethrow(e, InsufficientVirtualNetworkCapacityException.class);
            ExceptionUtil.rethrow(e, InsufficientAddressCapacityException.class);
            throw new IllegalStateException(e);
        }
    }
}
