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

import java.util.List;

import javax.inject.Inject;

import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;


public class DirectNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(DirectNetworkGuru.class);

    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    NicDao _nicDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    private NetworkService networkService;
    @Inject
    private NicSecondaryIpDao nicSecondaryIpDao;

    private static final TrafficType[] TrafficTypes = {TrafficType.Guest};
    protected IsolationMethod[] _isolationMethods;

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if the physical network isolation method contains the expected isolation method for this guru
     */
    protected boolean isMyIsolationMethod(PhysicalNetwork physicalNetwork) {
        for (IsolationMethod m : this.getIsolationMethods()) {
            List<String> isolationMethods = physicalNetwork.getIsolationMethods();
            if (CollectionUtils.isNotEmpty(isolationMethods)) {
                for (String method : isolationMethods) {
                    s_logger.debug(method + ": " + m.toString());
                    if (method.equalsIgnoreCase(m.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return TrafficTypes;
    }

    protected boolean canHandle(NetworkOffering offering, DataCenter dc, PhysicalNetwork physnet) {
        if (dc.getNetworkType() == NetworkType.Advanced
                && isMyTrafficType(offering.getTrafficType())
                && isMyIsolationMethod(physnet)
                && offering.getGuestType() == GuestType.Shared
                && !_ntwkOfferingSrvcDao.isProviderForNetworkOffering(offering.getId(), Network.Provider.NiciraNvp)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type " + GuestType.Shared);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());

        if (!canHandle(offering, dc, physnet)) {
            s_logger.info("Refusing to design this network");
            return null;
        }

        State state = State.Allocated;
        if (dc.getNetworkType() == NetworkType.Basic) {
            state = State.Setup;
        }

        NetworkVO config =
            new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Vlan, offering.getId(), state, plan.getDataCenterId(),
                    plan.getPhysicalNetworkId(), offering.isRedundantRouter());

        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) || (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }

            if ((userSpecified.getIp6Cidr() == null && userSpecified.getIp6Gateway() != null) ||
                (userSpecified.getIp6Cidr() != null && userSpecified.getIp6Gateway() == null)) {
                throw new InvalidParameterValueException("cidrv6 and gatewayv6 must be specified together.");
            }

            if (userSpecified.getCidr() != null) {
                config.setCidr(userSpecified.getCidr());
                config.setGateway(userSpecified.getGateway());
            }

            if (userSpecified.getIp6Cidr() != null) {
                config.setIp6Cidr(userSpecified.getIp6Cidr());
                config.setIp6Gateway(userSpecified.getIp6Gateway());
            }

            if (userSpecified.getBroadcastUri() != null) {
                config.setBroadcastUri(userSpecified.getBroadcastUri());
                config.setState(State.Setup);
            }

            if (userSpecified.getBroadcastDomainType() != null) {
                config.setBroadcastDomainType(userSpecified.getBroadcastDomainType());
            }
        }

        boolean isSecurityGroupEnabled = _networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Service.SecurityGroup);
        if (isSecurityGroupEnabled) {
            config.setName("SecurityGroupEnabledNetwork");
            config.setDisplayText("SecurityGroupEnabledNetwork");
        }

        return config;
    }

    protected DirectNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] { new IsolationMethod("VLAN"), new IsolationMethod("VXLAN") };
    }

    public IsolationMethod[] getIsolationMethods() {
        return _isolationMethods;
    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (profile != null) {
            profile.setIPv4Dns1(dc.getDns1());
            profile.setIPv4Dns2(dc.getDns2());
            profile.setIPv6Dns1(dc.getIp6Dns1());
            profile.setIPv6Dns2(dc.getIp6Dns2());
        }
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException, ConcurrentOperationException {

        DataCenter dc = _dcDao.findById(network.getDataCenterId());

        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        } else if (nic.getIPv4Address() == null && nic.getIPv6Address() == null) {
            nic.setReservationStrategy(ReservationStrategy.Start);
        } else {
            nic.setReservationStrategy(ReservationStrategy.Create);
        }

        allocateDirectIp(nic, network, vm, dc, nic.getRequestedIPv4(), nic.getRequestedIPv6());
        nic.setReservationStrategy(ReservationStrategy.Create);

        if (nic.getMacAddress() == null) {
            nic.setMacAddress(_networkModel.getNextAvailableMacAddressInNetwork(network.getId()));
            if (nic.getMacAddress() == null) {
                throw new InsufficientAddressCapacityException("Unable to allocate more mac addresses", Network.class, network.getId());
            }
        }

        return nic;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIPv4Address() == null && nic.getIPv6Address() == null) {
            allocateDirectIp(nic, network, vm, dest.getDataCenter(), null, null);
            nic.setReservationStrategy(ReservationStrategy.Create);
        }
    }

    @DB
    protected void allocateDirectIp(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final DataCenter dc, final String requestedIp4Addr,
        final String requestedIp6Addr) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {

        try {
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<InsufficientCapacityException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws InsufficientVirtualNetworkCapacityException,
                        InsufficientAddressCapacityException {
                    if (_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                        _ipAddrMgr.allocateNicValues(nic, dc, vm, network, requestedIp4Addr, requestedIp6Addr);
                    } else {
                        _ipAddrMgr.allocateDirectIp(nic, dc, vm, network, requestedIp4Addr, requestedIp6Addr);
                        //save the placeholder nic if the vm is the Virtual router
                        if (vm.getType() == VirtualMachine.Type.DomainRouter) {
                            Nic placeholderNic = _networkModel.getPlaceholderNicForRouter(network, null);
                            if (placeholderNic == null) {
                                s_logger.debug("Saving placeholder nic with ip4 address " + nic.getIPv4Address() + " and ipv6 address " + nic.getIPv6Address() +
                                        " for the network " + network);
                                _networkMgr.savePlaceholderNic(network, nic.getIPv4Address(), nic.getIPv6Address(), VirtualMachine.Type.DomainRouter);
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

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        return true;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        return network;
    }

    @Override
    @DB
    public void deallocate(final Network network, final NicProfile nic, VirtualMachineProfile vm) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deallocate network: networkId: " + nic.getNetworkId() + ", ip: " + nic.getIPv4Address());
        }

        if (nic.getIPv4Address() != null) {
            final IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIPv4Address());
            if (ip != null) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        // if the ip address a part of placeholder, don't release it
                        Nic placeholderNic = _networkModel.getPlaceholderNicForRouter(network, null);
                        if (placeholderNic != null && placeholderNic.getIPv4Address().equalsIgnoreCase(ip.getAddress().addr())) {
                            s_logger.debug("Not releasing direct ip " + ip.getId() + " yet as its ip is saved in the placeholder");
                        } else {
                            _ipAddrMgr.markIpAsUnavailable(ip.getId());
                            _ipAddressDao.unassignIpAddress(ip.getId());
                        }

                        //unassign nic secondary ip address
                        s_logger.debug("remove nic " + nic.getId() + " secondary ip ");
                        List<String> nicSecIps = null;
                        nicSecIps = _nicSecondaryIpDao.getSecondaryIpAddressesForNic(nic.getId());
                        for (String secIp : nicSecIps) {
                            if (NetUtils.isValidIp4(secIp)) {
                                IPAddressVO pubIp = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), secIp);
                                _ipAddrMgr.markIpAsUnavailable(pubIp.getId());
                                _ipAddressDao.unassignIpAddress(pubIp.getId());
                            } else {
                                NicSecondaryIpVO nicSecIp = nicSecondaryIpDao.findByIp6AddressAndNetworkId(secIp, nic.getNetworkId());
                                if (nicSecIp != null) {
                                    networkService.releaseSecondaryIpFromNic(nicSecIp.getId());
                                }
                            }
                        }
                    }
                });
            }
        }

        nic.deallocate();
    }

    @Override
    public void shutdown(NetworkProfile network, NetworkOffering offering) {
    }

    @Override
    @DB
    public boolean trash(Network network, NetworkOffering offering) {
        //Have to remove all placeholder nics
        try {
            long id = network.getId();
            final List<NicVO> nics = _nicDao.listPlaceholderNicsByNetworkId(id);
            if (nics != null) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        for (Nic nic : nics) {
                            if (nic.getIPv4Address() != null) {
                                s_logger.debug("Releasing ip " + nic.getIPv4Address() + " of placeholder nic " + nic);
                                IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIPv4Address());
                                if (ip != null) {
                                    _ipAddrMgr.markIpAsUnavailable(ip.getId());
                                    _ipAddressDao.unassignIpAddress(ip.getId());
                                    s_logger.debug("Removing placeholder nic " + nic);
                                    _nicDao.remove(nic.getId());
                                }
                            }
                        }
                    }
                });
            }
            return true;
        }catch (Exception e) {
            s_logger.error("trash. Exception:" + e.getMessage());
            throw new CloudRuntimeException("trash. Exception:" + e.getMessage(),e);
        }
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        DataCenter dc = _dcDao.findById(networkProfile.getDataCenterId());
        networkProfile.setDns1(dc.getDns1());
        networkProfile.setDns2(dc.getDns2());
    }
}
