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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.acl.ControlledEntity;
import com.cloud.acl.SecurityChecker;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.CreateAccountCmd;
import com.cloud.api.commands.CreateUserCmd;
import com.cloud.api.commands.DeleteAccountCmd;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.DisableAccountCmd;
import com.cloud.api.commands.DisableUserCmd;
import com.cloud.api.commands.EnableAccountCmd;
import com.cloud.api.commands.EnableUserCmd;
import com.cloud.api.commands.ListResourceLimitsCmd;
import com.cloud.api.commands.LockUserCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateResourceLimitCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.server.Criteria;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
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
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.UserVmDao;

@Local(value={AccountManager.class, AccountService.class})
public class AccountManagerImpl implements AccountManager, AccountService, Manager {
	public static final Logger s_logger = Logger.getLogger(AccountManagerImpl.class);
	
	private String _name;
	@Inject private AccountDao _accountDao;
	@Inject private DomainDao _domainDao;
	@Inject private ResourceLimitDao _resourceLimitDao;
	@Inject private ResourceCountDao _resourceCountDao;
	@Inject private UserDao _userDao;
	@Inject private InstanceGroupDao _vmGroupDao;
	@Inject private UserAccountDao _userAccountDao;
	@Inject private VolumeDao _volumeDao;
	@Inject private UserVmDao _userVmDao;
    @Inject private IPAddressDao _publicIpAddressDao;
    @Inject private VlanDao _vlanDao;
    @Inject private DomainRouterDao _routerDao;
    @Inject private VMTemplateDao _templateDao;
    @Inject private PodVlanMapDao _podVlanMapDao;
    @Inject private NetworkDao _networkDao;
	
	
	@Inject private SecurityGroupManager _networkGroupMgr;
	@Inject private NetworkManager _networkMgr;
	@Inject private SnapshotManager _snapMgr;
	@Inject private UserVmManager _vmMgr;
	@Inject private StorageManager _storageMgr;
	@Inject private TemplateManager _tmpltMgr;
	@Inject private ConfigurationManager _configMgr;
	@Inject private VirtualNetworkApplianceManager _routerMgr;
	
    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));
	
	private final GlobalLock m_resourceCountLock = GlobalLock.getInternLock("resource.count");
	
	UserVO _systemUser;
	AccountVO _systemAccount;
	@Inject(adapter=SecurityChecker.class)
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
    public void incrementResourceCount(long accountId, ResourceType type, Long...delta) {
    	long numToIncrement = (delta.length == 0) ? 1 : delta[0].longValue();

    	if (m_resourceCountLock.lock(120)) { // 2 minutes
    	    try {
                _resourceCountDao.updateAccountCount(accountId, type, true, numToIncrement);

                // on a per-domain basis, increment the count
                // FIXME:  can this increment be done on the database side in a custom update statement?
                Account account = _accountDao.findById(accountId);
                Long domainId = account.getDomainId();
                while (domainId != null) {
                    _resourceCountDao.updateDomainCount(domainId, type, true, numToIncrement);
                    DomainVO domain = _domainDao.findById(domainId);
                    domainId = domain.getParent();
                }
    	    } finally {
    	        m_resourceCountLock.unlock();
    	    }
    	}
    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Long...delta) {
    	long numToDecrement = (delta.length == 0) ? 1 : delta[0].longValue();

        if (m_resourceCountLock.lock(120)) { // 2 minutes
            try {
                _resourceCountDao.updateAccountCount(accountId, type, false, numToDecrement);

                // on a per-domain basis, decrement the count
                // FIXME:  can this decrement be done on the database side in a custom update statement?
                Account account = _accountDao.findByIdIncludingRemoved(accountId);  // find all accounts, even removed accounts if this happens to be for an account that's being deleted
                Long domainId = account.getDomainId();
                while (domainId != null) {
                    _resourceCountDao.updateDomainCount(domainId, type, false, numToDecrement);
                    DomainVO domain = _domainDao.findById(domainId);
                    domainId = domain.getParent();
                }
            } finally {
                m_resourceCountLock.unlock();
            }
        }
    }

    @Override
    public long findCorrectResourceLimit(AccountVO account, ResourceType type) {
    	long max = -1;
    	
    	// Check account
		ResourceLimitVO limit = _resourceLimitDao.findByAccountIdAndType(account.getId(), type);
		
		if (limit != null) {
			max = limit.getMax().longValue();
		} else {
			// If the account has an infinite limit, check the ROOT domain
			Long domainId = account.getDomainId();
			while ((domainId != null) && (limit == null)) {
				limit = _resourceLimitDao.findByDomainIdAndType(domainId, type);
				DomainVO domain = _domainDao.findById(domainId);
				domainId = domain.getParent();
			}
	
			if (limit != null) {
				max = limit.getMax().longValue();
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

    @Override
    public boolean resourceLimitExceeded(Account account, ResourceType type, long...count) {
    	long numResources = ((count.length == 0) ? 1 : count[0]);

    	// Don't place any limits on system or admin accounts
    	long accountType = account.getType();
		if (accountType == Account.ACCOUNT_TYPE_ADMIN || accountType == Account.ACCOUNT_ID_SYSTEM) {
			return false;
		}

        if (m_resourceCountLock.lock(120)) { // 2 minutes
            try {
                // Check account
                ResourceLimitVO limit = _resourceLimitDao.findByAccountIdAndType(account.getId(), type);
                
                if (limit != null) {
                    long potentialCount = _resourceCountDao.getAccountCount(account.getId(), type) + numResources;
                    if (potentialCount > limit.getMax().longValue()) {
                        return true;
                    }
                }

                // check all domains in the account's domain hierarchy
                Long domainId = account.getDomainId();
                while (domainId != null) {
                    ResourceLimitVO domainLimit = _resourceLimitDao.findByDomainIdAndType(domainId, type);
                    if (domainLimit != null) {
                        long domainCount = _resourceCountDao.getDomainCount(domainId, type);
                        if ((domainCount + numResources) > domainLimit.getMax().longValue()) {
                            return true;
                        }
                    }
                    DomainVO domain = _domainDao.findById(domainId);
                    domainId = domain.getParent();
                }
            } finally {
                m_resourceCountLock.unlock();
            }
        }

		return false;
    }

    @Override
    public long getResourceCount(AccountVO account, ResourceType type) {
    	return _resourceCountDao.getAccountCount(account.getId(), type);
    }
    
    @Override
    public List<ResourceLimitVO> searchForLimits(Criteria c) {
        Long id = (Long) c.getCriteria(Criteria.ID);
        Long domainId = (Long) c.getCriteria(Criteria.DOMAINID);
        Long accountId = (Long) c.getCriteria(Criteria.ACCOUNTID);
        ResourceType type = (ResourceType) c.getCriteria(Criteria.TYPE);
        
        // For 2.0, we are just limiting the scope to having an user retrieve
        // limits for himself and if limits don't exist, use the ROOT domain's limits.
        // - Will
        List<ResourceLimitVO> limits = new ArrayList<ResourceLimitVO>();

        if ((accountId != null) && (domainId != null)) {
	        //if domainId==ROOT_DOMAIN and account belongs to admin
	        //return all records for resource limits (bug 3778)
	        
	        if (domainId == DomainVO.ROOT_DOMAIN) {
	        	AccountVO account = _accountDao.findById(accountId);
	        	
	        	if ((account != null) && (account.getType() == 1)) {
	        		// account belongs to admin return all limits
	        		limits = _resourceLimitDao.listAll();
	        		return limits;
	        	}
	        }
        }

        if (accountId != null) {
        	SearchBuilder<ResourceLimitVO> sb = _resourceLimitDao.createSearchBuilder();
        	sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        	sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        	SearchCriteria<ResourceLimitVO> sc = sb.create();

        	if (accountId != null) {
        		sc.setParameters("accountId", accountId);
        	}

        	if (type != null) {
        		sc.setParameters("type", type);
        	}

        	if (id != null) {
        	    sc.setParameters("id", id);
        	}

        	// Listing all limits for an account
        	if (type == null) {
        		//List<ResourceLimitVO> userLimits = _resourceLimitDao.search(sc, searchFilter);
        		List<ResourceLimitVO> userLimits = _resourceLimitDao.listByAccountId(accountId);
	        	List<ResourceLimitVO> rootLimits = _resourceLimitDao.listByDomainId(DomainVO.ROOT_DOMAIN);
	        	ResourceType resourceTypes[] = ResourceType.values();
        	
	        	for (ResourceType resourceType: resourceTypes) {
	        		boolean found = false;
	        		for (ResourceLimitVO userLimit : userLimits) {
	        			if (userLimit.getType() == resourceType) {
	        				limits.add(userLimit);
	        				found = true;
	        				break;
	        			}
	        		}
	        		if (!found) {
	        			// Check the ROOT domain
	        			for (ResourceLimitVO rootLimit : rootLimits) {
	        				if (rootLimit.getType() == resourceType) {
	        					limits.add(rootLimit);
	        					found = true;
	        					break;
	        				}
	        			}
	        		}
	        		if (!found) {
	        			limits.add(new ResourceLimitVO(domainId, accountId, resourceType, -1L));
	        		}
	        	}
        	} else {
        		AccountVO account = _accountDao.findById(accountId);
        		limits.add(new ResourceLimitVO(null, accountId, type, findCorrectResourceLimit(account, type)));
        	}
        } else if (domainId != null) {
        	if (type == null) {
        		ResourceType resourceTypes[] = ResourceType.values();
        		List<ResourceLimitVO> domainLimits = _resourceLimitDao.listByDomainId(domainId);
        		for (ResourceType resourceType: resourceTypes) {
	        		boolean found = false;
	        		for (ResourceLimitVO domainLimit : domainLimits) {
	        			if (domainLimit.getType() == resourceType) {
	        				limits.add(domainLimit);
	        				found = true;
	        				break;
	        			}
	        		}
	        		if (!found) {
	        			limits.add(new ResourceLimitVO(domainId, null, resourceType, -1L));
	        		}
        		}
        	} else {
        		limits.add(_resourceLimitDao.findByDomainIdAndType(domainId, type));
        	}
        }
        return limits;
    }

    @Override
    public List<ResourceLimitVO> searchForLimits(ListResourceLimitsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        Account account = UserContext.current().getCaller();

        if ((account == null) ||
            (account.getType() == Account.ACCOUNT_TYPE_ADMIN) ||
            (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
            (account.getType() == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN)) {

            if (accountName != null) {
                // Look up limits for the specified account

                if (domainId == null) {
                    throw new InvalidParameterValueException("Failed to list limits for account " + accountName + " no domain id specified.");
                }

                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                
                if (userAccount == null) {
                    throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                } else if (account != null && (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN)) {
                    // If this is a non-root admin, make sure that the admin and the user account belong in the same domain or
                    // that the user account's domain is a child domain of the parent
                    if (account.getDomainId() != userAccount.getDomainId() && !_domainDao.isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                        throw new PermissionDeniedException("You do not have permission to access limits for this account: " + accountName);
                    }
                }
                
                accountId = userAccount.getId();
                domainId = null;
            } else if (domainId != null) {
                // Look up limits for the specified domain
                accountId = null;
            } else if (account == null) {
                // Look up limits for the ROOT domain
                domainId = DomainVO.ROOT_DOMAIN;
            } else {
                // Look up limits for the admin's account
                accountId = account.getId();
                domainId = null;
            }
        } else {
            // Look up limits for the user's account
            accountId = account.getId();
            domainId = null;
        }       

        // Map resource type
        ResourceType resourceType = null;
        Integer type = cmd.getResourceType();
        try {
            if (type != null) {
                resourceType = ResourceType.values()[type];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidParameterValueException("Invalid resource type " + type + " given.  Please specify a valid resource type.");
        }

        Criteria c = new Criteria("id", Boolean.FALSE, cmd.getStartIndex(), cmd.getPageSizeVal());
        c.addCriteria(Criteria.ID, cmd.getId());
        c.addCriteria(Criteria.DOMAINID, domainId);
        c.addCriteria(Criteria.ACCOUNTID, accountId);
        c.addCriteria(Criteria.TYPE, resourceType);
        return searchForLimits(c);
    }

    @Override
    public ResourceLimitVO updateResourceLimit(UpdateResourceLimitCmd cmd)  {
        Account account = UserContext.current().getCaller();
    	String accountName = cmd.getAccountName();
    	Long domainId = cmd.getDomainId();
    	Long max = cmd.getMax();
    	Integer type = cmd.getResourceType();
    	
    	//Validate input
        Long accountId = null;

        if (max == null) {
        	max = new Long(-1);
        } else if (max < -1) {
        	throw new InvalidParameterValueException("Please specify either '-1' for an infinite limit, or a limit that is at least '0'.");
        }

        // Map resource type
        ResourceType resourceType;
        try {
        	resourceType = ResourceType.values()[type];
        } catch (ArrayIndexOutOfBoundsException e) {
        	throw new InvalidParameterValueException("Please specify a valid resource type.");
        }               
        
        
        // Either a domainId or an accountId must be passed in, but not both.
        if ((domainId == null) && (accountName == null)) {
            throw new InvalidParameterValueException("Either a domainId or domainId/account must be passed in.");
        }

        if (account != null) {
            if (domainId != null) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to update resource limit for " + ((account.getAccountName() == null) ? "" : "account " + account.getAccountName() + " in ") + "domain " + domainId + ", permission denied");
                }
            } else if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                domainId = DomainVO.ROOT_DOMAIN; // for root admin, default to root domain if domain is not specified
            }                 
            
            if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                if ((domainId != null) && (accountName == null) && domainId.equals(account.getDomainId())) {
                    // if the admin is trying to update their own domain, disallow...
                    throw new PermissionDeniedException("Unable to update resource limit for domain " + domainId + ", permission denied");
                }

            	// If there is an existing ROOT domain limit, make sure its max isn't being exceeded
            	Criteria c = new Criteria();
             	c.addCriteria(Criteria.DOMAINID, DomainVO.ROOT_DOMAIN);
             	c.addCriteria(Criteria.TYPE, resourceType);
            	List<ResourceLimitVO> currentRootDomainLimits = searchForLimits(c);
            	ResourceLimitVO currentRootDomainLimit = (currentRootDomainLimits.size() == 0) ? null : currentRootDomainLimits.get(0);
            	if (currentRootDomainLimit != null) {
            		long currentRootDomainMax = currentRootDomainLimits.get(0).getMax();
            		if ((max == -1 && currentRootDomainMax != -1) || max > currentRootDomainMax) {
            			throw new InvalidParameterValueException("The current ROOT domain limit for resource type " + resourceType + " is " + currentRootDomainMax + " and cannot be exceeded.");
            		}
            	}
            }
        } 


       if (accountName != null) {
           if (domainId == null) {
               throw new InvalidParameterValueException("domainId parameter is required if account is specified");
           }
            Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
            if (userAccount == null) {
                throw new InvalidParameterValueException("unable to find account by name " + account.getAccountName() + " in domain with id " + domainId);
            }
            accountId = userAccount.getId();
        }               

        if (accountId != null) {
            domainId = null;
        }
        
    	

        // Check if the domain or account exists and is valid
        if (accountId != null) {
        	AccountVO accountHandle = _accountDao.findById(accountId);
            if (accountHandle == null) {
                throw new InvalidParameterValueException("Please specify a valid account ID.");
            } else if (accountHandle.getRemoved() != null) {
            	throw new InvalidParameterValueException("Please specify an active account.");
            } else if (accountHandle.getType() == Account.ACCOUNT_TYPE_ADMIN || accountHandle.getType() == Account.ACCOUNT_ID_SYSTEM) {
            	throw new InvalidParameterValueException("Please specify a non-admin account.");
            }

            DomainVO domain = _domainDao.findById(accountHandle.getDomainId());
            long parentMaximum = findCorrectResourceLimit(domain, resourceType);
            if ((parentMaximum >= 0) && ((max.longValue() == -1) || (max.longValue() > parentMaximum))) {
                throw new InvalidParameterValueException("Account " + account.getAccountName() + "(id: " + accountId + ") has maximum allowed resource limit " + parentMaximum +
                        " for " + type + ", please specify a value less that or equal to " + parentMaximum);
            }
        } else if (domainId != null) {
        	DomainVO domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Please specify a valid domain ID.");
            } else if (domain.getRemoved() != null) {
            	throw new InvalidParameterValueException("Please specify an active domain.");
            }

            Long parentDomainId = domain.getParent();
            if (parentDomainId != null) {
                DomainVO parentDomain = _domainDao.findById(parentDomainId);
                long parentMaximum = findCorrectResourceLimit(parentDomain, resourceType);
                if ((parentMaximum >= 0) && (max.longValue() > parentMaximum)) {
                    throw new InvalidParameterValueException("Domain " + domain.getName() + "(id: " + domainId + ") has maximum allowed resource limit " + parentMaximum +
                            " for " + type + ", please specify a value less that or equal to " + parentMaximum);
                }
            }
        }

        // A valid limit type must be passed in
        if (resourceType == null) {
            throw new InvalidParameterValueException("A valid limit type must be passed in.");
        }

        // Check if a limit with the specified domainId/accountId/type combo already exists
        Filter searchFilter = new Filter(ResourceLimitVO.class, null, false, null, null);
        SearchCriteria<ResourceLimitVO> sc = _resourceLimitDao.createSearchCriteria();

        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }

        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }

        if (resourceType != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, resourceType);
        }

        List<ResourceLimitVO> limits = _resourceLimitDao.search(sc, searchFilter);
        if (limits.size() == 1) {
        	// Update the existing limit
            ResourceLimitVO limit = limits.get(0);
            _resourceLimitDao.update(limit.getId(), max);
            return _resourceLimitDao.findById(limit.getId());
        } else {
        	// Persist the new Limit
            return _resourceLimitDao.persist(new ResourceLimitVO(domainId, accountId, resourceType, max));
        }
    }

    @Override
    public AccountVO getSystemAccount() {
        if (_systemAccount == null) {
            _systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
        }
        return _systemAccount;
    }
    
    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
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
    public void checkAccess(Account caller, ControlledEntity... entities) {
        HashMap<Long, List<ControlledEntity>> domains = new HashMap<Long, List<ControlledEntity>>();
        
        for (ControlledEntity entity : entities) {
            if (entity.getAccountId() != -1 && entity.getDomainId() != -1) {
                List<ControlledEntity> toBeChecked = domains.get(entity.getDomainId());
                if (toBeChecked == null) {
                    toBeChecked = new ArrayList<ControlledEntity>();
                    domains.put(entity.getDomainId(), toBeChecked);
                }
                toBeChecked.add(entity);
            }
            boolean granted = false;
            for (SecurityChecker checker : _securityCheckers) {
                if (checker.checkAccess(caller, entity)) {
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
    
    private boolean doSetUserStatus(long userId, State state) {
        UserVO userForUpdate = _userDao.createForUpdate();
        userForUpdate.setState(state);
        return _userDao.update(Long.valueOf(userId), userForUpdate);
    }
    
    public boolean enableAccount(long accountId) {
        boolean success = false;
        AccountVO acctForUpdate = _accountDao.createForUpdate();
        acctForUpdate.setState(State.enabled);
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
        
        try {        	
            if (!_accountDao.remove(accountId)) {
                s_logger.error("Unable to delete account " + accountId);
                return false;
            }

            List<UserVO> users = _userDao.listByAccount(accountId);

            for(UserVO user : users){
            	_userDao.remove(user.getId());
            }
            
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Remove account " + accountId);
            }

            cleanupAccount(account, callerUserId, caller);
            return true;
        } catch (Exception e) {
            s_logger.error("exception deleting account: " + accountId, e);            
            return false;
        }
    }
    
    @Override
    public boolean cleanupAccount(AccountVO account, long callerUserId, Account caller) {
        long accountId = account.getId();
        boolean accountCleanupNeeded = false;
        
        try {
            //delete all vm groups belonging to accont
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
            // else, there are no snapshots, hence no directory to delete.
            
            // Destroy the account's VMs
            List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Destroying # of vms (accountId=" + accountId + "): " + vms.size());
            }

            for (UserVmVO vm : vms) {
                long startEventId = EventUtils.saveStartedEvent(callerUserId, vm.getAccountId(), EventTypes.EVENT_VM_DESTROY, "Destroyed VM instance : " + vm.getName());
                if (!_vmMgr.expunge(vm, callerUserId, caller)) {
                    s_logger.error("Unable to destroy vm: " + vm.getId());
                    accountCleanupNeeded = true;
                    EventUtils.saveEvent(callerUserId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_DESTROY, "Unable to destroy vm: " + vm.getId(), startEventId);
                } else {
                    EventUtils.saveEvent(callerUserId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_DESTROY, "Successfully destroyed VM instance : " + vm.getName(), startEventId);
                }
            }
            
            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volumeDao.findDetachedByAccount(accountId);
            for (VolumeVO volume : volumes) {
//                if(volume.getPoolId()==null){
//                    accountCleanupNeeded = true;
//                }
                _storageMgr.destroyVolume(volume);
            }

            // Destroy the account's routers
            List<DomainRouterVO> routers = _routerDao.listBy(accountId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Destroying # of routers (accountId=" + accountId + "): " + routers.size());
            }

            boolean routersCleanedUp = true;
            for (DomainRouterVO router : routers) {
                long startEventId = EventUtils.saveStartedEvent(callerUserId, router.getAccountId(), EventTypes.EVENT_ROUTER_DESTROY, "Starting to destroy router : " + router.getName());
                if (!_routerMgr.destroyRouter(router.getId())) {
                    s_logger.error("Unable to destroy router: " + router.getId());
                    routersCleanedUp = false;
                    EventUtils.saveEvent(callerUserId, router.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_ROUTER_DESTROY, "Unable to destroy router: " + router.getName(), startEventId);
                } else {
                    EventUtils.saveEvent(callerUserId, router.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ROUTER_DESTROY, "successfully destroyed router : " + router.getName(), startEventId);
                }
            }

            if (routersCleanedUp) {
                List<IPAddressVO> ips = _publicIpAddressDao.listByAccount(accountId);
                
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Found " + ips.size() + " public IP addresses for account with ID " + accountId);
                }
                
                for (IPAddressVO ip : ips) {
                    List<PodVlanMapVO> podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(ip.getVlanId());
                    if (podVlanMaps != null && podVlanMaps.size() != 0) {
                        Long podId = podVlanMaps.get(0).getPodId();
                        if (podId != null) {
                            continue;//bug 5561 do not release direct attach pod ips until vm is destroyed
                        }
                    }
                    
                    if (!_networkMgr.releasePublicIpAddress(ip.getAddress(), account.getId(), User.UID_SYSTEM)) {
                        s_logger.error("Unable to release IP: " + ip.getAddress());
                        accountCleanupNeeded = true;
                    } else {
                        decrementResourceCount(accountId, ResourceType.public_ip);
                    }
                }

            } else {
                accountCleanupNeeded = true;
            }
            
            
            // Delete the account's VLANs
            List<VlanVO> accountVlans = _vlanDao.listVlansForAccountByType(null, accountId, VlanType.DirectAttached);
            boolean allVlansDeleted = true;
            for (VlanVO vlan : accountVlans) {
                try {
                    allVlansDeleted = _configMgr.deleteVlanAndPublicIpRange(User.UID_SYSTEM, vlan.getId());
                } catch (InvalidParameterValueException e) {
                    allVlansDeleted = false;
                }
            }
            
            //delete networks
            s_logger.debug("Deleting networks for account " + account.getId());
            List<NetworkVO> networks = _networkDao.listByOwner(accountId);
            for (NetworkVO network : networks) {
                _networkMgr.deleteNetwork(network.getId());
                s_logger.debug("Network " + network.getId() + " successfully deleted.");
            }

            if (!allVlansDeleted) {
                accountCleanupNeeded = true;
            }
            
            // clean up templates
            List<VMTemplateVO> userTemplates = _templateDao.listByAccountId(accountId);
            boolean allTemplatesDeleted = true;
            for (VMTemplateVO template : userTemplates) {
                try {
                    allTemplatesDeleted = _tmpltMgr.delete(callerUserId, template.getId(), null);
                } catch (Exception e) {
                    s_logger.warn("Failed to delete template while removing account: " + template.getName() + " due to: " + e.getMessage());
                    allTemplatesDeleted = false;
                }
            }
            
            if (!allTemplatesDeleted) {
                accountCleanupNeeded = true;
            }

            return true;
        } finally {
            s_logger.info("Cleanup for account " + account.getId() + (accountCleanupNeeded ? " is needed." : " is not needed."));
            
            if (accountCleanupNeeded) {
                _accountDao.markForCleanup(accountId);
            }
        }
    }
    
    @Override
    public boolean disableAccount(long accountId) {
        boolean success = false;
        if (accountId <= 2) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("disableAccount -- invalid account id: " + accountId);
            }
            return false;
        }

        AccountVO account = _accountDao.findById(accountId);
        if ((account == null) || account.getState().equals(State.disabled)) {
            success = true;
        } else {
            AccountVO acctForUpdate = _accountDao.createForUpdate();
            acctForUpdate.setState(State.disabled);
            success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);

            success = (success && doDisableAccount(accountId));
        }
        return success;
    }
    
    private boolean doDisableAccount(long accountId) {
        List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
        boolean success = true;
        for (UserVmVO vm : vms) {
            try {
                success = (success && _vmMgr.stop(vm));
            } catch (AgentUnavailableException aue) {
                s_logger.warn("Agent running on host " + vm.getHostId() + " is unavailable, unable to stop vm " + vm.getName());
                success = false;
            }
        }

        List<DomainRouterVO> routers = _routerDao.listBy(accountId);
        for (DomainRouterVO router : routers) {
            success = (success && _routerMgr.stopRouter(router.getId()));
        }

        return success;
    }
    
    /////////////////////////////////////////////////////
    //////////////// API commands /////////////////////
    /////////////////////////////////////////////////////

    
    @Override
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
        String networkDomain = cmd.getNetworkdomain();
        Long userId = UserContext.current().getCallerUserId();
        
        try {
            if (accountName == null) {
                accountName = username;
            }
            if (domainId == null) {
                domainId = DomainVO.ROOT_DOMAIN;
            }

            Account account = _accountDao.findActiveAccount(accountName, domainId);
            if (account != null) {
                throw new CloudRuntimeException("The specified account: "+account.getAccountName()+" already exists");
            }

            if (!_userAccountDao.validateUsernameInDomain(username, domainId)) {
                throw new CloudRuntimeException("The user " + username + " already exists in domain " + domainId);
            }

            if (accountId == null) {
                if ((userType < Account.ACCOUNT_TYPE_NORMAL) || (userType > Account.ACCOUNT_TYPE_READ_ONLY_ADMIN)) {
                    throw new CloudRuntimeException("Invalid account type " + userType + " given; unable to create user");
                }

                // create a new account for the user
                AccountVO newAccount = new AccountVO();
                if (domainId == null) {
                    // root domain is default
                    domainId = DomainVO.ROOT_DOMAIN;
                }

                if ((domainId != DomainVO.ROOT_DOMAIN) && (userType == Account.ACCOUNT_TYPE_ADMIN)) {
                    throw new CloudRuntimeException("Invalid account type " + userType + " given for an account in domain " + domainId + "; unable to create user.");
                }

                newAccount.setAccountName(accountName);
                newAccount.setDomainId(domainId);
                newAccount.setType(userType);
                newAccount.setState(State.enabled);
                newAccount.setNetworkDomain(networkDomain);
                newAccount = _accountDao.persist(newAccount);
                accountId = newAccount.getId();
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
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating user: " + username + ", account: " + accountName + " (id:" + accountId + "), domain: " + domainId + " timezone:"+ timezone);
            }

            UserVO dbUser = _userDao.persist(user);
            
            _networkGroupMgr.createDefaultSecurityGroup(accountId);

            if (!user.getPassword().equals(dbUser.getPassword())) {
                throw new CloudRuntimeException("The user " + username + " being creating is using a password that is different than what's in the db");
            }

            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, EventTypes.EVENT_USER_CREATE, "User, " + username + " for accountId = " + accountId
                    + " and domainId = " + domainId + " was created.");
            return _userAccountDao.findById(dbUser.getId());
        } catch (Exception e) {
            EventUtils.saveEvent(new Long(1), new Long(1), EventVO.LEVEL_ERROR, EventTypes.EVENT_USER_CREATE, "Error creating user, " + username + " for accountId = " + accountId
                    + " and domainId = " + domainId);
            if (e instanceof CloudRuntimeException) {
                s_logger.info("unable to create user: " + e);
            } else {
                s_logger.warn("unknown exception creating user", e);
            }
            throw new CloudRuntimeException(e.getMessage());
        }
    }
    
    @Override
    public UserVO createUser(CreateUserCmd cmd){
    	String accountName = cmd.getAccountName();
    	Long domainId = cmd.getDomainId();
    	String userName = cmd.getUsername();
    	String password = cmd.getPassword();
    	String firstName = cmd.getFirstname();
    	String lastName = cmd.getLastname();
    	String email = cmd.getEmail();
    	String timeZone = cmd.getTimezone();
    	Long accountId = null;
    	Long userId = UserContext.current().getCallerUserId();
    	
    	Account account = _accountDao.findActiveAccount(accountName, domainId);
    	
    	if( account == null){
    		throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account to create user");
    	}else{
    		accountId = account.getAccountId();
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
            s_logger.debug("Creating user: " + userName + ", account: " + accountName + " (id:" + accountId + "), domain: " + domainId + " timezone:"+ timeZone);
        }

        UserVO dbUser = _userDao.persist(user);

        if (!user.getPassword().equals(dbUser.getPassword())) {
            throw new CloudRuntimeException("The user " + userName + " being creating is using a password that is different than what's in the db");
        }
        
        EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, EventTypes.EVENT_USER_CREATE, "User, " + userName + " for accountId = " + accountId
                + " and domainId = " + domainId + " was created.");
        return dbUser;
    }
    
    @Override
    public UserAccount updateUser(UpdateUserCmd cmd) throws InvalidParameterValueException {
        Long id = cmd.getId();
        String apiKey = cmd.getApiKey();
        String firstName = cmd.getFirstname();
        String email = cmd.getEmail();
        String lastName = cmd.getLastname();
        String password = cmd.getPassword();
        String secretKey = cmd.getSecretKey();
        String timeZone = cmd.getTimezone();
        String userName = cmd.getUsername();
        
        
        //Input validation
        UserVO user = _userDao.getUser(id);
        
        if (user == null) {
            throw new InvalidParameterValueException("unable to find user by id");
        }

        if((apiKey == null && secretKey != null) || (apiKey != null && secretKey == null)) {
            throw new InvalidParameterValueException("Please provide an api key/secret key pair");
        }
        
        // If the account is an admin type, return an error.  We do not allow this
        Account account = _accountDao.findById(user.getAccountId());
        
        if (account != null && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new PermissionDeniedException("user id : " + id + " is system account, update is not allowed");
        }

        if (firstName == null) { 
            firstName = user.getFirstname();
        }
        if (lastName == null) { 
            lastName = user.getLastname(); 
        }
        if (userName == null) { 
            userName = user.getUsername();  
        }
        if (password == null) { 
            password = user.getPassword();
        }
        if (email == null) {
            email = user.getEmail();
        }
        if (timeZone == null) {
            timeZone = user.getTimezone();
        }
        if (apiKey == null) {
            apiKey = user.getApiKey();
        }
        if (secretKey == null) {
            secretKey = user.getSecretKey();
        }

        Long accountId = user.getAccountId();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("updating user with id: " + id);
        }
        UserAccount userAccount = _userAccountDao.findById(id);
        try {
            //check if the apiKey and secretKey are globally unique
            if (apiKey != null && secretKey != null) {
                Pair<User, Account> apiKeyOwner = _accountDao.findUserAccountByApiKey(apiKey);

                if(apiKeyOwner != null) {
                    User usr = apiKeyOwner.first();
                    if (usr.getId() != id) {
                        throw new InvalidParameterValueException("The api key:"+apiKey+" exists in the system for user id:"+id+" ,please provide a unique key");
                    } else {
                        //allow the updation to take place
                    }
                }
            }

            _userDao.update(id, userName, password, firstName, lastName, email, accountId, timeZone, apiKey, secretKey);
            EventUtils.saveEvent(new Long(1), Long.valueOf(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_UPDATE, "User, " + userName + " for accountId = "
                    + accountId + " domainId = " + userAccount.getDomainId() + " and timezone = "+timeZone + " was updated.");
        } catch (Throwable th) {
            s_logger.error("error updating user", th);
            EventUtils.saveEvent(Long.valueOf(1), Long.valueOf(1), EventVO.LEVEL_ERROR, EventTypes.EVENT_USER_UPDATE, "Error updating user, " + userName
                    + " for accountId = " + accountId + " and domainId = " + userAccount.getDomainId());
            throw new CloudRuntimeException("Unable to update user " + id);
        } 
        return _userAccountDao.findById(id);
    }
    
    @Override
    public UserAccount disableUser(DisableUserCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Long userId = cmd.getId();
        Account adminAccount = UserContext.current().getCaller();
        
        //Check if user exists in the system
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
        	//user successfully disabled
        	return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException("Unable to disable user " + userId);
        }
    }
    
    @Override
    public UserAccount enableUser(EnableUserCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Long userId = cmd.getId();
        Account adminAccount = UserContext.current().getCaller();
        boolean success = false;
        
        //Check if user exists in the system
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

        // If the user is a System user, return an error.  We do not allow this
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
    //This method deletes the account
    public boolean deleteUserAccount(DeleteAccountCmd cmd) {
        UserContext ctx = UserContext.current();
        long callerUserId = ctx.getCallerUserId();
        Account caller = ctx.getCaller();
        
        Long accountId = cmd.getId();
                
        // If the user is a System user, return an error.  We do not allow this
        AccountVO account = _accountDao.findById(accountId);
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new PermissionDeniedException("Account id : " + accountId + " is a system account, delete is not allowed");
        }
        
        if(account == null){
        	throw new InvalidParameterValueException("The specified account does not exist in the system");
        }
        
        if(account.getRemoved() != null){
        	s_logger.info("The account:"+account.getAccountName()+" is already removed");
        	return true;
        }
        
        return deleteAccount(account, callerUserId, caller);
    }
    
    
    
    @Override
    public AccountVO enableAccount(EnableAccountCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        boolean success = false;
        Account account = _accountDao.findActiveAccount(accountName, domainId);

        //Check if account exists
        if (account == null) {
            s_logger.error("Unable to find account " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }
        
        //Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException ("Can not modify system account");
        }
        
        //Check if user performing the action is allowed to modify this account
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
    public AccountVO disableAccount(DisableAccountCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
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
    public AccountVO updateAccount(UpdateAccountCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String newAccountName = cmd.getNewName();
        
        boolean success = false;
        Account account = _accountDao.findAccount(accountName, domainId);
        
        //Check if account exists
        if (account == null) {
            s_logger.error("Unable to find account " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }
        
        //Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException ("Can not modify system account");
        }
        
        //Check if user performing the action is allowed to modify this account
        Account adminAccount = UserContext.current().getCaller();
        if ((adminAccount != null) && (adminAccount.getType() != Account.ACCOUNT_TYPE_ADMIN) && _domainDao.isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
          throw new PermissionDeniedException("Invalid account " + accountName + " in domain " + domainId + " given, permission denied");
        }

        //check if the given account name is unique in this domain for updating
        Account duplicateAcccount = _accountDao.findAccount(newAccountName, domainId);
        if(duplicateAcccount != null && duplicateAcccount.getRemoved() == null && duplicateAcccount.getId() != account.getId()){//allow same account to update itself
            throw new PermissionDeniedException("There already exists an account with the name:"+newAccountName+" in the domain:"+domainId+" with existing account id:"+duplicateAcccount.getId());
        }
                
        if (account.getAccountName().equals(newAccountName)) {
            success = true;
        } else {
            AccountVO acctForUpdate = _accountDao.createForUpdate();
            acctForUpdate.setAccountName(newAccountName);
            success = _accountDao.update(Long.valueOf(account.getId()), acctForUpdate);
        }
        if (success) {
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException("Unable to update account " + accountName + " in domain " + domainId);
        }
    }

	@Override
	public boolean deleteUser(DeleteUserCmd deleteUserCmd) {
		long id = deleteUserCmd.getId();
		
		UserVO user = _userDao.findById(id);
		
		if(user == null) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }
		
        if ((user != null) && (user.getAccountId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new InvalidParameterValueException("Account id : " + user.getAccountId() + " is a system account, delete for user associated with this account is not allowed");
        }
        
        long accountId = user.getAccountId();
        long userId = UserContext.current().getCallerUserId();
        boolean success = _userDao.remove(id);
        if(success){
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, EventTypes.EVENT_USER_DELETE, "Deleted User, " + user.getUsername() + " for accountId = " + user.getAccountId());
        } else {
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_USER_DELETE, "Failed to delete User, " + user.getUsername() + " for accountId = " + user.getAccountId());
        }
        return success;
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
                    
                    List<AccountVO> accounts = _accountDao.findCleanups();
                    s_logger.info("Found " + accounts.size() + " accounts to cleanup");
                    for (AccountVO account : accounts) {
                        s_logger.debug("Cleaning up " + account.getId());
                        try {
                            cleanupAccount(account, getSystemUser().getId(), getSystemAccount());
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on account " + account.getId(), e);
                        }
                    }
                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    if(txn != null) {
                        txn.close();
                    }
                    
                    lock.unlock();
                }
            } catch (Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }
}
