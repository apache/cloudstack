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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.utils.Pair;

public class CreateDiskOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDiskOfferingCmd.class.getName());

    private static final String s_name = "creatediskofferingresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISPLAY_TEXT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISK_SIZE, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_MIRRORED, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TAGS, Boolean.FALSE));
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
        // FIXME: add domain-private disk offerings
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        String displayText = (String)params.get(BaseCmd.Properties.DISPLAY_TEXT.getName());
        Long numGB = (Long) params.get(BaseCmd.Properties.DISK_SIZE.getName());
        Boolean isMirrored = (Boolean)params.get(BaseCmd.Properties.IS_MIRRORED.getName());
        String tags = (String)params.get(BaseCmd.Properties.TAGS.getName());

        if (isMirrored == null) {
            isMirrored = Boolean.FALSE;
        }
        if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN;
        }

        DiskOfferingVO diskOffering = null;
        try {
        	diskOffering = getManagementServer().createDiskOffering(domainId.longValue(), name, displayText, numGB.intValue(), isMirrored.booleanValue(), tags);
        } catch (InvalidParameterValueException ex) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, ex.getMessage());
        }
        
        if (diskOffering == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create disk offering");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), diskOffering.getId().toString()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), diskOffering.getDomainId()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(diskOffering.getDomainId()).getName()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), diskOffering.getName()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), diskOffering.getDisplayText()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE.getName(), diskOffering.getDiskSizeInBytes()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.IS_MIRRORED.getName(), diskOffering.isMirrored()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), diskOffering.getCreated()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TAGS.getName(), diskOffering.getTags()));
        return returnValues;
    }
}
