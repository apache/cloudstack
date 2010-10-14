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
import com.cloud.utils.Pair;

public class ListDomainsCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(ListDomainsCmd.class.getName());
	
    private static final String s_name = "listdomainsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_LEVEL, Boolean.FALSE));
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
    	Long domainId = (Long)params.get(BaseCmd.Properties.ID.getName());
        String domainName = (String)params.get(BaseCmd.Properties.NAME.getName());
        Integer level = (Integer)params.get(BaseCmd.Properties.DOMAIN_LEVEL.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());

        if (account != null) {
            if (domainId != null) {
                if (!getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to list domains for domain id " + domainId + ", permission denied.");
                }
            } else {
                domainId = account.getDomainId();
            }
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
        
        //temporary solution at API level. We need a permanent solution for all "listXXXXXXX & pageSize = -1" in the future.
        Criteria c;
        if(pageSizeNum != -1)
            c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        else
        	c = new Criteria("id", Boolean.TRUE, null, null);
        
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.ID, domainId);
        c.addCriteria(Criteria.NAME, domainName);
        c.addCriteria(Criteria.LEVEL, level);
        
        List<DomainVO> domains = getManagementServer().searchForDomains(c);
        
        List<Pair<String, Object>> domainTags = new ArrayList<Pair<String, Object>>();
        Object[] dTag = new Object[domains.size()];
        int i = 0;
        for (DomainVO domain : domains) {
            List<Pair<String, Object>> domainData = new ArrayList<Pair<String, Object>>();
            domainData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.valueOf(domain.getId()).toString()));
            domainData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), domain.getName()));
            domainData.add(new Pair<String, Object>(BaseCmd.Properties.LEVEL.getName(), domain.getLevel().toString()));
            
            if (domain.getParent() != null){
            	domainData.add(new Pair<String, Object>(BaseCmd.Properties.PARENT_DOMAIN_ID.getName(), domain.getParent().toString()));
            	domainData.add(new Pair<String, Object>(BaseCmd.Properties.PARENT_DOMAIN_NAME.getName(), 
            			getManagementServer().findDomainIdById(domain.getParent()).getName()));
            }
            dTag[i++] = domainData;
        }
        Pair<String, Object> domainTag = new Pair<String, Object>("domain", dTag);
        domainTags.add(domainTag);
        return domainTags;
    }
}
