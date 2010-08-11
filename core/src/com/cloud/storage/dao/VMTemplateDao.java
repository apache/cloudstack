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

package com.cloud.storage.dao;

import java.util.List;

import com.cloud.domain.DomainVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDao;

/*
 * Data Access Object for vm_templates table
 */
public interface VMTemplateDao extends GenericDao<VMTemplateVO, Long> {
	
	public enum TemplateFilter {
		featured,			// returns templates that have been marked as featured and public
		self,				// returns templates that have been registered or created by the calling user
		selfexecutable,		// same as self, but only returns templates that are ready to be deployed with
		sharedexecutable,	// ready templates that have been granted to the calling user by another user
		executable,			// templates that are owned by the calling user, or public templates, that can be used to deploy a new VM
		community,			// returns templates that have been marked as public but not featured
		all					// all templates (only usable by ROOT admins)
	}
	
	public List<VMTemplateVO> listByPublic();
	//finds by the column "unique_name"
	public VMTemplateVO findByName(String templateName);
	//finds by the column "name" 
	public VMTemplateVO findByTemplateName(String templateName);
	//public void update(VMTemplateVO template);
	public VMTemplateVO findRoutingTemplate();
	public VMTemplateVO findConsoleProxyTemplate();
	public VMTemplateVO findDefaultBuiltinTemplate();
	public String getRoutingTemplateUniqueName();
	public List<VMTemplateVO> findIsosByIdAndPath(Long domainId, Long accountId, String path);
	public List<VMTemplateVO> listReadyTemplates();
	public List<VMTemplateVO> listByAccountId(long accountId);
	public List<VMTemplateVO> searchTemplates(String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Account account, DomainVO domain, Integer pageSize, Long startIndex, Long zoneId);
	
	public long addTemplateToZone(VMTemplateVO tmplt, long zoneId);
	public List<VMTemplateVO> listAllInZone(long dataCenterId);	
	
}
