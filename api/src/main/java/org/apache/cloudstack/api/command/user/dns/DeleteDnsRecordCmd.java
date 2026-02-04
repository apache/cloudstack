package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;

@APICommand(name = "deleteDnsRecord", description = "Deletes a DNS record from the external provider",
           responseObject = SuccessResponse.class)
public class DeleteDnsRecordCmd extends BaseAsyncCmd {

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            required = true, description = "The ID of the DNS zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true)
    private String name;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true)
    private String type;

    // Getters
    public Long getZoneId() { return zoneId; }
    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public void execute() {
        try {
            boolean result = dnsProviderManager.deleteDnsRecord(this);
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete DNS Record");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Error deleting DNS Record: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() { return CallContext.current().getCallingAccount().getId(); }

    @Override
    public String getEventType() { return EventTypes.EVENT_DNS_RECORD_DELETE; }

    @Override
    public String getEventDescription() { return "Deleting DNS Record: " + getName(); }
}
