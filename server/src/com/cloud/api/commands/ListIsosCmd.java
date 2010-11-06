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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
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
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="listIsos", description="Lists all available ISO files.")
public class ListIsosCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListIsosCmd.class.getName());

    private static final String s_name = "listisosresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account of the ISO file. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.BOOTABLE, type=CommandType.BOOLEAN, description="true if the ISO is bootable, false otherwise")
    private Boolean bootable;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="lists all available ISO files by ID of a domain. If used with the account parameter, lists all available ISO files for the account in the ID of a domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor for which to restrict the search")
    private String hypervisor;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list all isos by id")
    private Long id;

    @Parameter(name=ApiConstants.IS_PUBLIC, type=CommandType.BOOLEAN, description="true if the ISO is publicly available to all users, false otherwise.")
    private Boolean publicIso;

    @Parameter(name=ApiConstants.IS_READY, type=CommandType.BOOLEAN, description="true if this ISO is ready to be deployed")
    private Boolean ready;

    @Parameter(name=ApiConstants.ISO_FILTER, type=CommandType.STRING, description="possible values are \"featured\", \"self\", \"self-executable\",\"executable\", and \"community\". " +
    														"* featured-ISOs that are featured and are publicself-ISOs that have been registered/created by the owner. " +
    														"* selfexecutable-ISOs that have been registered/created by the owner that can be used to deploy a new VM. " +
    														"* executable-all ISOs that can be used to deploy a new VM " +
    														"* community-ISOs that are public.")
    private String isoFilter = TemplateFilter.selfexecutable.toString();

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list all isos by name")
    private String isoName;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the ID of the zone")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Boolean isBootable() {
        return bootable;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public Long getId() {
        return id;
    }

    public Boolean isPublic() {
        return publicIso;
    }

    public Boolean isReady() {
        return ready;
    }

    public String getIsoFilter() {
        return isoFilter;
    }

    public String getIsoName() {
        return isoName;
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
    public ListResponse<TemplateResponse> getResponse() {
        TemplateFilter isoFilterObj = null;

        try {
            if (isoFilter == null) {
                isoFilterObj = TemplateFilter.selfexecutable;
            } else {
                isoFilterObj = TemplateFilter.valueOf(isoFilter);
            }
        } catch (IllegalArgumentException e) {
            // how did we get this far?  The request should've been rejected already before the response stage...
            isoFilterObj = TemplateFilter.selfexecutable;
        }

        boolean isAdmin = false;
        boolean isAccountSpecific = true;
        Account account = (Account)UserContext.current().getAccount();
        if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
            isAdmin = true;
            if ((accountName == null) || (domainId == null)) {
                isAccountSpecific = false;
            }
        }

        boolean onlyReady = (isoFilterObj == TemplateFilter.featured) || 
                            (isoFilterObj == TemplateFilter.selfexecutable) || 
                            (isoFilterObj == TemplateFilter.sharedexecutable) ||
                            (isoFilterObj == TemplateFilter.executable && isAccountSpecific) ||
                            (isoFilterObj == TemplateFilter.community);

        List<VMTemplateVO> isos = (List<VMTemplateVO>)getResponseObject();

        Map<Long, List<VMTemplateHostVO>> isoHostsMap = new HashMap<Long, List<VMTemplateHostVO>>();
        for (VMTemplateVO iso : isos) {
            // TODO:  implement
            List<VMTemplateHostVO> isoHosts = ApiDBUtils.listTemplateHostBy(iso.getId(), zoneId);
            if (iso.getName().equals("xs-tools.iso")) {
                List<Long> xstoolsZones = new ArrayList<Long>();
                // the xs-tools.iso is a special case since it will be available on every computing host in the zone and we want to return it once per zone
                List<VMTemplateHostVO> xstoolsHosts = new ArrayList<VMTemplateHostVO>();
                for (VMTemplateHostVO isoHost : isoHosts) {
                    // TODO:  implement
                    HostVO host = ApiDBUtils.findHostById(isoHost.getHostId());
                    if (!xstoolsZones.contains(Long.valueOf(host.getDataCenterId()))) {
                        xstoolsZones.add(Long.valueOf(host.getDataCenterId()));
                        xstoolsHosts.add(isoHost);
                    }
                }
                isoHostsMap.put(iso.getId(), xstoolsHosts);
            } else {
                isoHostsMap.put(iso.getId(), isoHosts);
            }
        }

        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
        List<TemplateResponse> isoResponses = new ArrayList<TemplateResponse>();
        for (VMTemplateVO iso : isos) {
            List<VMTemplateHostVO> isoHosts = isoHostsMap.get(iso.getId());
            for (VMTemplateHostVO isoHost : isoHosts) {
                if (onlyReady && isoHost.getDownloadState() != Status.DOWNLOADED) {
                    continue;
                }

                TemplateResponse isoResponse = new TemplateResponse();
                isoResponse.setId(iso.getId());
                isoResponse.setName(iso.getName());
                isoResponse.setDisplayText(iso.getDisplayText());
                isoResponse.setPublic(iso.isPublicTemplate());
                isoResponse.setCreated(isoHost.getCreated());
                isoResponse.setReady(isoHost.getDownloadState() == Status.DOWNLOADED);
                isoResponse.setBootable(iso.isBootable());
                isoResponse.setFeatured(iso.isFeatured());
                isoResponse.setCrossZones(iso.isCrossZones());

                // TODO:  implement
                GuestOS os = ApiDBUtils.findGuestOSById(iso.getGuestOSId());
                if (os != null) {
                    isoResponse.setOsTypeId(os.getId());
                    isoResponse.setOsTypeName(os.getDisplayName());
                } else {
                    isoResponse.setOsTypeId(-1L);
                    isoResponse.setOsTypeName("");
                }
                    
                // add account ID and name
                Account owner = ApiDBUtils.findAccountById(iso.getAccountId());
                if (owner != null) {
                    isoResponse.setAccount(owner.getAccountName());
                    isoResponse.setDomainId(owner.getDomainId());
                    // TODO:  implement
                    isoResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
                }
                
                // Add the zone ID
                // TODO:  implement
                HostVO host = ApiDBUtils.findHostById(isoHost.getHostId());
                DataCenterVO datacenter = ApiDBUtils.findZoneById(host.getDataCenterId());
                isoResponse.setZoneId(host.getDataCenterId());
                isoResponse.setZoneName(datacenter.getName());

                // If the user is an admin, add the template download status
                if (isAdmin || account.getId() == iso.getAccountId()) {
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
                        isoResponse.setStatus(isoStatus);
                    } else {
                        isoResponse.setStatus("Successfully Installed");
                    }
                }

                long isoSize = isoHost.getSize();
                if (isoSize > 0) {
                    isoResponse.setSize(isoSize);
                }
                
                AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("vm_template", iso.getId());
                if(asyncJob != null) {
                    isoResponse.setJobId(asyncJob.getId());
                    isoResponse.setJobStatus(asyncJob.getStatus());
                }

                isoResponse.setObjectName("iso");
                isoResponses.add(isoResponse);
            }
        }

        response.setResponses(isoResponses);
        response.setResponseName(getName());
        return response;
    }
}
