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

package com.cloud.network.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.SecurityGroupVO;
import com.cloud.server.ManagementServer;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={SecurityGroupDao.class})
public class SecurityGroupDaoImpl extends GenericDaoBase<SecurityGroupVO, Long> implements SecurityGroupDao {
    private SearchBuilder<SecurityGroupVO> AccountIdSearch;
    private DomainDao _domainDao = null;

    protected SecurityGroupDaoImpl() {
        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();

        _domainDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(DomainDao.class);
    }

    @Override
    public List<SecurityGroupVO> listByAccountId(long accountId) {
        SearchCriteria sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listActiveBy(sc);
    }

    @Override
    public boolean isNameInUse(Long accountId, Long domainId, String name) {
        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        } else {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            sc.addAnd("accountId", SearchCriteria.Op.NULL);
        }

        List<SecurityGroupVO> securityGroups = listActiveBy(sc);
        return ((securityGroups != null) && !securityGroups.isEmpty());
    }

    @Override
    public List<SecurityGroupVO> listAvailableGroups(Long accountId, Long domainId) {
        List<SecurityGroupVO> availableGroups = new ArrayList<SecurityGroupVO>();
        if ((accountId != null) || (domainId != null)) {
            if (accountId != null) {
                SearchCriteria sc = createSearchCriteria();
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
                List<SecurityGroupVO> accountGroups = listActiveBy(sc);
                availableGroups.addAll(accountGroups);
            } else if (domainId != null) {
                while (domainId != null) {
                    SearchCriteria sc = createSearchCriteria();
                    sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
                    if (accountId != null) {
                        sc.addAnd("accountId", SearchCriteria.Op.NEQ, accountId); // we added the account specific ones above
                    }
                    List<SecurityGroupVO> domainGroups = listActiveBy(sc);
                    availableGroups.addAll(domainGroups);

                    // get the parent domain, repeat the loop
                    DomainVO domain = _domainDao.findById(domainId);
                    domainId = domain.getParent();
                }
            }
        }
        return availableGroups;
    }
}
