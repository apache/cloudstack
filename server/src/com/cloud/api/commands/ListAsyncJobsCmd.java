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
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.async.AsyncJobVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListAsyncJobsCmd extends BaseCmd {
    private static final String s_name = "listasyncjobsresponse";
    
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.START_TZDATE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }
    
    public String getName() {
        return s_name;
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }
    
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
    	Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        Long accountId = (Long)params.get(BaseCmd.Properties.ACCOUNT_ID.getName());
        Date startDate = (Date)params.get(BaseCmd.Properties.START_TZDATE.getName());
        
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
        if(account == null) {
        	if(accountId != null)
        		c.addCriteria(Criteria.ACCOUNTID, accountId.longValue());
        } else {
        	if(account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
        		// for root admins, can take arbitrary account id from input
            	if(accountId != null)
            		c.addCriteria(Criteria.ACCOUNTID, accountId.longValue());
        	} else {
        		// for normal accounts, they can only query jobs of their own
            	c.addCriteria(Criteria.ACCOUNTID, account.getId());
        	}
        }
        
        if(startDate != null) {
        	c.addCriteria(Criteria.STARTDATE, startDate);
        }
        
   	 	List<AsyncJobVO> jobs = getManagementServer().searchForAsyncJobs(c);
	    if (jobs == null) {
	    	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find async jobs");
	    }
	    
        List<Pair<String, Object>> jobTags = new ArrayList<Pair<String, Object>>();
        Object[] sTag = new Object[jobs.size()];
        int i = 0;
        for(AsyncJobVO job : jobs) {
            List<Pair<String, Object>> jobData = new ArrayList<Pair<String, Object>>();
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), job.getId().toString()));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT_ID.getName(), String.valueOf(job.getAccountId())));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.USER_ID.getName(), String.valueOf(job.getUserId())));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.CMD.getName(), job.getCmd()));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(job.getStatus())));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_PROCESS_STATUS.getName(), String.valueOf(job.getProcessStatus())));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_RESULT_CODE.getName(), String.valueOf(job.getResultCode())));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_RESULT.getName(), job.getResult()));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_INSTANCE_TYPE.getName(), job.getInstanceType()));
            jobData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_INSTANCE_ID.getName(), String.valueOf(job.getInstanceId())));
       	 	jobData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(job.getCreated())));
        
            sTag[i++] = jobData;
        }
        
        Pair<String, Object> jobTag = new Pair<String, Object>("asyncjobs", sTag);
        jobTags.add(jobTag);
        
        return jobTags;
    }
}
