package com.cloud.network.element;

import com.cloud.api.commands.ConfigureDhcpElementCmd;
import com.cloud.network.VirtualRouterElements;
import com.cloud.utils.component.PluggableService;

public interface DhcpElementService extends PluggableService{
    boolean configure(ConfigureDhcpElementCmd cmd);
    VirtualRouterElements addElement(Long nspId);
    Long getIdByNspId(Long nspId);
    boolean isReady(long nspId);
    VirtualRouterElements getCreatedElement(long id);
}
