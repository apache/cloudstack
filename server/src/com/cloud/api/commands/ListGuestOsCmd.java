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
import com.cloud.server.Criteria;
import com.cloud.storage.GuestOSVO;
import com.cloud.utils.Pair;

public class ListGuestOsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListIsosCmd.class.getName());

    private static final String s_name = "listostypesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.OS_CATEGORY_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }
    @Override
    public String getName() {
        return s_name;
    }
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        
        List<GuestOSVO> guestOSList = null;
        try 
        {
        	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        	Long osCategoryId = (Long)params.get(BaseCmd.Properties.OS_CATEGORY_ID.getName());
            Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
            Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
            
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
            
            if (id != null) {
            	c.addCriteria(Criteria.ID, id);
            }
            
            if (osCategoryId != null) {
            	c.addCriteria(Criteria.OSCATEGORYID, osCategoryId);
            }
            guestOSList = getManagementServer().listGuestOSByCriteria(c);
        } 
        catch (Exception ex) 
        {
            s_logger.error("Exception listing guest OS", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to list guest OS due to exception: " + ex.getMessage());
        }
        
        Object[] tag = null;
        List<Pair<String, Object>> guestOSTags = new ArrayList<Pair<String, Object>>();
        if (guestOSList != null) {
	        tag = new Object[guestOSList.size()];
	        int i = 0;
	        for (GuestOSVO guestOS : guestOSList) {
	            List<Pair<String, Object>> guestOSData = new ArrayList<Pair<String, Object>>();
	            guestOSData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), guestOS.getId().toString()));
	            guestOSData.add(new Pair<String, Object>(BaseCmd.Properties.OS_CATEGORY_ID.getName(), guestOS.getCategoryId()));
	            guestOSData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), guestOS.getDisplayName()));
	
	            tag[i++] = guestOSData;
	        }
        } else {
        	tag = new Object[0];
        }
        Pair<String, Object> guestOSTag = new Pair<String, Object>("ostype", tag);
        guestOSTags.add(guestOSTag);
        return guestOSTags;
    }
}
