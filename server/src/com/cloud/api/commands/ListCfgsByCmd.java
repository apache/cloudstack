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
import com.cloud.configuration.ConfigurationVO;
import com.cloud.server.Criteria;
import com.cloud.utils.Pair;

public class ListCfgsByCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListCfgsByCmd.class.getName());

    private static final String s_name = "listconfigurationsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CATEGORY, Boolean.FALSE));
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
        String name = (String) params.get(BaseCmd.Properties.NAME.getName());
        String category = (String) params.get(BaseCmd.Properties.CATEGORY.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
    	Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        
    	Long startIndex = Long.valueOf(0);
        int pageSizeNum = 100;
    	if (pageSize != null) {
    		pageSizeNum = pageSize.intValue();
    	}
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }
        
        Criteria c = new Criteria ("name", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        if (keyword != null) {
        	c.addCriteria(Criteria.KEYWORD, keyword);
        } else {
        	c.addCriteria(Criteria.NAME, name);
    		c.addCriteria(Criteria.CATEGORY, category);
        }
		
    	List<ConfigurationVO> configs = getManagementServer().searchForConfigurations(c, false);
        
        if (configs == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to find configuration values for specified search criteria.");
        }
        
        List<Pair<String, Object>> cfgTags = new ArrayList<Pair<String, Object>>();
        Object[] cfgDataTags = new Object[configs.size()];
        
        int i = 0;
        for (ConfigurationVO config : configs) {
            List<Pair<String, Object>> cfgData = new ArrayList<Pair<String, Object>>();
            cfgData.add(new Pair<String, Object>(BaseCmd.Properties.CATEGORY.getName(), config.getCategory()));
            cfgData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), config.getName()));
            cfgData.add(new Pair<String, Object>(BaseCmd.Properties.VALUE.getName(), config.getValue()));
            cfgData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), config.getDescription()));
            cfgDataTags[i++] = cfgData;
        }
        Pair<String, Object> cfgTag = new Pair<String, Object>("configuration", cfgDataTags);
        cfgTags.add(cfgTag);
        return cfgTags;
    }
}
