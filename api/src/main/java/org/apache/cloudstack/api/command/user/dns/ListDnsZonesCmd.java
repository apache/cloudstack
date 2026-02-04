package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;

@APICommand(name = "listDnsZones", description = "Lists DNS Zones.",
        responseObject = DnsZoneResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDnsZonesCmd extends BaseListAccountResourcesCmd {
    private static final String COMMAND_RESPONSE_NAME = "listdnszonesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    ///
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            description = "list DNS zone by ID")
    private Long id;

    @Parameter(name = "dnsserverid", type = CommandType.UUID, entityType = DnsServerResponse.class,
            description = "list DNS zones belonging to a specific DNS Server")
    private Long dnsServerId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class,
            description = "list DNS zones associated with a specific Network")
    private Long networkId;

    public Long getId() { return id; }
    public Long getDnsServerId() { return dnsServerId; }
    public Long getNetworkId() { return networkId; }

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
