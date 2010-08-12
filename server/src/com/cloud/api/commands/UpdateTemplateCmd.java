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
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class UpdateTemplateCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(UpdateTemplateCmd.class.getName());
    private static final String s_name = "updatetemplateresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISPLAY_TEXT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.FORMAT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.OS_TYPE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PASSWORD_ENABLED, Boolean.FALSE));
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
        Long templateId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        String displayText = (String)params.get(BaseCmd.Properties.DISPLAY_TEXT.getName());
        String format = (String)params.get(BaseCmd.Properties.FORMAT.getName());
        Long guestOSId = (Long) params.get(BaseCmd.Properties.OS_TYPE_ID.getName());
        Boolean passwordEnabled = (Boolean)params.get(BaseCmd.Properties.PASSWORD_ENABLED.getName());
        
        VMTemplateVO template = getManagementServer().findTemplateById(templateId.longValue());
        if (template == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find template with id " + templateId);
        }

        if (account != null) {
        	if (!isAdmin(account.getType())) {
	            if (template.getAccountId() != account.getId()) {
	                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to edit template with id " + templateId);
	            }
        	} else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Long templateOwnerDomainId = getManagementServer().findDomainIdByAccountId(template.getAccountId());
                if (!getManagementServer().isChildDomain(account.getDomainId(), templateOwnerDomainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to modify template with id " + templateId);
                }
            }
        }

        if (templateId == Long.valueOf(1)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to update template with id " + templateId);
        }

        
        boolean success = false;
        try {
        	success = getManagementServer().updateTemplate(templateId, name, displayText, format, guestOSId, passwordEnabled, null);
        } catch (Exception ex) {
        	 s_logger.error("Exception editing template", ex);
             throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update template " + templateId + ": " + ex.getMessage());
        }

        VMTemplateVO updatedTemplate = getManagementServer().findTemplateById(templateId);
        if (success) {
            List<Pair<String, Object>> templateData = new ArrayList<Pair<String, Object>>();
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), updatedTemplate.getId().toString()));
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), updatedTemplate.getName()));
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), updatedTemplate.getDisplayText()));
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.IS_PUBLIC.getName(), Boolean.valueOf(updatedTemplate.isPublicTemplate()).toString()));
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(updatedTemplate.getCreated())));
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.FORMAT.getName(), updatedTemplate.getFormat()));
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), updatedTemplate.getGuestOSId()));
            templateData.add(new Pair<String, Object>(BaseCmd.Properties.PASSWORD_ENABLED.getName(), updatedTemplate.getEnablePassword()));
            return templateData;
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "internal error updating template");
        }
    }
}
