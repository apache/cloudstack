package com.cloud.network.guru;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.Pod;
import com.cloud.dc.StorageNetworkIpAddressVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkProfile;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.StorageNetworkManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.net.Ip4Address;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.Nic.ReservationStrategy;

@Local(value = NetworkGuru.class)
public class StorageNetworkGuru extends AdapterBase implements NetworkGuru {
	private static final Logger s_logger = Logger.getLogger(StorageNetworkGuru.class);
	@Inject StorageNetworkManager _sNwMgr;
	@Inject DataCenterDao _dcDao;
	
	protected StorageNetworkGuru() {
		super();
	}
	
	protected boolean canHandle(NetworkOffering offering) {
		if (offering.getTrafficType() == TrafficType.Storage && offering.isSystemOnly()) {
			return true;
		} else {
			s_logger.trace("It's not storage network offering, skip it.");
			return false;
		}
	}
	
	@Override
	public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
		if (!canHandle(offering)) {
			return null;
		}
		
		NetworkVO config = new NetworkVO(offering.getTrafficType(), Mode.Static, BroadcastDomainType.Native, offering.getId(), Network.State.Setup,
		        plan.getDataCenterId(), plan.getPhysicalNetworkId());
		return config;
	}

	@Override
	public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context)
	        throws InsufficientVirtualNetworkCapcityException {
		assert network.getTrafficType() == TrafficType.Storage : "Why are you sending this configuration to me " + network;
		return network;
	}

	@Override
	public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm)
	        throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException {
		assert network.getTrafficType() == TrafficType.Storage : "Well, I can't take care of this config now can I? " + network; 
		return new NicProfile(ReservationStrategy.Start, null, null, null, null);
	}

	@Override
	public Ip4Address acquireIp4Address(Network network, String requestedIp, String reservationId) throws InsufficientAddressCapacityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean releaseIp4Address(Network network, String reservationId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context)
	        throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException {
		Pod pod = dest.getPod();
		String ipAddress = null;
		Long macLong = null;
		Integer cidrSize = null;
		
		StorageNetworkIpAddressVO ip = _sNwMgr.acquireIpAddress(pod.getId());
		if (ip != null) {
			ipAddress = ip.getIpAddress();
			macLong = ip.getMac();
			cidrSize = ip.getCidrSize();
		} else {
			s_logger.debug("Can not get an ip from storage network ip range for pod " + pod.getId() + ", acquire one from managment ip range");
			/* Pick up an ip from management ip range if there is no available in storage ip range because of either user not added it or run out of */
			Pair<String, Long>ip1 = _dcDao.allocatePrivateIpAddress(dest.getDataCenter().getId(), pod.getId(), nic.getId(),
			        context.getReservationId());
			if (ip1 == null) {
				throw new InsufficientAddressCapacityException("Unable to get a storage network ip address", Pod.class, pod.getId());
			}
			ipAddress = ip1.first();
			macLong = ip1.second();
			cidrSize = pod.getCidrSize();
		}
		
		nic.setIp4Address(ipAddress);
		nic.setMacAddress(NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(macLong)));
		nic.setFormat(AddressFormat.Ip4);
		nic.setNetmask(NetUtils.getCidrNetmask(cidrSize));
		nic.setBroadcastType(BroadcastDomainType.Native);
        nic.setBroadcastUri(null);
        nic.setIsolationUri(null);
        s_logger.debug("Allocated a nic " + nic + " for " + vm);
	}

	@Override
	public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
		if (_sNwMgr.isAStorageIpAddress(nic.getIp4Address())) {
			_sNwMgr.releaseIpAddress(nic.getIp4Address());
			s_logger.debug("Release an storage ip " + nic.getIp4Address());
		} else {
			_dcDao.releasePrivateIpAddress(nic.getId(), nic.getReservationId());
			s_logger.debug("Release an storage ip that is from managment ip range " + nic.getIp4Address());
		}
		
		nic.deallocate();
		return true;
	}

	@Override
	public void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNicProfile(NicProfile profile, Network network) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown(NetworkProfile network, NetworkOffering offering) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean trash(Network network, NetworkOffering offering, Account owner) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateNetworkProfile(NetworkProfile networkProfile) {
		// TODO Auto-generated method stub

	}

}
