package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.DataCenterVO;
import com.cloud.server.ManagementServer;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ExtractVolumeCmd extends BaseCmd {

	public static final Logger s_logger = Logger.getLogger(ExtractVolumeCmd.class.getName());

    private static final String s_name = "extractvolumeresponse";
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
		Long volumeId    = (Long) params.get(BaseCmd.Properties.ID.getName());
		Long zoneId		   = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
		Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());				
		
		ManagementServer managementServer = getManagementServer();
        VolumeVO volume = managementServer.findVolumeById(volumeId);
        if (volume == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to find volume with id " + volumeId);
        }
		
        if(url.toLowerCase().contains("file://")){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "file:// type urls are currently unsupported");
        }
                
    	if (account != null) {    		    	
    		if(!isAdmin(account.getType())){
    			if (volume.getAccountId() != account.getId()){
    				throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId + " for account: " + account.getAccountName());
    			}
    		}else if(!managementServer.isChildDomain(account.getDomainId(), volume.getDomainId())){
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to extract volume " + volumeId + " to " + url + ", permission denied.");
    		}
    	}
    	
        try {
			managementServer.extractVolume(url, volumeId, zoneId);
		} catch (Exception e) {			
			s_logger.error(e.getMessage(), e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal Error Extracting the volume " + e.getMessage());
		}
		DataCenterVO zone = managementServer.getDataCenterBy(zoneId);		
		List<Pair<String, Object>> response = new ArrayList<Pair<String, Object>>();
		response.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), volumeId));
		response.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_NAME.getName(), volume.getName()));		
		response.add(new Pair<String, Object>(BaseCmd.Properties.URL.getName(), url));
		response.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), zoneId));
		response.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), zone.getName()));
		response.add(new Pair<String, Object>(BaseCmd.Properties.STATUS.getName(), "Processing"));
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
