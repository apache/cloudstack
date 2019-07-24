package org.apache.cloudstack.api.command.admin.vlan;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.apache.log4j.Logger;

import com.cloud.dc.Vlan;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;


@APICommand(name = "updateVlanIpRange", description = "Updates a VLAN IP range.", responseObject =
        VlanIpRangeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateVlanIpRangeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVlanIpRangeCmd.class.getName());

    private static final String s_name = "updatevlaniprangeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VlanIpRangeResponse.class, required = true,
            description = "the UUID of the VLAN IP range")
    private Long id;

    @Parameter(name = ApiConstants.END_IP, type = CommandType.STRING, description = "the ending IP address in the VLAN IP range")
    private String endIp;

    @Parameter(name = ApiConstants.GATEWAY, type = CommandType.STRING, description = "the gateway of the VLAN IP range")
    private String gateway;

    @Parameter(name = ApiConstants.NETMASK, type = CommandType.STRING, description = "the netmask of the VLAN IP range")
    private String netmask;

    @Parameter(name = ApiConstants.START_IP, type = CommandType.STRING, description = "the beginning IP address in the VLAN IP range")
    private String startIp;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.INTEGER, description = "Optional. the vlan the ip range sits on")
    private Integer vlan;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }
    public String getGateway() {
        return gateway;
    }
    public String getEndIp() {
        return endIp;
    }
    public String getNetmask() {
        return netmask;
    }
    public String getStartIp() {
        return startIp;
    }
    public Integer getVlan() {
        return vlan;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VLAN_IP_RANGE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Update vlan ip range " + getId() + " [StartIp=" + getStartIp() + ", EndIp=" + getEndIp() + ", vlan=" + getVlan() + ", netmask=" + getNetmask() + ']';
    }


    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException {
        try {
            Vlan result = _configService.updateVlanAndPublicIpRange(this);
            if (result != null) {
                VlanIpRangeResponse response = _responseGenerator.createVlanIpRangeResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to Update vlan ip range");
            }
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
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
