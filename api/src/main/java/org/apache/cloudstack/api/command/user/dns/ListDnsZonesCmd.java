package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;

@APICommand(name = "listDnsZones", description = "Lists DNS zones.",
        responseObject = DnsZoneResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDnsZonesCmd extends BaseListAccountResourcesCmd {
    private static final String COMMAND_RESPONSE_NAME = "listdnszonesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    ///
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            description = "List DNS zone by ID")
    private Long id;

    @Parameter(name = "dnsserverid", type = CommandType.UUID, entityType = DnsServerResponse.class,
            description = "List DNS zones belonging to a specific DNS server")
    private Long dnsServerId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "List by zone name")
    private String name;

    public Long getId() { return id; }
    public Long getDnsServerId() { return dnsServerId; }
    public String getName() { return name; }

    @Override
    public void execute() {
        ListResponse<DnsZoneResponse> response = dnsProviderManager.listDnsZones(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return COMMAND_RESPONSE_NAME;
    }
}
