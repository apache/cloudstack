package com.cloud.network.ovs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.ovs.dao.OvsWorkDao;
import com.cloud.network.ovs.dao.OvsWorkVO;
import com.cloud.network.ovs.dao.OvsWorkVO.Step;
import com.cloud.network.ovs.dao.VlanMappingDao;
import com.cloud.network.ovs.dao.VlanMappingDirtyDao;
import com.cloud.network.ovs.dao.VlanMappingVO;
import com.cloud.network.ovs.dao.VmFlowLogDao;
import com.cloud.network.ovs.dao.VmFlowLogVO;
import com.cloud.server.ManagementServer;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

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
	@Inject OvsWorkDao _workDao;
	@Inject VmFlowLogDao _flowLogDao;
	@Inject UserVmDao _userVMDao;
	@Inject VMInstanceDao _instanceDao;
	@Inject AccountDao _accountDao;
	String _name;
	boolean _isEnabled;
	ScheduledExecutorService _executorPool;
    ScheduledExecutorService _cleanupExecutor;
    OvsListener _ovsListener;

	private long _serverId;
	private final long _timeBetweenCleanups = 30; //seconds
	
	public  class WorkerThread implements Runnable {
		@Override
		public void run() {
			work();
		}
		
		WorkerThread() {
			
		}
	}
	
	public  class CleanupThread implements Runnable {
		@Override
		public void run() {
			cleanupFinishedWork();
			cleanupUnfinishedWork();
		}

		CleanupThread() {
			
		}
	}
	
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		_name = name;
		_isEnabled = _configDao.getValue(Config.OvsNetwork.key()).equalsIgnoreCase("true") ? true : false;
		_serverId = ((ManagementServer)ComponentLocator.getComponent(ManagementServer.Name)).getId();
	    _executorPool = Executors.newScheduledThreadPool(10, new NamedThreadFactory("OVS"));
	    _cleanupExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("OVS-Cleanup"));
	    _ovsListener = new OvsListener(this, _workDao);
	    _agentMgr.registerForHostEvents(_ovsListener, true, true, true);
		
		return true;
	}

	@Override
	public boolean start() {
		if (_isEnabled) {
			_cleanupExecutor.scheduleAtFixedRate(new CleanupThread(), _timeBetweenCleanups, _timeBetweenCleanups, TimeUnit.SECONDS);
		}
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public boolean isOvsNetworkEnabled() {
		return _isEnabled;
	}

	public void cleanupFinishedWork() {
		Date before = new Date(System.currentTimeMillis() - 24*3600*1000l);
		int numDeleted = _workDao.deleteFinishedWork(before);
		if (numDeleted > 0) {
			s_logger.info("Ovs cleanup deleted " + numDeleted + " finished work items older than " + before.toString());
		}
		
	}
	

	private void cleanupUnfinishedWork() {
		Date before = new Date(System.currentTimeMillis() - 30*1000l);
		List<OvsWorkVO> unfinished = _workDao.findUnfinishedWork(before);
		if (unfinished.size() > 0) {
			s_logger.info("Ovscleanup found " + unfinished.size() + " unfinished work items older than " + before.toString());
			Set<Long> affectedVms = new HashSet<Long>();
			for (OvsWorkVO work: unfinished) {
				affectedVms.add(work.getInstanceId());
			}
			
			s_logger.info("Ovs cleanup re-schedule unfinished work");
			scheduleFlowUpdateToHosts(affectedVms, false, null);
		} else {
			s_logger.debug("Ovs cleanup found no unfinished work items older than " + before.toString());
		}
	}
	
	//TODO: think about lock, how new VM start when we change rows
	@DB
	public void work() {
	    if (s_logger.isTraceEnabled()) {
	        s_logger.trace("Checking the database");
	    }
		final OvsWorkVO work = _workDao.take(_serverId);
		if (work == null) {
			return;
		}
		Long userVmId = work.getInstanceId();
		UserVm vm = null;
		Long seqnum = null;
		s_logger.info("Ovs working on " + work.toString());
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		try {
			vm = _userVMDao.acquireInLockTable(work.getInstanceId());
			if (vm == null) {
				s_logger.warn("Ovs unable to acquire lock on vm id=" + userVmId);
				return ;
			}
			
			String vlans = getVlanMapping(vm.getAccountId());
			String tag = Long.toString(_vlanMappingDao.findByAccountIdAndHostId(
							vm.getAccountId(), vm.getHostId()).getVlan());
			Long agentId = null;
			VmFlowLogVO log = _flowLogDao.findByVmId(userVmId);
			if (log == null) {
				s_logger.warn("Ovs cannot find log record for vm id=" + userVmId);
				return;
			}
			seqnum = log.getLogsequence();

			if (vm != null && vm.getState() == State.Running) {
				agentId = vm.getHostId();
				if (agentId != null ) {
					OvsSetTagAndFlowCommand cmd = new OvsSetTagAndFlowCommand(
							vm.getName(), tag, vlans, seqnum.toString(), vm.getId());
					Commands cmds = new Commands(cmd);
					try {
						_agentMgr.send(agentId, cmds, _ovsListener);
						// TODO: clean dirty in answerListener
					} catch (AgentUnavailableException e) {
						s_logger.debug("Unable to send updates for vm: "
								+ userVmId + "(agentid=" + agentId + ")");
						_workDao.updateStep(work.getInstanceId(), seqnum,
								Step.Done);
					}
				}
			}
		} finally {
			if (vm != null) {
				_userVMDao.releaseFromLockTable(userVmId);
				_workDao.updateStep(work.getId(),  Step.Done);
			}
			txn.commit();
		}

	
	}
	
	//TODO: think about lock
	@Override
	@DB
	public long askVlanId(long accountId, long hostId) {
		assert _isEnabled : "Who call me ??? while OvsNetwokr is not enabled!!!";
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		
		VlanMappingVO currVlan = _vlanMappingDao.findByAccountIdAndHostId(accountId, hostId);
		long vlan = 0;
		
		if (currVlan != null) {
			vlan = currVlan.getVlan();
			currVlan.ref();
			_vlanMappingDao.update(currVlan.getId(), currVlan);
			s_logger.debug("Already has an Vlan " + vlan + " on host " + hostId
					+ " for account " + accountId + ", use it, reference count is " + currVlan.getRef());
			txn.commit();
			return vlan;
		}
		
		List<VlanMappingVO>mappings = _vlanMappingDao.listByHostId(hostId);
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
		String s = String.format("allocate a new vlan %1$s(account:%2$s, hostId:%3$s), mark dirty",
						vlan, accountId, hostId);
		s_logger.debug("OVSDIRTY:" + s);
		txn.commit();
		return vlan;
	}

	@Override
	@DB
	public String getVlanMapping(long accountId) {
		assert _isEnabled : "Who call me ??? while OvsNetwork is not enabled!!!";
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		
		List<VlanMappingVO> ours = _vlanMappingDao.listByAccountId(accountId);
		txn.commit();
		
		ArrayList<Long>vlans = new ArrayList<Long>();
		for (VlanMappingVO vo : ours) {
			vlans.add(new Long(vo.getVlan()));
		}
		
		assert vlans.size() > 0 : "Vlan map can't be null";
		StringBuffer buf = new StringBuffer();
		buf.append("/");
		for (Long i : vlans) {
			buf.append(i.toString());
			buf.append("/");
		}
		return buf.toString();
	}

	protected void CheckAndCreateTunnel(Commands cmds, VMInstanceVO instance,
			DeployDestination dest) {
		if (!_isEnabled) {
			return;
		}
		
		if (instance.getType() != VirtualMachine.Type.User
				&& instance.getType() != VirtualMachine.Type.DomainRouter) {
			return;
		}
		
		long hostId = dest.getHost().getId();
		long accountId = instance.getAccountId();
		List<UserVmVO> vms = _userVmDao.listByAccountIdAndHostId(accountId, hostId);
		if (vms.size() > 1 || (vms.size() == 1 && vms.get(0).getId() != instance.getId())) {
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
				Commands cmd2s = new Commands( new OvsCreateGreTunnelCommand(myIp, "1"));
				_agentMgr.send(i.longValue(), cmd2s , _ovsListener);
				s_logger.debug("Ask host " + i.longValue() + " to create gre tunnel to " + hostId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	protected void applyDefaultFlow(Commands cmds,
			VMInstanceVO instance, DeployDestination dest) {
		if (!_isEnabled) {
			return;
		}
		
		VirtualMachine.Type vmType = instance.getType();
		if (vmType != VirtualMachine.Type.User
				&& vmType != VirtualMachine.Type.DomainRouter) {
			return;
		}
		
		String tag = Long.toString(askVlanId(instance.getAccountId(), instance.getHostId()));
		String vlans = getVlanMapping(instance.getAccountId());
		CheckAndUpdateDhcpFlow(instance);
		VmFlowLogVO log = _flowLogDao.findOrNewByVmId(instance.getId(), instance.getName());
		cmds.addCommand(new OvsSetTagAndFlowCommand(instance.getName(), tag, vlans,
				Long.toString(log.getLogsequence()), instance.getId()));
	}
	
	//FIXME: if router has record in database but not start, this will hang 10 secs due to host
	//plugin cannot found vif for router.
	protected void CheckAndUpdateDhcpFlow(VMInstanceVO instance) {
		if (!_isEnabled) {
			return;
		}
		
		long accountId = instance.getAccountId();
		DomainRouterVO router = _routerDao.findBy(accountId, instance.getDataCenterId());
		if (router == null) {
			return;
		}
		
		if (!_vlanMappingDirtyDao.isDirty(accountId)) {
			return;
		}
		
		try {
			String vlans = getVlanMapping(accountId);
			String tag = Long.toString(_vlanMappingDao.findByAccountIdAndHostId(router.getAccountId(),
							router.getHostId()).getVlan());
			VmFlowLogVO log = _flowLogDao.findOrNewByVmId(instance.getId(), instance.getName());
			s_logger.debug("ask router " + router.getName() + " on host "
					+ router.getHostId() + " update vlan map to " + vlans);
			_agentMgr.send(router.getHostId(), new OvsSetTagAndFlowCommand(
					router.getName(), tag, vlans, Long.toString(log.getLogsequence()), instance.getId()));	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@DB
	@Override
	public void scheduleFlowUpdateToHosts(Set<Long> affectedVms, boolean updateSeqno, Long delayMs) {
	    if (!_isEnabled) {
	        return;
	    }
	    
		if (affectedVms == null) {
			return;
		}
		
		if (delayMs == null)
			delayMs = new Long(100l);
		
		for (Long vmId: affectedVms) {
			Transaction txn = Transaction.currentTxn();
			txn.start();
			VmFlowLogVO log = null;
			OvsWorkVO work = null;
			VirtualMachine vm = null;
			try {
				vm = _userVMDao.acquireInLockTable(vmId);
				if (vm == null) {
					vm = _routerDao.acquireInLockTable(vmId);
					if (vm == null) {
						s_logger.warn("Ovs failed to acquire lock on vm id " + vmId);
						continue;
					}
				}
				log = _flowLogDao.findOrNewByVmId(vmId, vm.getName());
		
				if (log != null && updateSeqno){
					log.incrLogsequence();
					_flowLogDao.update(log.getId(), log);
				}
				
				work = _workDao.findByVmIdStep(vmId, Step.Scheduled);
				if (work == null) {
					work = new OvsWorkVO(vmId,  null, null, OvsWorkVO.Step.Scheduled, null);
					work = _workDao.persist(work);
				}
				
				work.setLogsequenceNumber(log.getLogsequence());
				 _workDao.update(work.getId(), work);	
			} finally {
				if (vm != null) {
					_userVMDao.releaseFromLockTable(vmId);
				}
			}
			txn.commit();

			_executorPool.schedule(new WorkerThread(), delayMs, TimeUnit.MILLISECONDS);

		}
	}
	
	protected Set<Long> getAffectedVms(VMInstanceVO instance, boolean tellRouter) {
		long accountId = instance.getAccountId();
		if (!_vlanMappingDirtyDao.isDirty(accountId)) {
			s_logger.debug("OVSAFFECTED: no VM affected by " + instance.getName());
			return null;
		}
		
		Set<Long> affectedVms = new HashSet<Long>();
		List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
		for (UserVmVO vm : vms) {
			affectedVms.add(new Long(vm.getId()));
		}
		
		if (tellRouter && instance.getType() != VirtualMachine.Type.DomainRouter) {
			DomainRouterVO router = _routerDao.findBy(accountId, instance.getDataCenterId());
			if (router != null) {
				affectedVms.add(new Long(router.getId()));
			}
		}
		return affectedVms;
	}
	
	protected void handleVmStateChange(VMInstanceVO instance, boolean tellRouter) {
		Set<Long> affectedVms = getAffectedVms(instance, tellRouter);
		scheduleFlowUpdateToHosts(affectedVms, true, null);
		_vlanMappingDirtyDao.clean(instance.getAccountId());
		s_logger.debug("OVSDIRTY:Clean dirty for account " + instance.getAccountId());
	}
	
	//TODO: think about lock
	@DB
	protected void checkAndRemove(VMInstanceVO instance) {
		long accountId = instance.getAccountId();
		long hostId = instance.getHostId();
		
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		VlanMappingVO vo = _vlanMappingDao.findByAccountIdAndHostId(accountId, hostId);
		if (vo.unref() == 0) {
			_vlanMappingDao.remove(vo.getId());
			_vlanMappingDirtyDao.markDirty(accountId);
			String s = String.format("%1$s is the last VM(host:%2$s, accountId:%3$s), remove vlan",
							instance.getName(), hostId, accountId);
			s_logger.debug("OVSDIRTY:" + s);
		} else {
			_vlanMappingDao.update(vo.getId(), vo);
			s_logger.debug(instance.getName()
					+ " reduces reference count of (account,host) = ("
					+ accountId + "," + hostId + ") to " + vo.getRef());
		}
		_flowLogDao.deleteByVmId(instance.getId());
		txn.commit();
		
		try {
			Commands cmds = new Commands(new OvsDeleteFlowCommand(instance.getName()));
			_agentMgr.send(hostId, cmds, _ovsListener);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void handleVmStateTransition(VMInstanceVO instance, State vmState) {
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
			handleVmStateChange(instance, false);
			break;
		case Stopping:
		case Stopped:
			checkAndRemove(instance);
			handleVmStateChange(instance, true);
			break;
		}
		
	}

	@Override
	public void UserVmCheckAndCreateTunnel(Commands cmds,
			VirtualMachineProfile<UserVmVO> profile, DeployDestination dest) {
		CheckAndCreateTunnel(cmds, (VMInstanceVO)profile.getVirtualMachine(), dest);	
	}

	@Override
	public void RouterCheckAndCreateTunnel(Commands cmds,
			VirtualMachineProfile<DomainRouterVO> profile,
			DeployDestination dest) {
		CheckAndCreateTunnel(cmds, (VMInstanceVO)profile.getVirtualMachine(), dest);	
	}

	@Override
	public void applyDefaultFlowToUserVm(Commands cmds,
			VirtualMachineProfile<UserVmVO> profile, DeployDestination dest) {
		applyDefaultFlow(cmds, profile.getVirtualMachine(), dest);
		
	}

	@Override
	public void applyDefaultFlowToRouter(Commands cmds,
			VirtualMachineProfile<DomainRouterVO> profile,
			DeployDestination dest) {
		applyDefaultFlow(cmds, profile.getVirtualMachine(), dest);
	}

	@Override
	public void fullSync(List<Pair<String, Long>> states) {
		if (!_isEnabled) {
			return;
		}
		
		//TODO:debug code, remove in future
		List<AccountVO> accounts = _accountDao.listAll();
		for (AccountVO acnt : accounts) {
			if (_vlanMappingDirtyDao.isDirty(acnt.getId())) {
				s_logger.warn("Vlan mapping for account "
						+ acnt.getAccountName() + " id " + acnt.getId()
						+ " is dirty");
			}
		}
		
		if (states.size() ==0) {
			s_logger.info("Nothing to do, Ovs fullsync is happy");
			return;
		}
		
		Set<Long>vmIds = new HashSet<Long>();
		for (Pair<String, Long>state : states) {
			if (state.second() == -1) {
				s_logger.warn("Ovs fullsync get wrong seqno for " + state.first());
				continue;
			}
			VmFlowLogVO log = _flowLogDao.findByName(state.first());
			if (log.getLogsequence() != state.second()) {
				s_logger.debug("Ovs fullsync detected unmatch seq number for " + state.first() + ", run sync");
				VMInstanceVO vo = _instanceDao.findById(log.getInstanceId());
				if (vo == null) {
					s_logger.warn("Ovs can't find " + state.first() + " in vm_instance!");
					continue;
				}
				
				if (vo.getType() != VirtualMachine.Type.User && vo.getType() != VirtualMachine.Type.DomainRouter) {
					s_logger.warn("Ovs fullsync: why we sync a " + vo.getType().toString() + " VM???");
					continue;
				}
				vmIds.add(new Long(vo.getId()));
			}
		}
		
		if (vmIds.size() > 0) {
			scheduleFlowUpdateToHosts(vmIds, true, null);
		}
	}

}
