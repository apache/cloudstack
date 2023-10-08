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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.network.vpc.VpcVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxDhcpRelayConfigCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

public class NsxGuestNetworkGuru extends GuestNetworkGuru implements NetworkMigrationResponder  {
    private static final Logger LOGGER = Logger.getLogger(NsxGuestNetworkGuru.class);

    @Inject
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Inject
    NsxControllerUtils nsxControllerUtils;
    @Inject
    DataCenterDao zoneDao;
    @Inject
    AccountDao accountDao;

    public NsxGuestNetworkGuru() {
        super();
        _isolationMethods = new PhysicalNetwork.IsolationMethod[] {new PhysicalNetwork.IsolationMethod("NSX")};
    }

    @Override
    public boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType,
                             PhysicalNetwork physicalNetwork) {
        return networkType == DataCenter.NetworkType.Advanced && isMyTrafficType(offering.getTrafficType())
                && isMyIsolationMethod(physicalNetwork) && networkOfferingServiceMapDao.isProviderForNetworkOffering(
                offering.getId(), Network.Provider.Nsx);
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, String name, Long vpcId, Account owner) {
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            LOGGER.debug("Refusing to design this network");
            return null;
        }

        NetworkVO network = (NetworkVO) super.design(offering, plan, userSpecified, name, vpcId, owner);
        network.setBroadcastDomainType(Networks.BroadcastDomainType.NSX);
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

        network.setBroadcastDomainType(Networks.BroadcastDomainType.NSX);
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
        implemented.setBroadcastUri(Networks.BroadcastDomainType.NSX.toUri("nsx"));
        try {
            long zoneId = network.getDataCenterId();
            DataCenter zone = zoneDao.findById(zoneId);
            if (isNull(zone)) {
                throw new CloudRuntimeException(String.format("Failed to find zone with id: %s", zoneId));
            }
            createNsxSegment(implemented, zone);
        } catch (Exception ex) {
            throw new CloudRuntimeException("unable to create NSX network " + network.getUuid() + "due to: " + ex.getMessage());
        }
        return network;
    }

    @Override
    @DB
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile vm) {
        // Do nothing
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest,
                             ReservationContext context) {
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
        implemented.setBroadcastUri(Networks.BroadcastDomainType.NSX.toUri("nsx"));
//        try {
//            long zoneId = network.getDataCenterId();
//            DataCenter zone = zoneDao.findById(zoneId);
//            if (isNull(zone)) {
//                throw new CloudRuntimeException(String.format("Failed to find zone with id: %s", zoneId));
//            }
//            createNsxSegment(implemented, zone);
//        } catch (Exception ex) {
//            throw new CloudRuntimeException("unable to create NSX network " + network.getUuid() + "due to: " + ex.getMessage());
//        }
        return implemented;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        NicProfile nicProfile = super.allocate(network, nic, vm);

        if (vm.getType() == VirtualMachine.Type.DomainRouter) {
            final DataCenter zone = _dcDao.findById(network.getDataCenterId());
            long zoneId = network.getDataCenterId();
            if (Objects.isNull(zone)) {
                String msg = String.format("Unable to find zone with id: %s", zoneId);
                LOGGER.error(msg);
                throw new CloudRuntimeException(msg);
            }
            Account account = accountDao.findById(network.getAccountId());
            if (Objects.isNull(account)) {
                String msg = String.format("Unable to find account with id: %s", network.getAccountId());
                LOGGER.error(msg);
                throw new CloudRuntimeException(msg);
            }
            VpcVO vpc = _vpcDao.findById(network.getVpcId());
            if (Objects.isNull(vpc)) {
                String msg = String.format("Unable to find VPC with id: %s", network.getVpcId());
                LOGGER.error(msg);
                throw new CloudRuntimeException(msg);
            }

            // Create the DHCP relay config for the segment
            String iPv4Address = nicProfile.getIPv4Address();
            List<String> addresses = List.of(iPv4Address);
            CreateNsxDhcpRelayConfigCommand command = new CreateNsxDhcpRelayConfigCommand(zone.getName(), zone.getId(),
                    account.getAccountName(), network.getAccountId(),
                    vpc.getName(), network.getName(), addresses);
            NsxAnswer answer = nsxControllerUtils.sendNsxCommand(command, zone.getId());
            if (!answer.getResult()) {
                String msg = String.format("Error creating DHCP relay config for network %s and nic %s: %s", network.getName(), nic.getName(), answer.getDetails());
                LOGGER.error(msg);
                throw new CloudRuntimeException(msg);
            }
        }
        return nicProfile;
    }

    @Override
    public void reserve(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
                        final DeployDestination dest, final ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        // Do nothing
    }

    @Override
    public boolean release(final NicProfile nic, final VirtualMachineProfile vm, final String reservationId) {
        return true;
    }

    @Override
    public void shutdown(final NetworkProfile profile, final NetworkOffering offering) {
        // Do nothing
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        return true;
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

    private void createNsxSegment(NetworkVO networkVO, DataCenter zone) {
            String vpcName = null;
        if (nonNull(networkVO.getVpcId())) {
            VpcVO vpc = _vpcDao.findById(networkVO.getVpcId());
            if (isNull(vpc)) {
                throw new CloudRuntimeException(String.format("Failed to find VPC network with id: %s", networkVO.getVpcId()));
            }
            vpcName = vpc.getName();
        }
        Account account = accountDao.findById(networkVO.getAccountId());
        if (isNull(account)) {
            throw new CloudRuntimeException(String.format("Unable to find account with id: %s", networkVO.getAccountId()));
        }
        CreateNsxSegmentCommand command = new CreateNsxSegmentCommand(zone.getName(), zone.getId(),
                account.getAccountName(), networkVO.getAccountId(), vpcName, networkVO);
        NsxAnswer answer = nsxControllerUtils.sendNsxCommand(command, zone.getId());
        if (!answer.getResult()) {
            throw new CloudRuntimeException("can not create NSX network");
        }
    }
}
