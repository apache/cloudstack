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
package com.cloud.agent.dhcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;

@Local(value = {DhcpSnooper.class})
public class FakeDhcpSnooper implements DhcpSnooper {
    private static final Logger s_logger = Logger.getLogger(FakeDhcpSnooper.class);
    private Queue<String> _ipAddresses = new ConcurrentLinkedQueue<String>();
    private Map<String, String> _macIpMap = new ConcurrentHashMap<String, String>();
    private Map<String, InetAddress> _vmIpMap = new ConcurrentHashMap<String, InetAddress>();

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        String guestIpRange = (String)params.get("guest.ip.range");
        if (guestIpRange != null) {
            String[] guestIps = guestIpRange.split("-");
            if (guestIps.length == 2) {
                long start = NetUtils.ip2Long(guestIps[0]);
                long end = NetUtils.ip2Long(guestIps[1]);
                while (start <= end) {
                    _ipAddresses.offer(NetUtils.long2Ip(start++));
                }
            }
        }
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public String getName() {
        return "FakeDhcpSnooper";
    }

    @Override
    public InetAddress getIPAddr(String macAddr, String vmName) {
        String ipAddr = _ipAddresses.poll();
        if (ipAddr == null) {
            s_logger.warn("No ip addresses left in queue");
            return null;
        }
        try {
            InetAddress inetAddr = InetAddress.getByName(ipAddr);
            _macIpMap.put(macAddr.toLowerCase(), ipAddr);
            _vmIpMap.put(vmName, inetAddr);
            s_logger.info("Got ip address " + ipAddr + " for vm " + vmName + " mac=" + macAddr.toLowerCase());
            return inetAddr;
        } catch (UnknownHostException e) {
            s_logger.warn("Failed to get InetAddress for " + ipAddr);
            return null;
        }
    }

    @Override
    public void cleanup(String macAddr, String vmName) {
        try {
            if (macAddr == null) {
                return;
            }
            InetAddress inetAddr = _vmIpMap.remove(vmName);
            String ipAddr = inetAddr.getHostName();
            for (Map.Entry<String, String> entry : _macIpMap.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(ipAddr)) {
                    macAddr = entry.getKey();
                    break;
                }
            }
            ipAddr = _macIpMap.remove(macAddr);

            s_logger.info("Cleaning up for mac address: " + macAddr + " ip=" + ipAddr + " inetAddr=" + inetAddr);
            if (ipAddr != null) {
                _ipAddresses.offer(ipAddr);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to cleanup: " + e.toString());
        }
    }

    @Override
    public Map<String, InetAddress> syncIpAddr() {
        return _vmIpMap;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public void initializeMacTable(List<Pair<String, String>> macVmNameList) {

    }

    @Override
    public InetAddress getDhcpServerIP() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub

    }

}
