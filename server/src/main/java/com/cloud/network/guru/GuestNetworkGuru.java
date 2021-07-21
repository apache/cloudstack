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
package com.cloud.network.guru;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import com.cloud.network.Network.GuestType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

public abstract class GuestNetworkGuru extends AdapterBase implements NetworkGuru, Configurable {
    private static final Logger s_logger = Logger.getLogger(GuestNetworkGuru.class);

    @Inject
    protected VpcDao _vpcDao;
    @Inject
    protected NetworkOrchestrationService _networkMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected VlanDao _vlanDao;
    @Inject
    protected NicDao _nicDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    protected PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    IpAddressManager _ipAddrMgr;
    Random _rand = new Random(System.currentTimeMillis());

    static final ConfigKey<Boolean> UseSystemGuestVlans =
            new ConfigKey<Boolean>(
                    "Advanced",
                    Boolean.class,
                    "use.system.guest.vlans",
                    "true",
                    "If true, when account has dedicated guest vlan range(s), once the vlans dedicated to the account have been consumed vlans will be allocated from the system pool",
                    false, ConfigKey.Scope.Account);

    private static final TrafficType[] TrafficTypes = {TrafficType.Guest};

    // Currently set to anything except STT for the Nicira integration.
    protected IsolationMethod[] _isolationMethods;

    String _defaultGateway;
    String _defaultCidr;

    protected GuestNetworkGuru() {
        super();
        _isolationMethods = null;
    }

