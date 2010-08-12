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
import com.cloud.dc.DataCenterVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

//FIXME: consolidate this class and ListDataCentersByCmd
public class ListZonesByCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListZonesByCmd.class.getName());

    private static final String s_name = "listzonesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.AVAILABLE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
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
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Boolean available = (Boolean)params.get(BaseCmd.Properties.AVAILABLE.getName());
        
        List<DataCenterVO> dataCenters = null;
        if (account != null) {
        	if (available != null && available) {
        		dataCenters = getManagementServer().listDataCenters();
        	} else {
        		dataCenters = getManagementServer().listDataCentersBy(account.getId().longValue());
        	}
        } else {
        	// available is kinda useless in this case because we can't exactly list by
        	// accountId if we don't have one.  In this case, we just assume the user
        	// wants all the zones.
            dataCenters = getManagementServer().listDataCenters();
        }

        if (dataCenters == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find zones for account " + account.getAccountName());
        }
        List<Pair<String, Object>> dcTags = new ArrayList<Pair<String, Object>>();
        Object[] dcInstTag = new Object[dataCenters.size()];
        int i = 0;
        for (DataCenterVO dataCenter : dataCenters) {
            List<Pair<String, Object>> dcData = new ArrayList<Pair<String, Object>>();
            if (dataCenter.getId() != null) {
                dcData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), dataCenter.getId().toString()));
            }
            dcData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), dataCenter.getName()));
            if ((dataCenter.getDescription() != null) && !dataCenter.getDescription().equalsIgnoreCase("null")) {
                dcData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), dataCenter.getDescription()));
            }
            if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
            	if (dataCenter.getDns1() != null) {
            		dcData.add(new Pair<String, Object>(BaseCmd.Properties.DNS1.getName(), dataCenter.getDns1()));
            	}
            	if (dataCenter.getDns2() != null) {
            		dcData.add(new Pair<String, Object>(BaseCmd.Properties.DNS2.getName(), dataCenter.getDns2()));
            	}
                if (dataCenter.getInternalDns1() != null) {
                    dcData.add(new Pair<String, Object>(BaseCmd.Properties.INTERNAL_DNS1.getName(), dataCenter.getInternalDns1()));
                }
                if (dataCenter.getInternalDns2() != null) {
                    dcData.add(new Pair<String, Object>(BaseCmd.Properties.INTERNAL_DNS2.getName(), dataCenter.getInternalDns2()));
                }
                if (dataCenter.getVnet() != null) {
                    dcData.add(new Pair<String, Object>("vlan", dataCenter.getVnet()));
                }
                if (dataCenter.getGuestNetworkCidr() != null) {
            		dcData.add(new Pair<String, Object>(BaseCmd.Properties.GUEST_CIDR_ADDRESS.getName(), dataCenter.getGuestNetworkCidr()));
            	}
            }

            dcInstTag[i++] = dcData;
        }
        Pair<String, Object> dcTag = new Pair<String, Object>("zone", dcInstTag);
        dcTags.add(dcTag);
        return dcTags;
    }
}
