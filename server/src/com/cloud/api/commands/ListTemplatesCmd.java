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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.async.AsyncJobVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.host.HostVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.VMTemplateDao.TemplateFilter;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListTemplatesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListTemplatesCmd.class.getName());

    private static final String s_name = "listtemplatesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_PUBLIC, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_READY, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TEMPLATE_FILTER, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));

    }

    @Override
    public String getName() {
        return s_name;
    }
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Long id = (Long) params.get(BaseCmd.Properties.ID.getName());
    	String name = (String) params.get(BaseCmd.Properties.NAME.getName());
    	Boolean isPublic = (Boolean) params.get(BaseCmd.Properties.IS_PUBLIC.getName());
    	Boolean isReady = (Boolean) params.get(BaseCmd.Properties.IS_READY.getName());
    	String templateFilterString = (String) params.get(BaseCmd.Properties.TEMPLATE_FILTER.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());   
        
        boolean isAdmin = false;                                        
        Long accountId = null;
        if ((account == null) || account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            isAdmin = true;
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list templates.");
                }

                if (accountName != null) {
                    account = getManagementServer().findActiveAccount(accountName, domainId);
                    if (account == null) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = account.getId();
                } 
            }
        } else {
            accountId = account.getId();
            accountName = account.getAccountName();
            domainId = account.getDomainId();
        }       
        
        TemplateFilter templateFilter;
        try {
        	templateFilter = TemplateFilter.valueOf(templateFilterString);
        } catch (IllegalArgumentException e) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid template filter.");
        }

        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 50;
        if (pageSize != null) {
            pageSizeNum = pageSize.intValue();
        }
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }
        
        boolean onlyReady = (templateFilter == TemplateFilter.featured) || 
        				  	(templateFilter == TemplateFilter.selfexecutable) || 
        				  	(templateFilter == TemplateFilter.sharedexecutable) ||
        				  	(templateFilter == TemplateFilter.executable && accountId != null) ||
        				  	(templateFilter == TemplateFilter.community);

        boolean showDomr = (templateFilter != TemplateFilter.selfexecutable);
        
        List<VMTemplateVO> templates = null;
        try {
        	templates = getManagementServer().listTemplates(id, name, keyword, templateFilter, false, null, accountId, pageSize, startIndex, zoneId);
        } catch (Exception e) {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
        }
        
        List<Object> tTagList = new ArrayList<Object>();
        List<Pair<String, Object>> templateTags = new ArrayList<Pair<String, Object>>();
        for (VMTemplateVO template : templates) {
        	if (!showDomr && template.getId() == TemplateConstants.DEFAULT_SYSTEM_VM_DB_ID) {
    			continue;
    		}
        	
        	List<VMTemplateHostVO> templateHostRefsForTemplate = getManagementServer().listTemplateHostBy(template.getId(), zoneId);
        	
        	for (VMTemplateHostVO templateHostRef : templateHostRefsForTemplate) {
        		if (onlyReady && templateHostRef.getDownloadState() != Status.DOWNLOADED) {
    				continue;
    			}
        		
        		List<Pair<String, Object>> templateData = new ArrayList<Pair<String, Object>>();
        		templateData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), template.getId().toString()));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), template.getName()));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), template.getDisplayText()));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.IS_PUBLIC.getName(), Boolean.valueOf(template.isPublicTemplate()).toString()));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(templateHostRef.getCreated())));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.IS_READY.getName(), Boolean.valueOf(templateHostRef.getDownloadState()==Status.DOWNLOADED).toString()));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.IS_FEATURED.getName(), Boolean.valueOf(template.isFeatured()).toString()));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.PASSWORD_ENABLED.getName(), Boolean.valueOf(template.getEnablePassword()).toString()));
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.CROSS_ZONES.getName(), Boolean.valueOf(template.isCrossZones()).toString()));
                
                GuestOS os = getManagementServer().findGuestOSById(template.getGuestOSId());
                if (os != null) {
                	templateData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), os.getId()));
                    templateData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_NAME.getName(), os.getDisplayName()));
                } else {
                	templateData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), -1));
                    templateData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_NAME.getName(), ""));
                }
                
                // add account ID and name
                Account owner = getManagementServer().findAccountById(template.getAccountId());
                if (owner != null) {
                    templateData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), owner.getAccountName()));
                    templateData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), owner.getDomainId()));
                    templateData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(owner.getDomainId()).getName()));
                }
                
                HostVO host = getManagementServer().getHostBy(templateHostRef.getHostId());
                
                // Add the zone ID
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), host.getDataCenterId()));
        		
                DataCenterVO datacenter = getManagementServer().getDataCenterBy(host.getDataCenterId());
                templateData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), datacenter.getName()));
        	                
                // If the user is an admin, add the template download status
                if (isAdmin || account.getId().longValue() == template.getAccountId()) {
                    // add download status
                    if (templateHostRef.getDownloadState()!=Status.DOWNLOADED) {
                        String templateStatus = "Processing";
                        if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS) {
                            if (templateHostRef.getDownloadPercent() == 100) {
                                templateStatus = "Installing Template";
                            } else {
                                templateStatus = templateHostRef.getDownloadPercent() + "% Downloaded";
                            }
                        } else {
                            templateStatus = templateHostRef.getErrorString();
                        }
                        templateData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_STATUS.getName(), templateStatus));
                    } else if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                    	templateData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_STATUS.getName(), "Download Complete"));
                    } else {
                    	templateData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_STATUS.getName(), "Successfully Installed"));
                    }
                }
                
                long templateSize = templateHostRef.getSize();
                if (templateSize > 0) {
                	templateData.add(new Pair<String, Object>(BaseCmd.Properties.SIZE.getName(), templateSize));
                }
                
                AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("vm_template", template.getId());
                if(asyncJob != null) {
                	templateData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), asyncJob.getId().toString()));
                	templateData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(asyncJob.getStatus())));
                }
                
                tTagList.add(templateData);     
        	}
        }
        
        Object[] tTag = new Object[tTagList.size()];
        for (int i = 0; i < tTagList.size(); i++) {
        	tTag[i] = tTagList.get(i);
        }
                               
        Pair<String, Object> templateTag = new Pair<String, Object>("template", tTag);
        templateTags.add(templateTag);

        return templateTags;
    }
}
