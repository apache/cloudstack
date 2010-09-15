/**
 * 
 */
package com.cloud.network.profiler;

import java.util.Map;

import javax.ejb.Local;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.NetworkProfiler;
import com.cloud.network.dao.NetworkConfigurationDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;

@Local(value=NetworkProfiler.class)
public class GuestNetworkProfiler extends AdapterBase implements NetworkProfiler {
    @Inject protected NetworkConfigurationDao _profileDao;
    @Inject protected DataCenterDao _dcDao;
    @Inject protected VlanDao _vlanDao; 
    
    protected GuestNetworkProfiler() {
        super();
    } 
    
    @Override
    public NetworkConfiguration convert(NetworkOffering offering, DeploymentPlan plan, Map<String, String> params, Account owner) {
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

}
