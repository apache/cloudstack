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

import java.util.List;

import com.cloud.projects.ProjectAccountVO;
import com.cloud.utils.db.GenericDao;

public interface ProjectAccountDao extends GenericDao<ProjectAccountVO, Long>{
    ProjectAccountVO getProjectOwner(long projectId);
    List<ProjectAccountVO> listByProjectId(long projectId);
    ProjectAccountVO findByProjectIdAccountId(long projectId, long accountId);
    
    boolean canAccessAccount(long accountId, long projectAccountId);
    
    boolean canAccessDomain(long accountId, long projectDomainId);
   
    boolean canModifyProjectAccount(long accountId, long projectAccountId);
    boolean canModifyProjectDomain(long accountId, long projectDomainId);
    
    List<Long> listPermittedAccountIds(long accountId);
}
