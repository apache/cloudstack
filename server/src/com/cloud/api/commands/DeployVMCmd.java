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
import com.cloud.async.executor.DeployVMResultObject;
import com.cloud.dc.DataCenterVO;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class DeployVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeployVMCmd.class.getName());
    
    private static final String s_name = "deployvirtualmachineresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SERVICE_OFFERING_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISK_OFFERING_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TEMPLATE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISPLAY_NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.GROUP, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_DATA, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NETWORK_GROUP_LIST, Boolean.FALSE));

    }

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "virtualmachine";
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        Long serviceOfferingId = (Long)params.get(BaseCmd.Properties.SERVICE_OFFERING_ID.getName());
        Long diskOfferingId = (Long)params.get(BaseCmd.Properties.DISK_OFFERING_ID.getName());
        Long templateId = (Long)params.get(BaseCmd.Properties.TEMPLATE_ID.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        String displayName = (String)params.get(BaseCmd.Properties.DISPLAY_NAME.getName());
        String group = (String)params.get(BaseCmd.Properties.GROUP.getName());
        String userData = (String) params.get(BaseCmd.Properties.USER_DATA.getName());
        String networkGroupList = (String)params.get(BaseCmd.Properties.NETWORK_GROUP_LIST.getName());

        String password = null;
        Long accountId = null;

        VMTemplateVO template = getManagementServer().findTemplateById(templateId);
        if (template == null) {
            throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "Unable to find template with id " + templateId);
        }

    	if (diskOfferingId != null) {
    	    DiskOfferingVO diskOffering = getManagementServer().findDiskOfferingById(diskOfferingId);
    	    if ((diskOffering == null) || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
                throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "Disk offering with id " + diskOfferingId + " doesn't exist in the system");
    	    }
    	}

        DataCenterVO zone = getManagementServer().findDataCenterById(zoneId);
        if (zone == null) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "Zone with id " + zoneId + " doesn't exist in the system");
        }

        ServiceOfferingVO serviceOffering = getManagementServer().findServiceOfferingById(serviceOfferingId);
        if (serviceOffering == null ) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "Service offering with id " + serviceOfferingId + " doesn't exist in the system");
        }

        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") ");
                }
                if (accountName != null) {
                    Account userAccount = getManagementServer().findActiveAccount(accountName, domainId);
                    if (userAccount == null) {
                        throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = userAccount.getId();
                }
            } else {
                accountId = ((account != null) ? account.getId() : null);
            }
        } else {
            accountId = account.getId();
        }

        if (accountId == null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "No valid account specified for deploying a virtual machine.");
        }
    	String [] groups = null;
        if (networkGroupList != null) {
        	groups = networkGroupList.split(",");
        	for (String groupName: groups) {
        		NetworkGroupVO groupVO = getManagementServer().findNetworkGroupByName(accountId, groupName);
        		if (groupVO == null) {
        			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Network group not found: " + groupName);
        		}
        	}
        }

    	// If command is executed via 8096 port, set userId to the id of System account (1)
    	if (userId == null) {
            userId = Long.valueOf(1);
        }

    	password = getManagementServer().generateRandomPassword();

    	ManagementServer mgr = getManagementServer();

    	try {
    		long jobId = mgr.deployVirtualMachineAsync(userId.longValue(), accountId.longValue(), zoneId.longValue(),
    				serviceOfferingId.longValue(),
    				templateId.longValue(), diskOfferingId, 
    				null, password, displayName, group, userData, groups);

    		long vmId = 0;
    		if (jobId == 0) {
    			s_logger.warn("Unable to schedule async-job for DeployVMAsync comamnd");
    		} else {
    			if (s_logger.isDebugEnabled())
    				s_logger.debug("DeployVMAsync command has been accepted, job id: " + jobId);

    			vmId = waitInstanceCreation(jobId);
    		}

    		List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
    		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName(), Long.valueOf(vmId)));
    		returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId)));

    		return returnValues;
    	} catch (Exception ex) {
    		s_logger.error("Unhandled exception, ", ex);
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create VM due to unhandled exception");
    	}
    }

	protected long getInstanceIdFromJobSuccessResult(String result) {
		DeployVMResultObject resultObject = (DeployVMResultObject)SerializerHelper.fromSerializedString(result);
		if (resultObject != null) {
			return resultObject.getId();
		}

		return 0;
	}
}
