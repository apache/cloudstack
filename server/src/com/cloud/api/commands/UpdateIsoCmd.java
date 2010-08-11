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
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class UpdateIsoCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIsoCmd.class.getName());

    private static final String s_name = "updateisoresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISPLAY_TEXT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.OS_TYPE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.BOOTABLE, Boolean.FALSE));
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
        String displayText = (String)params.get(BaseCmd.Properties.DISPLAY_TEXT.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        Long isoId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long guestOSId = (Long) params.get(BaseCmd.Properties.OS_TYPE_ID.getName());
        Boolean bootable = (Boolean) params.get(BaseCmd.Properties.BOOTABLE.getName());

        VMTemplateVO iso = getManagementServer().findTemplateById(isoId.longValue());
        if ((iso == null) || iso.getFormat() != Storage.ImageFormat.ISO) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find ISO with id " + isoId);
        }

        // do a permission check
        if (account != null) {
            Long isoOwner = iso.getAccountId();
            if (!isAdmin(account.getType())) {
                if ((isoOwner == null) || (account.getId().longValue() != isoOwner.longValue())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to modify ISO with id " + isoId);
                }
            } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Long isoOwnerDomainId = getManagementServer().findDomainIdByAccountId(isoOwner);
                if (!getManagementServer().isChildDomain(account.getDomainId(), isoOwnerDomainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to modify ISO with id " + isoId);
                }
            }
        }
        
        // do the update
        boolean success = false;
        try {
        	success = getManagementServer().updateTemplate(isoId, name, displayText, null, guestOSId, null, bootable);
        } catch (Exception ex) {
        	 s_logger.error("Exception editing ISO", ex);
             throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update ISO " + isoId + ": " + ex.getMessage());
        }

        VMTemplateVO updatedIso = getManagementServer().findTemplateById(isoId);
        if (success) {
            List<Pair<String, Object>> isoData = new ArrayList<Pair<String, Object>>();
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), updatedIso.getId().toString()));
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), updatedIso.getName()));
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), updatedIso.getDisplayText()));
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.IS_PUBLIC.getName(), Boolean.valueOf(updatedIso.isPublicTemplate()).toString()));
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(updatedIso.getCreated())));
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.FORMAT.getName(), updatedIso.getFormat()));
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), updatedIso.getGuestOSId()));
            isoData.add(new Pair<String, Object>(BaseCmd.Properties.BOOTABLE.getName(), updatedIso.isBootable()));
            return isoData;
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "internal error updating ISO");
        }
    }
}
