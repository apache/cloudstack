package com.cloud.api.commands;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.DataCenterVO;
import com.cloud.server.ManagementServer;
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
		
		ManagementServer managementServer = getManagementServer();
        VMTemplateVO template = managementServer.findTemplateById(templateId.longValue());
        if (template == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to find template with id " + templateId);
        }
        if (template.getName().startsWith("xs-tools") ){
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to extract the template " + template.getName() + " It is not supported yet");
        }
		
        if(url.toLowerCase().contains("file://")){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "file:// type urls are currently unsupported");
        }
                
    	if (account != null) {    		    	
    		if(!isAdmin(account.getType())){
    			if (template.getAccountId() != account.getId()){
    				throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find template with ID: " + templateId + " for account: " + account.getAccountName());
    			}
    		}else if(!managementServer.isChildDomain(account.getDomainId(), managementServer.findDomainIdByAccountId(template.getAccountId())) ) {
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to extract template " + templateId + " to " + url + ", permission denied.");
    		}
    	}
    	
        try {
			managementServer.extractTemplate(url, templateId, zoneId);
		} catch (Exception e) {			
			s_logger.error(e.getMessage(), e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal Error Extracting the template " + e.getMessage());
		}
		DataCenterVO zone = managementServer.getDataCenterBy(zoneId);		
		List<Pair<String, Object>> response = new ArrayList<Pair<String, Object>>();
		response.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), templateId));
		response.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), template.getName()));
		response.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), template.getDisplayText()));
		response.add(new Pair<String, Object>(BaseCmd.Properties.URL.getName(), url));
		response.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), zoneId));
		response.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), zone.getName()));
		response.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_STATUS.getName(), "Processing"));		
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