    @Override
    public boolean isMyTrafficType(final TrafficType type) {
        for (final TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return TrafficTypes;
    }

    public boolean isMyIsolationMethod(final PhysicalNetwork physicalNetwork) {
        if (physicalNetwork == null) {
            // Can't tell if there is no physical network
            return false;
        }

        List<String> methods = new ArrayList<String>();
        for (final String method : physicalNetwork.getIsolationMethods()) {
            methods.add(method.toLowerCase());
        }
        if (methods.isEmpty()) {
            // The empty isolation method is assumed to be VLAN
            s_logger.debug("Empty physical isolation type for physical network " + physicalNetwork.getUuid());
            methods = new ArrayList<String>(1);
            methods.add("VLAN".toLowerCase());
        }

        for (final IsolationMethod m : _isolationMethods) {
            if (methods.contains(m.toString().toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public IsolationMethod[] getIsolationMethods() {
        return _isolationMethods;
    }

    protected abstract boolean canHandle(NetworkOffering offering, final NetworkType networkType, PhysicalNetwork physicalNetwork);

    @Override
    public Network design(final NetworkOffering offering, final DeploymentPlan plan, final Network userSpecified, final Account owner) {
        final DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        final PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());

        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            return null;
        }

        final NetworkVO network =
                new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Vlan, offering.getId(), State.Allocated, plan.getDataCenterId(),
                        plan.getPhysicalNetworkId(), offering.isRedundantRouter());
        if (userSpecified != null) {
            if (userSpecified.getCidr() == null && userSpecified.getGateway() != null || userSpecified.getCidr() != null && userSpecified.getGateway() == null) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }

            if (userSpecified.getCidr() != null) {
                network.setCidr(userSpecified.getCidr());
                network.setGateway(userSpecified.getGateway());
            } else if (offering.getGuestType() != GuestType.L2 && (offering.getGuestType() == GuestType.Shared || !_networkModel.listNetworkOfferingServices(offering.getId()).isEmpty())) {
                final String guestNetworkCidr = dc.getGuestNetworkCidr();
                if (guestNetworkCidr != null) {
                    final String[] cidrTuple = guestNetworkCidr.split("\\/");
                    network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
                    network.setCidr(guestNetworkCidr);
                } else if (dc.getNetworkType() == NetworkType.Advanced) {
                    throw new CloudRuntimeException("Can't design network " + network + "; guest CIDR is not configured per zone " + dc);
                }
            }

            if (offering.isSpecifyVlan()) {
                network.setBroadcastUri(userSpecified.getBroadcastUri());
                if (!offering.isPersistent()) {
                    network.setState(State.Setup);
                }
                if (userSpecified.getPvlanType() != null) {
                    network.setBroadcastDomainType(BroadcastDomainType.Pvlan);
                    network.setPvlanType(userSpecified.getPvlanType());
                }
            }
        } else {
            final String guestNetworkCidr = dc.getGuestNetworkCidr();
            if (guestNetworkCidr == null && dc.getNetworkType() == NetworkType.Advanced) {
                throw new CloudRuntimeException("Can't design network " + network + "; guest CIDR is not configured per zone " + dc);
            }
            final String[] cidrTuple = guestNetworkCidr.split("\\/");
            network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
            network.setCidr(guestNetworkCidr);
        }

        return network;
    }

    @Override
    @DB
    public void deallocate(final Network network, final NicProfile nic, final VirtualMachineProfile vm) {
        if (network.getSpecifyIpRanges()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Deallocate network: networkId: " + nic.getNetworkId() + ", ip: " + nic.getIPv4Address());
            }

            final IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIPv4Address());
            if (ip != null) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(final TransactionStatus status) {
                        _ipAddrMgr.markIpAsUnavailable(ip.getId());
                        _ipAddressDao.unassignIpAddress(ip.getId());
                    }
                });
            }
            nic.deallocate();
        }
    }

    public int getVlanOffset(final long physicalNetworkId, final int vlanTag) {
        final PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork == null) {
            throw new CloudRuntimeException("Could not find the physical Network " + physicalNetworkId + ".");
        }

        if (pNetwork.getVnet() == null) {
            throw new CloudRuntimeException("Could not find vlan range for physical Network " + physicalNetworkId + ".");
        }
        Integer lowestVlanTag = null;
        final List<Pair<Integer, Integer>> vnetList = pNetwork.getVnet();
        //finding the vlanrange in which the vlanTag lies.
        for (final Pair<Integer, Integer> vnet : vnetList) {
            if (vlanTag >= vnet.first() && vlanTag <= vnet.second()) {
                lowestVlanTag = vnet.first();
            }
        }
        if (lowestVlanTag == null) {
            throw new InvalidParameterValueException("The vlan tag does not belong to any of the existing vlan ranges");
        }
        return vlanTag - lowestVlanTag;
    }

    public int getGloballyConfiguredCidrSize() {
        try {
            final String globalVlanBits = _configDao.getValue(Config.GuestVlanBits.key());
            return 8 + Integer.parseInt(globalVlanBits);
        } catch (final Exception e) {
            throw new CloudRuntimeException("Failed to read the globally configured VLAN bits size.");
        }
    }

    protected void allocateVnet(final Network network, final NetworkVO implemented, final long dcId, final long physicalNetworkId, final String reservationId)
            throws InsufficientVirtualNetworkCapacityException {
        if (network.getBroadcastUri() == null) {
            final String vnet = _dcDao.allocateVnet(dcId, physicalNetworkId, network.getAccountId(), reservationId, UseSystemGuestVlans.valueIn(network.getAccountId()));
            if (vnet == null) {
                throw new InsufficientVirtualNetworkCapacityException("Unable to allocate vnet as a " + "part of network " + network + " implement ", DataCenter.class,
                        dcId);
            }
            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vnet));
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(), EventVO.LEVEL_INFO,
                    EventTypes.EVENT_ZONE_VLAN_ASSIGN, "Assigned Zone Vlan: " + vnet + " Network Id: " + network.getId(), 0);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
    }

    @Override
    public Network implement(final Network network, final NetworkOffering offering, final DeployDestination dest, final ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException {
        assert network.getState() == State.Implementing : "Why are we implementing " + network;

        final long dcId = dest.getDataCenter().getId();

        //get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();

        // physical network id can be null in Guest Network in Basic zone, so locate the physical network
        if (physicalNetworkId == null) {
            physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
        }

        final NetworkVO implemented =
                new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                        network.getDataCenterId(), physicalNetworkId, offering.isRedundantRouter());

        allocateVnet(network, implemented, dcId, physicalNetworkId, context.getReservationId());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }
        return implemented;
    }

    @Override
    public NicProfile allocate(final Network network, NicProfile nic, final VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
    InsufficientAddressCapacityException {

        assert network.getTrafficType() == TrafficType.Guest : "Look at my name!  Why are you calling" + " me when the traffic type is : " + network.getTrafficType();

        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Start, null, null, null, null);
        }

        final DataCenter dc = _dcDao.findById(network.getDataCenterId());

        if (nic.getIPv4Address() == null) {
            nic.setBroadcastUri(network.getBroadcastUri());
            nic.setIsolationUri(network.getBroadcastUri());
            nic.setIPv4Gateway(network.getGateway());

            String guestIp = null;
            if (network.getSpecifyIpRanges()) {
                _ipAddrMgr.allocateDirectIp(nic, dc, vm, network, nic.getRequestedIPv4(), null);
            } else {
                //if Vm is router vm and source nat is enabled in the network, set ip4 to the network gateway
                boolean isGateway = false;
                if (vm.getVirtualMachine().getType() == VirtualMachine.Type.DomainRouter) {
                    if (network.getVpcId() != null) {
                        final Vpc vpc = _vpcDao.findById(network.getVpcId());
                        // Redundant Networks need a guest IP that is not the same as the gateway IP.
                        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.VPCVirtualRouter) && !vpc.isRedundant()) {
                            isGateway = true;
                        }
                    } else {
                        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.VirtualRouter)) {
                            isGateway = true;
                        }
                    }
                }

                if (isGateway) {
                    guestIp = network.getGateway();
                } else {
                    if (network.getGuestType() != GuestType.L2 && vm.getType() == VirtualMachine.Type.DomainRouter) {
                        Nic placeholderNic = _networkModel.getPlaceholderNicForRouter(network, null);
                        if (placeholderNic != null) {
                            s_logger.debug("Nic got an ip address " + placeholderNic.getIPv4Address() + " stored in placeholder nic for the network " + network);
                            guestIp = placeholderNic.getIPv4Address();
                        }
                    }
                    if (guestIp == null) {
                        if (vm.getVirtualMachine().getType() == VirtualMachine.Type.DomainRouter) {
                            guestIp = _ipAddrMgr.acquireGuestIpAddressByPlacement(network, nic.getRequestedIPv4());
                        } else {
                            guestIp = _ipAddrMgr.acquireGuestIpAddress(network, nic.getRequestedIPv4());
                        }
                    }
                    if (guestIp == null && network.getGuestType() != GuestType.L2 && !_networkModel.listNetworkOfferingServices(network.getNetworkOfferingId()).isEmpty()) {
                        throw new InsufficientVirtualNetworkCapacityException("Unable to acquire Guest IP" + " address for network " + network, DataCenter.class,
                                dc.getId());
                    }
                }

                nic.setIPv4Address(guestIp);
                if (network.getCidr() != null) {
                    nic.setIPv4Netmask(NetUtils.cidr2Netmask(_networkModel.getValidNetworkCidr(network)));
                }

                nic.setIPv4Dns1(dc.getDns1());
                nic.setIPv4Dns2(dc.getDns2());
                nic.setFormat(AddressFormat.Ip4);
            }
        }

        nic.setReservationStrategy(ReservationStrategy.Start);

        if (nic.getMacAddress() == null) {
            nic.setMacAddress(_networkModel.getNextAvailableMacAddressInNetwork(network.getId()));
            if (nic.getMacAddress() == null) {
                throw new InsufficientAddressCapacityException("Unable to allocate more mac addresses", Network.class, network.getId());
            }
        }

        return nic;
    }

    @Override
    public void updateNicProfile(final NicProfile profile, final Network network) {
        final DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (profile != null) {
            profile.setIPv4Dns1(dc.getDns1());
            profile.setIPv4Dns2(dc.getDns2());
        }
    }

    @Override
    public void reserve(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final DeployDestination dest, final ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        assert nic.getReservationStrategy() == ReservationStrategy.Start : "What can I do for nics that are not allocated at start? ";

        nic.setBroadcastUri(network.getBroadcastUri());
        nic.setIsolationUri(network.getBroadcastUri());
    }

    @Override
    public boolean release(final NicProfile nic, final VirtualMachineProfile vm, final String reservationId) {
        nic.setBroadcastUri(null);
        nic.setIsolationUri(null);
        return true;
    }

    @Override
    public void shutdown(final NetworkProfile profile, final NetworkOffering offering) {
        if (profile.getBroadcastUri() == null) {
            return; // Nothing to do here if the uri is null already
        }

        if ((profile.getBroadcastDomainType() == BroadcastDomainType.Vlan || profile.getBroadcastDomainType() == BroadcastDomainType.Vxlan) && !offering.isSpecifyVlan()) {
            s_logger.debug("Releasing vnet for the network id=" + profile.getId());
            _dcDao.releaseVnet(BroadcastDomainType.getValue(profile.getBroadcastUri()), profile.getDataCenterId(), profile.getPhysicalNetworkId(), profile.getAccountId(),
                    profile.getReservationId());
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), profile.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_RELEASE,
                    "Released Zone Vnet: " + BroadcastDomainType.getValue(profile.getBroadcastUri()) + " for Network: " + profile.getId(), 0);
        }

        profile.setBroadcastUri(null);
    }

    @Override
    public boolean trash(final Network network, final NetworkOffering offering) {
        return true;
    }

    @Override
    public void updateNetworkProfile(final NetworkProfile networkProfile) {
        final DataCenter dc = _dcDao.findById(networkProfile.getDataCenterId());
        networkProfile.setDns1(dc.getDns1());
        networkProfile.setDns2(dc.getDns2());
    }

    @Override
    public String getConfigComponentName() {
        return GuestNetworkGuru.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{UseSystemGuestVlans};
    }
}
