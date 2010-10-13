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
import com.cloud.server.Criteria;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.utils.Pair;

public class ListDiskOfferingsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListDiskOfferingsCmd.class.getName());

    private static final String s_name = "listdiskofferingsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
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
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
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

        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.DOMAINID, domainId);

        List<DiskOfferingVO> offerings = getManagementServer().searchForDiskOfferings(c);

        List<Pair<String, Object>> offeringTags = new ArrayList<Pair<String, Object>>();
        Object[] diskOffTag = new Object[offerings.size()];
        int i = 0;
        for (DiskOfferingVO offering : offerings) {
            List<Pair<String, Object>> offeringData = new ArrayList<Pair<String, Object>>();

            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), offering.getId().toString()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), offering.getDomainId()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(offering.getDomainId()).getName()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), offering.getName()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), offering.getDisplayText()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE.getName(), offering.getDiskSizeInBytes()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.IS_MIRRORED.getName(), offering.isMirrored()));
            offeringData.add(new Pair<String, Object>(BaseCmd.Properties.TAGS.getName(), offering.getTags()));
            diskOffTag[i++] = offeringData;
        }
        Pair<String, Object> offeringTag = new Pair<String, Object>("diskoffering", diskOffTag);
        offeringTags.add(offeringTag);
        return offeringTags;
    }
}
