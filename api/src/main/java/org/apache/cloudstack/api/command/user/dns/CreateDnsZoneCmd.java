package org.apache.cloudstack.api.command.user.dns;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.DnsProviderManager;
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;

@APICommand(name = "createDnsZone", description = "Creates a new DNS Zone on a specific server",
        responseObject = DnsZoneResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateDnsZoneCmd extends BaseAsyncCreateCmd {

    private static final String COMMAND_RESPONSE_NAME = "creatednszoneresponse";

    @Inject
    DnsProviderManager dnsProviderManager;

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "The name of the DNS zone (e.g. example.com)")
    private String name;

    @Parameter(name = "dnsserverid", type = CommandType.UUID, entityType = DnsServerResponse.class,
            required = true, description = "The ID of the DNS server to host this zone")
    private Long dnsServerId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class,
            description = "Optional: The Guest Network to associate with this zone for auto-registration")
    private Long networkId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING,
            description = "The type of zone (Public, Private). Defaults to Public.")
    private String type;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Display text for the zone")
    private String description;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public Long getDnsServerId() {
        return dnsServerId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    /////////////////////////////////////////////////////
    /////////////// Implementation //////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void create() throws ResourceAllocationException {
        try {
            DnsZone zone = dnsProviderManager.allocateDnsZone(this);
            if (zone != null) {
                setEntityId(zone.getId());
                setEntityUuid(zone.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create DNS Zone entity");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to allocate DNS Zone: " + e.getMessage());
        }
    }

    @Override
    public void execute() {
        try {
            DnsZone result = dnsProviderManager.provisionDnsZone(getEntityId());
            if (result != null) {
                DnsZoneResponse response = dnsProviderManager.createDnsZoneResponse(result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to provision DNS Zone on external provider");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to provision DNS Zone: " + e.getMessage());
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

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DNS_ZONE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating DNS zone: " + getName();
    }
}
