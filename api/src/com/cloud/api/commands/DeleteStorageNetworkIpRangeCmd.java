package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@Implementation(description="Deletes a storage network IP Range.", responseObject=SuccessResponse.class, since="3.0.0")
public class DeleteStorageNetworkIpRangeCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(DeleteStorageNetworkIpRangeCmd.class);
	
	private static final String s_name = "deletestoragenetworkiprangeresponse";
	
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="dc_storage_network_ip_range")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the uuid of the storage network ip range")
    private Long id;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }
    
	@Override
	public String getEventType() {
		return EventTypes.EVENT_STORAGE_IP_RANGE_DELETE;
	}

	@Override
	public String getEventDescription() {
		return "Deleting storage ip range " + getId();
	}

	@Override
	public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
	        ResourceAllocationException {
		try {
			_storageNetworkService.deleteIpRange(this);
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
		} catch (Exception e) {
			s_logger.warn("Failed to delete storage network ip range " + getId(), e);
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
		}
	}

	@Override
	public String getCommandName() {
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		return Account.ACCOUNT_ID_SYSTEM;
	}

}
