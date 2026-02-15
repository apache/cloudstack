package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "deleteDnsZone", description = "Removes a DNS Zone from CloudStack and the external provider",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteDnsZoneCmd extends BaseAsyncCmd {
    private static final String COMMAND_RESPONSE_NAME = "deletednszoneresponse";

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsZoneResponse.class, required = true, description = "The ID of the DNS zone")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation //////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            boolean result = dnsProviderManager.deleteDnsZone(getId());
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete DNS Zone");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete DNS Zone: " + e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return COMMAND_RESPONSE_NAME;
    }

    @Override
    public long getEntityOwnerId() {
        // Look up the Zone to find the Account Owner
        DnsZone zone = dnsProviderManager.getDnsZone(id);
        if (zone != null) {
            return zone.getAccountId();
        }
        // Fallback or System if not found (likely to fail in execute() anyway)
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DNS_ZONE_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting DNS Zone ID: " + getId();
    }
}
