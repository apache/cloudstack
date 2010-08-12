/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.network.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.NetworkIngressRulesCmd;
import com.cloud.agent.api.NetworkIngressRulesCmd.IpPortAndProto;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.security.NetworkGroupWorkVO.Step;
import com.cloud.network.security.dao.IngressRuleDao;
import com.cloud.network.security.dao.NetworkGroupDao;
import com.cloud.network.security.dao.NetworkGroupRulesDao;
import com.cloud.network.security.dao.NetworkGroupVMMapDao;
import com.cloud.network.security.dao.NetworkGroupWorkDao;
import com.cloud.network.security.dao.VmRulesetLogDao;
import com.cloud.server.Criteria;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.State;
import com.cloud.vm.UserVm;
import com.cloud.vm.dao.UserVmDao;

@Local(value={NetworkGroupManager.class})
public class NetworkGroupManagerImpl implements NetworkGroupManager {
    public static final Logger s_logger = Logger.getLogger(NetworkGroupManagerImpl.class.getName());

	@Inject NetworkGroupDao _networkGroupDao;
	@Inject IngressRuleDao  _ingressRuleDao;
	@Inject NetworkGroupVMMapDao _networkGroupVMMapDao;
	@Inject NetworkGroupRulesDao _networkGroupRulesDao;
	@Inject UserVmDao _userVMDao;
	@Inject AccountDao _accountDao;
	@Inject ConfigurationDao _configDao;
	@Inject NetworkGroupWorkDao _workDao;
	@Inject VmRulesetLogDao _rulesetLogDao;
	@Inject DomainDao _domainDao;
	
	@Inject AgentManager _agentMgr;
	ScheduledExecutorService _executorPool;
    ScheduledExecutorService _cleanupExecutor;

	private long _serverId;

	private final long _timeBetweenCleanups = 30; //seconds

	
	boolean _enabled = false;
	NetworkGroupListener _answerListener;
    
	
	private final class NetworkGroupVOComparator implements
			Comparator<NetworkGroupVO> {
		@Override
		public int compare(NetworkGroupVO o1, NetworkGroupVO o2) {
			return o1.getId().compareTo(o2.getId());
		}
	}

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
	
	


	
	public static class PortAndProto implements Comparable<PortAndProto>{
		String proto;
		int startPort;
		int endPort;
		public PortAndProto(String proto, int startPort, int endPort) {
			this.proto = proto;
			this.startPort = startPort;
			this.endPort = endPort;
		}
		public String getProto() {
			return proto;
		}
		public int getStartPort() {
			return startPort;
		}
		public int getEndPort() {
			return endPort;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + endPort;
			result = prime * result + ((proto == null) ? 0 : proto.hashCode());
			result = prime * result + startPort;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PortAndProto other = (PortAndProto) obj;
			if (endPort != other.endPort)
				return false;
			if (proto == null) {
				if (other.proto != null)
					return false;
			} else if (!proto.equals(other.proto))
				return false;
			if (startPort != other.startPort)
				return false;
			return true;
		}
		
		@Override
		public int compareTo(PortAndProto obj) {
			if (this == obj)
				return 0;
			if (obj == null)
				return 1;
			if (proto == null) {
				if (obj.proto != null)
					return -1;
				else
					return 0;
			}
			if (!obj.proto.equalsIgnoreCase(proto)) {
				return proto.compareTo(obj.proto);
			}
			if (startPort < obj.startPort)
				return -1;
			else if (startPort > obj.startPort)
				return 1;
			
			if (endPort < obj.endPort)
				return -1;
			else if (endPort > obj.endPort)
				return 1;
			
			return 0;
		}
		
	}

