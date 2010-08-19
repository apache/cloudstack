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

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;

@Implementation(method="registerTemplate", manager=Manager.TemplateManager)
public class RegisterTemplateCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(RegisterTemplateCmd.class.getName());

    private static final String s_name = "registertemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="bits", type=CommandType.INTEGER)
    private Integer bits;

    @Parameter(name="displaytext", type=CommandType.STRING, required=true)
    private String displayText;

    @Parameter(name="format", type=CommandType.STRING, required=true)
    private String format;

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

    @Parameter(name="url", type=CommandType.STRING, required=true)
    private String url;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getBits() {
        return bits;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getFormat() {
        return format;
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

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}


//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
//        String displayText = (String)params.get(BaseCmd.Properties.DISPLAY_TEXT.getName()); 
//        Integer bits = (Integer)params.get(BaseCmd.Properties.BITS.getName());
//        Boolean passwordEnabled = (Boolean)params.get(BaseCmd.Properties.PASSWORD_ENABLED.getName());
//        Boolean requiresHVM = (Boolean)params.get(BaseCmd.Properties.REQUIRES_HVM.getName());
//        String url = (String)params.get(BaseCmd.Properties.URL.getName());
//        Boolean isPublic = (Boolean)params.get(BaseCmd.Properties.IS_PUBLIC.getName());
//        Boolean featured = (Boolean)params.get(BaseCmd.Properties.IS_FEATURED.getName());
//        String format = (String)params.get(BaseCmd.Properties.FORMAT.getName());
//        Long guestOSId = (Long) params.get(BaseCmd.Properties.OS_TYPE_ID.getName());
//        Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
//
//        //parameters verification
//        if (bits == null) {
//            bits = Integer.valueOf(64);
//        }
//        if (passwordEnabled == null) {
//            passwordEnabled = false;
//        }
//        if (requiresHVM == null) {
//            requiresHVM = true;
//        }
//        if (isPublic == null) {
//            isPublic = Boolean.FALSE;
//        }
//        
//        if (zoneId.longValue() == -1) {
//        	zoneId = null;
//        }
//                
//        long accountId = 1L; // default to system account
//        if (account != null) {
//            accountId = account.getId().longValue();
//        }
//        
//        Account accountObj;
//        if (account == null) {
//        	accountObj = getManagementServer().findAccountById(accountId);
//        } else {
//        	accountObj = account;
//        }
//        
//        boolean isAdmin = (accountObj.getType() == Account.ACCOUNT_TYPE_ADMIN);
//        
//        if (!isAdmin && zoneId == null) {
//        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid zone Id.");
//        }
//        
//        if(url.toLowerCase().contains("file://")){
//        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "File:// type urls are currently unsupported");
//        }
//        
//        if((!url.toLowerCase().endsWith("vhd"))&&(!url.toLowerCase().endsWith("vhd.zip"))
//        	&&(!url.toLowerCase().endsWith("vhd.bz2"))&&(!url.toLowerCase().endsWith("vhd.gz") 
//        	&&(!url.toLowerCase().endsWith("qcow2"))&&(!url.toLowerCase().endsWith("qcow2.zip"))
//        	&&(!url.toLowerCase().endsWith("qcow2.bz2"))&&(!url.toLowerCase().endsWith("qcow2.gz")))){
//        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid "+format.toLowerCase());
//        }
//        	
//        boolean allowPublicUserTemplates = Boolean.parseBoolean(getManagementServer().getConfigurationValue("allow.public.user.templates"));        
//        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
//        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only private templates can be created.");
//        }
//        
//        if (!isAdmin || featured == null) {
//        	featured = Boolean.FALSE;
//        }
//
//        //If command is executed via 8096 port, set userId to the id of System account (1)
//        if (userId == null) {
//            userId = Long.valueOf(1);
//        }
//
//        Long templateId;
//        try {
//        	templateId = getManagementServer().createTemplate(userId, zoneId, name, displayText, isPublic, featured, format, "ext3", url, null, requiresHVM, bits, passwordEnabled, guestOSId, true);
//        } catch (InvalidParameterValueException ipve) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Internal error registering template " + name + "; " + ipve.getMessage());
//        } catch (IllegalArgumentException iae) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Internal error registering template " + name + "; " + iae.getMessage());
//        } catch (ResourceAllocationException rae) {
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error registering template " + name + "; " + rae.getMessage());
//        } catch (Exception ex) {
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error registering template " + name);
//        }
//        	
//    	VMTemplateVO template = getManagementServer().findTemplateById(templateId);
//    	List<Pair<String, Object>> templateTags = new ArrayList<Pair<String, Object>>();
//    	List<Object> tTagList = new ArrayList<Object>();
//    	if (template != null) {
//    		List<DataCenterVO> zones = null;
//        	
//        	if (zoneId != null) {
//        		zones = new ArrayList<DataCenterVO>();
//        		zones.add(getManagementServer().findDataCenterById(zoneId));
//        	} else {
//        		zones = getManagementServer().listDataCenters();   
//        	}
//        	        	
//        	for (DataCenterVO zone : zones) {
//        		VMTemplateHostVO templateHostRef = getManagementServer().findTemplateHostRef(templateId, zone.getId());        		
//    		
//        		// Use embeded object for response
//        		List<Pair<String, Object>> listForEmbeddedObject = new ArrayList<Pair<String, Object>>();
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), template.getId().toString()));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), template.getName()));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), template.getDisplayText()));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.IS_PUBLIC.getName(), Boolean.valueOf(template.isPublicTemplate()).toString()));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.CROSS_ZONES.getName(), Boolean.valueOf(template.isCrossZones()).toString()));
//        		
//        		if (templateHostRef != null) {
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(templateHostRef.getCreated())));
//        		}
//        	
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.IS_READY.getName(), (templateHostRef != null && templateHostRef.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED)));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.IS_FEATURED.getName(), Boolean.valueOf(template.isFeatured()).toString()));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.PASSWORD_ENABLED.getName(), Boolean.valueOf(template.getEnablePassword()).toString()));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.FORMAT.getName(), template.getFormat().toString()));
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_STATUS.getName(), "Processing"));
//        		GuestOS os = getManagementServer().findGuestOSById(template.getGuestOSId());
//        		if (os != null) {
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), os.getId()));
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_NAME.getName(), os.getDisplayName()));
//        		} else {
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_ID.getName(), -1));
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.OS_TYPE_NAME.getName(), ""));
//        		}
//
//        		Account owner = getManagementServer().findAccountById(template.getAccountId());
//        		if (owner != null) {
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT_ID.getName(), owner.getId()));
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), owner.getAccountName()));
//        			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), owner.getDomainId()));
//        		}
//
//        		listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), zone.getId()));
//    			listForEmbeddedObject.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), zone.getName()));
//
//    			tTagList.add(listForEmbeddedObject);            		
//        	}    	
//    	}
//    	
//    	Object[] tTag = new Object[tTagList.size()];
//        for (int i = 0; i < tTagList.size(); i++) {
//        	tTag[i] = tTagList.get(i);
//        }
//                               
//        Pair<String, Object> templateTag = new Pair<String, Object>("template", tTag);
//        templateTags.add(templateTag);
//
//        return templateTags;
//    
//    }
}
