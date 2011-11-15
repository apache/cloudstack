package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.AddExternalFirewallCmd;
import com.cloud.api.commands.DeleteExternalFirewallCmd;
import com.cloud.api.commands.ListExternalFirewallsCmd;
import com.cloud.host.Host;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.utils.component.PluggableService;

public interface JuniperSRXFirewallElementService  extends PluggableService {

    @Deprecated // API helper function supported for backward compatibility
    public Host addExternalFirewall(AddExternalFirewallCmd cmd);

    @Deprecated // API helper function supported for backward compatibility
    public boolean deleteExternalFirewall(DeleteExternalFirewallCmd cmd);
    
    @Deprecated // API helper function supported for backward compatibility
    public List<Host> listExternalFirewalls(ListExternalFirewallsCmd cmd);
    
    @Deprecated // API helper function supported for backward compatibility
    public ExternalFirewallResponse createExternalFirewallResponse(Host externalFirewall);
}
