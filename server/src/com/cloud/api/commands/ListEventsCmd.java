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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.domain.DomainVO;
import com.cloud.event.EventVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class ListEventsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListEventsCmd.class.getName());

    private static final String s_name = "listeventsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TYPE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.LEVEL, Boolean.FALSE));
//        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DESCRIPTION, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.START_DATE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.END_DATE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ENTRY_TIME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DURATION, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
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
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String eventType = (String)params.get(BaseCmd.Properties.TYPE.getName());
        String eventLevel = (String)params.get(BaseCmd.Properties.LEVEL.getName());
//        String eventLevel = (String)params.get(BaseCmd.Properties.DESCRIPTION.getName());
        Date startDate = (Date)params.get(BaseCmd.Properties.START_DATE.getName());
        Date endDate = (Date)params.get(BaseCmd.Properties.END_DATE.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer entryTime = (Integer)params.get(BaseCmd.Properties.ENTRY_TIME.getName());
        Integer duration = (Integer)params.get(BaseCmd.Properties.DURATION.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        boolean isAdmin = false;

        Long[] accountIds = null;
        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
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
        }

        if (accountId != null) {
            accountIds = new Long[1];
            accountIds[0] = accountId;
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
        Criteria c = new Criteria("createDate", Boolean.FALSE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.ACCOUNTID, accountIds);
        if (keyword != null) {
        	c.addCriteria(Criteria.KEYWORD, keyword);
        } else {
            if (isAdmin) {
                c.addCriteria(Criteria.DOMAINID, domainId);
            }
        	c.addCriteria(Criteria.TYPE, eventType);
    		c.addCriteria(Criteria.LEVEL, eventLevel);
    		c.addCriteria(Criteria.STARTDATE, startDate);
    		c.addCriteria(Criteria.ENDDATE, endDate);
        }

        List<EventVO> events;
        
        if(entryTime != null && duration != null){
            if(entryTime <= duration){
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Entry time shoule be greater than duration");
            }
            events = getManagementServer().listPendingEvents(entryTime, duration);
        }
        else {
            events = getManagementServer().searchForEvents(c);
        }

        if (events == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error searching for events");
        }
        List<Pair<String, Object>> eventTags = new ArrayList<Pair<String, Object>>();
        Object[] eTag = new Object[events.size()];
        int i = 0;
        for (EventVO event : events) {
            List<Pair<String, Object>> eventData = new ArrayList<Pair<String, Object>>();
            eventData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), (new Long(event.getId())).toString()));
            User user = getManagementServer().findUserById(event.getUserId());
            if (user != null) {
            	eventData.add(new Pair<String, Object>(BaseCmd.Properties.USERNAME.getName(), user.getUsername() ));
            }
            eventData.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), event.getType()));
            eventData.add(new Pair<String, Object>(BaseCmd.Properties.LEVEL.getName(), event.getLevel()));
            eventData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), event.getDescription()));
            Account acct = getManagementServer().findAccountById(Long.valueOf(event.getAccountId()));
            if (acct != null) {
            	eventData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), acct.getAccountName()));
            	eventData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), acct.getDomainId().toString()));
            	eventData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(acct.getDomainId()).getName()));
            }
            if (event.getCreateDate() != null) {
                eventData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(event.getCreateDate())));
            }
            eventData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), event.getState().toString()));
            eventData.add(new Pair<String, Object>(BaseCmd.Properties.PARENT_ID.getName(), (new Long(event.getStartId())).toString()));
            eTag[i++] = eventData;
        }
        Pair<String, Object> eventTag = new Pair<String, Object>("event", eTag);
        eventTags.add(eventTag);
        return eventTags;
    }
}