	@Override
	public void handleVmStateTransition(UserVm userVm, State vmState) {
		if (!_enabled) {
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
	
	public static class CidrComparator implements Comparator<String> {

		@Override
		public int compare(String cidr1, String cidr2) {
			return cidr1.compareTo(cidr2); //FIXME
		}
		
	}

	protected Map<PortAndProto, Set<String>> generateRulesForVM(Long userVmId){

		Map<PortAndProto, Set<String>> allowed = new TreeMap<PortAndProto, Set<String>>();

		List<NetworkGroupVMMapVO> groupsForVm = _networkGroupVMMapDao.listByInstanceId(userVmId);
		for (NetworkGroupVMMapVO mapVO: groupsForVm) {
			List<IngressRuleVO> rules = _ingressRuleDao.listByNetworkGroupId(mapVO.getNetworkGroupId());
			for (IngressRuleVO rule: rules){
				PortAndProto portAndProto = new PortAndProto(rule.getProtocol(), rule.getStartPort(), rule.getEndPort());
				Set<String> cidrs = allowed.get(portAndProto );
				if (cidrs == null) {
					cidrs = new TreeSet<String>(new CidrComparator());
				}
				if (rule.getAllowedNetworkId() != null){
					List<NetworkGroupVMMapVO> allowedInstances = _networkGroupVMMapDao.listByNetworkGroup(rule.getAllowedNetworkId(), State.Running);
					for (NetworkGroupVMMapVO ngmapVO: allowedInstances){
						String cidr = ngmapVO.getGuestIpAddress();
						if (cidr != null) {
							cidr = cidr + "/32";
							cidrs.add(cidr);
						}
					}
				}else if (rule.getAllowedSourceIpCidr() != null) {
					cidrs.add(rule.getAllowedSourceIpCidr());
				}
				if (cidrs.size() > 0)
					allowed.put(portAndProto, cidrs);
			}
		}


		return  allowed;
	}
	
	private String generateRulesetSignature(Map<PortAndProto, Set<String>> allowed) {
		String ruleset = allowed.toString();
		return DigestUtils.md5Hex(ruleset);
	}

	protected void handleVmStarted(UserVm userVm) {
		Set<Long> affectedVms = getAffectedVmsForVmStart(userVm);
		scheduleRulesetUpdateToHosts(affectedVms, true, null);
	}
	
	@DB
	public void scheduleRulesetUpdateToHosts(Set<Long> affectedVms, boolean updateSeqno, Long delayMs) {
	    if (!_enabled) {
	        return;
	    }
		if (delayMs == null)
			delayMs = new Long(100l);
		
		for (Long vmId: affectedVms) {
			Transaction txn = Transaction.currentTxn();
			txn.start();
			VmRulesetLogVO log = null;
			NetworkGroupWorkVO work = null;
			UserVm vm = null;
			try {
				vm = _userVMDao.acquire(vmId);
				if (vm == null) {
					s_logger.warn("Failed to acquire lock on vm id " + vmId);
					continue;
				}
				log = _rulesetLogDao.findByVmId(vmId);
				if (log == null) {
					log = new VmRulesetLogVO(vmId);
					log = _rulesetLogDao.persist(log);
				}
		
				if (log != null && updateSeqno){
					log.incrLogsequence();
					_rulesetLogDao.update(log.getId(), log);
				}
				work = _workDao.findByVmIdStep(vmId, Step.Scheduled);
				if (work == null) {
					work = new NetworkGroupWorkVO(vmId,  null, null, NetworkGroupWorkVO.Step.Scheduled, null);
					work = _workDao.persist(work);
				}
				
				work.setLogsequenceNumber(log.getLogsequence());
				 _workDao.update(work.getId(), work);
				
			} finally {
				if (vm != null) {
					_userVMDao.release(vmId);
				}
			}
			txn.commit();

			_executorPool.schedule(new WorkerThread(), delayMs, TimeUnit.MILLISECONDS);

		}
	}
	
	protected Set<Long> getAffectedVmsForVmStart(UserVm userVm) {
		Set<Long> affectedVms = new HashSet<Long>();
		affectedVms.add(userVm.getId());
		List<NetworkGroupVMMapVO> groupsForVm = _networkGroupVMMapDao.listByInstanceId(userVm.getId());
		//For each group, find the ingress rules that allow the group
		for (NetworkGroupVMMapVO mapVO: groupsForVm) {//FIXME: use custom sql in the dao
			List<IngressRuleVO> allowingRules = _ingressRuleDao.listByAllowedNetworkGroupId(mapVO.getNetworkGroupId());
			//For each ingress rule that allows a group that the vm belongs to, find the group it belongs to
			affectedVms.addAll(getAffectedVmsForIngressRules(allowingRules));
		}
		return affectedVms;
	}
	
	protected Set<Long> getAffectedVmsForVmStop(UserVm userVm) {
		Set<Long> affectedVms = new HashSet<Long>();
		List<NetworkGroupVMMapVO> groupsForVm = _networkGroupVMMapDao.listByInstanceId(userVm.getId());
		//For each group, find the ingress rules that allow the group
		for (NetworkGroupVMMapVO mapVO: groupsForVm) {//FIXME: use custom sql in the dao
			List<IngressRuleVO> allowingRules = _ingressRuleDao.listByAllowedNetworkGroupId(mapVO.getNetworkGroupId());
			//For each ingress rule that allows a group that the vm belongs to, find the group it belongs to
			affectedVms.addAll(getAffectedVmsForIngressRules(allowingRules));
		}
		return affectedVms;
	}
	
	
	protected Set<Long> getAffectedVmsForIngressRules(List<IngressRuleVO> allowingRules) {
		Set<Long> distinctGroups = new HashSet<Long> ();
		Set<Long> affectedVms = new HashSet<Long>();

		for (IngressRuleVO allowingRule: allowingRules){
			distinctGroups.add(allowingRule.getNetworkGroupId());
		}
		for (Long groupId: distinctGroups){
			//allVmUpdates.putAll(generateRulesetForGroupMembers(groupId));
			affectedVms.addAll(_networkGroupVMMapDao.listVmIdsByNetworkGroup(groupId));
		}
		return affectedVms;
	}

	
	
	protected NetworkIngressRulesCmd generateRulesetCmd(String vmName, String guestIp, String guestMac, Long vmId, String signature,  long seqnum, Map<PortAndProto, Set<String>> rules) {
		List<IpPortAndProto> result = new ArrayList<IpPortAndProto>();
		for (PortAndProto pAp : rules.keySet()) {
			Set<String> cidrs = rules.get(pAp);
			if (cidrs.size() > 0) {
				IpPortAndProto ipPortAndProto = new NetworkIngressRulesCmd.IpPortAndProto(pAp.getProto(), pAp.getStartPort(), pAp.getEndPort(), cidrs.toArray(new String[cidrs.size()]));
				result.add(ipPortAndProto);
			}
		}
		return new NetworkIngressRulesCmd(guestIp, guestMac, vmName, vmId, signature, seqnum, result.toArray(new IpPortAndProto[result.size()]));
	}
	
	protected void handleVmStopped(UserVm userVm) {
		Set<Long> affectedVms = getAffectedVmsForVmStop(userVm);
		scheduleRulesetUpdateToHosts(affectedVms, true, null);
	}
	
	
	@Override
	@DB
	public List<IngressRuleVO> authorizeNetworkGroupIngress(AccountVO account,
			String groupName, String protocol, int startPort, int endPort,
			String [] cidrList, List<NetworkGroupVO> authorizedGroups) {
		if (!_enabled) {
			return null;
		}
        final Transaction txn = Transaction.currentTxn();
        final Long accountId = account.getId();
		final Set<NetworkGroupVO> authorizedGroups2 = new TreeSet<NetworkGroupVO>(new NetworkGroupVOComparator());

		authorizedGroups2.addAll(authorizedGroups); //Ensure we don't re-lock the same row
		txn.start();
		NetworkGroupVO networkGroup = _networkGroupDao.findByAccountAndName(accountId, groupName);
		if (networkGroup == null) {
			s_logger.warn("Network security group not found: name= " + groupName);
			return null;
		}
		//Prevents other threads/management servers from creating duplicate ingress rules
		NetworkGroupVO networkGroupLock = _networkGroupDao.acquire(networkGroup.getId());
		if (networkGroupLock == null)  {
			s_logger.warn("Could not acquire lock on network security group: name= " + groupName);
			return null;
		}
		List<IngressRuleVO> newRules = new ArrayList<IngressRuleVO>();
		try {
			//Don't delete the group from under us.
			networkGroup = _networkGroupDao.lock(networkGroup.getId(), false);
			if (networkGroup == null) {
				s_logger.warn("Could not acquire lock on network group " + groupName);
				return null;
			}

			for (final NetworkGroupVO ngVO: authorizedGroups2) {
				final Long ngId = ngVO.getId();
				//Don't delete the referenced group from under us
				if (ngVO.getId() != networkGroup.getId()) {
					final NetworkGroupVO tmpGrp = _networkGroupDao.lock(ngId, false);
					if (tmpGrp == null) {
						s_logger.warn("Failed to acquire lock on network group: " + ngId);
						txn.rollback();
						return null;
					}
				}
				IngressRuleVO ingressRule = _ingressRuleDao.findByProtoPortsAndAllowedGroupId(networkGroup.getId(), protocol, startPort, endPort, ngVO.getId());
				if (ingressRule != null) {
					continue; //rule already exists.
				}
				ingressRule  = new IngressRuleVO(networkGroup.getId(), startPort, endPort, protocol, ngVO.getId(), ngVO.getName(), ngVO.getAccountName());
				ingressRule = _ingressRuleDao.persist(ingressRule);
				newRules.add(ingressRule);
			}
			for (String cidr: cidrList) {
				IngressRuleVO ingressRule = _ingressRuleDao.findByProtoPortsAndCidr(networkGroup.getId(),protocol, startPort, endPort, cidr);
				if (ingressRule != null) {
					continue;
				}
				ingressRule  = new IngressRuleVO(networkGroup.getId(), startPort, endPort, protocol, cidr);
				ingressRule = _ingressRuleDao.persist(ingressRule);
				newRules.add(ingressRule);
			}
			if (s_logger.isDebugEnabled()) {
	            s_logger.debug("Added " + newRules.size() + " rules to network group " + groupName);
			}
			txn.commit();
			final Set<Long> affectedVms = new HashSet<Long>();
			affectedVms.addAll(_networkGroupVMMapDao.listVmIdsByNetworkGroup(networkGroup.getId()));
			scheduleRulesetUpdateToHosts(affectedVms, true, null);
			return newRules;
		} catch (Exception e){
			s_logger.warn("Exception caught when adding ingress rules ", e);
			throw new CloudRuntimeException("Exception caught when adding ingress rules", e);
		} finally {
			if (networkGroupLock != null) {
				_networkGroupDao.release(networkGroupLock.getId());
			}
		}
		
	}
	
	@Override
	@DB
	public boolean revokeNetworkGroupIngress(AccountVO account,
			String groupName, String protocol, int startPort,
			int endPort, String[] cidrList, List<NetworkGroupVO> authorizedGroups) {
		
		if (!_enabled) {
			return false;
		}
		int numDeleted = 0;
		final int numToDelete = cidrList.length + authorizedGroups.size();
        final Transaction txn = Transaction.currentTxn();
        final Long accountId = account.getId();
		NetworkGroupVO networkGroup = _networkGroupDao.findByAccountAndName(accountId, groupName);
		if (networkGroup == null) {
			s_logger.warn("Network security group not found: name= " + groupName);
			return false;
		}
		try {
			txn.start();
			
			networkGroup = _networkGroupDao.acquire(networkGroup.getId());
			if (networkGroup == null)  {
				s_logger.warn("Could not acquire lock on network security group: name= " + groupName);
				return false;
			}
			for (final NetworkGroupVO ngVO: authorizedGroups) {
				numDeleted += _ingressRuleDao.deleteByPortProtoAndGroup(networkGroup.getId(), protocol, startPort, endPort, ngVO.getId());
			}
			for (final String cidr: cidrList) {
				numDeleted += _ingressRuleDao.deleteByPortProtoAndCidr(networkGroup.getId(), protocol, startPort, endPort, cidr);
			}
			s_logger.debug("revokeNetworkGroupIngress for group: " + groupName + ", numToDelete=" + numToDelete + ", numDeleted=" + numDeleted);
			
			final Set<Long> affectedVms = new HashSet<Long>();
			affectedVms.addAll(_networkGroupVMMapDao.listVmIdsByNetworkGroup(networkGroup.getId()));
			scheduleRulesetUpdateToHosts(affectedVms, true, null);
			
			return true;
		} catch (Exception e) {
			s_logger.warn("Exception caught when deleting ingress rules ", e);
			throw new CloudRuntimeException("Exception caught when deleting ingress rules", e);
		} finally {
			if (networkGroup != null) {
				_networkGroupDao.release(networkGroup.getId());
			}
			txn.commit();
		}
		
	}
	

	@DB
	@Override
	public NetworkGroupVO createNetworkGroup(String name, String description, Long domainId, Long accountId, String accountName) {
		if (!_enabled) {
			return null;
		}
		final Transaction txn = Transaction.currentTxn();
		AccountVO account = null;
		txn.start();
		try {
			account = _accountDao.acquire(accountId); //to ensure duplicate group names are not created.
			if (account == null) {
				s_logger.warn("Failed to acquire lock on account");
				return null;
			}
			NetworkGroupVO group = _networkGroupDao.findByAccountAndName(accountId, name);
			if (group == null){
				group = new NetworkGroupVO(name, description, domainId, accountId, accountName);
				group =  _networkGroupDao.persist(group);
			}
			return group;
		} finally {
			if (account != null) {
				_accountDao.release(accountId);
			}
			txn.commit();
		}
		
    }
	
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		String enabled =_configDao.getValue("direct.attach.network.groups.enabled");
		if ("true".equalsIgnoreCase(enabled)) {
			_enabled = true;
		}
		if (!_enabled) {
			return false;
		}
		_answerListener = new NetworkGroupListener(this, _agentMgr, _workDao);
		_agentMgr.registerForHostEvents(_answerListener, true, true, true);
		
        _serverId = ((ManagementServer)ComponentLocator.getComponent(ManagementServer.Name)).getId();
        _executorPool = Executors.newScheduledThreadPool(10, new NamedThreadFactory("NWGRP"));
        _cleanupExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NWGRP-Cleanup"));


 		return true;
	}


	@Override
	public String getName() {
		return this.getClass().getName();
	}


	@Override
	public boolean start() {
	    if (!_enabled) {
	        return true;
	    }
		_cleanupExecutor.scheduleAtFixedRate(new CleanupThread(), _timeBetweenCleanups, _timeBetweenCleanups, TimeUnit.SECONDS);
		return true;
	}


	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public NetworkGroupVO createDefaultNetworkGroup(Long accountId) {
		if (!_enabled) {
			return null;
		}
		NetworkGroupVO groupVO = _networkGroupDao.findByAccountAndName(accountId, NetworkGroupManager.DEFAULT_GROUP_NAME);
		if (groupVO == null ) {
			Account accVO = _accountDao.findById(accountId);
			if (accVO != null) {
				return createNetworkGroup(NetworkGroupManager.DEFAULT_GROUP_NAME, NetworkGroupManager.DEFAULT_GROUP_DESCRIPTION, accVO.getDomainId(), accVO.getId(), accVO.getAccountName());
			}
		}
		return groupVO;
	}
	
	@DB
	public void work() {

		s_logger.trace("Checking the database");
		final NetworkGroupWorkVO work = _workDao.take(_serverId);
		if (work == null) {
			return;
		}
		Long userVmId = work.getInstanceId();
		UserVm vm = null;
		Long seqnum = null;
		s_logger.info("Working on " + work.toString());
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		try {
			vm = _userVMDao.acquire(work.getInstanceId());
			if (vm == null) {
				s_logger.warn("Unable to acquire lock on vm id=" + userVmId);
				return ;
			}
			Long agentId = null;
			VmRulesetLogVO log = _rulesetLogDao.findByVmId(userVmId);
			if (log == null) {
				s_logger.warn("Cannot find log record for vm id=" + userVmId);
				return;
			}
			seqnum = log.getLogsequence();

			if (vm != null && vm.getState() == State.Running) {
				Map<PortAndProto, Set<String>> rules = generateRulesForVM(userVmId);
				agentId = vm.getHostId();
				if (agentId != null ) {
					_rulesetLogDao.findByVmId(work.getInstanceId());
					NetworkIngressRulesCmd cmd = generateRulesetCmd(vm.getInstanceName(), vm.getGuestIpAddress(), vm.getGuestMacAddress(), vm.getId(), generateRulesetSignature(rules), seqnum, rules);
					Command[] cmds = new Command[]{cmd};
					try {
						_agentMgr.send(agentId, cmds, false, _answerListener);
					} catch (AgentUnavailableException e) {
						s_logger.debug("Unable to send updates for vm: " + userVmId + "(agentid=" + agentId + ")");
						_workDao.updateStep(work.getInstanceId(), seqnum, Step.Done);
					}
				}
			}
		} finally {
			if (vm != null) {
				_userVMDao.release(userVmId);
				_workDao.updateStep(work.getId(),  Step.Done);
			}
			txn.commit();
		}

	
	}

	@Override
	@DB
	public boolean addInstanceToGroups(final Long userVmId, final List<NetworkGroupVO> groups) {
		if (groups != null) {
			final Set<NetworkGroupVO> uniqueGroups = new TreeSet<NetworkGroupVO>(new NetworkGroupVOComparator());
			uniqueGroups.addAll(groups);
			final Transaction txn = Transaction.currentTxn();
			txn.start();
			UserVm userVm = _userVMDao.acquire(userVmId); //ensures that duplicate entries are not created.
			if (userVm == null) {
				s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
			}
			try {
				for (NetworkGroupVO networkGroup:uniqueGroups) {
					//don't let the group be deleted from under us.
					NetworkGroupVO ngrpLock = _networkGroupDao.lock(networkGroup.getId(), false);
					if (ngrpLock == null) {
						s_logger.warn("Failed to acquire lock on network group id=" + networkGroup.getId() + " name=" + networkGroup.getName());
						txn.rollback();
						return false;
					}
					if (_networkGroupVMMapDao.findByVmIdGroupId(userVmId, networkGroup.getId()) == null) {
						NetworkGroupVMMapVO groupVmMapVO = new NetworkGroupVMMapVO(networkGroup.getId(), userVmId);
						_networkGroupVMMapDao.persist(groupVmMapVO);
					}
				}
				txn.commit();
				return true;
			} finally {
				if (userVm != null) {
					_userVMDao.release(userVmId);
				}
			}
			

        }
		return false;
		
	}

	@Override
	@DB
	public void removeInstanceFromGroups(Long userVmId) {
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		UserVm userVm = _userVMDao.acquire(userVmId); //ensures that duplicate entries are not created in addInstance
		if (userVm == null) {
			s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
		}
		int n = _networkGroupVMMapDao.deleteVM(userVmId);
		s_logger.info("Disassociated " + n + " network groups " + " from uservm " + userVmId);
		_userVMDao.release(userVmId);
		txn.commit();
	}

	@DB
	@Override
	public void deleteNetworkGroup(Long groupId, Long accountId) throws ResourceInUseException, PermissionDeniedException{
		if (!_enabled) {
			return ;
		}
		
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		
		final NetworkGroupVO group = _networkGroupDao.lock(groupId, true);
		if (group == null) {
			s_logger.info("Not deleting group -- cannot find id " + groupId);
			return;
		}
		
		if (group.getName().equalsIgnoreCase(NetworkGroupManager.DEFAULT_GROUP_NAME)) {
			txn.rollback();
			throw new PermissionDeniedException("The network group default is reserved");
		}
		
		List<IngressRuleVO> allowingRules = _ingressRuleDao.listByAllowedNetworkGroupId(groupId);
		if (allowingRules.size() != 0) {
			txn.rollback();
			throw new ResourceInUseException("Cannot delete group when there are ingress rules that allow this group");
		}
		
		List<IngressRuleVO> rulesInGroup = _ingressRuleDao.listByNetworkGroupId(groupId);
		if (rulesInGroup.size() != 0) {
			txn.rollback();
			throw new ResourceInUseException("Cannot delete group when there are ingress rules in this group");
		}
        _networkGroupDao.delete(groupId);
        txn.commit();
	}

    @Override
    public List<NetworkGroupRulesVO> searchForNetworkGroupRules(Criteria c) {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, c.getOffset(), c.getLimit());
        Object accountId = c.getCriteria(Criteria.ACCOUNTID);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object networkGroup = c.getCriteria(Criteria.NETWORKGROUP);
        Object instanceId = c.getCriteria(Criteria.INSTANCEID);
        Object recursive = c.getCriteria(Criteria.ISRECURSIVE);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchBuilder<NetworkGroupRulesVO> sb = _networkGroupRulesDao.createSearchBuilder();
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);

