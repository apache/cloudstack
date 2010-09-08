package com.cloud.api.commands;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ExtractTemplateCmd extends BaseCmd {

	public static final Logger s_logger = Logger.getLogger(ExtractTemplateCmd.class.getName());

    private static final String s_name = "extracttemplateresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {        
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.URL, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    }
    
	@Override
	public List<Pair<String, Object>> execute(Map<String, Object> params) {
		String url		   = (String) params.get(BaseCmd.Properties.URL.getName());
		Long templateId    = (Long) params.get(BaseCmd.Properties.ID.getName());
		Long zoneId		   = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
		Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());				
		
        VMTemplateVO template = getManagementServer().findTemplateById(templateId.longValue());
        if (template == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to find template with id " + templateId);
        }
		
        if(url.toLowerCase().contains("file://")){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "file:// type urls are currently unsupported");
        }
        
        boolean isAdmin;
    	if (account == null) {
    		// Admin API call
    		isAdmin = true;
    	} else {
    		// User API call
    		isAdmin = isAdmin(account.getType());
    	}
    	
    	if(!isAdmin){
    		if (template.getAccountId() != account.getId()){
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find template with ID: " + templateId + " for account: " + account.getAccountName());
    		}
    	}
    	
        try {
			getManagementServer().extractTemplate(url, templateId, zoneId);
		} catch (Exception e) {			
			s_logger.error(e.getMessage(), e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal Error Extracting the template " + e.getMessage());
		}				
				
		List<Pair<String, Object>> response = new ArrayList<Pair<String, Object>>();
		response.add(new Pair<String, Object>("template", templateId));
		response.add(new Pair<String, Object>("url", url));
		response.add(new Pair<String, Object>("zoneid", zoneId));
		return response;
	}

	@Override
	public String getName() {
		return s_name;
	}

	@Override
	public List<Pair<Enum, Boolean>> getProperties() {
		return s_properties;
	}

}
