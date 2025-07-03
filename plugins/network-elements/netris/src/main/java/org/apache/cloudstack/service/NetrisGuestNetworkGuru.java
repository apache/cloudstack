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
package org.apache.cloudstack.service;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.DomainVO;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.Account;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NetrisGuestNetworkGuru  extends GuestNetworkGuru implements NetworkMigrationResponder {

    @Inject
    private NetrisService netrisService;
    @Inject
    NetworkModel networkModel;
    @Inject
    NetworkDetailsDao networkDetailsDao;

    public NetrisGuestNetworkGuru() {
        super();
        _isolationMethods = new PhysicalNetwork.IsolationMethod[] {new PhysicalNetwork.IsolationMethod("Netris")};
    }

    @Override
    public boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType,
                             PhysicalNetwork physicalNetwork) {
        return networkType == DataCenter.NetworkType.Advanced && isMyTrafficType(offering.getTrafficType())
                && isMyIsolationMethod(physicalNetwork) && (NetworkOffering.NetworkMode.ROUTED.equals(offering.getNetworkMode())
                || (networkOfferingServiceMapDao.isProviderForNetworkOffering(
                offering.getId(), Network.Provider.Netris) && NetworkOffering.NetworkMode.NATTED.equals(offering.getNetworkMode())));
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, String name, Long vpcId, Account owner) {
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            logger.debug("Refusing to design this network");
            return null;
        }

        NetworkVO network = (NetworkVO) super.design(offering, plan, userSpecified, name, vpcId, owner);
        if (network == null) {
            return null;
        }
        network.setBroadcastDomainType(Networks.BroadcastDomainType.Netris);

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

        network.setBroadcastDomainType(Networks.BroadcastDomainType.Netris);
        network.setState(Network.State.Allocated);

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(),
                network.getBroadcastDomainType(), network.getNetworkOfferingId(), Network.State.Implemented,
                network.getDataCenterId(), network.getPhysicalNetworkId(), offering.isRedundantRouter());
        implemented.setAccountId(owner.getAccountId());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        if (vpcId != null) {
            implemented.setVpcId(vpcId);
        }

        if (name != null) {
            implemented.setName(name);
        }
        implemented.setBroadcastUri(Networks.BroadcastDomainType.Netris.toUri("netris"));

        return network;
    }

    @Override
    public void setup(Network network, long networkId) {
        try {
            NetworkVO designedNetwork  = _networkDao.findById(networkId);
            long zoneId = network.getDataCenterId();
            DataCenter zone = _dcDao.findById(zoneId);
            if (isNull(zone)) {
                throw new CloudRuntimeException(String.format("Failed to find zone with id: %s", zoneId));
            }
            try {
                allocateVnet(network, designedNetwork, network.getDataCenterId(), network.getPhysicalNetworkId(), network.getReservationId());
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("Failed to allocate VXLAN for Netris guest Network %s", network.getName()));
            }
            _networkDao.update(designedNetwork.getId(), designedNetwork);
            createNetrisVnet(designedNetwork, zone);
        } catch (Exception ex) {
            throw new CloudRuntimeException("unable to create Netris network " + network.getUuid() + "due to: " + ex.getMessage());
        }
    }

    @Override
    @DB
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile vm) {
        // Do nothing
    }

    @Override
    public boolean update(Network network, String prevNetworkName) {
        Long vpcId = network.getVpcId();
        String vpcName = null;
        if (Objects.nonNull(vpcId)) {
            VpcVO vpc = _vpcDao.findById(vpcId);
            if (Objects.nonNull(vpc)) {
                vpcName = vpc.getName();
            }
        }
        return netrisService.updateVnetResource(network.getDataCenterId(), network.getAccountId(), network.getDomainId(),
                vpcName, vpcId, network.getName(), network.getId(), prevNetworkName);
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest,
                             ReservationContext context) throws InsufficientVirtualNetworkCapacityException {
        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(),
                network.getBroadcastDomainType(), network.getNetworkOfferingId(), Network.State.Implemented,
                network.getDataCenterId(), network.getPhysicalNetworkId(), offering.isRedundantRouter());
        implemented.setAccountId(network.getAccountId());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        if (network.getVpcId() != null) {
            implemented.setVpcId(network.getVpcId());
        }

        if (network.getName() != null) {
            implemented.setName(network.getName());
        }
        allocateVnet(network, implemented, network.getDataCenterId(), network.getPhysicalNetworkId(), network.getReservationId());
        return implemented;
    }

    @Override
    protected void allocateVnet(Network network, NetworkVO implemented, long dcId, long physicalNetworkId, String reservationId)
            throws InsufficientVirtualNetworkCapacityException {
        String vnet = null;
        Long networkId = implemented.getId() > 0 ? implemented.getId() : network.getId();
        if (network.getBroadcastUri() == null) {
            NetworkDetailVO netrisVnetDetail = networkDetailsDao.findDetail(networkId, ApiConstants.NETRIS_VXLAN_ID);
            if (nonNull(netrisVnetDetail)) {
                vnet = netrisVnetDetail.getValue();
            } else {
                vnet = _dcDao.allocateVnet(dcId, physicalNetworkId, network.getAccountId(), reservationId, UseSystemGuestVlans.valueIn(network.getAccountId()));
                if (vnet == null) {
                    throw new InsufficientVirtualNetworkCapacityException("Unable to allocate vnet as a " + "part of network " + network + " implement ", DataCenter.class,
                            dcId);
                }
                networkDetailsDao.addDetail(networkId, ApiConstants.NETRIS_VXLAN_ID, vnet, true);
            }
            implemented.setBroadcastUri(Networks.BroadcastDomainType.Netris.toUri(vnet));
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VXLAN_ASSIGN,
                    "Assigned Zone vNet: " + vnet + " Network Id: " + networkId, networkId, ApiCommandResourceType.Network.toString(), 0);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        NicProfile nicProfile = super.allocate(network, nic, vm);
        if (vm.getType() != VirtualMachine.Type.DomainRouter) {
            return nicProfile;
        }

        final DataCenter zone = _dcDao.findById(network.getDataCenterId());
        long zoneId = network.getDataCenterId();
        if (Objects.isNull(zone)) {
            String msg = String.format("Unable to find zone with id: %s", zoneId);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        Account account = accountDao.findById(network.getAccountId());
        if (Objects.isNull(account)) {
            String msg = String.format("Unable to find account with id: %s", network.getAccountId());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        VpcVO vpc = _vpcDao.findById(network.getVpcId());
        if (Objects.isNull(vpc)) {
            String msg = String.format("Unable to find VPC with id: %s, allocating for network %s", network.getVpcId(), network.getName());
            logger.debug(msg);
        }

        DomainVO domain = domainDao.findById(account.getDomainId());
        if (Objects.isNull(domain)) {
            String msg = String.format("Unable to find domain with id: %s", account.getDomainId());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        NetworkOfferingVO networkOfferingVO = networkOfferingDao.findById(network.getNetworkOfferingId());

        if (isNull(network.getVpcId()) && networkOfferingVO.getNetworkMode().equals(NetworkOffering.NetworkMode.NATTED)) {
            // Netris Natted mode
            long domainId = domain.getId();
            long accountId = account.getId();
            long dataCenterId = zone.getId();
            long resourceId = network.getId();
            PublicIpAddress ipAddress = networkModel.getSourceNatIpAddressForGuestNetwork(account, network);
            String snatIP = ipAddress.getAddress().addr();
            boolean result = netrisService.createSnatRule(dataCenterId, accountId, domainId, vpc.getName(), vpc.getId(), network.getName(), resourceId, nonNull(network.getVpcId()), vpc.getCidr(), snatIP);
            if (!result) {
                String msg = String.format("Could not create Netris Nat Rule for IP %s", snatIP);
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
        }

        return nicProfile;
    }

    @Override
    public boolean prepareMigration(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) {
        return false;
    }

    @Override
    public void rollbackMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {
        // Do nothing
    }

    @Override
    public void commitMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {
        // Do nothing
    }

    public void createNetrisVnet(NetworkVO networkVO, DataCenter zone) {
        Account account = accountDao.findById(networkVO.getAccountId());
        if (isNull(account)) {
            throw new CloudRuntimeException(String.format("Unable to find account with id: %s", networkVO.getAccountId()));
        }
        DomainVO domain = domainDao.findById(account.getDomainId());
        if (Objects.isNull(domain)) {
            String msg = String.format("Unable to find domain with id: %s", account.getDomainId());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        String vpcName = null;
        Long vpcId = null;
        Boolean globalRouting = null;
        long networkOfferingId = networkVO.getNetworkOfferingId();
        NetworkOfferingVO networkOfferingVO = networkOfferingDao.findById(networkOfferingId);
        if (NetworkOffering.NetworkMode.ROUTED.equals(networkOfferingVO.getNetworkMode())) {
            globalRouting = true;
        }
        if (nonNull(networkVO.getVpcId())) {
            VpcVO vpc = _vpcDao.findById(networkVO.getVpcId());
            if (isNull(vpc)) {
                throw new CloudRuntimeException(String.format("Failed to find VPC network with id: %s", networkVO.getVpcId()));
            }
            vpcName = vpc.getName();
            vpcId = vpc.getId();
        } else {
            logger.debug(String.format("Creating IPAM Allocation before creating IPAM Subnet", networkVO.getName()));
            boolean isSourceNatSupported = !NetworkOffering.NetworkMode.ROUTED.equals(networkOfferingVO.getNetworkMode()) &&
                    networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(networkVO.getNetworkOfferingId(), Network.Service.SourceNat);
            boolean result = netrisService.createVpcResource(zone.getId(), networkVO.getAccountId(), networkVO.getDomainId(),
                    networkVO.getId(), networkVO.getName(), isSourceNatSupported, networkVO.getCidr(), false);
            if (!result) {
                String msg = String.format("Error creating Netris VPC for the network: %s", networkVO.getName());
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
        }
        boolean result = netrisService.createVnetResource(zone.getId(), account.getId(), domain.getId(), vpcName, vpcId,
                networkVO.getName(), networkVO.getId(), networkVO.getCidr(), globalRouting);
        if (!result) {
            throw new CloudRuntimeException("Failed to create Netris vNet resource for network: " + networkVO.getName());
        }
    }
}
