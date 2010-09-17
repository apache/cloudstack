/**
 * 
 */
package com.cloud.network.configuration;

import javax.ejb.Local;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.dao.NetworkConfigurationDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;

@Local(value=NetworkGuru.class)
public class GuestNetworkGuru extends AdapterBase implements NetworkGuru {
    @Inject protected NetworkConfigurationDao _profileDao;
    @Inject protected DataCenterDao _dcDao;
    @Inject protected VlanDao _vlanDao; 
    
    protected GuestNetworkGuru() {
        super();
    } 
    
    @Override
    public NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration userSpecified, Account owner) {
        if (offering.getTrafficType() != TrafficType.Guest) {
            return null;
        }
        
        GuestIpType ipType = offering.getGuestIpType();
        BroadcastDomainType broadcastType = null;
        Mode mode = null;
        if (ipType == GuestIpType.Virtualized) {
            mode = Mode.Dhcp;
            broadcastType = BroadcastDomainType.Vlan;
        } else {
            broadcastType = BroadcastDomainType.Native;
            mode = Mode.Dhcp;
        }
        
        NetworkConfigurationVO profile = new NetworkConfigurationVO(offering.getTrafficType(), mode, broadcastType, offering.getId(), plan.getDataCenterId());
        DataCenterVO dc = _dcDao.findById(plan.getDataCenterId());
        
        return profile;
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination) {
        // TODO Auto-generated method stub
        return null;
    }

}