        // only do a recursive domain search if the search is not limited by account or instance
        if ((accountId == null) && (instanceId == null) && (domainId != null) && Boolean.TRUE.equals(recursive)) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria sc = sb.create();
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
            if (networkGroup != null) {
                sc.setParameters("name", networkGroup);
            } else if (keyword != null) {
                SearchCriteria ssc = _networkGroupRulesDao.createSearchCriteria();
                ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                sc.addAnd("name", SearchCriteria.Op.SC, ssc);
            }
        } else if (instanceId != null) {
            return listNetworkGroupRulesByVM(((Long)instanceId).longValue());
        } else if (domainId != null) {
            if (Boolean.TRUE.equals(recursive)) {
                DomainVO domain = _domainDao.findById((Long)domainId);
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }

        return _networkGroupRulesDao.search(sc, searchFilter);
    }

	private List<NetworkGroupRulesVO> listNetworkGroupRulesByVM(long vmId) {
	    List<NetworkGroupRulesVO> results = new ArrayList<NetworkGroupRulesVO>();
	    List<NetworkGroupVMMapVO> networkGroupMappings = _networkGroupVMMapDao.listByInstanceId(vmId);
	    if (networkGroupMappings != null) {
	        for (NetworkGroupVMMapVO networkGroupMapping : networkGroupMappings) {
	            NetworkGroupVO group = _networkGroupDao.findById(networkGroupMapping.getNetworkGroupId());
	            List<NetworkGroupRulesVO> rules = _networkGroupRulesDao.listNetworkGroupRules(group.getAccountId(), networkGroupMapping.getGroupName());
	            if (rules != null) {
	                results.addAll(rules);
	            }
	        }
	    }
	    return results;
	}

	@Override
	public void fullSync(long agentId, HashMap<String, Pair<Long, Long>> newGroupStates) {
		Set<Long> affectedVms = new HashSet<Long>();
		for (String vmName: newGroupStates.keySet()) {
			Long vmId = newGroupStates.get(vmName).first();
			Long seqno = newGroupStates.get(vmName).second();

			VmRulesetLogVO log = _rulesetLogDao.findByVmId(vmId);
			if (log != null && log.getLogsequence() != seqno) {
				affectedVms.add(vmId);
			}
		}
		if (affectedVms.size() > 0){
			s_logger.info("Network Group full sync for agent " + agentId + " found " + affectedVms.size() + " vms out of sync");
			scheduleRulesetUpdateToHosts(affectedVms, false, null);
		}
		
	}
	
	public void cleanupFinishedWork() {
		Date before = new Date(System.currentTimeMillis() - 24*3600*1000l);
		int numDeleted = _workDao.deleteFinishedWork(before);
		if (numDeleted > 0) {
			s_logger.info("Network Group Work cleanup deleted " + numDeleted + " finished work items older than " + before.toString());
		}
		
	}
	


	private void cleanupUnfinishedWork() {
		Date before = new Date(System.currentTimeMillis() - 30*1000l);
		List<NetworkGroupWorkVO> unfinished = _workDao.findUnfinishedWork(before);
		if (unfinished.size() > 0) {
			s_logger.info("Network Group Work cleanup found " + unfinished.size() + " unfinished work items older than " + before.toString());
			Set<Long> affectedVms = new HashSet<Long>();
			for (NetworkGroupWorkVO work: unfinished) {
				affectedVms.add(work.getInstanceId());
			}
			scheduleRulesetUpdateToHosts(affectedVms, false, null);
		} else {
			s_logger.debug("Network Group Work cleanup found no unfinished work items older than " + before.toString());
		}
	}

	@Override
	public String getNetworkGroupsNamesForVm(long vmId) 
	{
		try
		{
			List<NetworkGroupVMMapVO>networkGroupsToVmMap =  _networkGroupVMMapDao.listByInstanceId(vmId);
        	int size = 0;
        	int j=0;		
            StringBuilder networkGroupNames = new StringBuilder();

            if(networkGroupsToVmMap != null)
            {
            	size = networkGroupsToVmMap.size();
            	
            	for(NetworkGroupVMMapVO nG: networkGroupsToVmMap)
            	{
            		//get the group id and look up for the group name
            		NetworkGroupVO currentNetworkGroup = _networkGroupDao.findById(nG.getNetworkGroupId());
            		networkGroupNames.append(currentNetworkGroup.getName());
            	
            		if(j<(size-1))
            		{
            			networkGroupNames.append(",");
            			j++;
            		}
            	}
            }
			
			return networkGroupNames.toString();
		}
		catch (Exception e)
		{
			s_logger.warn("Error trying to get network groups for a vm: "+e);
			return null;
		}

	}
}
