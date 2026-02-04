package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;

@APICommand(name = "createDnsRecord", description = "Creates a DNS record directly on the provider",
        responseObject = DnsRecordResponse.class)
public class CreateDnsRecordCmd extends BaseAsyncCmd {

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = DnsZoneResponse.class, required = true)
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Record name")
    private String name;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true, description = "Record type (A, CNAME)")
    private String type;

    @Parameter(name = "content", type = CommandType.STRING, required = true, description = "IP or target")
    private String content;

    @Parameter(name = "ttl", type = CommandType.INTEGER, description = "Time to live")
    private Integer ttl;

    // Getters
    public Long getZoneId() { return zoneId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public Integer getTtl() { return (ttl == null) ? 3600 : ttl; }

    @Override
    public void execute() {
        try {
            DnsRecordResponse response = dnsProviderManager.createDnsRecord(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create DNS Record: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() { return CallContext.current().getCallingAccount().getId(); }

    @Override
    public String getEventType() { return EventTypes.EVENT_DNS_RECORD_CREATE; }

    @Override
    public String getEventDescription() { return "Creating DNS Record: " + getName(); }
}
