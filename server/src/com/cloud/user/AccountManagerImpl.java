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

import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.event.ActionEventUtils;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.UserAccountJoinDao;
import com.cloud.api.query.vo.ControlledViewEntity;

import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.NetworkManager;
import com.cloud.network.VpnUserVO;
import com.cloud.network.as.AutoScaleManager;
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
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectInvitationVO;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account.State;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
@Local(value = { AccountManager.class, AccountService.class })
public class AccountManagerImpl extends ManagerBase implements AccountManager, Manager {
    public static final Logger s_logger = Logger.getLogger(AccountManagerImpl.class);

    @Inject
    private AccountDao _accountDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private InstanceGroupDao _vmGroupDao;
    @Inject
    private UserAccountDao _userAccountDao;
    @Inject
    private UserAccountJoinDao _userAccountJoinDao;
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
    protected SnapshotDao _snapshotDao;
    @Inject
    protected VMTemplateDao _vmTemplateDao;
    @Inject
    private SecurityGroupManager _networkGroupMgr;
    @Inject
    private NetworkManager _networkMgr;
    @Inject
    private SnapshotManager _snapMgr;
    @Inject
    private UserVmManager _vmMgr;
    @Inject
    private StorageManager _storageMgr;
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
    private DomainRouterDao _routerDao;
    @Inject
    Site2SiteVpnManager _vpnMgr;
    @Inject
    private AutoScaleManager _autoscaleMgr;
    @Inject VolumeManager volumeMgr;

