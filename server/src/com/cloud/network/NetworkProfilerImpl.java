/**
 * 
 */
package com.cloud.network;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.exception.ConflictingNetworkSettingsException;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.dao.NetworkProfileDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

@Local(value=NetworkProfiler.class)
public class NetworkProfilerImpl extends AdapterBase implements NetworkProfiler {
    @Inject protected NetworkProfileDao _profileDao;
    
    protected NetworkProfilerImpl() {
        super();
    }
    
    @Override
    public NetworkProfile convert(NetworkOffering offering, Map<String, String> params, Account owner) {
        List<NetworkProfileVO> profiles = _profileDao.listBy(owner.getId(), offering.getId());
        
        for (NetworkProfileVO profile : profiles) {
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
            throw new CloudRuntimeException("Unable to convert " + ipType);
        }
        
        return new NetworkProfileVO(owner.getId(), offering.getTrafficType(), mode, broadcastType, offering.getId());
    }

    @Override
    public List<? extends NetworkProfile> convert(Collection<? extends NetworkOffering> networkOfferings, Account owner) {
        List<NetworkProfileVO> profiles = _profileDao.listBy(owner.getId());
        for (NetworkOffering offering : networkOfferings) {
        }
        return null;
    }

    @Override
    public boolean check(VirtualMachine vm, ServiceOffering serviceOffering, Collection<? extends NetworkProfile> networkProfiles) throws ConflictingNetworkSettingsException {
        return false;
    }

}
