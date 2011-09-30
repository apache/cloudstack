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
import java.util.Set;

import com.cloud.domain.DomainVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

/*
 * Data Access Object for vm_templates table
 */
public interface VMTemplateDao extends GenericDao<VMTemplateVO, Long> {
	
	
	public List<VMTemplateVO> listByPublic();
	//finds by the column "unique_name"
	public VMTemplateVO findByName(String templateName);
	//finds by the column "name" 
	public VMTemplateVO findByTemplateName(String templateName);

	/**
	 * Find a template by name for a specific account.
	 * @param templateName the name to search for
	 * @param accountId the account to use for filtering the search results
	 * @return the template with the given name for the given account if it exists, null otherwise
	 */
	public VMTemplateVO findByTemplateNameAccountId(String templateName, Long accountId);

	//public void update(VMTemplateVO template);

	public List<VMTemplateVO> listAllSystemVMTemplates();

	public List<VMTemplateVO> listDefaultBuiltinTemplates();
	public String getRoutingTemplateUniqueName();
	public List<VMTemplateVO> findIsosByIdAndPath(Long domainId, Long accountId, String path);
	public List<VMTemplateVO> listReadyTemplates();
	public List<VMTemplateVO> listByAccountId(long accountId);
	public Set<Pair<Long, Long>> searchTemplates(String name, String keyword, TemplateFilter templateFilter, boolean isIso, List<HypervisorType> hypers, Boolean bootable,
			DomainVO domain, Long pageSize, Long startIndex, Long zoneId, HypervisorType hyperType, boolean onlyReady, boolean showDomr, List<Account> permittedAccounts, Account caller);
	
	public long addTemplateToZone(VMTemplateVO tmplt, long zoneId);
	public List<VMTemplateVO> listAllInZone(long dataCenterId);	
	
	public List<VMTemplateVO> listByHypervisorType(HypervisorType hyperType);
	public List<VMTemplateVO> publicIsoSearch(Boolean bootable);
    VMTemplateVO findSystemVMTemplate(long zoneId);
    VMTemplateVO findSystemVMTemplate(long zoneId, HypervisorType hType);

    VMTemplateVO findRoutingTemplate(HypervisorType type);
    List<Long> listPrivateTemplatesByHost(Long hostId);
    public Long countTemplatesForAccount(long accountId);
	
}
