package com.cloud.network.element;

import com.cloud.api.commands.ConfigureRedundantVirtualRouterElementCmd;

public interface RedundantVirtualRouterElementService extends VirtualRouterElementService {
    boolean configure(ConfigureRedundantVirtualRouterElementCmd cmd);
}
