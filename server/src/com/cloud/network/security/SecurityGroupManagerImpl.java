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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.cloud.agent.api.SecurityIngressRulesCmd;
import com.cloud.agent.api.SecurityIngressRulesCmd.IpPortAndProto;
import com.cloud.agent.manager.Commands;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AuthorizeSecurityGroupIngressCmd;
import com.cloud.api.commands.CreateSecurityGroupCmd;
import com.cloud.api.commands.DeleteSecurityGroupCmd;
import com.cloud.api.commands.ListSecurityGroupsCmd;
import com.cloud.api.commands.RevokeSecurityGroupIngressCmd;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.security.SecurityGroupWorkVO.Step;
import com.cloud.network.security.dao.IngressRuleDao;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.security.dao.SecurityGroupRulesDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.network.security.dao.SecurityGroupWorkDao;
import com.cloud.network.security.dao.VmRulesetLogDao;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;

@Local(value={SecurityGroupManager.class, SecurityGroupService.class})
public class SecurityGroupManagerImpl implements SecurityGroupManager, SecurityGroupService, Manager {
    public static final Logger s_logger = Logger.getLogger(SecurityGroupManagerImpl.class);

	@Inject SecurityGroupDao _securityGroupDao;
	@Inject IngressRuleDao  _ingressRuleDao;
	@Inject SecurityGroupVMMapDao _securityGroupVMMapDao;
	@Inject SecurityGroupRulesDao _securityGroupRulesDao;
	@Inject UserVmDao _userVMDao;
	@Inject AccountDao _accountDao;
	@Inject ConfigurationDao _configDao;
	@Inject SecurityGroupWorkDao _workDao;
	@Inject VmRulesetLogDao _rulesetLogDao;
	@Inject DomainDao _domainDao;
	@Inject AgentManager _agentMgr;
	ScheduledExecutorService _executorPool;
    ScheduledExecutorService _cleanupExecutor;

	private long _serverId;

	private final long _timeBetweenCleanups = 30; //seconds

	
	boolean _enabled = false;
	SecurityGroupListener _answerListener;
    
	
	private final class SecurityGroupVOComparator implements
			Comparator<SecurityGroupVO> {
		@Override
		public int compare(SecurityGroupVO o1, SecurityGroupVO o2) {
			return o1.getId() == o2.getId() ? 0 : o1.getId() < o2.getId() ? -1 : 1;
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
			if (this == obj) {
                return true;
            }
			if (obj == null) {
                return false;
            }
			if (getClass() != obj.getClass()) {
                return false;
            }
			PortAndProto other = (PortAndProto) obj;
			if (endPort != other.endPort) {
                return false;
            }
			if (proto == null) {
				if (other.proto != null) {
                    return false;
                }
			} else if (!proto.equals(other.proto)) {
                return false;
            }
			if (startPort != other.startPort) {
                return false;
            }
			return true;
		}
		
		@Override
		public int compareTo(PortAndProto obj) {
			if (this == obj) {
                return 0;
            }
			if (obj == null) {
                return 1;
            }
			if (proto == null) {
				if (obj.proto != null) {
                    return -1;
                } else {
                    return 0;
                }
			}
			if (!obj.proto.equalsIgnoreCase(proto)) {
				return proto.compareTo(obj.proto);
			}
			if (startPort < obj.startPort) {
                return -1;
            } else if (startPort > obj.startPort) {
                return 1;
            }
			
			if (endPort < obj.endPort) {
                return -1;
            } else if (endPort > obj.endPort) {
                return 1;
            }
			
			return 0;
		}
		
	}

