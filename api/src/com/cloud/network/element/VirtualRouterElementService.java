package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.ConfigureVirtualRouterElementCmd;
import com.cloud.api.commands.ListVirtualRouterElementsCmd;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.utils.component.PluggableService;

public interface VirtualRouterElementService extends PluggableService{
    VirtualRouterProvider configure(ConfigureVirtualRouterElementCmd cmd);
    VirtualRouterProvider addElement(Long nspId);
    VirtualRouterProvider getCreatedElement(long id);
    List<? extends VirtualRouterProvider> searchForVirtualRouterElement(ListVirtualRouterElementsCmd cmd);
}