    @Inject
    private List<UserAuthenticator> _userAuthenticators;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));

    int _allowedLoginAttempts;

    UserVO _systemUser;
    AccountVO _systemAccount;

    @Inject
    List<SecurityChecker> _securityCheckers;
    int _cleanupInterval;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _systemAccount = _accountDao.findById(AccountVO.ACCOUNT_ID_SYSTEM);
        if (_systemAccount == null) {
            throw new ConfigurationException("Unable to find the system account using " + Account.ACCOUNT_ID_SYSTEM);
        }

        _systemUser = _userDao.findById(UserVO.UID_SYSTEM);
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
    public boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)
                || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    public boolean isRootAdmin(short accountType) {
        return (accountType == Account.ACCOUNT_TYPE_ADMIN);
    }

    public boolean isResourceDomainAdmin(short accountType) {
        return (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN);
    }

    public boolean isInternalAccount(short accountType) {
        if (isRootAdmin(accountType) || (accountType == Account.ACCOUNT_ID_SYSTEM)) {
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

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to " + domain);
    }

    @Override
    public void checkAccess(Account caller, AccessType accessType, boolean sameOwner, ControlledEntity... entities) {

        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || isRootAdmin(caller.getType())) {
            // no need to make permission checks if the system/root admin makes the call
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("No need to make permission check for System/RootAdmin account, returning true");
            }
            return;
        }

        HashMap<Long, List<ControlledEntity>> domains = new HashMap<Long, List<ControlledEntity>>();
        Long ownerId = null;
        ControlledEntity prevEntity = null;

        for (ControlledEntity entity : entities) {
            long domainId = entity.getDomainId();
            if (entity.getAccountId() != -1 && domainId == -1) { // If account exists domainId should too so calculate
// it. This condition might be hit for templates or entities which miss domainId in their tables
                Account account = ApiDBUtils.findAccountById(entity.getAccountId());
                domainId = account != null ? account.getDomainId() : -1;
            }
            if (entity.getAccountId() != -1 && domainId != -1 && !(entity instanceof VirtualMachineTemplate) && !(accessType != null && accessType == AccessType.UseNetwork)) {
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
                if (checker.checkAccess(caller, entity, accessType)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Access to " + entity + " granted to " + caller + " by " + checker.getName());
                    }
                    granted = true;
                    break;
                }
            }

            if (sameOwner) {
                if (ownerId == null) {
                    ownerId = entity.getAccountId();
                } else if (ownerId.longValue() != entity.getAccountId()) {
                    throw new PermissionDeniedException("Entity " + entity + " and entity " + prevEntity + " belong to different accounts");
                }
                prevEntity = entity;
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
        // We just care for resource domain admin for now. He should be permitted to see only his zone.
        if (isResourceDomainAdmin(caller.getType())) {
            if (zoneId == null)
                return getZoneIdForAccount(caller);
            else if (zoneId.compareTo(getZoneIdForAccount(caller)) != 0)
                throw new PermissionDeniedException("Caller " + caller + "is not allowed to access the zone " + zoneId);
            else
                return zoneId;
        }

        else
            return zoneId;
    }

    private Long getZoneIdForAccount(Account account) {

        // Currently just for resource domain admin
        List<DataCenterVO> dcList = _dcDao.findZonesByDomainId(account.getDomainId());
        if (dcList != null && dcList.size() != 0)
            return dcList.get(0).getId();
        else
            throw new CloudRuntimeException("Failed to find any private zone for Resource domain admin.");

    }

    @DB
    public void updateLoginAttempts(Long id, int attempts, boolean toDisable) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            UserAccountVO user = null;
            user = _userAccountDao.lockRow(id, true);
            user.setLoginAttempts(attempts);
            if(toDisable) {
                user.setState(State.disabled.toString());
            }
            _userAccountDao.update(id, user);
             txn.commit();
        } catch (Exception e) {
            s_logger.error("Failed to update login attempts for user with id " + id );
        }
        txn.close();
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

    @Override
    public boolean lockAccount(long accountId) {
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

        //delete the account record
        if (!_accountDao.remove(accountId)) {
            s_logger.error("Unable to delete account " + accountId);
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Removed account " + accountId);
        }

        return cleanupAccount(account, callerUserId, caller);
    }

    @Override
    public boolean cleanupAccount(AccountVO account, long callerUserId, Account caller) {
        long accountId = account.getId();
        boolean accountCleanupNeeded = false;

        try {
            //cleanup the users from the account
            List<UserVO> users = _userDao.listByAccount(accountId);
            for (UserVO user : users) {
                if (!_userDao.remove(user.getId())) {
                    s_logger.error("Unable to delete user: " + user + " as a part of account " + account + " cleanup");
                    accountCleanupNeeded = true;
                }
            }

            //delete the account from project accounts
            _projectAccountDao.removeAccountFromProjects(accountId);

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

            // Destroy the account's VMs
            List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Expunging # of vms (accountId=" + accountId + "): " + vms.size());
            }

            // no need to catch exception at this place as expunging vm should pass in order to perform further cleanup
            for (UserVmVO vm : vms) {
                if (!_vmMgr.expunge(vm, callerUserId, caller)) {
                    s_logger.error("Unable to expunge vm: " + vm.getId());
                    accountCleanupNeeded = true;
                }
            }

            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volumeDao.findDetachedByAccount(accountId);
            for (VolumeVO volume : volumes) {
                if (!volume.getState().equals(Volume.State.Destroy)) {
                    try {
                        this.volumeMgr.deleteVolume(volume.getId(), caller);
                    } catch (Exception ex) {
                        s_logger.warn("Failed to cleanup volumes as a part of account id=" + accountId + " cleanup due to Exception: ", ex);
                        accountCleanupNeeded = true;
                    }
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
                    _remoteAccessVpnMgr.destroyRemoteAccessVpn(vpn.getServerAddressId(), caller);
                }
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Failed to cleanup remote access vpn resources as a part of account id=" + accountId + " cleanup due to Exception: ", ex);
                accountCleanupNeeded = true;
            }

            // Cleanup security groups
            int numRemoved = _securityGroupDao.removeByAccountId(accountId);
            s_logger.info("deleteAccount: Deleted " + numRemoved + " network groups for account " + accountId);

            // Delete all the networks
            boolean networksDeleted = true;
            s_logger.debug("Deleting networks for account " + account.getId());
            List<NetworkVO> networks = _networkDao.listByOwner(accountId);
            if (networks != null) {
                for (NetworkVO network : networks) {

                    ReservationContext context = new ReservationContextImpl(null, null, getActiveUser(callerUserId), caller);

                    if (!_networkMgr.destroyNetwork(network.getId(), context)) {
                        s_logger.warn("Unable to destroy network " + network + " as a part of account id=" + accountId + " cleanup.");
                        accountCleanupNeeded = true;
                        networksDeleted = false;
                    } else {
                        s_logger.debug("Network " + network.getId() + " successfully deleted as a part of account id=" + accountId + " cleanup.");
                    }
                }
            }

            //Delete all VPCs
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

            if (vpcsDeleted) {
                // release ip addresses belonging to the account
                List<? extends IpAddress> ipsToRelease = _ipAddressDao.listByAccount(accountId);
                for (IpAddress ip : ipsToRelease) {
                    s_logger.debug("Releasing ip " + ip + " as a part of account id=" + accountId + " cleanup");
                    if (!_networkMgr.disassociatePublicIpAddress(ip.getId(), callerUserId, caller)) {
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

            // delete account specific Virtual vlans (belong to system Public Network) - only when networks are cleaned
            // up successfully
            if (networksDeleted) {
                if (!_configMgr.deleteAccountSpecificVirtualRanges(accountId)) {
                    accountCleanupNeeded = true;
                } else {
                    s_logger.debug("Account specific Virtual IP ranges " + " are successfully deleted as a part of account id=" + accountId + " cleanup.");
                }
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
                    if (vm.getType() == Type.User) {
                        success = (success && _itMgr.advanceStop(_userVmDao.findById(vm.getId()), false, getSystemUser(), getSystemAccount()));
                    } else if (vm.getType() == Type.DomainRouter) {
                        success = (success && _itMgr.advanceStop(_routerDao.findById(vm.getId()), false, getSystemUser(), getSystemAccount()));
                    } else {
                        success = (success && _itMgr.advanceStop(vm, false, getSystemUser(), getSystemAccount()));
                    }
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

    // ///////////////////////////////////////////////////
    // ////////////// API commands /////////////////////
    // ///////////////////////////////////////////////////


    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_CREATE, eventDescription = "creating Account")
    public UserAccount createUserAccount(String userName, String password, String firstName, String lastName, String email, String timezone, String accountName, short accountType,
                                         Long domainId, String networkDomain, Map<String, String> details, String accountUUID, String userUUID) {

        if (accountName == null) {
            accountName = userName;
        }
        if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN;
        }

        if (userName.isEmpty()) {
            throw new InvalidParameterValueException("Username is empty");
        }

        if (firstName.isEmpty()) {
            throw new InvalidParameterValueException("Firstname is empty");
        }

        if (lastName.isEmpty()) {
            throw new InvalidParameterValueException("Lastname is empty");
        }

        // Validate domain
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        }

        // Check permissions
        checkAccess(UserContext.current().getCaller(), domain);

        if (!_userAccountDao.validateUsernameInDomain(userName, domainId)) {
            throw new InvalidParameterValueException("The user " + userName + " already exists in domain " + domainId);
        }

        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        // create account
        if(accountUUID == null){
            accountUUID = UUID.randomUUID().toString();
        }
        AccountVO account = createAccount(accountName, accountType, domainId, networkDomain, details, accountUUID);
        long accountId = account.getId();

        // create the first user for the account
        UserVO user = createUser(accountId, userName, password, firstName, lastName, email, timezone, userUUID);

        if (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            // set registration token
            byte[] bytes = (domainId + accountName + userName + System.currentTimeMillis()).getBytes();
            String registrationToken = UUID.nameUUIDFromBytes(bytes).toString();
            user.setRegistrationToken(registrationToken);
        }
        txn.commit();

        //check success
        return _userAccountDao.findById(user.getId());
    }

    @Override
    public UserVO createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId, String userUUID) {

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

        checkAccess(UserContext.current().getCaller(), domain);

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
        user = createUser(account.getId(), userName, password, firstName, lastName, email, timeZone, userUUID);
        return user;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_UPDATE, eventDescription = "updating User")
    public UserAccount updateUser(UpdateUserCmd cmd) {
        Long id = cmd.getId();
        String apiKey = cmd.getApiKey();
        String firstName = cmd.getFirstname();
        String email = cmd.getEmail();
        String lastName = cmd.getLastname();
        String password = cmd.getPassword();
        String secretKey = cmd.getSecretKey();
        String timeZone = cmd.getTimezone();
        String userName = cmd.getUsername();

        // Input validation
        UserVO user = _userDao.getUser(id);

        if (user == null) {
            throw new InvalidParameterValueException("unable to find user by id");
        }

        if ((apiKey == null && secretKey != null) || (apiKey != null && secretKey == null)) {
            throw new InvalidParameterValueException("Please provide an userApiKey/userSecretKey pair");
        }

        // If the account is an admin type, return an error. We do not allow this
        Account account = _accountDao.findById(user.getAccountId());

        // don't allow updating project account
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("unable to find user by id");
        }

        //don't allow updating system account
        if (account != null && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new PermissionDeniedException("user id : " + id + " is system account, update is not allowed");
        }

        checkAccess(UserContext.current().getCaller(), null, true, account);

        if (firstName != null) {
            if (firstName.isEmpty()) {
                throw new InvalidParameterValueException("Firstname is empty");
            }

            user.setFirstname(firstName);
        }
        if (lastName != null) {
            if (lastName.isEmpty()) {
                throw new InvalidParameterValueException("Lastname is empty");
            }

            user.setLastname(lastName);
        }
        if (userName != null) {
            if (userName.isEmpty()) {
                throw new InvalidParameterValueException("Username is empty");
            }

            // don't allow to have same user names in the same domain
            List<UserVO> duplicatedUsers = _userDao.findUsersByName(userName);
            for (UserVO duplicatedUser : duplicatedUsers) {
                if (duplicatedUser.getId() != user.getId()) {
                    Account duplicatedUserAccount = _accountDao.findById(duplicatedUser.getAccountId());
                    if (duplicatedUserAccount.getDomainId() == account.getDomainId()) {
                        throw new InvalidParameterValueException("User with name " + userName + " already exists in domain " + duplicatedUserAccount.getDomainId());
                    }
                }
            }

            user.setUsername(userName);
        }

        if (password != null) {
            String encodedPassword = null;
            for (Iterator<UserAuthenticator> en = _userAuthenticators.iterator(); en.hasNext();) {
                UserAuthenticator authenticator = en.next();
                encodedPassword = authenticator.encode(password);
                if (encodedPassword != null) {
                    break;
                }
            }
            if (encodedPassword == null) {
                throw new CloudRuntimeException("Failed to encode password");
            }
            user.setPassword(encodedPassword);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (timeZone != null) {
            user.setTimezone(timeZone);
        }
        if (apiKey != null) {
            user.setApiKey(apiKey);
        }
        if (secretKey != null) {
            user.setSecretKey(secretKey);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("updating user with id: " + id);
        }
        try {
            // check if the apiKey and secretKey are globally unique
            if (apiKey != null && secretKey != null) {
                Pair<User, Account> apiKeyOwner = _accountDao.findUserAccountByApiKey(apiKey);

                if (apiKeyOwner != null) {
                    User usr = apiKeyOwner.first();
                    if (usr.getId() != id) {
                        throw new InvalidParameterValueException("The api key:" + apiKey + " exists in the system for user id:" + id + " ,please provide a unique key");
                    } else {
                        // allow the updation to take place
                    }
                }
            }

            _userDao.update(id, user);
        } catch (Throwable th) {
            s_logger.error("error updating user", th);
            throw new CloudRuntimeException("Unable to update user " + id);
        }
        return _userAccountDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_DISABLE, eventDescription = "disabling User", async = true)
    public UserAccount disableUser(long userId) {
        Account caller = UserContext.current().getCaller();

        // Check if user exists in the system
        User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        Account account = _accountDao.findById(user.getAccountId());

        // don't allow disabling user belonging to project's account
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        // If the user is a System user, return an error
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("User id : " + userId + " is a system user, disabling is not allowed");
        }

        checkAccess(caller, null, true, account);

        boolean success = doSetUserStatus(userId, State.disabled);
        if (success) {
            // user successfully disabled
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to disable user " + userId);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_USER_ENABLE, eventDescription = "enabling User")
    public UserAccount enableUser(long userId) {

        Account caller = UserContext.current().getCaller();

        // Check if user exists in the system
        User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        Account account = _accountDao.findById(user.getAccountId());

        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        // If the user is a System user, return an error
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("User id : " + userId + " is a system user, enabling is not allowed");
        }

        checkAccess(caller, null, true, account);

        Transaction txn = Transaction.currentTxn();
        txn.start();

        boolean success = doSetUserStatus(userId, State.enabled);

        // make sure the account is enabled too
        success = success && enableAccount(user.getAccountId());

        txn.commit();

        if (success) {
            // whenever the user is successfully enabled, reset the login attempts to zero
            updateLoginAttempts(userId, 0, false);
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to enable user " + userId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_LOCK, eventDescription = "locking User")
    public UserAccount lockUser(long userId) {
        Account caller = UserContext.current().getCaller();

        // Check if user with id exists in the system
        User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }

        Account account = _accountDao.findById(user.getAccountId());

        // don't allow to lock user of the account of type Project
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }

        // If the user is a System user, return an error. We do not allow this
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("user id : " + userId + " is a system user, locking is not allowed");
        }

        checkAccess(caller, null, true, account);

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
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to lock user " + userId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DELETE, eventDescription = "deleting account", async = true)
    // This method deletes the account
    public boolean deleteUserAccount(long accountId) {

        UserContext ctx = UserContext.current();
        long callerUserId = ctx.getCallerUserId();
        Account caller = ctx.getCaller();

        // If the user is a System user, return an error. We do not allow this
        AccountVO account = _accountDao.findById(accountId);

        if (account.getRemoved() != null) {
            s_logger.info("The account:" + account.getAccountName() + " is already removed");
            return true;
        }

        // don't allow removing Project account
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("The specified account does not exist in the system");
        }

        checkAccess(caller, null, true, account);

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("Account id : " + accountId + " is a system account, delete is not allowed");
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
        return deleteAccount(account, callerUserId, caller);
    }

    @Override
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
        Account caller = UserContext.current().getCaller();
        checkAccess(caller, null, true, account);

        boolean success = enableAccount(account.getId());
        if (success) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to enable account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "locking account", async = true)
    public AccountVO lockAccount(String accountName, Long domainId, Long accountId) {
        Account caller = UserContext.current().getCaller();

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

        checkAccess(caller, null, true, account);

        if (lockAccount(account.getId())) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to lock account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "disabling account", async = true)
    public AccountVO disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();

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

        checkAccess(caller, null, true, account);

        if (disableAccount(account.getId())) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to update account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @DB
    public AccountVO updateAccount(UpdateAccountCmd cmd) {
        Long accountId = cmd.getId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String newAccountName = cmd.getNewName();
        String networkDomain = cmd.getNetworkDomain();
        Map<String, String> details = cmd.getDetails();

        boolean success = false;
        Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findEnabledAccount(accountName, domainId);
        }

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
        checkAccess(UserContext.current().getCaller(), _domainMgr.getDomain(account.getDomainId()));

        // check if the given account name is unique in this domain for updating
        Account duplicateAcccount = _accountDao.findActiveAccount(newAccountName, domainId);
        if (duplicateAcccount != null && duplicateAcccount.getId() != account.getId()) {// allow
                                                                                        // same
                                                                                        // account
                                                                                        // to
                                                                                        // update
                                                                                        // itself
            throw new InvalidParameterValueException("There already exists an account with the name:" + newAccountName + " in the domain:" + domainId + " with existing account id:"
                    + duplicateAcccount.getId());
        }

        if (networkDomain != null && !networkDomain.isEmpty()) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        AccountVO acctForUpdate = _accountDao.findById(account.getId());
        acctForUpdate.setAccountName(newAccountName);

        if (networkDomain != null) {
            if (networkDomain.isEmpty()) {
                acctForUpdate.setNetworkDomain(null);
            } else {
                acctForUpdate.setNetworkDomain(networkDomain);
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        success = _accountDao.update(account.getId(), acctForUpdate);

        if (details != null && success) {
            _accountDetailsDao.update(account.getId(), details);
        }

        txn.commit();

        if (success) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to update account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_DELETE, eventDescription = "deleting User")
    public boolean deleteUser(DeleteUserCmd deleteUserCmd) {
        long id = deleteUserCmd.getId();

        UserVO user = _userDao.findById(id);

        if (user == null) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }

        Account account = _accountDao.findById(user.getAccountId());

        // don't allow to delete the user from the account of type Project
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Account id : " + user.getAccountId() + " is a system account, delete for user associated with this account is not allowed");
        }

        checkAccess(UserContext.current().getCaller(), null, true, account);
        return _userDao.remove(id);
    }

    public class ResourceCountCalculateTask implements Runnable {
        @Override
        public void run() {

        }
    }

    protected class AccountCleanupTask implements Runnable {
        @Override
        public void run() {
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

                Transaction txn = null;
                try {
                    txn = Transaction.open(Transaction.CLOUD_DB);

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
                                _projectMgr.deleteProject(UserContext.current().getCaller(), UserContext.current().getCallerUserId(), project);
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
                    if (txn != null) {
                        txn.close();
                    }

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

        if (isAdmin(caller.getType()) && accountName != null && domainId != null) {
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
        } else if (!isAdmin(caller.getType()) && accountName != null && domainId != null) {
            if (!accountName.equals(caller.getAccountName()) || domainId.longValue() != caller.getDomainId()) {
                throw new PermissionDeniedException("Can't create/list resources for account " + accountName + " in domain " + domainId + ", permission denied");
            } else {
                return caller;
            }
        } else {
            if ((accountName == null && domainId != null) || (accountName != null && domainId == null)) {
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
    public Account getActiveAccountById(Long accountId) {
        if (accountId == null) {
            throw new InvalidParameterValueException("AccountId is required by account search");
        } else {
            return _accountDao.findById(accountId);
        }
    }

    @Override
    public Account getAccount(Long accountId) {
        if (accountId == null) {
            throw new InvalidParameterValueException("AccountId is required by account search");
        } else {
            return _accountDao.findByIdIncludingRemoved(accountId);
        }
    }

    @Override
    public RoleType getRoleType(Account account) {
        RoleType roleType = RoleType.Unknown;
        if (account == null)
            return roleType;
        short accountType = account.getType();

        // Account type to role type translation
        switch (accountType) {
            case Account.ACCOUNT_TYPE_ADMIN:
                roleType = RoleType.Admin;
                break;
            case Account.ACCOUNT_TYPE_DOMAIN_ADMIN:
                roleType = RoleType.DomainAdmin;
                break;
            case Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN:
                roleType = RoleType.ResourceAdmin;
                break;
            case Account.ACCOUNT_TYPE_NORMAL:
                roleType = RoleType.User;
                break;
        }
        return roleType;
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
    public Pair<List<Long>, Long> finalizeAccountDomainForList(Account caller, String accountName, Long domainId, Long projectId) {
        List<Long> permittedAccounts = new ArrayList<Long>();

        if (isAdmin(caller.getType())) {
            if (domainId == null && accountName != null) {
                throw new InvalidParameterValueException("accountName and domainId might be specified together");
            } else if (domainId != null) {
                Domain domain = _domainMgr.getDomain(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
                }

                checkAccess(caller, domain);

                if (accountName != null) {
                    Account owner = getActiveAccountByName(accountName, domainId);
                    if (owner == null) {
                        throw new InvalidParameterValueException("Unable to find account with name " + accountName + " in domain id=" + domainId);
                    }

                    permittedAccounts.add(owner.getId());
                }
            }
        } else if (accountName != null && domainId != null) {
            if (!accountName.equals(caller.getAccountName()) || domainId.longValue() != caller.getDomainId()) {
                throw new PermissionDeniedException("Can't list port forwarding rules for account " + accountName + " in domain " + domainId + ", permission denied");
            }
            permittedAccounts.add(getActiveAccountByName(accountName, domainId).getId());
        } else {
            permittedAccounts.add(caller.getAccountId());
        }

        if (domainId == null && caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            domainId = caller.getDomainId();
        }

        // set project information
        if (projectId != null) {
            if (projectId.longValue() == -1) {
                permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
            } else {
                permittedAccounts.clear();
                Project project = _projectMgr.getProject(projectId);
                if (project == null) {
                    throw new InvalidParameterValueException("Unable to find project by id " + projectId);
                }
                if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                    throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
                }
                permittedAccounts.add(project.getProjectAccountId());
            }
        }

        return new Pair<List<Long>, Long>(permittedAccounts, domainId);
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
    public AccountVO createAccount(String accountName, short accountType, Long domainId, String networkDomain, Map details, String uuid) {
        // Validate domain
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        }

        if (domain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The account cannot be created as domain " + domain.getName() + " is being deleted");
        }

        if ((domainId != DomainVO.ROOT_DOMAIN) && (accountType == Account.ACCOUNT_TYPE_ADMIN)) {
            throw new InvalidParameterValueException("Invalid account type " + accountType + " given for an account in domain " + domainId + "; unable to create user.");
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
        Transaction txn = Transaction.currentTxn();
        txn.start();

        AccountVO account = _accountDao.persist(new AccountVO(accountName, domainId, networkDomain, accountType, uuid));

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
        txn.commit();

        return account;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")
    public UserVO createUser(long accountId, String userName, String password, String firstName, String lastName, String email, String timezone, String userUUID) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating user: " + userName + ", accountId: " + accountId + " timezone:" + timezone);
        }

        String encodedPassword = null;
        for (UserAuthenticator  authenticator : _userAuthenticators) {
            encodedPassword = authenticator.encode(password);
            if (encodedPassword != null) {
                break;
            }
        }
        if (encodedPassword == null) {
            throw new CloudRuntimeException("Failed to encode password");
        }

        if(userUUID == null){
            userUUID =  UUID.randomUUID().toString();
        }
        UserVO user = _userDao.persist(new UserVO(accountId, userName, encodedPassword, firstName, lastName, email, timezone, userUUID));

        return user;
    }

    @Override
    public void logoutUser(Long userId) {
        UserAccount userAcct = _userAccountDao.findById(userId);
        if (userAcct != null) {
            ActionEventUtils.onActionEvent(userId, userAcct.getAccountId(), userAcct.getDomainId(), EventTypes.EVENT_USER_LOGOUT, "user has logged out");
        } // else log some kind of error event? This likely means the user doesn't exist, or has been deleted...
    }

    @Override
    public UserAccount getUserAccount(String username, Long domainId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieiving user: " + username + " in domain " + domainId);
        }

        UserAccount userAccount = _userAccountDao.getUserAccount(username, domainId);
        if (userAccount == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find user with name " + username + " in domain " + domainId);
            }
            return null;
        }

        return userAccount;
    }

    @Override
    public UserAccount authenticateUser(String username, String password, Long domainId, String loginIpAddress, Map<String, Object[]> requestParameters) {
        UserAccount user = null;
        if (password != null) {
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

            // - build a request string with sorted params, make sure it's all lowercase
            // - sign the request, verify the signature is the same
            List<String> parameterNames = new ArrayList<String>();

            for (Object paramNameObj : requestParameters.keySet()) {
                parameterNames.add((String) paramNameObj); // put the name in a list that we'll sort later
            }

            Collections.sort(parameterNames);

            try {
                for (String paramName : parameterNames) {
                    // parameters come as name/value pairs in the form String/String[]
                    String paramValue = ((String[]) requestParameters.get(paramName))[0];

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

                        if (unsignedRequest == null) {
                            unsignedRequest = paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                        } else {
                            unsignedRequest = unsignedRequest + "&" + paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                        }
                    }
                }

                if ((signature == null) || (timestamp == 0L)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Missing parameters in login request, signature = " + signature + ", timestamp = " + timestamp);
                    }
                    return null;
                }

                unsignedRequest = unsignedRequest.toLowerCase();

                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                mac.init(keySpec);
                mac.update(unsignedRequest.getBytes());
                byte[] encryptedBytes = mac.doFinal();
                String computedSignature = new String(Base64.encodeBase64(encryptedBytes));
                boolean equalSig = signature.equals(computedSignature);
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
            //don't allow to authenticate system user
            if (user.getId() == User.UID_SYSTEM) {
                s_logger.error("Failed to authenticate user: " + username + " in domain " + domainId);
                return null;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("User: " + username + " in domain " + domainId + " has successfully logged in");
            }
            if (NetUtils.isValidIp(loginIpAddress)) {
                ActionEventUtils.onActionEvent(user.getId(), user.getAccountId(), user.getDomainId(), EventTypes.EVENT_USER_LOGIN,
                        "user has logged in from IP Address " + loginIpAddress);
            } else {
                ActionEventUtils.onActionEvent(user.getId(), user.getAccountId(), user.getDomainId(), EventTypes.EVENT_USER_LOGIN,
                        "user has logged in. The IP Address cannot be determined");
            }
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

        boolean authenticated = false;
        for(UserAuthenticator authenticator : _userAuthenticators) {
            if (authenticator.authenticate(username, password, domainId, requestParameters)) {
                authenticated = true;
                break;
            }
        }

        if (authenticated) {
            UserAccount userAccount = _userAccountDao.getUserAccount(username, domainId);
            if (userAccount == null) {
                s_logger.warn("Unable to find an authenticated user with username " + username + " in domain " + domainId);
                return null;
            }

            Domain domain = _domainMgr.getDomain(domainId);
            String domainName = null;
            if (domain != null) {
                domainName = domain.getName();
            }

            if (!userAccount.getState().equalsIgnoreCase(Account.State.enabled.toString()) || !userAccount.getAccountState().equalsIgnoreCase(Account.State.enabled.toString())) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("User " + username + " in domain " + domainName + " is disabled/locked (or account is disabled/locked)");
                }
                throw new CloudAuthenticationException("User " + username + " in domain " + domainName + " is disabled/locked (or account is disabled/locked)");
                // return null;
            }
            // Whenever the user is able to log in successfully, reset the login attempts to zero
            if(!isInternalAccount(userAccount.getType()))
                updateLoginAttempts(userAccount.getId(), 0, false);

            return userAccount;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to authenticate user with username " + username + " in domain " + domainId);
            }

            UserAccount userAccount = _userAccountDao.getUserAccount(username, domainId);
            if (userAccount != null) {
                if (userAccount.getState().equalsIgnoreCase(Account.State.enabled.toString())) {
                    if (!isInternalAccount(userAccount.getType())) {
                        //Internal accounts are not disabled
                        int attemptsMade = userAccount.getLoginAttempts() + 1;
                        if (attemptsMade < _allowedLoginAttempts) {
                            updateLoginAttempts(userAccount.getId(), attemptsMade, false);
                            s_logger.warn("Login attempt failed. You have " + ( _allowedLoginAttempts - attemptsMade ) + " attempt(s) remaining");
                        } else {
                            updateLoginAttempts(userAccount.getId(), _allowedLoginAttempts, true);
                            s_logger.warn("User " + userAccount.getUsername() + " has been disabled due to multiple failed login attempts." +
                                    " Please contact admin.");
                        }
                    }
                } else {
                    s_logger.info("User " + userAccount.getUsername() + " is disabled/locked");
                }
            } else {
                s_logger.warn("Authentication failure: No user with name " + username + " for domainId " + domainId);
            }
            return null;
        }
    }

    @Override
    public Pair<User, Account> findUserByApiKey(String apiKey) {
        return _accountDao.findUserAccountByApiKey(apiKey);
    }

    @Override @DB
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd) {
        Long userId = cmd.getId();

        User user = getUserIncludingRemoved(userId);
        if (user == null) {
            throw new InvalidParameterValueException("unable to find user by id");
        }

        //don't allow updating system user
        if (user.getId() == User.UID_SYSTEM) {
            throw new PermissionDeniedException("user id : " + user.getId() + " is system account, update is not allowed");
        }

        // generate both an api key and a secret key, update the user table with the keys, return the keys to the user
        String[] keys = new String[2];
        Transaction txn = Transaction.currentTxn();
        txn.start();
        keys[0] = createUserApiKey(userId);
        keys[1] = createUserSecretKey(userId);
        txn.commit();

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
    public void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> sb,
            Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria) {

        if (sb.entity() instanceof IPAddressVO) {
            sb.and("accountIdIN", ((IPAddressVO) sb.entity()).getAllocatedToAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", ((IPAddressVO) sb.entity()).getAllocatedInDomainId(), SearchCriteria.Op.EQ);
        } else if (sb.entity() instanceof ProjectInvitationVO) {
            sb.and("accountIdIN", ((ProjectInvitationVO) sb.entity()).getForAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", ((ProjectInvitationVO) sb.entity()).getInDomainId(), SearchCriteria.Op.EQ);
        } else {
            sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        }

        if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);

            if (sb.entity() instanceof IPAddressVO) {
                sb.join("domainSearch", domainSearch, ((IPAddressVO) sb.entity()).getAllocatedInDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else if (sb.entity() instanceof ProjectInvitationVO) {
                sb.join("domainSearch", domainSearch, ((ProjectInvitationVO) sb.entity()).getInDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
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
                sb.join("accountSearch", accountSearch, ((IPAddressVO) sb.entity()).getAllocatedToAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else if (sb.entity() instanceof ProjectInvitationVO) {
                sb.join("accountSearch", accountSearch, ((ProjectInvitationVO) sb.entity()).getForAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else {
                sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            }
        }
    }

    @Override
    public void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc,
            Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria) {

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

    @Override
    public void buildACLSearchParameters(Account caller, Long id, String accountName, Long projectId, List<Long>
    permittedAccounts, Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject,
            boolean listAll, boolean forProjectInvitation) {
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
            if (domainId != null) {
                userAccount = _accountDao.findActiveAccount(accountName, domainId);
            } else {
                userAccount = _accountDao.findActiveAccount(accountName, caller.getDomainId());
            }

            if (userAccount != null) {
                checkAccess(caller, null, false, userAccount);
                //check permissions
                permittedAccounts.add(userAccount.getId());
            } else {
                throw new InvalidParameterValueException("could not find account " + accountName + " in domain " + domainId);
            }
        }

        // set project information
        if (projectId != null) {
            if (!forProjectInvitation) {
                if (projectId.longValue() == -1) {
                    if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                        permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
                    } else {
                        domainIdRecursiveListProject.third(Project.ListProjectResourcesCriteria.ListProjectResourcesOnly);
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
            }

        }
    }

    @Override
    public void buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> sb, Long domainId,
            boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria) {

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
    public void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> sc,
            Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria) {

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
}
