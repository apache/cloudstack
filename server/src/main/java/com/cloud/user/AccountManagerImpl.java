// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.user;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.QuerySelector;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserKeysCmd;
import org.apache.cloudstack.api.command.admin.user.MoveUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.region.gslb.GlobalLoadBalancerRuleDao;
import org.apache.cloudstack.utils.baremetal.BaremetalUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.ActionEvents;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.VpnUserVO;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.RemoteAccessVpnVO;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectInvitationVO;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.server.auth.UserAuthenticator.ActionOnFailedAuthentication;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account.State;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.ConstantTimeComparator;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class AccountManagerImpl extends ManagerBase implements AccountManager, Manager {
    public static final Logger s_logger = Logger.getLogger(AccountManagerImpl.class);

    @Inject
    private AccountDao _accountDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private InstanceGroupDao _vmGroupDao;
    @Inject
    private UserAccountDao _userAccountDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private SecurityGroupDao _securityGroupDao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private SecurityGroupManager _networkGroupMgr;
    @Inject
    private NetworkOrchestrationService _networkMgr;
    @Inject
    private SnapshotManager _snapMgr;
    @Inject
    private VMSnapshotManager _vmSnapshotMgr;
    @Inject
    private VMSnapshotDao _vmSnapshotDao;
    @Inject
    private UserVmManager _vmMgr;
    @Inject
    private TemplateManager _tmpltMgr;
    @Inject
    private ConfigurationManager _configMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private RemoteAccessVpnDao _remoteAccessVpnDao;
    @Inject
    private RemoteAccessVpnService _remoteAccessVpnMgr;
    @Inject
    private VpnUserDao _vpnUser;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private DomainManager _domainMgr;
    @Inject
    private ProjectManager _projectMgr;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    private AccountDetailsDao _accountDetailsDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private ProjectAccountDao _projectAccountDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private VpcManager _vpcMgr;
    @Inject
    private Site2SiteVpnManager _vpnMgr;
    @Inject
    private AutoScaleManager _autoscaleMgr;
    @Inject
    private VolumeApiService volumeService;
    @Inject
    private AffinityGroupDao _affinityGroupDao;
    @Inject
    private AccountGuestVlanMapDao _accountGuestVlanMapDao;
    @Inject
    private DataCenterVnetDao _dataCenterVnetDao;
    @Inject
    private ResourceLimitService _resourceLimitMgr;
    @Inject
    private ResourceLimitDao _resourceLimitDao;
    @Inject
    private DedicatedResourceDao _dedicatedDao;
    @Inject
    private GlobalLoadBalancerRuleDao _gslbRuleDao;
    @Inject
    private SSHKeyPairDao _sshKeyPairDao;

    private List<QuerySelector> _querySelectors;

    @Inject
    private MessageBus _messageBus;

    @Inject
    private GlobalLoadBalancingRulesService _gslbService;

    private List<UserAuthenticator> _userAuthenticators;
    protected List<UserAuthenticator> _userPasswordEncoders;

    @Inject
    private IpAddressManager _ipAddrMgr;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));

    private int _allowedLoginAttempts;

    private UserVO _systemUser;
    private AccountVO _systemAccount;

    private List<SecurityChecker> _securityCheckers;
    private int _cleanupInterval;

    public List<UserAuthenticator> getUserAuthenticators() {
        return _userAuthenticators;
    }

    public void setUserAuthenticators(List<UserAuthenticator> authenticators) {
        _userAuthenticators = authenticators;
    }

    public List<UserAuthenticator> getUserPasswordEncoders() {
        return _userPasswordEncoders;
    }

    public void setUserPasswordEncoders(List<UserAuthenticator> encoders) {
        _userPasswordEncoders = encoders;
    }

    public List<SecurityChecker> getSecurityCheckers() {
        return _securityCheckers;
    }

    public void setSecurityCheckers(List<SecurityChecker> securityCheckers) {
        _securityCheckers = securityCheckers;
    }

    public List<QuerySelector> getQuerySelectors() {
        return _querySelectors;
    }

    public void setQuerySelectors(List<QuerySelector> querySelectors) {
        _querySelectors = querySelectors;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
        if (_systemAccount == null) {
            throw new ConfigurationException("Unable to find the system account using " + Account.ACCOUNT_ID_SYSTEM);
        }

        _systemUser = _userDao.findById(User.UID_SYSTEM);
        if (_systemUser == null) {
            throw new ConfigurationException("Unable to find the system user using " + User.UID_SYSTEM);
        }

        Map<String, String> configs = _configDao.getConfiguration(params);

        String loginAttempts = configs.get(Config.IncorrectLoginAttemptsAllowed.key());
        _allowedLoginAttempts = NumbersUtil.parseInt(loginAttempts, 5);

        String value = configs.get(Config.AccountCleanupInterval.key());
        _cleanupInterval = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 day.

        return true;
    }

    @Override
    public UserVO getSystemUser() {
        if (_systemUser == null) {
            _systemUser = _userDao.findById(User.UID_SYSTEM);
        }
        return _systemUser;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new AccountCleanupTask(), _cleanupInterval, _cleanupInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public AccountVO getSystemAccount() {
        if (_systemAccount == null) {
            _systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
        }
        return _systemAccount;
    }

    @Override
    public boolean isAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            if ((isRootAdmin(accountId)) || (isDomainAdmin(accountId)) || (isResourceDomainAdmin(accountId))) {
                return true;
            } else if (acct.getType() == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN) {
                return true;
            }

        }
        return false;
    }

    @Override
    public boolean isRootAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            for (SecurityChecker checker : _securityCheckers) {
                try {
                    if (checker.checkAccess(acct, null, null, "SystemCapability")) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Root Access granted to " + acct + " by " + checker.getName());
                        }
                        return true;
                    }
                } catch (PermissionDeniedException ex) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDomainAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            for (SecurityChecker checker : _securityCheckers) {
                try {
                    if (checker.checkAccess(acct, null, null, "DomainCapability")) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("DomainAdmin Access granted to " + acct + " by " + checker.getName());
                        }
                        return true;
                    }
                } catch (PermissionDeniedException ex) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isNormalUser(long accountId) {
        AccountVO acct = _accountDao.findById(accountId);
        if (acct != null && acct.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            return true;
        }
        return false;
    }

    public boolean isResourceDomainAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            for (SecurityChecker checker : _securityCheckers) {
                try {
                    if (checker.checkAccess(acct, null, null, "DomainResourceCapability")) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("ResourceDomainAdmin Access granted to " + acct + " by " + checker.getName());
                        }
                        return true;
                    }
                } catch (PermissionDeniedException ex) {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean isInternalAccount(long accountId) {
        Account account = _accountDao.findById(accountId);
        if (account == null) {
            return false;  //account is deleted or does not exist
        }
        if (isRootAdmin(accountId) || (account.getType() == Account.ACCOUNT_ID_SYSTEM)) {
            return true;
        }
        return false;
    }

    @Override
    public void checkAccess(Account caller, Domain domain) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(caller, domain)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to " + domain + " by " + checker.getName());
                }
                return;
            }
        }
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to " + domain);
    }

    @Override
    public void checkAccess(Account caller, AccessType accessType, boolean sameOwner, ControlledEntity... entities) {
        checkAccess(caller, accessType, sameOwner, null, entities);
    }

    @Override
    public void checkAccess(Account caller, AccessType accessType, boolean sameOwner, String apiName, ControlledEntity... entities) {

        //check for the same owner
        Long ownerId = null;
        ControlledEntity prevEntity = null;
        if (sameOwner) {
            for (ControlledEntity entity : entities) {
                if (ownerId == null) {
                    ownerId = entity.getAccountId();
                } else if (ownerId.longValue() != entity.getAccountId()) {
                    throw new PermissionDeniedException("Entity " + entity + " and entity " + prevEntity + " belong to different accounts");
                }
                prevEntity = entity;
            }
        }

        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || isRootAdmin(caller.getId())) {
            // no need to make permission checks if the system/root admin makes the call
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("No need to make permission check for System/RootAdmin account, returning true");
            }

            return;
        }

        HashMap<Long, List<ControlledEntity>> domains = new HashMap<Long, List<ControlledEntity>>();

        for (ControlledEntity entity : entities) {
            long domainId = entity.getDomainId();
            if (entity.getAccountId() != -1 && domainId == -1) { // If account exists domainId should too so calculate
                // it. This condition might be hit for templates or entities which miss domainId in their tables
                Account account = ApiDBUtils.findAccountById(entity.getAccountId());
                domainId = account != null ? account.getDomainId() : -1;
            }
            if (entity.getAccountId() != -1 && domainId != -1 && !(entity instanceof VirtualMachineTemplate) && !(entity instanceof Network && accessType != null && accessType == AccessType.UseEntry)
                    && !(entity instanceof AffinityGroup)) {
                List<ControlledEntity> toBeChecked = domains.get(entity.getDomainId());
                // for templates, we don't have to do cross domains check
                if (toBeChecked == null) {
                    toBeChecked = new ArrayList<ControlledEntity>();
                    domains.put(domainId, toBeChecked);
                }
                toBeChecked.add(entity);
            }
            boolean granted = false;
            for (SecurityChecker checker : _securityCheckers) {
                if (checker.checkAccess(caller, entity, accessType, apiName)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Access to " + entity + " granted to " + caller + " by " + checker.getName());
                    }
                    granted = true;
                    break;
                }
            }

            if (!granted) {
                assert false : "How can all of the security checkers pass on checking this check: " + entity;
            throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to " + entity);
            }
        }

        for (Map.Entry<Long, List<ControlledEntity>> domain : domains.entrySet()) {
            for (SecurityChecker checker : _securityCheckers) {
                Domain d = _domainMgr.getDomain(domain.getKey());
                if (d == null || d.getRemoved() != null) {
                    throw new PermissionDeniedException("Domain is not found.", caller, domain.getValue());
                }
                try {
                    checker.checkAccess(caller, d);
                } catch (PermissionDeniedException e) {
                    e.addDetails(caller, domain.getValue());
                    throw e;
                }
            }
        }

        // check that resources belong to the same account

    }

    @Override
    public Long checkAccessAndSpecifyAuthority(Account caller, Long zoneId) {
        // We just care for resource domain admins for now, and they should be permitted to see only their zone.
        if (isResourceDomainAdmin(caller.getAccountId())) {
            if (zoneId == null) {
                return getZoneIdForAccount(caller);
            } else if (zoneId.compareTo(getZoneIdForAccount(caller)) != 0) {
                throw new PermissionDeniedException("Caller " + caller + "is not allowed to access the zone " + zoneId);
            } else {
                return zoneId;
            }
        } else {
            return zoneId;
        }
    }

    private Long getZoneIdForAccount(Account account) {

        // Currently just for resource domain admin
        List<DataCenterVO> dcList = _dcDao.findZonesByDomainId(account.getDomainId());
        if (dcList != null && dcList.size() != 0) {
            return dcList.get(0).getId();
        } else {
            throw new CloudRuntimeException("Failed to find any private zone for Resource domain admin.");
        }

    }

    @DB
    public void updateLoginAttempts(final Long id, final int attempts, final boolean toDisable) {
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    UserAccountVO user = null;
                    user = _userAccountDao.lockRow(id, true);
                    user.setLoginAttempts(attempts);
                    if (toDisable) {
                        user.setState(State.disabled.toString());
                    }
                    _userAccountDao.update(id, user);
                }
            });
        } catch (Exception e) {
            s_logger.error("Failed to update login attempts for user with id " + id);
        }
    }

    private boolean doSetUserStatus(long userId, State state) {
        UserVO userForUpdate = _userDao.createForUpdate();
        userForUpdate.setState(state);
        return _userDao.update(Long.valueOf(userId), userForUpdate);
    }

    @Override
    public boolean enableAccount(long accountId) {
        boolean success = false;
        AccountVO acctForUpdate = _accountDao.createForUpdate();
        acctForUpdate.setState(State.enabled);
        acctForUpdate.setNeedsCleanup(false);
        success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
        return success;
    }

    protected boolean lockAccount(long accountId) {
        boolean success = false;
        Account account = _accountDao.findById(accountId);
        if (account != null) {
            if (account.getState().equals(State.locked)) {
                return true; // already locked, no-op
            } else if (account.getState().equals(State.enabled)) {
                AccountVO acctForUpdate = _accountDao.createForUpdate();
                acctForUpdate.setState(State.locked);
                success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Attempting to lock a non-enabled account, current state is " + account.getState() + " (accountId: " + accountId + "), locking failed.");
                }
            }
        } else {
            s_logger.warn("Failed to lock account " + accountId + ", account not found.");
        }
        return success;
    }

    @Override
    public boolean deleteAccount(AccountVO account, long callerUserId, Account caller) {
        long accountId = account.getId();

        // delete the account record
        if (!_accountDao.remove(accountId)) {
            s_logger.error("Unable to delete account " + accountId);
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Removed account " + accountId);
        }

        return cleanupAccount(account, callerUserId, caller);
    }

    protected boolean cleanupAccount(AccountVO account, long callerUserId, Account caller) {
        long accountId = account.getId();
        boolean accountCleanupNeeded = false;

        try {
            // cleanup the users from the account
            List<UserVO> users = _userDao.listByAccount(accountId);
            for (UserVO user : users) {
                if (!_userDao.remove(user.getId())) {
                    s_logger.error("Unable to delete user: " + user + " as a part of account " + account + " cleanup");
                    accountCleanupNeeded = true;
                }
            }

            // delete global load balancer rules for the account.
            List<org.apache.cloudstack.region.gslb.GlobalLoadBalancerRuleVO> gslbRules = _gslbRuleDao.listByAccount(accountId);
            if (gslbRules != null && !gslbRules.isEmpty()) {
                _gslbService.revokeAllGslbRulesForAccount(caller, accountId);
            }

            // delete the account from project accounts
            _projectAccountDao.removeAccountFromProjects(accountId);

            if (account.getType() != Account.ACCOUNT_TYPE_PROJECT) {
                // delete the account from group
                _messageBus.publish(_name, MESSAGE_REMOVE_ACCOUNT_EVENT, PublishScope.LOCAL, accountId);
            }

            // delete all vm groups belonging to accont
            List<InstanceGroupVO> groups = _vmGroupDao.listByAccountId(accountId);
            for (InstanceGroupVO group : groups) {
                if (!_vmMgr.deleteVmGroup(group.getId())) {
                    s_logger.error("Unable to delete group: " + group.getId());
                    accountCleanupNeeded = true;
                }
            }

            // Delete the snapshots dir for the account. Have to do this before destroying the VMs.
            boolean success = _snapMgr.deleteSnapshotDirsForAccount(accountId);
            if (success) {
                s_logger.debug("Successfully deleted snapshots directories for all volumes under account " + accountId + " across all zones");
            }

            // clean up templates
            List<VMTemplateVO> userTemplates = _templateDao.listByAccountId(accountId);
            boolean allTemplatesDeleted = true;
            for (VMTemplateVO template : userTemplates) {
                if (template.getRemoved() == null) {
                    try {
                        allTemplatesDeleted = _tmpltMgr.delete(callerUserId, template.getId(), null);
                    } catch (Exception e) {
                        s_logger.warn("Failed to delete template while removing account: " + template.getName() + " due to: ", e);
                        allTemplatesDeleted = false;
                    }
                }
            }

            if (!allTemplatesDeleted) {
                s_logger.warn("Failed to delete templates while removing account id=" + accountId);
                accountCleanupNeeded = true;
            }

            // Destroy VM Snapshots
            List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.listByAccountId(Long.valueOf(accountId));
            for (VMSnapshot vmSnapshot : vmSnapshots) {
                try {
                    _vmSnapshotMgr.deleteVMSnapshot(vmSnapshot.getId());
                } catch (Exception e) {
                    s_logger.debug("Failed to cleanup vm snapshot " + vmSnapshot.getId() + " due to " + e.toString());
                }
            }

            // Destroy the account's VMs
            List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Expunging # of vms (accountId=" + accountId + "): " + vms.size());
            }

            for (UserVmVO vm : vms) {
                if (vm.getState() != VirtualMachine.State.Destroyed && vm.getState() != VirtualMachine.State.Expunging) {
                    try {
                        _vmMgr.destroyVm(vm.getId(), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        s_logger.warn("Failed destroying instance " + vm.getUuid() + " as part of account deletion.");
                    }
                }
                // no need to catch exception at this place as expunging vm
                // should pass in order to perform further cleanup
                if (!_vmMgr.expunge(vm, callerUserId, caller)) {
                    s_logger.error("Unable to expunge vm: " + vm.getId());
                    accountCleanupNeeded = true;
                }
            }

            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volumeDao.findDetachedByAccount(accountId);
            for (VolumeVO volume : volumes) {
                try {
                    volumeService.deleteVolume(volume.getId(), caller);
                } catch (Exception ex) {
                    s_logger.warn("Failed to cleanup volumes as a part of account id=" + accountId + " cleanup due to Exception: ", ex);
                    accountCleanupNeeded = true;
                }
            }

            // delete remote access vpns and associated users
            List<RemoteAccessVpnVO> remoteAccessVpns = _remoteAccessVpnDao.findByAccount(accountId);
            List<VpnUserVO> vpnUsers = _vpnUser.listByAccount(accountId);

            for (VpnUserVO vpnUser : vpnUsers) {
                _remoteAccessVpnMgr.removeVpnUser(accountId, vpnUser.getUsername(), caller);
            }

            try {
                for (RemoteAccessVpnVO vpn : remoteAccessVpns) {
                    _remoteAccessVpnMgr.destroyRemoteAccessVpnForIp(vpn.getServerAddressId(), caller, false);
                }
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Failed to cleanup remote access vpn resources as a part of account id=" + accountId + " cleanup due to Exception: ", ex);
                accountCleanupNeeded = true;
            }

            // Cleanup security groups
            int numRemoved = _securityGroupDao.removeByAccountId(accountId);
            s_logger.info("deleteAccount: Deleted " + numRemoved + " network groups for account " + accountId);

            // Cleanup affinity groups
            int numAGRemoved = _affinityGroupDao.removeByAccountId(accountId);
            s_logger.info("deleteAccount: Deleted " + numAGRemoved + " affinity groups for account " + accountId);

            // Delete all the networks
            boolean networksDeleted = true;
            s_logger.debug("Deleting networks for account " + account.getId());
            List<NetworkVO> networks = _networkDao.listByOwner(accountId);
            if (networks != null) {
                for (NetworkVO network : networks) {

                    ReservationContext context = new ReservationContextImpl(null, null, getActiveUser(callerUserId), caller);

                    if (!_networkMgr.destroyNetwork(network.getId(), context, false)) {
                        s_logger.warn("Unable to destroy network " + network + " as a part of account id=" + accountId + " cleanup.");
                        accountCleanupNeeded = true;
                        networksDeleted = false;
                    } else {
                        s_logger.debug("Network " + network.getId() + " successfully deleted as a part of account id=" + accountId + " cleanup.");
                    }
                }
            }

            // Delete all VPCs
            boolean vpcsDeleted = true;
            s_logger.debug("Deleting vpcs for account " + account.getId());
            List<? extends Vpc> vpcs = _vpcMgr.getVpcsForAccount(account.getId());
            for (Vpc vpc : vpcs) {

                if (!_vpcMgr.destroyVpc(vpc, caller, callerUserId)) {
                    s_logger.warn("Unable to destroy VPC " + vpc + " as a part of account id=" + accountId + " cleanup.");
                    accountCleanupNeeded = true;
                    vpcsDeleted = false;
                } else {
                    s_logger.debug("VPC " + vpc.getId() + " successfully deleted as a part of account id=" + accountId + " cleanup.");
                }
            }

            if (networksDeleted && vpcsDeleted) {
                // release ip addresses belonging to the account
                List<? extends IpAddress> ipsToRelease = _ipAddressDao.listByAccount(accountId);
                for (IpAddress ip : ipsToRelease) {
                    s_logger.debug("Releasing ip " + ip + " as a part of account id=" + accountId + " cleanup");
                    if (!_ipAddrMgr.disassociatePublicIpAddress(ip.getId(), callerUserId, caller)) {
                        s_logger.warn("Failed to release ip address " + ip + " as a part of account id=" + accountId + " clenaup");
                        accountCleanupNeeded = true;
                    }
                }
            }

            // Delete Site 2 Site VPN customer gateway
            s_logger.debug("Deleting site-to-site VPN customer gateways for account " + accountId);
            if (!_vpnMgr.deleteCustomerGatewayByAccount(accountId)) {
                s_logger.warn("Fail to delete site-to-site VPN customer gateways for account " + accountId);
            }

            // Delete autoscale resources if any
            try {
                _autoscaleMgr.cleanUpAutoScaleResources(accountId);
            } catch (CloudRuntimeException ex) {
                s_logger.warn("Failed to cleanup AutoScale resources as a part of account id=" + accountId + " cleanup due to exception:", ex);
                accountCleanupNeeded = true;
            }

            // release account specific Virtual vlans (belong to system Public Network) - only when networks are cleaned
            // up successfully
            if (networksDeleted) {
                if (!_configMgr.releaseAccountSpecificVirtualRanges(accountId)) {
                    accountCleanupNeeded = true;
                } else {
                    s_logger.debug("Account specific Virtual IP ranges " + " are successfully released as a part of account id=" + accountId + " cleanup.");
                }
            }

            // release account specific guest vlans
            List<AccountGuestVlanMapVO> maps = _accountGuestVlanMapDao.listAccountGuestVlanMapsByAccount(accountId);
            for (AccountGuestVlanMapVO map : maps) {
                _dataCenterVnetDao.releaseDedicatedGuestVlans(map.getId());
            }
            int vlansReleased = _accountGuestVlanMapDao.removeByAccountId(accountId);
            s_logger.info("deleteAccount: Released " + vlansReleased + " dedicated guest vlan ranges from account " + accountId);

            // release account specific acquired portable IP's. Since all the portable IP's must have been already
            // disassociated with VPC/guest network (due to deletion), so just mark portable IP as free.
            List<? extends IpAddress> ipsToRelease = _ipAddressDao.listByAccount(accountId);
            for (IpAddress ip : ipsToRelease) {
                if (ip.isPortable()) {
                    s_logger.debug("Releasing portable ip " + ip + " as a part of account id=" + accountId + " cleanup");
                    _ipAddrMgr.releasePortableIpAddress(ip.getId());
                }
            }

            // release dedication if any
            List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listByAccountId(accountId);
            if (dedicatedResources != null && !dedicatedResources.isEmpty()) {
                s_logger.debug("Releasing dedicated resources for account " + accountId);
                for (DedicatedResourceVO dr : dedicatedResources) {
                    if (!_dedicatedDao.remove(dr.getId())) {
                        s_logger.warn("Fail to release dedicated resources for account " + accountId);
                    }
                }
            }

            // Updating and deleting the resourceLimit and resourceCount should be the last step in cleanupAccount
// process.
            // Update resource count for this account and for parent domains.
            List<ResourceCountVO> resourceCounts = _resourceCountDao.listByOwnerId(accountId, ResourceOwnerType.Account);
            for (ResourceCountVO resourceCount : resourceCounts) {
                _resourceLimitMgr.decrementResourceCount(accountId, resourceCount.getType(), resourceCount.getCount());
            }

            // Delete resource count and resource limits entries set for this account (if there are any).
            _resourceCountDao.removeEntriesByOwner(accountId, ResourceOwnerType.Account);
            _resourceLimitDao.removeEntriesByOwner(accountId, ResourceOwnerType.Account);

            // Delete ssh keypairs
            List<SSHKeyPairVO> sshkeypairs = _sshKeyPairDao.listKeyPairs(accountId, account.getDomainId());
            for (SSHKeyPairVO keypair : sshkeypairs) {
                _sshKeyPairDao.remove(keypair.getId());
            }
            return true;
        } catch (Exception ex) {
            s_logger.warn("Failed to cleanup account " + account + " due to ", ex);
            accountCleanupNeeded = true;
            return true;
        } finally {
            s_logger.info("Cleanup for account " + account.getId() + (accountCleanupNeeded ? " is needed." : " is not needed."));
            if (accountCleanupNeeded) {
                _accountDao.markForCleanup(accountId);
            } else {
                account.setNeedsCleanup(false);
                _accountDao.update(accountId, account);
            }
        }
    }

    @Override
    public boolean disableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        boolean success = false;
        if (accountId <= 2) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("disableAccount -- invalid account id: " + accountId);
            }
            return false;
        }

        AccountVO account = _accountDao.findById(accountId);
        if ((account == null) || (account.getState().equals(State.disabled) && !account.getNeedsCleanup())) {
            success = true;
        } else {
            AccountVO acctForUpdate = _accountDao.createForUpdate();
            acctForUpdate.setState(State.disabled);
            success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);

            if (success) {
                boolean disableAccountResult = false;
                try {
                    disableAccountResult = doDisableAccount(accountId);
                } finally {
                    if (!disableAccountResult) {
                        s_logger.warn("Failed to disable account " + account + " resources as a part of disableAccount call, marking the account for cleanup");
                        _accountDao.markForCleanup(accountId);
                    } else {
                        acctForUpdate = _accountDao.createForUpdate();
                        account.setNeedsCleanup(false);
                        _accountDao.update(accountId, account);
                    }
                }
            }
        }
        return success;
    }

    private boolean doDisableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        List<VMInstanceVO> vms = _vmDao.listByAccountId(accountId);
        boolean success = true;
        for (VMInstanceVO vm : vms) {
            try {
                try {
                    _itMgr.advanceStop(vm.getUuid(), false);
                } catch (OperationTimedoutException ote) {
                    s_logger.warn("Operation for stopping vm timed out, unable to stop vm " + vm.getHostName(), ote);
                    success = false;
                }
            } catch (AgentUnavailableException aue) {
                s_logger.warn("Agent running on host " + vm.getHostId() + " is unavailable, unable to stop vm " + vm.getHostName(), aue);
                success = false;
            }
        }

        return success;
    }

    @Override
    @ActionEvents({@ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_CREATE, eventDescription = "creating Account"),
        @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")})
    public UserAccount createUserAccount(final String userName, final String password, final String firstName, final String lastName, final String email, final String timezone, String accountName,
            final short accountType, final Long roleId, Long domainId, final String networkDomain, final Map<String, String> details, String accountUUID, final String userUUID) {

        return createUserAccount(userName, password, firstName, lastName, email, timezone, accountName, accountType, roleId, domainId, networkDomain, details, accountUUID, userUUID,
                User.Source.UNKNOWN);
    }

    // ///////////////////////////////////////////////////
    // ////////////// API commands /////////////////////
    // ///////////////////////////////////////////////////

    @Override
    @DB
    @ActionEvents({@ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_CREATE, eventDescription = "creating Account"),
        @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")})
    public UserAccount createUserAccount(final String userName, final String password, final String firstName,
                                         final String lastName, final String email, final String timezone,
                                         String accountName, final short accountType, final Long roleId, Long domainId,
                                         final String networkDomain, final Map<String, String> details,
                                         String accountUUID, final String userUUID,
            final User.Source source) {

        if (accountName == null) {
            accountName = userName;
        }
        if (domainId == null) {
            domainId = Domain.ROOT_DOMAIN;
        }

        if (StringUtils.isEmpty(userName)) {
            throw new InvalidParameterValueException("Username is empty");
        }

        if (StringUtils.isEmpty(firstName)) {
            throw new InvalidParameterValueException("Firstname is empty");
        }

        if (StringUtils.isEmpty(lastName)) {
            throw new InvalidParameterValueException("Lastname is empty");
        }

        // Validate domain
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        }

        // Check permissions
        checkAccess(getCurrentCallingAccount(), domain);

        if (!_userAccountDao.validateUsernameInDomain(userName, domainId)) {
            throw new InvalidParameterValueException("The user " + userName + " already exists in domain " + domainId);
        }

        if (networkDomain != null && networkDomain.length() > 0) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        final String accountNameFinal = accountName;
        final Long domainIdFinal = domainId;
        final String accountUUIDFinal = accountUUID;
        Pair<Long, Account> pair = Transaction.execute(new TransactionCallback<Pair<Long, Account>>() {
            @Override
            public Pair<Long, Account> doInTransaction(TransactionStatus status) {
                // create account
                String accountUUID = accountUUIDFinal;
                if (accountUUID == null) {
                    accountUUID = UUID.randomUUID().toString();
                }
                AccountVO account = createAccount(accountNameFinal, accountType, roleId, domainIdFinal, networkDomain, details, accountUUID);
                long accountId = account.getId();

                // create the first user for the account
                UserVO user = createUser(accountId, userName, password, firstName, lastName, email, timezone, userUUID, source);

                if (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                    // set registration token
                    byte[] bytes = (domainIdFinal + accountNameFinal + userName + System.currentTimeMillis()).getBytes();
                    String registrationToken = UUID.nameUUIDFromBytes(bytes).toString();
                    user.setRegistrationToken(registrationToken);
                }

                return new Pair<Long, Account>(user.getId(), account);
            }
        });

        long userId = pair.first();
        Account account = pair.second();

        // create correct account and group association based on accountType
        if (accountType != Account.ACCOUNT_TYPE_PROJECT) {
            Map<Long, Long> accountGroupMap = new HashMap<Long, Long>();
            accountGroupMap.put(account.getId(), new Long(accountType + 1));
            _messageBus.publish(_name, MESSAGE_ADD_ACCOUNT_EVENT, PublishScope.LOCAL, accountGroupMap);
        }

        CallContext.current().putContextParameter(Account.class, account.getUuid());

        // check success
        return _userAccountDao.findById(userId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")
    public UserVO createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId, String userUUID,
            User.Source source) {
        // default domain to ROOT if not specified
        if (domainId == null) {
            domainId = Domain.ROOT_DOMAIN;
        }

        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new CloudRuntimeException("The domain " + domainId + " does not exist; unable to create user");
        } else if (domain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The user cannot be created as domain " + domain.getName() + " is being deleted");
        }

        checkAccess(getCurrentCallingAccount(), domain);

        Account account = _accountDao.findEnabledAccount(accountName, domainId);
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain id=" + domainId + " to create user");
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("Account id : " + account.getId() + " is a system account, can't add a user to it");
        }

        if (!_userAccountDao.validateUsernameInDomain(userName, domainId)) {
            throw new CloudRuntimeException("The user " + userName + " already exists in domain " + domainId);
        }
        UserVO user = null;
        user = createUser(account.getId(), userName, password, firstName, lastName, email, timeZone, userUUID, source);
        return user;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")
    public UserVO createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId, String userUUID) {

        return createUser(userName, password, firstName, lastName, email, timeZone, accountName, domainId, userUUID, User.Source.UNKNOWN);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_UPDATE, eventDescription = "Updating User")
    public UserAccount updateUser(UpdateUserCmd updateUserCmd) {
        UserVO user = retrieveAndValidateUser(updateUserCmd);
        s_logger.debug("Updating user with Id: " + user.getUuid());

        validateAndUpdateApiAndSecretKeyIfNeeded(updateUserCmd, user);
        Account account = retrieveAndValidateAccount(user);

        validateAndUpdateFirstNameIfNeeded(updateUserCmd, user);
        validateAndUpdateLastNameIfNeeded(updateUserCmd, user);
        validateAndUpdateUsernameIfNeeded(updateUserCmd, user, account);

        validateUserPasswordAndUpdateIfNeeded(updateUserCmd.getPassword(), user, updateUserCmd.getCurrentPassword());
        String email = updateUserCmd.getEmail();
        if (StringUtils.isNotBlank(email)) {
            user.setEmail(email);
        }
        String timezone = updateUserCmd.getTimezone();
        if (StringUtils.isNotBlank(timezone)) {
            user.setTimezone(timezone);
        }
        _userDao.update(user.getId(), user);
        return _userAccountDao.findById(user.getId());
    }

    /**
     * Updates the password in the user POJO if needed. If no password is provided, then the password is not updated.
     * The following validations are executed if 'password' is not null. Admins (root admins or domain admins) can execute password updates without entering the current password.
     * <ul>
     *  <li> If 'password' is blank, we throw an {@link InvalidParameterValueException};
     *  <li> If 'current password' is not provided and user is not an Admin, we throw an {@link InvalidParameterValueException};
     *  <li> If a normal user is calling this method, we use {@link #validateCurrentPassword(UserVO, String)} to check if the provided old password matches the database one;
     * </ul>
     *
     * If all checks pass, we encode the given password with the most preferable password mechanism given in {@link #_userPasswordEncoders}.
     */
    protected void validateUserPasswordAndUpdateIfNeeded(String newPassword, UserVO user, String currentPassword) {
        if (newPassword == null) {
            s_logger.trace("No new password to update for user: " + user.getUuid());
            return;
        }
        if (StringUtils.isBlank(newPassword)) {
            throw new InvalidParameterValueException("Password cannot be empty or blank.");
        }
        Account callingAccount = getCurrentCallingAccount();
        boolean isRootAdminExecutingPasswordUpdate = callingAccount.getId() == Account.ACCOUNT_ID_SYSTEM || isRootAdmin(callingAccount.getId());
        boolean isDomainAdmin = isDomainAdmin(callingAccount.getId());
        boolean isAdmin = isDomainAdmin || isRootAdminExecutingPasswordUpdate;
        if (isAdmin) {
            s_logger.trace(String.format("Admin account [uuid=%s] executing password update for user [%s] ", callingAccount.getUuid(), user.getUuid()));
        }
        if (!isAdmin && StringUtils.isBlank(currentPassword)) {
            throw new InvalidParameterValueException("To set a new password the current password must be provided.");
        }
        if (CollectionUtils.isEmpty(_userPasswordEncoders)) {
            throw new CloudRuntimeException("No user authenticators configured!");
        }
        if (!isAdmin) {
            validateCurrentPassword(user, currentPassword);
        }
        UserAuthenticator userAuthenticator = _userPasswordEncoders.get(0);
        String newPasswordEncoded = userAuthenticator.encode(newPassword);
        user.setPassword(newPasswordEncoded);
    }

    /**
     * Iterates over all configured user authenticators and tries to authenticate the user using the current password.
     * If the user is authenticated with success, we have nothing else to do here; otherwise, an {@link InvalidParameterValueException} is thrown.
     */
    protected void validateCurrentPassword(UserVO user, String currentPassword) {
        AccountVO userAccount = _accountDao.findById(user.getAccountId());
        boolean currentPasswordMatchesDataBasePassword = false;
        for (UserAuthenticator userAuthenticator : _userPasswordEncoders) {
            Pair<Boolean, ActionOnFailedAuthentication> authenticationResult = userAuthenticator.authenticate(user.getUsername(), currentPassword, userAccount.getDomainId(), null);
            if (authenticationResult == null) {
                s_logger.trace(String.format("Authenticator [%s] is returning null for the authenticate mehtod.", userAuthenticator.getClass()));
                continue;
            }
            if (BooleanUtils.toBoolean(authenticationResult.first())) {
                s_logger.debug(String.format("User [id=%s] re-authenticated [authenticator=%s] during password update.", user.getUuid(), userAuthenticator.getName()));
                currentPasswordMatchesDataBasePassword = true;
                break;
            }
        }
        if (!currentPasswordMatchesDataBasePassword) {
            throw new InvalidParameterValueException("Current password is incorrect.");
        }
    }

    /**
     * Validates the user 'username' if provided. The 'username' cannot be blank (when provided).
     * <ul>
     *  <li> If the 'username' is not provided, we do not update it (setting to null) in the User POJO.
     *  <li> If the 'username' is blank, we throw an {@link InvalidParameterValueException}.
     *  <li> The username must be unique in each domain. Therefore, if there is already another user with the same username, an {@link InvalidParameterValueException} is thrown.
     * </ul>
     */
    protected void validateAndUpdateUsernameIfNeeded(UpdateUserCmd updateUserCmd, UserVO user, Account account) {
        String userName = updateUserCmd.getUsername();
        if (userName == null) {
            return;
        }
        if (StringUtils.isBlank(userName)) {
            throw new InvalidParameterValueException("Username cannot be empty.");
        }
        List<UserVO> duplicatedUsers = _userDao.findUsersByName(userName);
        for (UserVO duplicatedUser : duplicatedUsers) {
            if (duplicatedUser.getId() == user.getId()) {
                continue;
            }
            Account duplicatedUserAccountWithUserThatHasTheSameUserName = _accountDao.findById(duplicatedUser.getAccountId());
            if (duplicatedUserAccountWithUserThatHasTheSameUserName.getDomainId() == account.getDomainId()) {
                DomainVO domain = _domainDao.findById(duplicatedUserAccountWithUserThatHasTheSameUserName.getDomainId());
                throw new InvalidParameterValueException(String.format("Username [%s] already exists in domain [id=%s,name=%s]", duplicatedUser.getUsername(), domain.getUuid(), domain.getName()));
            }
        }
        user.setUsername(userName);
    }

    /**
     * Validates the user 'lastName' if provided. The 'lastName' cannot be blank (when provided).
     * <ul>
     *  <li> If the 'lastName' is not provided, we do not update it (setting to null) in the User POJO.
     *  <li> If the 'lastName' is blank, we throw an {@link InvalidParameterValueException}.
     * </ul>
     */
    protected void validateAndUpdateLastNameIfNeeded(UpdateUserCmd updateUserCmd, UserVO user) {
        String lastName = updateUserCmd.getLastname();
        if (lastName != null) {
            if (StringUtils.isBlank(lastName)) {
                throw new InvalidParameterValueException("Lastname cannot be empty.");
            }

            user.setLastname(lastName);
        }
    }

    /**
     * Validates the user 'firstName' if provided. The 'firstName' cannot be blank (when provided).
     * <ul>
     *  <li> If the 'firstName' is not provided, we do not update it (setting to null) in the User POJO.
     *  <li> If the 'firstName' is blank, we throw an {@link InvalidParameterValueException}.
     * </ul>
     */
    protected void validateAndUpdateFirstNameIfNeeded(UpdateUserCmd updateUserCmd, UserVO user) {
        String firstName = updateUserCmd.getFirstname();
        if (firstName != null) {
            if (StringUtils.isBlank(firstName)) {
                throw new InvalidParameterValueException("Firstname cannot be empty.");
            }
            user.setFirstname(firstName);
        }
    }

    /**
     * Searches an account for the given users. Then, we validate it as follows:
     * <ul>
     *  <li>If no account is found for the given user, we throw a {@link CloudRuntimeException}. There must be something wrong in the database for this case.
     *  <li>If the account is of {@link Account#ACCOUNT_TYPE_PROJECT}, we throw an {@link InvalidParameterValueException}.
     *  <li>If the account is of {@link Account#ACCOUNT_ID_SYSTEM}, we throw an {@link InvalidParameterValueException}.
     * </ul>
     *
     * Afterwards, we check if the logged user has access to the user being updated via {@link #checkAccess(Account, AccessType, boolean, ControlledEntity...)}
     */
    protected Account retrieveAndValidateAccount(UserVO user) {
        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new CloudRuntimeException("Unable to find user account with ID: " + user.getAccountId());
        }
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find user with ID: " + user.getUuid());
        }
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("user UUID : " + user.getUuid() + " is a system account; update is not allowed.");
        }
        checkAccess(getCurrentCallingAccount(), AccessType.OperateEntry, true, account);
        return account;
    }

    /**
     * Returns the calling account using the method {@link CallContext#getCallingAccount()}.
     * We are introducing this method to avoid using 'PowerMockRunner' in unit tests. Then, we can mock the calls to this method, which facilitates the development of test cases.
     */
    protected Account getCurrentCallingAccount() {
        return CallContext.current().getCallingAccount();
    }

    /**
     * Validates user API and Secret keys. If a new pair of keys is provided, we update them in the user POJO.
     * <ul>
     * <li>When updating the keys, it must be provided a pair (API and Secret keys); otherwise, an {@link InvalidParameterValueException} is thrown.
     * <li>If a pair of keys is provided, we validate to see if there is an user already using the provided API key. If there is someone else using, we throw an {@link InvalidParameterValueException} because two users cannot have the same API key.
     * </ul>
     */
    protected void validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmd updateUserCmd, UserVO user) {
        String apiKey = updateUserCmd.getApiKey();
        String secretKey = updateUserCmd.getSecretKey();

        boolean isApiKeyBlank = StringUtils.isBlank(apiKey);
        boolean isSecretKeyBlank = StringUtils.isBlank(secretKey);
        if (isApiKeyBlank ^ isSecretKeyBlank) {
            throw new InvalidParameterValueException("Please provide a userApiKey/userSecretKey pair");
        }
        if (isApiKeyBlank && isSecretKeyBlank) {
            return;
        }
        Pair<User, Account> apiKeyOwner = _accountDao.findUserAccountByApiKey(apiKey);
        if (apiKeyOwner != null) {
            User userThatHasTheProvidedApiKey = apiKeyOwner.first();
            if (userThatHasTheProvidedApiKey.getId() != user.getId()) {
                throw new InvalidParameterValueException(String.format("The API key [%s] already exists in the system. Please provide a unique key.", apiKey));
            }
        }
        user.setApiKey(apiKey);
        user.setSecretKey(secretKey);
    }

    /**
     * Searches for a user with the given userId. If no user is found we throw an {@link InvalidParameterValueException}.
     */
    protected UserVO retrieveAndValidateUser(UpdateUserCmd updateUserCmd) {
        Long userId = updateUserCmd.getId();

        UserVO user = _userDao.getUser(userId);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find user with id: " + userId);
        }
        return user;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_DISABLE, eventDescription = "disabling User", async = true)
    public UserAccount disableUser(long userId) {
        Account caller = getCurrentCallingAccount();

        // Check if user exists in the system
        User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("unable to find user account " + user.getAccountId());
        }

        // don't allow disabling user belonging to project's account
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        // If the user is a System user, return an error
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("User id : " + userId + " is a system user, disabling is not allowed");
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);

        boolean success = doSetUserStatus(userId, State.disabled);
        if (success) {

            CallContext.current().putContextParameter(User.class, user.getUuid());

            // user successfully disabled
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to disable user " + userId);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_USER_ENABLE, eventDescription = "enabling User")
    public UserAccount enableUser(final long userId) {

        Account caller = getCurrentCallingAccount();

        // Check if user exists in the system
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("unable to find user account " + user.getAccountId());
        }

        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        // If the user is a System user, return an error
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("User id : " + userId + " is a system user, enabling is not allowed");
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);

        boolean success = Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = doSetUserStatus(userId, State.enabled);

                // make sure the account is enabled too
                success = success && enableAccount(user.getAccountId());

                return success;
            }
        });

        if (success) {
            // whenever the user is successfully enabled, reset the login attempts to zero
            updateLoginAttempts(userId, 0, false);

            CallContext.current().putContextParameter(User.class, user.getUuid());

            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to enable user " + userId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_LOCK, eventDescription = "locking User")
    public UserAccount lockUser(long userId) {
        Account caller = getCurrentCallingAccount();

        // Check if user with id exists in the system
        User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }

        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("unable to find user account " + user.getAccountId());
        }

        // don't allow to lock user of the account of type Project
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }

        // If the user is a System user, return an error. We do not allow this
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("user id : " + userId + " is a system user, locking is not allowed");
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);

        // make sure the account is enabled too
        // if the user is either locked already or disabled already, don't change state...only lock currently enabled