	@Override
	public void handleVmStateTransition(UserVm userVm, State vmState) {
		if (!_enabled) {
			return;
		}
		switch (vmState) {
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

		List<SecurityGroupVMMapVO> groupsForVm = _securityGroupVMMapDao.listByInstanceId(userVmId);
		for (SecurityGroupVMMapVO mapVO: groupsForVm) {
			List<IngressRuleVO> rules = _ingressRuleDao.listBySecurityGroupId(mapVO.getSecurityGroupId());
			for (IngressRuleVO rule: rules){
				PortAndProto portAndProto = new PortAndProto(rule.getProtocol(), rule.getStartPort(), rule.getEndPort());
				Set<String> cidrs = allowed.get(portAndProto );
				if (cidrs == null) {
					cidrs = new TreeSet<String>(new CidrComparator());
				}
				if (rule.getAllowedNetworkId() != null){
					List<SecurityGroupVMMapVO> allowedInstances = _securityGroupVMMapDao.listBySecurityGroup(rule.getAllowedNetworkId(), State.Running);
					for (SecurityGroupVMMapVO ngmapVO: allowedInstances){
						String cidr = ngmapVO.getGuestIpAddress();
						if (cidr != null) {
							cidr = cidr + "/32";
							cidrs.add(cidr);
						}
					}
				}else if (rule.getAllowedSourceIpCidr() != null) {
					cidrs.add(rule.getAllowedSourceIpCidr());
				}
				if (cidrs.size() > 0) {
                    allowed.put(portAndProto, cidrs);
                }
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
		if (delayMs == null) {
            delayMs = new Long(100l);
        }
		
		for (Long vmId: affectedVms) {
			Transaction txn = Transaction.currentTxn();
			txn.start();
			VmRulesetLogVO log = null;
			SecurityGroupWorkVO work = null;
			UserVm vm = null;
			try {
				vm = _userVMDao.acquireInLockTable(vmId);
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
					work = new SecurityGroupWorkVO(vmId,  null, null, SecurityGroupWorkVO.Step.Scheduled, null);
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
	
	protected Set<Long> getAffectedVmsForVmStart(UserVm userVm) {
		Set<Long> affectedVms = new HashSet<Long>();
		affectedVms.add(userVm.getId());
		List<SecurityGroupVMMapVO> groupsForVm = _securityGroupVMMapDao.listByInstanceId(userVm.getId());
		//For each group, find the ingress rules that allow the group
		for (SecurityGroupVMMapVO mapVO: groupsForVm) {//FIXME: use custom sql in the dao
			List<IngressRuleVO> allowingRules = _ingressRuleDao.listByAllowedSecurityGroupId(mapVO.getSecurityGroupId());
			//For each ingress rule that allows a group that the vm belongs to, find the group it belongs to
			affectedVms.addAll(getAffectedVmsForIngressRules(allowingRules));
		}
		return affectedVms;
	}
	
	protected Set<Long> getAffectedVmsForVmStop(UserVm userVm) {
		Set<Long> affectedVms = new HashSet<Long>();
		List<SecurityGroupVMMapVO> groupsForVm = _securityGroupVMMapDao.listByInstanceId(userVm.getId());
		//For each group, find the ingress rules that allow the group
		for (SecurityGroupVMMapVO mapVO: groupsForVm) {//FIXME: use custom sql in the dao
			List<IngressRuleVO> allowingRules = _ingressRuleDao.listByAllowedSecurityGroupId(mapVO.getSecurityGroupId());
			//For each ingress rule that allows a group that the vm belongs to, find the group it belongs to
			affectedVms.addAll(getAffectedVmsForIngressRules(allowingRules));
		}
		return affectedVms;
	}
	
	
	protected Set<Long> getAffectedVmsForIngressRules(List<IngressRuleVO> allowingRules) {
		Set<Long> distinctGroups = new HashSet<Long> ();
		Set<Long> affectedVms = new HashSet<Long>();

		for (IngressRuleVO allowingRule: allowingRules){
			distinctGroups.add(allowingRule.getSecurityGroupId());
		}
		for (Long groupId: distinctGroups){
			//allVmUpdates.putAll(generateRulesetForGroupMembers(groupId));
			affectedVms.addAll(_securityGroupVMMapDao.listVmIdsBySecurityGroup(groupId));
		}
		return affectedVms;
	}

	
	
	protected SecurityIngressRulesCmd generateRulesetCmd(String vmName, String guestIp, String guestMac, Long vmId, String signature,  long seqnum, Map<PortAndProto, Set<String>> rules) {
		List<IpPortAndProto> result = new ArrayList<IpPortAndProto>();
		for (PortAndProto pAp : rules.keySet()) {
			Set<String> cidrs = rules.get(pAp);
			if (cidrs.size() > 0) {
				IpPortAndProto ipPortAndProto = new SecurityIngressRulesCmd.IpPortAndProto(pAp.getProto(), pAp.getStartPort(), pAp.getEndPort(), cidrs.toArray(new String[cidrs.size()]));
				result.add(ipPortAndProto);
			}
		}
		return new SecurityIngressRulesCmd(guestIp, guestMac, vmName, vmId, signature, seqnum, result.toArray(new IpPortAndProto[result.size()]));
	}
	
	protected void handleVmStopped(UserVm userVm) {
		Set<Long> affectedVms = getAffectedVmsForVmStop(userVm);
		scheduleRulesetUpdateToHosts(affectedVms, true, null);
	}
	
	
	@Override @DB @SuppressWarnings("rawtypes")
	public List<IngressRuleVO> authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
		String groupName = cmd.getSecurityGroupName();
		String protocol = cmd.getProtocol();
		Integer startPort = cmd.getStartPort();
		Integer endPort = cmd.getEndPort();
		Integer icmpType = cmd.getIcmpType();
		Integer icmpCode = cmd.getIcmpCode();
		List<String> cidrList = cmd.getCidrList();
		Map groupList = cmd.getUserSecurityGroupList();
        Account account = UserContext.current().getCaller();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
		Integer startPortOrType = null;
        Integer endPortOrCode = null;
        Long accountId = null;
		
		if (!_enabled) {
			return null;
		}
		
		//Verify input parameters
        if (protocol == null) {
        	protocol = "all";
        }

        if (!NetUtils.isValidSecurityGroupProto(protocol)) {
        	s_logger.debug("Invalid protocol specified " + protocol);
        	 throw new InvalidParameterValueException("Invalid protocol " + protocol);
        }
        if ("icmp".equalsIgnoreCase(protocol) ) {
            if ((icmpType == null) || (icmpCode == null)) {
                throw new InvalidParameterValueException("Invalid ICMP type/code specified, icmpType = " + icmpType + ", icmpCode = " + icmpCode);
            }
        	if (icmpType == -1 && icmpCode != -1) {
        		throw new InvalidParameterValueException("Invalid icmp type range" );
        	} 
        	if (icmpCode > 255) {
        		throw new InvalidParameterValueException("Invalid icmp code " );
        	}
        	startPortOrType = icmpType;
        	endPortOrCode= icmpCode;
        } else if (protocol.equals("all")) {
        	if ((startPort != null) || (endPort != null)) {
                throw new InvalidParameterValueException("Cannot specify startPort or endPort without specifying protocol");
            }
        	startPortOrType = 0;
        	endPortOrCode = 0;
        } else {
            if ((startPort == null) || (endPort == null)) {
                throw new InvalidParameterValueException("Invalid port range specified, startPort = " + startPort + ", endPort = " + endPort);
            }
            if (startPort == 0 && endPort == 0) {
                endPort = 65535;
            }
            if (startPort > endPort) {
                s_logger.debug("Invalid port range specified: " + startPort + ":" + endPort);
                throw new InvalidParameterValueException("Invalid port range " );
            }
            if (startPort > 65535 || endPort > 65535 || startPort < -1 || endPort < -1) {
                s_logger.debug("Invalid port numbers specified: " + startPort + ":" + endPort);
                throw new InvalidParameterValueException("Invalid port numbers " );
            }
            
        	if (startPort < 0 || endPort < 0) {
        		throw new InvalidParameterValueException("Invalid port range " );
        	}
            startPortOrType = startPort;
            endPortOrCode= endPort;
        }
        
        protocol = protocol.toLowerCase();

        if ((account == null) || isAdmin(account.getType())) {
            if ((accountName != null) && (domainId != null)) {
                // if it's an admin account, do a quick permission check
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find rules for security group id = " + groupName + ", permission denied.");
                    }
                    throw new PermissionDeniedException("Unable to find rules for security group id = " + groupName + ", permission denied.");
                }

                Account groupOwner = _accountDao.findActiveAccount(accountName, domainId);
                if (groupOwner == null) {
                    throw new PermissionDeniedException("Unable to find account " + accountName + " in domain " + domainId);
                }
                accountId = groupOwner.getId();
            } else {
                if (account != null) {
                    accountId = account.getId();
                    domainId = account.getDomainId();
                }
            }
        } else {
            if (account != null) {
                accountId = account.getId();
                domainId = account.getDomainId();
            }
        }

        if (accountId == null) {
            throw new InvalidParameterValueException("Unable to find account for security group " + groupName + "; failed to authorize ingress.");
        }
      

        if (cidrList == null && groupList == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("At least one cidr or at least one security group needs to be specified");
            }
        	throw new InvalidParameterValueException("At least one cidr or at least one security group needs to be specified");
        }
        
        List<SecurityGroupVO> authorizedGroups = new ArrayList<SecurityGroupVO> ();
        if (groupList != null) {
            Collection userGroupCollection = groupList.values();
            Iterator iter = userGroupCollection.iterator();
            while (iter.hasNext()) {
                HashMap userGroup = (HashMap)iter.next();
        		String group = (String)userGroup.get("group");
        		String authorizedAccountName = (String)userGroup.get("account");
        		if ((group == null) || (authorizedAccountName == null)) {
        			 throw new InvalidParameterValueException("Invalid user group specified, fields 'group' and 'account' cannot be null, please specify groups in the form:  userGroupList[0].group=XXX&userGroupList[0].account=YYY");
        		}

        		Account authorizedAccount = _accountDao.findActiveAccount(authorizedAccountName, domainId);
        		if (authorizedAccount == null) {
        		    if (s_logger.isDebugEnabled()) {
        		        s_logger.debug("Nonexistent account: " + authorizedAccountName + ", domainid: " + domainId + " when trying to authorize ingress for " + groupName + ":" + protocol + ":" + startPortOrType + ":" + endPortOrCode);
        		    }
        		    throw new InvalidParameterValueException("Nonexistent account: " + authorizedAccountName + " when trying to authorize ingress for " + groupName + ":" + protocol + ":" + startPortOrType + ":" + endPortOrCode);
        		}

        		SecurityGroupVO groupVO = _securityGroupDao.findByAccountAndName(authorizedAccount.getId(), group);
        		if (groupVO == null) {
        		    if (s_logger.isDebugEnabled()) {
        		        s_logger.debug("Nonexistent group " + group + " for account " + authorizedAccountName + "/" + domainId);
        		    }
        		    throw new InvalidParameterValueException("Invalid group (" + group + ") given, unable to authorize ingress.");
        		}
        		authorizedGroups.add(groupVO);
        	}
        }
		
        final Transaction txn = Transaction.currentTxn();
		final Set<SecurityGroupVO> authorizedGroups2 = new TreeSet<SecurityGroupVO>(new SecurityGroupVOComparator());

		authorizedGroups2.addAll(authorizedGroups); //Ensure we don't re-lock the same row
		txn.start();
		SecurityGroupVO securityGroup = _securityGroupDao.findByAccountAndName(accountId, groupName);
		if (securityGroup == null) {
			s_logger.warn("Security group not found: name= " + groupName);
			return null;
		}
		//Prevents other threads/management servers from creating duplicate ingress rules
		SecurityGroupVO securityGroupLock = _securityGroupDao.acquireInLockTable(securityGroup.getId());
		if (securityGroupLock == null)  {
			s_logger.warn("Could not acquire lock on network security group: name= " + groupName);
			return null;
		}
		List<IngressRuleVO> newRules = new ArrayList<IngressRuleVO>();
		try {
			//Don't delete the group from under us.
			securityGroup = _securityGroupDao.lockRow(securityGroup.getId(), false);
			if (securityGroup == null) {
				s_logger.warn("Could not acquire lock on network group " + groupName);
				return null;
			}

			for (final SecurityGroupVO ngVO: authorizedGroups2) {
				final Long ngId = ngVO.getId();
				//Don't delete the referenced group from under us
				if (ngVO.getId() != securityGroup.getId()) {
					final SecurityGroupVO tmpGrp = _securityGroupDao.lockRow(ngId, false);
					if (tmpGrp == null) {
						s_logger.warn("Failed to acquire lock on security group: " + ngId);
						txn.rollback();
						return null;
					}
				}
				IngressRuleVO ingressRule = _ingressRuleDao.findByProtoPortsAndAllowedGroupId(securityGroup.getId(), protocol, startPortOrType, endPortOrCode, ngVO.getId());
				if (ingressRule != null) {
					continue; //rule already exists.
				}
				ingressRule  = new IngressRuleVO(securityGroup.getId(), startPortOrType, endPortOrCode, protocol, ngVO.getId(), ngVO.getName(), ngVO.getAccountName());
				ingressRule = _ingressRuleDao.persist(ingressRule);
				newRules.add(ingressRule);
			}
			if(cidrList != null) {
				for (String cidr: cidrList) {
					IngressRuleVO ingressRule = _ingressRuleDao.findByProtoPortsAndCidr(securityGroup.getId(),protocol, startPortOrType, endPortOrCode, cidr);
					if (ingressRule != null) {
						continue;
					}
					ingressRule  = new IngressRuleVO(securityGroup.getId(), startPortOrType, endPortOrCode, protocol, cidr);
					ingressRule = _ingressRuleDao.persist(ingressRule);
					newRules.add(ingressRule);
				}
			}
			if (s_logger.isDebugEnabled()) {
	            s_logger.debug("Added " + newRules.size() + " rules to security group " + groupName);
			}
			txn.commit();
			final Set<Long> affectedVms = new HashSet<Long>();
			affectedVms.addAll(_securityGroupVMMapDao.listVmIdsBySecurityGroup(securityGroup.getId()));
			scheduleRulesetUpdateToHosts(affectedVms, true, null);
			return newRules;
		} catch (Exception e){
			s_logger.warn("Exception caught when adding ingress rules ", e);
			throw new CloudRuntimeException("Exception caught when adding ingress rules", e);
		} finally {
			if (securityGroupLock != null) {
				_securityGroupDao.releaseFromLockTable(securityGroupLock.getId());
			}
		}
	}
	
	@Override
	@DB @SuppressWarnings("rawtypes")
	public boolean revokeSecurityGroupIngress(RevokeSecurityGroupIngressCmd cmd) {
		
		//input validation
		Account account = UserContext.current().getCaller();
		Long userId  = UserContext.current().getCallerUserId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Integer startPort = cmd.getStartPort();
        Integer endPort = cmd.getEndPort();
        Integer icmpType = cmd.getIcmpType();
        Integer icmpCode = cmd.getIcmpCode();
        String protocol = cmd.getProtocol();
        String securityGroup = cmd.getSecurityGroupName();
        String cidrList = cmd.getCidrList();
        Map groupList = cmd.getUserSecurityGroupList();
        String [] cidrs = null;
        Long accountId = null;
        Integer startPortOrType = null;
        Integer endPortOrCode = null;
        if (protocol == null) {
        	protocol = "all";
        }
        //FIXME: for exceptions below, add new enums to BaseCmd.PARAM_ to reflect the error condition more precisely
        if (!NetUtils.isValidSecurityGroupProto(protocol)) {
        	s_logger.debug("Invalid protocol specified " + protocol);
        	 throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid protocol " + protocol);
        }
        if ("icmp".equalsIgnoreCase(protocol) ) {
            if ((icmpType == null) || (icmpCode == null)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid ICMP type/code specified, icmpType = " + icmpType + ", icmpCode = " + icmpCode);
            }
            if (icmpType == -1 && icmpCode != -1) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid icmp type range" );
            } 
            if (icmpCode > 255) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid icmp code " );
            }
            startPortOrType = icmpType;
            endPortOrCode= icmpCode;
        } else if (protocol.equals("all")) {
        	if ((startPort != null) || (endPort != null)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Cannot specify startPort or endPort without specifying protocol");
            }
        	startPortOrType = 0;
        	endPortOrCode = 0;
        } else {
            if ((startPort == null) || (endPort == null)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid port range specified, startPort = " + startPort + ", endPort = " + endPort);
            }
            if (startPort == 0 && endPort == 0) {
                endPort = 65535;
            }
            if (startPort > endPort) {
                s_logger.debug("Invalid port range specified: " + startPort + ":" + endPort);
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid port range " );
            }
            if (startPort > 65535 || endPort > 65535 || startPort < -1 || endPort < -1) {
                s_logger.debug("Invalid port numbers specified: " + startPort + ":" + endPort);
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid port numbers " );
            }
            
            if (startPort < 0 || endPort < 0) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid port range " );
            }
            startPortOrType = startPort;
            endPortOrCode= endPort;
        }

