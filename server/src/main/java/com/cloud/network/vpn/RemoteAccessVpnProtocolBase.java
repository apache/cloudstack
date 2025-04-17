// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.vpn;

import com.cloud.domain.dao.DomainDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.RulesManager;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import com.cloud.network.vpc.dao.VpcDao;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * This class describe the behaviour of a single vpn server provider (EG: L2TP, Wireguard, OpenVPN)
 */
public abstract class RemoteAccessVpnProtocolBase extends ManagerBase implements Configurable, RemoteAccessVpnService  {

    private final static Logger s_logger = Logger.getLogger(RemoteAccessVpnProtocolBase.class);

    static final String RemoteAccessVpnClientIpRangeCK = "remote.access.vpn.client.iprange";

    static final ConfigKey<String> RemoteAccessVpnClientIpRange = new ConfigKey<String>(
            "" + "Network",
            String.class,
            RemoteAccessVpnClientIpRangeCK,
            "10.1.2.1-10.1.2.8",
            "The range of ips to be allocated to remote access vpn clients. The first ip in the range is used by the VPN server",
            false,
            ConfigKey.Scope.Account
    );


    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    VpcDao _vpcDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    FirewallManager _firewallMgr;

    int _pskLength;
    int _userLimit;
    List<RemoteAccessVPNServiceProvider> _vpnServiceProviders;


    void validateRemoteAccessVpnConfiguration() throws ConfigurationException {
        String ipRange = RemoteAccessVpnClientIpRange.value();
        if (ipRange == null) {
            s_logger.warn(String.format("Remote access VPN configuration: Global configuration [%s] missing client IP range.", RemoteAccessVpnClientIpRange.key()));
            return;
        }

        if (_pskLength < 8 || _pskLength > 256) {
            throw new ConfigurationException(String.format("Remote access VPN configuration: IPSec preshared key length [%s] should be between 8 and 256.", _pskLength));
        }

        validateIpRange(ipRange, ConfigurationException.class);
    }

    protected <T extends Throwable> void validateIpRange(String ipRange, Class<T> exceptionClass) throws T {
        String[] range = ipRange.split("-");

        if (range.length != 2) {
            handleExceptionOnValidateIpRangeError(exceptionClass, String.format("IP range [%s] is an invalid IP range.", ipRange));
        }

        if (!NetUtils.isValidIp4(range[0]) || !NetUtils.isValidIp4(range[1])) {
            handleExceptionOnValidateIpRangeError(exceptionClass, String.format("One or both IPs sets in the range [%s] are invalid IPs.", ipRange));
        }

        if (!NetUtils.validIpRange(range[0], range[1])) {
            handleExceptionOnValidateIpRangeError(exceptionClass, String.format("Range of IPs [%s] is invalid.", ipRange));
        }
    }

    protected <T extends Throwable> void handleExceptionOnValidateIpRangeError(Class<T> exceptionClass, String errorMessage) throws T {
        try {
            throw exceptionClass.getConstructor(String.class).newInstance(errorMessage);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException ex) {
            throw new CloudRuntimeException(String.format("Unexpected exception [%s] while throwing error [%s] on validateIpRange.", ex.getMessage(), errorMessage), ex);
        }
    }


    public List<RemoteAccessVPNServiceProvider> getVpnServiceProviders() {
        return _vpnServiceProviders;
    }

    public void setVpnServiceProviders(List<RemoteAccessVPNServiceProvider> vpnServiceProviders) {
        _vpnServiceProviders = vpnServiceProviders;
    }

}
