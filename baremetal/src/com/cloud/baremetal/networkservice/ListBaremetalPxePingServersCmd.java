package com.cloud.baremetal.networkservice;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@Implementation(responseObject = BaremetalPxePingResponse.class, description = "lists PING Pxe servers")
public class ListBaremetalPxePingServersCmd extends BaseListCmd {
    private static final Logger s_logger = Logger.getLogger(ListBaremetalPxePingServersCmd.class);
    private static final String s_name = "listpingpxeserverresponse";

    @PlugService
    BaremetalPxeManager _pxeMgr;
    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @IdentityMapper(entityTableName = "external_pxe_devices")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = true, description = "Ping pxe server device ID")
    private Long id;
    
    @IdentityMapper(entityTableName = "host_pod_ref")
    @Parameter(name = ApiConstants.POD_ID, type = CommandType.LONG, required = true, description = "Pod ID where pxe server is in")
    private Long podId;

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

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException, NetworkRuleConflictException {
        try {
            ListResponse<BaremetalPxeResponse> response = new ListResponse<BaremetalPxeResponse>();
            List<BaremetalPxeResponse> pxeResponses = _pxeMgr.listPxeServers(this);
            response.setResponses(pxeResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (Exception e) {
            s_logger.debug("Exception happend while executing ListPingPxeServersCmd" ,e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
