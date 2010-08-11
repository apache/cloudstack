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
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.async.executor.CreatePrivateTemplateResultObject;
import com.cloud.serializer.SerializerHelper;
import com.cloud.server.Criteria;
import com.cloud.storage.Snapshot;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class CreateTemplateCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTemplateCmd.class.getName());
    private static final String s_name = "createtemplateresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.BITS, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISPLAY_TEXT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_FEATURED, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_PUBLIC, Boolean.FALSE)); 
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.OS_TYPE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PASSWORD_ENABLED, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.REQUIRES_HVM, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SNAPSHOT_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VOLUME_ID, Boolean.FALSE));

        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE)); 
    }

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="bits", type=CommandType.INTEGER)
    private Integer bits;

    @Parameter(name="displaytext", type=CommandType.STRING, required=true)
    private String displayText;

    @Parameter(name="isfeatured", type=CommandType.BOOLEAN)
    private Boolean featured;

    @Parameter(name="ispublic", type=CommandType.BOOLEAN)
    private Boolean publicTemplate;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String templateName;

    @Parameter(name="ostypeid", type=CommandType.LONG, required=true)
    private Long osTypeId;

    @Parameter(name="passwordenabled", type=CommandType.BOOLEAN)
    private Boolean passwordEnabled;

    @Parameter(name="requireshvm", type=CommandType.BOOLEAN)
    private Boolean requiresHvm;

    @Parameter(name="snapshotid", type=CommandType.LONG)
    private Long snapshotId;

    @Parameter(name="volumeid", type=CommandType.LONG)
    private Long volumeId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getBits() {
        return bits;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return publicTemplate;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public Boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public Boolean getRequiresHvm() {
        return requiresHvm;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Long getVolumeId() {
        return volumeId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "template";  
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }
	
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        String description = (String)params.get(BaseCmd.Properties.DISPLAY_TEXT.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        Long volumeId = (Long)params.get(BaseCmd.Properties.VOLUME_ID.getName());
        Long guestOSId = (Long) params.get(BaseCmd.Properties.OS_TYPE_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Boolean requiresHvm = (Boolean)params.get(BaseCmd.Properties.REQUIRES_HVM.getName());
        Integer bits = (Integer)params.get(BaseCmd.Properties.BITS.getName());
        Boolean passwordEnabled = (Boolean)params.get(BaseCmd.Properties.PASSWORD_ENABLED.getName());
        Boolean isPublic = (Boolean)params.get(BaseCmd.Properties.IS_PUBLIC.getName());
        Boolean featured = (Boolean)params.get(BaseCmd.Properties.IS_FEATURED.getName());
        Long snapshotId = (Long)params.get(BaseCmd.Properties.SNAPSHOT_ID.getName());
        
        if (volumeId == null && snapshotId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Specify at least one of the two parameters volumeId or snapshotId");
        }
        VolumeVO volume = null;
        // Verify input parameters
        if (snapshotId != null) {
            Snapshot snapshot = getManagementServer().findSnapshotById(snapshotId);
            if (snapshot == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "No snapshot exists with the given id: " + snapshotId);
            }
            
            if (volumeId != null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Specify only one of the two parameters volumeId or snapshotId");
            }
            // Set the volumeId to that of the snapshot. All further input parameter checks will be done w.r.t the volume.
            volumeId = snapshot.getVolumeId();
			volume = getManagementServer().findAnyVolumeById(volumeId);
        } else {
            volume = getManagementServer().findAnyVolumeById(volumeId);
        }
        
        if (volume == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a volume with id " + volumeId);
        }

        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        if (!isAdmin) {
            if (account.getId().longValue() != volume.getAccountId()) {
            	throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to find a volume with id " + volumeId + " for this account");
            }
        } else if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), volume.getDomainId())) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create a template from volume with id " + volumeId + ", permission denied.");
        }

        if (isPublic == null) {
        	isPublic = Boolean.FALSE;
        }   
        
        boolean allowPublicUserTemplates = Boolean.parseBoolean(getManagementServer().getConfigurationValue("allow.public.user.templates"));        
        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only private templates can be created.");
        }
        
        if (!isAdmin || featured == null) {
        	featured = Boolean.FALSE;
        }

        Criteria c = new Criteria();
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.CREATED_BY, Long.valueOf(volume.getAccountId()));
        List<VMTemplateVO> templates = getManagementServer().searchForTemplates(c);
        if ((templates != null) && !templates.isEmpty()) {
            for (VMTemplateVO template : templates) {
                if (template.getName().equalsIgnoreCase(name)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "a private template with name " + name + " already exists for account " +
                            volume.getAccountId() + ", please try again with a different name");
                }
            }
        }
        
        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        try {
            long jobId = getManagementServer().createPrivateTemplateAsync(userId, volumeId, name, description, guestOSId, requiresHvm, bits, passwordEnabled, isPublic, featured, snapshotId);

            if (jobId == 0) {
            	s_logger.warn("Unable to schedule async-job for CreateTemplate command");
            } else {
    	        if (s_logger.isDebugEnabled())
    	        	s_logger.debug("CreateTemplate command has been accepted, job id: " + jobId);
            }

            long templateId = waitInstanceCreation(jobId);
            List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), Long.valueOf(templateId))); 

            return returnValues;
        } catch (Exception ex) {
            throw new ServerApiException(BaseCmd.CREATE_PRIVATE_TEMPLATE_ERROR, "Unhandled exception while creating template name: " + name + " for volume " + volumeId + ", reason, " + ex.getMessage());
        }
    }

	protected long getInstanceIdFromJobSuccessResult(String result) {
		CreatePrivateTemplateResultObject resultObject = (CreatePrivateTemplateResultObject)SerializerHelper.fromSerializedString(result);
		if (resultObject != null) {
			return resultObject.getId();
		}

		return 0;
	}
}
