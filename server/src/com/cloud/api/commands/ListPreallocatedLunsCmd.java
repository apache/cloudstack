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
import com.cloud.dc.ClusterVO;
import com.cloud.server.Criteria;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.utils.Pair;

public class ListPreallocatedLunsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListPreallocatedLunsCmd.class.getName());

    private static final String s_name = "listpreallocatedlunsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TARGET_IQN, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SCOPE, Boolean.FALSE));
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
        String targetIqn = (String)params.get(BaseCmd.Properties.TARGET_IQN.getName());
        String scope = (String)params.get(BaseCmd.Properties.SCOPE.getName());
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
        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        if (targetIqn != null) {
            c.addCriteria(Criteria.TARGET_IQN, targetIqn);
        }         
        if(scope != null){
        	c.addCriteria(Criteria.SCOPE, scope);
        }
        
        List<PreallocatedLunVO> preAllocatedLuns = getManagementServer().getPreAllocatedLuns(c);
        
        if(preAllocatedLuns == null)
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find pre allocated luns");
        
        List<Pair<String, Object>> preAllocatedLunTags = new ArrayList<Pair<String, Object>>();
        Object[] sTag = new Object[preAllocatedLuns.size()];
        int i = 0;
        for (PreallocatedLunVO lun : preAllocatedLuns) 
        {
            List<Pair<String, Object>> lunData = new ArrayList<Pair<String, Object>>();
            
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), lun.getId()));
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), lun.getVolumeId()));
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), lun.getDataCenterId()));
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.LUN.getName(), lun.getLun()));
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.PORTAL.getName(), lun.getPortal()));
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.SIZE.getName(), lun.getSize()));
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.TAKEN.getName(), lun.getTaken()));
            lunData.add(new Pair<String, Object>(BaseCmd.Properties.TARGET_IQN.getName(), lun.getTargetIqn()));
            
            sTag[i++] = lunData;
        }
        Pair<String, Object> lunTag = new Pair<String, Object>("preallocatedlun", sTag);
        preAllocatedLunTags.add(lunTag);
        return preAllocatedLunTags;

        
    }
    

}