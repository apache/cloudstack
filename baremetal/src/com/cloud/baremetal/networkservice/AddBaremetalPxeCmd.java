package com.cloud.baremetal.networkservice;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.baremetal.database.BaremetalPxeVO;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.UserContext;

public class AddBaremetalPxeCmd extends BaseAsyncCmd {
    private static final String s_name = "addexternalpxeresponse";
    public static final Logger s_logger = Logger.getLogger(AddBaremetalPxeCmd.class);
    
    @PlugService BaremetalPxeManager pxeMgr;
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, required=true, description="the Physical Network ID")
    private Long physicalNetworkId;
    
    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, required = true, description="Pod Id")
    private Long podId;
    
    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the external pxe device")
    private String url;

    @Parameter(name=ApiConstants.PXE_SERVER_TYPE, type=CommandType.STRING, required = true, description="type of pxe device")
    private String deviceType;
    
    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Credentials to reach external pxe device")
    private String username;
    
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Credentials to reach external pxe device")
    private String password;
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_BAREMETAL_PXE_SERVER_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding an external pxe server";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException, NetworkRuleConflictException {
        try {
            BaremetalPxeVO vo = pxeMgr.addPxeServer(this);
        } catch (Exception e) {
            s_logger.warn("Unable to add external pxe server with url: " + getUrl(), e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage()); 
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return UserContext.current().getCaller().getId();
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public Long getPodId() {
        return podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
}
