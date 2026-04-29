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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

import java.util.Date;
import java.util.StringTokenizer;

public class SnmpEnhancedPatternLayout {
    private String _pairDelimiter = "//";
    private String _keyValueDelimiter = "::";

    private static final int LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER = 9;
    private static final int LENGTH_OF_STRING_MESSAGE = 8;

    public SnmpTrapInfo parseEvent(LogEvent event) {
        SnmpTrapInfo snmpTrapInfo = null;

        Message message = event.getMessage();
        final String formattedMessage = message.getFormattedMessage();
        if (formattedMessage.contains("alertType") && formattedMessage.contains("message")) {
            snmpTrapInfo = new SnmpTrapInfo();
            final StringTokenizer messageSplitter = new StringTokenizer(formattedMessage, _pairDelimiter);
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
                    snmpTrapInfo.setAlertType(Short.parseShort(valueToken));
                } else if (keyToken.equalsIgnoreCase("dataCenterId") && !valueToken.equalsIgnoreCase("null")) {
                    snmpTrapInfo.setDataCenterId(Long.parseLong(valueToken));
                } else if (keyToken.equalsIgnoreCase("podId") && !valueToken.equalsIgnoreCase("null")) {
                    snmpTrapInfo.setPodId(Long.parseLong(valueToken));
                } else if (keyToken.equalsIgnoreCase("clusterId") && !valueToken.equalsIgnoreCase("null")) {
                    snmpTrapInfo.setClusterId(Long.parseLong(valueToken));
                } else if (keyToken.equalsIgnoreCase("message") && !valueToken.equalsIgnoreCase("null")) {
                    snmpTrapInfo.setMessage(getSnmpMessage(formattedMessage));
                }
            }

            snmpTrapInfo.setGenerationTime(new Date(event.getTimeMillis()));
        }
        return snmpTrapInfo;
    }

    private String getSnmpMessage(String message) {
        int lastIndexOfKeyValueDelimiter = message.lastIndexOf(_keyValueDelimiter);
        int lastIndexOfMessageInString = message.lastIndexOf("message");

        if (lastIndexOfKeyValueDelimiter - lastIndexOfMessageInString <= LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER) {
            return message.substring(lastIndexOfKeyValueDelimiter + _keyValueDelimiter.length()).trim();
        } else if (lastIndexOfMessageInString < lastIndexOfKeyValueDelimiter) {
            return message.substring(lastIndexOfMessageInString + _keyValueDelimiter.length() + LENGTH_OF_STRING_MESSAGE).trim();
        }

        return message.substring(message.lastIndexOf("message" + _keyValueDelimiter) + LENGTH_OF_STRING_MESSAGE_AND_KEY_VALUE_DELIMITER).trim();
    }
}
