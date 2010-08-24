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

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;

@Local(value={AccountManager.class})
public class AccountManagerImpl implements AccountManager {
	public static final Logger s_logger = Logger.getLogger(AccountManagerImpl.class.getName());
	
	private String _name;
	@Inject private AccountDao _accountDao;
	@Inject private DomainDao _domainDao;
	@Inject private ResourceLimitDao _resourceLimitDao;
	@Inject private ResourceCountDao _resourceCountDao;
	private final GlobalLock m_resourceCountLock = GlobalLock.getInternLock("resource.count");
	
	AccountVO _systemAccount;
	
	@Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
    	_name = name;
    	
    	_systemAccount = _accountDao.findById(AccountVO.ACCOUNT_ID_SYSTEM);
    	if (_systemAccount == null) {
    	    throw new ConfigurationException("Unable to find the system account using " + AccountVO.ACCOUNT_ID_SYSTEM);
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
    public ResourceLimitVO updateResourceLimit(Long domainId, Long accountId, ResourceType type, Long max) throws InvalidParameterValueException  {
    	// Either a domainId or an accountId must be passed in, but not both.
        if ((domainId == null) && (accountId == null)) {
            throw new InvalidParameterValueException("Either a domainId or domainId/accountId must be passed in.");
        }

        // Check if the domain or account exists and is valid
        if (accountId != null) {
        	AccountVO account = _accountDao.findById(accountId);
            if (account == null) {
                throw new InvalidParameterValueException("Please specify a valid account ID.");
            } else if (account.getRemoved() != null) {
            	throw new InvalidParameterValueException("Please specify an active account.");
            } else if (account.getType() == Account.ACCOUNT_TYPE_ADMIN || account.getType() == Account.ACCOUNT_ID_SYSTEM) {
            	throw new InvalidParameterValueException("Please specify a non-admin account.");
            }

            DomainVO domain = _domainDao.findById(account.getDomainId());
            long parentMaximum = findCorrectResourceLimit(domain, type);
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
                long parentMaximum = findCorrectResourceLimit(parentDomain, type);
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
            return _resourceLimitDao.persist(new ResourceLimitVO(domainId, accountId, type, max));
        }
    }
    
    @Override
    public AccountVO getSystemAccount() {
        return _systemAccount;
    }

}
