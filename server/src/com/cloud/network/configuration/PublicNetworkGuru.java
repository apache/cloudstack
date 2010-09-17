/**
 * 
 */
package com.cloud.network.configuration;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.DomainVO;
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
            s_logger.debug("Unable to get public ip address (type=Virtual) for console proxy vm for data center  : " + dcId);
            ipAndVlan = _vlanDao.assignPodDirectAttachIpAddress(dcId, podId, Account.ACCOUNT_ID_SYSTEM, DomainVO.ROOT_DOMAIN);
            if (ipAndVlan == null)
                s_logger.debug("Unable to get public ip address (type=DirectAttach) for console proxy vm for data center  : " + dcId);
        }
        if (ipAndVlan != null) {
            VlanVO vlan = ipAndVlan.second();
            return null;
//            networkInfo net = new networkInfo(ipAndVlan.first(), vlan.getVlanNetmask(), vlan.getVlanGateway(), vlan.getId(), vlan.getVlanId());
  //          return net;
        }
            return null;
    }

    @Override
    public boolean release(String uniqueName, String uniqueId) {
        return false;
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination) {
        return config;
    }
}
