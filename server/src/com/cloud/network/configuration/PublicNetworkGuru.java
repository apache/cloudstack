/**
 * 
 */
package com.cloud.network.configuration;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network.AddressFormat;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.IsolationType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.dao.NetworkConfigurationDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;

@Local(value={NetworkGuru.class})
public class PublicNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(PublicNetworkGuru.class);
    
    @Inject DataCenterDao _dcDao;
    @Inject VlanDao _vlanDao;
    @Inject NetworkConfigurationDao _networkConfigDao;

    @Override
    public NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration config, Account owner) {
        if (offering.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        return new NetworkConfigurationVO(offering.getTrafficType(), Mode.Static, BroadcastDomainType.Vlan, offering.getId(), plan.getDataCenterId());
    }
    
    protected PublicNetworkGuru() {
        super();
    }

    @Override
    public NicProfile allocate(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        if (config.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        } else {
            nic.setStrategy(ReservationStrategy.Create);
        }
        
        String mac = _networkConfigDao.getNextAvailableMacAddress(config.getId());
        if (mac == null) {
            throw new InsufficientAddressCapacityException("Not enough mac addresses");
        }
        nic.setMacAddress(mac);
        
        return nic;
    }

    @Override
    public String reserve(NicProfile ch, NetworkConfiguration configuration, VirtualMachineProfile vm, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        if (ch.getReservationId() != null) {
            return ch.getReservationId();
        }
        
        DataCenter dc = dest.getDataCenter();
        long dcId = dc.getId();
        
        if (ch.getIp4Address() != null) {
            Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(dcId, vm.getVm().getAccountId(), vm.getVm().getDomainId(), VlanType.VirtualNetwork, true);
            if (ipAndVlan == null) {
                throw new InsufficientVirtualNetworkCapcityException("Unable to get public ip address in " + dcId);
            }
            VlanVO vlan = ipAndVlan.second();
            ch.setIp4Address(ipAndVlan.first());
            ch.setGateway(vlan.getVlanGateway());
            ch.setNetmask(vlan.getVlanNetmask());
            ch.setIsolationUri(IsolationType.Vlan.toUri(vlan.getVlanId()));
            ch.setBroadcastType(BroadcastDomainType.Vlan);
            ch.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlan.getVlanId()));
            ch.setFormat(AddressFormat.Ip4);
            ch.setReservationId(Long.toString(vlan.getId()));
        }
        ch.setDns1(dc.getDns1());
        ch.setDns2(dc.getDns2());
        
        return ch.getReservationId();
    }

    @Override
    public boolean release(String uniqueId) {
        return _vlanDao.releaseFromLockTable(Long.parseLong(uniqueId));
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination) {
        return config;
    }
    
    @Override
    public void destroy(NetworkConfiguration config, NetworkOffering offering) {
    }
}
