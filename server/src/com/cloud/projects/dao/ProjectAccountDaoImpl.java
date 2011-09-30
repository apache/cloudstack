/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.projects.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectAccountVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={ProjectAccountDao.class})
public class ProjectAccountDaoImpl extends GenericDaoBase<ProjectAccountVO, Long> implements ProjectAccountDao {
    private static final Logger s_logger = Logger.getLogger(ProjectAccountDaoImpl.class);
    protected final SearchBuilder<ProjectAccountVO> AllFieldsSearch;
    
    protected ProjectAccountDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("role", AllFieldsSearch.entity().getAccountRole(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectId", AllFieldsSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectAccountId", AllFieldsSearch.entity().getProjectAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public ProjectAccountVO getProjectOwner(long projectId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("role", ProjectAccount.Role.Owner);
        sc.setParameters("projectId", projectId);
        
        return findOneBy(sc);
    }
    
    @Override
    public List<ProjectAccountVO> listByProjectId(long projectId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);
        
        return listBy(sc);
    }
    
    @Override
    public ProjectAccountVO findByProjectIdAccountId(long projectId, long accountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);
        sc.setParameters("accountId", accountId);
        
        return findOneBy(sc);
    }
    
    @Override
    public boolean canAccessProjectAccount(long accountId, long projectAccountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("projectAccountId", projectAccountId);
        
        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canModifyProjectAccount(long accountId, long projectAccountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("projectAccountId", projectAccountId);
        sc.setParameters("role", ProjectAccount.Role.Owner);
        
        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public List<Long> listPermittedAccountIds(long accountId) {
        List<Long> permittedAccounts = new ArrayList<Long>();
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        
        List<ProjectAccountVO> records = listBy(sc);
        
        for (ProjectAccountVO record : records) {
            permittedAccounts.add(record.getProjectAccountId());
        }
        
        return permittedAccounts;
    }
}
