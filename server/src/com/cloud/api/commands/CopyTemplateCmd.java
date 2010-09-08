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

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="copyTemplate", manager=Manager.TemplateManager)
public class CopyTemplateCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(CopyTemplateCmd.class.getName());
    private static final String s_name = "copytemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="destzoneid", type=CommandType.LONG, required=true)
    private Long destZoneId;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="sourcezoneid", type=CommandType.LONG, required=true)
    private Long sourceZoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDestinationZoneId() {
        return destZoneId;
    }

    public Long getId() {
        return id;
    }

    public Long getSourceZoneId() {
        return sourceZoneId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    public static String getStaticName() {
        return s_name;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long templateId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long sourceZoneId = (Long)params.get(BaseCmd.Properties.SOURCE_ZONE_ID.getName());
//        Long destZoneId = (Long)params.get(BaseCmd.Properties.DEST_ZONE_ID.getName());
//
//        if (userId == null) {
//            userId = Long.valueOf(1);
//        }
//
//        VMTemplateVO template1 = getManagementServer().findTemplateById(templateId.longValue());
//        if (template1 == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find template with id " + templateId);
//        }
//        
//        boolean isIso = Storage.ImageFormat.ISO.equals(template1.getFormat());
//        if (isIso) {
//        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid template.");
//        }
//
//        if (account != null) {
//            if (!isAdmin(account.getType())) {
//                if (template1.getAccountId() != account.getId()) {
//                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to copy template with id " + templateId);
//                }
//            } else {
//                Account templateOwner = getManagementServer().findAccountById(template1.getAccountId());
//                if ((templateOwner != null) && !getManagementServer().isChildDomain(account.getDomainId(), templateOwner.getDomainId())) {
//                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to copy template with id " + templateId + " to zone " + destZoneId);
//                }
//            }
//        }
//        
//        try {
//    		long jobId = getManagementServer().copyTemplateAsync(userId, templateId, sourceZoneId, destZoneId);
//
//    		if (jobId == 0) {
//            	s_logger.warn("Unable to schedule async-job for CopyTemplate command");
//            } else {
//    	        if (s_logger.isDebugEnabled()) {
//    	        	s_logger.debug("CopyTemplate command has been accepted, job id: " + jobId);
//    	        }
//            }
//    		
//    		templateId = waitInstanceCreation(jobId);
//    		List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), Long.valueOf(templateId))); 
//            
//            return returnValues;
//    	} catch (Exception ex) {
//    	    if (ex instanceof ServerApiException) {
//    	        throw (ServerApiException)ex;
//    	    }
//    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to copy template: " + ex.getMessage());
//    	}
// 
//    }
//    
//    protected long getInstanceIdFromJobSuccessResult(String result) {
//    	CopyTemplateResultObject resultObject = (CopyTemplateResultObject)SerializerHelper.fromSerializedString(result);
//		if (resultObject != null) {
//			return resultObject.getId();
//		}
//
//		return 0;
//	}
    
	@Override
	public String getResponse() {
		// TODO Add the response object as per executor
		return null;
	}
}

