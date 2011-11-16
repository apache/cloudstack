package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.AddExternalFirewallCmd;
import com.cloud.api.commands.AddSrxFirewallCmd;
import com.cloud.api.commands.ConfigureSrxFirewallCmd;
import com.cloud.api.commands.DeleteExternalFirewallCmd;
import com.cloud.api.commands.DeleteSrxFirewallCmd;
import com.cloud.api.commands.ListExternalFirewallsCmd;
import com.cloud.api.commands.ListSrxFirewallNetworksCmd;
import com.cloud.api.commands.ListSrxFirewallsCmd;
import com.cloud.api.response.SrxFirewallResponse;
import com.cloud.host.Host;
import com.cloud.network.ExternalFirewallDeviceVO;
import com.cloud.network.Network;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.utils.component.PluggableService;

public interface JuniperSRXFirewallElementService  extends PluggableService {

    /**
     * adds a SRX firewall device in to a physical network
     * @param AddSrxFirewallCmd 
     * @return ExternalFirewallDeviceVO object for the firewall added
     */
    public ExternalFirewallDeviceVO addSrxFirewall(AddSrxFirewallCmd cmd);

    /**
     * removes SRX firewall device from a physical network
     * @param DeleteSrxFirewallCmd 
     * @return true if firewall device successfully deleted
     */
    public boolean deleteSrxFirewall(DeleteSrxFirewallCmd cmd);

    /**
     * configures a SRX firewal device added in a physical network
     * @param ConfigureSrxFirewallCmd
     * @return ExternalFirewallDeviceVO for the device configured
     */
    public ExternalFirewallDeviceVO configureSrxFirewall(ConfigureSrxFirewallCmd cmd);

    /**
     * lists all the SRX firewall devices added in to a physical network
     * @param ListSrxFirewallsCmd
     * @return list of ExternalFirewallDeviceVO for the devices in the physical network.
     */
    public List<ExternalFirewallDeviceVO> listSrxFirewalls(ListSrxFirewallsCmd cmd);

    /**
     * lists all the guest networks using a SRX firewall device
     * @param ListSrxFirewallNetworksCmd
     * @return list of the guest networks that are using this F5 load balancer
     */
    public List<? extends Network> listNetworks(ListSrxFirewallNetworksCmd cmd);

    public SrxFirewallResponse createSrxFirewallResponse(ExternalFirewallDeviceVO fwDeviceVO);


    @Deprecated // API helper function supported for backward compatibility
    public Host addExternalFirewall(AddExternalFirewallCmd cmd);

    @Deprecated // API helper function supported for backward compatibility
    public boolean deleteExternalFirewall(DeleteExternalFirewallCmd cmd);
    
    @Deprecated // API helper function supported for backward compatibility
    public List<Host> listExternalFirewalls(ListExternalFirewallsCmd cmd);
    
    @Deprecated // API helper function supported for backward compatibility
    public ExternalFirewallResponse createExternalFirewallResponse(Host externalFirewall);
}
