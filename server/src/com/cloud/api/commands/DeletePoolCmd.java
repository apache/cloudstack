package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.storage.StorageManager;

@Implementation(method="deletePool", manager=StorageManager.class, description="Deletes a storage pool.")
public class DeletePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeletePoolCmd.class.getName());
    private static final String s_name = "deletestoragepoolresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="Storage pool id")
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override @SuppressWarnings("unchecked")
    public SuccessResponse getResponse() {
    	if ((Boolean)getResponseObject()) {
	    	return new SuccessResponse();
	    } else {
	    	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete storage pool");
	    }
    }
}
