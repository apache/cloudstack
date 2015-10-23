/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.network.guru;

import java.net.URI;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
public class MidoNetPublicNetworkGuru extends PublicNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(MidoNetPublicNetworkGuru.class);

    // Inject any stuff we need to use (DAOs etc)
    @Inject
    NetworkModel _networkModel;
    @Inject
    AccountDao _accountDao;
    @Inject
    IpAddressManager _ipAddrMgr;

    // Don't need to change traffic type stuff, public is fine

    // Only change is to make broadcast domain type Mido
    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network network, Account owner) {
        s_logger.debug("design called with network: " + network);
        if (!canHandle(offering)) {
            return null;
        }

        if (offering.getTrafficType() == Networks.TrafficType.Public) {
            NetworkVO ntwk =
                new NetworkVO(offering.getTrafficType(), Networks.Mode.Static, Networks.BroadcastDomainType.Mido, offering.getId(), Network.State.Allocated,
                    plan.getDataCenterId(), plan.getPhysicalNetworkId(), offering.getRedundantRouter());
            return ntwk;
        } else {
            return null;
        }
    }

    protected MidoNetPublicNetworkGuru() {
        super();
    }

    @Override
    protected void getIp(NicProfile nic, DataCenter dc, VirtualMachineProfile vm, Network network) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIPv4Address() == null) {
            PublicIp ip = _ipAddrMgr.assignPublicIpAddress(dc.getId(), null, vm.getOwner(), Vlan.VlanType.VirtualNetwork, null, null, false);
            nic.setIPv4Address(ip.getAddress().addr());

            nic.setIPv4Gateway(ip.getGateway());

            // Set netmask to /24 for now
            // TDO make it /32 and go via router for anything else on the subnet
            nic.setIPv4Netmask("255.255.255.0");

            // Make it the default nic so that a default route is set up.
            nic.setDefaultNic(true);

            //nic.setIsolationUri(Networks.IsolationType..Mido.toUri(ip.getVlanTag()));
            nic.setBroadcastUri(network.getBroadcastUri());
            //nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
            nic.setFormat(Networks.AddressFormat.Ip4);
            nic.setReservationId(String.valueOf(ip.getVlanTag()));
            nic.setMacAddress(ip.getMacAddress());
        }

        nic.setIPv4Dns1(dc.getDns1());
        nic.setIPv4Dns2(dc.getDns2());
    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        s_logger.debug("updateNicProfile called with network: " + network + " profile: " + profile);

        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (profile != null) {
            profile.setIPv4Dns1(dc.getDns1());
            profile.setIPv4Dns2(dc.getDns2());
        }
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException, ConcurrentOperationException {

        if (nic == null) {
            nic = new NicProfile(Nic.ReservationStrategy.Create, null, null, null, null);
        }
        s_logger.debug("allocate called with network: " + network + " nic: " + nic + " vm: " + vm);
        DataCenter dc = _dcDao.findById(network.getDataCenterId());

        if (nic.getRequestedIPv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + nic);
        }

        getIp(nic, dc, vm, network);

        if (nic.getIPv4Address() == null) {
            nic.setReservationStrategy(Nic.ReservationStrategy.Start);
        } else if (vm.getVirtualMachine().getType() == VirtualMachine.Type.DomainRouter) {
            nic.setReservationStrategy(Nic.ReservationStrategy.Managed);
        } else {
            nic.setReservationStrategy(Nic.ReservationStrategy.Create);
        }

        nic.setBroadcastUri(generateBroadcastUri(network));

        return nic;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        s_logger.debug("reserve called with network: " + network + " nic: " + nic + " vm: " + vm);
        if (nic.getIPv4Address() == null) {
            getIp(nic, dest.getDataCenter(), vm, network);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        s_logger.debug("release called with nic: " + nic + " vm: " + vm);
        return true;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        s_logger.debug("implement called with network: " + network);
        long dcId = destination.getDataCenter().getId();

        //get physical network id
        long physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());

        NetworkVO implemented =
            new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), Network.State.Allocated,
                network.getDataCenterId(), physicalNetworkId, offering.getRedundantRouter());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        implemented.setBroadcastUri(generateBroadcastUri(network));

        return implemented;

    }

    @Override
    @DB
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {
        s_logger.debug("deallocate called with network: " + network + " nic: " + nic + " vm: " + vm);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("public network deallocate network: networkId: " + nic.getNetworkId() + ", ip: " + nic.getIPv4Address());
        }

        final IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIPv4Address());
        if (ip != null && nic.getReservationStrategy() != Nic.ReservationStrategy.Managed) {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    _ipAddrMgr.markIpAsUnavailable(ip.getId());
                    _ipAddressDao.unassignIpAddress(ip.getId());
                }
            });
        }
        nic.deallocate();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deallocated nic: " + nic);
        }
    }

    @Override
    public void shutdown(NetworkProfile network, NetworkOffering offering) {
        s_logger.debug("shutdown called with network: " + network);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        s_logger.debug("trash called with network: " + network);
        return true;
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        DataCenter dc = _dcDao.findById(networkProfile.getDataCenterId());
        networkProfile.setDns1(dc.getDns1());
        networkProfile.setDns2(dc.getDns2());
    }

    private URI generateBroadcastUri(Network network) {
        AccountVO acc = _accountDao.findById(network.getAccountId());
        String accountUUIDStr = acc.getUuid();
        String networkUUIDStr = String.valueOf(network.getId());
        return Networks.BroadcastDomainType.Mido.toUri(accountUUIDStr + "." + networkUUIDStr + ":" + networkUUIDStr);
    }

}