// users
        boolean success = true;
        if (user.getState().equals(State.locked)) {
            // already locked...no-op
            return _userAccountDao.findById(userId);
        } else if (user.getState().equals(State.enabled)) {
            success = doSetUserStatus(user.getId(), State.locked);

            boolean lockAccount = true;
            List<UserVO> allUsersByAccount = _userDao.listByAccount(user.getAccountId());
            for (UserVO oneUser : allUsersByAccount) {
                if (oneUser.getState().equals(State.enabled)) {
                    lockAccount = false;
                    break;
                }
            }

            if (lockAccount) {
                success = (success && lockAccount(user.getAccountId()));
            }
        } else {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Attempting to lock a non-enabled user, current state is " + user.getState() + " (userId: " + user.getId() + "), locking failed.");
            }
            success = false;
        }

        if (success) {

            CallContext.current().putContextParameter(User.class, user.getUuid());

            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to lock user " + userId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DELETE, eventDescription = "deleting account", async = true)
    public boolean deleteUserAccount(long accountId) {

        CallContext ctx = CallContext.current();
        long callerUserId = ctx.getCallingUserId();
        Account caller = ctx.getCallingAccount();

        // If the user is a System user, return an error. We do not allow this
        AccountVO account = _accountDao.findById(accountId);

        if (account == null || account.getRemoved() != null) {
            if (account != null) {
                s_logger.info("The account:" + account.getAccountName() + " is already removed");
            }
            return true;
        }

        // don't allow removing Project account
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("The specified account does not exist in the system");
        }

        checkAccess(caller, null, true, account);

        // don't allow to delete default account (system and admin)
        if (account.isDefault()) {
            throw new InvalidParameterValueException("The account is default and can't be removed");
        }

        // Account that manages project(s) can't be removed
        List<Long> managedProjectIds = _projectAccountDao.listAdministratedProjectIds(accountId);
        if (!managedProjectIds.isEmpty()) {
            StringBuilder projectIds = new StringBuilder();
            for (Long projectId : managedProjectIds) {
                projectIds.append(projectId + ", ");
            }

            throw new InvalidParameterValueException("The account id=" + accountId + " manages project(s) with ids " + projectIds + "and can't be removed");
        }

        CallContext.current().putContextParameter(Account.class, account.getUuid());

        return deleteAccount(account, callerUserId, caller);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_ENABLE, eventDescription = "enabling account", async = true)
    public AccountVO enableAccount(String accountName, Long domainId, Long accountId) {

        // Check if account exists
        Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("Account id : " + accountId + " is a system account, enable is not allowed");
        }

        // Check if user performing the action is allowed to modify this account
        Account caller = getCurrentCallingAccount();
        checkAccess(caller, AccessType.OperateEntry, true, account);

        boolean success = enableAccount(account.getId());
        if (success) {

            CallContext.current().putContextParameter(Account.class, account.getUuid());

            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to enable account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "locking account", async = true)
    public AccountVO lockAccount(String accountName, Long domainId, Long accountId) {
        Account caller = getCurrentCallingAccount();

        Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("Account id : " + accountId + " is a system account, lock is not allowed");
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);

        if (lockAccount(account.getId())) {
            CallContext.current().putContextParameter(Account.class, account.getUuid());
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to lock account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "disabling account", async = true)
    public AccountVO disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        Account caller = getCurrentCallingAccount();

        Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("Account id : " + accountId + " is a system account, disable is not allowed");
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);

        if (disableAccount(account.getId())) {
            CallContext.current().putContextParameter(Account.class, account.getUuid());
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to update account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_UPDATE, eventDescription = "updating account", async = true)
    public AccountVO updateAccount(UpdateAccountCmd cmd) {
        Long accountId = cmd.getId();
        Long domainId = cmd.getDomainId();
        Long roleId = cmd.getRoleId();
        String accountName = cmd.getAccountName();
        String newAccountName = cmd.getNewName();
        String networkDomain = cmd.getNetworkDomain();
        final Map<String, String> details = cmd.getDetails();

        boolean success = false;
        Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findEnabledAccount(accountName, domainId);
        }

        final AccountVO acctForUpdate = _accountDao.findById(account.getId());

        // Check if account exists
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            s_logger.error("Unable to find account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Unable to find account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }

        // Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        // Check if user performing the action is allowed to modify this account
        Account caller = getCurrentCallingAccount();
        checkAccess(caller, _domainMgr.getDomain(account.getDomainId()));

        if(newAccountName != null) {

            if (newAccountName.isEmpty()) {
                throw new InvalidParameterValueException("The new account name for account '" + account.getUuid() + "' " +
                        "within domain '" + domainId + "'  is empty string. Account will be not renamed.");
            }

            // check if the new proposed account name is absent in the domain
            Account existingAccount = _accountDao.findActiveAccount(newAccountName, domainId);
            if (existingAccount != null && existingAccount.getId() != account.getId()) {
                throw new InvalidParameterValueException("The account with the proposed name '" +
                        newAccountName + "' exists in the domain '" +
                        domainId + "' with existing account id '" + existingAccount.getId() + "'");
            }

            acctForUpdate.setAccountName(newAccountName);
        }

        if (networkDomain != null && !networkDomain.isEmpty()) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException("Invalid network domain or format. " +
                        "Total length shouldn't exceed 190 chars. Every domain part must be between 1 and 63 " +
                        "characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }


        if (roleId != null) {
            final List<Role> roles = cmd.roleService.listRoles();
            final boolean roleNotFound = roles.stream().filter(r -> r.getId() == roleId).count() == 0;
            if (roleNotFound) {
                throw new InvalidParameterValueException("Role with ID '" + roleId.toString() + "' " +
                        "is not found or not available for the account '" + account.getUuid() + "' " +
                        "in the domain '" + domainId + "'.");
            }

            acctForUpdate.setRoleId(roleId);
        }

        if (networkDomain != null) {
            if (networkDomain.isEmpty()) {
                acctForUpdate.setNetworkDomain(null);
            } else {
                acctForUpdate.setNetworkDomain(networkDomain);
            }
        }

        final Account accountFinal = account;
        success = Transaction.execute((TransactionCallback<Boolean>) status -> {
            boolean success1 = _accountDao.update(accountFinal.getId(), acctForUpdate);

            if (details != null && success1) {
                _accountDetailsDao.update(accountFinal.getId(), details);
            }

            return success1;
        });

        if (success) {
            CallContext.current().putContextParameter(Account.class, account.getUuid());
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to update account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_DELETE, eventDescription = "deleting User")
    public boolean deleteUser(DeleteUserCmd deleteUserCmd) {
        UserVO user = getValidUserVO(deleteUserCmd.getId());

        Account account = _accountDao.findById(user.getAccountId());

        // don't allow to delete the user from the account of type Project
        checkAccountAndAccess(user, account);
        return _userDao.remove(deleteUserCmd.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_MOVE, eventDescription = "moving User to a new account")
    public boolean moveUser(MoveUserCmd cmd) {
        final Long id = cmd.getId();
        UserVO user = getValidUserVO(id);
        Account oldAccount = _accountDao.findById(user.getAccountId());
        checkAccountAndAccess(user, oldAccount);
        long domainId = oldAccount.getDomainId();

        long newAccountId = getNewAccountId(domainId, cmd.getAccountName(), cmd.getAccountId());

        return moveUser(user, newAccountId);
    }

    @Override
    public boolean moveUser(long id, Long domainId, Account newAccount) {
        UserVO user = getValidUserVO(id);
        Account oldAccount = _accountDao.findById(user.getAccountId());
        checkAccountAndAccess(user, oldAccount);
        checkIfNotMovingAcrossDomains(domainId, newAccount);
        return moveUser(user, newAccount.getId());
    }

    private boolean moveUser(UserVO user, long newAccountId) {
        if (newAccountId == user.getAccountId()) {
            // could do a not silent fail but the objective of the user is reached
            return true; // no need to create a new user object for this user
        }

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                UserVO newUser = new UserVO(user);
                user.setExternalEntity(user.getUuid());
                user.setUuid(UUID.randomUUID().toString());
                user.setApiKey(null);
                user.setSecretKey(null);
                _userDao.update(user.getId(), user);
                newUser.setAccountId(newAccountId);
                boolean success = _userDao.remove(user.getId());
                UserVO persisted = _userDao.persist(newUser);
                return success && persisted.getUuid().equals(user.getExternalEntity());
            }
        });
    }

    private long getNewAccountId(long domainId, String accountName, Long accountId) {
        Account newAccount = null;
        if (StringUtils.isNotBlank(accountName)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Getting id for account by name '" + accountName + "' in domain " + domainId);
            }
            newAccount = _accountDao.findEnabledAccount(accountName, domainId);
        }
        if (newAccount == null && accountId != null) {
            newAccount = _accountDao.findById(accountId);
        }
        if (newAccount == null) {
            throw new CloudRuntimeException("no account name or account id. this should have been caught before this point");
        }

        checkIfNotMovingAcrossDomains(domainId, newAccount);
        return newAccount.getAccountId();
    }

    private void checkIfNotMovingAcrossDomains(long domainId, Account newAccount) {
        if (newAccount.getDomainId() != domainId) {
            // not in scope
            throw new InvalidParameterValueException("moving a user from an account in one domain to an account in annother domain is not supported!");
        }
    }

    private void checkAccountAndAccess(UserVO user, Account account) {
        // don't allow to delete the user from the account of type Project
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Project users cannot be deleted or moved.");
        }

        checkAccess(getCurrentCallingAccount(), AccessType.OperateEntry, true, account);
        CallContext.current().putContextParameter(User.class, user.getUuid());
    }

    private UserVO getValidUserVO(long id) {
        UserVO user = _userDao.findById(id);

        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }

        // don't allow to delete default user (system and admin users)
        if (user.isDefault()) {
            throw new InvalidParameterValueException("The user is default and can't be (re)moved");
        }

        return user;
    }

    protected class AccountCleanupTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("AccountCleanup");
                if (lock == null) {
                    s_logger.debug("Couldn't get the global lock");
                    return;
                }

                if (!lock.lock(30)) {
                    s_logger.debug("Couldn't lock the db");
                    return;
                }

                try {
                    // Cleanup removed accounts
                    List<AccountVO> removedAccounts = _accountDao.findCleanupsForRemovedAccounts(null);
                    s_logger.info("Found " + removedAccounts.size() + " removed accounts to cleanup");
                    for (AccountVO account : removedAccounts) {
                        s_logger.debug("Cleaning up " + account.getId());
                        cleanupAccount(account, getSystemUser().getId(), getSystemAccount());
                    }

                    // cleanup disabled accounts
                    List<AccountVO> disabledAccounts = _accountDao.findCleanupsForDisabledAccounts();
                    s_logger.info("Found " + disabledAccounts.size() + " disabled accounts to cleanup");
                    for (AccountVO account : disabledAccounts) {
                        s_logger.debug("Disabling account " + account.getId());
                        try {
                            disableAccount(account.getId());
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on account " + account.getId(), e);
                        }
                    }

                    // cleanup inactive domains
                    List<? extends Domain> inactiveDomains = _domainMgr.findInactiveDomains();
                    s_logger.info("Found " + inactiveDomains.size() + " inactive domains to cleanup");
                    for (Domain inactiveDomain : inactiveDomains) {
                        long domainId = inactiveDomain.getId();
                        try {
                            List<AccountVO> accountsForCleanupInDomain = _accountDao.findCleanupsForRemovedAccounts(domainId);
                            if (accountsForCleanupInDomain.isEmpty()) {
                                // release dedication if any, before deleting the domain
                                List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listByDomainId(domainId);
                                if (dedicatedResources != null && !dedicatedResources.isEmpty()) {
                                    s_logger.debug("Releasing dedicated resources for domain" + domainId);
                                    for (DedicatedResourceVO dr : dedicatedResources) {
                                        if (!_dedicatedDao.remove(dr.getId())) {
                                            s_logger.warn("Fail to release dedicated resources for domain " + domainId);
                                        }
                                    }
                                }
                                s_logger.debug("Removing inactive domain id=" + domainId);
                                _domainMgr.removeDomain(domainId);
                            } else {
                                s_logger.debug("Can't remove inactive domain id=" + domainId + " as it has accounts that need cleanup");
                            }
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on domain " + domainId, e);
                        }
                    }

                    // cleanup inactive projects
                    List<ProjectVO> inactiveProjects = _projectDao.listByState(Project.State.Disabled);
                    s_logger.info("Found " + inactiveProjects.size() + " disabled projects to cleanup");
                    for (ProjectVO project : inactiveProjects) {
                        try {
                            Account projectAccount = getAccount(project.getProjectAccountId());
                            if (projectAccount == null) {
                                s_logger.debug("Removing inactive project id=" + project.getId());
                                _projectMgr.deleteProject(CallContext.current().getCallingAccount(), CallContext.current().getCallingUserId(), project);
                            } else {
                                s_logger.debug("Can't remove disabled project " + project + " as it has non removed account id=" + project.getId());
                            }
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on project " + project, e);
                        }
                    }

                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }

    @Override
    public Account finalizeOwner(Account caller, String accountName, Long domainId, Long projectId) {
        // don't default the owner to the system account
        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM && ((accountName == null || domainId == null) && projectId == null)) {
            throw new InvalidParameterValueException("Account and domainId are needed for resource creation");
        }

        // projectId and account/domainId can't be specified together
        if ((accountName != null && domainId != null) && projectId != null) {
            throw new InvalidParameterValueException("ProjectId and account/domainId can't be specified together");
        }

        if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id=" + projectId);
            }

            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new PermissionDeniedException("Account " + caller + " is unauthorised to use project id=" + projectId);
            }

            return getAccount(project.getProjectAccountId());
        }

        if (isAdmin(caller.getId()) && accountName != null && domainId != null) {
            Domain domain = _domainMgr.getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
            }

            Account owner = _accountDao.findActiveAccount(accountName, domainId);
            if (owner == null) {
                throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
            }
            checkAccess(caller, domain);

            return owner;
        } else if (!isAdmin(caller.getId()) && accountName != null && domainId != null) {
            if (!accountName.equals(caller.getAccountName()) || domainId.longValue() != caller.getDomainId()) {
                throw new PermissionDeniedException("Can't create/list resources for account " + accountName + " in domain " + domainId + ", permission denied");
            } else {
                return caller;
            }
        } else {
            if (accountName != null && domainId == null) {
                throw new InvalidParameterValueException("AccountName and domainId must be specified together");
            }
            // regular user can't create/list resources for other people
            return caller;
        }
    }

    @Override
    public Account getActiveAccountByName(String accountName, Long domainId) {
        if (accountName == null || domainId == null) {
            throw new InvalidParameterValueException("Both accountName and domainId are required for finding active account in the system");
        } else {
            return _accountDao.findActiveAccount(accountName, domainId);
        }
    }

    @Override
    public UserAccount getActiveUserAccount(String username, Long domainId) {
        return _userAccountDao.getUserAccount(username, domainId);
    }

    @Override
    public Account getActiveAccountById(long accountId) {
        return _accountDao.findById(accountId);
    }

    @Override
    public Account getAccount(long accountId) {
        return _accountDao.findByIdIncludingRemoved(accountId);
    }

    @Override
    public RoleType getRoleType(Account account) {
        if (account == null) {
            return RoleType.Unknown;
        }
        return RoleType.getByAccountType(account.getType());
    }

    @Override
    public User getActiveUser(long userId) {
        return _userDao.findById(userId);
    }

    @Override
    public User getUserIncludingRemoved(long userId) {
        return _userDao.findByIdIncludingRemoved(userId);
    }

    @Override
    public User getActiveUserByRegistrationToken(String registrationToken) {
        return _userDao.findUserByRegistrationToken(registrationToken);
    }

    @Override
    public void markUserRegistered(long userId) {
        UserVO userForUpdate = _userDao.createForUpdate();
        userForUpdate.setRegistered(true);
        _userDao.update(Long.valueOf(userId), userForUpdate);
    }

    @Override
    @DB
    public AccountVO createAccount(final String accountName, final short accountType, final Long roleId, final Long domainId, final String networkDomain, final Map<String, String> details,
            final String uuid) {
        // Validate domain
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        }

        if (domain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The account cannot be created as domain " + domain.getName() + " is being deleted");
        }

        if ((domainId != Domain.ROOT_DOMAIN) && (accountType == Account.ACCOUNT_TYPE_ADMIN)) {
            throw new InvalidParameterValueException(
                    "Invalid account type " + accountType + " given for an account in domain " + domainId + "; unable to create user of admin role type in non-ROOT domain.");
        }

        // Validate account/user/domain settings
        if (_accountDao.findActiveAccount(accountName, domainId) != null) {
            throw new InvalidParameterValueException("The specified account: " + accountName + " already exists");
        }

        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        // Verify account type
        if ((accountType < Account.ACCOUNT_TYPE_NORMAL) || (accountType > Account.ACCOUNT_TYPE_PROJECT)) {
            throw new InvalidParameterValueException("Invalid account type " + accountType + " given; unable to create user");
        }

        if (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            List<DataCenterVO> dc = _dcDao.findZonesByDomainId(domainId);
            if (dc.isEmpty()) {
                throw new InvalidParameterValueException("The account cannot be created as domain " + domain.getName() + " is not associated with any private Zone");
            }
        }

        // Create the account
        return Transaction.execute(new TransactionCallback<AccountVO>() {
            @Override
            public AccountVO doInTransaction(TransactionStatus status) {
                AccountVO account = _accountDao.persist(new AccountVO(accountName, domainId, networkDomain, accountType, roleId, uuid));

                if (account == null) {
                    throw new CloudRuntimeException("Failed to create account name " + accountName + " in domain id=" + domainId);
                }

                Long accountId = account.getId();

                if (details != null) {
                    _accountDetailsDao.persist(accountId, details);
                }

                // Create resource count records for the account
                _resourceCountDao.createResourceCounts(accountId, ResourceLimit.ResourceOwnerType.Account);

                // Create default security group
                _networkGroupMgr.createDefaultSecurityGroup(accountId);

                return account;
            }
        });
    }

    protected UserVO createUser(long accountId, String userName, String password, String firstName, String lastName, String email, String timezone, String userUUID, User.Source source) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating user: " + userName + ", accountId: " + accountId + " timezone:" + timezone);
        }

        String encodedPassword = null;
        for (UserAuthenticator authenticator : _userPasswordEncoders) {
            encodedPassword = authenticator.encode(password);
            if (encodedPassword != null) {
                break;
            }
        }
        if (encodedPassword == null) {
            throw new CloudRuntimeException("Failed to encode password");
        }

        if (userUUID == null) {
            userUUID = UUID.randomUUID().toString();
        }
        UserVO user = _userDao.persist(new UserVO(accountId, userName, encodedPassword, firstName, lastName, email, timezone, userUUID, source));
        CallContext.current().putContextParameter(User.class, user.getUuid());
        return user;
    }

    @Override
    public void logoutUser(long userId) {
        UserAccount userAcct = _userAccountDao.findById(userId);
        if (userAcct != null) {
            ActionEventUtils.onActionEvent(userId, userAcct.getAccountId(), userAcct.getDomainId(), EventTypes.EVENT_USER_LOGOUT, "user has logged out");
        } // else log some kind of error event? This likely means the user doesn't exist, or has been deleted...
    }

    @Override
    public UserAccount authenticateUser(final String username, final String password, final Long domainId, final InetAddress loginIpAddress, final Map<String, Object[]> requestParameters) {
        UserAccount user = null;
        if (password != null && !password.isEmpty()) {
            user = getUserAccount(username, password, domainId, requestParameters);
        } else {
            String key = _configDao.getValue("security.singlesignon.key");
            if (key == null) {
                // the SSO key is gone, don't authenticate
                return null;
            }

            String singleSignOnTolerance = _configDao.getValue("security.singlesignon.tolerance.millis");
            if (singleSignOnTolerance == null) {
                // the SSO tolerance is gone (how much time before/after system time we'll allow the login request to be
                // valid),
                // don't authenticate
                return null;
            }

            long tolerance = Long.parseLong(singleSignOnTolerance);
            String signature = null;
            long timestamp = 0L;
            String unsignedRequest = null;
            StringBuffer unsignedRequestBuffer = new StringBuffer();

            // - build a request string with sorted params, make sure it's all lowercase
            // - sign the request, verify the signature is the same
            List<String> parameterNames = new ArrayList<String>();

            for (Object paramNameObj : requestParameters.keySet()) {
                parameterNames.add((String)paramNameObj); // put the name in a list that we'll sort later
            }

            Collections.sort(parameterNames);

            try {
                for (String paramName : parameterNames) {
                    // parameters come as name/value pairs in the form String/String[]
                    String paramValue = ((String[])requestParameters.get(paramName))[0];

                    if ("signature".equalsIgnoreCase(paramName)) {
                        signature = paramValue;
                    } else {
                        if ("timestamp".equalsIgnoreCase(paramName)) {
                            String timestampStr = paramValue;
                            try {
                                // If the timestamp is in a valid range according to our tolerance, verify the request
                                // signature, otherwise return null to indicate authentication failure
                                timestamp = Long.parseLong(timestampStr);
                                long currentTime = System.currentTimeMillis();
                                if (Math.abs(currentTime - timestamp) > tolerance) {
                                    if (s_logger.isDebugEnabled()) {
                                        s_logger.debug("Expired timestamp passed in to login, current time = " + currentTime + ", timestamp = " + timestamp);
                                    }
                                    return null;
                                }
                            } catch (NumberFormatException nfe) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Invalid timestamp passed in to login: " + timestampStr);
                                }
                                return null;
                            }
                        }

                        if (unsignedRequestBuffer.length() != 0) {
                            unsignedRequestBuffer.append("&");
                        }
                        unsignedRequestBuffer.append(paramName).append("=").append(URLEncoder.encode(paramValue, "UTF-8"));
                    }
                }

                if ((signature == null) || (timestamp == 0L)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Missing parameters in login request, signature = " + signature + ", timestamp = " + timestamp);
                    }
                    return null;
                }

                unsignedRequest = unsignedRequestBuffer.toString().toLowerCase().replaceAll("\\+", "%20");

                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                mac.init(keySpec);
                mac.update(unsignedRequest.getBytes());
                byte[] encryptedBytes = mac.doFinal();
                String computedSignature = new String(Base64.encodeBase64(encryptedBytes));
                boolean equalSig = ConstantTimeComparator.compareStrings(signature, computedSignature);
                if (!equalSig) {
                    s_logger.info("User signature: " + signature + " is not equaled to computed signature: " + computedSignature);
                } else {
                    user = _userAccountDao.getUserAccount(username, domainId);
                }
            } catch (Exception ex) {
                s_logger.error("Exception authenticating user", ex);
                return null;
            }
        }

        if (user != null) {
            // don't allow to authenticate system user
            if (user.getId() == User.UID_SYSTEM) {
                s_logger.error("Failed to authenticate user: " + username + " in domain " + domainId);
                return null;
            }
            // don't allow baremetal system user
            if (BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME.equals(user.getUsername())) {
                s_logger.error("Won't authenticate user: " + username + " in domain " + domainId);
                return null;
            }

            // We authenticated successfully by now, let's check if we are allowed to login from the ip address the reqest comes from
            final Account account = getAccount(user.getAccountId());
            final DomainVO domain = (DomainVO) _domainMgr.getDomain(account.getDomainId());

            // Get the CIDRs from where this account is allowed to make calls
            final String accessAllowedCidrs = ApiServiceConfiguration.ApiAllowedSourceCidrList.valueIn(account.getId()).replaceAll("\\s", "");
            final Boolean ApiSourceCidrChecksEnabled = ApiServiceConfiguration.ApiSourceCidrChecksEnabled.value();

            if (ApiSourceCidrChecksEnabled) {
                s_logger.debug("CIDRs from which account '" + account.toString() + "' is allowed to perform API calls: " + accessAllowedCidrs);

                // Block when is not in the list of allowed IPs
                if (!NetUtils.isIpInCidrList(loginIpAddress, accessAllowedCidrs.split(","))) {
                    s_logger.warn("Request by account '" + account.toString() + "' was denied since " + loginIpAddress.toString().replace("/", "") + " does not match " + accessAllowedCidrs);
                    throw new CloudAuthenticationException("Failed to authenticate user '" + username + "' in domain '" + domain.getPath() + "' from ip "
                            + loginIpAddress.toString().replace("/", "") + "; please provide valid credentials");
                }
            }

            // Here all is fine!
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("User: " + username + " in domain " + domainId + " has successfully logged in");
            }

            ActionEventUtils.onActionEvent(user.getId(), user.getAccountId(), user.getDomainId(), EventTypes.EVENT_USER_LOGIN, "user has logged in from IP Address " + loginIpAddress);

            return user;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("User: " + username + " in domain " + domainId + " has failed to log in");
            }
            return null;
        }
    }

    private UserAccount getUserAccount(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to log in user: " + username + " in domain " + domainId);
        }
        UserAccount userAccount = _userAccountDao.getUserAccount(username, domainId);

        boolean authenticated = false;
        HashSet<ActionOnFailedAuthentication> actionsOnFailedAuthenticaion = new HashSet<ActionOnFailedAuthentication>();
        User.Source userSource = userAccount != null ? userAccount.getSource() : User.Source.UNKNOWN;
        for (UserAuthenticator authenticator : _userAuthenticators) {
            if (userSource != User.Source.UNKNOWN) {
                if (!authenticator.getName().equalsIgnoreCase(userSource.name())) {
                    continue;
                }
            }
            Pair<Boolean, ActionOnFailedAuthentication> result = authenticator.authenticate(username, password, domainId, requestParameters);
            if (result.first()) {
                authenticated = true;
                break;
            } else if (result.second() != null) {
                actionsOnFailedAuthenticaion.add(result.second());
            }
        }

        boolean updateIncorrectLoginCount = actionsOnFailedAuthenticaion.contains(ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);

        if (authenticated) {

            Domain domain = _domainMgr.getDomain(domainId);
            String domainName = null;
            if (domain != null) {
                domainName = domain.getName();
            }
            userAccount = _userAccountDao.getUserAccount(username, domainId);

            if (!userAccount.getState().equalsIgnoreCase(Account.State.enabled.toString()) || !userAccount.getAccountState().equalsIgnoreCase(Account.State.enabled.toString())) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("User " + username + " in domain " + domainName + " is disabled/locked (or account is disabled/locked)");
                }
                throw new CloudAuthenticationException("User " + username + " (or their account) in domain " + domainName + " is disabled/locked. Please contact the administrator.");
            }
            // Whenever the user is able to log in successfully, reset the login attempts to zero
            if (!isInternalAccount(userAccount.getId())) {
                updateLoginAttempts(userAccount.getId(), 0, false);
            }

            return userAccount;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to authenticate user with username " + username + " in domain " + domainId);
            }

            if (userAccount == null) {
                s_logger.warn("Unable to find an user with username " + username + " in domain " + domainId);
                return null;
            }

            if (userAccount.getState().equalsIgnoreCase(Account.State.enabled.toString())) {
                if (!isInternalAccount(userAccount.getId())) {
                    // Internal accounts are not disabled
                    int attemptsMade = userAccount.getLoginAttempts() + 1;
                    if (updateIncorrectLoginCount) {
                        if (attemptsMade < _allowedLoginAttempts) {
                            updateLoginAttempts(userAccount.getId(), attemptsMade, false);
                            s_logger.warn("Login attempt failed. You have " + (_allowedLoginAttempts - attemptsMade) + " attempt(s) remaining");
                        } else {
                            updateLoginAttempts(userAccount.getId(), _allowedLoginAttempts, true);
                            s_logger.warn("User " + userAccount.getUsername() + " has been disabled due to multiple failed login attempts." + " Please contact admin.");
                        }
                    }
                }
            } else {
                s_logger.info("User " + userAccount.getUsername() + " is disabled/locked");
            }
            return null;
        }
    }

    @Override
    public Pair<User, Account> findUserByApiKey(String apiKey) {
        return _accountDao.findUserAccountByApiKey(apiKey);
    }

    @Override
    public Map<String, String> getKeys(GetUserKeysCmd cmd) {
        final long userId = cmd.getID();

        User user = getActiveUser(userId);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }
        final ControlledEntity account = getAccount(getUserAccountById(userId).getAccountId()); //Extracting the Account from the userID of the requested user.
        checkAccess(CallContext.current().getCallingUser(), account);

        Map<String, String> keys = new HashMap<String, String>();
        keys.put("apikey", user.getApiKey());
        keys.put("secretkey", user.getSecretKey());

        return keys;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REGISTER_FOR_SECRET_API_KEY, eventDescription = "register for the developer API keys")
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd) {
        Account caller = getCurrentCallingAccount();
        final Long userId = cmd.getId();

        User user = getUserIncludingRemoved(userId);
        if (user == null) {
            throw new InvalidParameterValueException("unable to find user by id");
        }

        Account account = _accountDao.findById(user.getAccountId());
        checkAccess(caller, null, true, account);

        // don't allow updating system user
        if (user.getId() == User.UID_SYSTEM) {
            throw new PermissionDeniedException("user id : " + user.getId() + " is system account, update is not allowed");
        }
        // don't allow baremetal system user
        if (BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME.equals(user.getUsername())) {
            throw new PermissionDeniedException("user id : " + user.getId() + " is system account, update is not allowed");
        }

        // generate both an api key and a secret key, update the user table with the keys, return the keys to the user
        final String[] keys = new String[2];
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                keys[0] = createUserApiKey(userId);
                keys[1] = createUserSecretKey(userId);
            }
        });

        return keys;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REGISTER_FOR_SECRET_API_KEY, eventDescription = "register for the developer API keys")
    public String[] createApiKeyAndSecretKey(final long userId) {
        Account caller = getCurrentCallingAccount();
        User user = getUserIncludingRemoved(userId);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }
        Account account = _accountDao.findById(user.getAccountId());
        checkAccess(caller, null, true, account);
        final String[] keys = new String[2];
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                keys[0] = AccountManagerImpl.this.createUserApiKey(userId);
                keys[1] = AccountManagerImpl.this.createUserSecretKey(userId);
            }
        });
        return keys;
    }

    private String createUserApiKey(long userId) {
        try {
            UserVO updatedUser = _userDao.createForUpdate();

            String encodedKey = null;
            Pair<User, Account> userAcct = null;
            int retryLimit = 10;
            do {
                // FIXME: what algorithm should we use for API keys?
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
                SecretKey key = generator.generateKey();
                encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());
                userAcct = _accountDao.findUserAccountByApiKey(encodedKey);
                retryLimit--;
            } while ((userAcct != null) && (retryLimit >= 0));

            if (userAcct != null) {
                return null;
            }
            updatedUser.setApiKey(encodedKey);
            _userDao.update(userId, updatedUser);
            return encodedKey;
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating secret key for user id=" + userId, ex);
        }
        return null;
    }

    private String createUserSecretKey(long userId) {
        try {
            UserVO updatedUser = _userDao.createForUpdate();
            String encodedKey = null;
            int retryLimit = 10;
            UserVO userBySecretKey = null;
            do {
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
                SecretKey key = generator.generateKey();
                encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());
                userBySecretKey = _userDao.findUserBySecretKey(encodedKey);
                retryLimit--;
            } while ((userBySecretKey != null) && (retryLimit >= 0));

            if (userBySecretKey != null) {
                return null;
            }

            updatedUser.setSecretKey(encodedKey);
            _userDao.update(userId, updatedUser);
            return encodedKey;
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating secret key for user id=" + userId, ex);
        }
        return null;
    }

    @Override
    public void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        if (sb.entity() instanceof IPAddressVO) {
            sb.and("accountIdIN", ((IPAddressVO)sb.entity()).getAllocatedToAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", ((IPAddressVO)sb.entity()).getAllocatedInDomainId(), SearchCriteria.Op.EQ);
        } else if (sb.entity() instanceof ProjectInvitationVO) {
            sb.and("accountIdIN", ((ProjectInvitationVO)sb.entity()).getForAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", ((ProjectInvitationVO)sb.entity()).getInDomainId(), SearchCriteria.Op.EQ);
        } else {
            sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        }

        if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);

            if (sb.entity() instanceof IPAddressVO) {
                sb.join("domainSearch", domainSearch, ((IPAddressVO)sb.entity()).getAllocatedInDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else if (sb.entity() instanceof ProjectInvitationVO) {
                sb.join("domainSearch", domainSearch, ((ProjectInvitationVO)sb.entity()).getInDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else {
                sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            }

        }
        if (listProjectResourcesCriteria != null) {
            SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
            if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                accountSearch.and("type", accountSearch.entity().getType(), SearchCriteria.Op.EQ);
            } else if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.SkipProjectResources) {
                accountSearch.and("type", accountSearch.entity().getType(), SearchCriteria.Op.NEQ);
            }

            if (sb.entity() instanceof IPAddressVO) {
                sb.join("accountSearch", accountSearch, ((IPAddressVO)sb.entity()).getAllocatedToAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else if (sb.entity() instanceof ProjectInvitationVO) {
                sb.join("accountSearch", accountSearch, ((ProjectInvitationVO)sb.entity()).getForAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else {
                sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            }
        }
    }

    @Override
    public void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        if (listProjectResourcesCriteria != null) {
            sc.setJoinParameters("accountSearch", "type", Account.ACCOUNT_TYPE_PROJECT);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (isRecursive) {
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }
    }

    //TODO: deprecate this to use the new buildACLSearchParameters with permittedDomains, permittedAccounts, and permittedResources as return
    @Override
    public void buildACLSearchParameters(Account caller, Long id, String accountName, Long projectId, List<Long> permittedAccounts,
            Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject, boolean listAll, boolean forProjectInvitation) {
        Long domainId = domainIdRecursiveListProject.first();
        if (domainId != null) {
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id " + domainId);
            }
            // check permissions
            checkAccess(caller, domain);
        }

        if (accountName != null) {
            if (projectId != null) {
                throw new InvalidParameterValueException("Account and projectId can't be specified together");
            }

            Account userAccount = null;
            Domain domain = null;
            if (domainId != null) {
                userAccount = _accountDao.findActiveAccount(accountName, domainId);
                domain = _domainDao.findById(domainId);
            } else {
                userAccount = _accountDao.findActiveAccount(accountName, caller.getDomainId());
                domain = _domainDao.findById(caller.getDomainId());
            }

            if (userAccount != null) {
                checkAccess(caller, null, false, userAccount);
                // check permissions
                permittedAccounts.add(userAccount.getId());
            } else {
                throw new InvalidParameterValueException("could not find account " + accountName + " in domain " + domain.getUuid());
            }
        }

        // set project information
        if (projectId != null) {
            if (!forProjectInvitation) {
                if (projectId == -1L) {
                    if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                        domainIdRecursiveListProject.third(Project.ListProjectResourcesCriteria.ListProjectResourcesOnly);
                        if (listAll) {
                            domainIdRecursiveListProject.third(ListProjectResourcesCriteria.ListAllIncludingProjectResources);
                        }
                    } else {
                        permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
                        // permittedAccounts can be empty when the caller is not a part of any project (a domain account)
                        if (permittedAccounts.isEmpty()) {
                            permittedAccounts.add(caller.getId());
                        }
                    }
                } else {
                    Project project = _projectMgr.getProject(projectId);
                    if (project == null) {
                        throw new InvalidParameterValueException("Unable to find project by id " + projectId);
                    }
                    if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                        throw new PermissionDeniedException("Account " + caller + " can't access project id=" + projectId);
                    }
                    permittedAccounts.add(project.getProjectAccountId());
                }
            }
        } else {
            if (id == null) {
                domainIdRecursiveListProject.third(Project.ListProjectResourcesCriteria.SkipProjectResources);
            }
            if (permittedAccounts.isEmpty() && domainId == null) {
                if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                    permittedAccounts.add(caller.getId());
                } else if (!listAll) {
                    if (id == null) {
                        permittedAccounts.add(caller.getId());
                    } else if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                        domainIdRecursiveListProject.first(caller.getDomainId());
                        domainIdRecursiveListProject.second(true);
                    }
                } else if (domainId == null) {
                    if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                        domainIdRecursiveListProject.first(caller.getDomainId());
                        domainIdRecursiveListProject.second(true);
                    }
                }
            } else if (domainId != null) {
                if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                    permittedAccounts.add(caller.getId());
                }
            }

        }

    }

    @Override
    public void buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);

        if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the
            // admin case if isRecursive is true
            sb.and("domainPath", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        if (listProjectResourcesCriteria != null) {
            if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                sb.and("accountType", sb.entity().getAccountType(), SearchCriteria.Op.EQ);
            } else if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.SkipProjectResources) {
                sb.and("accountType", sb.entity().getAccountType(), SearchCriteria.Op.NEQ);
            }
        }

    }

    @Override
    public void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {
        if (listProjectResourcesCriteria != null) {
            sc.setParameters("accountType", Account.ACCOUNT_TYPE_PROJECT);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (isRecursive) {
                sc.setParameters("domainPath", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }

    }

    @Override
    public UserAccount getUserByApiKey(String apiKey) {
        return _userAccountDao.getUserByApiKey(apiKey);
    }

    @Override
    public List<String> listAclGroupsByAccount(Long accountId) {
        if (_querySelectors == null || _querySelectors.size() == 0) {
            return new ArrayList<String>();
        }

        QuerySelector qs = _querySelectors.get(0);
        return qs.listAclGroupsByAccount(accountId);
    }

    @Override
    public Long finalyzeAccountId(final String accountName, final Long domainId, final Long projectId, final boolean enabledOnly) {
        if (accountName != null) {
            if (domainId == null) {
                throw new InvalidParameterValueException("Account must be specified with domainId parameter");
            }

            final Domain domain = _domainMgr.getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id");
            }

            final Account account = getActiveAccountByName(accountName, domainId);
            if (account != null && account.getType() != Account.ACCOUNT_TYPE_PROJECT) {
                if (!enabledOnly || account.getState() == Account.State.enabled) {
                    return account.getId();
                } else {
                    throw new PermissionDeniedException("Can't add resources to the account id=" + account.getId() + " in state=" + account.getState() + " as it's no longer active");
                }
            } else {
                // idList is not used anywhere, so removed it now
                // List<IdentityProxy> idList = new ArrayList<IdentityProxy>();
                // idList.add(new IdentityProxy("domain", domainId, "domainId"));
                throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain with specified id");
            }
        }

        if (projectId != null) {
            final Project project = _projectMgr.getProject(projectId);
            if (project != null) {
                if (!enabledOnly || project.getState() == Project.State.Active) {
                    return project.getProjectAccountId();
                } else {
                    final PermissionDeniedException ex = new PermissionDeniedException(
                            "Can't add resources to the project with specified projectId in state=" + project.getState() + " as it's no longer active");
                    ex.addProxyObject(project.getUuid(), "projectId");
                    throw ex;
                }
            } else {
                throw new InvalidParameterValueException("Unable to find project by id");
            }
        }
        return null;
    }

    @Override
    public UserAccount getUserAccountById(Long userId) {
        return _userAccountDao.findById(userId);
    }

    @Override
    public void checkAccess(Account account, ServiceOffering so, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, so, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + account + " to " + so + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + so);
    }

    @Override
    public void checkAccess(Account account, DiskOffering dof, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, dof, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + account + " to " + dof + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + dof);
    }

    @Override
    public void checkAccess(Account account, NetworkOffering nof, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, nof, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + account + " to " + nof + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + nof);
    }

    @Override
    public void checkAccess(Account account, VpcOffering vof, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, vof, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + account + " to " + vof + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + vof);
    }

    @Override
    public void checkAccess(User user, ControlledEntity entity) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(user, entity)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + user + "to " + entity + "by " + checker.getName());
                }
                return;
            }
        }
        throw new PermissionDeniedException("There's no way to confirm " + user + " has access to " + entity);
    }

    @Override
    public String getConfigComponentName() {
        return AccountManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {UseSecretKeyInResponse};
    }
}
