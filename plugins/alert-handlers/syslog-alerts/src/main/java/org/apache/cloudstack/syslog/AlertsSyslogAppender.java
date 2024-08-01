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

package org.apache.cloudstack.syslog;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


import com.cloud.utils.net.NetUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.message.SimpleMessage;

@Plugin(name = "AlertSyslogAppender", category = "Core", elementType = "appender", printObject = true)
public class AlertsSyslogAppender extends AbstractAppender {
    String syslogHosts;
    String delimiter = ",";
    List<String> syslogHostsList = null;
    List<SyslogAppender> syslogAppenders = null;
    private String facility;
    private String pairDelimiter = "//";
    private String keyValueDelimiter = "::";
    private int alertType = -1;
    private long dataCenterId = 0;
    private long podId = 0;
    private long clusterId = 0;
    private String sysMessage = null;
    public static final int LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER = 9;
    public static final int LENGTH_OF_STRING_MESSAGE = 8;
    public static final String MESSAGE_DELIMITER_STRING = "   ";
    //add the alertType in this array it its level needs to be set to critical
    private static final int[] criticalAlerts = {7, 8, 9, 10, 11, 12, 13, 15, 16, 19, 20, 27};
    private static final Map<Integer, String> alertsMap;

    static {
        Map<Integer, String> aMap = new HashMap<>(27);
        aMap.put(0, "availableMemory");
        aMap.put(1, "availableCpu");
        aMap.put(2, "availableStorage");
        aMap.put(3, "remainingStorageAllocated");
        aMap.put(4, "unallocatedVirtualNetworkpublicIp");
        aMap.put(5, "unallocatedPrivateIp");
        aMap.put(6, "availableSecondaryStorage");
        aMap.put(7, "host");
        aMap.put(8, "userVmState");
        aMap.put(9, "domainRouterVmState ");
        aMap.put(10, "consoleProxyVmState");
        aMap.put(11, "routingConnection");
        aMap.put(12, "storageIssueSystemVms");
        aMap.put(13, "usageServerStatus");
        aMap.put(14, "managementNode");
        aMap.put(15, "domainRouterMigrate");
        aMap.put(16, "consoleProxyMigrate");
        aMap.put(17, "userVmMigrate");
        aMap.put(18, "unallocatedVlan");
        aMap.put(19, "ssvmStopped");
        aMap.put(20, "usageServerResult");
        aMap.put(21, "storageDelete");
        aMap.put(22, "updateResourceCount");
        aMap.put(23, "usageSanityResult");
        aMap.put(24, "unallocatedDirectAttachedPublicIp");
        aMap.put(25, "unallocatedLocalStorage");
        aMap.put(26, "resourceLimitExceeded");
        aMap.put(27, "sync");

        alertsMap = Collections.unmodifiableMap(aMap);
    }

    protected AlertsSyslogAppender(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions, final Property[] properties, String facility,
            String syslogHosts){
        super(name, filter, layout, ignoreExceptions, properties);
        this.facility = facility;
        this.syslogHosts = syslogHosts;
    }

    @Override
    public void append(LogEvent event) {
        if (syslogAppenders != null && !syslogAppenders.isEmpty()) {
            try {
                String logMessage = event.getMessage().getFormattedMessage();
                if (logMessage.contains("alertType") && logMessage.contains("message")) {
                    parseMessage(logMessage);
                    String syslogMessage = createSyslogMessage();

                    LogEvent syslogEvent = new Log4jLogEvent(event.getLoggerName(), event.getMarker(), event.getLoggerFqcn(), event.getLevel(), new SimpleMessage(syslogMessage),  event.getThrown());

                    for (SyslogAppender syslogAppender : syslogAppenders) {
                        syslogAppender.append(syslogEvent);
                    }
                }
            } catch (Exception e) {
                getHandler().error(e.getMessage());
            }
        }
    }

