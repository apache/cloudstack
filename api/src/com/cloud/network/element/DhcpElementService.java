package com.cloud.network.element;

import com.cloud.api.commands.ConfigureDhcpElementCmd;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.utils.component.PluggableService;

public interface DhcpElementService extends PluggableService{
    boolean configure(ConfigureDhcpElementCmd cmd);
    VirtualRouterProvider addElement(Long nspId);
    Long getIdByNspId(Long nspId);
    VirtualRouterProvider getCreatedElement(long id);
}
