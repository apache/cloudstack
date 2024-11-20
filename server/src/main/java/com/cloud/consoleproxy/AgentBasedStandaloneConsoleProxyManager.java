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
package com.cloud.consoleproxy;

import java.util.List;


import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.vm.UserVmVO;

/**
 * This class is intended to replace the use of console proxy VMs managed by the Apache CloudStack (ACS)
 * to non ACS console proxy services. The documentation that describe its use and requirements can be found in <a href="https://cwiki.apache.org/confluence/display/CLOUDSTACK/QuickCloud">QuickCloud</a>.
 */
public class AgentBasedStandaloneConsoleProxyManager extends AgentBasedConsoleProxyManager {

    @Override
    public ConsoleProxyInfo assignProxy(long dataCenterId, long userVmId) {
        UserVmVO userVm = _userVmDao.findById(userVmId);
        if (userVm == null) {
            logger.warn("User VM " + userVmId + " no longer exists, return a null proxy for user vm:" + userVmId);
            return null;
        }

        HostVO host = findHost(userVm);
        if (host != null) {
            HostVO allocatedHost = null;
            /*Is there a consoleproxy agent running on the same machine?*/
            List<HostVO> hosts = _hostDao.listAllIncludingRemoved();
            for (HostVO hv : hosts) {
                if (hv.getType() == Host.Type.ConsoleProxy && hv.getPublicIpAddress().equalsIgnoreCase(host.getPublicIpAddress())) {
                    allocatedHost = hv;
                    break;
                }
            }
            if (allocatedHost == null) {
                /*Is there a consoleproxy agent running in the same pod?*/
                for (HostVO hv : hosts) {
                    if (hv.getType() == Host.Type.ConsoleProxy && hv.getPodId().equals(host.getPodId())) {
                        allocatedHost = hv;
                        break;
                    }
                }
            }
            if (allocatedHost == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to find a console proxy at host: " + host.getName() + " and in the pod: " + host.getPodId() + " to user vm " + userVmId);
                }
                return null;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Assign standalone console proxy running at " + allocatedHost.getName() + " to user vm " + userVmId + " with public IP "
                        + allocatedHost.getPublicIpAddress());
            }

            // only private IP, public IP, host id have meaningful values, rest of all are place-holder values
            String publicIp = allocatedHost.getPublicIpAddress();
            if (publicIp == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Host " + allocatedHost.getName() + "/" + allocatedHost.getPrivateIpAddress()
                            + " does not have public interface, we will return its private IP for cosole proxy.");
                }
                publicIp = allocatedHost.getPrivateIpAddress();
            }

            int urlPort = _consoleProxyUrlPort;
            if (allocatedHost.getProxyPort() != null && allocatedHost.getProxyPort().intValue() > 0) {
                urlPort = allocatedHost.getProxyPort().intValue();
            }

            return new ConsoleProxyInfo(_sslEnabled, publicIp, _consoleProxyPort, urlPort, _consoleProxyUrlDomain);
        } else {
            logger.warn("Host that VM is running is no longer available, console access to VM " + userVmId + " will be temporarily unavailable.");
        }
        return null;
    }
}
