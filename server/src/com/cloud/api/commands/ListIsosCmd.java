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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.async.AsyncJobVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.domain.DomainVO;
import com.cloud.host.HostVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.VMTemplateDao.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListIsosCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListIsosCmd.class.getName());

    private static final String s_name = "listisosresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_PUBLIC, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_READY, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ISO_FILTER, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.BOOTABLE, Boolean.FALSE));
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
    	String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
    	Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
    	Long id = (Long) params.get(BaseCmd.Properties.ID.getName());
    	String name = (String) params.get(BaseCmd.Properties.NAME.getName());
    	Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        String isoFilterString = (String) params.get(BaseCmd.Properties.ISO_FILTER.getName());
        Boolean bootable = (Boolean)params.get(BaseCmd.Properties.BOOTABLE.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());

    	boolean isAdmin = false;

    	TemplateFilter isoFilter;
        try {
        	if (isoFilterString == null) {
        		isoFilter = TemplateFilter.selfexecutable;
        	} else {
        		isoFilter = TemplateFilter.valueOf(isoFilterString);
        	}
        } catch (IllegalArgumentException e) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid template filter.");
        }
    	
        Long accountId = null;
        if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
            isAdmin = true;
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list events.");
                }
                if (accountName != null) {
                    Account userAccount = getManagementServer().findAccountByName(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
            accountName = account.getAccountName();
            domainId = account.getDomainId();
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
        
        boolean onlyReady = (isoFilter == TemplateFilter.featured) || 
	  						(isoFilter == TemplateFilter.selfexecutable) || 
	  						(isoFilter == TemplateFilter.sharedexecutable) ||
	  						(isoFilter == TemplateFilter.executable && accountId != null) ||
	  						(isoFilter == TemplateFilter.community);
        
        List<VMTemplateVO> isos = null;
        try {
        	isos = getManagementServer().listTemplates(id, name, keyword, isoFilter, true, bootable, accountId, pageSize, startIndex, zoneId);
        } catch (Exception e) {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
        }
        
        int numTags = 0;
        Map<Long, List<VMTemplateHostVO>> isoHostsMap = new HashMap<Long, List<VMTemplateHostVO>>();
        for (VMTemplateVO iso : isos) {
        	List<VMTemplateHostVO> isoHosts = getManagementServer().listTemplateHostBy(iso.getId(), zoneId);
            if (iso.getName().equals("xs-tools.iso")) {
                List<Long> xstoolsZones = new ArrayList<Long>();
                // the xs-tools.iso is a special case since it will be available on every computing host in the zone and we want to return it once per zone
                List<VMTemplateHostVO> xstoolsHosts = new ArrayList<VMTemplateHostVO>();
                for (VMTemplateHostVO isoHost : isoHosts) {
                    HostVO host = getManagementServer().getHostBy(isoHost.getHostId());
                    if (!xstoolsZones.contains(Long.valueOf(host.getDataCenterId()))) {
                        xstoolsZones.add(Long.valueOf(host.getDataCenterId()));
                        xstoolsHosts.add(isoHost);
                    }
                }
                isoHostsMap.put(iso.getId(), xstoolsHosts);
                numTags += xstoolsHosts.size();
            } else {
                isoHostsMap.put(iso.getId(), isoHosts);
                numTags += isoHosts.size();
            }
        }

        List<Object> isoTagList = new ArrayList<Object>();
        List<Pair<String, Object>> isoTags = new ArrayList<Pair<String, Object>>();
        for (VMTemplateVO iso : isos) {
        	List<VMTemplateHostVO> isoHosts = isoHostsMap.get(iso.getId());
        	for (VMTemplateHostVO isoHost : isoHosts) {
        		if (onlyReady && isoHost.getDownloadState() != Status.DOWNLOADED) {
    				continue;
    			}
        		
        		List<Pair<String, Object>> isoData = new ArrayList<Pair<String, Object>>();
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), iso.getId().toString()));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), iso.getName()));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), iso.getDisplayText()));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.IS_PUBLIC.getName(), Boolean.valueOf(iso.isPublicTemplate()).toString()));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(isoHost.getCreated())));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.IS_READY.getName(), Boolean.valueOf(isoHost.getDownloadState()==Status.DOWNLOADED).toString()));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.BOOTABLE.getName(), Boolean.valueOf(iso.isBootable()).toString()));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.IS_FEATURED.getName(), Boolean.valueOf(iso.isFeatured()).toString()));
        		isoData.add(new Pair<String, Object>(BaseCmd.Properties.CROSS_ZONES.getName(), Boolean.valueOf(iso.isCrossZones()).toString()));
        		
        		GuestOS os = getManagementServer().findGuestOSById(iso.getGuestOSId());
	            if(os != null) {
	            	isoData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), os.getId()));
	            	isoData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_NAME.getName(), os.getDisplayName()));
	            } else {
	            	isoData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), -1));
	            	isoData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_NAME.getName(), ""));
	            }
	            	
        		// add account ID and name
        		Account owner = getManagementServer().findAccountById(iso.getAccountId());
        		if (owner != null) {
        			isoData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), owner.getAccountName()));
                    isoData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), owner.getDomainId()));
                    isoData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(owner.getDomainId()).getName()));
        		}
        		
        		// Add the zone ID
                HostVO host = getManagementServer().getHostBy(isoHost.getHostId());
                isoData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), host.getDataCenterId()));

                DataCenterVO datacenter = getManagementServer().getDataCenterBy(host.getDataCenterId());
                isoData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), datacenter.getName()));
        	                
                // If the user is an admin, add the template download status
                if (isAdmin || account.getId().longValue() == iso.getAccountId()) {
                	// add download status
                	if (isoHost.getDownloadState()!=Status.DOWNLOADED) {
                		String isoStatus = "Processing";
                		if (isoHost.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                			isoStatus = "Download Complete";
                		} else if (isoHost.getDownloadState() == VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS) {
                			if (isoHost.getDownloadPercent() == 100) {
                				isoStatus = "Installing ISO";
                			} else {
                				isoStatus = isoHost.getDownloadPercent() + "% Downloaded";
                			}
                		} else {
                			isoStatus = isoHost.getErrorString();
                		}
                		isoData.add(new Pair<String, Object>(BaseCmd.Properties.ISO_STATUS.getName(), isoStatus));
                	} else {
                		isoData.add(new Pair<String, Object>(BaseCmd.Properties.ISO_STATUS.getName(), "Successfully Installed"));
                	}
                }

                long isoSize = isoHost.getSize();
                if (isoSize > 0) {
                	isoData.add(new Pair<String, Object>(BaseCmd.Properties.SIZE.getName(), isoSize));
                }
                
                AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("vm_template", iso.getId());
                if(asyncJob != null) {
                	isoData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), asyncJob.getId().toString()));
                	isoData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(asyncJob.getStatus())));
                }

                isoTagList.add(isoData);     
        	}
        }
        
        Object[] iTag = new Object[isoTagList.size()];
        for (int i = 0; i < isoTagList.size(); i++) {
        	iTag[i] = isoTagList.get(i);
        }
        
        Pair<String, Object> isoTag = new Pair<String, Object>("iso", iTag);
        isoTags.add(isoTag);
        
        return isoTags;
    }
}
