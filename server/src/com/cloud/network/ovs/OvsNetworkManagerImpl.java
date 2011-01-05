package com.cloud.network.ovs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.ovs.dao.VlanMappingDao;
import com.cloud.network.ovs.dao.VlanMappingDirtyDao;
import com.cloud.network.ovs.dao.VlanMappingVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

@Local(value={OvsNetworkManager.class})
public class OvsNetworkManagerImpl implements OvsNetworkManager {
	private static final Logger s_logger = Logger.getLogger(OvsNetworkManagerImpl.class);
	@Inject ConfigurationDao _configDao;
	@Inject VlanMappingDao _vlanMappingDao;
	@Inject UserVmDao _userVmDao;
	@Inject HostDao _hostDao;
	@Inject AgentManager _agentMgr;
	@Inject NicDao _nicDao;
	@Inject NetworkDao _networkDao;
	@Inject VlanMappingDirtyDao _vlanMappingDirtyDao;
	@Inject DomainRouterDao _routerDao;
	String _name;
	boolean _isEnabled;
	
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		_name = name;
		_isEnabled = _configDao.getValue(Config.OvsNetwork.key()).equalsIgnoreCase("true") ? true : false;
		
		return true;
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return _name;
	}

	@Override
	public boolean isOvsNetworkEnabled() {
		// TODO Auto-generated method stub
		return _isEnabled;
	}

	@Override
	public long askVlanId(long accountId, long hostId) {
		assert _isEnabled : "Who call me ??? while OvsNetwokr is not enabled!!!";
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		
		List<VlanMappingVO> mappings = _vlanMappingDao.listByAccountIdAndHostId(accountId, hostId);
		long vlan = 0;
		
		if (mappings.size() !=0) {
			assert mappings.size() == 1 : "We should only have one vlan for an account on a host";
			txn.commit();
			vlan = mappings.get(0).getVlan();
			s_logger.debug("Already has an Vlan " + vlan + " on host " + hostId
					+ " for account " + accountId + ", use it!");
			return vlan;
		}
		
		mappings = _vlanMappingDao.listByHostId(hostId);
		if (mappings.size() > 0) {
			ArrayList<Long> vlans = new ArrayList<Long>();
			for (VlanMappingVO vo : mappings) {
				vlans.add(new Long(vo.getVlan()));
			}
			
			// Find first available vlan
			int i;
			for (i=0; i<4096; i++) {
				if (!vlans.contains(new Long(i))) {
					vlan = i;
					break;
				}
			}
			assert i!=4096 : "Terrible, vlan exhausted on this server!!!";
		}
		
		VlanMappingVO newVlan = new VlanMappingVO(accountId, hostId, vlan);
		_vlanMappingDao.persist(newVlan);
		_vlanMappingDirtyDao.markDirty(accountId);
		txn.commit();
		return 0;
	}

	@Override
	public String getVlanMapping(long accountId) {
		assert _isEnabled : "Who call me ??? while OvsNetwokr is not enabled!!!";
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		
		List<VlanMappingVO> ours = _vlanMappingDao.listByAccountId(accountId);
		txn.commit();
		
		ArrayList<Long>vlans = new ArrayList<Long>();
		for (VlanMappingVO vo : ours) {
			vlans.add(new Long(vo.getVlan()));
		}
		
		StringBuffer buf = new StringBuffer();
		for (Long i : vlans) {
			buf.append("/");
			buf.append(i.toString());
			buf.append("/");
		}
		return buf.toString();
	}

	@Override
	public void CheckAndCreateTunnel(Commands cmds, VirtualMachineProfile<UserVmVO> profile,
			DeployDestination dest) {
		if (!_isEnabled) {
			return;
		}
		
		UserVmVO userVm = profile.getVirtualMachine();
		if (userVm.getType() != VirtualMachine.Type.User) {
			return;
		}
		
		long hostId = dest.getHost().getId();
		long accountId = userVm.getAccountId();
		List<UserVmVO> vms = _userVmDao.listByAccountIdAndHostId(accountId, hostId);
		if (vms.size() != 0) {
			s_logger.debug("Already has GRE tunnel for account " + accountId
					+ " for host " + hostId);
			return;
		}
		
		vms = _userVmDao.listByAccountId(accountId);
		List<Long>remoteHostIds = new ArrayList<Long>();
		for (UserVmVO v : vms) {
			Long rh = v.getHostId();
			if (rh == null || rh.longValue() == hostId) {
				continue;
			}
			
			if (!remoteHostIds.contains(rh)) {
				remoteHostIds.add(rh);
			}
		}
		
		try {
			String myIp = dest.getHost().getPrivateIpAddress();
			for (Long i : remoteHostIds) {
				HostVO rHost = _hostDao.findById(i.longValue());
				cmds.addCommand(
						0, new OvsCreateGreTunnelCommand(rHost.getPrivateIpAddress(), "1"));
				_agentMgr.send(i.longValue(), new OvsCreateGreTunnelCommand(myIp, "1"));
				s_logger.debug("Ask host " + i.longValue() + " to create gre tunnel to " + hostId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	private String parseVlanAndMapping(String uri) {
		String sub = uri.substring(BroadcastDomainType.Vswitch.scheme().length() + "://".length() - 1);
		return sub;
	}
	
	@Override
	public void applyDefaultFlow(Commands cmds,
			VirtualMachineProfile<UserVmVO> profile, DeployDestination dest) {
		if (!_isEnabled) {
			return;
		}
		
		UserVmVO userVm = profile.getVirtualMachine();
		VirtualMachine.Type vmType = userVm.getType();
		if (vmType != VirtualMachine.Type.User
				&& vmType != VirtualMachine.Type.DomainRouter) {
			return;
		}
		
		List<NicVO> nics = _nicDao.listBy(userVm.getId());
		if (nics.size() == 0)
			return;
		
		NicVO nic = null;
		if (vmType == VirtualMachine.Type.DomainRouter) {
			for (NicVO n : nics) {
				NetworkVO network = _networkDao.findById(n.getNetworkId());
				if (network.getTrafficType() == TrafficType.Guest) {
					nic = n;
					break;
				}
			}
		} else {
			nic = nics.get(0);
		}
		
		assert nic!=null : "Why there is no guest network nic???";
		String vlans = parseVlanAndMapping(nic.getBroadcastUri().toASCIIString());
		cmds.addCommand(new OvsSetTagAndFlowCommand(userVm.getName(), vlans));
	}

	@Override
	public void CheckAndUpdateDhcpFlow(Network nw) {
		if (!_isEnabled) {
			return;
		}
		
		DomainRouterVO router = _routerDao.findByNetworkConfiguration(nw.getId());
		if (router == null) {
			return;
		}
		
		long accountId = nw.getAccountId();
		if (!_vlanMappingDirtyDao.isDirty(accountId)) {
			return;
		}
		
		try {
			String vlans = getVlanMapping(accountId);
			_agentMgr.send(router.getHostId(), new OvsSetTagAndFlowCommand(
					router.getName(), vlans));
			s_logger.debug("ask router " + router.getName() + " on host "
					+ router.getHostId() + " update vlan map to " + vlans);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void handleVmStarted(UserVm userVm) {
		scheduleFlowUpdateToHosts(affectedVms, true, null);
	}
	
	protected void handleVmStopped(UserVm userVm) {
		scheduleFlowUpdateToHosts(affectedVms, true, null);
	}
	
	@Override
	public void handleVmStateTransition(UserVm userVm, State vmState) {
		if (!_isEnabled) {
			return;
		}
		
		switch (vmState) {
		case Creating:
		case Destroyed:
		case Error:
		case Migrating:
		case Expunging:
		case Starting:
		case Unknown:
			return;
		case Running:
			handleVmStarted(userVm);
			break;
		case Stopping:
		case Stopped:
			handleVmStopped(userVm);
			break;
		}
		
	}

}
