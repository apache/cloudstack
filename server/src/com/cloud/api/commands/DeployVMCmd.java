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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.ExecutionException;

@Implementation(method="deployVirtualMachine", description="Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.")
public class DeployVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeployVMCmd.class.getName());
    
    private static final String s_name = "deployvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name=ApiConstants.DISK_OFFERING_ID, type=CommandType.LONG, description="the ID of the disk offering for the virtual machine. If the template is of ISO format, the diskOfferingId is for the root disk volume. Otherwise this parameter is used to dinidcate the offering for the data disk volume. If the templateId parameter passed is from a Template object, the diskOfferingId refers to a DATA Disk Volume created. If the templateId parameter passed is from an ISO object, the diskOfferingId refers to a ROOT Disk Volume created.")
    private Long diskOfferingId;

    @Parameter(name=ApiConstants.DISPLAY_NAME, type=CommandType.STRING, description="an optional user generated name for the virtual machine")
    private String displayName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name=ApiConstants.GROUP, type=CommandType.STRING, description="an optional group for the virtual machine")
    private String group;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor on which to deploy the virtual machine")
    private String hypervisor;

    @Parameter(name=ApiConstants.NETWORK_GROUP_LIST, type=CommandType.LIST, collectionType=CommandType.STRING, description="comma separated list of network groups that going to be applied to the virtual machine. Should be passed only when vm is created from service offering with Direct Attach Network support")
    private List<String> networkGroupList;

    @Parameter(name=ApiConstants.SERVICE_OFFERING_ID, type=CommandType.LONG, required=true, description="the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @Parameter(name=ApiConstants.SIZE, type=CommandType.LONG, description="the arbitrary size for the DATADISK volume. Mutually exclusive with diskOfferingId")
    private Long size;

    @Parameter(name=ApiConstants.TEMPLATE_ID, type=CommandType.LONG, required=true, description="the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name=ApiConstants.USER_DATA, type=CommandType.STRING, description="an optional binary data that can be sent to the virtual machine upon a successful deployment. This binary data must be base64 encoded before adding it to the request. Currently only HTTP GET is supported. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding.")
    private String userData;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="availability zone for the virtual machine")
    private Long zoneId;

    // unexposed parameter needed for serializing/deserializing the command
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, expose=false)
    private String password;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getGroup() {
        return group;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public List<String> getNetworkGroupList() {
        return networkGroupList;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getSize() {
        return size;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getUserData() {
        return userData;
    }

    public Long getZoneId() {
        return zoneId;
    }

    // not exposed parameter
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    
    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
        return "virtualmachine";
    }
    
    @Override
    public long getAccountId() {
        Account account = UserContext.current().getAccount();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = ApiDBUtils.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "deploying Vm";
    }

    @Override @SuppressWarnings("unchecked")
    public UserVmResponse getResponse() {
        UserVm userVm = (UserVm)getResponseObject();
        UserVmResponse response = ApiResponseHelper.createUserVmResponse(userVm);
        
        // FIXME:  where will the password come from in this case?
//        if (templatePasswordEnabled) { 
//            response.setPassword(getPassword());
//        } 
        response.setResponseName(getName());
        return response;
    }
    
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException, StorageUnavailableException{
        try {
            UserVm result = _mgr.deployVirtualMachine(this);
            return result;
        } catch (ResourceAllocationException ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        } catch (ExecutionException ex1) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex1.getMessage());
        }
    }
}
