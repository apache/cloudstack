package com.cloud.consoleproxy;

import com.cloud.info.ConsoleProxyInfo;

public interface ConsoleProxyService {

    public abstract ConsoleProxyInfo assignProxy(long dataCenterId, long userVmId);

}