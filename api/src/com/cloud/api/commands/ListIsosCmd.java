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
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.async.AsyncJob;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;

@Implementation(description="Lists all available ISO files.", responseObject=TemplateResponse.class)
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
    
    public boolean listInReadyState() {
        Account account = UserContext.current().getCaller();
        // It is account specific if account is admin type and domainId and accountName are not null
        //boolean isAccountSpecific = (account == null || isAdmin(account.getType())) && (getAccountName() != null) && (getDomainId() != null);
        // Show only those that are downloaded.
        TemplateFilter templateFilter = TemplateFilter.valueOf(getIsoFilter());
        boolean onlyReady = (templateFilter == TemplateFilter.featured) || (templateFilter == TemplateFilter.selfexecutable) || (templateFilter == TemplateFilter.sharedexecutable)
        || (templateFilter == TemplateFilter.executable) || (templateFilter == TemplateFilter.community);
        return onlyReady;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    
    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Iso;
    }
    
    @Override
    public void execute(){
        Set<Pair<Long, Long>> isoZonePairSet = _mgr.listIsos(this);
        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
        List<TemplateResponse> templateResponses = new ArrayList<TemplateResponse>();

        for (Pair<Long, Long> iso : isoZonePairSet) {
            List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
            responses = _responseGenerator.createIsoResponses(iso.first(), iso.second(), listInReadyState());
            templateResponses.addAll(responses);
        }
        response.setResponses(templateResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
