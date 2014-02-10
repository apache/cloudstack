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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.net.SyslogAppender;
import org.apache.log4j.spi.LoggingEvent;

import com.cloud.utils.net.NetUtils;

public class AlertsSyslogAppender extends AppenderSkeleton {
    String _syslogHosts = null;
    String _delimiter = ",";
    List<String> _syslogHostsList = null;
    List<SyslogAppender> _syslogAppenders = null;
    private String _facility;
    private String _pairDelimiter = "//";
    private String _keyValueDelimiter = "::";
    private int alertType = -1;
    private long dataCenterId = 0;
    private long podId = 0;
    private long clusterId = 0;
    private String sysMessage = null;
    public static final int LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER = 9;
    public static final int LENGTH_OF_STRING_MESSAGE = 8;
    public static final String MESSAGE_DELIMITER_STRING = "   ";
    //add the alertType in this array it its level needs to be set to critical
    private static final int[] criticalAlerts = {7, 8, 9, 10, 11, 12, 13, 15, 16, 19, 20};
    private static final Map<Integer, String> alertsMap;

    static {
        Map<Integer, String> aMap = new HashMap<Integer, String>(27);
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

        alertsMap = Collections.unmodifiableMap(aMap);
    }

    @Override
    protected void append(LoggingEvent event) {
        if (!isAsSevereAsThreshold(event.getLevel())) {
            return;
        }

        if (_syslogAppenders != null && !_syslogAppenders.isEmpty()) {
            try {
                String logMessage = event.getRenderedMessage();
                if (logMessage.contains("alertType") && logMessage.contains("message")) {
                    parseMessage(logMessage);
                    String syslogMessage = createSyslogMessage();

                    LoggingEvent syslogEvent = new LoggingEvent(event.getFQNOfLoggerClass(), event.getLogger(), event.getLevel(), syslogMessage, null);

                    for (SyslogAppender syslogAppender : _syslogAppenders) {
                        syslogAppender.append(syslogEvent);
                    }
                }
            } catch (Exception e) {
                errorHandler.error(e.getMessage());
            }
        }
    }

    @Override
    synchronized public void close() {
        for (SyslogAppender syslogAppender : _syslogAppenders) {
            syslogAppender.close();
        }
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    void setSyslogAppenders() {
        if (_syslogAppenders == null) {
            _syslogAppenders = new ArrayList<SyslogAppender>();
        }

        if (_syslogHosts == null || _syslogHosts.trim().isEmpty()) {
            reset();
            return;
        }

        _syslogHostsList = parseSyslogHosts(_syslogHosts);

        if (!validateIpAddresses()) {
            reset();
            errorHandler.error(" Invalid format for the IP Addresses parameter ");
            return;
        }

        for (String syslogHost : _syslogHostsList) {
            _syslogAppenders.add(new SyslogAppender(getLayout(), syslogHost, SyslogAppender.getFacility(_facility)));
        }
    }

    private List<String> parseSyslogHosts(String syslogHosts) {
        List<String> result = new ArrayList<String>();

        final StringTokenizer tokenizer = new StringTokenizer(syslogHosts, _delimiter);
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken().trim());
        }
        return result;
    }

    private boolean validateIpAddresses() {
        for (String ipAddress : _syslogHostsList) {
            if (ipAddress.trim().equalsIgnoreCase("localhost")) {
                continue;
            }
            if (!NetUtils.isValidIp(ipAddress)) {
                return false;
            }
        }
        return true;
    }

    void parseMessage(String logMessage) {
        final StringTokenizer messageSplitter = new StringTokenizer(logMessage, _pairDelimiter);
        while (messageSplitter.hasMoreTokens()) {
            final String pairToken = messageSplitter.nextToken();
            final StringTokenizer pairSplitter = new StringTokenizer(pairToken, _keyValueDelimiter);
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
            message.append("alertType").append(_keyValueDelimiter).append(" ").append(alertsMap.get(alertType)).append(MESSAGE_DELIMITER_STRING);
            if (dataCenterId != 0) {
                message.append("dataCenterId").append(_keyValueDelimiter).append(" ").append(dataCenterId).append(MESSAGE_DELIMITER_STRING);
            }

            if (podId != 0) {
                message.append("podId").append(_keyValueDelimiter).append(" ").append(podId).append(MESSAGE_DELIMITER_STRING);
            }

            if (clusterId != 0) {
                message.append("clusterId").append(_keyValueDelimiter).append(" ").append(clusterId).append(MESSAGE_DELIMITER_STRING);
            }

            if (sysMessage != null) {
                message.append("message").append(_keyValueDelimiter).append(" ").append(sysMessage);
            } else {
                errorHandler.error(" What is the use of alert without message ");
            }
        } else {
            errorHandler.error(" Invalid alert Type ");
        }

        return message.toString();
    }

    private String getSyslogMessage(String message) {
        int lastIndexOfKeyValueDelimiter = message.lastIndexOf(_keyValueDelimiter);
        int lastIndexOfMessageInString = message.lastIndexOf("message");

        if (lastIndexOfKeyValueDelimiter - lastIndexOfMessageInString <= LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER) {
            return message.substring(lastIndexOfKeyValueDelimiter + _keyValueDelimiter.length()).trim();
        } else if (lastIndexOfMessageInString < lastIndexOfKeyValueDelimiter) {
            return message.substring(lastIndexOfMessageInString + _keyValueDelimiter.length() + LENGTH_OF_STRING_MESSAGE).trim();
        }

        return message.substring(message.lastIndexOf("message" + _keyValueDelimiter) + LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER).trim();
    }

    private void reset() {
        _syslogAppenders.clear();
    }

    public void setFacility(String facility) {
        if (facility == null) {
            return;
        }

        this._facility = facility;
        if (_syslogAppenders != null && !_syslogAppenders.isEmpty()) {
            for (SyslogAppender syslogAppender : _syslogAppenders) {
                syslogAppender.setFacility(facility);
            }
        }
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

    public String getFacility() {
        return _facility;
    }

    public String getSyslogHosts() {
        return _syslogHosts;
    }

    public void setSyslogHosts(String syslogHosts) {
        this._syslogHosts = syslogHosts;
        this.setSyslogAppenders();
    }

    public String getDelimiter() {
        return _delimiter;
    }

    public void setDelimiter(String delimiter) {
        this._delimiter = delimiter;
    }

    public String getPairDelimiter() {
        return _pairDelimiter;
    }

    public void setPairDelimiter(String pairDelimiter) {
        this._pairDelimiter = pairDelimiter;
    }

    public String getKeyValueDelimiter() {
        return _keyValueDelimiter;
    }

    public void setKeyValueDelimiter(String keyValueDelimiter) {
        this._keyValueDelimiter = keyValueDelimiter;
    }
}
