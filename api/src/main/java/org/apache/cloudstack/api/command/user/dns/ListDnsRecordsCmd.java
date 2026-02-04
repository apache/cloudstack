package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;

@APICommand(name = "listDnsRecords", description = "Lists DNS records from the external provider",
        responseObject = DnsRecordResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDnsRecordsCmd extends BaseListCmd {
    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            required = true, description = "the ID of the DNS zone to list records from")
    private Long zoneId;

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public void execute() {
        // The manager will fetch live data from the plugin
        ListResponse<DnsRecordResponse> response = dnsProviderManager.listDnsRecords(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}