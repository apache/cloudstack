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
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class RegisterCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RegisterCmd.class.getName());

    private static final String s_name = "registeruserkeysresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.ID.getName());

        User user = getManagementServer().findUserById(userId);

        if (user == null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to find user for id : " + userId);
        }
        
        // generate both an api key and a secret key, update the user table with the keys, return the keys to the user
        String apiKey = getManagementServer().createApiKey(user.getId());
        String secretKey = getManagementServer().createSecretKey(user.getId());

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();

        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.API_KEY.getName(), apiKey));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SECRET_KEY.getName(), secretKey));
        return returnValues;
    }
}
