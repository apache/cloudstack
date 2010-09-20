/**
 * 
 */
package com.cloud.network.configuration;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NetworkConcierge;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;

@Local(value={NetworkGuru.class, NetworkConcierge.class})
public class PublicNetworkGuru extends AdapterBase implements NetworkGuru, NetworkConcierge {
    private static final Logger s_logger = Logger.getLogger(PublicNetworkGuru.class);
    
    @Inject DataCenterDao _dcDao;
    @Inject VlanDao _vlanDao;

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
    public String getUniqueName() {
        return getName();
    }

    @Override
    public NicProfile allocate(VirtualMachine vm, NetworkConfiguration config, NicProfile nic) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        if (config.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        if (nic != null) {
            throw new CloudRuntimeException("Unsupported nic settings");
        }
        
        return new NicProfile(null, null, null);
    }

    @Override
    public boolean create(Nic nic) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        return true;
    }

    @Override
    public String reserve(VirtualMachine vm, NicProfile ch, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        long dcId = dest.getDataCenter().getId();
        long podId = dest.getPod().getId();

        Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(dcId, vm.getAccountId(), vm.getDomainId(), VlanType.VirtualNetwork, true);
        if (ipAndVlan == null) {
            throw new InsufficientVirtualNetworkCapcityException("Unable to get public ip address in " + dcId);
        }
        VlanVO vlan = ipAndVlan.second();
        ch.setIp4Address(ipAndVlan.first());
        ch.setGateway(vlan.getVlanGateway());
        ch.setNetmask(vlan.getVlanNetmask());
        try {
            ch.setIsolationUril(new URI("vlan://" + vlan.getVlanId()));
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("URI Syntax: " + "vlan://" + vlan.getVlanId(), e);
        }
        ch.setBroadcastType(BroadcastDomainType.Vlan);
        
        return Long.toString(vlan.getId());
    }

    @Override
    public boolean release(String uniqueName, String uniqueId) {
        return _vlanDao.release(Long.parseLong(uniqueId));
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination) {
        return config;
    }
}
