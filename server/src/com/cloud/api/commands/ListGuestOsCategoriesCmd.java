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
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.utils.Pair;

public class ListGuestOsCategoriesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListIsosCmd.class.getName());

    private static final String s_name = "listoscategoriesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
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
        
        List<GuestOSCategoryVO> guestOSCategoryList = null;
        try {
        	guestOSCategoryList = getManagementServer().listAllGuestOSCategories();
        } catch (Exception ex) {
            s_logger.error("Exception listing guest OS categories", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to list guest OS categories due to exception: " + ex.getMessage());
        }
        
        Object[] tag = null;
        List<Pair<String, Object>> guestOSCategoryTags = new ArrayList<Pair<String, Object>>();
        if (guestOSCategoryList != null) {
	        tag = new Object[guestOSCategoryList.size()];
	        int i = 0;
	        for (GuestOSCategoryVO guestOSCategory : guestOSCategoryList) {
	            List<Pair<String, Object>> guestOSCategoryData = new ArrayList<Pair<String, Object>>();
	            guestOSCategoryData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), guestOSCategory.getId().toString()));
	            guestOSCategoryData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), guestOSCategory.getName()));
	
	            tag[i++] = guestOSCategoryData;
	        }
        } else {
        	tag = new Object[0];
        }
        Pair<String, Object> guestOSCategoryTag = new Pair<String, Object>("oscategory", tag);
        guestOSCategoryTags.add(guestOSCategoryTag);
        return guestOSCategoryTags;
    }
}
