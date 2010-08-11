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

import com.cloud.alert.AlertVO;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.server.Criteria;
import com.cloud.utils.Pair;

public class ListAlertsCmd extends BaseCmd{
	
	   public static final Logger s_logger = Logger.getLogger(ListAlertsCmd.class.getName());

	    private static final String s_name = "listalertsresponse";
	    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
	    
	    public String getName() {
	        return s_name;
	    }
	    public List<Pair<Enum, Boolean>> getProperties() {
	        return s_properties;
	    }
	    
	    static {
	    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TYPE, Boolean.FALSE));
	    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
	    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
	    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
	    }

	    
	    @Override
	    public List<Pair<String, Object>> execute(Map<String, Object> params) {
	    	String alertType = (String)params.get(BaseCmd.Properties.TYPE.getName());
	    	String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
	    	Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
	    	Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
	    	
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
	        
	        Criteria c = new Criteria ("lastSent", Boolean.FALSE, startIndex, Long.valueOf(pageSizeNum));
	        c.addCriteria(Criteria.KEYWORD, keyword);
	        
	        if (keyword == null)
	        	c.addCriteria(Criteria.TYPE, alertType);
	        
	        List<AlertVO> alerts = getManagementServer().searchForAlerts(c);
	        
	        if (alerts == null ) {
	        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find alerts");
	        }
	        
	        List<Pair<String, Object>> alertsTags = new ArrayList<Pair<String, Object>>();
	        Object[] aTag = new Object[alerts.size()];
	        int i=0;
	        
	        for (AlertVO alert : alerts) {
	        	List<Pair<String, Object>> alertData = new ArrayList<Pair<String, Object>>();
	        	alertData.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), alert.getType()));
	        	alertData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), alert.getSubject()));
	            alertData.add(new Pair<String, Object>(BaseCmd.Properties.SENT.getName(), alert.getLastSent()));
	            aTag[i++] = alertData;
	        }
	        
	        Pair<String, Object> alertTag = new Pair<String, Object>("alert", aTag);
	        alertsTags.add(alertTag);
	        return alertsTags;

	    }
}
