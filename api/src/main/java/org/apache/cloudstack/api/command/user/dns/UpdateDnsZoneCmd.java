package org.apache.cloudstack.api.command.user.dns;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsProviderManager;
import org.apache.cloudstack.dns.DnsZone;

@APICommand(name = "updateDnsZone", description = "Updates a DNS Zone's metadata",
        responseObject = DnsZoneResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateDnsZoneCmd extends BaseCmd {

    private static final String COMMAND_RESPONSE_NAME = "updatednszoneresponse";

    @Inject
    DnsProviderManager dnsProviderManager;

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            required = true, description = "The ID of the DNS zone")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Display text for the zone")
    private String description;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// Implementation //////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            DnsZone result = dnsProviderManager.updateDnsZone(this);
            if (result != null) {
                DnsZoneResponse response = dnsProviderManager.createDnsZoneResponse(result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update DNS Zone on external provider");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update DNS Zone: " + e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return COMMAND_RESPONSE_NAME;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
