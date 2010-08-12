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
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class UpdateTemplatePermissionsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateTemplatePermissionsCmd.class.getName());
    private static final String s_name = "updatetemplatepermissionsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_PUBLIC, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_FEATURED, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_NAMES, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.OP, Boolean.FALSE));
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
        Boolean isPublic = (Boolean)params.get(BaseCmd.Properties.IS_PUBLIC.getName());
        Boolean isFeatured = (Boolean)params.get(BaseCmd.Properties.IS_FEATURED.getName());
        String accoutNames = (String)params.get(BaseCmd.Properties.ACCOUNT_NAMES.getName());
        String operation = (String)params.get(BaseCmd.Properties.OP.getName());

        Boolean publishTemplateResult = Boolean.FALSE;

        VMTemplateVO template = getManagementServer().findTemplateById(templateId.longValue());
        if (template == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find template with id " + templateId);
        }

        if (account != null) {
            if (!isAdmin(account.getType()) && (template.getAccountId() != account.getId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to update permissions for template with id " + templateId);
            } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Long templateOwnerDomainId = getManagementServer().findDomainIdByAccountId(template.getAccountId());
                if (!getManagementServer().isChildDomain(account.getDomainId(), templateOwnerDomainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update permissions for template with id " + templateId);
                }
            }
        }

        if (templateId == Long.valueOf(1)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to update permissions for template with id " + templateId);
        }
        
        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        boolean allowPublicUserTemplates = Boolean.parseBoolean(getManagementServer().getConfigurationValue("allow.public.user.templates"));        
        if (!isAdmin && !allowPublicUserTemplates && isPublic != null && isPublic) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only private templates can be created.");
        }

        // package up the accountNames as a list
        List<String> accountNameList = new ArrayList<String>();
        if (accoutNames != null) {
            if ((operation == null) || (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove") && !operation.equalsIgnoreCase("reset"))) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid operation on accounts, the operation must be either 'add' or 'remove' in order to modify launch permissions." +
                        "  Given operation is: '" + operation + "'");
            }
            StringTokenizer st = new StringTokenizer(accoutNames, ",");
            while (st.hasMoreTokens()) {
                accountNameList.add(st.nextToken());
            }
        }

        try {
            getManagementServer().updateTemplatePermissions(templateId, operation, isPublic, isFeatured, accountNameList);
            publishTemplateResult = Boolean.TRUE;
        } catch (InvalidParameterValueException ex) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to update template permissions for template " + template.getName() + ":  internal error.");
        } catch (PermissionDeniedException ex) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Failed to update template permissions for template " + template.getName() + ":  internal error.");
        } catch (Exception ex) {
             s_logger.error("Exception editing template", ex);
             throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update template permissions for template " + template.getName() + ":  internal error.");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), publishTemplateResult.toString()));
        return returnValues;
    }
}
