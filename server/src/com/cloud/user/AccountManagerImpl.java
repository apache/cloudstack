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
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.UpdateResourceLimitCmd;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.Criteria;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={AccountManager.class})
public class AccountManagerImpl implements AccountManager {
	public static final Logger s_logger = Logger.getLogger(AccountManagerImpl.class.getName());
	
	private String _name;
	private AccountDao _accountDao;
	private DomainDao _domainDao;
	private UserDao _userDao;
	private VMTemplateDao _templateDao;
	private ResourceLimitDao _resourceLimitDao;
	private ResourceCountDao _resourceCountDao;
	private final GlobalLock m_resourceCountLock = GlobalLock.getInternLock("resource.count");
	
	@Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
    	_name = name;
    	final ComponentLocator locator = ComponentLocator.getCurrentLocator();
    	
    	_accountDao = locator.getDao(AccountDao.class);
        if (_accountDao == null) {
            throw new ConfigurationException("Unable to get the account dao.");
        }
        
        _domainDao = locator.getDao(DomainDao.class);
        if (_domainDao == null) {
        	throw new ConfigurationException("Unable to get the domain dao.");
        }
        
        _userDao = locator.getDao(UserDao.class);
        if (_userDao == null) {
            throw new ConfigurationException("Unable to get the user dao.");
        }
        
        _templateDao = locator.getDao(VMTemplateDao.class);
        if (_templateDao == null) {
        	throw new ConfigurationException("Unable to get the template dao.");
        }
        
        _resourceLimitDao = locator.getDao(ResourceLimitDao.class);
        if (_resourceLimitDao == null) {
            throw new ConfigurationException("Unable to get " + ResourceLimitDao.class.getName());
        }
        
        _resourceCountDao = locator.getDao(ResourceCountDao.class);
        if (_resourceCountDao == null) {
            throw new ConfigurationException("Unable to get " + ResourceCountDao.class.getName());
        }
    	
    	return true;
    }
	
    @Override
    public String getName() {
        return _name;
    }
	
    @Override
    public boolean start() {
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
                Account account = _accountDao.findById(accountId);
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
    public boolean resourceLimitExceeded(AccountVO account, ResourceType type, long...count) {
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
        Long domainId = (Long) c.getCriteria(Criteria.DOMAINID);
        Long accountId = (Long) c.getCriteria(Criteria.ACCOUNTID);
        ResourceType type = (ResourceType) c.getCriteria(Criteria.TYPE);
        
        // For 2.0, we are just limiting the scope to having an user retrieve
        // limits for himself and if limits don't exist, use the ROOT domain's limits.
        // - Will
        List<ResourceLimitVO> limits = new ArrayList<ResourceLimitVO>();
        

        if(accountId!=null && domainId!=null)
        {
	        //if domainId==1 and account belongs to admin
	        //return all records for resource limits (bug 3778)
	        
	        if(domainId==1)
	        {
	        	AccountVO account = _accountDao.findById(accountId);
	        	
	        	if(account!=null && account.getType()==1)
	        	{
	        		//account belongs to admin
	        		//return all limits
	        		limits = _resourceLimitDao.listAll();
	        		return limits;
	        	}
	        }
	
	        //if account belongs to system, accountid=1,domainid=1
	        //return all the records for resource limits (bug:3778)
	        if(accountId==1 && domainId==1)
	        {
	        	limits = _resourceLimitDao.listAll();
	        	return limits;
	        }
        }
        
        if (accountId != null) {
        	SearchBuilder<ResourceLimitVO> sb = _resourceLimitDao.createSearchBuilder();
        	sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        	sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);

        	SearchCriteria<ResourceLimitVO> sc = sb.create();

        	if (accountId != null) {
        		sc.setParameters("accountId", accountId);
        	}

        	if (type != null) {
        		sc.setParameters("type", type);
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
    public ResourceLimitVO updateResourceLimit(UpdateResourceLimitCmd cmd) throws InvalidParameterValueException  {

    	Account account = (Account)UserContext.current().getAccountObject();
    	Long userId = UserContext.current().getUserId();
    	Long domainId = cmd.getDomainId();
    	Long max = cmd.getMax();
    	Integer type = cmd.getResourceType();
    	
    	//Validate input
        Long accountId = null;

        if (max == null) {
        	max = new Long(-1);
        } else if (max < -1) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify either '-1' for an infinite limit, or a limit that is at least '0'.");
        }

        // Map resource type
        ResourceType resourceType;
        try {
        	resourceType = ResourceType.values()[type];
        } catch (ArrayIndexOutOfBoundsException e) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid resource type.");
        }               

        /*
        if (accountName==null && domainId != null && !domainId.equals(DomainVO.ROOT_DOMAIN)) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Resource limits must be made for an account or the ROOT domain.");
        }
        */

        if (account != null) {
            if (domainId != null) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update resource limit for " + ((account.getAccountName() == null) ? "" : "account " + account.getAccountName() + " in ") + "domain " + domainId + ", permission denied");
                }
            } else if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                domainId = DomainVO.ROOT_DOMAIN; // for root admin, default to root domain if domain is not specified
            }                 
            
            if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                if ((domainId != null) && (account.getAccountName() == null) && domainId.equals(account.getDomainId())) {
                    // if the admin is trying to update their own domain, disallow...
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update resource limit for " + ((account.getAccountName() == null) ? "" : "account " + account.getAccountName() + " in ") + "domain " + domainId + ", permission denied");
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
            			throw new ServerApiException(BaseCmd.PARAM_ERROR, "The current ROOT domain limit for resource type " + resourceType + " is " + currentRootDomainMax + " and cannot be exceeded.");
            		}
            	}
            }
        } else if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN; // for system commands, default to root domain if domain is not specified
        }

        if (domainId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to update resource limit, unable to determine domain in which to update limit.");
        } else if (account.getAccountName() != null) {
            if (domainId == null) {
                domainId = DomainVO.ROOT_DOMAIN;
            }
            Account userAccount = _accountDao.findActiveAccount(account.getAccountName(), domainId);
            if (userAccount == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find account by name " + account.getAccountName() + " in domain with id " + domainId);
            }
            accountId = userAccount.getId();
            domainId = userAccount.getDomainId();
        }               

        if (accountId != null) domainId = null;
        
    	// Either a domainId or an accountId must be passed in, but not both.
        if ((domainId == null) && (accountId == null)) {
            throw new InvalidParameterValueException("Either a domainId or domainId/accountId must be passed in.");
        }

        // Check if the domain or account exists and is valid
        if (accountId != null) {
        	AccountVO accountHandle = _accountDao.findById(accountId);
            if (accountHandle == null) {
                throw new InvalidParameterValueException("Please specify a valid account ID.");
            } else if (accountHandle.getRemoved() != null) {
            	throw new InvalidParameterValueException("Please specify an active account.");
            } else if (account.getType() == Account.ACCOUNT_TYPE_ADMIN || accountHandle.getType() == Account.ACCOUNT_ID_SYSTEM) {
            	throw new InvalidParameterValueException("Please specify a non-admin account.");
            }

            DomainVO domain = _domainDao.findById(account.getDomainId());
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
        if (type == null) {
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

        if (type != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, type);
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
}
