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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.domain.DomainVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.Pair;
import com.cloud.vm.State;
import com.cloud.vm.UserVm;

public class ListAccountsCmd extends BaseCmd{
	public static final Logger s_logger = Logger.getLogger(ListAccountsCmd.class.getName());
    private static final String s_name = "listaccountsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_TYPE, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STATE, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_CLEANUP_REQUIRED, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Long type = (Long)params.get(BaseCmd.Properties.ACCOUNT_TYPE.getName());
        String state = (String)params.get(BaseCmd.Properties.STATE.getName()); 
        Boolean needCleanup = (Boolean)params.get(BaseCmd.Properties.IS_CLEANUP_REQUIRED.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        boolean isAdmin = false;
		Long accountId = null;

        String accountName = null;

        if ((account == null) || isAdmin(account.getType())) {
        	accountName = (String)params.get(BaseCmd.Properties.NAME.getName());
        	isAdmin = true;
        	if (domainId == null) {
                // default domainId to the admin's domain
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
        	} else if (account != null) {
        	    if (!getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
        	        throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list accounts");
        	    }
        	}
        } else {
        	accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        	accountId = account.getId();
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
        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
		if (isAdmin == true) {
			c.addCriteria(Criteria.ID, id);
			c.addCriteria(Criteria.ACCOUNTNAME, accountName);
			c.addCriteria(Criteria.DOMAINID, domainId);
			c.addCriteria(Criteria.TYPE, type);
			c.addCriteria(Criteria.STATE, state);
			c.addCriteria(Criteria.ISCLEANUPREQUIRED, needCleanup);
			c.addCriteria(Criteria.KEYWORD, keyword);
		} else {
			c.addCriteria(Criteria.ID, accountId);
		}

        List<AccountVO> accounts = getManagementServer().searchForAccounts(c);

        List<Pair<String, Object>> accountTags = new ArrayList<Pair<String, Object>>();
        Object[] aTag = new Object[accounts.size()];
        int i = 0;
        for (AccountVO accountO : accounts) {
        	boolean accountIsAdmin = (accountO.getType() == Account.ACCOUNT_TYPE_ADMIN);
        	
    		List<Pair<String, Object>> accountData = new ArrayList<Pair<String, Object>>();
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.valueOf(accountO.getId()).toString()));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), accountO.getAccountName()));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT_TYPE.getName(), Short.valueOf(accountO.getType()).toString()));
            DomainVO domain = getManagementServer().findDomainIdById(accountO.getDomainId());
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), domain.getId().toString()));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), domain.getName()));

            //get network stat
            List<UserStatisticsVO> stats = getManagementServer().listUserStatsBy(accountO.getId());
            if (stats == null) {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error searching for user stats");
            }

            long bytesSent = 0;
            long bytesReceived = 0;
            for (UserStatisticsVO stat : stats) {
            	long rx = stat.getNetBytesReceived() + stat.getCurrentBytesReceived();
    			long tx = stat.getNetBytesSent() + stat.getCurrentBytesSent();
                bytesReceived = bytesReceived + Long.valueOf(rx);
                bytesSent = bytesSent + Long.valueOf(tx);
            }
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.BYTES_RECEIVED.getName(), Long.valueOf(bytesReceived).toString()));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.BYTES_SENT.getName(), Long.valueOf(bytesSent).toString()));

            // Get resource limits and counts
            
            long vmLimit = getManagementServer().findCorrectResourceLimit(ResourceType.user_vm, accountO.getId());
            String vmLimitDisplay = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
            long vmTotal = getManagementServer().getResourceCount(ResourceType.user_vm, accountO.getId());
            String vmAvail = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.VM_LIMIT.getName(), vmLimitDisplay));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.VM_TOTAL.getName(), vmTotal));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.VM_AVAIL.getName(), vmAvail));
            
            long ipLimit = getManagementServer().findCorrectResourceLimit(ResourceType.public_ip, accountO.getId());
            String ipLimitDisplay = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
            long ipTotal = getManagementServer().getResourceCount(ResourceType.public_ip, accountO.getId());
            String ipAvail = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit - ipTotal);
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.IP_LIMIT.getName(), ipLimitDisplay));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.IP_TOTAL.getName(), ipTotal));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.IP_AVAIL.getName(), ipAvail));
            
            long volumeLimit = getManagementServer().findCorrectResourceLimit(ResourceType.volume, accountO.getId());
            String volumeLimitDisplay = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
            long volumeTotal = getManagementServer().getResourceCount(ResourceType.volume, accountO.getId());
            String volumeAvail = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_LIMIT.getName(), volumeLimitDisplay));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_TOTAL.getName(), volumeTotal));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_AVAIL.getName(), volumeAvail));
            
            long snapshotLimit = getManagementServer().findCorrectResourceLimit(ResourceType.snapshot, accountO.getId());
            String snapshotLimitDisplay = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
            long snapshotTotal = getManagementServer().getResourceCount(ResourceType.snapshot, accountO.getId());
            String snapshotAvail = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.SNAPSHOT_LIMIT.getName(), snapshotLimitDisplay));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.SNAPSHOT_TOTAL.getName(), snapshotTotal));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.SNAPSHOT_AVAIL.getName(), snapshotAvail));
            
            long templateLimit = getManagementServer().findCorrectResourceLimit(ResourceType.template, accountO.getId());
            String templateLimitDisplay = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
            long templateTotal = getManagementServer().getResourceCount(ResourceType.template, accountO.getId());
            String templateAvail = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_LIMIT.getName(), templateLimitDisplay));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_TOTAL.getName(), templateTotal));
            accountData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_AVAIL.getName(), templateAvail));
            
    	    // Get stopped and running VMs

    	    int vmStopped = 0;
    	    int vmRunning = 0;

    	    Long[] accountIds = new Long[1];
    	    accountIds[0] = accountO.getId();

    	    Criteria c1 = new Criteria();
    	    c1.addCriteria(Criteria.ACCOUNTID, accountIds);
    	    List<? extends UserVm> virtualMachines = getManagementServer().searchForUserVMs(c1);

    	    //get Running/Stopped VMs
    	    for (Iterator<? extends UserVm> iter = virtualMachines.iterator(); iter.hasNext();) {
    	    	// count how many stopped/running vms we have
    		    UserVm vm = iter.next();

    		  	if (vm.getState() == State.Stopped) {
    		  		vmStopped++;
    		  	} else if (vm.getState() == State.Running) {
    		  		vmRunning++;
    		  	}
    	    }

    	    accountData.add(new Pair<String, Object>(BaseCmd.Properties.VM_STOPPED.getName(), vmStopped));
    	    accountData.add(new Pair<String, Object>(BaseCmd.Properties.VM_RUNNING.getName(), vmRunning));

    	    //show this info to admins only
    	    if (isAdmin == true) {
    	    	accountData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), accountO.getState()));
                accountData.add(new Pair<String, Object>(BaseCmd.Properties.IS_CLEANUP_REQUIRED.getName(), Boolean.valueOf(accountO.getNeedsCleanup()).toString()));
    	    }

            aTag[i++] = accountData;
        }
        Pair<String, Object> accountTag = new Pair<String, Object>("account", aTag);
        accountTags.add(accountTag);
        return accountTags;
    }
}
