package com.cloud.baremetal.networkservice;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseListCmd;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ListResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@Implementation(responseObject = BaremetalDhcpResponse.class, description = "lists external DHCP servers")
public class ListBaremetalDhcpCmd extends BaseListCmd {
    private static final Logger s_logger = Logger.getLogger(ListBaremetalDhcpCmd.class);
    private static final String s_name = "listexternaldhcpresponse";
    @PlugService BaremetalDhcpManager _dhcpMgr;
    
    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @IdentityMapper(entityTableName = "baremetal_dhcp_devices")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "DHCP server device ID")
    private Long id;
    
    @IdentityMapper(entityTableName = "host_pod_ref")
    @Parameter(name = ApiConstants.POD_ID, type = CommandType.LONG, description = "Pod ID where pxe server is in")
    private Long podId;
    
    @Parameter(name = ApiConstants.DHCP_SERVER_TYPE, type = CommandType.STRING, description = "Type of DHCP device")
    private String deviceType;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPodId() {
        return podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }
    
    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException, NetworkRuleConflictException {
    	try {
            ListResponse<BaremetalDhcpResponse> response = new ListResponse<BaremetalDhcpResponse>();
            List<BaremetalDhcpResponse> dhcpResponses = _dhcpMgr.listBaremetalDhcps(this);
            response.setResponses(dhcpResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
    	} catch (Exception e) {
    		s_logger.debug("Exception happend while executing ListBaremetalDhcpCmd");
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
    	}

    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
