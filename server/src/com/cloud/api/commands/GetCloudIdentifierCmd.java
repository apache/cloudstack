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
import com.cloud.utils.Pair;

public class GetCloudIdentifierCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GetCloudIdentifierCmd.class.getName());

    private static final String s_name = "getcloudidentifierresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());

        ArrayList<String> signedResponse = getManagementServer().getCloudIdentifierResponse(userId);
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CLOUD_IDENTIFIER.getName(),signedResponse.get(0)));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SIGNATURE.getName(),signedResponse.get(1)));
        return returnValues;
    }
    

}