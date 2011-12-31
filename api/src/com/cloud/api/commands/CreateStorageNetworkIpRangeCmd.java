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
import com.cloud.api.response.StorageNetworkIpRangeResponse;
import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@Implementation(description="Creates a VLAN IP range.", responseObject=StorageNetworkIpRangeResponse.class)
public class CreateStorageNetworkIpRangeCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(CreateStorageNetworkIpRangeCmd.class);
	
	private static final String s_name = "createstoragenetworkiprangeresponse";
	
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////    
    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="optional parameter. Have to be specified for Direct Untagged vlan only.")
    private Long podId;
    
    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, required=true, description="the beginning IP address in the VLAN IP range")
    private String startIp;

    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address in the VLAN IP range")
    private String endIp;
    
    @Parameter(name=ApiConstants.VLAN, type=CommandType.INTEGER, description="the ID or VID of the VLAN. Optional, null means no vlan")
    private Integer vlan;
    
    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID of the VLAN IP range")
    private Long zoneId;
    
    @IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="the network id")
    private Long networkID;
    
    @Parameter(name=ApiConstants.NETMASK, type=CommandType.STRING, required=true, description="the netmask for the Pod")
    private String netmask;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEndIp() {
        return endIp;
    }

    public Long getPodId() {
        return podId;
    }

    public String getStartIp() {
        return startIp;
    }

    public Integer getVlan() {
        return vlan;
    }
    
    public Long getZoneId() {
        return zoneId;
    }
    
    public Long getNetworkID() {
        return networkID;
    }
    
    public String getNetmask() {
    	return netmask;
    }

	@Override
	public String getEventType() {
		return EventTypes.EVENT_STORAGE_IP_RANGE_CREATE;
	}

	@Override
	public String getEventDescription() {
		return "Creating storage ip range from " + getStartIp() + " to " + getEndIp() + " with vlan " + getVlan();
	}

	@Override
	public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
	        ResourceAllocationException {
		try {
			StorageNetworkIpRange result = _storageNetworkService.createIpRange(this);
			StorageNetworkIpRangeResponse response = _responseGenerator.createStorageNetworkIpRangeResponse(result);
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (Exception e) {
			s_logger.warn("Create storage network IP range failed", e);
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
