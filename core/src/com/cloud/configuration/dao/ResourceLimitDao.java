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

package com.cloud.configuration.dao;

import java.util.List;

import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.utils.db.GenericDao;

public interface ResourceLimitDao extends GenericDao<ResourceLimitVO, Long> {
	
	public ResourceLimitVO findByDomainIdAndType(Long domainId, ResourceCount.ResourceType type);
	public ResourceLimitVO findByAccountIdAndType(Long accountId, ResourceCount.ResourceType type);
	public List<ResourceLimitVO> listByAccountId(Long accountId);
	public List<ResourceLimitVO> listByDomainId(Long domainId);
	public boolean update(Long id, Long max);
	public ResourceCount.ResourceType getLimitType(String type);
	
}
