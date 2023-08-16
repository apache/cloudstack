package org.apache.cloudstack.service;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.Network;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.log4j.Logger;

import javax.inject.Inject;

public class NsxGuestNetworkGuru extends GuestNetworkGuru implements NetworkMigrationResponder  {
    private static final Logger LOGGER = Logger.getLogger(NsxGuestNetworkGuru.class);

    @Inject
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;

    private static final Networks.TrafficType[] TrafficTypes = {Networks.TrafficType.Guest};

    public NsxGuestNetworkGuru() {
        super();
        _isolationMethods = new PhysicalNetwork.IsolationMethod[] {new PhysicalNetwork.IsolationMethod("NSX")};
    }


    @Override
    public boolean prepareMigration(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) {
        return false;
    }

    @Override
    public void rollbackMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {

    }

    @Override
    public void commitMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {

    }

    @Override
    public boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType,
                             PhysicalNetwork physicalNetwork) {
        return networkType == DataCenter.NetworkType.Advanced && isMyTrafficType(offering.getTrafficType())
                && isMyIsolationMethod(physicalNetwork) && networkOfferingServiceMapDao.isProviderForNetworkOffering(
                offering.getId(), Network.Provider.Tungsten);
    }
}
