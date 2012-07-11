package com.cloud.api.commands;

import java.util.ArrayList;
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
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NiciraNvpDeviceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.element.NiciraNvpElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=NiciraNvpDeviceResponse.class, description="Lists Nicira NVP devices")
public class ListNiciraNvpDevicesCmd extends BaseListCmd {
    private static final Logger s_logger = Logger.getLogger(ListNiciraNvpDevicesCmd.class.getName());
    private static final String s_name = "listniciranvpdevices";
    @PlugService NiciraNvpElementService _niciraNvpElementService;

   /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="the Physical Network ID")
    private Long physicalNetworkId;

    @IdentityMapper(entityTableName="external_nicira_nvp_devices")
    @Parameter(name=ApiConstants.NICIRA_NVP_DEVICE_ID, type=CommandType.LONG,  description="nicira nvp device ID")
    private Long niciraNvpDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNiciraNvpDeviceId() {
        return niciraNvpDeviceId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            List<NiciraNvpDeviceVO> niciraDevices = _niciraNvpElementService.listNiciraNvpDevices(this);
            ListResponse<NiciraNvpDeviceResponse> response = new ListResponse<NiciraNvpDeviceResponse>();
            List<NiciraNvpDeviceResponse> niciraDevicesResponse = new ArrayList<NiciraNvpDeviceResponse>();

            if (niciraDevices != null && !niciraDevices.isEmpty()) {
                for (NiciraNvpDeviceVO niciraDeviceVO : niciraDevices) {
                    NiciraNvpDeviceResponse niciraDeviceResponse = _niciraNvpElementService.createNiciraNvpDeviceResponse(niciraDeviceVO);
                    niciraDevicesResponse.add(niciraDeviceResponse);
                }
            }

            response.setResponses(niciraDevicesResponse);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
}
