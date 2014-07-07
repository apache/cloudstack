package org.apache.cloudstack.api;

import com.cloud.baremetal.manager.BaremetalVlanManager;
import com.cloud.baremetal.networkservice.BaremetalRctResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;

/**
 * Created by frank on 5/8/14.
 */
@APICommand(name = "addBaremetalRct", description = "adds baremetal rack configuration text", responseObject = BaremetalRctResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddBaremetalRctCmd extends BaseAsyncCmd {
    private static final String s_name = "addbaremetalrctresponse";
    public static final Logger s_logger = Logger.getLogger(AddBaremetalRctCmd.class);

    @Inject
    private BaremetalVlanManager vlanMgr;

    @Parameter(name=ApiConstants.BAREMETAL_RCT_URL, required = true, description = "http url to baremetal RCT configuration")
    private String rctUrl;

    public String getRctUrl() {
        return rctUrl;
    }

    public void setRctUrl(String rctUrl) {
        this.rctUrl = rctUrl;
    }

    public String getEventType() {
        return EventTypes.EVENT_BAREMETAL_RCT_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding baremetal rct configuration";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            BaremetalRctResponse rsp = vlanMgr.addRct(this);
            this.setResponseObject(rsp);
        } catch (Exception e) {
            s_logger.warn(String.format("unable to add baremetal RCT[%s]", getRctUrl()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
