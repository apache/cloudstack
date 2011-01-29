package com.cloud.network.guru;

import javax.ejb.Local;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.ovs.OvsNetworkManager;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=NetworkGuru.class)
public class ExternalGuestNetworkGuru extends GuestNetworkGuru {
	
	@Inject NetworkManager _networkMgr;
	@Inject NetworkDao _networkDao;
	@Inject DataCenterDao _zoneDao;
	@Inject ConfigurationDao _configDao;
	@Inject OvsNetworkManager _ovsNetworkMgr;
	@Inject OvsTunnelManager _tunnelMgr;
	
	@Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        
        if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
        	return null;
        }

        NetworkVO config = (NetworkVO) super.design(offering, plan, userSpecified, owner);
        if (config == null) {
            return null;
        } else if (_networkMgr.zoneIsConfiguredForExternalNetworking(plan.getDataCenterId())) {
        	config.setState(State.Allocated);
    	}     	        
        
        return config;
    }
	
	 @Override
	 public Network implement(Network config, NetworkOffering offering, DeployDestination dest, ReservationContext context) {
		 assert (config.getState() == State.Implementing) : "Why are we implementing " + config;
		 
		 if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
	        	return null;
	     }
	        
		 if (!_networkMgr.zoneIsConfiguredForExternalNetworking(config.getDataCenterId())) {
			 return super.implement(config, offering, dest, context);
		 }
		 
		 DataCenter zone = dest.getDataCenter();
		 NetworkVO implemented = new NetworkVO(config.getTrafficType(), config.getGuestType(), config.getMode(), config.getBroadcastDomainType(), config.getNetworkOfferingId(), config.getDataCenterId(), State.Allocated);
	     
	     // Get a vlan tag
	     int vlanTag;
	     if (config.getBroadcastUri() == null) {
	    	 String vnet = _dcDao.allocateVnet(zone.getId(), config.getAccountId(), context.getReservationId());
	    	 
	    	 try {
	    		 vlanTag = Integer.parseInt(vnet);
	    	 } catch (NumberFormatException e) {
	    		 throw new CloudRuntimeException("Obtained an invalid guest vlan tag. Exception: " + e.getMessage());
	    	 }

	    	 implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlanTag));	     	
	     } else {
	    	 vlanTag = Integer.parseInt(config.getBroadcastUri().getHost());
	    	 implemented.setBroadcastUri(config.getBroadcastUri());
	     }	          
	     
	     // Determine the offset from the lowest vlan tag
	     int offset = getVlanOffset(zone, vlanTag);
	     
	     // Determine the new gateway and CIDR
	     String[] oldCidr = config.getCidr().split("/");
	     String oldCidrAddress = oldCidr[0];	     
	     int cidrSize = getGloballyConfiguredCidrSize();	       
	     
	     // If the offset has more bits than there is room for, return null
	     long bitsInOffset = 32 - Integer.numberOfLeadingZeros(offset);
	     if (bitsInOffset > (cidrSize - 8)) {
	    	 throw new CloudRuntimeException("The offset " + offset + " needs " + bitsInOffset + " bits, but only have " + (cidrSize - 8) + " bits to work with.");
	     }
	     
	     long newCidrAddress = (NetUtils.ip2Long(oldCidrAddress) & 0xff000000) | (offset << (32 - cidrSize));
	     implemented.setGateway(NetUtils.long2Ip(newCidrAddress + 1));
	     implemented.setCidr(NetUtils.long2Ip(newCidrAddress) + "/" + cidrSize);
	     implemented.setState(State.Implemented);
	     
	     return implemented;	    
	 }

	 @Override
	 public NicProfile allocate(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
		 NicProfile profile = super.allocate(config, nic, vm);
		 
		 if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
	        	return null;
	     }
		 
		 if (_networkMgr.zoneIsConfiguredForExternalNetworking(config.getDataCenterId())) {
			 profile.setStrategy(ReservationStrategy.Start);
			 profile.setIp4Address(null);
			 profile.setGateway(null);
			 profile.setNetmask(null);
		 }

		 return profile;
	 }
	 
	 @Override
	 public void deallocate(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
		 super.deallocate(config, nic, vm);
		 
		 if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
	        	return;
	     }
		 
		 if (_networkMgr.zoneIsConfiguredForExternalNetworking(config.getDataCenterId())) {
			 nic.setIp4Address(null);
			 nic.setGateway(null);			
			 nic.setNetmask(null);
			 nic.setBroadcastUri(null);
			 nic.setIsolationUri(null);
		 }
	 }
	 
	 @Override
	 public void reserve(NicProfile nic, Network config, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
		 assert (nic.getReservationStrategy() == ReservationStrategy.Start) : "What can I do for nics that are not allocated at start? ";
		 if (_ovsNetworkMgr.isOvsNetworkEnabled()) {
	        	return;
	     }
		 DataCenter dc = _dcDao.findById(config.getDataCenterId());
		 if (_networkMgr.zoneIsConfiguredForExternalNetworking(config.getDataCenterId())) {
			 nic.setBroadcastUri(config.getBroadcastUri());
			 nic.setIsolationUri(config.getBroadcastUri());
			 nic.setDns1(dc.getDns1());
		     nic.setDns2(dc.getDns2());
		     nic.setNetmask(NetUtils.cidr2Netmask(config.getCidr()));
		     long cidrAddress = NetUtils.ip2Long(config.getCidr().split("/")[0]);
		     int cidrSize = getGloballyConfiguredCidrSize();	
		     nic.setGateway(config.getGateway());
		     
		     if (nic.getIp4Address() == null) {
		    	 nic.setIp4Address(acquireGuestIpAddress(config));
		     } else {
		    	 long ipMask = NetUtils.ip2Long(nic.getIp4Address()) & ~(0xffffffffffffffffl << (32 - cidrSize));
		    	 nic.setIp4Address(NetUtils.long2Ip(cidrAddress | ipMask));		    	 
		     }
		 } else {
			 super.reserve(nic, config, vm, dest, context);
		 }
				 
	 }
	 
	 @Override
	 public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {		 
		 if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
	        	return true;
	     }
		 
		 NetworkVO network  = _networkDao.findById(nic.getNetworkId());
		 if (network != null && _networkMgr.zoneIsConfiguredForExternalNetworking(network.getDataCenterId())) {
			 return true;
		 } else {
			 return super.release(nic, vm, reservationId);
		 }
	 }
	 
	 private int getGloballyConfiguredCidrSize() {
		 try {
	    	 String globalVlanBits = _configDao.getValue(Config.GuestVlanBits.key());
	    	 return 8 + Integer.parseInt(globalVlanBits);
	     } catch (Exception e) {
	    	 throw new CloudRuntimeException("Failed to read the globally configured VLAN bits size.");
	     }	   
	 }
	 
	 private int getVlanOffset(DataCenter zone, int vlanTag) {
	     if (zone.getVnet() == null) {
	    	 throw new CloudRuntimeException("Could not find vlan range for zone " + zone.getName() + ".");
	     }
		 
	     String vlanRange[] = zone.getVnet().split("-");
	     int lowestVlanTag = Integer.valueOf(vlanRange[0]);
	     return vlanTag - lowestVlanTag;
	 }
}
