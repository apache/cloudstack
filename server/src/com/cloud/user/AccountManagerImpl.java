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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.acl.ControlledEntity;
import com.cloud.acl.SecurityChecker;
import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.CreateAccountCmd;
import com.cloud.api.commands.CreateDomainCmd;
import com.cloud.api.commands.CreateUserCmd;
import com.cloud.api.commands.DeleteAccountCmd;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.DisableAccountCmd;
import com.cloud.api.commands.DisableUserCmd;
import com.cloud.api.commands.EnableAccountCmd;
import com.cloud.api.commands.EnableUserCmd;
import com.cloud.api.commands.LockUserCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateResourceCountCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.ResourceLimit.OwnerType;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpn.RemoteAccessVpnService;
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
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
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
    private DomainDao _domainDao;
    @Inject
    private ResourceLimitDao _resourceLimitDao;
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
    private IPAddressDao _ipAddressDao;
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
    private AlertManager _alertMgr;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));
    
    protected SearchBuilder<ResourceCountVO> ResourceCountSearch;

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
        
        ResourceCountSearch = _resourceCountDao.createSearchBuilder();
        ResourceCountSearch.and("id", ResourceCountSearch.entity().getId(), SearchCriteria.Op.IN);
        ResourceCountSearch.and("accountId", ResourceCountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ResourceCountSearch.and("domainId", ResourceCountSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ResourceCountSearch.done();

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

    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta) {
        //don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            s_logger.trace("Not incrementing resource count for system accounts, returning");
            return;
        }
        long numToIncrement = (delta.length == 0) ? 1 : delta[0].longValue();

        if (!updateResourceCount(accountId, type, true, numToIncrement)) {
            //we should fail the operation (resource creation) when failed to update the resource count
            throw new CloudRuntimeException("Failed to increment resource count of type " + type + " for account id=" + accountId);
        }
    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta) {
        //don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            s_logger.trace("Not decrementing resource count for system accounts, returning");
            return;
        }
        long numToDecrement = (delta.length == 0) ? 1 : delta[0].longValue();
        
        if (!updateResourceCount(accountId, type, false, numToDecrement)) {
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, "Failed to decrement resource count of type " + type + " for account id=" + accountId, 
                        "Failed to decrement resource count of type " + type + " for account id=" + accountId + "; use updateResourceCount API to recalculate/fix the problem");
        }
    }

    @Override
    public long findCorrectResourceLimit(long accountId, ResourceType type) {
        long max = -1;

        ResourceLimitVO limit = _resourceLimitDao.findByAccountIdAndType(accountId, type);

        // Check if limit is configured for account
        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            // If the account has an no limit set, then return global default account limits
            try {
                switch (type) {
                case public_ip:
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountPublicIPs.key()));
                    break;
                case snapshot:
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountSnapshots.key()));
                    break;
                case template:
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountTemplates.key()));
                    break;
                case user_vm:
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountUserVms.key()));
                    break;
                case volume:
                    max = Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountVolumes.key()));
                    break;
                }
            } catch (NumberFormatException nfe) {
                s_logger.error("Invalid value is set for the default account limit.");
            }
        }

        return max;
    }

    @Override
    public long findCorrectResourceLimit(DomainVO domain, ResourceType type) {
        long max = -1;

        // Check account
        ResourceLimitVO limit = _resourceLimitDao.findByDomainIdAndType(domain.getId(), type);

        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            // check domain hierarchy
            Long domainId = domain.getParent();
            while ((domainId != null) && (limit == null)) {
                limit = _resourceLimitDao.findByDomainIdAndType(domainId, type);
                DomainVO tmpDomain = _domainDao.findById(domainId);
                domainId = tmpDomain.getParent();
            }

            if (limit != null) {
                max = limit.getMax().longValue();
            }
        }

        return max;
    }

    @Override @DB
    public boolean resourceLimitExceeded(Account account, ResourceType type, long... count) {
        long numResources = ((count.length == 0) ? 1 : count[0]);

        // Don't place any limits on system or admin accounts
        if (isRootAdmin(account.getType())) {
            return false;
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            //Lock all rows first so nobody else can read it 
            Set<Long> rowIdsToLock = _resourceCountDao.listAllRowsToUpdateForAccount(account.getId(), account.getDomainId(), type);
            SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
            sc.setParameters("id", rowIdsToLock.toArray());
            _resourceCountDao.lockRows(sc, null, true);

            // Check account limits
            long accountLimit = findCorrectResourceLimit(account.getId(), type);
            long potentialCount = _resourceCountDao.getAccountCount(account.getId(), type) + numResources;
            if (accountLimit != -1 && potentialCount > accountLimit) {
                return true;
            }

            // check all domains in the account's domain hierarchy
            Long domainId = account.getDomainId();
            while (domainId != null) {
                ResourceLimitVO domainLimit = _resourceLimitDao.findByDomainIdAndType(domainId, type);
                if (domainLimit != null && domainLimit.getMax().longValue() != -1) {
                    long domainCount = _resourceCountDao.getDomainCount(domainId, type);
                    if ((domainCount + numResources) > domainLimit.getMax().longValue()) {
                        return true;
                    }
                }
                DomainVO domain = _domainDao.findById(domainId);
                domainId = domain.getParent();
            }
            
            return false;
        } finally {
            txn.commit();
        }
    }

    @Override
    public long getResourceCount(AccountVO account, ResourceType type) {
        return _resourceCountDao.getAccountCount(account.getId(), type);
    }


    @Override
    public List<ResourceLimitVO> searchForLimits(Long id, String accountName, Long domainId, Integer type, Long startIndex, Long pageSizeVal) {
       Account caller = UserContext.current().getCaller();
       List<ResourceLimitVO> limits = new ArrayList<ResourceLimitVO>();
       boolean isAccount = true;
       
       Long accountId = null;
       
       if (!isAdmin(caller.getType())) {
           accountId = caller.getId();
           domainId = null;
       } else {
           if (domainId != null) {
               //verify domain information and permissions
               Domain domain = _domainDao.findById(domainId);
               if (domain == null) {
                   //return empty set
                   return limits;
               }
               
               checkAccess(caller, domain);
               
               if (accountName != null) {
                   //Verify account information and permissions
                   Account account = _accountDao.findAccount(accountName, domainId);
                   if (account == null) {
                       //return empty set
                       return limits;
                   }
                   
                   checkAccess(caller, null, account);
                   
                   accountId = account.getId();
                   domainId = null;
               } 
           }
       }
       
       // Map resource type
       ResourceType resourceType = null;
       if (type != null) {
           try {
               resourceType = ResourceType.values()[type];
           } catch (ArrayIndexOutOfBoundsException e) {
               throw new InvalidParameterValueException("Please specify a valid resource type.");
           }
       }

       //If id is passed in, get the record and return it if permission check has passed
       if (id != null) {
           ResourceLimitVO vo = _resourceLimitDao.findById(id);
           if (vo.getAccountId() != null) {
               checkAccess(caller, null, _accountDao.findById(vo.getAccountId()));
               limits.add(vo);
           } else if (vo.getDomainId() != null) {
               checkAccess(caller, _domainDao.findById(vo.getDomainId()));
               limits.add(vo);
           }
           
           return limits;
       } 
       
       
       //If account is not specified, default it to caller account
       if (accountId == null) {
           if (domainId == null) {
               accountId = caller.getId();
               isAccount = true;
           } else {
               isAccount = false;
           }
       } else {
           isAccount = true;
       }   
       
       SearchBuilder<ResourceLimitVO> sb = _resourceLimitDao.createSearchBuilder();
       sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
       sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
       sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);

       SearchCriteria<ResourceLimitVO> sc = sb.create();
       Filter filter = new Filter(ResourceLimitVO.class, "id", true, startIndex, pageSizeVal);
       
       if (accountId != null) {
           sc.setParameters("accountId", accountId);
       } 
       
       if (domainId != null) {
           sc.setParameters("domainId", domainId);
           sc.setParameters("accountId", (Object[])null);
       }
       
       if (resourceType != null) {
           sc.setParameters("type", resourceType);
       }
       
       List<ResourceLimitVO> foundLimits = _resourceLimitDao.search(sc, filter);
       
       if (resourceType != null) {
           if (foundLimits.isEmpty()) {
               if (isAccount) {
                   limits.add(new ResourceLimitVO(null, accountId, resourceType, findCorrectResourceLimit(accountId, resourceType)));
               } else {
                   limits.add(new ResourceLimitVO(domainId, null, resourceType, findCorrectResourceLimit(_domainDao.findById(domainId), resourceType)));
               }
           } else {
               limits.addAll(foundLimits);
           }
       } else {
           limits.addAll(foundLimits);
           
           //see if any limits are missing from the table, and if yes - get it from the config table and add
           ResourceType[] resourceTypes = ResourceCount.ResourceType.values();
           if (foundLimits.size() != resourceTypes.length) {
               List<String> accountLimitStr = new ArrayList<String>();
               List<String> domainLimitStr = new ArrayList<String>();
               for (ResourceLimitVO foundLimit : foundLimits) {
                   if (foundLimit.getAccountId() != null) {
                       accountLimitStr.add(foundLimit.getType().toString());
                   } else {
                       domainLimitStr.add(foundLimit.getType().toString());
                   }
               }
               
               //get default from config values
               if (isAccount) {
                   if (accountLimitStr.size() < resourceTypes.length) {
                       for (ResourceType rt : resourceTypes) {
                           if (!accountLimitStr.contains(rt.toString())) {
                               limits.add(new ResourceLimitVO(null, accountId, rt, findCorrectResourceLimit(accountId, rt)));
                           }
                       }
                   }
                   
               } else {
                   if (domainLimitStr.size() < resourceTypes.length) {
                       for (ResourceType rt : resourceTypes) {
                           if (!domainLimitStr.contains(rt.toString())) {
                               limits.add(new ResourceLimitVO(domainId, null, rt, findCorrectResourceLimit(_domainDao.findById(domainId), rt)));
                           }
                       }
                   }
               }
           }
       }
       
       return limits;
    }

    @Override
    public ResourceLimitVO updateResourceLimit(String accountName, Long domainId, int typeId, Long max) {
        Account caller = UserContext.current().getCaller();
        Long ownerId = null;
        OwnerType ownerType = OwnerType.Account;

        if (max == null) {
            max = new Long(-1);
        } else if (max < -1) {
            throw new InvalidParameterValueException("Please specify either '-1' for an infinite limit, or a limit that is at least '0'.");
        }

        // Map resource type
        ResourceType resourceType = null;
        try {
            resourceType = ResourceType.values()[typeId];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidParameterValueException("Please specify a valid resource type.");
        }
        
        //check permisions
        if (domainId != null) {
            //verify domain information and permissions
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id " + domainId);
            }
            
            checkAccess(caller, domain);
            
            if (accountName != null) {
                //Verify account information and permissions
                Account account = _accountDao.findAccount(accountName, domainId);
                
                checkAccess(caller, null, account);
                
                if (account.getType() == Account.ACCOUNT_ID_SYSTEM) {
                    throw new InvalidParameterValueException("Can't update system account");
                }
                
                ownerId = account.getId();
            } else {
                if ((caller.getDomainId() == domainId.longValue()) && caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                    // if the admin is trying to update their own domain, disallow...
                    throw new PermissionDeniedException("Unable to update resource limit for domain " + domainId + ", permission denied");
                }
                
                Long parentDomainId = domain.getParent();
                if (parentDomainId != null) {
                    DomainVO parentDomain = _domainDao.findById(parentDomainId);
                    long parentMaximum = findCorrectResourceLimit(parentDomain, resourceType);
                    if ((parentMaximum >= 0) && (max.longValue() > parentMaximum)) {
                        throw new InvalidParameterValueException("Domain (id: " + domainId + ") has maximum allowed resource limit " + parentMaximum + " for " + resourceType
                                + ", please specify a value less that or equal to " + parentMaximum);
                    }
                }
                
                ownerId = domain.getId();
                ownerType = OwnerType.Domain;
            }
        }

        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndType(ownerId, ownerType, resourceType);
        if (limit != null) {
            // Update the existing limit
            _resourceLimitDao.update(limit.getId(), max);
            return _resourceLimitDao.findById(limit.getId());
        } else {
            if (ownerType == OwnerType.Account) {
                return _resourceLimitDao.persist(new ResourceLimitVO(null, ownerId, resourceType, max));
            } else {
                return _resourceLimitDao.persist(new ResourceLimitVO(ownerId, null, resourceType, max));
            }
        }
    }

    @Override @DB
    public long updateAccountResourceCount(long accountId, ResourceType type) {
        Long count=null;

        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            // this lock guards against the updates to user_vm, volume, snapshot, public _ip and template table 
            // as any resource creation precedes with the resourceLimitExceeded check which needs this lock too
            SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
            sc.setParameters("accountId", accountId);
            _resourceCountDao.lockRows(sc, null, true);
            
            switch (type) {
            case user_vm:
                count = _userVmDao.countAllocatedVMsForAccount(accountId);
                break;
            case volume:
                count = _volumeDao.countAllocatedVolumesForAccount(accountId);
                long virtualRouterCount = _vmDao.countAllocatedVirtualRoutersForAccount(accountId);
                count = count - virtualRouterCount;  // don't count the volumes of virtual router
                break;
            case snapshot:
                count = _snapshotDao.countSnapshotsForAccount(accountId);
                break;
            case public_ip:
                count = _ipAddressDao.countAllocatedIPsForAccount(accountId);
                break;
            case template:
                count = _vmTemplateDao.countTemplatesForAccount(accountId);
                break;
            }
            _resourceCountDao.setAccountCount(accountId, type, (count == null) ? 0 : count.longValue());
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to update resource count for account with Id" + accountId);
        } finally {
            txn.commit();
        }

        return (count==null)?0:count.longValue();
    }

    @Override @DB
    public long updateDomainResourceCount(long domainId, ResourceType type) {
        long count=0;

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        try {
            //Lock all rows first so nobody else can read it 
            Set<Long> rowIdsToLock = _resourceCountDao.listRowsToUpdateForDomain(domainId, type);
            SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
            sc.setParameters("id", rowIdsToLock.toArray());
            _resourceCountDao.lockRows(sc, null, true);
            
            List<DomainVO> domainChildren = _domainDao.findImmediateChildrenForParent(domainId);
            // for each child domain update the resource count
            for (DomainVO domainChild : domainChildren) {
                long domainCount = updateDomainResourceCount(domainChild.getId(), type);
                count = count + domainCount; // add the child domain count to parent domain count
            }

            List<AccountVO> accounts = _accountDao.findActiveAccountsForDomain(domainId);
            for (AccountVO account : accounts) {
                long accountCount = updateAccountResourceCount(account.getId(), type);
                count = count + accountCount; // add account's resource count to parent domain count
            }

            _resourceCountDao.setDomainCount(domainId, type, count);
       } catch (Exception e) {
           throw new CloudRuntimeException("Failed to update resource count for domain with Id " + domainId);
       } finally {
          txn.commit();
       }

       return count;
    }

    @Override
    public List<ResourceCountVO> updateResourceCount(UpdateResourceCountCmd cmd) throws InvalidParameterValueException, CloudRuntimeException, PermissionDeniedException{
        Account callerAccount = UserContext.current().getCaller();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        long count=0;
        List<ResourceCountVO> counts = new ArrayList<ResourceCountVO>();
        List<ResourceType> resourceTypes = new ArrayList<ResourceType>();

        ResourceType resourceType=null;
        Integer typeId = cmd.getResourceType();

        if (typeId != null) {
            try {
                resourceType = ResourceType.values()[typeId];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InvalidParameterValueException("Please specify a valid resource type.");
            }
        }

        DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("Please specify a valid domain ID.");
        }
        checkAccess(callerAccount, domain);

        if (accountName != null) {
            Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
            if (userAccount == null) {
                throw new InvalidParameterValueException("unable to find account by name " + accountName + " in domain with id " + domainId);
            }
            accountId = userAccount.getId();
        }

        try {
            if (resourceType != null) {
            	resourceTypes.add(resourceType);
            } else {
            	resourceTypes = Arrays.asList(ResourceType.values());
            }

            for (ResourceType type : resourceTypes) {
                if (accountId != null) {
                    count = updateAccountResourceCount(accountId, type);
                    counts.add(new ResourceCountVO(accountId, domainId, type, count));
                } else {
                    count = updateDomainResourceCount(domainId, type);
                    counts.add(new ResourceCountVO(accountId, domainId, type, count));
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        return counts;
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
    public void checkAccess(Account caller, Domain domain) throws PermissionDeniedException {
        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM) {
            //no need to make permission checks if the system makes the call
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("No need to make permission check for System account, returning true");
            } 
            return;
        }
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
    public void checkAccess(Account caller, AccessType accessType, ControlledEntity... entities) {
        HashMap<Long, List<ControlledEntity>> domains = new HashMap<Long, List<ControlledEntity>>();
        
        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || isRootAdmin(caller.getType())) {
            //no need to make permission checks if the system/root admin makes the call
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("No need to make permission check for System/RootAdmin account, returning true");
            } 
            return;
        }

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
                Domain d = _domainDao.findById(domain.getKey());
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

    private boolean lockAccountInternal(long accountId) {
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
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_CREATE, eventDescription = "creating Account")
    @DB
    public UserAccount createAccount(CreateAccountCmd cmd) {
        Long accountId = null;
        String username = cmd.getUsername();
        String password = cmd.getPassword();
        String firstName = cmd.getFirstname();
        String lastName = cmd.getLastname();
        Long domainId = cmd.getDomainId();
        String email = cmd.getEmail();
        String timezone = cmd.getTimezone();
        String accountName = cmd.getAccountName();
        short userType = cmd.getAccountType().shortValue();

        String networkDomain = cmd.getNetworkDomain();
        
        if (accountName == null) {
            accountName = username;
        }
        if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN;
        }
        
        DomainVO domain = _domainDao.findById(domainId);
        
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        } 
        
        checkAccess(UserContext.current().getCaller(), domain);

        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account != null) {
            throw new InvalidParameterValueException("The specified account: " + account.getAccountName() + " already exists");
        }

        if (domain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The account cannot be created as domain " + domain.getName() + " is being deleted");
        }

        if (!_userAccountDao.validateUsernameInDomain(username, domainId)) {
            throw new InvalidParameterValueException("The user " + username + " already exists in domain " + domainId);
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

        //Create account itself
        if (accountId == null) {
            if ((userType < Account.ACCOUNT_TYPE_NORMAL) || (userType > Account.ACCOUNT_TYPE_READ_ONLY_ADMIN)) {
                throw new InvalidParameterValueException("Invalid account type " + userType + " given; unable to create user");
            }

            // create a new account for the user
            AccountVO newAccount = new AccountVO();
            if (domainId == null) {
                // root domain is default
                domainId = DomainVO.ROOT_DOMAIN;
            }

            if ((domainId != DomainVO.ROOT_DOMAIN) && (userType == Account.ACCOUNT_TYPE_ADMIN)) {
                throw new InvalidParameterValueException("Invalid account type " + userType + " given for an account in domain " + domainId + "; unable to create user.");
            }

            newAccount.setAccountName(accountName);
            newAccount.setDomainId(domainId);
            newAccount.setType(userType);
            newAccount.setState(State.enabled);
            newAccount.setNetworkDomain(networkDomain);
            newAccount = _accountDao.persist(newAccount);
            accountId = newAccount.getId();
        }

        if (userType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            List<DataCenterVO> dc = _dcDao.findZonesByDomainId(domainId);
            if (dc == null || dc.size() == 0) {
                throw new CloudRuntimeException("The account cannot be created as domain " + domain.getName() + " is not associated with any private Zone");
            }
        }
        if (accountId == null) {
            throw new CloudRuntimeException("Failed to create account for user: " + username + "; unable to create user");
        }

        UserVO user = new UserVO();
        user.setUsername(username);
        user.setPassword(password);
        user.setState(State.enabled);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setAccountId(accountId.longValue());
        user.setEmail(email);
        user.setTimezone(timezone);
        
        if(userType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN){
            //set registration token
            byte[] bytes = (domainId + accountName + username + System.currentTimeMillis()).getBytes();
            String registrationToken = UUID.nameUUIDFromBytes(bytes).toString();
            user.setRegistrationToken(registrationToken);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating user: " + username + ", account: " + accountName + " (id:" + accountId + "), domain: " + domainId + " timezone:" + timezone);
        }
        
        //Create resource count records for the account
        _resourceCountDao.createResourceCounts(accountId, ResourceLimit.OwnerType.Account);
        
        //Create a user
        UserVO dbUser = _userDao.persist(user);
        
        //Create default security group
        _networkGroupMgr.createDefaultSecurityGroup(accountId);
        
        txn.commit();

        if (!user.getPassword().equals(dbUser.getPassword())) {
            throw new CloudRuntimeException("The user " + username + " being creating is using a password that is different than what's in the db");
        }
        return _userAccountDao.findById(dbUser.getId());
       
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")
    public UserVO createUser(CreateUserCmd cmd) {
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        String userName = cmd.getUsername();
        String password = cmd.getPassword();
        String firstName = cmd.getFirstname();
        String lastName = cmd.getLastname();
        String email = cmd.getEmail();
        String timeZone = cmd.getTimezone();
        Long accountId = null;

        // default domain to ROOT if not specified
        if (domainId == null) {
            domainId = Domain.ROOT_DOMAIN;
        }
        DomainVO domain = _domainDao.findById(domainId);
        checkAccess(UserContext.current().getCaller(), domain);
        Account account = _accountDao.findActiveAccount(accountName, domainId);

        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain id=" + domainId + " to create user");
        } else {
            accountId = account.getAccountId();
        }

        if (domain == null) {
            throw new CloudRuntimeException("The domain " + domainId + " does not exist; unable to create user");
        } else {
            if (domain.getState().equals(Domain.State.Inactive)) {
                throw new CloudRuntimeException("The user cannot be created as domain " + domain.getName() + " is being deleted");
            }
        }

        if (!_userAccountDao.validateUsernameInDomain(userName, domainId)) {
            throw new CloudRuntimeException("The user " + userName + " already exists in domain " + domainId);
        }

        UserVO user = new UserVO();
        user.setUsername(userName);
        user.setPassword(password);
        user.setState(State.enabled);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setAccountId(accountId.longValue());
        user.setEmail(email);
        user.setTimezone(timeZone);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating user: " + userName + ", account: " + accountName + " (id:" + accountId + "), domain: " + domainId + " timezone:" + timeZone);
        }

        UserVO dbUser = _userDao.persist(user);

        if (!user.getPassword().equals(dbUser.getPassword())) {
            throw new CloudRuntimeException("The user " + userName + " being creating is using a password that is different than what's in the db");
        }

        return dbUser;
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
    public UserAccount disableUser(DisableUserCmd cmd) {
        Long userId = cmd.getId();
        Account adminAccount = UserContext.current().getCaller();

        // Check if user exists in the system
        User user = _userDao.findById(userId);
        if ((user == null) || (user.getRemoved() != null)) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        // If the user is a System user, return an error
        Account account = _accountDao.findById(user.getAccountId());
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new InvalidParameterValueException("User id : " + userId + " is a system user, disabling is not allowed");
        }

        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
            throw new PermissionDeniedException("Failed to disable user " + userId + ", permission denied.");
        }

        boolean success = doSetUserStatus(userId, State.disabled);
        if (success) {
            // user successfully disabled
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to disable user " + userId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_ENABLE, eventDescription = "enabling User")
    public UserAccount enableUser(EnableUserCmd cmd) {
        Long userId = cmd.getId();
        Account adminAccount = UserContext.current().getCaller();
        boolean success = false;

        // Check if user exists in the system
        User user = _userDao.findById(userId);
        if ((user == null) || (user.getRemoved() != null)) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        // If the user is a System user, return an error
        Account account = _accountDao.findById(user.getAccountId());
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new InvalidParameterValueException("User id : " + userId + " is a system user, enabling is not allowed");
        }

        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
            throw new PermissionDeniedException("Failed to enable user " + userId + ", permission denied.");
        }

        success = doSetUserStatus(userId, State.enabled);

        // make sure the account is enabled too
        success = (success && enableAccount(user.getAccountId()));

        if (success) {
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to enable user " + userId);
        }
    }

    @Override
    public UserAccount lockUser(LockUserCmd cmd) {
        boolean success = false;

        Account adminAccount = UserContext.current().getCaller();
        Long id = cmd.getId();

        // Check if user with id exists in the system
        User user = _userDao.findById(id);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        } else if (user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }

        // If the user is a System user, return an error. We do not allow this
        Account account = _accountDao.findById(user.getAccountId());
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new PermissionDeniedException("user id : " + id + " is a system user, locking is not allowed");
        }

        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
            throw new PermissionDeniedException("Failed to lock user " + id + ", permission denied.");
        }

        // make sure the account is enabled too
        // if the user is either locked already or disabled already, don't change state...only lock currently enabled users
        if (user.getState().equals(State.locked)) {
            // already locked...no-op
            return _userAccountDao.findById(id);
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
                success = (success && lockAccountInternal(user.getAccountId()));
            }
        } else {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Attempting to lock a non-enabled user, current state is " + user.getState() + " (userId: " + user.getId() + "), locking failed.");
            }
        }

        if (success) {
            return _userAccountDao.findById(id);
        } else {
            throw new CloudRuntimeException("Unable to lock user " + id);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DELETE, eventDescription = "deleting account", async = true)
    // This method deletes the account
    public boolean deleteUserAccount(DeleteAccountCmd cmd) {

        UserContext ctx = UserContext.current();
        long callerUserId = ctx.getCallerUserId();
        Account caller = ctx.getCaller();

        Long accountId = cmd.getId();

        // If the user is a System user, return an error. We do not allow this
        AccountVO account = _accountDao.findById(accountId);
        checkAccess(UserContext.current().getCaller(), null, account);
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new PermissionDeniedException("Account id : " + accountId + " is a system account, delete is not allowed");
        }

        if (account == null) {
            throw new InvalidParameterValueException("The specified account does not exist in the system");
        }

        if (account.getRemoved() != null) {
            s_logger.info("The account:" + account.getAccountName() + " is already removed");
            return true;
        }

        if(!deleteAccount(account, callerUserId, caller)){
            throw new CloudRuntimeException("Unable to delete account " + account.getAccountName() + " in domain " + account.getDomainId());
        }
        return true;
    }

    @Override
    public AccountVO enableAccount(EnableAccountCmd cmd) {
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        boolean success = false;
        Account account = _accountDao.findActiveAccount(accountName, domainId);

        // Check if account exists
        if (account == null) {
            s_logger.error("Unable to find account " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }

        // Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        // Check if user performing the action is allowed to modify this account
        Account adminAccount = UserContext.current().getCaller();
        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
            throw new PermissionDeniedException("Invalid account " + accountName + " in domain " + domainId + " given, permission denied");
        }

        success = enableAccount(account.getId());
        if (success) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to enable account " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "locking account", async = true)
    public AccountVO lockAccount(DisableAccountCmd cmd) {
        Account adminAccount = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();

        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), domainId)) {
            throw new PermissionDeniedException("Failed to lock account " + accountName + " in domain " + domainId + ", permission denied.");
        }

        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find active account with name " + accountName + " in domain " + domainId);
        }

        // don't allow modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("can not lock system account");
        }

        if (lockAccountInternal(account.getId())) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to lock account " + accountName + " in domain " + domainId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "disabling account", async = true)
    public AccountVO disableAccount(DisableAccountCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();

        Account adminAccount = UserContext.current().getCaller();
        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), domainId)) {
            throw new PermissionDeniedException("Failed to disable account " + accountName + " in domain " + domainId + ", permission denied.");
        }

        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }
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
        if (account == null) {
            s_logger.error("Unable to find account " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }

        // Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        // Check if user performing the action is allowed to modify this account
        Account adminAccount = UserContext.current().getCaller();
        if ((adminAccount != null) && (adminAccount.getType() != Account.ACCOUNT_TYPE_ADMIN) && _domainDao.isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
            throw new PermissionDeniedException("Invalid account " + accountName + " in domain " + domainId + " given, permission denied");
        }

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

        if ((user != null) && (user.getAccountId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new InvalidParameterValueException("Account id : " + user.getAccountId() + " is a system account, delete for user associated with this account is not allowed");
        }
        checkAccess(UserContext.current().getCaller(), null, _accountDao.findById(user.getAccountId()));
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
                    List<DomainVO> inactiveDomains = _domainDao.findInactiveDomains();
                    s_logger.info("Found " + inactiveDomains.size() + " inactive domains to cleanup");
                    for (DomainVO inactiveDomain : inactiveDomains) {
                        long domainId = inactiveDomain.getId();
                        try {
                            List<AccountVO> accountsForCleanupInDomain = _accountDao.findCleanupsForRemovedAccounts(domainId);
                            if (accountsForCleanupInDomain.isEmpty()) {
                                s_logger.debug("Removing inactive domain id=" + domainId);
                                _domainDao.remove(domainId);
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
            DomainVO domain = _domainDao.findById(domainId);
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
    public Account getActiveAccount(String accountName, Long domainId) {
        if (accountName == null || domainId == null) {
            throw new InvalidParameterValueException("Both accountName and domainId are required for finding active account in the system");
        } else {
            return _accountDao.findActiveAccount(accountName, domainId);
        }
    }

    @Override
    public Account getActiveAccount(Long accountId) {
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
    public Domain getDomain(long domainId) {
        return _domainDao.findById(domainId);
    }

    @Override
    public Pair<String, Long> finalizeAccountDomainForList(Account caller, String accountName, Long domainId) {
        if (isAdmin(caller.getType())) {
            if (domainId == null && accountName != null) {
                throw new InvalidParameterValueException("accountName and domainId might be specified together");
            } else if (domainId != null) {
                Domain domain = getDomain(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
                }

                checkAccess(caller, domain);

                if (accountName != null) {
                    Account owner = getActiveAccount(accountName, domainId);
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
	
	@Override
	public Set<Long> getDomainParentIds(long domainId) {
	   return _domainDao.getDomainParentIds(domainId);
	}
	
	@Override
	public Set<Long> getDomainChildrenIds(String parentDomainPath) {
	    Set<Long> childDomains = new HashSet<Long>();
	    SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
	    sc.addAnd("path", SearchCriteria.Op.LIKE, parentDomainPath + "%");
	    
	    List<DomainVO> domains = _domainDao.search(sc, null);
	    
	    for (DomainVO domain : domains) {
	        childDomains.add(domain.getId());
	    }
	    
	    return childDomains;
	}
	
	@DB
	public boolean updateResourceCount(long accountId, ResourceType type, boolean increment, long delta) {
	    boolean result = true;
	    try {
	        Transaction txn = Transaction.currentTxn();
	        txn.start();
	        
	        Set<Long> rowsToLock = _resourceCountDao.listAllRowsToUpdateForAccount(accountId, getAccount(accountId).getDomainId(), type);
	        
	        //Lock rows first
	        SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
	        sc.setParameters("id", rowsToLock.toArray());
	        List<ResourceCountVO> rowsToUpdate = _resourceCountDao.lockRows(sc, null, true);    
	        
	        for (ResourceCountVO rowToUpdate : rowsToUpdate) {
	            if (!_resourceCountDao.updateById(rowToUpdate.getId(), increment, delta)) {
	                s_logger.trace("Unable to update resource count for the row " + rowToUpdate);
	                result = false;
	            }
	        }
	        
	        txn.commit();
	    } catch (Exception ex) {
	        s_logger.error("Failed to update resource count for account id=" + accountId);
	        result = false;
	    }
	    return result;
	}
	
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DOMAIN_CREATE, eventDescription = "creating Domain")
    @DB
    public Domain createDomain(CreateDomainCmd cmd) {
        String name = cmd.getDomainName();
        Long parentId = cmd.getParentDomainId();
        Long ownerId = UserContext.current().getCaller().getId();
        Account caller = UserContext.current().getCaller();
        String networkDomain = cmd.getNetworkDomain();

        if (ownerId == null) {
            ownerId = Long.valueOf(1);
        }

        if (parentId == null) {
            parentId = Long.valueOf(DomainVO.ROOT_DOMAIN);
        }

        DomainVO parentDomain = _domainDao.findById(parentId);
        if (parentDomain == null) {
            throw new InvalidParameterValueException("Unable to create domain " + name + ", parent domain " + parentId + " not found.");
        }
        
        if (parentDomain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The domain cannot be created as the parent domain " + parentDomain.getName() + " is being deleted");
        }
        
        checkAccess(caller, parentDomain);

        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        sc.addAnd("parent", SearchCriteria.Op.EQ, parentId);
        List<DomainVO> domains = _domainDao.search(sc, null);
        if ((domains == null) || domains.isEmpty()) {
            DomainVO domain = new DomainVO(name, ownerId, parentId, networkDomain);
            try {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                
                domain = _domainDao.create(domain);
                _resourceCountDao.createResourceCounts(domain.getId(), ResourceLimit.OwnerType.Domain);
                
                txn.commit();
                return domain;
            } catch (IllegalArgumentException ex) {
                s_logger.warn("Failed to create domain ", ex);
                throw ex;
            }
        } else {
            throw new InvalidParameterValueException("Domain with name " + name + " already exists for the parent id=" + parentId);
        }
    }
}
