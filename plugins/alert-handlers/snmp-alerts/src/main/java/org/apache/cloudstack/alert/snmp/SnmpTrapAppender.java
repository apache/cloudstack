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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.cloud.utils.net.NetUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "SnmpTrapAppender", category = "Core", elementType = "appender", printObject = true)
public class SnmpTrapAppender extends AbstractAppender {
    protected static Logger LOGGER = LogManager.getLogger(SnmpTrapAppender.class);
    private String _delimiter = ",";
    private String snmpManagerIpAddresses;
    private String snmpManagerPorts;
    private String snmpManagerCommunities;

    private String _oldSnmpManagerIpAddresses = null;
    private String _oldSnmpManagerPorts = null;
    private String _oldSnmpManagerCommunities = null;

    private List<String> _ipAddresses = null;
    private List<String> _communities = null;
    private List<String> _ports = null;

    private SnmpEnhancedPatternLayout snmpEnhancedPatternLayout;

    List<SnmpHelper> _snmpHelpers = new ArrayList<>();

    protected SnmpTrapAppender(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions, final Property[] properties,
            String snmpManagerIpAddresses, String snmpManagerPorts, String snmpManagerCommunities) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.snmpEnhancedPatternLayout = new SnmpEnhancedPatternLayout();
        this.snmpManagerIpAddresses = snmpManagerIpAddresses;
        this.snmpManagerPorts = snmpManagerPorts;
        this.snmpManagerCommunities = snmpManagerCommunities;
    }

    @Override
    public void append(LogEvent event) {
        SnmpTrapInfo snmpTrapInfo = snmpEnhancedPatternLayout.parseEvent(event);

        if (snmpTrapInfo != null && !_snmpHelpers.isEmpty()) {
            for (SnmpHelper helper : _snmpHelpers) {
                try {
                    helper.sendSnmpTrap(snmpTrapInfo);
                } catch (Exception e) {
                    getHandler().error(e.getMessage());
                }
            }
        }
    }

    @PluginFactory
    public static SnmpTrapAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter, @PluginAttribute("ignoreExceptions") boolean ignoreExceptions, @PluginElement("properties") final Property[] properties,
            @PluginAttribute("SnmpManagerIpAddresses") String snmpManagerIpAddresses, @PluginAttribute("SnmpManagerPorts") String snmpManagerPorts,
            @PluginAttribute("SnmpManagerCommunities") String snmpManagerCommunities) {

        if (name == null) {
            LOGGER.error("No name provided for SnmpTrapAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new SnmpTrapAppender(name, filter, layout, ignoreExceptions, properties, snmpManagerIpAddresses, snmpManagerPorts, snmpManagerCommunities);
    }

    void setSnmpHelpers() {
        if (snmpManagerIpAddresses == null || snmpManagerIpAddresses.trim().isEmpty() || snmpManagerCommunities == null || snmpManagerCommunities.trim().isEmpty() ||
            snmpManagerPorts == null || snmpManagerPorts.trim().isEmpty()) {
            reset();
            return;
        }

        if (_oldSnmpManagerIpAddresses != null && _oldSnmpManagerIpAddresses.equals(snmpManagerIpAddresses) &&
            _oldSnmpManagerCommunities.equals(snmpManagerCommunities) && _oldSnmpManagerPorts.equals(snmpManagerPorts)) {
            return;
        }

        _oldSnmpManagerIpAddresses = snmpManagerIpAddresses;
        _oldSnmpManagerPorts = snmpManagerPorts;
        _oldSnmpManagerCommunities = snmpManagerCommunities;

        _ipAddresses = parse(snmpManagerIpAddresses);
        _communities = parse(snmpManagerCommunities);
        _ports = parse(snmpManagerPorts);

        if (!(_ipAddresses.size() == _communities.size() && _ipAddresses.size() == _ports.size())) {
            reset();
            getHandler().error(" size of ip addresses , communities, " + "and ports list doesn't match, " + "setting all to null");
            return;
        }

        if (!validateIpAddresses() || !validatePorts()) {
            reset();
            getHandler().error(" Invalid format for the IP Addresses or Ports parameter ");
            return;
        }

        String address;

        for (int i = 0; i < _ipAddresses.size(); i++) {
            address = _ipAddresses.get(i) + "/" + _ports.get(i);
            try {
                _snmpHelpers.add(new SnmpHelper(address, _communities.get(i)));
            } catch (Exception e) {
                getHandler().error(e.getMessage());
            }
        }
    }

    private void reset() {
        _ipAddresses = null;
        _communities = null;
        _ports = null;
        _snmpHelpers.clear();
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

    public void setSnmpManagerIpAddresses(String snmpManagerIpAddresses) {
        this.snmpManagerIpAddresses = snmpManagerIpAddresses;
        setSnmpHelpers();
    }


    public void setSnmpManagerPorts(String snmpManagerPorts) {
        this.snmpManagerPorts = snmpManagerPorts;
        setSnmpHelpers();
    }

    public void setSnmpManagerCommunities(String snmpManagerCommunities) {
        this.snmpManagerCommunities = snmpManagerCommunities;
        setSnmpHelpers();
    }

}
