package com.cloud.network.element;

import com.cloud.api.commands.ConfigureVirtualRouterElementCmd;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.utils.component.PluggableService;

public interface VirtualRouterElementService extends PluggableService{
    boolean configure(ConfigureVirtualRouterElementCmd cmd);
    VirtualRouterProvider addElement(Long nspId);
    Long getIdByNspId(Long nspId);
    VirtualRouterProvider getCreatedElement(long id);
}
