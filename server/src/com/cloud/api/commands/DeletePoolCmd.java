package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.Pair;

public class DeletePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeletePoolCmd.class.getName());

    private static final String s_name = "deletepoolresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
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
    	Long poolId = (Long) params.get(BaseCmd.Properties.ID.getName());
    	
    	//verify parameters
    	StoragePoolVO sPool = getManagementServer().findPoolById(poolId);
    	if (sPool == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find pool by id " + poolId);
    	}
    	
    	if (sPool.getPoolType().equals(StoragePoolType.LVM)) {
    		throw new ServerApiException(BaseCmd.UNSUPPORTED_ACTION_ERROR, "Unable to delete local storage id: " + poolId);
    	}
    	
    	boolean deleted = true;
        try {
             deleted = getManagementServer().deletePool(poolId);
             
        } catch (Exception ex) {
            s_logger.error("Exception deleting pool", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
        if (!deleted) {
       	 	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Volumes exist on primary storage, unable to delete");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), "true"));
        
        return returnValues;
    }
}