        if ((account == null) || isAdmin(account.getType())) {
            if ((accountName != null) && (domainId != null)) {
                // if it's an admin account, do a quick permission check
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find rules for network security group id = " + securityGroup + ", permission denied.");
                    }
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find rules for network security group id = " + securityGroup + ", permission denied.");
                }
                Account groupOwner =  _accountDao.findActiveAccount(accountName, domainId);
                if (groupOwner == null) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                }
                accountId = groupOwner.getId();
            } else {
                if (account != null) {
                    accountId = account.getId();
                    domainId = account.getDomainId();
                }
            }
        } else {
            if (account != null) {
                accountId = account.getId();
                domainId = account.getDomainId();
            }
        }

        if (accountId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account for network security group " + securityGroup + "; failed to revoke ingress.");
        }

        SecurityGroupVO sg = _securityGroupDao.findByAccountAndName(accountId, securityGroup);
        if (sg == null) {
            s_logger.debug("Unable to find network security group with id " + securityGroup);
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find network security group with id " + securityGroup);
        }

        if (cidrList == null && groupList == null) {
        	s_logger.debug("At least one cidr or at least one security group needs to be specified");
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "At least one cidr or at least one security group needs to be specified");
        }
        List<String> authorizedCidrs = new ArrayList<String>();
        if (cidrList != null) {
        	if (protocol.equals("all")) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Cannot specify cidrs without specifying protocol and ports.");	
        	}
        	cidrs = cidrList.split(",");
        	for (String cidr: cidrs) {
        		if (!NetUtils.isValidCIDR(cidr)) {
                    s_logger.debug( "Invalid cidr (" + cidr + ") given, unable to revoke ingress.");	
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid cidr (" + cidr + ") given, unable to revoke ingress.");	
        		}
        		authorizedCidrs.add(cidr);
        	}
        }

        List<SecurityGroupVO> authorizedGroups = new ArrayList<SecurityGroupVO> ();
        if (groupList != null) {
            Collection userGroupCollection = groupList.values();
            Iterator iter = userGroupCollection.iterator();
            while (iter.hasNext()) {
                HashMap userGroup = (HashMap)iter.next();
        		String group = (String)userGroup.get("group");
        		String authorizedAccountName = (String)userGroup.get("account");
        		if ((group == null) || (authorizedAccountName == null)) {
        			 throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid user group specified, fields 'group' and 'account' cannot be null, please specify groups in the form:  userGroupList[0].group=XXX&userGroupList[0].account=YYY");
        		}

        		Account authorizedAccount = _accountDao.findActiveAccount(authorizedAccountName, domainId);
                if (authorizedAccount == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Nonexistent account: " + authorizedAccountName + ", domainid: " + domainId + " when trying to revoke ingress for " + securityGroup + ":" + protocol + ":" + startPortOrType + ":" + endPortOrCode);
                    }
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Nonexistent account: " + authorizedAccountName + " when trying to revoke ingress for " + securityGroup + ":" + protocol + ":" + startPortOrType + ":" + endPortOrCode);
                }

                SecurityGroupVO groupVO = _securityGroupDao.findByAccountAndName(authorizedAccount.getId(), group);
                if (groupVO == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Nonexistent group and/or accountId: " + accountId + ", groupName=" + group);
                    }
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid account/group pair  (" + userGroup + ") given, unable to revoke ingress.");
                }
                authorizedGroups.add(groupVO);
        	}
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }
		
		if (!_enabled) {
			return false;
		}
		int numDeleted = 0;
		final int numToDelete = cidrList.length() + authorizedGroups.size();
        final Transaction txn = Transaction.currentTxn();

		SecurityGroupVO securityGroupHandle = _securityGroupDao.findByAccountAndName(accountId, securityGroup);
		if (securityGroupHandle == null) {
			s_logger.warn("Network security group not found: name= " + securityGroup);
			return false;
		}
		try {
			txn.start();
			
			securityGroupHandle = _securityGroupDao.acquireInLockTable(securityGroupHandle.getId());
			if (securityGroupHandle == null)  {
				s_logger.warn("Could not acquire lock on network security group: name= " + securityGroup);
				return false;
			}
			for (final SecurityGroupVO ngVO: authorizedGroups) {
				numDeleted += _ingressRuleDao.deleteByPortProtoAndGroup(securityGroupHandle.getId(), protocol, startPort, endPort, ngVO.getId());
			}
			for (final String cidr: cidrs) {
				numDeleted += _ingressRuleDao.deleteByPortProtoAndCidr(securityGroupHandle.getId(), protocol, startPort, endPort, cidr);
			}
			s_logger.debug("revokeSecurityGroupIngress for group: " + securityGroup + ", numToDelete=" + numToDelete + ", numDeleted=" + numDeleted);
			
			final Set<Long> affectedVms = new HashSet<Long>();
			affectedVms.addAll(_securityGroupVMMapDao.listVmIdsBySecurityGroup(securityGroupHandle.getId()));
			scheduleRulesetUpdateToHosts(affectedVms, true, null);
			
			return true;
		} catch (Exception e) {
			s_logger.warn("Exception caught when deleting ingress rules ", e);
			throw new CloudRuntimeException("Exception caught when deleting ingress rules", e);
		} finally {
			if (securityGroup != null) {
				_securityGroupDao.releaseFromLockTable(securityGroupHandle.getId());
			}
			txn.commit();
		}
		
	}
	
	private static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}

	@Override
    public SecurityGroupVO createSecurityGroup(CreateSecurityGroupCmd cmd) throws PermissionDeniedException, InvalidParameterValueException {
        if (!_enabled) {
            return null;
        }

        String accountName = cmd.getAccountName();
	    Long domainId = cmd.getDomainId();
	    Long accountId = null;

	    Account account = UserContext.current().getCaller();
        if (account != null) {
            if ((account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                if ((domainId != null) && (accountName != null)) {
                    if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                        throw new PermissionDeniedException("Unable to create security group in domain " + domainId + ", permission denied.");
                    }

                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId + ", failed to create security group " + cmd.getSecurityGroupName());
                    }

                    accountId = userAccount.getId();
                } else {
                    // the admin must be creating a security group for himself/herself
                    if (account != null) {
                        accountId = account.getId();
                        domainId = account.getDomainId();
                        accountName = account.getAccountName();
                    }
                }
            } else {
                accountId = account.getId();
                domainId = account.getDomainId();
                accountName = account.getAccountName();
            }
        }

        // if no account exists in the context, it's a system level command, look up the account
        if (accountId == null) {
            if ((accountName != null) && (domainId != null)) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId + ", failed to create security group " + cmd.getSecurityGroupName());
                }
            } else {
                throw new InvalidParameterValueException("Missing account information (account: " + accountName + ", domain: " + domainId + "), failed to create security group " + cmd.getSecurityGroupName());
            }
        }

        if (_securityGroupDao.isNameInUse(accountId, domainId, cmd.getSecurityGroupName())) {
            throw new InvalidParameterValueException("Unable to create security group, a group with name " + cmd.getSecurityGroupName() + " already exisits.");
        }

        return createSecurityGroup(cmd.getSecurityGroupName(), cmd.getDescription(), domainId, accountId, accountName);
	}

	@DB
	@Override
	public SecurityGroupVO createSecurityGroup(String name, String description, Long domainId, Long accountId, String accountName) {
		if (!_enabled) {
			return null;
		}
		final Transaction txn = Transaction.currentTxn();
		AccountVO account = null;
		txn.start();
		try {
			account = _accountDao.acquireInLockTable(accountId); //to ensure duplicate group names are not created.
			if (account == null) {
				s_logger.warn("Failed to acquire lock on account");
				return null;
			}
			SecurityGroupVO group = _securityGroupDao.findByAccountAndName(accountId, name);
			if (group == null){
				group = new SecurityGroupVO(name, description, domainId, accountId, accountName);
				group =  _securityGroupDao.persist(group);
			}
			return group;
		} finally {
			if (account != null) {
				_accountDao.releaseFromLockTable(accountId);
			}
			txn.commit();
		}
		
    }
	
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		String enabled =_configDao.getValue("direct.attach.security.groups.enabled");
		if ("true".equalsIgnoreCase(enabled)) {
			_enabled = true;
		}
		if (!_enabled) {
			return false;
		}
		_answerListener = new SecurityGroupListener(this, _agentMgr, _workDao);
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
	public SecurityGroupVO createDefaultSecurityGroup(Long accountId) {
		if (!_enabled) {
			return null;
		}
		SecurityGroupVO groupVO = _securityGroupDao.findByAccountAndName(accountId, SecurityGroupManager.DEFAULT_GROUP_NAME);
		if (groupVO == null ) {
			Account accVO = _accountDao.findById(accountId);
			if (accVO != null) {
				return createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION, accVO.getDomainId(), accVO.getId(), accVO.getAccountName());
			}
		}
		return groupVO;
	}
	
	@DB
	public void work() {
	    if (s_logger.isTraceEnabled()) {
	        s_logger.trace("Checking the database");
	    }
		final SecurityGroupWorkVO work = _workDao.take(_serverId);
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
			vm = _userVMDao.acquireInLockTable(work.getInstanceId());
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
					SecurityIngressRulesCmd cmd = generateRulesetCmd(vm.getInstanceName(), vm.getGuestIpAddress(), vm.getGuestMacAddress(), vm.getId(), generateRulesetSignature(rules), seqnum, rules);
					Commands cmds = new Commands(cmd);
					try {
						_agentMgr.send(agentId, cmds, _answerListener);
					} catch (AgentUnavailableException e) {
						s_logger.debug("Unable to send updates for vm: " + userVmId + "(agentid=" + agentId + ")");
						_workDao.updateStep(work.getInstanceId(), seqnum, Step.Done);
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

	@Override
	@DB
	public boolean addInstanceToGroups(final Long userVmId, final List<SecurityGroupVO> groups) {
		if (!_enabled) {
			return true;
		}
		if (groups != null) {
			final Set<SecurityGroupVO> uniqueGroups = new TreeSet<SecurityGroupVO>(new SecurityGroupVOComparator());
			uniqueGroups.addAll(groups);
			final Transaction txn = Transaction.currentTxn();
			txn.start();
			UserVm userVm = _userVMDao.acquireInLockTable(userVmId); //ensures that duplicate entries are not created.
			if (userVm == null) {
				s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
			}
			try {
				for (SecurityGroupVO securityGroup:uniqueGroups) {
					//don't let the group be deleted from under us.
					SecurityGroupVO ngrpLock = _securityGroupDao.lockRow(securityGroup.getId(), false);
					if (ngrpLock == null) {
						s_logger.warn("Failed to acquire lock on network group id=" + securityGroup.getId() + " name=" + securityGroup.getName());
						txn.rollback();
						return false;
					}
					if (_securityGroupVMMapDao.findByVmIdGroupId(userVmId, securityGroup.getId()) == null) {
						SecurityGroupVMMapVO groupVmMapVO = new SecurityGroupVMMapVO(securityGroup.getId(), userVmId);
						_securityGroupVMMapDao.persist(groupVmMapVO);
					}
				}
				txn.commit();
				return true;
			} finally {
				if (userVm != null) {
					_userVMDao.releaseFromLockTable(userVmId);
				}
			}
			

        }
		return false;
		
	}

	@Override
	@DB
	public void removeInstanceFromGroups(Long userVmId) {
		if (!_enabled) {
			return;
		}
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		UserVm userVm = _userVMDao.acquireInLockTable(userVmId); //ensures that duplicate entries are not created in addInstance
		if (userVm == null) {
			s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
		}
		int n = _securityGroupVMMapDao.deleteVM(userVmId);
		s_logger.info("Disassociated " + n + " network groups " + " from uservm " + userVmId);
		_userVMDao.releaseFromLockTable(userVmId);
		txn.commit();
	}

	@DB
	@Override
	public boolean deleteSecurityGroup(DeleteSecurityGroupCmd cmd) throws ResourceInUseException, PermissionDeniedException, InvalidParameterValueException{
		String name = cmd.getSecurityGroupName();
		String accountName = cmd.getAccountName();
		Long domainId = cmd.getDomainId();
		Account account = UserContext.current().getCaller();
		
		if (!_enabled) {
			return true;
		}
		
		//Verify input parameters
        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            if ((accountName != null) && (domainId != null)) {
                // if it's an admin account, do a quick permission check
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find rules network group " + name + ", permission denied.");
                    }
                    throw new PermissionDeniedException("Unable to network group " + name + ", permission denied.");
                }

                Account groupOwner = _accountDao.findActiveAccount(accountName, domainId);
                if (groupOwner == null) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                }
                accountId = groupOwner.getId();
            } else {
                if (account != null) {
                    accountId = account.getId();
                    domainId = account.getDomainId();
                }
            }
        } else {
            if (account != null) {
                accountId = account.getId();
                domainId = account.getDomainId();
            }
        }

        if (accountId == null) {
            throw new InvalidParameterValueException("Unable to find account for network group " + name + "; failed to delete group.");
        }

        SecurityGroupVO sg = _securityGroupDao.findByAccountAndName(accountId, name);
        if (sg == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find network group " + name + "; failed to delete group.");
        }
        
        Long groupId = sg.getId();
		
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		
		final SecurityGroupVO group = _securityGroupDao.lockRow(groupId, true);
		if (group == null) {
			s_logger.info("Not deleting group -- cannot find id " + groupId);
			return false;
		}
		
		if (group.getName().equalsIgnoreCase(SecurityGroupManager.DEFAULT_GROUP_NAME)) {
			txn.rollback();
			throw new PermissionDeniedException("The network group default is reserved");
		}
		
		List<IngressRuleVO> allowingRules = _ingressRuleDao.listByAllowedSecurityGroupId(groupId);
		if (allowingRules.size() != 0) {
			txn.rollback();
			throw new ResourceInUseException("Cannot delete group when there are ingress rules that allow this group");
		}
		
		List<IngressRuleVO> rulesInGroup = _ingressRuleDao.listBySecurityGroupId(groupId);
		if (rulesInGroup.size() != 0) {
			txn.rollback();
			throw new ResourceInUseException("Cannot delete group when there are ingress rules in this group");
		}
        _securityGroupDao.expunge(groupId);
        txn.commit();
        return true;
	}

    @Override
    public List<SecurityGroupRulesVO> searchForSecurityGroupRules(ListSecurityGroupsCmd cmd) throws PermissionDeniedException, InvalidParameterValueException {
        Account account = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        Long instanceId = cmd.getVirtualMachineId();
        String networkGroup = cmd.getSecurityGroupName();
        Boolean recursive = Boolean.FALSE;
        Long id = cmd.getId();

        // permissions check
        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list network groups for account " + accountName + " in domain " + domainId + "; permission denied.");
                }
                if (accountName != null) {
                    Account acct = _accountDao.findActiveAccount(accountName, domainId);
                    if (acct != null) {
                        accountId = acct.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else if (instanceId != null) {
                UserVmVO userVM = _userVMDao.findById(instanceId);
                if (userVM == null) {
                    throw new InvalidParameterValueException("Unable to list network groups for virtual machine instance " + instanceId + "; instance not found.");
                }
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), userVM.getDomainId())) {
                    throw new PermissionDeniedException("Unable to list network groups for virtual machine instance " + instanceId + "; permission denied.");
                }
            } else if (account != null) {
                // either an admin is searching for their own group, or admin is listing all groups and the search needs to be restricted to domain admin's domain
                if (networkGroup != null) {
                    accountId = account.getId();
                } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    domainId = account.getDomainId();
                    recursive = Boolean.TRUE;
                }
            }
        } else {
            if (instanceId != null) {
                UserVmVO userVM = _userVMDao.findById(instanceId);
                if (userVM == null) {
                    throw new InvalidParameterValueException("Unable to list network groups for virtual machine instance " + instanceId + "; instance not found.");
                }

                if (account != null) {
                    // check that the user is the owner of the VM (admin case was already verified
                    if (account.getId() != userVM.getAccountId()) {
                        throw new PermissionDeniedException("Unable to list network groups for virtual machine instance " + instanceId + "; permission denied.");
                    }
                }
            } else {
                accountId = ((account != null) ? account.getId() : null);
            }
        }

        List<SecurityGroupRulesVO> securityRulesList = new ArrayList<SecurityGroupRulesVO>();
        
        if(id != null){
        	SecurityGroupRulesVO secGrp = _securityGroupRulesDao.findById(id); 
        	if(secGrp != null) {
                securityRulesList.add(secGrp);
            }
        	return securityRulesList;
        }
        
        Filter searchFilter = new Filter(SecurityGroupVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Object keyword = cmd.getKeyword();

        SearchBuilder<SecurityGroupVO> sb = _securityGroupDao.createSearchBuilder();
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);

        // only do a recursive domain search if the search is not limited by account or instance
        if ((accountId == null) && (instanceId == null) && (domainId != null) && Boolean.TRUE.equals(recursive)) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<SecurityGroupVO> sc = sb.create();
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
            if (networkGroup != null) {
                sc.setParameters("name", networkGroup);
            } else if (keyword != null) {
                SearchCriteria<SecurityGroupRulesVO> ssc = _securityGroupRulesDao.createSearchCriteria();
                ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                sc.addAnd("name", SearchCriteria.Op.SC, ssc);
            }
        } else if (domainId != null) {
            if (Boolean.TRUE.equals(recursive)) {
                DomainVO domain = _domainDao.findById(domainId);
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }
        
        List<SecurityGroupVO> securityGroups = _securityGroupDao.search(sc, searchFilter);
        for (SecurityGroupVO group : securityGroups) {
           securityRulesList.addAll(_securityGroupRulesDao.listSecurityRulesByGroupId(group.getId()));
        }
        
       if (instanceId != null) {
            return listSecurityGroupRulesByVM(instanceId.longValue());
       } 
       
        return securityRulesList;
    }

	private List<SecurityGroupRulesVO> listSecurityGroupRulesByVM(long vmId) {
	    List<SecurityGroupRulesVO> results = new ArrayList<SecurityGroupRulesVO>();
	    List<SecurityGroupVMMapVO> networkGroupMappings = _securityGroupVMMapDao.listByInstanceId(vmId);
	    if (networkGroupMappings != null) {
	        for (SecurityGroupVMMapVO networkGroupMapping : networkGroupMappings) {
	            SecurityGroupVO group = _securityGroupDao.findById(networkGroupMapping.getSecurityGroupId());
	            List<SecurityGroupRulesVO> rules = _securityGroupRulesDao.listSecurityGroupRules(group.getAccountId(), networkGroupMapping.getGroupName());
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
		List<SecurityGroupWorkVO> unfinished = _workDao.findUnfinishedWork(before);
		if (unfinished.size() > 0) {
			s_logger.info("Network Group Work cleanup found " + unfinished.size() + " unfinished work items older than " + before.toString());
			Set<Long> affectedVms = new HashSet<Long>();
			for (SecurityGroupWorkVO work: unfinished) {
				affectedVms.add(work.getInstanceId());
			}
			scheduleRulesetUpdateToHosts(affectedVms, false, null);
		} else {
			s_logger.debug("Network Group Work cleanup found no unfinished work items older than " + before.toString());
		}
	}

	@Override
	public String getSecurityGroupsNamesForVm(long vmId) 
	{
		try
		{
			List<SecurityGroupVMMapVO>networkGroupsToVmMap =  _securityGroupVMMapDao.listByInstanceId(vmId);
        	int size = 0;
        	int j=0;		
            StringBuilder networkGroupNames = new StringBuilder();

            if(networkGroupsToVmMap != null)
            {
            	size = networkGroupsToVmMap.size();
            	
            	for(SecurityGroupVMMapVO nG: networkGroupsToVmMap)
            	{
            		//get the group id and look up for the group name
            		SecurityGroupVO currentNetworkGroup = _securityGroupDao.findById(nG.getSecurityGroupId());
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
