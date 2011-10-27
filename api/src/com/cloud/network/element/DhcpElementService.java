package com.cloud.network.element;

import com.cloud.api.commands.ConfigureDhcpElementCmd;
import com.cloud.utils.component.PluggableService;

public interface DhcpElementService extends PluggableService{
    boolean configure(ConfigureDhcpElementCmd cmd);
    boolean addElement(Long nspId, String uuid);
    Long getIdByUUID(String uuid);
    boolean isReady(String uuid);
}
