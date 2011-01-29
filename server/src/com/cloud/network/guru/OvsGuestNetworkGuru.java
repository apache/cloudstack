package com.cloud.network.guru;

import javax.ejb.Local;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.ovs.OvsNetworkManager;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Inject;
import com.cloud.vm.ReservationContext;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Network.State;

@Local(value=NetworkGuru.class)
public class OvsGuestNetworkGuru extends GuestNetworkGuru {
	@Inject OvsNetworkManager _ovsNetworkMgr;
	@Inject NetworkManager _externalNetworkManager;
	@Inject OvsTunnelManager _ovsTunnelMgr;
	
	@Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
      
		if (!_ovsNetworkMgr.isOvsNetworkEnabled() && !_ovsTunnelMgr.isOvsTunnelEnabled()) {
			return null;
		}
		
        NetworkVO config = (NetworkVO) super.design(offering, plan, userSpecified, owner); 
        if (config == null) {
        	return null;
        }
        
        config.setBroadcastDomainType(BroadcastDomainType.Vswitch);
        
        return config;
	}
	
	@Override
	 public Network implement(Network config, NetworkOffering offering, DeployDestination dest, ReservationContext context) {
		 assert (config.getState() == State.Implementing) : "Why are we implementing " + config;
		 if (!_ovsNetworkMgr.isOvsNetworkEnabled()&& !_ovsTunnelMgr.isOvsTunnelEnabled()) {
			 return null;
		 }
		
		 NetworkVO implemented = (NetworkVO)super.implement(config, offering, dest, context);		 
		 
		 String uri = null;
		 if (_ovsNetworkMgr.isOvsNetworkEnabled()) {
		     uri = "vlan";
		 } else if (_ovsTunnelMgr.isOvsTunnelEnabled()) {
		     uri = Long.toString(config.getAccountId());
		 }
		 
		 implemented.setBroadcastUri(BroadcastDomainType.Vswitch.toUri(uri));
         return implemented;
	}
	
}
