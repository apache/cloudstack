package org.apache.cloudstack.api.command.user.dns;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.dns.DnsProviderManager;

@APICommand(name = "listDnsServers", description = "Lists DNS servers owned by the account.",
        responseObject = DnsServerResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDnsServersCmd  extends BaseListAccountResourcesCmd {
    private static final String COMMAND_RESPONSE_NAME = "listdnsserversresponse";

    @Inject
    DnsProviderManager dnsProviderManager;

    /////////////////////////////////////////////////////
    //////////////// API Parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DnsServerResponse.class,
            description = "the ID of the DNS server")
    private Long id;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING,
            description = "filter by provider type (e.g. PowerDNS, Cloudflare)")
    private String provider;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    /////////////////////////////////////////////////////
    /////////////// Implementation //////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<DnsServerResponse> response = dnsProviderManager.listDnsServers(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return COMMAND_RESPONSE_NAME;
    }
}
