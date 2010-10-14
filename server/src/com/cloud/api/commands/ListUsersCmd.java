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
import com.cloud.domain.DomainVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.user.UserAccountVO;
import com.cloud.utils.Pair;

public class ListUsersCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListUsersCmd.class.getName());

    private static final String s_name = "listusersresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USERNAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_TYPE, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STATE, Boolean.FALSE));
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
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
    	String userName = (String)params.get(BaseCmd.Properties.USERNAME.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Long type = (Long)params.get(BaseCmd.Properties.ACCOUNT_TYPE.getName());
        String state = (String)params.get(BaseCmd.Properties.STATE.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());

        if (domainId != null) {
            if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list users.");
            }
        } else {
            // default domainId to the admin's domain
            domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
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
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.ACCOUNTNAME, accountName);
        c.addCriteria(Criteria.DOMAINID, domainId);
        c.addCriteria(Criteria.USERNAME, userName);
        c.addCriteria(Criteria.TYPE, type);
        c.addCriteria(Criteria.STATE, state);

        List<UserAccountVO> users = getManagementServer().searchForUsers(c);
        
        List<Pair<String, Object>> userTags = new ArrayList<Pair<String, Object>>();
        Object[] uTag = new Object[users.size()];
        int i = 0;
        for (UserAccountVO user: users) {
        	if ((user.getRemoved() == null)&&(user.getId() != 1)) {
        		List<Pair<String, Object>> userData = new ArrayList<Pair<String, Object>>();
                userData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.valueOf(user.getId()).toString()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.USERNAME.getName(), user.getUsername()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.FIRSTNAME.getName(), user.getFirstname()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.LASTNAME.getName(), user.getLastname()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.EMAIL.getName(), user.getEmail())); 
                userData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(user.getCreated())));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), user.getState()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), getManagementServer().findAccountById(user.getAccountId()).getAccountName()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT_TYPE.getName(), getManagementServer().findAccountById(user.getAccountId()).getType()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), user.getDomainId().toString()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(user.getDomainId()).getName()));
                userData.add(new Pair<String, Object>(BaseCmd.Properties.TIMEZONE.getName(),user.getTimezone()));
                if(user.getApiKey()!=null)
                	userData.add(new Pair<String, Object>(BaseCmd.Properties.API_KEY.getName(), user.getApiKey()));
                if(user.getSecretKey()!=null)
                	userData.add(new Pair<String, Object>(BaseCmd.Properties.SECRET_KEY.getName(), user.getSecretKey()));
                uTag[i++] = userData;
        	}
        }
        Pair<String, Object> userTag = new Pair<String, Object>("user", uTag);
        userTags.add(userTag);
        return userTags;
    	
    }
}
