package com.cloud.network.element;

import com.cloud.api.commands.ConfigureVirtualRouterElementCmd;

public interface VirtualRouterElementService extends DhcpElementService {
    boolean configure(ConfigureVirtualRouterElementCmd cmd);
}
