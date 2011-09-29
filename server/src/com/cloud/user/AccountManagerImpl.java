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

package com.cloud.user;

import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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
import javax.naming.ConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.acl.ControlledEntity;
import com.cloud.acl.SecurityChecker;
import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.RegisterCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
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
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { AccountManager.class, AccountService.class })
public class AccountManagerImpl implements AccountManager, AccountService, Manager {
    public static final Logger s_logger = Logger.getLogger(AccountManagerImpl.class);

    private String _name;
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
    private Adapters<UserAuthenticator> _userAuthenticators;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));

    UserVO _systemUser;
    AccountVO _systemAccount;
    @Inject(adapter = SecurityChecker.class)
    Adapters<SecurityChecker> _securityCheckers;
    int _cleanupInterval;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        _systemAccount = _accountDao.findById(AccountVO.ACCOUNT_ID_SYSTEM);
        if (_systemAccount == null) {
            throw new ConfigurationException("Unable to find the system account using " + Account.ACCOUNT_ID_SYSTEM);
        }

        _systemUser = _userDao.findById(UserVO.UID_SYSTEM);
        if (_systemUser == null) {
            throw new ConfigurationException("Unable to find the system user using " + User.UID_SYSTEM);
        }

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> configs = configDao.getConfiguration(params);

        String value = configs.get(Config.AccountCleanupInterval.key());
        _cleanupInterval = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 hour.
        
        _userAuthenticators = locator.getAdapters(UserAuthenticator.class);
        if (_userAuthenticators == null || !_userAuthenticators.isSet()) {
            s_logger.error("Unable to find an user authenticator.");
        }

        return true;
    }

    @Override
    public UserVO getSystemUser() {
        return _systemUser;
    }

    @Override
    public String getName() {
        return _name;
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


    public AccountVO getSystemAccount() {
        if (_systemAccount == null) {
            _systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
        }
        return _systemAccount;
    }

    @Override
    public boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    public boolean isRootAdmin(short accountType) {
        return (accountType == Account.ACCOUNT_TYPE_ADMIN);
    }

    public boolean isResourceDomainAdmin(short accountType) {
        return (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN);
    }

    @Override
    public void checkAccess(Account caller, Domain domain, AccessType accessType) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(caller, domain, accessType)) {
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
    public void checkAccess(Account caller, AccessType accessType, ControlledEntity... entities) {
        HashMap<Long, List<ControlledEntity>> domains = new HashMap<Long, List<ControlledEntity>>();

        for (ControlledEntity entity : entities) {
        	long domainId = entity.getDomainId();
        	if (entity.getAccountId() != -1 && domainId == -1){ // If account exists domainId should too so calculate it. This condition might be hit for templates or entities which miss domainId in their tables           
        		Account account = ApiDBUtils.findAccountById(entity.getAccountId());
        		domainId = account != null ? account.getDomainId() : -1 ;
        	}
        	if (entity.getAccountId() != -1 && domainId != -1 && !(entity instanceof VirtualMachineTemplate)) {
                List<ControlledEntity> toBeChecked = domains.get(entity.getDomainId());
                //for templates, we don't have to do cross domains check
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

            if (!granted) {
                assert false : "How can all of the security checkers pass on checking this check: " + entity;
                throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to " + entity);
            }
        }

        for (Map.Entry<Long, List<ControlledEntity>> domain : domains.entrySet()) {
            for (SecurityChecker checker : _securityCheckers) {
                Domain d =  _domainMgr.getDomain(domain.getKey());
                if (d == null || d.getRemoved() != null) {
                    throw new PermissionDeniedException("Domain is not found.", caller, domain.getValue());
                }
                try {
                    checker.checkAccess(caller, d, accessType);
                } catch (PermissionDeniedException e) {
                    e.addDetails(caller, domain.getValue());
                    throw e;
                }
            }
        }
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

    private boolean doSetUserStatus(long userId, State state) {
        UserVO userForUpdate = _userDao.createForUpdate();
        userForUpdate.setState(state);
        return _userDao.update(Long.valueOf(userId), userForUpdate);
    }

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

        if (!_accountDao.remove(accountId)) {
            s_logger.error("Unable to delete account " + accountId);
            return false;
        }

        List<UserVO> users = _userDao.listByAccount(accountId);

        for (UserVO user : users) {
            _userDao.remove(user.getId());
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Remove account " + accountId);
        }

        return cleanupAccount(account, callerUserId, caller);

    }

    @Override
    public boolean cleanupAccount(AccountVO account, long callerUserId, Account caller) {
        long accountId = account.getId();
        boolean accountCleanupNeeded = false;

        try {
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
                s_logger.debug("Destroying # of vms (accountId=" + accountId + "): " + vms.size());
            }

            //no need to catch exception at this place as expunging vm should pass in order to perform further cleanup
            for (UserVmVO vm : vms) {
                if (!_vmMgr.expunge(vm, callerUserId, caller)) {
                    s_logger.error("Unable to destroy vm: " + vm.getId());
                    accountCleanupNeeded = true;
                }
            }

            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volumeDao.findDetachedByAccount(accountId);
            for (VolumeVO volume : volumes) {
                if (!volume.getState().equals(Volume.State.Destroy)) {
                    try {
                        _storageMgr.destroyVolume(volume);
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
                _remoteAccessVpnMgr.removeVpnUser(accountId, vpnUser.getUsername());
            }

            try {
                for (RemoteAccessVpnVO vpn : remoteAccessVpns) {
                    _remoteAccessVpnMgr.destroyRemoteAccessVpn(vpn.getServerAddressId());
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

                    ReservationContext context = new ReservationContextImpl(null, null, getActiveUser(callerUserId), account);

                    if (!_networkMgr.destroyNetwork(network.getId(), context)) {
                        s_logger.warn("Unable to destroy network " + network + " as a part of account id=" + accountId + " cleanup.");
                        accountCleanupNeeded = true;
                        networksDeleted = false;
                    } else {
                        s_logger.debug("Network " + network.getId() + " successfully deleted as a part of account id=" + accountId + " cleanup.");
                    }
                }
            }

            // delete account specific Virtual vlans (belong to system Public Network) - only when networks are cleaned up
            // successfully
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
        }finally {
            s_logger.info("Cleanup for account " + account.getId() + (accountCleanupNeeded ? " is needed." : " is not needed."));
            if (accountCleanupNeeded) {
                _accountDao.markForCleanup(accountId);
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
                if (!doDisableAccount(accountId)) {
                    s_logger.warn("Failed to disable account " + account + " resources as a part of disableAccount call, marking the account for cleanup");
                    _accountDao.markForCleanup(accountId);
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
                    success = (success && _itMgr.advanceStop(vm, false, getSystemUser(), getSystemAccount()));
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
    public UserAccount createUserAccount(String userName, String password, String firstName, String lastName, String email, String timezone, String accountName, short accountType, Long domainId, String networkDomain) {
        
        if (accountName == null) {
            accountName = userName;
        }
        if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN;
        }
        
        //Validate domain
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        }
        
        //Check permissions
        checkAccess(UserContext.current().getCaller(), domain, null);


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
        
        //create account
        Account account = createAccount(accountName, accountType, domainId, networkDomain);
        long accountId = account.getId();

        //create the first user for the account
        UserVO user = createUser(accountId, userName, password, firstName, lastName, email, timezone);
        
        if(accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN){
            //set registration token
            byte[] bytes = (domainId + accountName + userName + System.currentTimeMillis()).getBytes();
            String registrationToken = UUID.nameUUIDFromBytes(bytes).toString();
            user.setRegistrationToken(registrationToken);
        }
        
        
        txn.commit();
        return _userAccountDao.findById(user.getId());
    }

    @Override
    public UserVO createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId) {

        // default domain to ROOT if not specified
        if (domainId == null) {
            domainId = Domain.ROOT_DOMAIN;
        }
        
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null)  { 
            throw new CloudRuntimeException("The domain " + domainId + " does not exist; unable to create user");
        } else if (domain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The user cannot be created as domain " + domain.getName() + " is being deleted");
        }
        
        checkAccess(UserContext.current().getCaller(), domain, null);
        
        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain id=" + domainId + " to create user");
        } 

        if (!_userAccountDao.validateUsernameInDomain(userName, domainId)) {
            throw new CloudRuntimeException("The user " + userName + " already exists in domain " + domainId);
        }

        UserVO user = createUser(account.getId(), userName, password, firstName, lastName, email, timeZone);

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
        
        //don't allow updating project account
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("unable to find user by id");
        }

        if (account != null && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new PermissionDeniedException("user id : " + id + " is system account, update is not allowed");
        }

        checkAccess(UserContext.current().getCaller(), null, account);

        if (firstName != null) {
            user.setFirstname(firstName);
        }
        if (lastName != null) {
            user.setLastname(lastName);
        }
        if (userName != null) {
            //don't allow to have same user names in the same domain
            List<UserVO> duplicatedUsers = _userDao.findUsersLike(userName);
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
            user.setPassword(password);
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
        
        //don't allow disabling user belonging to project's account
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        
        // If the user is a System user, return an error
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("User id : " + userId + " is a system user, disabling is not allowed");
        }
        
        checkAccess(caller, null, account);

        boolean success = doSetUserStatus(userId, State.disabled);
        if (success) {
            // user successfully disabled
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to disable user " + userId);
        }
    }

    @Override @DB
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

        checkAccess(caller, null, account);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        boolean success = doSetUserStatus(userId, State.enabled);

        // make sure the account is enabled too
        success = success && enableAccount(user.getAccountId());
        
        txn.commit();

        if (success) {
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
        
        //don't allow to lock user of the account of type Project
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }
        
        // If the user is a System user, return an error. We do not allow this
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("user id : " + userId + " is a system user, locking is not allowed");
        }
        
        checkAccess(caller, null, account);

        // make sure the account is enabled too
        // if the user is either locked already or disabled already, don't change state...only lock currently enabled users
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
        
        checkAccess(caller, null, account);
        
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException("Account id : " + accountId + " is a system account, delete is not allowed");
        }
        
        return deleteAccount(account, callerUserId, caller);
    }

    @Override
    public AccountVO enableAccount(String accountName, long domainId) {
        
        // Check if account exists
        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }

        // Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        // Check if user performing the action is allowed to modify this account
        Account caller = UserContext.current().getCaller();
        checkAccess(caller, null, account);

        boolean success = enableAccount(account.getId());
        if (success) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to enable account " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "locking account", async = true)
    public AccountVO lockAccount(String accountName, Long domainId) {
        Account caller = UserContext.current().getCaller();
        
        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active account with name " + accountName + " in domain " + domainId);
        }
        
        checkAccess(caller, null, account);
 
        // don't allow modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("can not lock system account");
        }

        if (lockAccount(account.getId())) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to lock account " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "disabling account", async = true)
    public AccountVO disableAccount(String accountName, Long domainId) throws ConcurrentOperationException, ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();
        
        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }
        
        checkAccess(caller, null, account);
        
        if (disableAccount(account.getId())) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to update account " + accountName + " in domain " + domainId);
        }
    }

    @Override
    public AccountVO updateAccount(UpdateAccountCmd cmd) {
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String newAccountName = cmd.getNewName();
        String networkDomain = cmd.getNetworkDomain();

        boolean success = false;
        Account account = _accountDao.findAccount(accountName, domainId);

        // Check if account exists
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            s_logger.error("Unable to find account " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }

        // Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        // Check if user performing the action is allowed to modify this account
        checkAccess(UserContext.current().getCaller(), _domainMgr.getDomain(account.getDomainId()), null);

        // check if the given account name is unique in this domain for updating
        Account duplicateAcccount = _accountDao.findAccount(newAccountName, domainId);
        if (duplicateAcccount != null && duplicateAcccount.getRemoved() == null && duplicateAcccount.getId() != account.getId()) {// allow
                                                                                                                                  // same
                                                                                                                                  // account
                                                                                                                                  // to
                                                                                                                                  // update
                                                                                                                                  // itself
            throw new InvalidParameterValueException("There already exists an account with the name:" + newAccountName + " in the domain:" + domainId + " with existing account id:"
                    + duplicateAcccount.getId());
        }

        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }
        
        AccountVO acctForUpdate = _accountDao.findById(account.getId());
        acctForUpdate.setAccountName(newAccountName);
        
        if (networkDomain != null) {
            acctForUpdate.setNetworkDomain(networkDomain);
        }
        
        success = _accountDao.update(account.getId(), acctForUpdate);
        
        if (success) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to update account " + accountName + " in domain " + domainId);
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
        
        //don't allow to delete the user from the account of type Project
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Account id : " + user.getAccountId() + " is a system account, delete for user associated with this account is not allowed");
        }
        
        checkAccess(UserContext.current().getCaller(), null, account);
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

                    //Cleanup removed accounts
                    List<AccountVO> removedAccounts = _accountDao.findCleanupsForRemovedAccounts(null);
                    s_logger.info("Found " + removedAccounts.size() + " removed accounts to cleanup");
                    for (AccountVO account : removedAccounts) {
                        s_logger.debug("Cleaning up " + account.getId());
                        try {
                            if (cleanupAccount(account, getSystemUser().getId(), getSystemAccount())) {
                                account.setNeedsCleanup(false);
                                _accountDao.update(account.getId(), account);
                            }
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on account " + account.getId(), e);
                        }
                    }
                    
                    //cleanup disabled accounts
                    List<AccountVO> disabledAccounts = _accountDao.findCleanupsForDisabledAccounts();
                    s_logger.info("Found " + disabledAccounts.size() + " disabled accounts to cleanup");
                    for (AccountVO account : disabledAccounts) {
                        s_logger.debug("Cleaning up " + account.getId());
                        try {
                            if (disableAccount(account.getId())) {
                                account.setNeedsCleanup(false);
                                _accountDao.update(account.getId(), account);
                            }
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on account " + account.getId(), e);
                        }
                    }
                    
                    //cleanup inactive domains
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
                                s_logger.debug("Can't remove inactive domain id=" + domainId + " as it has accounts that need clenaup");
                            } 
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on domain " + domainId, e);
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
    public Account finalizeOwner(Account caller, String accountName, Long domainId) {
        // don't default the owner to the system account
        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM && (accountName == null || domainId == null)) {
            throw new InvalidParameterValueException("Account and domainId are needed for resource creation");
        }

        if (isAdmin(caller.getType()) && accountName != null && domainId != null) {
            Domain domain =  _domainMgr.getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
            }

            Account owner = _accountDao.findActiveAccount(accountName, domainId);
            if (owner == null) {
                throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
            }
            checkAccess(caller, domain, null);

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
    public User getActiveUser(long userId) {
        return _userDao.findById(userId);
    }
    
    @Override
    public User getUser(long userId) {
        return _userDao.findByIdIncludingRemoved(userId);
    }

    @Override
    public Pair<String, Long> finalizeAccountDomainForList(Account caller, String accountName, Long domainId) {
        if (isAdmin(caller.getType())) {
            if (domainId == null && accountName != null) {
                throw new InvalidParameterValueException("accountName and domainId might be specified together");
            } else if (domainId != null) {
                Domain domain = _domainMgr.getDomain(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
                }

                checkAccess(caller, domain, null);

                if (accountName != null) {
                    Account owner = getActiveAccountByName(accountName, domainId);
                    if (owner == null) {
                        throw new InvalidParameterValueException("Unable to find account with name " + accountName + " in domain id=" + domainId);
                    }
                }
            }
        } else if (accountName != null && domainId != null) {
            if (!accountName.equals(caller.getAccountName()) || domainId.longValue() != caller.getDomainId()) {
                throw new PermissionDeniedException("Can't list port forwarding rules for account " + accountName + " in domain " + domainId + ", permission denied");
            }
        } else {
            accountName = caller.getAccountName();
            domainId = caller.getDomainId();
        }

        return new Pair<String, Long>(accountName, domainId);
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
    
    @Override @DB
    public Account createAccount(String accountName, short accountType, Long domainId, String networkDomain) {
        //Validate domain
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

        //Validate account/user/domain settings
        if ( _accountDao.findActiveAccount(accountName, domainId) != null) {
            throw new InvalidParameterValueException("The specified account: " + accountName + " already exists");
        }
        
        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }
        
        //Verify account type
        if ((accountType < Account.ACCOUNT_TYPE_NORMAL) || (accountType > Account.ACCOUNT_TYPE_PROJECT)) {
            throw new InvalidParameterValueException("Invalid account type " + accountType + " given; unable to create user");
        }
        
        if (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            List<DataCenterVO> dc = _dcDao.findZonesByDomainId(domainId);
            if (dc.isEmpty()) {
                throw new InvalidParameterValueException("The account cannot be created as domain " + domain.getName() + " is not associated with any private Zone");
            }
        }
        
        //Create the account
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        Account account = _accountDao.persist(new AccountVO(accountName, domainId, networkDomain, accountType));
        
        if (account == null) {
            throw new CloudRuntimeException("Failed to create account name " + accountName + " in domain id=" + domainId);
        }
        
        Long accountId = account.getId();
        
        //Create resource count records for the account
        _resourceCountDao.createResourceCounts(accountId, ResourceLimit.ResourceOwnerType.Account);
        
        //Create default security group
        _networkGroupMgr.createDefaultSecurityGroup(accountId);
        
        
        txn.commit();
        
        return account;
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")
    public UserVO createUser(long accountId, String userName, String password, String firstName, String lastName, String email, String timezone) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating user: " + userName + ", accountId: " + accountId + " timezone:" + timezone);
        }
        UserVO user = _userDao.persist(new UserVO(accountId, userName, password, firstName, lastName, email, timezone));
        
        return user;
    }
    
    
    @Override
    public void logoutUser(Long userId) {
        UserAccount userAcct = _userAccountDao.findById(userId);
        if (userAcct != null) {
            EventUtils.saveEvent(userId, userAcct.getAccountId(), EventTypes.EVENT_USER_LOGOUT, "user has logged out");
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
    public UserAccount authenticateUser(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        UserAccount user = null;
        if (password != null) {
            user = getUserAccount(username, password, domainId);
        } else {
            String key = _configDao.getValue("security.singlesignon.key");
            if (key == null) {
                // the SSO key is gone, don't authenticate
                return null;
            }

            String singleSignOnTolerance = _configDao.getValue("security.singlesignon.tolerance.millis");
            if (singleSignOnTolerance == null) {
                // the SSO tolerance is gone (how much time before/after system time we'll allow the login request to be valid),
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
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("User: " + username + " in domain " + domainId + " has successfully logged in");
            }
            EventUtils.saveEvent(user.getId(), user.getAccountId(), EventTypes.EVENT_USER_LOGIN, "user has logged in");
            return user;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("User: " + username + " in domain " + domainId + " has failed to log in");
            }
            return null;
        }
    }
    
    private UserAccount getUserAccount(String username, String password, Long domainId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to log in user: " + username + " in domain " + domainId);
        }

        // We only use the first adapter even if multiple have been configured
        Enumeration<UserAuthenticator> en = _userAuthenticators.enumeration();
        UserAuthenticator authenticator = en.nextElement();
        boolean authenticated = authenticator.authenticate(username, password, domainId);

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
            return userAccount;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to authenticate user with username " + username + " in domain " + domainId);
            }
            return null;
        }
    }
    
    @Override
    public Pair<User, Account> findUserByApiKey(String apiKey) {
        return _accountDao.findUserAccountByApiKey(apiKey);
    }
    
    @Override
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd) {
        Long userId = cmd.getId();

        if (getUser(userId) == null) {
            throw new InvalidParameterValueException("unable to find user for id : " + userId);
        }

        // generate both an api key and a secret key, update the user table with the keys, return the keys to the user
        String[] keys = new String[2];
        keys[0] = createUserApiKey(userId);
        keys[1] = createUserSecretKey(userId);

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
    
  
}
