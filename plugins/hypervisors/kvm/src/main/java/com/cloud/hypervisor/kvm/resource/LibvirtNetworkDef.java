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
package com.cloud.hypervisor.kvm.resource;

import java.util.ArrayList;
import java.util.List;

public class LibvirtNetworkDef {
    enum NetworkType {
        BRIDGE, NAT, LOCAL
    }

    private final String _networkName;
    private final String _uuid;
    private NetworkType _networkType;
    private String _brName;
    private boolean _stp;
    private int _delay;
    private String _fwDev;
    private final String _domainName;
    private String _brIPAddr;
    private String _brNetMask;
    private final List<IPRange> ipranges = new ArrayList<IPRange>();
    private final List<DhcpMapping> dhcpMaps = new ArrayList<DhcpMapping>();

    public static class DhcpMapping {
        String _mac;
        String _name;
        String _ip;

        public DhcpMapping(String mac, String name, String ip) {
            _mac = mac;
            _name = name;
            _ip = ip;
        }
    }

    public static class IPRange {
        String _start;
        String _end;

        public IPRange(String start, String end) {
            _start = start;
            _end = end;
        }
    }

    public LibvirtNetworkDef(String netName, String uuid, String domName) {
        _networkName = netName;
        _uuid = uuid;
        _domainName = domName;
    }

    public void defNATNetwork(String brName, boolean stp, int delay, String fwNic, String ipAddr, String netMask) {
        _networkType = NetworkType.NAT;
        _brName = brName;
        _stp = stp;
        _delay = delay;
        _fwDev = fwNic;
        _brIPAddr = ipAddr;
        _brNetMask = netMask;
    }

    public void defBrNetwork(String brName, boolean stp, int delay, String fwNic, String ipAddr, String netMask) {
        _networkType = NetworkType.BRIDGE;
        _brName = brName;
        _stp = stp;
        _delay = delay;
        _fwDev = fwNic;
        _brIPAddr = ipAddr;
        _brNetMask = netMask;
    }

    public void defLocalNetwork(String brName, boolean stp, int delay, String ipAddr, String netMask) {
        _networkType = NetworkType.LOCAL;
        _brName = brName;
        _stp = stp;
        _delay = delay;
        _brIPAddr = ipAddr;
        _brNetMask = netMask;
    }

    public void adddhcpIPRange(String start, String end) {
        IPRange ipr = new IPRange(start, end);
        ipranges.add(ipr);
    }

    public void adddhcpMapping(String mac, String host, String ip) {
        DhcpMapping map = new DhcpMapping(mac, host, ip);
        dhcpMaps.add(map);
    }

    @Override
    public String toString() {
        StringBuilder netBuilder = new StringBuilder();
        netBuilder.append("<network>\n");
        netBuilder.append("<name>" + _networkName + "</name>\n");
        if (_uuid != null)
            netBuilder.append("<uuid>" + _uuid + "</uuid>\n");
        if (_brName != null) {
            netBuilder.append("<bridge name='" + _brName + "'");
            if (_stp) {
                netBuilder.append(" stp='on'");
            } else {
                netBuilder.append(" stp='off'");
            }
            if (_delay != -1) {
                netBuilder.append(" delay='" + _delay + "'");
            }
            netBuilder.append("/>\n");
        }
        if (_domainName != null) {
            netBuilder.append("<domain name='" + _domainName + "'/>\n");
        }
        if (_networkType == NetworkType.BRIDGE) {
            netBuilder.append("<forward mode='route'");
            if (_fwDev != null) {
                netBuilder.append(" dev='" + _fwDev + "'");
            }
            netBuilder.append("/>\n");
        } else if (_networkType == NetworkType.NAT) {
            netBuilder.append("<forward mode='nat'");
            if (_fwDev != null) {
                netBuilder.append(" dev='" + _fwDev + "'");
            }
            netBuilder.append("/>\n");
        }
        if (_brIPAddr != null || _brNetMask != null || !ipranges.isEmpty() || !dhcpMaps.isEmpty()) {
            netBuilder.append("<ip");
            if (_brIPAddr != null)
                netBuilder.append(" address='" + _brIPAddr + "'");
            if (_brNetMask != null) {
                netBuilder.append(" netmask='" + _brNetMask + "'");
            }
            netBuilder.append(">\n");

            if (!ipranges.isEmpty() || !dhcpMaps.isEmpty()) {
                netBuilder.append("<dhcp>\n");
                for (IPRange ip : ipranges) {
                    netBuilder.append("<range start='" + ip._start + "'" + " end='" + ip._end + "'/>\n");
                }
                for (DhcpMapping map : dhcpMaps) {
                    netBuilder.append("<host mac='" + map._mac + "' name='" + map._name + "' ip='" + map._ip + "'/>\n");
                }
                netBuilder.append("</dhcp>\n");
            }
            netBuilder.append("</ip>\n");
        }
        netBuilder.append("</network>\n");
        return netBuilder.toString();
    }
}