    @PluginFactory
    public static AlertsSyslogAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter, @PluginAttribute("ignoreExceptions") boolean ignoreExceptions, @PluginElement("properties") final Property[] properties,
            @PluginAttribute("facility") String facility, @PluginAttribute("syslogHosts") String syslogHosts) {
            return new AlertsSyslogAppender(name, filter, layout, ignoreExceptions, properties, facility, syslogHosts);
    }

    void setSyslogAppenders() {
        if (syslogAppenders == null) {
            syslogAppenders = new ArrayList<SyslogAppender>();
        }

        if (syslogHosts == null || syslogHosts.trim().isEmpty()) {
            reset();
            return;
        }

        syslogHostsList = parseSyslogHosts(syslogHosts);

        if (!validateIpAddresses()) {
            reset();
            getHandler().error(" Invalid format for the IP Addresses parameter ");
            return;
        }

        for (String syslogHost : syslogHostsList) {
            syslogAppenders.add(SyslogAppender.newSyslogAppenderBuilder().setFacility(Facility.toFacility(facility)).setHost(syslogHost).setLayout(getLayout()).build());
        }
    }

    private List<String> parseSyslogHosts(String syslogHosts) {
        List<String> result = new ArrayList<String>();

        final StringTokenizer tokenizer = new StringTokenizer(syslogHosts, delimiter);
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken().trim());
        }
        return result;
    }

    private boolean validateIpAddresses() {
        for (String ipAddress : syslogHostsList) {
            String[] hostTokens = (ipAddress.trim()).split(":");
            String ip = hostTokens[0];

            if (hostTokens.length >= 1 && hostTokens.length <= 2) {
                if (hostTokens.length == 2 && !NetUtils.isValidPort(hostTokens[1])) {
                    return false;
                }
                if (ip.equalsIgnoreCase("localhost")) {
                    continue;
                }
                if (!NetUtils.isValidIp4(ip)) {
                    return false;
                }
            } else
            {
                return false;
            }
        }

        return true;
    }

    void parseMessage(String logMessage) {
        final StringTokenizer messageSplitter = new StringTokenizer(logMessage, pairDelimiter);
        while (messageSplitter.hasMoreTokens()) {
            final String pairToken = messageSplitter.nextToken();
            final StringTokenizer pairSplitter = new StringTokenizer(pairToken, keyValueDelimiter);
            String keyToken;
            String valueToken;

            if (pairSplitter.hasMoreTokens()) {
                keyToken = pairSplitter.nextToken().trim();
            } else {
                break;
            }

            if (pairSplitter.hasMoreTokens()) {
                valueToken = pairSplitter.nextToken().trim();
            } else {
                break;
            }

            if (keyToken.equalsIgnoreCase("alertType") && !valueToken.equalsIgnoreCase("null")) {
                alertType = Short.parseShort(valueToken);
            } else if (keyToken.equalsIgnoreCase("dataCenterId") && !valueToken.equalsIgnoreCase("null")) {
                dataCenterId = Long.parseLong(valueToken);
            } else if (keyToken.equalsIgnoreCase("podId") && !valueToken.equalsIgnoreCase("null")) {
                podId = Long.parseLong(valueToken);
            } else if (keyToken.equalsIgnoreCase("clusterId") && !valueToken.equalsIgnoreCase("null")) {
                clusterId = Long.parseLong(valueToken);
            } else if (keyToken.equalsIgnoreCase("message") && !valueToken.equalsIgnoreCase("null")) {
                sysMessage = getSyslogMessage(logMessage);
            }
        }
    }

    String createSyslogMessage() {
        StringBuilder message = new StringBuilder();
        message.append(severityOfAlert(alertType)).append(MESSAGE_DELIMITER_STRING);
        InetAddress ip;
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            ip = null;
        }

        if (ip != null) {
            message.append(ip.getHostName()).append(MESSAGE_DELIMITER_STRING);
        } else {
            message.append("unknown" + MESSAGE_DELIMITER_STRING);
        }

        if (alertType >= 0) {
            message.append("alertType").append(keyValueDelimiter).append(" ").append(alertsMap.containsKey(alertType) ? alertsMap.get(alertType) : "unknown")
                    .append(MESSAGE_DELIMITER_STRING);
            if (dataCenterId != 0) {
                message.append("dataCenterId").append(keyValueDelimiter).append(" ").append(dataCenterId).append(MESSAGE_DELIMITER_STRING);
            }

            if (podId != 0) {
                message.append("podId").append(keyValueDelimiter).append(" ").append(podId).append(MESSAGE_DELIMITER_STRING);
            }

            if (clusterId != 0) {
                message.append("clusterId").append(keyValueDelimiter).append(" ").append(clusterId).append(MESSAGE_DELIMITER_STRING);
            }

            if (sysMessage != null) {
                message.append("message").append(keyValueDelimiter).append(" ").append(sysMessage);
            } else {
                getHandler().error("What is the use of alert without message ");
            }
        } else {
            getHandler().error("Invalid alert Type ");
        }

        return message.toString();
    }

    private String getSyslogMessage(String message) {
        int lastIndexOfKeyValueDelimiter = message.lastIndexOf(keyValueDelimiter);
        int lastIndexOfMessageInString = message.lastIndexOf("message");

        if (lastIndexOfKeyValueDelimiter - lastIndexOfMessageInString <= LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER) {
            return message.substring(lastIndexOfKeyValueDelimiter + keyValueDelimiter.length()).trim();
        } else if (lastIndexOfMessageInString < lastIndexOfKeyValueDelimiter) {
            return message.substring(lastIndexOfMessageInString + keyValueDelimiter.length() + LENGTH_OF_STRING_MESSAGE).trim();
        }

        return message.substring(message.lastIndexOf("message" + keyValueDelimiter) + LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER).trim();
    }

    private void reset() {
        syslogAppenders.clear();
    }

    private String severityOfAlert(int alertType) {
        if (isCritical(alertType)) {
            return "CRITICAL";
        } else {
            return "WARN";
        }
    }

    private boolean isCritical(int alertType) {
        for (int type : criticalAlerts) {
            if (type == alertType) {
                return true;
            }
        }
        return false;
    }

    public void setSyslogHosts(String syslogHosts) {
        this.syslogHosts = syslogHosts;
        setSyslogAppenders();
    }

}
