/**
 * 
 */
package com.cloud.network;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConflictingNetworkSettingsException;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.dao.NetworkConfigurationDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VirtualMachine;

@Local(value=NetworkProfiler.class)
public class NetworkProfilerImpl extends AdapterBase implements NetworkProfiler {
    @Inject protected NetworkConfigurationDao _profileDao;
    @Inject protected DataCenterDao _dcDao;
    
    protected NetworkProfilerImpl() {
        super();
    }
    
    @Override
    public NetworkConfiguration convert(NetworkOffering offering, DeploymentPlan plan, Map<String, String> params, Account owner) {
        List<NetworkConfigurationVO> profiles = _profileDao.listBy(owner.getId(), offering.getId());
        
        for (NetworkConfigurationVO profile : profiles) {
            // FIXME: We should do more comparisons such as if the specific cidr matches.
            return profile;
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
        
        NetworkConfigurationVO profile = new NetworkConfigurationVO(offering.getTrafficType(), mode, broadcastType, offering.getId());
        DataCenterVO dc = _dcDao.findById(plan.getDataCenterId());
        return profile;
    }

    @Override
    public List<? extends NetworkConfiguration> convert(Collection<? extends NetworkOffering> networkOfferings, Account owner) {
        List<NetworkConfigurationVO> profiles = _profileDao.listBy(owner.getId());
        for (NetworkOffering offering : networkOfferings) {
        }
        return null;
    }

    @Override
    public boolean check(VirtualMachine vm, ServiceOffering serviceOffering, Collection<? extends NetworkConfiguration> networkProfiles) throws ConflictingNetworkSettingsException {
        return false;
    }

}
