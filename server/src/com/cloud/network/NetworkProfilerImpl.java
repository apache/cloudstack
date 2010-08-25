/**
 * 
 */
package com.cloud.network;

import java.util.Collection;
import java.util.List;

import javax.ejb.Local;

import com.cloud.exception.ConflictingNetworkSettingsException;
import com.cloud.network.dao.NetworkProfileDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VirtualMachine;

@Local(value=NetworkProfiler.class)
public class NetworkProfilerImpl extends AdapterBase implements NetworkProfiler {
    @Inject protected NetworkProfileDao _profileDao;
    
    protected NetworkProfilerImpl() {
        super();
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
        // TODO Auto-generated method stub
        return false;
    }

}
