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

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.host.HostVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao.TemplateFilter;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="listTemplates")
public class ListTemplatesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListTemplatesCmd.class.getName());

    private static final String s_name = "listtemplatesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String templateName;

    @Parameter(name="templatefilter", type=CommandType.STRING, required=true)
    private String templateFilter;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateFilter() {
        return templateFilter;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        TemplateFilter templateFilterObj;
        try {
            templateFilterObj = TemplateFilter.valueOf(templateFilter);
        } catch (IllegalArgumentException e) {
            // how did we get this far?  The request should've been rejected already before the response stage...
            templateFilterObj = TemplateFilter.selfexecutable;
        }

        boolean isAdmin = false;
        boolean isAccountSpecific = true;
        Account account = (Account)UserContext.current().getAccountObject();
        if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
            isAdmin = true;
            if ((accountName == null) || (domainId == null)) {
                isAccountSpecific = false;
            }
        }

        boolean onlyReady = (templateFilterObj == TemplateFilter.featured) || 
                            (templateFilterObj == TemplateFilter.selfexecutable) || 
                            (templateFilterObj == TemplateFilter.sharedexecutable) ||
                            (templateFilterObj == TemplateFilter.executable && isAccountSpecific) ||
                            (templateFilterObj == TemplateFilter.community);

        boolean showDomr = (templateFilterObj != TemplateFilter.selfexecutable);

        // get the response
        List<VMTemplateVO> templates = (List<VMTemplateVO>)getResponseObject();
        ListResponse response = new ListResponse();
        List<TemplateResponse> templateResponses = new ArrayList<TemplateResponse>();

        for (VMTemplateVO template : templates) {
            if (!showDomr && template.getId() == TemplateConstants.DEFAULT_SYSTEM_VM_DB_ID) {
                continue;
            }

            List<VMTemplateHostVO> templateHostRefsForTemplate = ApiDBUtils.listTemplateHostBy(template.getId(), zoneId);

            for (VMTemplateHostVO templateHostRef : templateHostRefsForTemplate) {
                if (onlyReady && templateHostRef.getDownloadState() != Status.DOWNLOADED) {
                    continue;
                }

                TemplateResponse templateResponse = new TemplateResponse();
                templateResponse.setId(template.getId());
                templateResponse.setName(template.getName());
                templateResponse.setDisplayText(template.getDisplayText());
                templateResponse.setPublic(template.isPublicTemplate());
                templateResponse.setCreated(templateHostRef.getCreated());
                if (template.getRemoved() != null) {
                    templateResponse.setRemoved(template.getRemoved());
                }
                templateResponse.setReady(templateHostRef.getDownloadState()==Status.DOWNLOADED);
                templateResponse.setFeatured(template.isFeatured());
                templateResponse.setPasswordEnabled(template.getEnablePassword());
                templateResponse.setCrossZones(template.isCrossZones());
                templateResponse.setFormat(template.getFormat());
                
                GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
                if (os != null) {
                    templateResponse.setOsTypeId(os.getId());
                    templateResponse.setOsTypeName(os.getDisplayName());
                } else {
                    templateResponse.setOsTypeId(-1L);
                    templateResponse.setOsTypeName("");
                }
                
                // add account ID and name
                Account owner = ApiDBUtils.findAccountById(template.getAccountId());
                if (owner != null) {
                    templateResponse.setAccount(owner.getAccountName());
                    templateResponse.setDomainId(owner.getDomainId());
                    templateResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
                }
                
                HostVO host = ApiDBUtils.findHostById(templateHostRef.getHostId());
                DataCenterVO datacenter = ApiDBUtils.findZoneById(host.getDataCenterId());
                
                // Add the zone ID
                templateResponse.setZoneId(host.getDataCenterId());
                templateResponse.setZoneName(datacenter.getName());
                
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
                        templateResponse.setStatus(templateStatus);
                    } else if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                        templateResponse.setStatus("Download Complete");
                    } else {
                        templateResponse.setStatus("Successfully Installed");
                    }
                }
                
                long templateSize = templateHostRef.getSize();
                if (templateSize > 0) {
                    templateResponse.setSize(templateSize);
                }
                
                AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("vm_template", template.getId());
                if (asyncJob != null) {
                    templateResponse.setJobId(asyncJob.getId());
                    templateResponse.setJobStatus(asyncJob.getStatus());
                }

                templateResponse.setResponseName("template");
                templateResponses.add(templateResponse);
            }
        }

        response.setResponses(templateResponses);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}
