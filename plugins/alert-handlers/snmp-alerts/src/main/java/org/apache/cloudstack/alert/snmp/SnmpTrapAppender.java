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
// under the License

package org.apache.cloudstack.alert.snmp;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

import com.cloud.utils.net.NetUtils;

public class SnmpTrapAppender extends AppenderSkeleton {
    private String _delimiter = ",";
    private String _snmpManagerIpAddresses;
    private String _snmpManagerPorts;
    private String _snmpManagerCommunities;

    private String _oldSnmpManagerIpAddresses = null;
    private String _oldSnmpManagerPorts = null;
    private String _oldSnmpManagerCommunities = null;

    private List<String> _ipAddresses = null;
    private List<String> _communities = null;
    private List<String> _ports = null;

    List<SnmpHelper> _snmpHelpers = new ArrayList<SnmpHelper>();

    @Override
    protected void append(LoggingEvent event) {
        SnmpEnhancedPatternLayout snmpEnhancedPatternLayout;

        if (getLayout() == null) {
            errorHandler.error("No layout set for the Appender named [" + getName() + ']', null, ErrorCode.MISSING_LAYOUT);
            return;
        }

        if (getLayout() instanceof SnmpEnhancedPatternLayout) {
            snmpEnhancedPatternLayout = (SnmpEnhancedPatternLayout)getLayout();
        } else {
            return;
        }

        if (!isAsSevereAsThreshold(event.getLevel())) {
            return;
        }

        SnmpTrapInfo snmpTrapInfo = snmpEnhancedPatternLayout.parseEvent(event);

        if (snmpTrapInfo != null && !_snmpHelpers.isEmpty()) {
            for (SnmpHelper helper : _snmpHelpers) {
                try {
                    helper.sendSnmpTrap(snmpTrapInfo);
                } catch (Exception e) {
                    errorHandler.error(e.getMessage());
                }
            }
        }
    }

    void setSnmpHelpers() {
        if (_snmpManagerIpAddresses == null || _snmpManagerIpAddresses.trim().isEmpty() || _snmpManagerCommunities == null || _snmpManagerCommunities.trim().isEmpty() ||
            _snmpManagerPorts == null || _snmpManagerPorts.trim().isEmpty()) {
            reset();
            return;
        }

        if (_oldSnmpManagerIpAddresses != null && _oldSnmpManagerIpAddresses.equals(_snmpManagerIpAddresses) &&
            _oldSnmpManagerCommunities.equals(_snmpManagerCommunities) && _oldSnmpManagerPorts.equals(_snmpManagerPorts)) {
            return;
        }

        _oldSnmpManagerIpAddresses = _snmpManagerIpAddresses;
        _oldSnmpManagerPorts = _snmpManagerPorts;
        _oldSnmpManagerCommunities = _snmpManagerCommunities;

        _ipAddresses = parse(_snmpManagerIpAddresses);
        _communities = parse(_snmpManagerCommunities);
        _ports = parse(_snmpManagerPorts);

        if (!(_ipAddresses.size() == _communities.size() && _ipAddresses.size() == _ports.size())) {
            reset();
            errorHandler.error(" size of ip addresses , communities, " + "and ports list doesn't match, " + "setting all to null");
            return;
        }

        if (!validateIpAddresses() || !validatePorts()) {
            reset();
            errorHandler.error(" Invalid format for the IP Addresses or Ports parameter ");
            return;
        }

        String address;

        for (int i = 0; i < _ipAddresses.size(); i++) {
            address = _ipAddresses.get(i) + "/" + _ports.get(i);
            try {
                _snmpHelpers.add(new SnmpHelper(address, _communities.get(i)));
            } catch (Exception e) {
                errorHandler.error(e.getMessage());
            }
        }
    }

    private void reset() {
        _ipAddresses = null;
        _communities = null;
        _ports = null;
        _snmpHelpers.clear();
    }

    @Override
    public void close() {
        if (!closed)
            closed = true;
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    private List<String> parse(String str) {
        List<String> result = new ArrayList<String>();

        final StringTokenizer tokenizer = new StringTokenizer(str, _delimiter);
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken().trim());
        }
        return result;
    }

    private boolean validatePorts() {
        for (String port : _ports) {
            if (!NetUtils.isValidPort(port)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateIpAddresses() {
        for (String ipAddress : _ipAddresses) {
            if (ipAddress.trim().equalsIgnoreCase("localhost")) {
                continue;
            }
            if (!NetUtils.isValidIp4(ipAddress)) {
                return false;
            }
        }
        return true;
    }

    public String getSnmpManagerIpAddresses() {
        return _snmpManagerIpAddresses;
    }

    public void setSnmpManagerIpAddresses(String snmpManagerIpAddresses) {
        this._snmpManagerIpAddresses = snmpManagerIpAddresses;
        setSnmpHelpers();
    }

    public String getSnmpManagerPorts() {
        return _snmpManagerPorts;
    }

    public void setSnmpManagerPorts(String snmpManagerPorts) {
        this._snmpManagerPorts = snmpManagerPorts;
        setSnmpHelpers();
    }

    public String getSnmpManagerCommunities() {
        return _snmpManagerCommunities;
    }

    public void setSnmpManagerCommunities(String snmpManagerCommunities) {
        this._snmpManagerCommunities = snmpManagerCommunities;
        setSnmpHelpers();
    }

    public String getDelimiter() {
        return _delimiter;
    }

    public void setDelimiter(String delimiter) {
        this._delimiter = delimiter;
    }
}
