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
package org.apache.cloudstack.network.guru;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkProfile;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.network.element.SspElement;
import org.apache.cloudstack.network.element.SspManager;

import javax.inject.Inject;

/**
 * Stratosphere SDN Platform NetworkGuru
 */
public class SspGuestNetworkGuru extends GuestNetworkGuru implements NetworkMigrationResponder {

    @Inject
    SspManager _sspMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;

    public SspGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {new IsolationMethod("SSP")};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, NetworkType networkType, PhysicalNetwork physicalNetwork) {
        logger.trace("canHandle");

        String setting = null;
        if (physicalNetwork != null && physicalNetwork.getIsolationMethods().contains("SSP")) {
            // Be careful, PhysicalNetwork#getIsolationMethods() returns List<String>, not List<IsolationMethod>
            setting = "physicalnetwork setting";
        } else if (_ntwkOfferingSrvcDao.isProviderForNetworkOffering(offering.getId(), Network.Provider.getProvider(SspElement.s_SSP_NAME))) {
            setting = "network offering setting";
        }
        if (setting != null) {
            if (networkType != NetworkType.Advanced) {
                logger.info("SSP enebled by " + setting + " but not active because networkType was " + networkType);
            } else if (!isMyTrafficType(offering.getTrafficType())) {
                logger.info("SSP enabled by " + setting + " but not active because traffic type not Guest");
            } else if (offering.getGuestType() != Network.GuestType.Isolated) {
                logger.info("SSP works for network isolatation.");
            } else if (!_sspMgr.canHandle(physicalNetwork)) {
                logger.info("SSP manager not ready");
            } else {
                return true;
            }
        } else {
            logger.debug("SSP not configured to be active");
        }
        return false;
    }

    /* (non-Javadoc)
     * FYI: What is done in parent class is allocateVnet(vlan).
     * Effective return object members are: cidr, broadcastUri, gateway, mode, physicalNetworkId
     * The other members will be silently ignored.
     * This method is called at DeployVMCmd#execute (running phase) - NetworkManagerImpl#prepare
     * @see org.apache.cloudstack.network.guru.GuestNetworkGuru#implement(com.cloud.network.Network, com.cloud.offering.NetworkOffering, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
     */
    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        logger.trace("implement " + network.toString());
        super.implement(network, offering, dest, context);
        _sspMgr.createNetwork(network, offering, dest, context);
        return network;
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        logger.trace("shutdown " + profile.toString());
        _sspMgr.deleteNetwork(profile);
        super.shutdown(profile, offering);
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        super.reserve(nic, network, vm, dest, context);
        _sspMgr.createNicEnv(network, nic, dest, context);
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        Network network = _networkDao.findById(nic.getNetworkId());
        _sspMgr.deleteNicEnv(network, nic, new ReservationContextImpl(reservationId, null, null));
        return super.release(nic, vm, reservationId);
    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        super.updateNicProfile(profile, network);
    }

    @Override
    public boolean prepareMigration(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) {
        try {
            reserve(nic, network, vm, dest, context);
        } catch (InsufficientVirtualNetworkCapacityException e) {
            logger.error("prepareForMigration failed", e);
            return false;
        } catch (InsufficientAddressCapacityException e) {
            logger.error("prepareForMigration failed", e);
            return false;
        }
        return true;
    }

    @Override
    public void rollbackMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {
        release(nic, vm, dst.getReservationId());
    }

    @Override
    public void commitMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {
        release(nic, vm, src.getReservationId());
    }
}
