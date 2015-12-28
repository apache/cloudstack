package com.cloud.naming;

import com.cloud.vm.ConsoleProxy;
import com.cloud.vm.ConsoleProxyVO;

public interface ConsoleProxyNamingPolicy extends ResourceNamingPolicy<ConsoleProxy, ConsoleProxyVO>{

    public String getConsoleProxyName(Long id);
}
