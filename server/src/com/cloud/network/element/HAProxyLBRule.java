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
package com.cloud.network.element;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;

@Component
public class HAProxyLBRule {

    private Logger logger = Logger.getLogger(getClass());

    public boolean validateHAProxyLBRule(final LoadBalancingRule rule) {
        final String timeEndChar = "dhms";

        if (rule.getSourcePortStart() == NetUtils.HAPROXY_STATS_PORT) {
            logger.debug("Can't create LB on port 8081, haproxy is listening for  LB stats on this port");
            return false;
        }

        for (final LbStickinessPolicy stickinessPolicy : rule.getStickinessPolicies()) {
            final List<Pair<String, String>> paramsList = stickinessPolicy.getParams();

            if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {

            } else if (StickinessMethodType.SourceBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                String tablesize = "200k"; // optional
                String expire = "30m"; // optional

                /* overwrite default values with the stick parameters */
                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("tablesize".equalsIgnoreCase(key)) {
                        tablesize = value;
                    }
                    if ("expire".equalsIgnoreCase(key)) {
                        expire = value;
                    }
                }
                if (expire != null && !containsOnlyNumbers(expire, timeEndChar)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: expire is not in timeformat: " + expire);
                }
                if (tablesize != null && !containsOnlyNumbers(tablesize, "kmg")) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: tablesize is not in size format: " + tablesize);

                }
            } else if (StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                String length = null; // optional
                String holdTime = null; // optional

                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("length".equalsIgnoreCase(key)) {
                        length = value;
                    }
                    if ("holdtime".equalsIgnoreCase(key)) {
                        holdTime = value;
                    }
                }

                if (length != null && !containsOnlyNumbers(length, null)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: length is not a number: " + length);
                }
                if (holdTime != null && !containsOnlyNumbers(holdTime, timeEndChar) && !containsOnlyNumbers(holdTime, null)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: holdtime is not in timeformat: " + holdTime);
                }
            }
        }
        return true;
    }

    /**
     * This function detects numbers like 12 ,32h ,42m .. etc,. 1) plain number
     * like 12 2) time or tablesize like 12h, 34m, 45k, 54m , here last
     * character is non-digit but from known characters .
     */
    private static boolean containsOnlyNumbers(final String str, final String endChar) {
        if (str == null) {
            return false;
        }

        String number = str;
        if (endChar != null) {
            boolean matchedEndChar = false;
            if (str.length() < 2) {
                return false; // at least one numeric and one char. example:
            }
            // 3h
            final char strEnd = str.toCharArray()[str.length() - 1];
            for (final char c : endChar.toCharArray()) {
                if (strEnd == c) {
                    number = str.substring(0, str.length() - 1);
                    matchedEndChar = true;
                    break;
                }
            }
            if (!matchedEndChar) {
                return false;
            }
        }
        try {
            Integer.parseInt(number);
        } catch (final NumberFormatException e) {
            return false;
        }
        return true;
    }

}
